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

package team.pepsi.pepsimod.launcher.classloading;

import net.minecraftforge.fml.common.FMLLog;
import team.pepsi.pepsimod.launcher.LauncherMixinLoader;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class PepsiModClassLoader extends URLClassLoader {
    public final Map<String, byte[]> extraClassDefs;
    //public Method findClass = null;

    public PepsiModClassLoader(URL[] urls, ClassLoader parent, Map<String, byte[]> extraClassDefs) {
        super(urls, parent);
        this.extraClassDefs = new HashMap<>(extraClassDefs);
        /*Class<?> currentLoader = parent.getClass();
        while (findClass == null) {
            FMLLog.log.info("Trying class: " + currentLoader.getCanonicalName());
            try {
                findClass = currentLoader.getDeclaredMethod("findClass", String.class);
                FMLLog.log.info("Found method in " + currentLoader.getCanonicalName());
                findClass.setAccessible(true);
                FMLLog.log.info("Set method to accessible");
            } catch (NoSuchMethodException e) {
                currentLoader = currentLoader.getSuperclass();
            }
        }*/
        try {
            Class.forName("team.pepsi.pepsimod.launcher.resources.PepsiURLStreamHandler").newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Class<?> findClass(final String name) throws ClassNotFoundException {
        //FMLLog.log.info("findClass:" + name);
        byte[] classBytes = this.extraClassDefs.getOrDefault(name, null);
        if (classBytes != null) {
            FMLLog.log.info("[PepsiModClassLoader] loading class: " + name);
            return defineClass(name, classBytes, 0, classBytes.length);
        }
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            return LauncherMixinLoader.tryLoadingClassAsMainLoader(name);
        }
    }

    @Override
    public Class<?> loadClass(String var1) throws ClassNotFoundException {
        return this.loadClass(var1, false);
    }

    @Override
    public Class<?> loadClass(String var1, boolean var2) throws ClassNotFoundException {
        //FMLLog.log.info("loadClass:" + var1);
        try {
            return super.loadClass(var1, false);
        } catch (ClassNotFoundException e) {
        }

        return LauncherMixinLoader.tryLoadingClassAsMainLoader(var1);
    }

    /*@Override
    public InputStream getResourceAsStream(String name) {
        String original = name;
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        if (name.contains("pepsimod"))  {
            FMLLog.log.info(name);
        }
        if (PepsimodSent.INSTANCE.assets.containsKey(name)) {
            FMLLog.log.info("Stream: " + name);
            return new ByteArrayInputStream(PepsimodSent.INSTANCE.assets.get(name));
        }

        return super.getResourceAsStream(name);
    }

    @Override
    public URL getResource(String name) {
        String original = name;
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        if (PepsimodSent.INSTANCE.assets.containsKey(name)) {
            FMLLog.log.info("Resource: " + name);
            FMLLog.log.info("pepsi://" + name);
            try {
                return new URL("pepsi://" + name);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        //return getBootstrapResource(var1);
        return super.getResource(original);
    }*/
}
