package plot.models;

import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import plot.ExtendedLineChart;
import util.D;

import java.util.List;
import java.util.stream.Collectors;

public abstract class PlotModeModelBase {

    protected final ObjectProperty<ExtendedLineChart> chart;
    protected final DoubleProperty delta;
    protected final DoubleProperty nextX;
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
    public ReadOnlyDoubleProperty nextXProperty() {
        return nextX;
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
    public double getNextX() {
        return nextX.get();
    }

    public void setChart(final ExtendedLineChart chart) {
        this.chart.set(chart);
    }
    protected void setFirstScreen(boolean firstScreen) {
        this.firstScreen.set(firstScreen);
    }
    public void setDelta(double delta) {
        if (delta < 0) {
            D.error(this, "Cannot set delta < 0");
            return;
        }
        this.delta.set(delta);
    }
    protected void setNextX(double nextX) {
        this.nextX.set(nextX);
    }

    // endregion

    protected PlotModeModelBase(final ExtendedLineChart chart) {
        this.chart = new SimpleObjectProperty<>(chart);
        firstScreen = new SimpleBooleanProperty(true);
        delta = new SimpleDoubleProperty(5.0 / 1000.0);
        nextX = new SimpleDoubleProperty(0.0);
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
        D.info(this, "Data size: " + data.size() + " channels size: " + data.stream().map(l -> "" + l.size()).collect(Collectors.joining(",")));

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
     * Moves first list values that they correspond to consecutive points y-values in second list
     * Added values are removed from first list. If anything stays there it indicates it would lay outside visible range.
     *
     * @param yValues list of y values
     * @param points  list of points [x,y]
     * @return x value corresponding to last added point (not greater than lastX)
     */
    protected double moveDataToPointsList(List<Double> yValues, List<XYChart.Data<Number, Number>> points) {
        final double maxX = getChart().getXAxis().getUpperBound();
        final double dx = getDelta();

        double x = getNextX();
        int numMoved = 0;

        for (; numMoved < yValues.size() && x <= maxX; numMoved++) {
            double y = yValues.get(numMoved);
            XYChart.Data<Number, Number> point = new XYChart.Data<>(x, y);
            D.info(this, "Moving point #" + points.size() + " point [" + x + "," + y + "]");
            points.add(point);
            x += dx;
        }

        yValues.subList(0, numMoved).clear();

        return x;
    }

    /**
     * Adds data to chart
     *
     * @param data array [series][data_values]
     */
    public abstract void addData(final List<List<Double>> data);

    public abstract void reset();
}
