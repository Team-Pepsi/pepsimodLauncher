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
import team.pepsi.pepsimod.common.ClientAuthInfo;
import team.pepsi.pepsimod.common.ClientChangePassword;
import team.pepsi.pepsimod.common.ServerLoginErrorMessage;
import team.pepsi.pepsimod.common.ServerPepsiModSending;
import team.pepsi.pepsimod.common.util.CryptUtils;
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

        int res = JOptionPane.showConfirmDialog(null, array, "Login",
                JOptionPane.OK_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        tag.setString("username", login.getText());
        tag.setString("password", password.getText());
        tag.save();
    }

    private static Object handlePacket(Object obj, int state, Object... arguments) throws IllegalStateException {
        FMLLog.log.info(obj.getClass().getCanonicalName());
        if (obj instanceof ServerLoginErrorMessage) {
            ServerLoginErrorMessage pck = (ServerLoginErrorMessage) obj;
            FMLLog.log.info(pck.error);
            if (pck.error.startsWith("notAnError")) {
                return new ClientChangePassword((String) arguments[0]);
            }
            JOptionPane.showMessageDialog(null, pck.error, "pepsimod error", JOptionPane.OK_OPTION);
            if (pck.error.toLowerCase().startsWith("invalid credentials")) {
                promptForCredentials();
                return null;
            } else {
                Runtime.getRuntime().exit(2073);
                FMLCommonHandler.instance().exitJava(23098, true);
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
            } catch (IllegalStateException e) {
                JOptionPane.showMessageDialog(null, "Invalid password!", "pepsimod error", JOptionPane.OK_OPTION);
                promptForCredentials();
                return null;
            }
            HashMap<String, byte[]> classes = (HashMap<String, byte[]>) decrypted;
            currentState = pck.assets;
            currentState = Zlib.inflate(currentState);
            try {
                //if an IllegalStateException is thrown, then we can assume that the password is incorrect (as it's used for decryption)
                currentState = CryptUtils.decrypt(currentState, getPassword());
                if (currentState == null) {
                    throw new IllegalStateException();
                }
                decrypted = CerializableUtils.fromBytes(currentState);
            } catch (IllegalStateException e) {
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
            socket = new Socket("127.0.0.1", 48273); //TODO: use server address
            ClientAuthInfo info = new ClientAuthInfo(getUsername(), 0, getHWID());
            os = new ObjectOutputStream(socket.getOutputStream());
            is = new ObjectInputStream(socket.getInputStream());
            //FMLLog.log.info(getUsername() + " " + getHWID());
            os.writeObject(info);
            os.flush();
            Object obj = handlePacket(is.readObject(), info.nextRequest);
            if (obj == null) {
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

    public static String changePassword(String newPassword) {
        Socket socket = null;
        ObjectInputStream is = null;
        ObjectOutputStream os = null;
        boolean reboot = false, errored = false;
        try {
            socket = new Socket("127.0.0.1", 48273); //TODO: use server address
            ClientAuthInfo info = new ClientAuthInfo(getUsername(), 1, getHWID());
            os = new ObjectOutputStream(socket.getOutputStream());
            is = new ObjectInputStream(socket.getInputStream());
            os.writeObject(info);
            os.flush();
            Object obj = is.readObject();
            obj = handlePacket(obj, info.nextRequest, newPassword);
            if (obj instanceof ClientChangePassword) {
                os.writeObject(obj);
                os.flush();
            } else {
                reboot = true;
            }
            os.writeObject(new ClientChangePassword(newPassword));
            os.flush();
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
            newPassword = changePassword(newPassword);
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
}
