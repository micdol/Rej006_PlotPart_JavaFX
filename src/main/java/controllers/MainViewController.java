package controllers;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import model.CursorModel;
import plot.*;
import plot.models.*;
import util.CursorManager;
import util.D;
import util.ProgressListCell;
import util.SineSignalGenerator;
import util.listeners.PlotModelChangeListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainViewController {

    // region FXML Controls

    @FXML
    private BorderPane root;

    @FXML
    private Button btnStart;

    @FXML
    private Button btnStop;

    @FXML
    private Button btnResetZoom;

    @FXML
    private Button btnResetData;

    @FXML
    private ComboBox<PlotMode> cbPlotMode;

    @FXML
    private VBox vbCursors;

    @FXML
    private ExtendedLineChart chart;

    // endregion FXML Controls

    /**
     * Cursors currently visible in the view
     */
    private final List<CursorViewController> cursors;
    private final SineSignalGenerator generator;
    private final ProgressBar prgComboFill;

    public MainViewController() {
        cursors = new ArrayList<>();
        generator = new SineSignalGenerator();
        prgComboFill = new ProgressBar(0.0);
    }

    @FXML
    void initialize() {
        D.info(MainViewController.this, "Initializing");

        cbPlotMode.setButtonCell(new ProgressListCell(prgComboFill));
        cbPlotMode.getItems().setAll(PlotMode.values());
        cbPlotMode.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            prgComboFill.progressProperty().unbind();
            PlotModeModelBase tmp = null;
            if (nv == PlotMode.BUFFER) {
                tmp = new BufferModeModel(chart);
                prgComboFill.progressProperty().bind(((BufferModeModel) tmp).bufferFillProperty());
            }
            if (nv == PlotMode.CURSOR) tmp = new CursorModeModel(chart);
            if (nv == PlotMode.SCREEN) tmp = new ScreenModeModel(chart);
            if (nv == PlotMode.FREE) tmp = new FreeModeModel(chart, ov != null);
            final PlotModeModelBase model = tmp;
            model.setDelta(generator.getDelta() / 1000.0);
            chart.setPlotModel(model);
        });
        cbPlotMode.getSelectionModel().select(PlotMode.FREE);

        // Monitor available cursors for deletion
        CursorManager.getInstance().unmodifiableCursorPool().addListener((ListChangeListener<? super CursorModel>) change -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    D.info(MainViewController.this, "Cursors: [" + change.getRemoved().stream().map(c -> ((CursorModel) c).getName()).collect(Collectors.joining(",")) + "] were removed");
                    cursors.stream()
                            .filter(ctrl -> change.getRemoved().contains(ctrl.getData()))
                            .forEach(ctrl -> vbCursors.getChildren().remove(ctrl.getViewRoot()));
                }
            }
        });

        // Data generation
        generator.deltaProperty().addListener((o, ov, nv) -> chart.getPlotModel().setDelta(nv.doubleValue() / 1000.0));
        generator.numberOfSeriesProperty().addListener((o, ov, nv) -> {
            final ObservableList<XYChart.Series<Number, Number>> chartSeries = chart.getData();
            chartSeries.clear();
            for (int i = 0; i < nv.intValue(); i++) {
                chartSeries.add(new XYChart.Series<>());
            }
        });
        generator.setDelta(200);
        generator.setUpdateInterval(40);
        generator.setNumberOfSeries(6);
        generator.setPeriod(3215);
        generator.dataReadyProperty().addListener((o, wasReady, isReady) -> {
            if (isReady) {
                Platform.runLater(() -> chart.getPlotModel().addData(generator.getData()));
            }
        });

        Platform.runLater(() -> onResetZoomClicked(null));
        D.info(MainViewController.this, "Initialized");
    }

    @FXML
    void onAddClicked(ActionEvent e) {
        CursorModel cursorModel = new CursorModel();
        D.info(MainViewController.this, "Adding cursor: " + cursorModel);
        CursorManager.getInstance().register(cursorModel);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/CursorView.fxml"));
        try {
            Parent root = loader.load();
            CursorViewController controller = loader.getController();
            controller.setData(cursorModel);
            vbCursors.getChildren().add(root);
            cursors.add(controller);
            chart.addCursor(cursorModel);
            D.info(MainViewController.this, "Added cursor: " + cursorModel);
        } catch (IOException ex) {
            D.error(MainViewController.this, "Failed loading CursorView: " + ex);
            ex.printStackTrace();
        }
    }

    @FXML
    void onStartClicked(ActionEvent e) {
        generator.start();
    }

    @FXML
    void onStopClicked(ActionEvent e) {
        generator.stop();
        cbPlotMode.getSelectionModel().select(PlotMode.FREE);
    }

    @FXML
    public void onResetZoomClicked(ActionEvent event) {
        chart.getXAxis().resetZoom();
        chart.getYAxis().resetZoom();
    }

    @FXML
    public void onResetDataClicked(ActionEvent event) {
        chart.getPlotModel().reset();
        generator.reset();
    }
}
