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

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class DataTag implements Serializable {
    public static final File USER_FOLDER = new File(System.getProperty("user.dir"));
    public static final File HOME_FOLDER = new File(System.getProperty("user.home"));
    private static final long serialVersionUID = 1L;
    private final File file;
    public HashMap<String, Serializable> objs;
    private HashMap<String, Integer> ints;
    private HashMap<String, String> strings;
    private HashMap<String, Boolean> booleans;
    private HashMap<String, Byte> bytes;
    private HashMap<String, Float> floats;
    private HashMap<String, Short> shorts;
    private HashMap<String, Double> doubles;
    private HashMap<String, Long> longs;
    private HashMap<String, DataTag> tags;
    private HashMap<String, int[]> intArrays;
    private HashMap<String, String[]> stringArrays;
    private HashMap<String, boolean[]> booleanArrays;
    private HashMap<String, byte[]> byteArrays;
    private HashMap<String, float[]> floatArrays;
    private HashMap<String, short[]> shortArrays;
    private HashMap<String, double[]> doubleArrays;
    private HashMap<String, long[]> longArrays;
    private HashMap<String, Serializable[]> objArrays;

    public DataTag(File saveTo) {
        file = saveTo;
        set();
        init();
    }

    public DataTag(DataTag tag) {
        FileHelper.createFile(new File(tag.file.getParentFile(), tag.file.getName().replaceAll(".dat", "")).toString(), true);
        file = new File(tag.file.getParentFile(), tag.file.getName().replaceAll(".dat", "") + "/" + tag.file.getName().replaceAll(".dat", "") + " tag - " + tag.tags.size() + ".dat");
        set();
        init();
    }

    private void set() {
        ints = new HashMap<String, Integer>();
        strings = new HashMap<String, String>();
        booleans = new HashMap<String, Boolean>();
        bytes = new HashMap<String, Byte>();
        floats = new HashMap<String, Float>();
        shorts = new HashMap<String, Short>();
        doubles = new HashMap<String, Double>();
        longs = new HashMap<String, Long>();
        tags = new HashMap<String, DataTag>();
        objs = new HashMap<String, Serializable>();
        intArrays = new HashMap<String, int[]>();
        stringArrays = new HashMap<String, String[]>();
        booleanArrays = new HashMap<String, boolean[]>();
        byteArrays = new HashMap<String, byte[]>();
        floatArrays = new HashMap<String, float[]>();
        shortArrays = new HashMap<String, short[]>();
        doubleArrays = new HashMap<String, double[]>();
        longArrays = new HashMap<String, long[]>();
        objArrays = new HashMap<String, Serializable[]>();
    }

    public void init() {
        FileHelper.createFile(file.getPath());
        load();
    }

    private void check(String name) {
        if (name == null)
            throw new IllegalArgumentException("Name Cannot be Null!", new NullPointerException());
    }

    public int setInteger(String name, int value) {
        check(name);
        ints.put(name, value);
        return value;
    }

    public String setString(String name, String value) {
        check(name);
        strings.put(name, value);
        return value;
    }

    public boolean setBoolean(String name, boolean value) {
        check(name);
        booleans.put(name, value);
        return value;
    }

    public byte setByte(String name, byte value) {
        check(name);
        bytes.put(name, value);
        return value;
    }

    public float setFloat(String name, float value) {
        check(name);
        floats.put(name, value);
        return value;
    }

    public short setShort(String name, short value) {
        check(name);
        shorts.put(name, value);
        return value;
    }

    public double setDouble(String name, double value) {
        check(name);
        doubles.put(name, value);
        return value;
    }

    public long setLong(String name, long value) {
        check(name);
        longs.put(name, value);
        return value;
    }

    public DataTag setTag(String name, DataTag value) {
        check(name);
        tags.put(name, value);
        return value;
    }

    public Serializable setSerializable(String name, Serializable obj) {
        check(name);
        if (obj == null) {
            objs.remove(name);
        } else {
            objs.put(name, obj);
        }
        return obj;
    }

    public int[] setIntegerArray(String name, int[] value) {
        check(name);
        intArrays.put(name, value);
        return value;
    }

    public String[] setStringArray(String name, String[] value) {
        check(name);
        stringArrays.put(name, value);
        return value;
    }

    public boolean[] setBooleanArray(String name, boolean[] value) {
        check(name);
        booleanArrays.put(name, value);
        return value;
    }

    public byte[] setByteArray(String name, byte[] value) {
        check(name);
        byteArrays.put(name, value);
        return value;
    }

    public float[] setFloatArray(String name, float[] value) {
        check(name);
        floatArrays.put(name, value);
        return value;
    }

    public short[] setShortArray(String name, short[] value) {
        check(name);
        shortArrays.put(name, value);
        return value;
    }

    public double[] setDoubleArray(String name, double[] value) {
        check(name);
        doubleArrays.put(name, value);
        return value;
    }

    public long[] setLongArray(String name, long[] value) {
        check(name);
        longArrays.put(name, value);
        return value;
    }

    public Serializable[] setSerializableArray(String name, Serializable[] value) {
        check(name);
        objArrays.put(name, value);
        return value;
    }

    public int getInteger(String name, int def) {
        return ints.containsKey(name) ? ints.get(name) : this.setInteger(name, def);
    }

    public String getString(String name, String def) {
        return strings.containsKey(name) ? strings.get(name) : this.setString(name, def);
    }

    public boolean getBoolean(String name, boolean def) {
        return booleans.containsKey(name) ? booleans.get(name) : this.setBoolean(name, def);
    }

    public byte getByte(String name, byte def) {
        return bytes.containsKey(name) ? bytes.get(name) : this.setByte(name, def);
    }

    public float getFloat(String name, float def) {
        return floats.containsKey(name) ? floats.get(name) : this.setFloat(name, def);
    }

    public short getShort(String name, short def) {
        return shorts.containsKey(name) ? shorts.get(name) : this.setShort(name, def);
    }

    public double getDouble(String name, double def) {
        return doubles.containsKey(name) ? doubles.get(name) : this.setDouble(name, def);
    }

    public long getLong(String name, long def) {
        return longs.containsKey(name) ? longs.get(name) : this.setLong(name, def);
    }

    public DataTag getTag(String name, DataTag def) {
        return tags.containsKey(name) ? tags.get(name).load() : this.setTag(name, def);
    }

    public Serializable getSerializable(String name, Serializable def) {
        return objs.containsKey(name) ? objs.get(name) : this.setSerializable(name, def);
    }

    public int[] getIntegerArray(String name, int[] def) {
        return intArrays.containsKey(name) ? intArrays.get(name) : this.setIntegerArray(name, def);
    }

    public String[] getStringArray(String name, String[] def) {
        return stringArrays.containsKey(name) ? stringArrays.get(name) : this.setStringArray(name, def);
    }

    public boolean[] getBooleanArray(String name, boolean[] def) {
        return booleanArrays.containsKey(name) ? booleanArrays.get(name) : this.setBooleanArray(name, def);
    }

    public byte[] getByteArray(String name, byte[] def) {
        return byteArrays.containsKey(name) ? byteArrays.get(name) : this.setByteArray(name, def);
    }

    public float[] getFloatArray(String name, float[] def) {
        return floatArrays.containsKey(name) ? floatArrays.get(name) : this.setFloatArray(name, def);
    }

    public short[] getShortArray(String name, short[] def) {
        return shortArrays.containsKey(name) ? shortArrays.get(name) : this.setShortArray(name, def);
    }

    public double[] getDoubleArray(String name, double[] def) {
        return doubleArrays.containsKey(name) ? doubleArrays.get(name) : this.setDoubleArray(name, def);
    }

    public long[] getLongArray(String name, long[] def) {
        return longArrays.containsKey(name) ? longArrays.get(name) : this.setLongArray(name, def);
    }

    public Serializable[] getSerializableArray(String name, Serializable[] def) {
        return objArrays.containsKey(name) ? objArrays.get(name) : this.setSerializableArray(name, def);
    }

    public int getInteger(String name) {
        return ints.containsKey(name) ? ints.get(name) : 0;
    }

    public String getString(String name) {
        return strings.containsKey(name) ? strings.get(name) : "";
    }

    public boolean getBoolean(String name) {
        return booleans.containsKey(name) ? booleans.get(name) : false;
    }

    public byte getByte(String name) {
        return bytes.containsKey(name) ? bytes.get(name) : 0;
    }

    public float getFloat(String name) {
        return floats.containsKey(name) ? floats.get(name) : 0.0F;
    }

    public short getShort(String name) {
        return shorts.containsKey(name) ? shorts.get(name) : 0;
    }

    public double getDouble(String name) {
        return doubles.containsKey(name) ? doubles.get(name) : 0.0D;
    }

    public long getLong(String name) {
        return longs.containsKey(name) ? longs.get(name) : 0L;
    }

    public DataTag getTag(String name) {
        return tags.containsKey(name) ? tags.get(name).load() : new DataTag(this);
    }

    public Serializable getSerializable(String name) {
        return objs.containsKey(name) ? objs.get(name) : null;
    }

    public int[] getIntegerArray(String name) {
        return intArrays.containsKey(name) ? intArrays.get(name) : null;
    }

    public String[] getStringArray(String name) {
        return stringArrays.containsKey(name) ? stringArrays.get(name) : null;
    }

    public boolean[] getBooleanArray(String name) {
        return booleanArrays.containsKey(name) ? booleanArrays.get(name) : null;
    }

    public byte[] getByteArray(String name) {
        return byteArrays.containsKey(name) ? byteArrays.get(name) : null;
    }

    public float[] getFloatArray(String name) {
        return floatArrays.containsKey(name) ? floatArrays.get(name) : null;
    }

    public short[] getShortArray(String name) {
        return shortArrays.containsKey(name) ? shortArrays.get(name) : null;
    }

    public double[] getDoubleArray(String name) {
        return doubleArrays.containsKey(name) ? doubleArrays.get(name) : null;
    }

    public long[] getLongArray(String name) {
        return longArrays.containsKey(name) ? longArrays.get(name) : null;
    }

    public Serializable[] getSerializableArray(String name) {
        return objArrays.containsKey(name) ? objArrays.get(name) : null;
    }

    private DataTag load() {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
            DataTag obj = (DataTag) in.readObject();

            ints = obj.ints;
            strings = obj.strings;
            booleans = obj.booleans;
            bytes = obj.bytes;
            floats = obj.floats;
            shorts = obj.shorts;
            doubles = obj.doubles;
            longs = obj.longs;
            tags = obj.tags;
            objs = obj.objs;
            intArrays = obj.intArrays;
            stringArrays = obj.stringArrays;
            booleanArrays = obj.booleanArrays;
            byteArrays = obj.byteArrays;
            floatArrays = obj.floatArrays;
            shortArrays = obj.shortArrays;
            doubleArrays = obj.doubleArrays;
            longArrays = obj.longArrays;
            objArrays = obj.objArrays;

            in.close();
        } catch (Exception i) {
            if (!i.getClass().equals(EOFException.class)) {
                System.err.println("Exception: " + i.getClass().getName());
                i.printStackTrace();
            }
        }

        return this;
    }

    public DataTag save() {
        try {
            file.delete();
            file.createNewFile();
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(this);
            out.close();
        } catch (IOException e) {
            System.err.println("Exception: " + e.getClass().getName());
            e.printStackTrace();
        }

        return this;
    }
}

class FileHelper {
    /**
     * Create a file by default (Not a directory)
     *
     * @param dir The path for the file
     * @return True: If the file was created
     */
    public static boolean createFile(String dir) {
        return FileHelper.createFile(dir, false);
    }

    /**
     * Create a file or a directory by default
     *
     * @param dir         The path for the file
     * @param isDirectory Is is a directory of a file
     * @return True: If the file was created
     */
    public static boolean createFile(String dir, boolean isDirectory) {
        boolean returning = false;

        Path p = Paths.get(dir);
        try {
            if (Files.exists(p)) {
                returning = true;
            } else if (isDirectory) {
                Files.createDirectory(p);
                returning = true;
            } else {
                Files.createFile(p);
                returning = true;
            }
        } catch (IOException e) {
            System.err.println("Error Creating File!");
            System.err.println("Path: " + dir);
            System.err.println("Directory: " + isDirectory);
            e.printStackTrace();
        }
        return returning;
    }

    /**
     * Deletes the given file
     *
     * @param fileName The path for the file
     * @return True: If the file was successfully deleted
     */
    public static boolean deleteFile(String fileName) {
        Path p = Paths.get(fileName);

        try {
            return Files.deleteIfExists(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static ArrayList<File> files(File dir) {
        ArrayList<File> files = new ArrayList<File>();

        if (!dir.isDirectory())
            throw new IllegalArgumentException("dir Isn't a Directory! " + dir);

        for (int i = 0; i < dir.listFiles().length; i++) {
            if (dir.listFiles()[i].isDirectory()) {
                files.addAll(files(dir.listFiles()[i]));
            }
            files.add(dir.listFiles()[i]);
        }

        return files;
    }

    /**
     * Retrieves all the lines of a file and neatly puts them into an array!
     *
     * @param fileName The path for the file
     * @return The Lines of the given file
     */
    public static String[] getFileContents(String fileName) {
        ArrayList<String> lines = new ArrayList<String>();
        String line = "";
        BufferedReader reader = getFileReader(fileName);

        try {
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines.toArray(new String[0]);
    }

    /**
     * Creates a <code>BufferedReader</code> for the given File
     * <p>
     * <b><i>WARNING:</i></b> CAN STILL, VERY EASILY CAUSE AN {@link IOException}
     * <p>
     * Recommended you don't use this and use {@link #getFileContents(String)} instead!
     *
     * @param fileName The path for the file
     * @return The given file's <code>BufferedReader</code>
     */
    public static BufferedReader getFileReader(String fileName) {
        Charset c = Charset.forName("US-ASCII");
        Path p = Paths.get(fileName);

        try {
            return Files.newBufferedReader(p, c);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns all the Files in the specified directory and all sub-directories.
     * <p>
     * <p>
     * For instance, If you have a folder, /Files/Documents/Maps, and call this method for Hello. It will return all the files in Documents and all the files in Maps!
     *
     * @param directory The directory to check.
     * @return All the files in the folder and sub folders
     */
    public static File[] getFilesInFolder(File dir) {
        return files(dir).toArray(new File[0]);
    }

    /**
     * Prints the files lines to the console
     *
     * @param fileName The path for the file
     */
    public static void printFileContents(String fileName) {
        String[] lines = getFileContents(fileName);

        for (int i = 0; i < lines.length; i++) {
            System.out.println("Line[" + i + "]: " + lines[i]);
        }
    }

    /**
     * Deletes the given file and creates a new one with no content
     *
     * @param fileName The path for the file
     * @return A Path to the given File
     */
    public static Path resetFile(String fileName) {
        return resetFile(fileName, "");
    }

    /**
     * Deletes the given file and creates a new one with the given text
     *
     * @param fileName  The path for the file
     * @param textToAdd Any text you would like to add to the new file
     * @return A Path to the given File
     */
    public static Path resetFile(String fileName, String textToAdd) {
        Path p = Paths.get(fileName);

        deleteFile(fileName);
        createFile(fileName, false);
        FileHelper.writeToFile(fileName, textToAdd, false);

        return p;
    }

    /**
     * Writes the given string to the given File with a new line afterwards
     *
     * @param fileName The path for the file
     * @param stuff    The String you want to write to the given file
     * @return True: if the String was written to the file
     */
    public static boolean writeToFile(String fileName, String stuff) {
        return FileHelper.writeToFile(fileName, stuff, true);
    }

    /**
     * Writes the given string to the given File with
     *
     * @param fileName The path for the file
     * @param stuff    The String you want to write to the given file
     * @param newLine  If you want a '\n' character after the 'stuff' parameter
     * @return True: if the String was written to the file
     */
    public static boolean writeToFile(String fileName, String stuff, boolean newLine) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
            writer.write(stuff);
            if (newLine) {
                writer.newLine();
            }
            writer.close();
            return true;
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
            x.printStackTrace();
            return false;
        }
    }
}
