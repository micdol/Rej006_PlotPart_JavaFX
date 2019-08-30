package controllers;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import model.CursorModel;
import plot.BufferModeModel;
import plot.ExtendedLineChart;
import plot.PlotMode;
import util.CursorManager;
import util.D;
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
    private final List<SineSignalGenerator> generators;
    private final ProgressBar prgComboFill;

    public MainViewController() {
        cursors = new ArrayList<>();
        generators = new ArrayList<>();
        prgComboFill = new ProgressBar(0.5);
    }

    @FXML
    void initialize() {
        D.info(MainViewController.this, "Initializing");

        cbPlotMode.setButtonCell(new ListCell<PlotMode>() {
            @Override
            protected void updateItem(PlotMode item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) return;
                if (item == PlotMode.BUFFER) {
                    StackPane sp = new StackPane();
                    Label label = new Label(item.toString());
                    label.setTextFill(Color.BLACK);
                    sp.getChildren().addAll(prgComboFill, label);
                    setGraphic(sp);
                } else {
                    setText(item.toString());
                    setGraphic(null);
                }
            }
        });
        cbPlotMode.getItems().setAll(PlotMode.values());
        cbPlotMode.getSelectionModel().select(chart.getPlotMode());


        chart.plotModeProperty().bind(cbPlotMode.getSelectionModel().selectedItemProperty());
        chart.plotModelProperty().addListener(new PlotModelChangeListener(prgComboFill));
        chart.setPlotModel(new BufferModeModel(chart));

        // Monitor available cursors for deletion
        CursorManager.getInstance()
                .unmodifiableCursorPool()
                .addListener((ListChangeListener<? super CursorModel>) change -> {
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
        SineSignalGenerator generator = new SineSignalGenerator();

        generator.deltaProperty().addListener((o, ov, nv) -> chart.getPlotModel().setDelta(nv.doubleValue() / 1000.0));
        generator.numberOfSeriesProperty().addListener((o, ov, nv) -> {
            final ObservableList<XYChart.Series<Number, Number>> chartSeries = chart.getData();
            chartSeries.clear();
            for (int i = 0; i < nv.intValue(); i++) {
                chartSeries.add(new XYChart.Series<>());
            }
        });

        generator.setDelta(21);
        generator.setUpdateInterval(37);
        generator.setNumberOfSeries(5);
        generator.setPeriod(1337);
        generator.dataReadyProperty().addListener((o, wasReady, isReady) -> {
            if (isReady) {
                Platform.runLater(() -> chart.getPlotModel().addData(generator.getData()));
            }
        });
        generators.add(generator);


        Platform.runLater(() -> onResetZoomClicked(null));
        D.info(MainViewController.this, "Initialized");
    }


    @FXML
    void onAddClicked(ActionEvent e) {
        CursorModel cursorModel = new CursorModel();
        D.info(MainViewController.this, "Adding new cursor: " + cursorModel);

        CursorManager.getInstance().register(cursorModel);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/CursorView.fxml"));
        try {
            Parent root = loader.load();
            CursorViewController controller = loader.getController();
            controller.setData(cursorModel);
            vbCursors.getChildren().add(root);
            cursors.add(controller);
            chart.addCursor(cursorModel);
        } catch (IOException ex) {
            D.error(MainViewController.this, "Failed loading CursorView: " + ex);
            ex.printStackTrace();
        }
    }

    @FXML
    void onStartClicked(ActionEvent e) {
        generators.forEach(SineSignalGenerator::start);
    }

    @FXML
    void onStopClicked(ActionEvent e) {
        generators.forEach(SineSignalGenerator::stop);
    }

    public void onResetZoomClicked(ActionEvent event) {
        chart.getXAxis().resetZoom();
        chart.getYAxis().resetZoom();
    }
    public void onResetDataClicked(ActionEvent event) {
        chart.getPlotModel().reset();
    }
}
