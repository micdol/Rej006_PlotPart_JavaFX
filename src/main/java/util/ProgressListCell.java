package util;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import plot.PlotMode;

public class ProgressListCell extends ListCell<PlotMode> {

    private final StackPane spRoot;
    private final Label lblName;
    private final ProgressBar prgIndicator;

    public ProgressListCell(ProgressBar prgIndicator) {
        this.prgIndicator = prgIndicator;
        this.lblName = new Label();
        this.lblName.setTextFill(Color.BLACK);
        this.spRoot = new StackPane();
        this.spRoot.getChildren().addAll(prgIndicator, lblName);
    }

    @Override
    protected void updateItem(PlotMode item, boolean empty) {
        super.updateItem(item, empty);

        if (item == null || empty) {
            return;
        }

        if (item == PlotMode.BUFFER) {
            setText(null);
            lblName.setText(item.toString());
            setGraphic(spRoot);
        } else {
            setText(item.toString());
            setGraphic(null);
        }
    }
}
