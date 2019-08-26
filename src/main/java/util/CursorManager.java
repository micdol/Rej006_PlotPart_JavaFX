package util;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import model.CursorModel;
import sun.plugin.dom.exception.InvalidStateException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CursorManager {

    /**
     * All available (registered) cursors
     */
    private final ObservableList<CursorModel> cursorPool;

    /**
     * Read-only wrapper for all available cursors
     */
    private final ObservableList<CursorModel> unmodifiableCursorPool;

    /**
     * Map holding list of references for each of the available cursors
     */
    private final Map<CursorModel, ObservableList<CursorModel>> refMap;

    /**
     * Listener monitoring whether reference changed at any cursor.
     * Change requires recalculation of possible references for each cursor.
     */
    private final ChangeListener<CursorModel> refChanged;

    public ObservableList<CursorModel> unmodifiableCursorPool() {
        return unmodifiableCursorPool;
    }

    /**
     * @param cursor
     * @return ObservableList of all available references
     */
    public ObservableList<CursorModel> getReferencesFor(CursorModel cursor) {
        return refMap.get(cursor);
    }

    /**
     * Registers a cursor to the pool of all cursors. Required if cursor should be available as possible reference
     * for other cursors.
     *
     * @param cursor
     */
    public void register(CursorModel cursor) {
        if (cursor == null) {
            D.error(CursorManager.this, "Try to register null cursor");
            throw new NullPointerException("Try to register null cursor");
        }
        if (cursorPool.indexOf(cursor) >= 0) {
            D.error(CursorManager.this, "Try to register already registered cursor: " + cursor);
            throw new InvalidStateException("Try to register already registered cursor: " + cursor);
        }

        D.info(CursorManager.this, "Registering cursor: " + cursor);

        ObservableList<CursorModel> references = FXCollections.observableArrayList();
        refMap.put(cursor, references);
        cursorPool.add(cursor);
        cursor.referenceProperty().addListener(refChanged);

        D.info(CursorManager.this, "Registered cursor: " + cursor);
    }

    /**
     * Unregisters cursor from the cursor pool making it unavailable as possible reference for other cursors.
     * for other cursors.
     *
     * @param cursor
     */
    public void unregister(CursorModel cursor) {
        if (cursor == null) {
            D.error(CursorManager.this, "Try to unregister null cursor");
            throw new NullPointerException("Try to unregister null cursor");
        }

        if (cursorPool.indexOf(cursor) < 0) {
            D.error(CursorManager.this, "Try to unregister cursor already unregistered or not registered at all: " + cursor);
            throw new InvalidStateException("Try to unregister cursor already unregistered or not registered at all: " + cursor);
        }

        D.info(CursorManager.this, "Unregistering cursor: " + cursor);

        // Reset references for all cursors referencing cursor being unregistered
        // "what happened, happened and couldn't have happened any other way" xD
        cursorPool.stream()
                .filter(c -> c.getReference() == cursor)
                .forEach(c -> c.setReference(null));
        cursorPool.remove(cursor);
        refMap.remove(cursor);
        cursor.referenceProperty().removeListener(refChanged);

        D.info(CursorManager.this, "Unregistered cursor: " + cursor);
    }

    private void recalculateReferences() {
        for (CursorModel cursor : cursorPool) {
            final ObservableList<CursorModel> currRefs = refMap.get(cursor);
            final CursorModel currRef = cursor.getReference();

            D.info(CursorManager.this, "Recalculating references for: " + cursor + ", selected: " + currRef + ", available: [" + currRefs.stream().map(CursorModel::getName).collect(Collectors.joining(",")) + "]");

            final List<CursorModel> newRefs = cursorPool
                    .stream()
                    .filter(c -> canReference(c, cursor))
                    .collect(Collectors.toList());

            // Clear cannot be used since it breaks bindings if ObservableList is eg. bound to ComboBox (would clear selection)
            currRefs.removeIf(r -> !newRefs.contains(r));
            newRefs.stream()
                    .filter(r -> !currRefs.contains(r))
                    .forEach(currRefs::add);

            D.info(CursorManager.this, "Recalculated references for: " + cursor + ", selected: " + currRef + ", available: [" + currRefs.stream().map(CursorModel::getName).collect(Collectors.joining(",")) + "]");
        }
    }

    private boolean canReference(CursorModel potentialReference, CursorModel cursor) {
        if (potentialReference == cursor) return false;
        if (potentialReference.getReference() == null) return true;
        return canReference(potentialReference.getReference(), cursor);
    }

    //region Singleton

    private CursorManager() {
        if (__Holder.INSTANCE != null) {
            D.error(CursorManager.this, "Instance already constructed");
            throw new IllegalStateException(CursorManager.class.getName() + " already constructed");
        }

        cursorPool = FXCollections.observableArrayList();
        unmodifiableCursorPool = FXCollections.unmodifiableObservableList(cursorPool);
        refMap = new HashMap<>();
        ListChangeListener<CursorModel> cursorPoolChanged = change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    D.info(CursorManager.this, "Cursor pool changed, added: [" + change.getAddedSubList().stream().map(CursorModel::getName).collect(Collectors.joining(",")) + "]");
                    recalculateReferences();
                }
                if (change.wasRemoved()) {
                    D.info(CursorManager.this, "Cursor pool changed, removed [" + change.getRemoved().stream().map(CursorModel::getName).collect(Collectors.joining(",")) + "]");
                    recalculateReferences();
                }
            }
        };
        cursorPool.addListener(cursorPoolChanged);
        refChanged = (o, nv, ov) -> {
            D.info(CursorManager.this, "Reference changed from: " + ov + " to: " + nv);
            recalculateReferences();
        };
    }

    public static CursorManager getInstance() {
        return __Holder.INSTANCE;
    }

    private static class __Holder {
        private static final CursorManager INSTANCE = new CursorManager();
    }

    //endregion
}
