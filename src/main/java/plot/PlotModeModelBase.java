package plot;

import com.sun.istack.internal.NotNull;
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

    public void setChart(final ExtendedLineChart chart) {
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

    protected PlotModeModelBase(final ExtendedLineChart chart) {
        this.chart = new SimpleObjectProperty<>(chart);
        firstScreen = new SimpleBooleanProperty(true);
        delta = new SimpleDoubleProperty(5.0 / 1000.0);
    }

    /**
     * Checks whether provided data is valid that is:
     * - number of series in data (first level) matches number of series in {@link this.chart}
     * - number of data points in each series is the same
     *
     * @param data - [series][data_values]
     */
    protected void checkDataValid(final List<List<Double>> data) throws IllegalArgumentException {
        final ObservableList<XYChart.Series<Number, Number>> seriesList = getChart().getData();

        if (seriesList.size() != data.size()) {
            D.error(this, "Data length not equal to number of series");
            throw new IllegalArgumentException("Data length not equal to number of series");
        }
        for (int i = 1; i < data.size(); i++) {
            if (data.get(i - 1).size() != data.get(i).size()) {
                D.error(this, "Samples for each series must be same size");
                throw new IllegalArgumentException("Samples for each series must be same size");
            }
        }
    }

    /**
     * Adds data to chart
     *
     * @param data array [series][data_values]
     */
    public abstract void addData(final List<List<Double>> data);

    public abstract void reset();
}
