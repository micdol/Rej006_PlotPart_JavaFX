package util;

import javafx.beans.NamedArg;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;

public class NumberSpinnerValueFactory extends SpinnerValueFactory<Number> {

    public NumberSpinnerValueFactory(@NamedArg("min") double min,
                                     @NamedArg("max") double max) {
        this(min, max, min);
    }

    public NumberSpinnerValueFactory(@NamedArg("min") double min,
                                     @NamedArg("max") double max,
                                     @NamedArg("initialValue") double initialValue) {
        this(min, max, initialValue, 1);
    }

    public NumberSpinnerValueFactory(@NamedArg("min") double min,
                                     @NamedArg("max") double max,
                                     @NamedArg("initialValue") double initialValue,
                                     @NamedArg("amountToStepBy") double amountToStepBy) {
        setMin(min);
        setMax(max);
        setAmountToStepBy(amountToStepBy);
        setConverter(new StringConverter<Number>() {
            private final DecimalFormat df = new DecimalFormat("#.##");

            @Override
            public String toString(Number value) {
                // If the specified value is null, return a zero-length String
                if (value == null) {
                    return "";
                }

                return df.format(value.doubleValue());
            }

            @Override
            public Double fromString(String value) {
                try {
                    // If the specified value is null or zero-length, return null
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
        });

        valueProperty().addListener((o, oldValue, newValue) -> {
            // when the value is set, we need to react to ensure it is a
            // valid value (and if not, blow up appropriately)
            if (newValue.doubleValue() < getMin()) {
                setValue(getMin());
            } else if (newValue.doubleValue() > getMax()) {
                setValue(getMax());
            }
        });
        setValue(initialValue >= min && initialValue <= max ? initialValue : min);
    }


    private ObjectProperty<Number> min = new SimpleObjectProperty<>(this, "min");

    public final void setMin(double value) {
        min.set(value);
    }

    public final double getMin() {
        return min.get().doubleValue();
    }

    public final ObjectProperty<Number> minProperty() {
        return min;
    }

    private ObjectProperty<Number> max = new SimpleObjectProperty<>(this, "max");

    public final void setMax(double value) {
        max.set(value);
    }

    public final double getMax() {
        return max.get().doubleValue();
    }

    public final ObjectProperty<Number> maxProperty() {
        return max;
    }

    private DoubleProperty amountToStepBy = new SimpleDoubleProperty(this, "amountToStepBy");

    public final void setAmountToStepBy(double value) {
        amountToStepBy.set(value);
    }

    public final double getAmountToStepBy() {
        return amountToStepBy.get();
    }

    public final DoubleProperty amountToStepByProperty() {
        return amountToStepBy;
    }

    @Override
    public void decrement(int steps) {
        final BigDecimal currentValue = BigDecimal.valueOf(getValue().doubleValue());
        final BigDecimal minBigDecimal = BigDecimal.valueOf(getMin());
        final BigDecimal maxBigDecimal = BigDecimal.valueOf(getMax());
        final BigDecimal amountToStepByBigDecimal = BigDecimal.valueOf(getAmountToStepBy());
        BigDecimal newValue = currentValue.subtract(amountToStepByBigDecimal.multiply(BigDecimal.valueOf(steps)));
        setValue(newValue.compareTo(minBigDecimal) >= 0 ? newValue.doubleValue() : getMin());
    }

    @Override
    public void increment(int steps) {
        final BigDecimal currentValue = BigDecimal.valueOf(getValue().doubleValue());
        final BigDecimal minBigDecimal = BigDecimal.valueOf(getMin());
        final BigDecimal maxBigDecimal = BigDecimal.valueOf(getMax());
        final BigDecimal amountToStepByBigDecimal = BigDecimal.valueOf(getAmountToStepBy());
        BigDecimal newValue = currentValue.add(amountToStepByBigDecimal.multiply(BigDecimal.valueOf(steps)));
        setValue(newValue.compareTo(maxBigDecimal) <= 0 ? newValue.doubleValue() : getMax());
    }
}
