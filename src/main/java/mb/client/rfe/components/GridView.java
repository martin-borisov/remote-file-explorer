package mb.client.rfe.components;

import static java.text.MessageFormat.format;

import java.awt.MouseInfo;
import java.awt.Point;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.controlsfx.control.TaskProgressView;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.concurrent.Task;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import mb.client.rfe.model.ResourceTableItem;
import mb.client.rfe.model.WebDAVResource;
import mb.client.rfe.service.ResourceRepositoryService;
import mb.client.rfe.service.WebDAVServiceException;
import mb.client.rfe.service.WebDAVUtil;
import mb.client.rfe.tasks.DownloadFileTask;
import mb.client.rfe.tasks.LoadThumbsTask;

public class GridView extends TilePane {
    
    private static final Logger LOG = Logger.getLogger(GridView.class.getName());
    
    private ResourceRepositoryService service;
    private ObservableList<ResourceTableItem> fileList;
    private TreeView<WebDAVResource> tree;
    private TaskProgressView<Task<?>> tpv;
    private LoadThumbsTask currentLoadThumbsTask;
    private VBox selectedItem;
    private SimpleStringProperty messageProperty;
    private SimpleDoubleProperty progressProperty;

    public GridView(ResourceRepositoryService service, ObservableList<ResourceTableItem> fileList, 
            TreeView<WebDAVResource> tree, TaskProgressView<Task<?>> tpv, double hgap, double vgap) {
        super(hgap, vgap);
        setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        this.service = service;
        this.fileList = fileList;
        this.tree = tree;
        this.tpv = tpv;
        createProperties();
        createListeners();
    }
    
    private void createProperties() {
        messageProperty = new SimpleStringProperty();
        progressProperty = new SimpleDoubleProperty();
    }
    
    private void createListeners() {
        this.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onRowDoubleClick((ResourceTableItem) this.getSelectedItem().getUserData());
            }
        });
        
        fileList.addListener(new ListChangeListener<ResourceTableItem>() {
            public void onChanged(Change<? extends ResourceTableItem> c) {
                onFileListChanged(c);
            }
        });
    }
    
    public ReadOnlyStringProperty thumbLoadingMessageProperty() {
        return messageProperty;
    }
    
    public ReadOnlyDoubleProperty thumbLoadingProgressProperty() {
        return progressProperty;
    }

    public Node addItem(String label, FontIcon icon, int width, ResourceTableItem item) {
        
        // Default icon
        icon.setIconSize(width);
        
        // Label
        Label lbl = new Label(label);
        lbl.setMaxWidth(width);
        lbl.setWrapText(true);
        lbl.setTooltip(new Tooltip(label));
        
        VBox box = new VBox(icon, lbl);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(10));
        box.setUserData(item);
        getChildren().add(box);
        
        // Grid selection
        box.setOnMousePressed(event -> {
            deselectPrevious();
            selectedItem = box;
            markSelected(box);
            
            // Show context menu on secondary mouse button
            if(event.getButton() == MouseButton.SECONDARY) {
                WebDAVResource res = item.getDavRes();
                
                ResourceContextMenu menu = new ResourceContextMenu(
                        evt -> {
                            ComponentUtils.showResourcePropertiesDialog(res);
                            }, 
                        null, 
                        evt -> {
                            Alert dialog = ComponentUtils.createResourceDeletionDialog(res.getAbsolutePath());
                            Optional<ButtonType> input = dialog.showAndWait();
                            if (input.get() == ButtonType.OK){
                                try {
                                    service.delete(res);
                                    fileList.remove(item);
                                } catch (WebDAVServiceException e) {
                                    
                                    // Show alert
                                    Alert alert = new Alert(AlertType.ERROR);
                                    alert.setTitle("Error");
                                    alert.setHeaderText(format("Failed to delete ''{0}''", res.getAbsolutePath()));
                                    alert.showAndWait();
                                }
                            }
                        },
                        null
                    );
                
                Point loc = MouseInfo.getPointerInfo().getLocation();
                menu.show(box, loc.getX(), loc.getY());
            }
        });
        
        return box;
    }
    
    public void removeItem(ResourceTableItem item) {
        getChildren().removeIf(n -> n.getUserData() == item);
    }

    public VBox getSelectedItem() {
        return selectedItem;
    }
    
    public void setService(ResourceRepositoryService service) {
        this.service = service;
    }
    
    private void markSelected(VBox box) {
        box.setBackground(new Background(new BackgroundFill(Color.DARKGREY, new CornerRadii(5), Insets.EMPTY)));
    }
    
    private void deselectPrevious() {
        if(selectedItem != null) {
            selectedItem.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
        }
    }
    
    private void onRowDoubleClick(ResourceTableItem res) {
        if(res.isDirectory()) {
            
            // Open directories
            TreeItem<WebDAVResource> selItem = tree.getSelectionModel().getSelectedItem();
            if(selItem != null) {
                if(selItem.isExpanded()) {
                    selectTreeItemIfSameAsGridItem(res, selItem);
                } else {
                    selItem.setExpanded(true);
                
                    // React when new items get added to the tree node (this happens asynchronously)
                    selItem.getChildren().addListener(new ListChangeListener<TreeItem<WebDAVResource>>() {
                        public void onChanged(Change<? extends TreeItem<WebDAVResource>> change) {
                            change.next();
                            if(change.wasAdded()) {
                                selectTreeItemIfSameAsGridItem(res, selItem);
                            }
                        }
                    });
                }
            }
            
        } else {
            
            // Download files
            DownloadFileTask task = new DownloadFileTask(service,  res.getDavRes());
            tpv.getTasks().add(0, task);
            WebDAVUtil.startTask(task);
        }
    }
    
    private void selectTreeItemIfSameAsGridItem(ResourceTableItem tableItem,  TreeItem<WebDAVResource> treeItem) {
        
        // Select item which was double clicked
        for (TreeItem<WebDAVResource> childItem : treeItem.getChildren()) {
            if(tableItem.getName().equals(childItem.getValue().getName())) {
                tree.getSelectionModel().select(childItem);
                break;
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void onFileListChanged(Change<? extends ResourceTableItem> change) {

        // This is needed before inspecting the change
        change.next();
        
        // React on addition or reordering
        if(change.wasAdded() || change.wasPermutated()) {
            List<? extends ResourceTableItem> list = change.getList();
        
            // TODO Make width configurable
            final int width = 100;
            getChildren().clear();
            for (ResourceTableItem res : list) {
                addItem(res.getName(), res.getIcon(), width, res);
            }
            
            stopLoadThumbsTaskIfRunning();
            startLoadThumbsTask((List<ResourceTableItem>) list, width);
            
        } else if(change.wasRemoved()) {
            change.getRemoved().forEach(this::removeItem);
        }
    }
    
    
    private void startLoadThumbsTask(List<ResourceTableItem> list, int width) {
        
        // Load image thumbs
        ObservableMap<ResourceTableItem, Image> thumbMap = FXCollections.observableHashMap();
        currentLoadThumbsTask = new LoadThumbsTask(service, list, thumbMap, width);

        // Replace the icons with thumbs as they get loaded
        thumbMap.addListener(new MapChangeListener<ResourceTableItem, Image>() {
            public void onChanged(Change<? extends ResourceTableItem, ? extends Image> change) {
                if(change.wasAdded()) {
                    LOG.fine(format("Thumb map updated; size: {0}", change.getMap().size()));
                    findAndUpdateGridThumbWithImage(change.getKey(), change.getValueAdded());
                }
            }
        });
        
        // Reset progress when done
        currentLoadThumbsTask.setOnSucceeded(evt -> {
            messageProperty.setValue(null);
            progressProperty.setValue(0);
        });
        
        // Show message and progress on status bar while loading
        currentLoadThumbsTask.messageProperty().addListener((obs, oldVal, newVal) -> {
            messageProperty.setValue(newVal);
        });
        currentLoadThumbsTask.progressProperty().addListener((obs, oldVal, newVal) -> {
            progressProperty.setValue(newVal);
        });
        
        // Trigger the task
        WebDAVUtil.startTask(currentLoadThumbsTask);
    }
    
    private void stopLoadThumbsTaskIfRunning() {
        if(currentLoadThumbsTask != null) {
            currentLoadThumbsTask.cancel();
        }
    }
    
    /**
     * Updates the icon of the given node represented by a {@link ResourceTableItem} with a thumb
     */
    private void findAndUpdateGridThumbWithImage(ResourceTableItem res, Image image) {
        getChildren().stream().filter(node -> node.getUserData().equals(res)).findFirst().ifPresent((node) -> {
            Platform.runLater(() -> {
                ((VBox) node).getChildren().remove(0);
                ((VBox) node).getChildren().add(0, new ImageView(image));
            });
        });
    }
}
