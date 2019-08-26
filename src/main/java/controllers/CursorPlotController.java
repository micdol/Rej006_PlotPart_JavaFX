package controllers;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.chart.Axis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import model.CursorModel;


public class CursorPlotController extends Line {

    private final CursorModel data;
    private final NumberAxis axis;
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

    public CursorPlotController(CursorModel data, NumberAxis axis) {
        super();
        this.data = data;
        this.axis = axis;
        this.pixelPosition = new SimpleObjectProperty<>();

        pixelPosition.addListener((o, ov, nv) -> data.setPosition(this.axis.getValueForDisplay(nv.doubleValue())));
        this.data.positionProperty().addListener((o, ov, nv) -> setPixelPosition(axis.getDisplayPosition(nv)));

        this.data.colorProperty().addListener((o, ov, nv) -> setStroke(nv));
        startXProperty().bindBidirectional(pixelPosition);
        endXProperty().bind(startXProperty());
        setStartY(0);
        setEndY(2000);

        this.axis.getScene().widthProperty().addListener((o, ov, nv) -> setPixelPosition(axis.getDisplayPosition(data.getPosition())));
        this.axis.lowerBoundProperty().addListener((o, ov, nv) -> setPixelPosition(axis.getDisplayPosition(data.getPosition())));
        this.axis.upperBoundProperty().addListener((o, ov, nv) -> setPixelPosition(axis.getDisplayPosition(data.getPosition())));

        setupMouseMovement();
        setStroke(this.data.getColor());
        Platform.runLater(() -> setPixelPosition(this.axis.getDisplayPosition(this.data.getPosition())));
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
