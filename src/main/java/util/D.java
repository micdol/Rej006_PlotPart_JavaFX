package util;

import javafx.beans.value.ChangeListener;

import java.text.SimpleDateFormat;
import java.util.Date;

public class D {
    private static final SimpleDateFormat m_format = new SimpleDateFormat("HH:mm:ss.SSS");

    private static String timestamp() {
        return m_format.format(new Date());
    }

    private static void log(String type, Object source, Object message) {
        System.out.println("[" + type + "] [" + timestamp() + "] [" + Thread.currentThread().getId() + "] [" + source.getClass().getName() + "] " + message);
    }

    public static ChangeListener DEBUG_LISTENER(Object source) {
        return (o, ov, nv) -> System.out.println("[INFO] [" + timestamp() + "] [" + Thread.currentThread().getId() + "] Value of: " + source + " changed from: " + ov + " to: " + nv);
    }

    public static void info(Object source, Object message) {
        log("INFO", source, message);
    }

    public static void warn(Object source, Object message) {
        log("WARN", source, message);
    }

    public static void error(Object source, Object message) {
        log("ERROR", source, message);
    }

}
