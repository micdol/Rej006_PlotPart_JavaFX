package plot.models;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import plot.ExtendedLineChart;
import util.D;

import java.util.List;

public class CursorModeModel extends PlotModeModelBase {

    private final XYChart.Data<Number, Number> cursorsPoint;

    // region Properties

    public ReadOnlyObjectProperty<Number> cursorPositionProperty() {
        return cursorsPoint.XValueProperty();
    }

    public double getCursorPosition() {
        return cursorsPoint.getXValue().doubleValue();
    }

    protected void setCursorPosition(double cursorPosition) {
        this.cursorsPoint.setXValue(cursorPosition);
    }

    // endregion

    public CursorModeModel(ExtendedLineChart chart) {
        super(chart);
        this.cursorsPoint = new XYChart.Data<>(0, 10e10);
        cursorsPoint.XValueProperty().bind(nextXProperty());
        reset();
        addCursorPoints();
    }

    @Override
    public void reset() {
        setFirstScreen(true);
        setNextX(0);
        getChart().getData().forEach(s -> s.getData().clear());
        getChart().getXAxis().setPanEnable(false);
    }

    @Override
    public void addData(List<List<Double>> data) {
        if (isFirstScreen()) {
            addDataFirstScreen(data);
        } else {
            addDataCursor(data);
        }
    }

    /**
     * Adds points so that they appear in the chart immediately.
     *
     * @param data list of series -> y values list
     */
    private void addDataFirstScreen(List<List<Double>> data) {
        D.info(CursorModeModel.this, "First screen adding, last x: " + getNextX());

        checkDataValid(data);

        final ObservableList<XYChart.Series<Number, Number>> chartSeries = getChart().getData();
        final double maxX = getChart().getXAxis().getUpperBound();
        double x = 0;

        for (int i = 0; i < data.size(); i++) {
            final List<Double> yValues = data.get(i);
            final List<XYChart.Data<Number, Number>> chartSeriesPoints = chartSeries.get(i).getData();
            x = moveDataToPointsList(yValues, chartSeriesPoints);
        }

        setNextX(x);
        setFirstScreen(x <= maxX);
        if (!isFirstScreen()) {
            setNextX(0.0);
            addDataCursor(data);
        }

        D.info(CursorModeModel.this, "Finished adding, last x: " + getNextX());
    }

    /**
     * Adds point values so that they replace points on the right of the cursor.
     * Cursor is then moved.
     *
     * @param data list of series -> y values list
     */
    private void addDataCursor(List<List<Double>> data) {
        D.info(CursorModeModel.this, "Adding before cursor");

        checkDataValid(data);

        final ObservableList<XYChart.Series<Number, Number>> chartSeries = getChart().getData();
        final double maxX = getChart().getXAxis().getUpperBound();
        double x = 0.0;

        for (int i = 0; i < data.size(); i++) {
            final List<Double> yValues = data.get(i);
            final List<XYChart.Data<Number, Number>> chartSeriesPoints = chartSeries.get(i).getData();
            x = moveDataToPointsList(yValues, chartSeriesPoints);
            chartSeriesPoints.subList(0, yValues.size()).clear();
        }
        setNextX(x);

        if (x >= maxX) {
            setNextX(0);
            addDataCursor(data);
        }

        D.info(CursorModeModel.this, "Added before cursor, lastX: " + getNextX());
    }

    private void addCursorPoints() {
        final ObservableList<XYChart.Series<Number, Number>> chartSeries = getChart().getData();

        for (int i = 0; i < chartSeries.size(); i++) {
            final List<XYChart.Data<Number, Number>> chartSeriesPoints = chartSeries.get(i).getData();
            chartSeriesPoints.add(cursorsPoint);
        }
    }
}
