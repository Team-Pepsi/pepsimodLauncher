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

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import sun.misc.Unsafe;
import team.pepsi.pepsimod.launcher.classloading.PepsiModClassLoader;
import team.pepsi.pepsimod.launcher.resources.PepsiResourceAdder;
import team.pepsi.pepsimod.launcher.util.PepsimodSent;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

public class LauncherMixinLoader implements IFMLLoadingPlugin {
    public static boolean isObfuscatedEnvironment = false;
    public static PepsiModClassLoader classLoader;
    public static ArrayList<String> loadingClasses = new ArrayList<>();
    public static JDialog dialog;
    public static JLabel label = new JLabel("....................................................................................................................");
    public Object coremod;

    public LauncherMixinLoader() {
        JFrame f= new JFrame();
        dialog = new JDialog(f , "pepsimod", true);
        dialog.setModal(false);
        dialog.setLayout(new FlowLayout());
        dialog.add(label);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        try {
            PepsiModServerManager.downloadPepsiMod();

            classLoader = new PepsiModClassLoader(new URL[0], null, PepsimodSent.INSTANCE.classes);

            Field parent = ClassLoader.class.getDeclaredField("parent");
            long offset = getUnsafe().objectFieldOffset(parent);
            getUnsafe().putObject(getClass().getClassLoader().getParent(), offset, classLoader);
            getUnsafe().putObject(Launch.classLoader, offset, new PepsiResourceAdder());

            Field resourceCache = LaunchClassLoader.class.getDeclaredField("resourceCache");
            resourceCache.setAccessible(true);
            Map<String, byte[]> classCache = (Map<String, byte[]>) resourceCache.get(Launch.classLoader);
            FMLLog.log.info("Initial size: " + classCache.size());
            for (Map.Entry<String, byte[]> entry : PepsimodSent.INSTANCE.classes.entrySet()) {
                classCache.put(entry.getKey(), entry.getValue());
            }
            FMLLog.log.info("Size after: " + ((Map<String, byte[]>) resourceCache.get(Launch.classLoader)).size());

            FMLLog.log.info("ClassLoader heirachy:");
            ClassLoader loader = Launch.classLoader;
            while (getParent(loader) != null) {
                FMLLog.log.info(loader.getClass().getCanonicalName());
                loader = getParent(loader);
            }
            FMLLog.log.info(loader.getClass().getCanonicalName());
            coremod = Class.forName("net.daporkchop.pepsimod.PepsiModMixinLoader").newInstance();
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("FATAL ERROR IN PEPSIMOD LAUNCHER, SYSTEM WILL EXIT NOW!!!");
            Runtime.getRuntime().exit(0);
        }
        dialog.setVisible(false);
        dialog.dispose();
        dialog = null;
    }

    public static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (Exception e) {
            e.printStackTrace();
            Runtime.getRuntime().exit(928273);
        }

        return null;
    }

    public static Class<?> tryLoadingClassAsMainLoader(String name) throws ClassNotFoundException {
        if (loadingClasses.contains(name)) {
            throw new ClassNotFoundException("CLASS NOT LOADED ON SECOND ITERATION! " + name);
        }
        loadingClasses.add(name);
        Class<?> toReturn = null;
        toReturn = Launch.classLoader.loadClass(name);
        if (toReturn == null) {
            FMLLog.log.info("Unable to load class " + name + " with Launch.classLoader");
            toReturn = LauncherMixinLoader.class.getClassLoader().loadClass(name);
        }
        loadingClasses.remove(name);
        if (toReturn == null) {
            FMLLog.log.info("Failed to load class " + name);
            throw new ClassNotFoundException("unable to load class");
        }
        return toReturn;
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{"team.pepsi.pepsimod.launcher.ClassLoadingNotifier"};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        isObfuscatedEnvironment = (boolean) (Boolean) data.get("runtimeDeobfuscationEnabled");
        try {
            Method m = Class.forName("net.daporkchop.pepsimod.PepsiModMixinLoader").getDeclaredMethod("injectData", Map.class);
            m.invoke(coremod, data);
        } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException | NoSuchMethodException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    @Override
    public String getAccessTransformerClass() {
        try {
            Method m = Class.forName("net.daporkchop.pepsimod.PepsiModMixinLoader").getDeclaredMethod("getAccessTransformerClass");
            m.invoke(coremod);
        } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException | NoSuchMethodException e) {
            throw new IllegalStateException(e.getCause());
        }
        return "";
    }

    public ClassLoader getParent(ClassLoader loader) {
        if (loader instanceof LaunchClassLoader) {
            try {
                Field f = LaunchClassLoader.class.getDeclaredField("parent");
                f.setAccessible(true);
                return (ClassLoader) f.get(loader);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return loader.getParent();
    }
}
