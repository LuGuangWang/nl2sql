package com.wlg.nl2sql.tools;

public final class Strings {
    private Strings(){}

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.length() == 0;
    }

    public static boolean notNullOrEmpty(String string){
        return !isNullOrEmpty(string);
    }
}
