package plot;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.chart.ValueAxis;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import util.D;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ExtendedNumberAxis extends ValueAxis<Number> {

    /**
     * Object returned by autoRange() or getRange() used to setRange() and calculateTickValues()
     */
    private static class Range {
        double lower, upper, span;
        Range(double lower, double upper) {
            this.lower = lower;
            this.upper = upper;
            this.span = Math.abs(upper - lower);
        }
    }

    private final DecimalFormat tickLabelFormatter = new DecimalFormat("#.###");

    private final DoubleProperty zoomScale;
    private final BooleanProperty zoomEnable;
    private final BooleanProperty panEnable;
    private final ObjectProperty<MouseButton> panMouseButton;
    private final BooleanProperty panning;

    // region Properties

    public DoubleProperty zoomScaleProperty() {
        return zoomScale;
    }
    public BooleanProperty zoomEnableProperty() {
        return zoomEnable;
    }
    public BooleanProperty panEnableProperty() {
        return panEnable;
    }
    public ObjectProperty<MouseButton> panMouseButtonProperty() {
        return panMouseButton;
    }
    public ReadOnlyBooleanProperty panningProperty() {
        return panning;
    }

    public double getZoomScale() {
        return zoomScale.get();
    }
    public boolean isZoomEnable() {
        return zoomEnable.get();
    }
    public boolean isPanEnable() {
        return panEnable.get();
    }
    public MouseButton getPanMouseButton() {
        return panMouseButton.get();
    }
    public boolean isPanning() {
        return panning.get();
    }

    public void setZoomScale(double value) {
        this.zoomScale.set(value);
    }
    public void setZoomEnable(boolean zoomEnable) {
        this.zoomEnable.set(zoomEnable);
    }
    public void setPanEnable(boolean panEnable) {
        this.panEnable.set(panEnable);
    }
    public void setPanMouseButton(MouseButton panMouseButton) {
        this.panMouseButton.set(panMouseButton);
    }
    // Panning flag is managed internally, it would make no sense to make it editable from the outside
    protected void setPanning(boolean panning) {
        this.panning.set(panning);
    }

    // endregion

    public ExtendedNumberAxis() {
        zoomScale = new SimpleDoubleProperty(0.9);
        zoomEnable = new SimpleBooleanProperty(true);
        panEnable = new SimpleBooleanProperty(true);
        panMouseButton = new SimpleObjectProperty<>(MouseButton.MIDDLE);
        panning = new SimpleBooleanProperty(false);

        // Don't allow zoom values outside (0,1) range
        zoomScale.addListener((o, ov, nv) -> {
            if (nv.doubleValue() <= 0) {
                D.warn(ExtendedNumberAxis.this, "Setting zoom must be > 0, 0.0001 will be used");
                setZoomScale(0.0001);
            }
            if (nv.doubleValue() > 1) {
                D.warn(ExtendedNumberAxis.this, "Setting zoom must be < 1, 0.9999 will be used");
                setZoomScale(0.9999);
            }
        });

        setupZooming();
        setupPanning();
    }

    // region Panning
    // Panning is done when axis is grabbed (default MMB) and dragged, by default enabled

    // TODO add modifier? Not sure, seems like rather intuitive to have it like it is now

    /**
     * Helper class to handle simple point related operations
     */
    private class Pt {
        double x;
        double y;
    }

    /**
     * Starting point of pan drag, delta is measured to next detected point,
     * then points are swapped and the process is repeated until release
     */
    private final Pt panAnchor = new Pt();

    private void setupPanning() {
        addEventHandler(MouseEvent.MOUSE_PRESSED, this::onPanStart);
        addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onPanDrag);
        addEventHandler(MouseEvent.MOUSE_RELEASED, this::onPanEnd);
        setPanEnable(true);
    }
    private void onPanStart(MouseEvent e) {
        if (!isPanEnable()) {
            D.warn(ExtendedNumberAxis.this, "Pan is disabled");
            return;
        }

        if (e.getButton() != getPanMouseButton()) return;

        D.info(ExtendedNumberAxis.this, "Pan started @ [" + e.getX() + "," + e.getY() + "]");

        setPanning(true);
        panAnchor.x = e.getX();
        panAnchor.y = e.getY();
    }
    private void onPanDrag(MouseEvent e) {
        if (!isPanning()) return;

        double panDeltaPx = getSide().isHorizontal() ? panAnchor.x - e.getX() : panAnchor.y - e.getY();
        D.info(ExtendedNumberAxis.this, "Pan delta [px]: " + panDeltaPx);

        if (Math.abs(panDeltaPx) < 3) return;
        panAnchor.x = e.getX();
        panAnchor.y = e.getY();
        double panDelta = getValueForDisplay(panDeltaPx).doubleValue() - getValueForDisplay(0).doubleValue();
        D.info(ExtendedNumberAxis.this, "Pan delta: " + panDelta);

        setRange(new Range(getLowerBound() + panDelta, getUpperBound() + panDelta), false);
    }
    private void onPanEnd(MouseEvent e) {
        if (!isPanning()) return;

        D.info(ExtendedNumberAxis.this, "Pan ended");

        setPanning(false);
        panAnchor.x = panAnchor.y = 0;
    }
    // endregion

    // region Zooming
    // Zooming is done via scrolling when mouse is over axis, by default enabled
    // May be disabled for good, also is disabled when panning is currently active

    // TODO add modifier? Not sure, seems like rather intuitive to have it like it is now

    private void setupZooming() {
        addEventHandler(ScrollEvent.SCROLL, this::onScroll);
        setZoomEnable(true);
    }

    private void onScroll(ScrollEvent e) {
        double positionPx = getSide().isHorizontal() ? e.getX() : e.getY();
        if (e.getDeltaY() > 0) {
            onZoomIn(positionPx);
        } else if (e.getDeltaY() < 0) {
            onZoomOut(positionPx);
        }
    }

    public void resetZoom() {
        setRange(new Range(getSide().isHorizontal() ? 0 : -10, 10), false);
    }

    /**
     * Zooms in by getZoomScale() so that positionPx stays in the same spot on screen after zooming
     *
     * @param positionPx position in pixels to zoom at
     */
    public void onZoomIn(double positionPx) {
        onZoomImpl(positionPx, getZoomScale());
    }

    /**
     * Zooms out by 1.0/getZoomScale() so that positionPx stays in the same spot on screen after zooming
     *
     * @param positionPx position in pixels to zoom at
     */
    public void onZoomOut(double positionPx) {
        onZoomImpl(positionPx, 1.0 / getZoomScale());
    }

    // Little lie since in fact this one uses onZoom :D
    private void onZoomImpl(double positionPx, double s) {
        Range r = getRange();
        double lowerPx = positionPx - positionPx * s;
        double upperPx = lowerPx + s * (getSide().isHorizontal() ? getWidth() : getHeight());
        onZoom(lowerPx, upperPx);
    }

    /**
     * Zooms in/out so that lower bound corresponds to pixel position @ lowerPx, upper similarly
     *
     * @param lowerPx value in pixels where lower should be after zooming (in current coordinates, can be negative)
     * @param upperPx value in pixels where upper should be after zooming (in current coordinates, can be negative)
     */
    public void onZoom(double lowerPx, double upperPx) {
        if (!isZoomEnable() || isPanning()) {
            D.warn(ExtendedNumberAxis.this, "Zoom is disabled");
            return;
        }
        double nl = getValueForDisplay(lowerPx).doubleValue();
        double nu = getValueForDisplay(upperPx).doubleValue();
        setRange(new Range(Math.min(nl, nu), Math.max(nl, nu)), false);
    }

    // endregion

    // region Overrides

    @Override
    protected List<Number> calculateMinorTickMarks() {
        // Minor ticks not supported
        return new ArrayList<>();
    }
    @Override
    protected void setRange(Object range, boolean animate) {
        if (animate) {
            D.warn(ExtendedNumberAxis.this, "Animations are not supported (setRange)");
        }
        Range r = (Range) range;
        D.info(ExtendedNumberAxis.this, "Setting new range, lower: " + r.lower + ", upper: " + r.upper);
        Platform.runLater(() -> setUpperBound(r.upper));
        Platform.runLater(() -> setLowerBound(r.lower));
    }
    @Override
    protected Range getRange() {
        return new Range(getLowerBound(), getUpperBound());
    }
    @Override
    protected List<Number> calculateTickValues(double length, Object range) {
        Range r = (Range) range;
        List<Number> tickValues = new ArrayList<>();

        // Always 11 major ticks so that screen is divided into 10 segments
        for (double tick = r.lower; tick < r.upper; tick += r.span / 10.0) {
            tickValues.add(tick);
        }
        tickValues.add(r.upper);

        return tickValues;
    }
    @Override
    protected String getTickMarkLabel(Number value) {
        if (value == null) return null;
        double span = getUpperBound() - getLowerBound();
        tickLabelFormatter.applyPattern(span > 50 ? "#" : span > 10 ? "#.#" : span > 1 ? "#.##" : "#.###");
        return tickLabelFormatter.format(value);
    }
    @Override
    public Number getValueForDisplay(double displayPosition) {
        if (getSide().isHorizontal()) {
            return getLowerBound() + displayPosition * (getUpperBound() - getLowerBound()) / getWidth();
        } else {
            return getUpperBound() + displayPosition * (getLowerBound() - getUpperBound()) / getHeight();
        }
    }
    @Override
    public double getDisplayPosition(Number value) {
        if (getSide().isHorizontal()) {
            return getWidth() / (getUpperBound() - getLowerBound()) * (value.doubleValue() - getLowerBound());
        } else {
            return (getHeight() * (-getUpperBound() + value.doubleValue())) / (getLowerBound() - getUpperBound());
        }

    }
    // endregion
}