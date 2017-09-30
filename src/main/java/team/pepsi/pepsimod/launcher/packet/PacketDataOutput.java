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

package team.pepsi.pepsimod.launcher.packet;


import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

public class PacketDataOutput extends OutputStream implements DataOutput {
    private final Packet packet;

    public PacketDataOutput(Packet packet) {
        this.packet = packet;
    }

    public void write(int b) throws IOException {
        this.packet.writeByte(b);
    }

    public void write(byte[] b) throws IOException {
        this.packet.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = off; i < len; ++i) {
            this.packet.writeByte(b[i]);
        }

    }

    public void writeBoolean(boolean v) throws IOException {
        this.packet.writeBoolean(v);
    }

    public void writeByte(int v) throws IOException {
        this.packet.writeByte(v);
    }

    public void writeShort(int v) throws IOException {
        this.packet.writeShort(v);
    }

    public void writeChar(int v) throws IOException {
        this.packet.writeUnsignedShort(v);
    }

    public void writeInt(int v) throws IOException {
        this.packet.writeInt(v);
    }

    public void writeLong(long v) throws IOException {
        this.packet.writeLong(v);
    }

    public void writeFloat(float v) throws IOException {
        this.packet.writeFloat((double) v);
    }

    public void writeDouble(double v) throws IOException {
        this.packet.writeDouble(v);
    }

    public void writeBytes(String s) throws IOException {
        this.packet.write(s.getBytes());
    }

    public void writeChars(String s) throws IOException {
        char[] var2 = s.toCharArray();
        int var3 = var2.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            char c = var2[var4];
            this.packet.writeUnsignedShort(c);
        }

    }

    public void writeUTF(String s) throws IOException {
        this.packet.writeString(s);
    }
}
