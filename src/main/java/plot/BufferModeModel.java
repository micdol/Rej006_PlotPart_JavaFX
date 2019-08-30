package plot;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.*;
import util.D;

import java.util.ArrayList;
import java.util.List;

public class BufferModeModel extends PlotModeModelBase {

    private final List<List<XYChart.Data<Number, Number>>> buffer;
    private final DoubleProperty bufferFill;
    private double lastX;

    // region Properties

    public DoubleProperty bufferFillProperty() {
        return bufferFill;
    }

    public double getBufferFill() {
        return bufferFill.get();
    }

    public void setBufferFill(double bufferFill) {
        this.bufferFill.set(bufferFill);
    }

    // endregion

    public BufferModeModel(ExtendedLineChart chart) {
        super(chart);
        bufferFill = new SimpleDoubleProperty(0);
        buffer = new ArrayList<>();
        lastX = 0.0;
        chart.getData().addListener((ListChangeListener<? super XYChart.Series<Number, Number>>) change -> {
            while (change.next()) {
                if (change.wasRemoved() || change.wasAdded()) {
                    reset();
                    return;
                }
            }
        });
        chart.getXAxis().panEnableProperty().addListener((o, ov, nv) -> chart.getXAxis().setPanEnable(false));
    }

    @Override
    public void reset() {
        setFirstScreen(true);
        buffer.clear();
        for (int i = 0; i < getChart().getData().size(); i++) {
            buffer.add(new ArrayList<>());
        }
        lastX = 0.0;
        setBufferFill(0.0);
        getChart().getData().forEach(s -> s.getData().clear());
        getChart().getXAxis().setPanEnable(false);
    }

    @Override
    public void addData(List<List<Double>> data) {
        if (isFirstScreen()) {
            addDataFirstScreen(data);
        } else {
            addDataBuffer(data);
        }
    }

    /**
     * Moves first list values that they correspond to consecutive points y-values in second list
     * @param yValues list of y values
     * @param points list of points [x,y]
     * @return x value corresponding to last added point
     */
    private double moveDataToPointsList(List<Double> yValues, List<Data<Number, Number>> points) {
        final double maxX = getChart().getXAxis().getUpperBound();
        final double dx = getDelta();

        double x = lastX;
        int numMoved = 0;

        for (; numMoved < yValues.size() && x <= maxX; numMoved++) {
            double y = yValues.get(numMoved);
            Data<Number, Number> point = new Data<>(x, y);
            D.info(BufferModeModel.this, "Adding #" + points.size() + " point [" + x + "," + y + "]");
            points.add(point);
            x += dx;
        }

        yValues.subList(0, numMoved).clear();

        return x;
    }

    /**
     * Adds points so that they appear in the chart immediately.
     * @param data list of series -> y values list
     */
    private void addDataFirstScreen(List<List<Double>> data) {
        checkDataValid(data);

        final ObservableList<Series<Number, Number>> chartSeries = getChart().getData();
        final double maxX = getChart().getXAxis().getUpperBound();
        double x = 0;

        for (int i = 0; i < data.size(); i++) {
            final List<Double> yValues = data.get(i);
            final List<Data<Number, Number>> chartSeriesPoints = chartSeries.get(i).getData();
            x = moveDataToPointsList(yValues, chartSeriesPoints);
        }

        lastX = x;

        setFirstScreen(lastX <= maxX);
        if (!isFirstScreen()) {
            D.info(BufferModeModel.this, "Adding to leftovers from first screen to the buffer");
            lastX = 0;
            addDataBuffer(data);
        }

        D.info(BufferModeModel.this, "Finished adding, last x: " + lastX);
    }

    /**
     * Adds point values so that they are placed in the buffer. If buffer is filled during this operation it is swapped.
     * @param data list of series -> y values list
     */
    private void addDataBuffer(List<List<Double>> data) {
        D.info(BufferModeModel.this, "Adding to buffer");

        checkDataValid(data);

        final double maxX = getChart().getXAxis().getUpperBound();
        double x = 0;
        for (int i = 0; i < data.size(); i++) {
            final List<Double> yValues = data.get(i);
            final List<Data<Number, Number>> bufferSeriesPoints = buffer.get(i);
            x = moveDataToPointsList(yValues, bufferSeriesPoints);
        }
        lastX = x;

        D.info(BufferModeModel.this, "Last x after adding: " + lastX + ", maxX: " + maxX);
        setBufferFill(lastX / maxX);
        if (getBufferFill() >= 1) {
            swapBuffer();
            // At this point data may still contain some values which didn't fit into buffer before (filled up)
            addDataBuffer(data);
        }
        D.info(BufferModeModel.this, "Added to buffer, last x: " + lastX + ", buffer fill: " + getBufferFill());
    }

    /**
     * Replaces chart data with buffered data.
     */
    private void swapBuffer() {
        D.info(BufferModeModel.this, "Swapping buffer");

        final ObservableList<Series<Number, Number>> chartSeries = getChart().getData();
        for (int i = 0; i < chartSeries.size(); i++) {
            final List<Data<Number, Number>> bufferSeriesPoints = buffer.get(i);
            final List<Data<Number, Number>> chartSeriesPoints = chartSeries.get(i).getData();
            chartSeriesPoints.clear();
            chartSeriesPoints.addAll(bufferSeriesPoints);
            bufferSeriesPoints.clear();
        }
        lastX = 0;

        D.info(BufferModeModel.this, "Buffer swapped");
    }

}
