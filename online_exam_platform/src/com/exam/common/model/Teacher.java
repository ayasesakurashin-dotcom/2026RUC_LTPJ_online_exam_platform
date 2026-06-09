package com.exam.common.model;

public class Teacher extends User {
    private static final long serialVersionUID = 1L;

    public Teacher() {}

    public Teacher(String username, String password) {
        super(username, password, "TEACHER");
    }
}
