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

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.launch.MixinBootstrap;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

public class LauncherTweaker implements ITweaker {
    public LauncherTweaker() {
        try {
            Method m = MixinBootstrap.class.getDeclaredMethod("start");
            m.setAccessible(true);
            m.invoke(null);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void acceptOptions(List<String> args, File gameDir, final File assetsDir, String profile) {
        try {
            Method m = MixinBootstrap.class.getDeclaredMethod("doInit", List.class);
            m.setAccessible(true);
            m.invoke(null, args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        classLoader.addClassLoaderExclusion("net.daporkchop.pepsimod.");
        classLoader.addClassLoaderExclusion("team.pepsi.pepsimod.");
        try {
            Method m = MixinBootstrap.class.getDeclaredMethod("injectIntoClassLoader", LaunchClassLoader.class);
            m.setAccessible(true);
            m.invoke(null, classLoader);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public String getLaunchTarget() {
        return "net.minecraft.client.main.Main";
    }

    public String[] getLaunchArguments() {
        return new String[0];
    }
}
