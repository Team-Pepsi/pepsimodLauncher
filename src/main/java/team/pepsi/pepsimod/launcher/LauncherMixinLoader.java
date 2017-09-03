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

import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import sun.misc.Unsafe;
import team.pepsi.pepsimod.common.util.Zlib;
import team.pepsi.pepsimod.launcher.classloading.PepsiModClassLoader;
import team.pepsi.pepsimod.launcher.util.PepsimodSent;

import javax.annotation.Nullable;
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
    public Object coremod;

    public LauncherMixinLoader() {
        try {
            PepsiModServerManager.downloadPepsiMod();
            classLoader = new PepsiModClassLoader(new URL[0], getClass().getClassLoader(), PepsimodSent.INSTANCE.classes);
            Field parent = ClassLoader.class.getDeclaredField("parent");
            long offset = getUnsafe().objectFieldOffset(parent);
            getUnsafe().putObject(getClass().getClassLoader(), offset, classLoader);
            Field resourceCache = LaunchClassLoader.class.getDeclaredField("resourceCache");
            resourceCache.setAccessible(true);
            Map<String, byte[]> classCache = (Map<String, byte[]>) resourceCache.get(Launch.classLoader);
            FMLLog.log.info("Initial size: " + classCache.size());
            for (Map.Entry<String, byte[]> entry : PepsimodSent.INSTANCE.classes.entrySet()) {
                classCache.put(entry.getKey(), Zlib.inflate(entry.getValue()));
            }
            FMLLog.log.info("Size after: " + ((Map<String, byte[]>) resourceCache.get(Launch.classLoader)).size());
            coremod = Class.forName("net.daporkchop.pepsimod.PepsiModMixinLoader").newInstance();
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("FATAL ERROR IN PEPSIMOD LAUNCHER, SYSTEM WILL EXIT NOW!!!");
            Runtime.getRuntime().exit(0);
        }
        System.out.println("\n\n\nPepsiMod Mixin init\n\n");
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.pepsimod.json");

        for (Method m : ClientBrandRetriever.class.getDeclaredMethods()) {
            System.out.println(m.getName() + " " + m.toString());
        }

        if (hasForge()) {
            MixinEnvironment.getDefaultEnvironment().setObfuscationContext("searge");
            FMLLog.log.info("Forge found!");
        }

        System.out.println(MixinEnvironment.getDefaultEnvironment().getObfuscationContext());
    }

    public static Unsafe getUnsafe() {
        try {
            FMLLog.log.info("Getting field");
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            FMLLog.log.info("Setting field to be accessible");
            f.setAccessible(true);
            FMLLog.log.info("Getting value");
            return (Unsafe) f.get(null);
        } catch (Exception e) {
            e.printStackTrace();
            Runtime.getRuntime().exit(928273);
        }

        return null;
    }

    public static Class<?> tryLoadingClassAsMainLoader(String name) throws ClassNotFoundException {
        if (loadingClasses.contains(name)) {
            throw new ClassNotFoundException("CLASS NOT FOUND ON SECOND ITERATION! " + name);
        }
        loadingClasses.add(name);
        Class<?> toReturn = null;
        toReturn = Launch.classLoader.loadClass(name);
        if (toReturn == null) {
            FMLLog.log.info("Unable to load class " + name + " with Launch.classLoader");
            toReturn = LauncherMixinLoader.class.getClassLoader().loadClass(name);
        }
        if (toReturn == null) {
            FMLLog.log.info("Failed to load class " + name);
            throw new ClassNotFoundException("unable to find class");
        }
        loadingClasses.remove(name);
        return toReturn;
    }

    private boolean hasForge() {
        for (IClassTransformer transformer : Launch.classLoader.getTransformers()) {
            if (transformer.getClass().getName().contains("fml")) {
                return true;
            }
        }
        return false;
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
}
