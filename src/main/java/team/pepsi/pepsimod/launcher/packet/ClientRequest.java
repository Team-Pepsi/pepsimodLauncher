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

public class ClientRequest extends Packet {
    public String username;
    public String hwid;
    public String version;
    public String password;
    public int protocol;
    public int nextRequest;

    public ClientRequest() {
        super(0);
    }

    public ClientRequest(ByteBuf packet) {
        super(packet);
    }

    @Override
    public void encode() {
        this.writeString(username);
        this.writeString(hwid);
        this.writeString(version);
        this.writeString(password);
        this.writeInt(protocol);
        this.writeInt(nextRequest);
    }

    @Override
    public void decode() {
        username = readString();
        hwid = readString();
        version = readString();
        password = readString();
        protocol = readInt();
        nextRequest = readInt();
    }
}
