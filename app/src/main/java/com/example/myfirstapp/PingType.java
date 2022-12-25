package com.example.myfirstapp;

public enum PingType {
    ONCE,
    RECURRING;

    public static PingType fromInt(int value) {
        if (value == 1) {
            return RECURRING;
        }
        return ONCE;
    }
}
