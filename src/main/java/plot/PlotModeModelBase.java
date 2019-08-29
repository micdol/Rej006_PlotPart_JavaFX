package plot;

import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import util.D;

import java.util.List;

public abstract class PlotModeModelBase {

    protected final ObjectProperty<ExtendedLineChart> chart;
    protected final DoubleProperty delta;
    protected final BooleanProperty firstScreen;

    // region Properties

    public ObjectProperty<ExtendedLineChart> chartProperty() {
        return chart;
    }
    public BooleanProperty firstScreenProperty() {
        return firstScreen;
    }
    public DoubleProperty deltaProperty() {
        return delta;
    }

    public ExtendedLineChart getChart() {
        return chart.get();
    }
    public boolean isFirstScreen() {
        return firstScreen.get();
    }
    public double getDelta() {
        return delta.get();
    }

    public void setChart(ExtendedLineChart chart) {
        this.chart.set(chart);
    }
    public void setFirstScreen(boolean firstScreen) {
        this.firstScreen.set(firstScreen);
    }
    public void setDelta(double delta) {
        if (delta < 0) {
            D.error(this, "Cannot set delta < 0");
            return;
        }
        this.delta.set(delta);
    }

    // endregion

    protected PlotModeModelBase(ExtendedLineChart chart) {
        this.chart = new SimpleObjectProperty<>(chart);
        firstScreen = new SimpleBooleanProperty(true);
        delta = new SimpleDoubleProperty(5.0 / 1000.0);
    }

    /**
     * Adds data to chart
     *
     * @param data array [series][data_values]
     */
    public abstract void addData(double[][] data);

    public abstract void reset();
}
