package controllers;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import model.CursorModel;
import util.CursorManager;
import util.D;
import util.NumberSpinnerValueFactory;
import util.NumberStringConverter;

public class CursorViewController {

    // region FXML Controls

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private TitledPane tpRoot;

    @FXML
    private Button btnColor;

    @FXML
    private ComboBox<CursorModel> cbReference;

    @FXML
    private Button btnClearReference;

    @FXML
    private Spinner<Number> spPosition;

    @FXML
    private Spinner<Number> spDelta;

    @FXML
    private Button btnDelete;

    // endregion

    public TitledPane getViewRoot() {
        return tpRoot;
    }

    private CursorModel data;

    public CursorModel getData() {
        return data;
    }

    public void setData(CursorModel newData) {
        if (data != null) {
            D.error(CursorViewController.this, "Cursor was already assigned for controller");
            throw new IllegalStateException("Cursor was already assigned for controller");
        }

        D.info(CursorViewController.this, "Assigning cursor: " + newData);

        data = newData;

        tpRoot.setText(data.getName());

        spDelta.getValueFactory().valueProperty().bindBidirectional(data.deltaProperty());

        spPosition.getValueFactory().valueProperty().bindBidirectional(data.positionProperty());

        cbReference.setItems(CursorManager.getInstance().getReferencesFor(data));
        cbReference.setCellFactory(lv -> new ListCell<CursorModel>() {
            @Override
            protected void updateItem(CursorModel item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null || empty ? null : item.getName());
            }
        });
        cbReference.getSelectionModel().select(data.getReference());
        cbReference.valueProperty().bindBidirectional(data.referenceProperty());
        data.referenceProperty().addListener((o, ov, nv) -> {
            if (nv == null) cbReference.valueProperty().set(null);
        });

        btnColor.backgroundProperty().bind(Bindings.createObjectBinding(() -> new Background(new BackgroundFill(newData.getColor(), null, null)), newData.colorProperty()));

        D.info(CursorViewController.this, "Assigned cursor: " + newData);
    }

    @FXML
    void onClearReferenceClicked(ActionEvent event) {
        D.info(CursorViewController.this, "Clearing reference for cursor: " + data);

        cbReference.getSelectionModel().select(null);
        cbReference.valueProperty().set(null);

        D.info(CursorViewController.this, "Cleared reference for cursor: " + data);
    }

    @FXML
    void onColorClicked(ActionEvent event) {
        D.info(CursorViewController.this, "Changing color cursor: " + data);

        Dialog<Color> d = new Dialog<>();
        d.setTitle("Color Dialog");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        HBox hbox = new HBox();
        hbox.setSpacing(5);
        ColorPicker colorPicker = new ColorPicker(data.getColor());
        Label label = new Label("Kolor:");
        HBox.setMargin(label, new Insets(5));
        hbox.getChildren().addAll(label, colorPicker);
        d.getDialogPane().setContent(hbox);
        d.setResultConverter(btn -> btn == ButtonType.OK ? colorPicker.getValue() : null);
        d.showAndWait().ifPresent(c -> data.setColor(c));

        D.info(CursorViewController.this, "Changed color cursor: " + data + ", color: " + data.getColor());
    }

    @FXML
    void onDeleteClicked(ActionEvent event) {
        D.info(CursorViewController.this, "Deleting cursor: " + data);
        CursorManager.getInstance().unregister(data);
        D.info(CursorViewController.this, "Deleted cursor: " + data);
    }

    @FXML
    void initialize() {
        D.info(CursorViewController.this, "Initializing");

        spDelta.disableProperty().bind(cbReference.getSelectionModel().selectedItemProperty().isNull());

        final NumberSpinnerValueFactory positionFactory = new NumberSpinnerValueFactory(-1000, 1000, 0, 0.05);
        positionFactory.setConverter(new NumberStringConverter());
        spPosition.setValueFactory(positionFactory);
        spPosition.addEventHandler(ScrollEvent.SCROLL, e -> {
            if (e.getDeltaY() > 0) spPosition.increment();
            else spPosition.decrement();
        });

        final NumberSpinnerValueFactory deltaFactory = new NumberSpinnerValueFactory(-1000, 1000, 0, 0.05);
        deltaFactory.setConverter(new NumberStringConverter());
        spDelta.setValueFactory(deltaFactory);
        spPosition.addEventHandler(ScrollEvent.SCROLL, e -> {
            if (e.getDeltaY() > 0) spDelta.increment();
            else spDelta.decrement();
        });

        D.info(CursorViewController.this, "Initialized");
    }
}
