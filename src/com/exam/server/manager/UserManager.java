package com.exam.server.manager;

import com.exam.common.model.Admin;
import com.exam.common.model.Student;
import com.exam.common.model.Teacher;
import com.exam.common.model.User;
import com.exam.server.storage.FileStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserManager {

    private List<User> users;

    public UserManager() {
        users = FileStorage.load(FileStorage.USERS_FILE);
        if (users.isEmpty()) {
            users.add(new Admin("admin", "admin"));
            users.add(new Teacher("t1", "123"));
            users.add(new Student("s1", "123"));
            users.add(new Student("s2", "123"));
            save();
        }
    }

    public synchronized User login(String username, String password) {
        Optional<User> user = users.stream()
                .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password))
                .findFirst();
        return user.orElse(null);
    }

    public synchronized User register(String username, String password, String role) {
        if (users.stream().anyMatch(u -> u.getUsername().equals(username))) {
            return null; // 用户名已存在
        }
        User user;
        switch (role.toUpperCase()) {
            case "STUDENT":
                user = new Student(username, password);
                break;
            case "TEACHER":
                user = new Teacher(username, password);
                break;
            default:
                return null;
        }
        users.add(user);
        save();
        return user;
    }

    public synchronized List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    public synchronized boolean deleteUser(String username) {
        // 不能删除admin
        if ("admin".equals(username)) return false;
        boolean removed = users.removeIf(u -> u.getUsername().equals(username));
        if (removed) save();
        return removed;
    }

    public synchronized User addUser(String username, String password, String role) {
        return register(username, password, role);
    }

    private void save() {
        FileStorage.save(FileStorage.USERS_FILE, users);
    }
}
