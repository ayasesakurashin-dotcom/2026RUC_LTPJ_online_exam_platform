package com.exam.server.storage;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileStorage {

    public static final String DATA_DIR = "data";
    public static final String USERS_FILE = DATA_DIR + "/users.dat";
    public static final String EXAMS_FILE = DATA_DIR + "/exams.dat";
    public static final String SCORES_FILE = DATA_DIR + "/scores.dat";

    static {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T extends Serializable> List<T> load(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            return (List<T>) ois.readObject();
        } catch (Exception e) {
            System.err.println("Failed to load " + filePath + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static synchronized <T extends Serializable> void save(String filePath, List<T> data) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)))) {
            oos.writeObject(data);
            oos.flush();
        } catch (Exception e) {
            System.err.println("Failed to save " + filePath + ": " + e.getMessage());
        }
    }
}
