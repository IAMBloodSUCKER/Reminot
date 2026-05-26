package ru.demo.utils;

import java.io.InputStream;
import java.util.Properties;

public class Checking {
    
    public static String checkVersion() {
        String version = null;
        try (InputStream input = Checking.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                return null;
            }
            Properties props = new Properties();
            props.load(input);
            version = props.getProperty("application.version");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return version;
    }
}
