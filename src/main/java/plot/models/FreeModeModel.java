package plot.models;

import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import plot.ExtendedLineChart;
import util.D;

import java.util.List;

public class FreeModeModel extends PlotModeModelBase {

    public FreeModeModel(ExtendedLineChart chart) {
        this(chart, false);
    }

    public FreeModeModel(ExtendedLineChart chart, boolean inheritData) {
        super(chart);
        setNextX(0);
        getChart().getXAxis().setPanEnable(true);
        getChart().getXAxis().setZoomEnable(true);
        if (!inheritData) {
            getChart().getData().forEach(s -> s.getData().clear());
        }
    }

    @Override
    public void addData(List<List<Double>> data) {
        checkDataValid(data);

        D.info(FreeModeModel.this, "Adding data, channels: " + data.size() + ", each: " + data.get(0).size() + " points");

        final ObservableList<XYChart.Series<Number, Number>> chartSeries = getChart().getData();
        double x = 0;

        for (int i = 0; i < data.size(); i++) {
            final List<Double> yValues = data.get(i);
            final List<XYChart.Data<Number, Number>> chartSeriesPoints = chartSeries.get(i).getData();
            x = moveDataToPointsList(yValues, chartSeriesPoints);
        }
        setNextX(x);

        D.info(FreeModeModel.this, "Added data, channels: " + data.size() + ", each: " + data.get(0).size() + " points");
    }

    @Override
    public void reset() {
        setNextX(0);
        getChart().getData().forEach(s -> s.getData().clear());
        getChart().getXAxis().setPanEnable(true);
        getChart().getXAxis().setZoomEnable(true);
    }
}
