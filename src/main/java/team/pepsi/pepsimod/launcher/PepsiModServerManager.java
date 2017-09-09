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

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import org.jutils.jhardware.HardwareInfo;
import org.jutils.jhardware.model.*;
import team.pepsi.pepsimod.common.*;
import team.pepsi.pepsimod.common.message.ClientboundMessage;
import team.pepsi.pepsimod.common.util.CryptUtils;
import team.pepsi.pepsimod.common.util.SerializableUtils;
import team.pepsi.pepsimod.common.util.Zlib;
import team.pepsi.pepsimod.launcher.util.CerializableUtils;
import team.pepsi.pepsimod.launcher.util.DataTag;
import team.pepsi.pepsimod.launcher.util.PepsimodSent;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.Socket;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.HashMap;
import java.util.Map;

public class PepsiModServerManager {
    public static final int
            ERROR_BANNED = 0,
            ERROR_HWID = 1,
            ERROR_WRONGCLASS = 2,
            NOTIFICATION_SUCCESS = -1,
            NOTIFICATION_USER = 1,
            NOTIFICATION_IGNORE = -2,
            NOTIFICATION_SENDPASS = 0;
    public static final int protocol = 1;
    public static DataTag tag = new DataTag(new File(DataTag.HOME_FOLDER.getPath() + File.separatorChar + "pepsimodauth.dat"));
    public static String hwid = null;

    static {
        removeCryptographyRestrictions();
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

    private static Object handlePacket(Object obj, int state, Object... arguments) {
        if (obj instanceof ClientboundMessage) {
            obj = SerializableUtils.fromBytes(((ClientboundMessage) obj).data);
        } else if (obj instanceof ServerPepsiModSending) {

        } else {
            forceShutdown();
        }
        if (obj instanceof ServerLoginErrorMessage) {
            ServerLoginErrorMessage pck = (ServerLoginErrorMessage) obj;
            FMLLog.log.info(pck.error);
            if (LauncherMixinLoader.dialog != null) {
                LauncherMixinLoader.dialog.setVisible(false);
            }
            JOptionPane.showMessageDialog(null, pck.error, "pepsimod error", JOptionPane.OK_OPTION);
            forceShutdown();
        } else if (obj instanceof ServerNotification) {
            ServerNotification pck = (ServerNotification) obj;
            switch (pck.code) {
                case NOTIFICATION_IGNORE:
                    return null;
                case NOTIFICATION_SUCCESS:
                    JOptionPane.showMessageDialog(null, pck.error, "pepsimod password change", JOptionPane.OK_OPTION);
                    return null;
                case NOTIFICATION_USER:
                    JOptionPane.showMessageDialog(null, pck.error, "pepsimod error", JOptionPane.OK_OPTION);
                    promptForCredentials();
                    return null;
                case NOTIFICATION_SENDPASS:
                    return new ClientChangePassword((String) arguments[0]);
                default:
                    FMLLog.log.info("invalid notification code: " + pck.code);
                    forceShutdown();
                    return "asdf";
            }
        } else if (obj instanceof ServerPepsiModSending) {
            ServerPepsiModSending pck = (ServerPepsiModSending) obj;
            byte[] currentState = pck.classes;
            currentState = Zlib.inflate(currentState);
            Object decrypted;
            try {
                //if an IllegalStateException is thrown, then we can assume that the password is incorrect (as it's used for decryption)
                currentState = CryptUtils.decrypt(currentState, getPassword());
                if (currentState == null) {
                    throw new IllegalStateException();
                }
                decrypted = CerializableUtils.fromBytes(currentState);
            } catch (Exception e) {
                LauncherMixinLoader.dialog.setVisible(false);
                JOptionPane.showMessageDialog(null, "Invalid password!", "pepsimod error", JOptionPane.OK_OPTION);
                promptForCredentials();
                return null;
            }
            HashMap<String, byte[]> classes = (HashMap<String, byte[]>) decrypted;
            currentState = pck.assets;
            currentState = Zlib.inflate(currentState);
            try {
                currentState = CryptUtils.decrypt(currentState, getPassword());
                if (currentState == null) {
                    throw new IllegalStateException();
                }
                decrypted = CerializableUtils.fromBytes(currentState);
            } catch (Exception e) {
                LauncherMixinLoader.dialog.setVisible(false);
                JOptionPane.showMessageDialog(null, "Invalid password!", "pepsimod error", JOptionPane.OK_OPTION);
                promptForCredentials();
                return null;
            }
            HashMap<String, byte[]> assets = (HashMap<String, byte[]>) decrypted;
            for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
                classes.put(entry.getKey(), Zlib.inflate(entry.getValue())); //inflate everything
            }
            for (Map.Entry<String, byte[]> entry : assets.entrySet()) {
                assets.put(entry.getKey(), Zlib.inflate(entry.getValue())); //inflate everything
            }
            new PepsimodSent(classes, assets);
            return PepsimodSent.INSTANCE;
        }

        throw new IllegalStateException("Invalid packet recieved: " + obj.getClass().getCanonicalName());
    }

    public static PepsimodSent downloadPepsiMod() {
        Socket socket = null;
        ObjectInputStream is = null;
        ObjectOutputStream os = null;
        boolean restart = false, errored = false;
        try {
            socket = new Socket("anarchy.daporkchop.net", 48273); //TODO: use server address
            ClientAuthInfo info = new ClientAuthInfo(getUsername(), 0, getHWID(), protocol);
            os = new ObjectOutputStream(socket.getOutputStream());
            is = new ObjectInputStream(socket.getInputStream());

            handlePacket(is.readObject(), info.nextRequest);
            os.writeObject(info);
            os.flush();

            Object obj = handlePacket(is.readObject(), info.nextRequest);
            if (obj == null) { //if user is incorrect
                restart = true;
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            errored = true;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                FMLCommonHandler.instance().exitJava(43987, true);
            }
        }

        if (errored) {
            FMLCommonHandler.instance().exitJava(43986, true);
        } else if (restart) {
            FMLLog.log.info("restart");
            return downloadPepsiMod();
        }

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

    public static String setPassword(String newPassword) {
        Socket socket = null;
        ObjectInputStream is = null;
        ObjectOutputStream os = null;
        boolean reboot = false, errored = false;

        try {
            socket = new Socket("anarchy.daporkchop.net", 48273); //TODO: use server address
            ClientAuthInfo info = new ClientAuthInfo(getUsername(), 1, getHWID(), protocol);
            os = new ObjectOutputStream(socket.getOutputStream());
            is = new ObjectInputStream(socket.getInputStream());

            handlePacket(is.readObject(), info.nextRequest);
            os.writeObject(info);
            os.flush();

            Object obj = handlePacket(is.readObject(), info.nextRequest, newPassword);
            if (obj instanceof ClientChangePassword) {
                os.writeObject(obj);
                os.flush();
            } else {
                reboot = true;
                throw new IllegalStateException("take me back to the start plox");
            }
            handlePacket(is.readObject(), info.nextRequest);
        } catch (IOException | ClassNotFoundException | IllegalStateException e) {
            e.printStackTrace();
            errored = true;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                FMLCommonHandler.instance().exitJava(983752, true);
            }
        }

        if (errored) {
            FMLCommonHandler.instance().exitJava(43986, true);
        } else if (reboot) {
            newPassword = setPassword();
        }

        tag.setString("password", newPassword);
        tag.save();
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
}
