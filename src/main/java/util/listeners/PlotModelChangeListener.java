package util.listeners;

import com.sun.istack.internal.Nullable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ProgressBar;
import plot.models.BufferModeModel;
import plot.models.PlotModeModelBase;

public class PlotModelChangeListener implements ChangeListener<PlotModeModelBase> {

    private final ProgressBar bufferFillProgressBar;

    /**
     * @param bufferFillProgressBar - progress bar to synchronize with buffer fill in {@link BufferModeModel}
     */
    public PlotModelChangeListener(@Nullable ProgressBar bufferFillProgressBar) {
        this.bufferFillProgressBar = bufferFillProgressBar;
    }

    @Override
    public void changed(ObservableValue<? extends PlotModeModelBase> observable, PlotModeModelBase oldModel, PlotModeModelBase newModel) {
        unbindFromProgressBar(oldModel);
        bindToProgressBar(newModel);
        newModel.reset();
    }

    private void unbindFromProgressBar(PlotModeModelBase model) {
        if (model instanceof BufferModeModel && bufferFillProgressBar != null) {
            bufferFillProgressBar
                    .progressProperty()
                    .unbind();
        }
    }

    private void bindToProgressBar(PlotModeModelBase model) {
        if (model instanceof BufferModeModel && bufferFillProgressBar != null) {
            BufferModeModel bufferModel = (BufferModeModel) model;
            bufferFillProgressBar
                    .progressProperty()
                    .bind(bufferModel.bufferFillProperty());
        }
    }
}
