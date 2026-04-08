package com.eyetwin.tools;

import java.io.InputStream;
import java.util.Properties;

public class FlouciConfig {
    private static final Properties props = new Properties();

    static {
        try (InputStream in = FlouciConfig.class.getResourceAsStream("/flouci.properties")) {
            if (in != null) props.load(in);
        } catch (Exception e) {
            System.err.println("[FlouciConfig] Erreur : " + e.getMessage());
        }
    }

    public static String getAppToken()    { return props.getProperty("flouci.app.token", ""); }
    public static String getAppSecret()   { return props.getProperty("flouci.app.secret", ""); }
    public static String getTrackingId()  { return props.getProperty("flouci.tracking.id", ""); }
    public static String getApiUrl()      { return props.getProperty("flouci.api.url", "https://developers.flouci.com"); }
}