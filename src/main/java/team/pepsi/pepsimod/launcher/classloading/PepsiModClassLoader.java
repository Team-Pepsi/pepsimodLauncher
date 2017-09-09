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
    public String[] strings;
    public byte[][] bytes;
    //public Method findClass = null;

    public PepsiModClassLoader(URL[] urls, ClassLoader parent, Map<String, byte[]> extraClassDefs) {
        super(urls, parent);
        this.extraClassDefs = new HashMap<>(extraClassDefs);
        try {
            Class.forName("team.pepsi.pepsimod.launcher.resources.PepsiURLStreamHandler").newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        strings = new String[extraClassDefs.entrySet().size()];
        bytes = new byte[extraClassDefs.entrySet().size()][];
        int i = 0;
        for (Map.Entry<String, byte[]> entry : extraClassDefs.entrySet()) {
            strings[i] = entry.getKey();
            bytes[i] = entry.getValue();
            i++;
        }
    }

    @Override
    public Class<?> findClass(final String name) throws ClassNotFoundException {
        if (canLoadClass(name)) {
            byte[] classBytes = getClass(name);
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
        try {
            return super.loadClass(var1, var2);
        } catch (ClassNotFoundException e) {
        }

        return LauncherMixinLoader.tryLoadingClassAsMainLoader(var1);
    }

    public boolean canLoadClass(String name) {
        for (String s : strings) {
            if (s.equals(name)) {
                return true;
            }
        }

        return false;
    }

    public byte[] getClass(String name) {
        for (int i = 0; i < strings.length; i++) {
            String s = strings[i];
            if (s.equals(name)) {
                return bytes[i];
            }
        }

        return null;
    }
}
