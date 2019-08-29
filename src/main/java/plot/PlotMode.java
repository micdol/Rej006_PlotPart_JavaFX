package plot;

import javafx.beans.value.ObservableValue;

public enum PlotMode {
    /**
     * All options enabled, no fancy scaling, animating
     */
    FREE,
    /**
     * Samples are added to the buffer instead to the view.
     * When buffer is filled up they are drawn to the screen and process repeats.
     */
    BUFFER,
    /**
     * Samples are added on the left of the cursors sweeping through screen.
     * Samples on the right are old data.
     */
    CURSOR,
    /**
     * Screen span is fixed and it "travels" with data
     */
    SCREEN
}
