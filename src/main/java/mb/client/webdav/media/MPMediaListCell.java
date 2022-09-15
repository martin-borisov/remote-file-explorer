package mb.client.webdav.media;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.Charset;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import mb.client.webdav.components.Icons;

public class MPMediaListCell extends ListCell<MPMedia> {
    
    public MPMediaListCell() {
        super();
        setupDragAndDrop();
    }
    
    protected void updateItem(MPMedia media, boolean empty, MPMedia currentlyPlayingMedia) {
        super.updateItem(media, empty);
        
        if (empty) {
            setGraphic(null);
            setText(null);
            setTooltip(null);
        } else if(media != null) {
            
            // Title
            Text titleText = new Text(media.getName());
            titleText.setStyle("-fx-font-weight: bold");
            
            // Source
            String source = URLDecoder.decode(media.getSource(), Charset.defaultCharset());
            //Text sourceText = new Text(source);
            //sourceText.setStyle("-fx-fill: dimgrey");
            
            // Layout
            HBox hBox = new HBox(titleText);
            hBox.setSpacing(5);
            hBox.setAlignment(Pos.BASELINE_LEFT);
            
            //VBox vBox = new VBox(hBox, sourceText);
            //vBox.setStyle("-fx-spacing: 5");
            
            // Playing
            if(media.equals(currentlyPlayingMedia)) {
                hBox.getChildren().add(0, Icons.play());
            }
            
            setGraphic(hBox);
            setText(null);
            setTooltip(new Tooltip(source));
        }
    }
    
    private void setupDragAndDrop() {
        
        setOnDragDetected(event -> {
            if (!isEmpty()) {
                Dragboard db = startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(getListView().getItems().indexOf(getItem())));
                db.setContent(content);
                event.consume();
            }
        });
        
        setOnDragOver(event -> {
            if (event.getGestureSource() != this &&
                   (event.getDragboard().hasString() || event.getDragboard().hasFiles())) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        
        setOnDragDropped(event -> {
            boolean success = false;
            Dragboard db = event.getDragboard();
            if(db.hasString()) {
                int idx = Integer.valueOf(db.getString());
                ObservableList<MPMedia> items = getListView().getItems();
                MPMedia movedItem = items.get(idx);
                if (isEmpty()) {

                    // Move item to end of list
                    items.remove(movedItem);
                    items.add(movedItem);
                    
                } else {
                    
                    // Move item before current
                    items.remove(movedItem);
                    int newIdx = getIndex() > idx ? getIndex() - 1 : getIndex();
                    items.add(newIdx, movedItem);
                }
                getListView().getSelectionModel().select(movedItem);
                success = true;
            } else if(db.hasFiles()) {
                for (File file : db.getFiles()) {
                    
                    // Make sure we add audio files only
                    if (file.isFile() && MPUtils.isMedia(file)) {
                        MPMedia media = new MPMedia(file.getName(), file.toURI().toString(), MPMedia.Type.AUDIO);
                        if (isEmpty()) {
                            getListView().getItems().add(media);
                        } else {
                            getListView().getItems().add(getIndex(), media);
                        }
                    }
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
        
        setOnDragEntered(event -> {
            if (event.getGestureSource() != this) {
                Dragboard db = event.getDragboard();
                if(db.hasString() || 
                        (db.hasFiles() && db.getFiles().stream().anyMatch(file -> MPUtils.isMedia(file)))) {
                    setStyle("-fx-border-style: solid none none none; -fx-border-width: 5; -fx-border-color: lightblue;");
                }
            }
        });
        
        setOnDragExited(event -> {
            if (event.getGestureSource() != this &&
                    (event.getDragboard().hasString() || event.getDragboard().hasFiles())) {
                setStyle("-fx-border-style: none none none none;");
            }
        });
    }

}
