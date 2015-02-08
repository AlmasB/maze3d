package com.almasb.maze;

import java.util.ResourceBundle;

public final class AppProperties {

    private static final ResourceBundle RESOURCES = ResourceBundle
            .getBundle("props.app");

    public static String getVersion() {
        try {
            return RESOURCES.getString("version");
        }
        catch (Exception e) {
            return "VERSION NOT FOUND";
        }
    }
}
