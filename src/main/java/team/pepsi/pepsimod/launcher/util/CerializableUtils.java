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

package team.pepsi.pepsimod.launcher.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * like serializable but better
 */
public class CerializableUtils {
    public static Object fromBytes(byte[] bytes) throws IllegalStateException {
        ByteArrayInputStream bis = null;
        ObjectInputStream in = null;
        Object toReturn = null;
        boolean errored = false;

        try {
            bis = new ByteArrayInputStream(bytes);
            in = new ObjectInputStream(bis);
            toReturn = in.readObject();
        } catch (ClassNotFoundException | IOException var13) {
            var13.printStackTrace();
            errored = true;
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }

                if (in != null) {
                    in.close();
                }
            } catch (IOException var12) {
            }

        }

        if (errored) {
            throw new IllegalStateException();
        }

        return toReturn;
    }
}
