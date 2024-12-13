package eu.smoothcloud.chain.network.packet;

import io.netty5.buffer.Buffer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public abstract class SmoothPacket {

    private int protocolVersion;
    private int packetType;

    /**
     * Liest ein VarInt aus dem Buffer.
     *
     * @param buffer Der Buffer, aus dem gelesen wird.
     * @return Der dekodierte VarInt-Wert.
     */
    public static int readVarInt(Buffer buffer) {
        int value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = buffer.readByte();
            value |= (currentByte & 0x7F) << (position++ * 7);

            if (position > 5) {
                throw new IllegalArgumentException("VarInt ist zu groß!");
            }
            if ((currentByte & 0x80) == 0) {
                break;
            }
        }
        return value;
    }

    /**
     * Schreibt ein VarInt in den Buffer.
     *
     * @param value  Der zu schreibende Wert.
     * @param buffer Der Ziel-Buffer.
     */
    public static void writeVarInt(int value, Buffer buffer) {
        while ((value & 0xFFFFFF80) != 0L) {
            buffer.writeByte((byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        buffer.writeByte((byte) (value & 0x7F));
    }

    /**
     * Liest einen String aus dem Buffer.
     *
     * @param buffer Der Buffer, aus dem gelesen wird.
     * @return Der gelesene String.
     */
    public static String readString(Buffer buffer) {
        int length = readVarInt(buffer);
        byte[] bytes = new byte[length];
        buffer.readBytes(ByteBuffer.wrap(bytes));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Schreibt einen String in den Buffer.
     *
     * @param value  Der zu schreibende String.
     * @param buffer Der Ziel-Buffer.
     */
    public static void writeString(String value, Buffer buffer) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(bytes.length, buffer);
        buffer.writeBytes(bytes);
    }

    /**
     * Liest ein Byte-Array aus dem Buffer.
     *
     * @param buffer Der Buffer, aus dem gelesen wird.
     * @return Das gelesene Byte-Array.
     */
    public static byte[] readArray(Buffer buffer) {
        int length = readVarInt(buffer);
        byte[] array = new byte[length];
        buffer.readBytes(ByteBuffer.wrap(array));
        return array;
    }

    /**
     * Schreibt ein Byte-Array in den Buffer.
     *
     * @param array  Das zu schreibende Byte-Array.
     * @param buffer Der Ziel-Buffer.
     */
    public static void writeArray(byte[] array, Buffer buffer) {
        writeVarInt(array.length, buffer);
        buffer.writeBytes(array);
    }

    /**
     * Liest eine Liste von Strings aus dem Buffer.
     *
     * @param buffer Der Buffer, aus dem gelesen wird.
     * @return Die gelesene Liste von Strings.
     */
    public static List<String> readStringArray(Buffer buffer) {
        int length = readVarInt(buffer);
        List<String> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            list.add(readString(buffer));
        }
        return list;
    }

    /**
     * Schreibt eine Liste von Strings in den Buffer.
     *
     * @param list   Die zu schreibende Liste von Strings.
     * @param buffer Der Ziel-Buffer.
     */
    public static void writeStringArray(List<String> list, Buffer buffer) {
        writeVarInt(list.size(), buffer);
        for (String str : list) {
            writeString(str, buffer);
        }
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public int getPacketType() {
        return packetType;
    }

    public void setPacketType(int packetType) {
        this.packetType = packetType;
    }

    /**
     * Methode zum Lesen eines Pakets.
     *
     * @param buffer Der Buffer, aus dem das Paket gelesen wird.
     */
    public abstract void read(Buffer buffer);

    /**
     * Methode zum Schreiben eines Pakets.
     *
     * @param buffer Der Buffer, in den das Paket geschrieben wird.
     */
    public abstract void write(Buffer buffer);

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();
}