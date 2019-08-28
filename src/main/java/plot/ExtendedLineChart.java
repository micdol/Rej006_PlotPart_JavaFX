package plot;

import controllers.CursorPlotController;
import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import model.CursorModel;
import util.CursorManager;
import util.D;

public class ExtendedLineChart extends LineChart<Number, Number> {

    public ExtendedLineChart(@NamedArg("xAxis") ValueAxis<Number> xAxis, @NamedArg("yAxis") ValueAxis<Number> yAxis) {
        super(xAxis, yAxis);

        zoomRectWrapper = new ReadOnlyObjectWrapper<>(new Rectangle(0, 0, new Color(0, 1, 0, 0.5)));
        zoomRect = zoomRectWrapper.getReadOnlyProperty();
        rectZoomingWrapper = new ReadOnlyBooleanWrapper(false);
        rectZooming = rectZoomingWrapper.getReadOnlyProperty();
        zoomRectButton = new SimpleObjectProperty<>(MouseButton.PRIMARY);
        zoomInModifier = new SimpleObjectProperty<>(KeyCode.CONTROL);
        zoomOutModifier = new SimpleObjectProperty<>(KeyCode.ALT);

        setAnimated(false);

        xAxis.setMinorTickVisible(false);
        xAxis.setAnimated(false);
        xAxis.setAutoRanging(false);
        xAxis.setUpperBound(30);

        yAxis.setMinorTickVisible(false);
        yAxis.setAnimated(false);
        yAxis.setAutoRanging(false);
        yAxis.setUpperBound(10);
        yAxis.setLowerBound(-10);

        setupZooming();
        setupCursors();
    }

    // region Cursors

    public void addCursor(CursorModel data) {
        D.info(ExtendedLineChart.this, "Adding cursor: " + data);

        boolean alreadyPresent = getPlotChildren().stream()
                .filter(n -> n instanceof CursorPlotController)
                .map(n -> ((CursorPlotController) n).getData())
                .anyMatch(cm -> cm == data);

        if (alreadyPresent) {
            D.warn(ExtendedLineChart.this, "Cursor: " + data + " already present");
            return;
        }

        CursorPlotController controller = new CursorPlotController(data, getXAxis());
        getPlotChildren().add(controller);

        D.info(ExtendedLineChart.this, "Added cursor: " + data);
    }

    private void setupCursors() {
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

    // endregion

    // region Zooming

    private final ReadOnlyObjectProperty<Rectangle> zoomRect;
    private final ReadOnlyObjectWrapper<Rectangle> zoomRectWrapper;
    private final ReadOnlyBooleanProperty rectZooming;
    private final ReadOnlyBooleanWrapper rectZoomingWrapper;
    private final ObjectProperty<MouseButton> zoomRectButton;
    private final ObjectProperty<KeyCode> zoomInModifier;
    private final ObjectProperty<KeyCode> zoomOutModifier;
    private boolean isZoomInModifierDown, isZoomOutModifierDown;

    // region Properties

    public ReadOnlyObjectProperty<Rectangle> zoomRectProperty() {
        return zoomRect;
    }
    public ReadOnlyBooleanProperty rectZoomingProperty() {
        return rectZooming;
    }
    public ObjectProperty<MouseButton> zoomRectButtonProperty() {
        return zoomRectButton;
    }
    public ObjectProperty<KeyCode> zoomInModifierProperty() {
        return zoomInModifier;
    }
    public ObjectProperty<KeyCode> zoomOutModifierProperty() {
        return zoomOutModifier;
    }

    public Rectangle getZoomRect() {
        return zoomRect.get();
    }
    public boolean isRectZooming() {
        return rectZooming.get();
    }
    public MouseButton getZoomRectButton() {
        return zoomRectButton.get();
    }
    public KeyCode getZoomInModifier() {
        return zoomInModifier.get();
    }
    public KeyCode getZoomOutModifier() {
        return zoomOutModifier.get();
    }
    public double getZoomOutScale() {
        return 1.5;
    }
    public double getZoomInScale() {
        return 1.0 / getZoomOutScale();
    }

    private void setZoomRect(double x, double y, double w, double h) {
        Rectangle zoomRect = getZoomRect();
        zoomRect.setX(x);
        zoomRect.setY(y);
        zoomRect.setWidth(w);
        zoomRect.setHeight(h);
    }
    public void setRectZooming(boolean isRectZooming) {
        rectZoomingWrapper.set(isRectZooming);
    }
    public void setZoomRectButton(MouseButton zoomRectButton) {
        this.zoomRectButton.set(zoomRectButton);
    }
    public void setZoomInModifier(KeyCode zoomInModifier) {
        this.zoomInModifier.set(zoomInModifier);
    }
    public void setZoomOutModifier(KeyCode zoomOutModifier) {
        this.zoomOutModifier.set(zoomOutModifier);
    }

    // endregion

    private void setupZooming() {
        Platform.runLater(() -> {
            D.info(ExtendedLineChart.this, "Setting up zooming");

            Node cpb = lookup(".chart-plot-background");
            cpb.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onZoomRectStart);
            cpb.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onZoomRectDrag);
            cpb.addEventHandler(MouseEvent.MOUSE_RELEASED, this::onZoomRectEnd);
            cpb.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onZoomOutClick);
            cpb.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onZoomInClick);
            cpb.addEventHandler(ScrollEvent.SCROLL, this::onZoomScroll);

            getScene().addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                isZoomInModifierDown = e.getCode() == getZoomInModifier();
                isZoomOutModifierDown = e.getCode() == getZoomOutModifier();
                D.info(ExtendedLineChart.this, "Key pressed, zoom in modifier: " + isZoomInModifierDown + ", zoom out modifier: " + zoomOutModifier);
            });
            getScene().addEventHandler(KeyEvent.KEY_RELEASED, e -> {
                isZoomInModifierDown = (e.getCode() == getZoomInModifier()) != isZoomInModifierDown;
                isZoomOutModifierDown = (e.getCode() == getZoomOutModifier()) != isZoomOutModifierDown;
                D.info(ExtendedLineChart.this, "Key released, zoom in modifier: " + isZoomInModifierDown + ", zoom out modifier: " + zoomOutModifier);
            });

            getPlotChildren().add(getZoomRect());
            rectZoomingWrapper.bindBidirectional(getZoomRect().visibleProperty());
            setRectZooming(false);

            D.info(ExtendedLineChart.this, "Zooming set up");
        });
    }

    private class Pt {
        double x;
        double y;
    }

    private final Pt anchor = new Pt(), movable = new Pt(), topLeft = new Pt(), bottomRight = new Pt();

    private void onZoomRectStart(MouseEvent e) {
        if (isZoomInModifierDown && !isRectZooming() && e.getButton() == getZoomRectButton()) {
            D.info(ExtendedLineChart.this, "Starting rect zoom @ [" + e.getX() + "," + e.getY() + "]");
            e.consume();
            Rectangle zoomRect = getZoomRect();

            anchor.x = movable.x = e.getX();
            anchor.y = movable.y = e.getY();

            double nx = anchor.x;
            double ny = anchor.y;
            double nw = movable.x - anchor.x;
            double nh = movable.y - anchor.y;

            setZoomRect(nx, ny, nw, nh);
            setRectZooming(true);
        }
    }
    private void onZoomRectDrag(MouseEvent e) {
        if (isRectZooming()) {
            e.consume();
            Rectangle zoomRect = getZoomRect();

            movable.x = e.getX();
            movable.y = e.getY();

            topLeft.x = Math.min(anchor.x, movable.x);
            topLeft.y = Math.min(anchor.y, movable.y);
            bottomRight.x = Math.max(anchor.x, movable.x);
            bottomRight.y = Math.max(anchor.y, movable.y);

            double nx = topLeft.x;
            double ny = topLeft.y;
            double nw = bottomRight.x - topLeft.x;
            double nh = bottomRight.y - topLeft.y;

            setZoomRect(nx, ny, nw, nh);
        }
    }
    private void onZoomRectEnd(MouseEvent e) {
        if (isRectZooming()) {
            D.info(ExtendedLineChart.this, "Ending rect zoom @ [" + e.getX() + "," + e.getY() + "]");
            e.consume();
            onZoom(getZoomRect());
            setRectZooming(false);
        }
    }
    private void onZoomOutClick(MouseEvent e) {
        // Sometimes it seem like CTRL, ALT, SHIFT press is not previously detected
        isZoomOutModifierDown = isZoomOutModifierDown ||
                (e.isControlDown() && getZoomOutModifier() == KeyCode.CONTROL) ||
                (e.isAltDown() && getZoomOutModifier() == KeyCode.ALT) ||
                (e.isShiftDown() && getZoomOutModifier() == KeyCode.SHIFT);

        if (isZoomOutModifierDown && e.getButton() == getZoomRectButton() && e.getClickCount() > 1) {
            e.consume();
            getXAxis().onZoomOut(e.getX());
            getYAxis().onZoomOut(e.getY());
        }
    }
    private void onZoomInClick(MouseEvent e) {
        // Sometimes it seem like CTRL, ALT, SHIFT press is not previously detected
        isZoomInModifierDown = isZoomInModifierDown ||
                (e.isControlDown() && getZoomInModifier() == KeyCode.CONTROL) ||
                (e.isAltDown() && getZoomInModifier() == KeyCode.ALT) ||
                (e.isShiftDown() && getZoomInModifier() == KeyCode.SHIFT);

        if (isZoomInModifierDown && e.getButton() == getZoomRectButton() && e.getClickCount() > 1) {
            e.consume();
            getXAxis().onZoomIn(e.getX());
            getYAxis().onZoomIn(e.getY());
        }
    }
    private void onZoomScroll(ScrollEvent e) {
        // Noticed that for scrolling its more intuitive to have either of the modifiers down
        if (isZoomInModifierDown || isZoomOutModifierDown) {
            e.consume();
            if (e.getDeltaY() > 0) {
                getXAxis().onZoomIn(e.getX());
                getYAxis().onZoomIn(e.getY());
            } else if (e.getDeltaY() < 0) {
                getXAxis().onZoomOut(e.getX());
                getYAxis().onZoomOut(e.getY());
            }
        }
    }

    private void onZoom(Rectangle zoomRect) {
        if (zoomRect.getWidth() > 3) {
            getXAxis().onZoom(zoomRect.getX(), zoomRect.getX() + zoomRect.getWidth());
        }
        if (zoomRect.getHeight() > 3) {
            getYAxis().onZoom(zoomRect.getY(), zoomRect.getY() + zoomRect.getHeight());
        }
    }

    // endregion

    // region Overrides

    @Override
    public ExtendedNumberAxis getXAxis() {
        return (ExtendedNumberAxis) super.getXAxis();
    }

    @Override
    public ExtendedNumberAxis getYAxis() {
        return (ExtendedNumberAxis) super.getYAxis();
    }

    // endregion
}