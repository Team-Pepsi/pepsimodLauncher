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

package team.pepsi.pepsimod.launcher.resources;

import team.pepsi.pepsimod.launcher.util.PepsimodSent;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Hashtable;

public class PepsiURLStreamHandler extends URLStreamHandler {
    {
        try {
            Field f = URL.class.getDeclaredField("handlers");
            f.setAccessible(true);
            if (!((Hashtable<String, URLStreamHandler>) f.get(null)).containsKey("pepsi")) {
                ((Hashtable<String, URLStreamHandler>) f.get(null)).put("pepsi", this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public URLConnection openConnection(URL url) {
        return new PepsiURLConnection(PepsimodSent.INSTANCE.assets.get(url.toExternalForm().substring("pepsi://".length())), url);
    }
}
