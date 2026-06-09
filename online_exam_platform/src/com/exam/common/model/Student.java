package com.exam.common.model;

public class Student extends User {
    private static final long serialVersionUID = 1L;

    public Student() {}

    public Student(String username, String password) {
        super(username, password, "STUDENT");
    }
}
