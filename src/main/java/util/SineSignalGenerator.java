package util;

import javafx.beans.property.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class SineSignalGenerator {

    private static final double TWO_PI = Math.PI * 2.0;

    /**
     * Number of series to generate, first level of data
     */
    private final IntegerProperty numberOfSeries;
    /**
     * Indicates that data buffer is filled and ready
     */
    private final BooleanProperty dataReady;
    /**
     * Data x-delta, in milliseconds
     */
    private final LongProperty delta;
    /**
     * Interval between data is ready and updated, in milliseconds
     */
    private final LongProperty updateInterval;
    /**
     * Whether data generation is currently running;
     */
    private final BooleanProperty running;
    private final LongProperty period;

    /**
     * data[numberOfSeries][numberOfDataPoints]
     * On each update contains NEW data
     */
    private double[][] data;
    private List<List<Double>> buffer;
    private long dataIdx, bufferIdx;
    private final Random random;

    private final ThreadFactory daemonFactory = new DaemonThreadFactory();
    private ScheduledExecutorService updateExecutor;
    private ScheduledExecutorService generateExecutor;

    // region Properties

    public IntegerProperty numberOfSeriesProperty() {
        return numberOfSeries;
    }
    public BooleanProperty dataReadyProperty() {
        return dataReady;
    }
    public LongProperty deltaProperty() {
        return delta;
    }
    public LongProperty updateIntervalProperty() {
        return updateInterval;
    }
    public BooleanProperty runningProperty() {
        return running;
    }
    public LongProperty periodProperty() {
        return period;
    }

    public int getNumberOfSeries() {
        return numberOfSeries.get();
    }
    public boolean isDataReady() {
        return dataReady.get();
    }
    public long getDelta() {
        return delta.get();
    }
    public long getUpdateInterval() {
        return updateInterval.get();
    }
    public synchronized double[][] getData() {
        return data;
    }
    public boolean isRunning() {
        return running.get();
    }
    public long getPeriod() {
        return period.get();
    }

    public void setNumberOfSeries(int numberOfSeries) {
        if (numberOfSeries < 1) {
            D.error(SineSignalGenerator.this, "Cannot set negative number of series");
            return;
        }

        this.numberOfSeries.set(numberOfSeries);
        reset();
    }
    public void setDataReady(boolean dataReady) {
        this.dataReady.set(dataReady);
    }
    public void setDelta(long delta_ms) {
        if (delta_ms < 0) {
            D.error(SineSignalGenerator.this, "Cannot set negative delta");
            return;
        }

        this.delta.set(delta_ms);
        reset();
    }
    public void setUpdateInterval(long updateInterval_ms) {
        if (updateInterval_ms < 0) {
            D.error(SineSignalGenerator.this, "Cannot set negative update interval");
            return;
        }

        this.updateInterval.set(updateInterval_ms);
        reset();
    }
    private void setRunning(boolean running) {
        this.running.set(running);
    }
    public void setPeriod(long period_ms) {
        if (period_ms < 1) {
            D.error(SineSignalGenerator.this, "Cannot set period < 1");
            return;
        }
        this.period.set(period_ms);
    }
    // region Properties

    public SineSignalGenerator() {
        numberOfSeries = new SimpleIntegerProperty(0);
        dataReady = new SimpleBooleanProperty(false);
        delta = new SimpleLongProperty(1L);
        updateInterval = new SimpleLongProperty(1L);
        running = new SimpleBooleanProperty(false);
        random = new Random(123L);
        buffer = new ArrayList<>();
        period = new SimpleLongProperty(random.nextLong() % 2000 + 3000);
    }

    public synchronized void start() {
        if (isRunning()) {
            stop();
        }

        D.info(SineSignalGenerator.this, "Starting");

        int numOfPoints = (int) Math.ceil((double) getUpdateInterval() / getDelta());
        buffer.clear();
        for (int i = 0; i < getNumberOfSeries(); i++) {
            buffer.add(new ArrayList<>(numOfPoints));
        }
        dataIdx = bufferIdx = 0;

        generateExecutor = Executors.newSingleThreadScheduledExecutor(daemonFactory);
        generateExecutor.scheduleWithFixedDelay(this::generate, 0, getDelta(), TimeUnit.MILLISECONDS);

        updateExecutor = Executors.newSingleThreadScheduledExecutor(daemonFactory);
        updateExecutor.scheduleWithFixedDelay(this::update, getUpdateInterval(), getUpdateInterval(), TimeUnit.MILLISECONDS);

        setRunning(true);

        D.info(SineSignalGenerator.this, "Started");
    }

    public synchronized void stop() {
        if (!isRunning()) return;
        D.info(SineSignalGenerator.this, "Stopping");

        generateExecutor.shutdownNow();
        updateExecutor.shutdownNow();
        setDataReady(false);
        setRunning(false);

        D.info(SineSignalGenerator.this, "Stopped");
    }

    public synchronized void reset() {
        boolean wasRunning = isRunning();
        stop();
        if (wasRunning) start();
    }

    private synchronized void generate() {
        double period = getPeriod();
        double delta = getDelta();
        double numOfSeries = getNumberOfSeries();
        for (int i = 0; i < numOfSeries; i++) {
            double t = TWO_PI * dataIdx * delta / period;
            double offset = i * TWO_PI / (numOfSeries + 1); // +1 to have it not totally symmetric
            double y = 3.0 * Math.sin(t + offset);
            buffer.get(i).add(y);
            //D.info(SignalGenerator.this, "Generated #" + dataIdx + " (#" + bufferIdx + ") point value: " + y + " for series #" + i);
        }
        dataIdx++;
        bufferIdx++;
    }

    private synchronized void update() {
        int numberOfPoints = buffer.get(0).size();
        if(numberOfPoints == 0) return;

        data = new double[getNumberOfSeries()][numberOfPoints];
        for (int i = 0; i < getNumberOfSeries(); i++) {
            final List<Double> seriesData = buffer.get(i);
            for (int j = 0; j < numberOfPoints; j++) {
                data[i][j] = seriesData.get(j);
            }
            seriesData.clear();
        }
        //D.info(SineSignalGenerator.this, "Update triggered, ready: " + getNumberOfSeries() + " series, each: " + data[0].length + " points");
        bufferIdx = 0;

        // Don't want to create event, just "ping" listeners
        setDataReady(true);
        setDataReady(false);
    }

    private double noise() {
        return random.nextDouble();
    }

}
