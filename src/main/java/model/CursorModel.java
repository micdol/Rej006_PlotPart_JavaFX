package model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.paint.Color;

public class CursorModel {
    // For default name generation
    private static int NEXT_NAME = 0;

    private final StringProperty name;
    private final ObjectProperty<Number> position;
    private final ObjectProperty<CursorModel> reference;
    private final ObjectProperty<Number> delta;
    private final ObjectProperty<Color> color;

    // region Properties

    public StringProperty nameProperty() {
        return name;
    }

    public ObjectProperty<Number> positionProperty() {
        return position;
    }

    public ObjectProperty<CursorModel> referenceProperty() {
        return reference;
    }

    public ObjectProperty<Number> deltaProperty() {
        return delta;
    }

    public ObjectProperty<Color> colorProperty() {
        return color;
    }


    public String getName() {
        return name.get();
    }

    public double getPosition() {
        return position.get().doubleValue();
    }

    public CursorModel getReference() {
        return reference.get();
    }

    public double getDelta() {
        return delta.get().doubleValue();
    }

    public Color getColor() {
        return color.get();
    }


    public void setName(String name) {
        this.name.set(name);
    }

    public void setPosition(Number position) {
        this.position.set(position);
    }

    public void setReference(CursorModel reference) {
        this.reference.set(reference);
    }

    public void setDelta(double delta) {
        this.delta.set(delta);
    }

    public void setColor(Color color) {
        this.color.set(color);
    }

    // endregion

    private final ChangeListener<Number> refPosChanged;
    private final ChangeListener<Number> positionChanged;
    private final ChangeListener<Number> deltaChanged;

    public CursorModel() {
        name = new SimpleStringProperty(NEXT_NAME++ + "");
        position = new SimpleObjectProperty<>(3);
        reference = new SimpleObjectProperty<>(null);
        delta = new SimpleObjectProperty<>(0.0);
        color = new SimpleObjectProperty<>(Color.BLUE);

        // Moving reference should move this cursor, keeping delta constant
        refPosChanged = (o, oldPos, newRefPos) -> setPosition(newRefPos.doubleValue() - getDelta());

        // Changing delta should update this cursors position
        deltaChanged = (o, oldDelta, newDelta) -> setPosition(getReference().getPosition() - newDelta.doubleValue());

        // Changing position should update delta
        positionChanged = (o, oldPos, newPos) -> {
            // Not to cause stack overflow first delta listener need to be removed and re-added after change is applied
            delta.removeListener(deltaChanged);
            setDelta(getReference().getPosition() - newPos.doubleValue());
            delta.addListener(deltaChanged);
        };

        // Changing reference should setup interactions or remove them if reference is cleared
        reference.addListener((o, oldRef, newRef) -> {
            if (newRef != null) {
                setDelta(newRef.getPosition() - getPosition());
                newRef.position.addListener(refPosChanged);
                delta.addListener(deltaChanged);
                position.addListener(positionChanged);
            } else {
                oldRef.position.removeListener(refPosChanged);
                delta.removeListener(deltaChanged);
                position.removeListener(positionChanged);
            }
        });
    }

    // This makes object nicely printable inside combo boxes (when selected)
    @Override
    public String toString() {
        return getName();
    }
}
