package com.example.antispamsystem.model;

public class User {
    private String firstName;
    private String lastName;
    private Boolean robotCheck;
    private String cursorData;   // Для хранения данных о движениях курсора
    private String keyPressData; // Для хранения данных о нажатиях клавиш

    // Геттеры и сеттеры
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Boolean getRobotCheck() {
        return robotCheck;
    }

    public void setRobotCheck(Boolean robotCheck) {
        this.robotCheck = robotCheck;
    }

    public String getCursorData() {
        return cursorData;
    }

    public void setCursorData(String cursorData) {
        this.cursorData = cursorData;
    }

    public String getKeyPressData() {
        return keyPressData;
    }

    public void setKeyPressData(String keyPressData) {
        this.keyPressData = keyPressData;
    }
}
