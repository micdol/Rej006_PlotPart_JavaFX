package plot.models;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import plot.ExtendedLineChart;
import plot.ExtendedNumberAxis;
import util.D;

import java.util.List;

public class ScreenModeModel extends PlotModeModelBase {

    public ScreenModeModel(ExtendedLineChart chart) {
        super(chart);
        getChart().getData().forEach(s -> s.getData().clear());
        getChart().getXAxis().setPanEnable(false);
    }

    @Override
    public void addData(List<List<Double>> data) {
        D.info(ScreenModeModel.this, "First screen adding, last x: " + getNextX());

        checkDataValid(data);

        final ObservableList<XYChart.Series<Number, Number>> chartSeries = getChart().getData();
        final double maxX = getChart().getXAxis().getUpperBound();
        final double dx = getDelta();
        double x = getNextX();

        if (x > maxX) {
            int numPointsToAdd = data.get(0).size();
            movePointsLeft(numPointsToAdd);
        }

        for (int i = 0; i < data.size(); i++) {
            final List<Double> yValues = data.get(i);
            final List<XYChart.Data<Number, Number>> chartSeriesPoints = chartSeries.get(i).getData();
            x = moveDataToPointsList(yValues, chartSeriesPoints);
        }
        setNextX(x);

        D.info(ScreenModeModel.this, "Finished adding, last x: " + getNextX());
    }
    @Override
    public void reset() {
        setFirstScreen(true);
        setNextX(0);
        getChart().getData().forEach(s -> s.getData().clear());

        ExtendedNumberAxis xAxis = getChart().getXAxis();
        Platform.runLater(() -> {
            double span = xAxis.getSpan();
            System.out.println("span: " + span);
            xAxis.setLowerBound(0);
            xAxis.setUpperBound(span);
            xAxis.setPanEnable(false);
        });
    }

    private void movePointsLeft(int numberOfPoints) {
        final ObservableList<XYChart.Series<Number, Number>> chartSeries = getChart().getData();
        final double dx = getDelta();
        double x = 0;
        for (int i = 0; i < chartSeries.size(); i++) {
            final List<XYChart.Data<Number, Number>> chartSeriesPoints = chartSeries.get(i).getData();
            chartSeriesPoints.subList(0, numberOfPoints).clear();
            for (int j = 0; j < chartSeriesPoints.size(); j++) {
                x = j * dx;
                chartSeriesPoints.get(j).setXValue(x);
            }
        }
        setNextX(chartSeries.get(0).getData().size() * dx);
    }

}
