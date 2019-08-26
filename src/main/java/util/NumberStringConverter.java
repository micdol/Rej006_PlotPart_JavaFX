package util;

import javafx.util.StringConverter;

import java.text.DecimalFormat;
import java.text.ParseException;

public class NumberStringConverter extends StringConverter<Number> {

    private final DecimalFormat df = new DecimalFormat("#.###");

    @Override
    public String toString(Number value) {
        if (value == null) {
            return "";
        }

        return df.format(value.doubleValue());
    }

    @Override
    public Double fromString(String value) {
        try {
            if (value == null) {
                return null;
            }

            value = value.trim();

            if (value.length() < 1) {
                return null;
            }

            // Perform the requested parsing
            return df.parse(value).doubleValue();
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }
}
