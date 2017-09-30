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


import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;

public class PacketDataInput extends InputStream implements DataInput {
    private final Packet packet;

    public PacketDataInput(Packet packet) {
        this.packet = packet;
    }

    public int read() throws IOException {
        return this.packet.remaining() <= 0 ? -1 : this.packet.readUnsignedByte();
    }

    public void readFully(byte[] b) throws IOException {
        for (int i = 0; i < b.length; ++i) {
            b[i] = this.packet.readByte();
        }

    }

    public void readFully(byte[] b, int off, int len) throws IOException {
        for (int i = off; i < len; ++i) {
            b[i] = this.packet.readByte();
        }

    }

    public int skipBytes(int n) throws IOException {
        int skipped;
        for (skipped = 0; skipped < n && this.packet.remaining() > 0; ++skipped) {
            this.packet.readByte();
        }

        return skipped;
    }

    public boolean readBoolean() throws IOException {
        return this.packet.readBoolean();
    }

    public byte readByte() throws IOException {
        return this.packet.readByte();
    }

    public int readUnsignedByte() throws IOException {
        return this.packet.readUnsignedByte();
    }

    public short readShort() throws IOException {
        return this.packet.readShort();
    }

    public int readUnsignedShort() throws IOException {
        return this.packet.readUnsignedShort();
    }

    public char readChar() throws IOException {
        return (char) this.packet.readUnsignedShort();
    }

    public int readInt() throws IOException {
        return this.packet.readInt();
    }

    public long readLong() throws IOException {
        return this.packet.readLong();
    }

    public float readFloat() throws IOException {
        return this.packet.readFloat();
    }

    public double readDouble() throws IOException {
        return this.packet.readDouble();
    }

    public String readLine() throws IOException {
        throw new RuntimeException("This method is not supported by " + this.getClass().getSimpleName());
    }

    public String readUTF() throws IOException {
        return this.packet.readString();
    }
}

