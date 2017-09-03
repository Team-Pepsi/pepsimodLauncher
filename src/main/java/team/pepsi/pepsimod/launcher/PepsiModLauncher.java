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

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Mod(name = "pepsimod", modid = "pepsimod", version = "0.1")
public class PepsiModLauncher {
    public static Object pepsimodInstance;
    public static Logger logger;

    public PepsiModLauncher() {
        if (LauncherMixinLoader.isObfuscatedEnvironment) {
            try {
                pepsimodInstance = Class.forName("net.daporkchop.pepsimod.PepsiMod").newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                //can't happen
                throw new IllegalStateException(e.getCause());
            }
        }
    }

    @Mod.EventHandler
    public void construct(FMLConstructionEvent event) {
        System.out.println("FMLConstructionEvent");
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        logger.info("FMLPreInitializationEvent");
        if (pepsimodInstance != null) {
            try {
                Method m = Class.forName("net.daporkchop.pepsimod.PepsiMod").getDeclaredMethod("preInit", FMLPreInitializationEvent.class);
                m.invoke(pepsimodInstance, event);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                //can't happen
                throw new IllegalStateException(e.getCause());
            }
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("FMLInitializationEvent");
        if (pepsimodInstance != null) {
            try {
                Method m = Class.forName("net.daporkchop.pepsimod.PepsiMod").getDeclaredMethod("init", FMLInitializationEvent.class);
                m.invoke(pepsimodInstance, event);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                //can't happen
                throw new IllegalStateException(e.getCause());
            }
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        logger.info("FMLPostInitializationEvent");
        if (pepsimodInstance != null) {
            try {
                Method m = Class.forName("net.daporkchop.pepsimod.PepsiMod").getDeclaredMethod("postInit", FMLPostInitializationEvent.class);
                m.invoke(pepsimodInstance, event);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                //can't happen
                throw new IllegalStateException(e.getCause());
            }
        }
    }
}
