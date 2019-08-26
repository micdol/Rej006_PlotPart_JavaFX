package util;

import controllers.CursorPlotController;
import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.collections.ListChangeListener;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import model.CursorModel;

public class ExtendedLineChart extends LineChart<Number, Number> {

    public ExtendedLineChart(@NamedArg("xAxis") NumberAxis xAxis, @NamedArg("yAxis") NumberAxis yAxis) {
        super(xAxis, yAxis);

        // Monitor available cursors for deletion
        CursorManager.getInstance()
                .unmodifiableCursorPool()
                .addListener((ListChangeListener<? super CursorModel>) c -> {
                    while (c.next()) {
                        if (c.wasRemoved()) {
                            D.info(ExtendedLineChart.this, "Cursor was removed");
                            getPlotChildren().stream()
                                    .filter(n -> n instanceof CursorPlotController)
                                    .map(n -> (CursorPlotController) n)
                                    .filter(ctrl -> c.getRemoved().contains(ctrl.getData()))
                                    .forEach(ctrl -> Platform.runLater(() -> getPlotChildren().remove(ctrl)));
                        }
                    }
                });
    }

    public void addCursor(CursorModel data) {
        CursorPlotController controller = new CursorPlotController(data, getXAxis());
        getPlotChildren().add(controller);
    }

    @Override
    public NumberAxis getXAxis() {
        return (NumberAxis) super.getXAxis();
    }

    @Override
    public NumberAxis getYAxis() {
        return (NumberAxis) super.getYAxis();
    }
}