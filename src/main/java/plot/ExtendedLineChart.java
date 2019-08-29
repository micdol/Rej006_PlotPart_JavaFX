package plot;

import controllers.CursorPlotController;
import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import model.CursorModel;
import util.CursorManager;
import util.D;

public class ExtendedLineChart extends LineChart<Number, Number> {

    private final SimpleObjectProperty<Rectangle> zoomRect;
    private final SimpleBooleanProperty rectZooming;
    private final ObjectProperty<MouseButton> zoomRectButton;
    private final ObjectProperty<KeyCode> zoomInModifier;
    private final ObjectProperty<KeyCode> zoomOutModifier;
    private final BooleanProperty zoomInModifierDown;
    private final BooleanProperty zoomOutModifierDown;
    private final SimpleObjectProperty<MouseButton> panButton;
    private final BooleanProperty panning;
    private final ObjectProperty<PlotMode> mode;

    private static class Pt {
        double x;
        double y;
    }

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
    public ReadOnlyBooleanProperty zoomInModifierDownProperty() {
        return zoomInModifierDown;
    }
    public ReadOnlyBooleanProperty zoomOutModifierDownProperty() {
        return zoomOutModifierDown;
    }
    public SimpleObjectProperty<MouseButton> panButtonProperty() {
        return panButton;
    }
    public ReadOnlyBooleanProperty panningProperty() {
        return panning;
    }
    public ObjectProperty<PlotMode> modeProperty() {
        return mode;
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
    public boolean isZoomInModifierDown() {
        return zoomInModifierDown.get();
    }
    public boolean isZoomOutModifierDown() {
        return zoomOutModifierDown.get();
    }
    public MouseButton getPanButton() {
        return panButton.get();
    }
    public boolean isPanning() {
        return panning.get();
    }
    public PlotMode getMode() {
        return mode.get();
    }

    private void setZoomRect(double x, double y, double w, double h) {
        Rectangle zoomRect = getZoomRect();
        zoomRect.setX(x);
        zoomRect.setY(y);
        zoomRect.setWidth(w);
        zoomRect.setHeight(h);
    }
    private void setRectZooming(boolean isRectZooming) {
        rectZooming.set(isRectZooming);
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
    private void setZoomInModifierDown(boolean zoomInModifierDown) {
        this.zoomInModifierDown.set(zoomInModifierDown);
    }
    private void setZoomOutModifierDown(boolean zoomOutModifierDown) {
        this.zoomOutModifierDown.set(zoomOutModifierDown);
    }
    public void setPanButton(MouseButton panButton) {
        this.panButton.set(panButton);
    }
    private void setPanning(boolean panning) {
        this.panning.set(panning);
    }
    public void setMode(PlotMode mode) {
        this.mode.set(mode);
    }

    // endregion

    public ExtendedLineChart(@NamedArg("xAxis") ValueAxis<Number> xAxis, @NamedArg("yAxis") ValueAxis<Number> yAxis) {
        super(xAxis, yAxis);

        zoomRect = new SimpleObjectProperty<>(new Rectangle(0, 0, new Color(0, 1, 0, 0.5)));
        rectZooming = new SimpleBooleanProperty(false);
        zoomRectButton = new SimpleObjectProperty<>(MouseButton.PRIMARY);
        zoomInModifier = new SimpleObjectProperty<>(KeyCode.CONTROL);
        zoomOutModifier = new SimpleObjectProperty<>(KeyCode.ALT);
        zoomInModifierDown = new SimpleBooleanProperty(false);
        zoomOutModifierDown = new SimpleBooleanProperty(false);
        panButton = new SimpleObjectProperty<>(MouseButton.MIDDLE);
        panning = new SimpleBooleanProperty(false);
        mode = new SimpleObjectProperty<>(PlotMode.FREE);

        setAnimated(false);

        xAxis.setMinorTickVisible(false);
        xAxis.setAnimated(false);
        xAxis.setAutoRanging(false);
        xAxis.setUpperBound(10);

        yAxis.setMinorTickVisible(false);
        yAxis.setAnimated(false);
        yAxis.setAutoRanging(false);
        yAxis.setUpperBound(10);
        yAxis.setLowerBound(-10);

        setupZooming();
        setupPanning();
        setupCursors();
        setupMouseCursors();
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

    // region  Mouse Cursors

    private void setupMouseCursors() {
        final Cursor zoomInCursor = new ImageCursor(new Image("/images/zoom-in-rect-cursor.png"), 0, 0);
        final Cursor zoomOutCursor = new ImageCursor(new Image("/images/zoom-out-rect-cursor.png"), 0, 0);

        zoomInModifierDown.addListener((o, ov, nv) -> setCursor(nv ? zoomInCursor : Cursor.DEFAULT));
        zoomOutModifierDown.addListener((o, ov, nv) -> setCursor(nv ? zoomOutCursor : Cursor.DEFAULT));
        panning.addListener((o, ov, nv) -> setCursor(nv ? Cursor.CLOSED_HAND : Cursor.DEFAULT));
    }

    // endregion

    // region Panning

    private final Pt panAnchor = new Pt();

    private void setupPanning() {
        addEventHandler(MouseEvent.MOUSE_PRESSED, this::onPanStart);
        addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onPanDrag);
        addEventHandler(MouseEvent.MOUSE_RELEASED, this::onPanEnd);
        getXAxis().setPanEnable(true);
        getYAxis().setPanEnable(true);
    }
    private void onPanStart(MouseEvent e) {
        if (e.getButton() != getPanButton()) return;
        setPanning(true);
        panAnchor.x = e.getX();
        panAnchor.y = e.getY();
    }
    private void onPanDrag(MouseEvent e) {
        if (!isPanning()) return;
        if (getXAxis().onPan(panAnchor.x, e.getX())) {
            panAnchor.x = e.getX();
        }
        if (getYAxis().onPan(panAnchor.y, e.getY())) {
            panAnchor.y = e.getY();
        }
    }
    private void onPanEnd(MouseEvent e) {
        if (!isPanning()) return;
        getXAxis().onPan(panAnchor.x, e.getX());
        getYAxis().onPan(panAnchor.y, e.getY());
        setPanning(false);
        panAnchor.x = panAnchor.y = 0;
    }
    // endregion

    // region Zooming

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
                setZoomInModifierDown(e.getCode() == getZoomInModifier());
                setZoomOutModifierDown(e.getCode() == getZoomOutModifier());
                D.info(ExtendedLineChart.this, "Key pressed, zoom in modifier: " + isZoomInModifierDown() + ", zoom out modifier: " + zoomOutModifier);
            });
            getScene().addEventHandler(KeyEvent.KEY_RELEASED, e -> {
                setZoomInModifierDown((e.getCode() == getZoomInModifier()) != isZoomInModifierDown());
                setZoomOutModifierDown((e.getCode() == getZoomOutModifier()) != isZoomOutModifierDown());
                D.info(ExtendedLineChart.this, "Key released, zoom in modifier: " + isZoomInModifierDown() + ", zoom out modifier: " + zoomOutModifier);
            });

            getPlotChildren().add(getZoomRect());
            rectZooming.bindBidirectional(getZoomRect().visibleProperty());
            setRectZooming(false);

            D.info(ExtendedLineChart.this, "Zooming set up");
        });
    }

    private final Pt anchor = new Pt(), movable = new Pt(), topLeft = new Pt(), bottomRight = new Pt();

    private void onZoomRectStart(MouseEvent e) {
        if (isZoomInModifierDown() && !isRectZooming() && e.getButton() == getZoomRectButton()) {
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
        setZoomOutModifierDown(isZoomOutModifierDown() ||
                (e.isControlDown() && getZoomOutModifier() == KeyCode.CONTROL) ||
                (e.isAltDown() && getZoomOutModifier() == KeyCode.ALT) ||
                (e.isShiftDown() && getZoomOutModifier() == KeyCode.SHIFT));

        if (isZoomOutModifierDown() && e.getButton() == getZoomRectButton() && e.getClickCount() > 1) {
            e.consume();
            getXAxis().onZoomOut(e.getX());
            getYAxis().onZoomOut(e.getY());
        }
    }
    private void onZoomInClick(MouseEvent e) {
        // Sometimes it seem like CTRL, ALT, SHIFT press is not previously detected
        setZoomInModifierDown(isZoomInModifierDown() ||
                (e.isControlDown() && getZoomInModifier() == KeyCode.CONTROL) ||
                (e.isAltDown() && getZoomInModifier() == KeyCode.ALT) ||
                (e.isShiftDown() && getZoomInModifier() == KeyCode.SHIFT));

        if (isZoomInModifierDown() && e.getButton() == getZoomRectButton() && e.getClickCount() > 1) {
            e.consume();
            getXAxis().onZoomIn(e.getX());
            getYAxis().onZoomIn(e.getY());
        }
    }
    private void onZoomScroll(ScrollEvent e) {
        // Noticed that for scrolling its more intuitive to have either of the modifiers down
        if (isZoomInModifierDown() || isZoomOutModifierDown()) {
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