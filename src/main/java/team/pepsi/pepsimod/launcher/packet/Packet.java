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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class Packet {
    public ByteBuf buffer;
    private short id;
    private PacketDataInput input;
    private PacketDataOutput output;

    public Packet(int id) {
        this.buffer = Unpooled.buffer();
        if (id >= 0 && id <= 255) {
            this.writeUnsignedByte(this.id = (short) id);
        } else {
            throw new IllegalArgumentException("Invalid ID, must be in between 0-255");
        }
    }

    public Packet(ByteBuf buffer) {
        buffer.resetReaderIndex();
        this.buffer = Unpooled.copiedBuffer(buffer);
        this.input = new PacketDataInput(this);
        this.output = new PacketDataOutput(this);
        if (this.remaining() < 1) {
            throw new IllegalArgumentException("The packet contains no data, it has no ID to be read");
        } else {
            this.id = this.readUnsignedByte();
        }
    }

    public Packet(byte[] data) {
        this(Unpooled.copiedBuffer(data));
    }

    public final short getId() {
        return this.id;
    }

    public void encode() {
    }

    public void decode() {
    }

    public void setBuffer(byte[] buffer, boolean updateId) {
        this.setBuffer(buffer);
        if (updateId) {
            this.id = this.readUnsignedByte();
        }

    }

    public Packet flip() {
        byte[] data = this.buffer.array();
        this.buffer = Unpooled.copiedBuffer(data);
        this.id = this.readUnsignedByte();
        return this;
    }

    public Packet read(byte[] dest) {
        for (int i = 0; i < dest.length; ++i) {
            dest[i] = this.buffer.readByte();
        }

        return this;
    }

    public byte[] read(int length) {
        byte[] data = new byte[length];

        for (int i = 0; i < data.length; ++i) {
            data[i] = this.buffer.readByte();
        }

        return data;
    }

    public byte readByte() {
        return this.buffer.readByte();
    }

    public short readUnsignedByte() {
        return (short) (this.buffer.readByte() & 255);
    }

    private byte readCFUByte() {
        return (byte) (~this.buffer.readByte() & 255);
    }

    private byte[] readCFU(int length) {
        byte[] data = new byte[length];

        for (int i = 0; i < data.length; ++i) {
            data[0] = this.readCFUByte();
        }

        return data;
    }

    public boolean readBoolean() {
        return this.readUnsignedByte() > 0;
    }

    public short readShort() {
        return this.buffer.readShort();
    }

    public short readShortLE() {
        return this.buffer.readShortLE();
    }

    public int readUnsignedShort() {
        return this.buffer.readShort() & '\uffff';
    }

    public int readUnsignedShortLE() {
        return this.buffer.readShortLE() & '\uffff';
    }

    public int readTriadLE() {
        return this.buffer.readByte() & 255 | (this.buffer.readByte() & 255) << 8 | (this.buffer.readByte() & 15) << 16;
    }

    public int readInt() {
        return this.buffer.readInt();
    }

    public int readIntLE() {
        return this.buffer.readIntLE();
    }

    public long readUnsignedInt() {
        return (long) this.buffer.readInt() & 4294967295L;
    }

    public long readUnsignedIntLE() {
        return (long) this.buffer.readIntLE() & 4294967295L;
    }

    public long readLong() {
        return this.buffer.readLong();
    }

    public long readLongLE() {
        return this.buffer.readLongLE();
    }

    public BigInteger readUnsignedLong() {
        byte[] ulBytes = this.read(8);
        return new BigInteger(ulBytes);
    }

    public BigInteger readUnsignedLongLE() {
        byte[] ulBytesReversed = this.read(8);
        byte[] ulBytes = new byte[ulBytesReversed.length];

        for (int i = 0; i < ulBytes.length; ++i) {
            ulBytes[i] = ulBytesReversed[ulBytesReversed.length - i - 1];
        }

        return new BigInteger(ulBytes);
    }

    public float readFloat() {
        return this.buffer.readFloat();
    }

    public double readDouble() {
        return this.buffer.readDouble();
    }

    public String readString() {
        int len = this.readUnsignedShort();
        byte[] data = this.read(len);
        return new String(data);
    }

    public String readStringLE() {
        int len = this.readUnsignedShortLE();
        byte[] data = this.read(len);
        return new String(data);
    }

    public InetSocketAddress readAddress() throws UnknownHostException {
        short version = this.readUnsignedByte();
        byte[] addressBytes;
        int port;
        if (version == 4) {
            addressBytes = this.readCFU(4);
            port = this.readUnsignedShort();
            return new InetSocketAddress(InetAddress.getByAddress(addressBytes), port);
        } else if (version == 6) {
            addressBytes = this.readCFU(16);
            this.read(10);
            port = this.readUnsignedShort();
            return new InetSocketAddress(InetAddress.getByAddress(Arrays.copyOfRange(addressBytes, 0, 16)), port);
        } else {
            throw new UnknownHostException("Unknown protocol IPv" + version);
        }
    }

    public Packet write(byte[] data) {
        for (int i = 0; i < data.length; ++i) {
            this.buffer.writeByte(data[i]);
        }

        return this;
    }

    public Packet pad(int length) {
        for (int i = 0; i < length; ++i) {
            this.buffer.writeByte(0);
        }

        return this;
    }

    public Packet writeByte(int b) {
        this.buffer.writeByte((byte) b);
        return this;
    }

    public Packet writeUnsignedByte(int b) {
        this.buffer.writeByte((byte) b & 255);
        return this;
    }

    private Packet writeCFUByte(byte b) {
        this.buffer.writeByte(~b & 255);
        return this;
    }

    private Packet writeCFU(byte[] data) {
        for (int i = 0; i < data.length; ++i) {
            this.writeCFUByte(data[i]);
        }

        return this;
    }

    public Packet writeBoolean(boolean b) {
        this.buffer.writeByte(b ? 1 : 0);
        return this;
    }

    public Packet writeShort(int s) {
        this.buffer.writeShort(s);
        return this;
    }

    public Packet writeShortLE(int s) {
        this.buffer.writeShortLE(s);
        return this;
    }

    public Packet writeUnsignedShort(int s) {
        this.buffer.writeShort((short) s & '\uffff');
        return this;
    }

    public Packet writeUnsignedShortLE(int s) {
        this.buffer.writeShortLE((short) s & '\uffff');
        return this;
    }

    public Packet writeTriadLE(int t) {
        this.buffer.writeByte((byte) (t & 255));
        this.buffer.writeByte((byte) (t >> 8 & 255));
        this.buffer.writeByte((byte) (t >> 16 & 255));
        return this;
    }

    public Packet writeInt(int i) {
        this.buffer.writeInt(i);
        return this;
    }

    public Packet writeUnsignedInt(long i) {
        this.buffer.writeInt((int) i & -1);
        return this;
    }

    public Packet writeIntLE(int i) {
        this.buffer.writeIntLE(i);
        return this;
    }

    public Packet writeUnsignedIntLE(long i) {
        this.buffer.writeIntLE((int) i & -1);
        return this;
    }

    public Packet writeLong(long l) {
        this.buffer.writeLong(l);
        return this;
    }

    public Packet writeLongLE(long l) {
        this.buffer.writeLongLE(l);
        return this;
    }

    public Packet writeUnsignedLong(BigInteger bi) {
        byte[] ulBytes = bi.toByteArray();
        if (ulBytes.length > 8) {
            throw new IllegalArgumentException("BigInteger is too big to fit into a long");
        } else {
            int i;
            for (i = 0; i < 8 - ulBytes.length; ++i) {
                this.writeByte(0);
            }

            for (i = 0; i < ulBytes.length; ++i) {
                this.writeByte(ulBytes[i]);
            }

            return this;
        }
    }

    public Packet writeUnsignedLong(long l) {
        return this.writeUnsignedLong(new BigInteger(Long.toString(l)));
    }

    public Packet writeUnsignedLongLE(BigInteger bi) {
        byte[] ulBytes = bi.toByteArray();
        if (ulBytes.length > 8) {
            throw new IllegalArgumentException("BigInteger is too big to fit into a long");
        } else {
            int i;
            for (i = ulBytes.length - 1; i >= 0; --i) {
                this.writeByte(ulBytes[i]);
            }

            for (i = 0; i < 8 - ulBytes.length; ++i) {
                this.writeByte(0);
            }

            return this;
        }
    }

    public Packet writeUnsignedLongLE(long l) {
        return this.writeUnsignedLongLE(new BigInteger(Long.toString(l)));
    }

    public Packet writeFloat(double f) {
        this.buffer.writeFloat((float) f);
        return this;
    }

    public Packet writeDouble(double d) {
        this.buffer.writeDouble(d);
        return this;
    }

    public Packet writeString(String s) {
        byte[] data = s.getBytes();
        this.writeUnsignedShort(data.length);
        this.write(data);
        return this;
    }

    public Packet writeStringLE(String s) {
        byte[] data = s.getBytes();
        this.writeUnsignedShortLE(data.length);
        this.write(data);
        return this;
    }

    public Packet writeAddress(InetSocketAddress address) throws UnknownHostException {
        byte[] addressBytes = address.getAddress().getAddress();
        if (addressBytes.length == 4) {
            this.writeUnsignedByte(4);
            this.writeCFU(addressBytes);
            this.writeUnsignedShort(address.getPort());
        } else {
            if (addressBytes.length != 16) {
                throw new UnknownHostException("Unknown protocol IPv" + addressBytes.length);
            }

            this.writeUnsignedByte(6);
            this.writeCFU(addressBytes);
            this.pad(10);
            this.writeUnsignedShort(address.getPort());
        }

        return this;
    }

    public Packet writeAddress(InetAddress address, int port) throws UnknownHostException {
        return this.writeAddress(new InetSocketAddress(address, port));
    }

    public Packet writeAddress(String address, int port) throws UnknownHostException {
        return this.writeAddress(new InetSocketAddress(address, port));
    }

    public byte[] array() {
        return this.buffer.isDirect() ? null : Arrays.copyOfRange(this.buffer.array(), 0, this.buffer.writerIndex());
    }

    public int size() {
        return this.array().length;
    }

    public ByteBuf buffer() {
        return this.buffer.retain();
    }

    public PacketDataInput getDataInput() {
        return this.input;
    }

    public PacketDataOutput getDataOutput() {
        return this.output;
    }

    public int remaining() {
        return this.buffer.readableBytes();
    }

    public final Packet setBuffer(byte[] buffer) {
        this.buffer = Unpooled.copiedBuffer(buffer);
        return this;
    }

    public Packet clear() {
        this.buffer.clear();
        return this;
    }
}
