package controllers;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.chart.ValueAxis;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import model.CursorModel;
import util.D;


public class CursorPlotController extends Line {

    private final CursorModel data;
    private final ValueAxis<Number> axis;
    private final ObjectProperty<Number> pixelPosition;

    // region Properties
    public ObjectProperty<Number> pixelPositionProperty() {
        return pixelPosition;
    }
    public Number getPixelPosition() {
        return pixelPosition.get();
    }
    public CursorModel getData() {
        return data;
    }
    public void setPixelPosition(Number pixelPosition) {
        this.pixelPosition.set(pixelPosition);
    }

    // endregion

    public CursorPlotController(CursorModel data, ValueAxis<Number> axis) {
        super();
        this.data = data;
        this.axis = axis;
        this.pixelPosition = new SimpleObjectProperty<>();

        ChangeListener updatePositionListener = (o, ov, nv) -> Platform.runLater(() -> {
            setPixelPosition(this.axis.getDisplayPosition(this.data.getPosition()));
            D.info(CursorPlotController.this, "Something forced update, updating PIXEL position to: " + getPixelPosition());
        });

        this.axis.getScene().widthProperty().addListener(updatePositionListener);
        this.axis.getScene().heightProperty().addListener(updatePositionListener);
        this.axis.lowerBoundProperty().addListener(updatePositionListener);
        this.axis.upperBoundProperty().addListener(updatePositionListener);

        // Run only when cursor is added and scene can be retrieved
        Platform.runLater(() -> {
            setPixelPosition(this.axis.getDisplayPosition(this.data.getPosition()));
            try {
                Stage s = (Stage) getScene().getWindow();
                s.maximizedProperty().addListener(updatePositionListener);
                s.fullScreenProperty().addListener(updatePositionListener);
            } catch (ClassCastException e) {
                D.error(CursorPlotController.this, "Could not obtain stage, cursor may behave incorrectly on minimize, maximize");
            }
        });

        this.pixelPosition.addListener((o, ov, nv) -> {
            this.data.setPosition(this.axis.getValueForDisplay(nv.doubleValue()));
            D.info(CursorPlotController.this, "PIXEL position changed to: " + nv + " updating DATA position to: " + this.data.getPosition());
        });
        this.data.positionProperty().addListener((o, ov, nv) -> {
            setPixelPosition(this.axis.getDisplayPosition(nv));
            D.info(CursorPlotController.this, "DATA position changed to: " + nv + ", updating PIXEL position to: " + getPixelPosition());
        });

        this.data.colorProperty().addListener((o, ov, nv) -> setStroke(nv));
        startXProperty().bindBidirectional(pixelPosition);
        endXProperty().bindBidirectional(pixelPosition);
        setStartY(0);
        setEndY(2000);

        setupMouseMovement();
        setStroke(this.data.getColor());
    }

    private void setupMouseMovement() {
        addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            toFront();
            setStrokeWidth(3);
            setStroke(new Color(data.getColor().getRed(), data.getColor().getGreen(), data.getColor().getBlue(), 0.5));
        });
        addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            setStrokeWidth(1);
            setStroke(data.getColor());
        });
        addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> setStartX(e.getX()));
    }

}
