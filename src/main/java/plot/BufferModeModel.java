package plot;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import util.D;

import java.util.ArrayList;
import java.util.List;

public class BufferModeModel extends PlotModeModelBase {

    private final List<List<XYChart.Data<Number, Number>>> buffer;
    private final DoubleProperty bufferFill;
    private double lastX;
    // region Properties

    public double getBufferFill() {
        return bufferFill.get();
    }
    public DoubleProperty bufferFillProperty() {
        return bufferFill;
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
    public void addData(double[][] seriesData) {
        if (isFirstScreen()) {
            addDataFirstScreen(seriesData);
        } else {
            addDataBuffer(seriesData);
        }
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

    /**
     * How many points can still be inserted before upper bound reached.
     * Inserting @returned_value + 1 will cause to last laying outside visible range
     *
     * @return number of points
     */
    private int numPointsToFill() {
        return (int) Math.floor((getChart().getXAxis().getUpperBound() - lastX) / getDelta());
    }

    private void addDataFirstScreen(double[][] data) {
        final ObservableList<XYChart.Series<Number, Number>> seriesList = getChart().getData();

        // Data validation
        if (seriesList.size() != data.length) {
            D.error(BufferModeModel.this, "Data length not equal to number of series");
            return;
        }
        for (int i = 1; i < data.length; i++) {
            if (data[i - 1].length != data[i].length) {
                D.error(BufferModeModel.this, "Samples for each series must be same size");
                return;
            }
        }

        D.info(BufferModeModel.this, "Adding first screen, #series: " + data.length + ", #points: " + data[0].length + ", lastX: " + lastX);

        final double dx = getDelta();
        // TODO do not worry about span != maxX - lower should be 0 but this is not programmed yet!
        final double maxX = getChart().getXAxis().getUpperBound();

        // These need to be here since it provide info about how/if data was processed after loops
        // If its not equal to the length of series data then some points where outside the screen and need to be
        // placed in buffer
        int seriesDataIdx = 0;
        double x = 0;

        // For each series...
        for (int seriesIdx = 0; seriesIdx < data.length; seriesIdx++) {
            x = lastX;
            final double[] seriesData = data[seriesIdx];
            final ObservableList<XYChart.Data<Number, Number>> chartSeriesData = getChart().getData().get(seriesIdx).getData();

            // For each point in series...
            for (seriesDataIdx = 0; seriesDataIdx < seriesData.length && x <= maxX; seriesDataIdx++, x += dx) {
                double y = seriesData[seriesDataIdx];
                D.info(BufferModeModel.this, "Adding #" + chartSeriesData.size() + " point [" + x + "," + y + "] for series #" + seriesIdx);
                XYChart.Data<Number, Number> pt = new XYChart.Data<>(x, y);
                chartSeriesData.add(pt);
            }
        }
        lastX = x;
        D.info(BufferModeModel.this, "Finished adding, last x: " + lastX);

        setFirstScreen(seriesDataIdx == data[0].length);
        if (!isFirstScreen()) {
            D.info(BufferModeModel.this, "Adding to leftovers from first screen to the buffer");
            lastX = 0;
            addDataBuffer(data, seriesDataIdx);
        }
    }

    private void addDataBuffer(double[][] data, int from) {
        D.info(BufferModeModel.this, "Adding to buffer from position: " + from);
        // For each series...
        double x = 0;
        final double dx = getDelta();

        for (int seriesIdx = 0; seriesIdx < data.length; seriesIdx++) {
            x = lastX;
            final double[] seriesData = data[seriesIdx];
            final List<XYChart.Data<Number, Number>> bufferData = buffer.get(seriesIdx);
            // For each point in series starting from @from...
            for (int dataIdx = from; dataIdx < seriesData.length; dataIdx++, x += dx) {
                double y = seriesData[dataIdx];
                D.info(BufferModeModel.this, "Adding #" + bufferData.size() + " point to buffer: [" + x + "," + y + "], series #" + seriesIdx);
                XYChart.Data<Number, Number> pt = new XYChart.Data<>(x, y);
                bufferData.add(pt);
            }
        }
        lastX = x;

        // TODO do not worry about span != maxX - lower should be 0 but this is not programmed yet!
        final double maxX = getChart().getXAxis().getUpperBound();
        D.info(BufferModeModel.this, "Last x after adding: " + lastX + ", maxX: " + maxX);
        setBufferFill(lastX / maxX);

        if (getBufferFill() > 1) {
            swapBuffer();
        }

        D.info(BufferModeModel.this, "Added to buffer, last x: " + lastX + ", buffer fill: " + getBufferFill());
    }

    private void addDataBuffer(double[][] data) {
        addDataBuffer(data, 0);
    }

    private void swapBuffer() {
        D.info(BufferModeModel.this, "Swapping buffer");

        final double dx = getDelta();
        // TODO do not worry about span != maxX - lower should be 0 but this is not programmed yet!
        final double maxX = getChart().getXAxis().getUpperBound();

        for (int seriesIdx = 0; seriesIdx < buffer.size(); seriesIdx++) {
            final List<XYChart.Data<Number, Number>> bufferSeriesData = buffer.get(seriesIdx);
            final ObservableList<XYChart.Data<Number, Number>> chartSeriesData = getChart().getData().get(seriesIdx).getData();
            chartSeriesData.clear();

            // Check if there are already too many points for a whole screen
            int numPts = (int) Math.ceil(maxX / dx);

            D.info(BufferModeModel.this, "Points to swap: " + numPts + ", points in buffer: " + bufferSeriesData.size());
            final List<XYChart.Data<Number, Number>> partToSwap = bufferSeriesData.subList(0, numPts);
            chartSeriesData.addAll(partToSwap);
            partToSwap.clear();

            for(int i=0; i<bufferSeriesData.size(); i++) {
                bufferSeriesData.get(i).setXValue(i *dx);
            }

            D.info(BufferModeModel.this, "Curr last x: " + lastX + ", new last x: " + bufferSeriesData.size() * dx);
            lastX = bufferSeriesData.size() * dx;
        }

        D.info(BufferModeModel.this, "Buffer swapped, last x: " + lastX);
    }

}
