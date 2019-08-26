package util;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SignalGenerator {

    private final ObservableList<XYChart.Data<Number, Number>> data;
    private final ObservableList<XYChart.Data<Number, Number>> dataChunk;
    private long updateInterval;
    private long generationInterval;
    private boolean isRunning;

    public boolean isRunning() {
        return isRunning;
    }

    public ObservableList<XYChart.Data<Number, Number>> getData() {
        return data;
    }

    public void setUpdateInterval(long updateInterval) {
        boolean wasRunning = isRunning;
        if (updateInterval != this.updateInterval) {
            stop();
        }
        this.updateInterval = updateInterval;
        if (wasRunning) {
            start();
        }
    }

    public void setGenerationInterval(long generationInterval) {
        boolean wasRunning = isRunning;
        if (generationInterval != this.generationInterval) {
            stop();
        }
        if (generationInterval < 1) {
            generationInterval = 1;
        }
        this.generationInterval = generationInterval;
        if (wasRunning) {
            start();
        }
    }

    private final double T;
    private double x, offset;
    private final Random noiseGen;
    private ScheduledExecutorService updateExecutor, generationExecutor;

    public SignalGenerator() {
        data = FXCollections.observableArrayList();
        dataChunk = FXCollections.observableArrayList();
        noiseGen = new Random();
        T = 2;
        updateInterval = 1000 / 30;
        generationInterval = 1;
        x = 0;
        offset = noiseGen.nextDouble() * T;
        isRunning = false;
    }

    public void start() {
        stop();
        isRunning = true;
        updateExecutor = Executors.newSingleThreadScheduledExecutor();
        generationExecutor = Executors.newSingleThreadScheduledExecutor();
        updateExecutor.scheduleAtFixedRate(this::update, updateInterval, updateInterval, TimeUnit.MILLISECONDS);
        generationExecutor.scheduleAtFixedRate(this::generate, generationInterval, generationInterval, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (updateExecutor != null) {
            updateExecutor.shutdownNow();
            generationExecutor.shutdownNow();
            isRunning = false;
        }
    }

    private void update() {
        Platform.runLater(() -> {
            data.clear();
            synchronized (dataChunk) {
                data.addAll(dataChunk);
                dataChunk.clear();
            }
        });
    }

    private void generate() {
        double y = Math.sin(2.0 * Math.PI * x / T + offset);
        XYChart.Data<Number, Number> pt = new XYChart.Data<>(x, y);
        synchronized (dataChunk) {
            dataChunk.add(pt);
            x += generationInterval / 1000.0;
        }

    }
}
