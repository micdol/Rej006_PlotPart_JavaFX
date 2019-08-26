package controllers;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import model.CursorModel;
import util.CursorManager;
import util.D;
import util.ExtendedLineChart;
import util.SignalGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class MainViewController {

    @FXML
    private BorderPane root;

    @FXML
    private VBox vbCursors;

    @FXML
    private ExtendedLineChart chart;

    /**
     * Cursors currently visible in the view
     */
    private final List<CursorViewController> cursors;

    private final List<SignalGenerator> generators;

    public MainViewController() {
        cursors = new ArrayList<>();
        generators = new ArrayList<>();
    }

    @FXML
    void initialize() {
        D.info(MainViewController.this, "Initializing");

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
        for (int i = 0; i < 6; i++) {
            SignalGenerator generator = new SignalGenerator();
            generator.setGenerationInterval(new Random().nextInt(100) + 10);
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            chart.getData().add(series);
            generator.getData().addListener((InvalidationListener) il -> series.getData().addAll(generator.getData()));
            generators.add(generator);
        }
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
        generators.forEach(SignalGenerator::start);
    }

    @FXML
    void onStopClicked(ActionEvent e) {
        generators.forEach(SignalGenerator::stop);
    }

}
