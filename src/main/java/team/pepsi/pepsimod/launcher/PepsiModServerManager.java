/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016 Team Pepsi
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it. Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from Team Pepsi.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: Team Pepsi), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package team.pepsi.pepsimod.launcher;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import org.jutils.jhardware.HardwareInfo;
import org.jutils.jhardware.model.*;
import team.pepsi.pepsimod.common.util.CryptUtils;
import team.pepsi.pepsimod.common.util.Zlib;
import team.pepsi.pepsimod.launcher.packet.ClientRequest;
import team.pepsi.pepsimod.launcher.packet.Packet;
import team.pepsi.pepsimod.launcher.packet.ServerClose;
import team.pepsi.pepsimod.launcher.packet.ServerPepsimodSend;
import team.pepsi.pepsimod.launcher.util.CerializableUtils;
import team.pepsi.pepsimod.launcher.util.DataTag;
import team.pepsi.pepsimod.launcher.util.PepsimodSent;

import javax.swing.*;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.HashMap;
import java.util.Map;

public class PepsiModServerManager {
    public static final int protocol = 4;
    public static DataTag tag = new DataTag(new File(DataTag.HOME_FOLDER.getPath() + File.separatorChar + "pepsimodauth.dat"));
    public static String hwid = null;
    public static boolean errored = false;
    public static boolean wrongPass = false;
    public static String newPassword = null;
    static boolean processedResponse = false;

    static {
        LauncherMixinLoader.label.setText("Removing cryptography restrictions...");
        removeCryptographyRestrictions();
        LauncherMixinLoader.label.setText("Generating HWID...");
        getHWID();
    }

    public static void promptForCredentials() {
        JLabel label_login = new JLabel("Username:");
        JTextField login = new JTextField();

        JLabel label_password = new JLabel("Password:");
        JPasswordField password = new JPasswordField();

        Object[] array = {label_login, login, label_password, password};

        if (LauncherMixinLoader.dialog != null) {
            LauncherMixinLoader.dialog.setVisible(false);
        }
        int res = JOptionPane.showConfirmDialog(null, array, "Login",
                JOptionPane.OK_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.YES_OPTION) {
            FMLCommonHandler.instance().exitJava(93287, true);
        }
        tag.setString("username", login.getText());
        tag.setString("password", password.getText());
        tag.save();
        if (LauncherMixinLoader.dialog != null) {
            LauncherMixinLoader.dialog.setVisible(true);
        }
    }

    public static PepsimodSent decrypt(ServerPepsimodSend send) {
        LauncherMixinLoader.label.setText("Decrypting data...");
        byte[] currentState = send.classes;
        currentState = Zlib.inflate(currentState);
        Object decrypted;
        try {
            currentState = CryptUtils.decrypt(currentState, getPassword());
            if (currentState == null) {
                return null;
            }
            decrypted = CerializableUtils.fromBytes(currentState);
        } catch (Exception e) {
            LauncherMixinLoader.dialog.setVisible(false);
            JOptionPane.showMessageDialog(null, "Invalid password!", "pepsimod error", JOptionPane.OK_OPTION);
            return null;
        }
        HashMap<String, byte[]> classes = (HashMap<String, byte[]>) decrypted;
        currentState = send.assets;
        currentState = Zlib.inflate(currentState);
        try {
            currentState = CryptUtils.decrypt(currentState, getPassword());
            if (currentState == null) {
                return null;
            }
            decrypted = CerializableUtils.fromBytes(currentState);
        } catch (Exception e) {
            LauncherMixinLoader.dialog.setVisible(false);
            JOptionPane.showMessageDialog(null, "Invalid password!", "pepsimod error", JOptionPane.OK_OPTION);
            return null;
        }
        HashMap<String, byte[]> assets = (HashMap<String, byte[]>) decrypted;
        for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            classes.put(entry.getKey(), Zlib.inflate(entry.getValue())); //inflate everything
        }
        for (Map.Entry<String, byte[]> entry : assets.entrySet()) {
            assets.put(entry.getKey(), Zlib.inflate(entry.getValue())); //inflate everything
        }
        return new PepsimodSent(classes, assets, send.config);
    }

    private static boolean handleClose(ServerClose close) {
        if (close.hard) {
            FMLLog.log.info(close.message);
            if (LauncherMixinLoader.dialog != null) {
                LauncherMixinLoader.dialog.setVisible(false);
            }
            JOptionPane.showMessageDialog(null, close.message, "pepsimod error", JOptionPane.OK_OPTION);
            forceShutdown();
            return false;
        } else {
            JOptionPane.showMessageDialog(null, close.message, "pepsimod error", JOptionPane.OK_OPTION);
            return true;
        }
    }

    public static PepsimodSent downloadPepsiMod() {
        errored = false;
        wrongPass = false;
        processedResponse = false;
        FMLLog.log.info("Preparing...");
        LauncherMixinLoader.label.setText("Communicating with pepsimod server...");
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap().group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    FMLLog.log.info("initialized channel");
                    ch.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                    ch.pipeline().addLast("frameEncoder", new LengthFieldPrepender(4));
                    ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) {
                            FMLLog.log.info("Read from channel");
                            Packet packet = new Packet((ByteBuf) msg);
                            packet.decode();
                            FMLLog.log.info("Handling packet ID " + packet.getId());
                            if (packet.getId() == 1) {
                                wrongPass = false;
                                ServerPepsimodSend pepsimodSend = new ServerPepsimodSend(packet.buffer);
                                pepsimodSend.decode();
                                FMLLog.log.info("Class size: " + pepsimodSend.classes.length + " bytes");
                                if (decrypt(pepsimodSend) == null) {
                                    wrongPass = true;
                                } else {
                                    FMLLog.log.info("Successfully decrypted pepsimod!");
                                    ctx.close();
                                }
                            } else if (packet.getId() == 0) {
                                ServerClose close = new ServerClose(packet.buffer);
                                close.decode();
                                if (handleClose(close)) {
                                    wrongPass = true;
                                } else {
                                    errored = true;
                                }
                            }
                            processedResponse = true;
                        }
                    });
                }
            });
            FMLLog.log.info("Created bootstrap");
            Channel channel = bootstrap.connect("home.daporkchop.net", 48273).sync().channel();
            FMLLog.log.info("Connected!");
            ClientRequest request = new ClientRequest();
            request.hwid = getHWID();
            request.nextRequest = 0;
            request.protocol = protocol;
            request.username = getUsername();
            request.version = getVersion();
            request.password = "";
            request.config = "";
            request.encode();
            channel.writeAndFlush(request.buffer);
            FMLLog.log.info("sent!");
            while (!processedResponse) {
                if (processedResponse) {
                    break;
                } else {
                    Thread.sleep(1000);
                    FMLLog.log.info("Waiting...");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }

        if (errored) {
            forceShutdown();
        } else if (wrongPass) {
            promptForCredentials();
            return downloadPepsiMod();
        }
        FMLLog.log.info("Done!");

        return PepsimodSent.INSTANCE;
    }

    public static String setPassword() {
        JLabel label_password = new JLabel("New password:");
        JPasswordField password = new JPasswordField();

        Object[] array = {label_password, password};

        if (LauncherMixinLoader.dialog != null) {
            LauncherMixinLoader.dialog.setVisible(false);
        }
        int res = JOptionPane.showConfirmDialog(null, array, "Change password", JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.YES_OPTION) {
            return null;
        }

        return setPassword(password.getText());
    }

    public static String setPassword(String toSet) {
        errored = false;
        wrongPass = false;
        processedResponse = false;
        FMLLog.log.info("Preparing...");
        LauncherMixinLoader.label.setText("Communicating with pepsimod server...");
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap().group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    System.out.println("initialized channel");
                    ch.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                    ch.pipeline().addLast("frameEncoder", new LengthFieldPrepender(4));
                    ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) {
                            System.out.println("Read from channel");
                            Packet packet = new Packet((ByteBuf) msg);
                            packet.decode();
                            System.out.println("Handling packet ID " + packet.getId());
                            if (packet.getId() == 0) {
                                ServerClose close = new ServerClose(packet.buffer);
                                close.decode();
                                if (close.message.toLowerCase().startsWith("success")) {
                                    ctx.close();
                                    return;
                                }
                                if (handleClose(close)) {
                                    promptForCredentials();
                                } else {
                                    forceShutdown();
                                }
                                ctx.close();
                            }
                        }
                    });
                }
            });
            FMLLog.log.info("Created bootstrap");
            Channel channel = bootstrap.connect("home.daporkchop.net", 48273).sync().channel();
            FMLLog.log.info("Connected!");
            ClientRequest request = new ClientRequest();
            request.hwid = getHWID();
            request.nextRequest = 1;
            request.protocol = protocol;
            request.username = getUsername();
            request.version = getVersion();
            request.password = toSet;
            request.config = "";
            request.encode();
            channel.writeAndFlush(request.buffer);
            FMLLog.log.info("sent!");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }

        tag.setString("password", toSet);
        tag.save();
        FMLLog.log.info("Done!");

        return newPassword;
    }

    public static String setConfig(String toSet) {
        errored = false;
        wrongPass = false;
        processedResponse = false;
        FMLLog.log.info("Preparing...");
        LauncherMixinLoader.label.setText("Communicating with pepsimod server...");
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap().group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    System.out.println("initialized channel");
                    ch.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                    ch.pipeline().addLast("frameEncoder", new LengthFieldPrepender(4));
                    ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) {
                            System.out.println("Read from channel");
                            Packet packet = new Packet((ByteBuf) msg);
                            packet.decode();
                            System.out.println("Handling packet ID " + packet.getId());
                            if (packet.getId() == 0) {
                                ServerClose close = new ServerClose(packet.buffer);
                                close.decode();
                                if (close.message.toLowerCase().startsWith("success")) {
                                    ctx.close();
                                    return;
                                }
                                if (handleClose(close)) {
                                    promptForCredentials();
                                } else {
                                    forceShutdown();
                                }
                                ctx.close();
                            }
                        }
                    });
                }
            });
            FMLLog.log.info("Created bootstrap");
            Channel channel = bootstrap.connect("home.daporkchop.net", 48273).sync().channel();
            FMLLog.log.info("Connected!");
            ClientRequest request = new ClientRequest();
            request.hwid = getHWID();
            request.nextRequest = 2;
            request.protocol = protocol;
            request.username = getUsername();
            request.version = getVersion();
            request.password = "";
            request.config = toSet;
            request.encode();
            channel.writeAndFlush(request.buffer);
            FMLLog.log.info("sent!");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }

        tag.setString("password", toSet);
        tag.save();
        FMLLog.log.info("Done!");

        return newPassword;
    }

    public static String getUsername() {
        if (tag.getString("username", null) == null) {
            promptForCredentials();
        }
        return tag.getString("username");
    }

    public static String getPassword() {
        return tag.getString("password");
    }

    public static String getHWID() {
        if (hwid != null) {
            return hwid;
        }
        ProcessorInfo info = HardwareInfo.getProcessorInfo();
        BiosInfo info1 = HardwareInfo.getBiosInfo();
        MotherboardInfo info2 = HardwareInfo.getMotherboardInfo();
        GraphicsCardInfo info3 = HardwareInfo.getGraphicsCardInfo();
        MemoryInfo info4 = HardwareInfo.getMemoryInfo();
        OSInfo info5 = HardwareInfo.getOSInfo();
        GraphicsCard card = info3.getGraphicsCards().size() > 0 ? info3.getGraphicsCards().get(0) : null;
        hwid = info.getCacheSize() + info.getModelName() + info.getModel() + info.getVendorId() + info.getMhz() + info.getNumCores() + info.getFamily() + info1.getVersion() + info1.getDate() + info1.getManufacturer() + info2.getManufacturer() + info2.getVersion() + info2.getName() + (card != null ? card.getChipType() + card.getName() + card.getManufacturer() + card.getDeviceType() : "nonce") + info3.getGraphicsCards().size() + info4.getTotalMemory() + info5.getName() + info5.getManufacturer() + info5.getVersion();
        return hwid;
    }

    private static void removeCryptographyRestrictions() {
        if (!isRestrictedCryptography()) {
            FMLLog.log.info("Cryptography restrictions removal not needed");
            return;
        }
        try {
        /*
         * Do the following, but with reflection to bypass access checks:
         *
         * JceSecurity.isRestricted = false;
         * JceSecurity.defaultPolicy.perms.clear();
         * JceSecurity.defaultPolicy.add(CryptoAllPermission.INSTANCE);
         */
            final Class<?> jceSecurity = Class.forName("javax.crypto.JceSecurity");
            final Class<?> cryptoPermissions = Class.forName("javax.crypto.CryptoPermissions");
            final Class<?> cryptoAllPermission = Class.forName("javax.crypto.CryptoAllPermission");

            final Field isRestrictedField = jceSecurity.getDeclaredField("isRestricted");
            isRestrictedField.setAccessible(true);
            final Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(isRestrictedField, isRestrictedField.getModifiers() & ~Modifier.FINAL);
            isRestrictedField.set(null, false);

            final Field defaultPolicyField = jceSecurity.getDeclaredField("defaultPolicy");
            defaultPolicyField.setAccessible(true);
            final PermissionCollection defaultPolicy = (PermissionCollection) defaultPolicyField.get(null);

            final Field perms = cryptoPermissions.getDeclaredField("perms");
            perms.setAccessible(true);
            ((Map<?, ?>) perms.get(defaultPolicy)).clear();

            final Field instance = cryptoAllPermission.getDeclaredField("INSTANCE");
            instance.setAccessible(true);
            defaultPolicy.add((Permission) instance.get(null));

            FMLLog.log.info("Successfully removed cryptography restrictions");
        } catch (final Exception e) {
            FMLLog.log.info("Failed to remove cryptography restrictions");
        }
    }

    private static boolean isRestrictedCryptography() {
        // This matches Oracle Java 7 and 8, but not Java 9 or OpenJDK.
        final String name = System.getProperty("java.runtime.name");
        final String ver = System.getProperty("java.version");
        return name != null && name.equals("Java(TM) SE Runtime Environment")
                && ver != null && (ver.startsWith("1.7") || ver.startsWith("1.8"));
    }

    public static void forceShutdown() {
        try {
            Runtime.class.getDeclaredMethod("exit", int.class).invoke(Runtime.getRuntime(), 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Runtime.class.getDeclaredMethod("halt", int.class).invoke(Runtime.getRuntime(), 3);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            FMLCommonHandler.instance().exitJava(1, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            FMLCommonHandler.instance().exitJava(3, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new NullPointerException("xd let's crash the game since forceshutdown didn't work");
    }

    public static String getVersion() {
        try {
            Field f = ForgeVersion.class.getDeclaredField("mcVersion");
            String version = "pepsimod-" + f.get(null);
            FMLLog.log.info(version);
            return version;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }
}
