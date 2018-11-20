package com.solace.sample.imagepersistence;

public final class Utils {

    public static String getEnvironmentValue(String varName, String defaultValue) {

        String envValue = System.getenv(varName);

        if (!(envValue != null && ! envValue.isEmpty())) {
            envValue = defaultValue;
        }

        return envValue;
    }
}
