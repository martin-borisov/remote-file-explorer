package mb.client.webdav;

import static java.text.MessageFormat.format;

import java.io.File;
import java.text.Collator;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.controlsfx.control.TaskProgressView;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import mb.client.webdav.components.ComponentUtils;
import mb.client.webdav.components.ResourceContextMenu;
import mb.client.webdav.media.MPMedia;
import mb.client.webdav.media.MPlayer;
import mb.client.webdav.model.ResourceTableItem;
import mb.client.webdav.model.WebDAVResource;
import mb.client.webdav.service.ConfigService;
import mb.client.webdav.service.WebDAVService;
import mb.client.webdav.service.WebDAVServiceException;
import mb.client.webdav.service.WebDAVUtil;
import mb.client.webdav.tasks.DownloadFileTask;
import mb.client.webdav.tasks.UploadFileTask;

public class TableViewHelper {
    
    private static final Logger LOG = Logger.getLogger(TableViewHelper.class.getName());
    private static final ConfigService config = ConfigService.getInstance();
    private static final Collator DEFAULT_COLLATOR = Collator.getInstance();
    private static final DataFormat WEBDAV_RESOURCE_PATH = new DataFormat("webdav/resourcepath");
    
    private WebDAVService service;
    private ObservableList<ResourceTableItem> fileList;
    private TreeView<WebDAVResource> tree;
    private TaskProgressView<Task<?>> tpv;
    private MPlayer player;
    private TableView<ResourceTableItem> table;
    
    public TableViewHelper(WebDAVService service, ObservableList<ResourceTableItem> fileList,
            TreeView<WebDAVResource> tree, TaskProgressView<Task<?>> tpv, MPlayer player) {
        this.service = service;
        this.fileList = fileList;
        this.tree = tree;
        this.tpv = tpv;
        this.player = player;
        createTable();
    }
    
    public TableView<ResourceTableItem> getTable() {
        return table;
    }
    
    public void setService(WebDAVService service) {
        this.service = service;
    }

    private TableView<ResourceTableItem> createTable() {
        
        table = new TableView<>();
        table.setTableMenuButtonVisible(true);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-border-width: 0; -fx-border-color: transparent; -fx-table-cell-border-color: transparent;"); // Hide border
        
        // Row double click and drag & drop
        table.setRowFactory(tv -> {
            TableRow<ResourceTableItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onRowDoubleClick(row.getItem());
                }
            });
            
            row.setOnDragDetected(event -> onRowDragDetected(row, event));
            row.setOnDragEntered(event -> onRowDragEntered(row, event));
            row.setOnDragOver(event -> onRowDragOver(row, event));
            row.setOnDragDropped(event -> onRowDragDropped(row, event));
            return row;
        });
        
        // Explicit sort is needed to preserve sorting when updating table data
        fileList.addListener(new ListChangeListener<ResourceTableItem>() {
            public void onChanged(Change<? extends ResourceTableItem> change) {
                change.next();
                
                // Don't do anything on sort (which triggers a permutation) as we end up in an endless loop
                if(!change.wasPermutated() && !change.wasRemoved()) {
                    table.sort();
                } 
            }
        });

        // Context menu
        createContextMenu();
        
        // Columns
        TableColumn<ResourceTableItem, FontIcon> iconCol = new TableColumn<>("Icon");
        iconCol.setCellValueFactory(new PropertyValueFactory<>("icon"));
        iconCol.setPrefWidth(30);
        iconCol.setSortable(false);
        iconCol.setCellFactory(col -> {

            TableCell<ResourceTableItem, FontIcon> cell = new TableCell<ResourceTableItem, FontIcon>() {
                protected void updateItem(FontIcon item, boolean empty) {
                    super.updateItem(item, empty);
                    setText("");
                    
                    if(item == null || empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(item);
                    }
                }
            };
            return cell;
        });
        
        TableColumn<ResourceTableItem, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(Double.valueOf(config.getOrCreateProperty("col.name.width", String.valueOf(300))));
        
        // Sorting by name (directory first)
        nameCol.setComparator((name1, name2) -> {
            
            int result = 0;
            
            ResourceTableItem res1 = findResourceByName(name1);
            ResourceTableItem res2 = findResourceByName(name2);
            
            // The XOR and switching res based on sort type ensures directories are always on top
            if(res1.isDirectory() ^ res2.isDirectory()) {
                result = (nameCol.getSortType() == SortType.ASCENDING ? res1 : res2).isDirectory() ? -1 : 1;
            } else {
                
                // Proper locale-based comparison
                result = DEFAULT_COLLATOR.compare(name1, name2); 
            }
            
            return result;
        });
        
        TableColumn<ResourceTableItem, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        
        TableColumn<ResourceTableItem, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(p -> {
            return new SimpleObjectProperty<String>(
                    p.getValue().getSize() >= 0 ? FileUtils.byteCountToDisplaySize(p.getValue().getSize()) : "");
        });
        
        // TODO Implement sorting by size
        
        TableColumn<ResourceTableItem, Date> createdCol = new TableColumn<>("Created");
        createdCol.setCellValueFactory(new PropertyValueFactory<>("created"));
        
        TableColumn<ResourceTableItem, Date> modifiedCol = new TableColumn<>("Modified");
        modifiedCol.setCellValueFactory(new PropertyValueFactory<>("modified"));
        
        table.getColumns().addAll(Arrays.asList(iconCol, nameCol, typeCol, sizeCol, createdCol, modifiedCol));
        table.setItems(fileList);
        return table;
    }
    
    private void onRowDoubleClick(ResourceTableItem res) {
        if(res.isDirectory()) {
            
            // Open directories
            TreeItem<WebDAVResource> selItem = tree.getSelectionModel().getSelectedItem();
            if(selItem != null) {
                if(selItem.isExpanded()) {
                    selectTreeItemIfSameAsTableItem(res, selItem);
                } else {
                    selItem.setExpanded(true);
                
                    // React when new items get added to the tree node (this happens asynchronously)
                    selItem.getChildren().addListener(new ListChangeListener<TreeItem<WebDAVResource>>() {
                        public void onChanged(Change<? extends TreeItem<WebDAVResource>> change) {
                            change.next();
                            if(change.wasAdded()) {
                                selectTreeItemIfSameAsTableItem(res, selItem);
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
    
    private void selectTreeItemIfSameAsTableItem(ResourceTableItem tableItem,  TreeItem<WebDAVResource> treeItem) {
        
        // Select item which was double clicked
        for (TreeItem<WebDAVResource> childItem : treeItem.getChildren()) {
            if(tableItem.getName().equals(childItem.getValue().getName())) {
                tree.getSelectionModel().select(childItem);
                break;
            }
        }
    }
    
    private void createContextMenu() {
        table.setContextMenu(new ResourceContextMenu(
                event -> onShowSelectedResourceProperties(), 
                event -> onAddSelectedResourceToPlaylist(),
                event -> onDeleteSelectedResource(),
                event -> onCreateDirectory()));
    }
    
    private ResourceTableItem findResourceByName(String name) {
        ObservableList<ResourceTableItem> items = table.getItems();
        return items.stream()
                .filter(item -> name.equals(item.getName()))
                .findAny()
                .orElse(null);

    }
    
    private void addResourceToPlaylistIfMedia(WebDAVResource res) {
        MPMedia media = WebDAVUtil.mpMediaFromWebDAVResource(res);
        if (media != null && WebDAVUtil.isAudioMedia(res)) {
            media.setUser(service.getHost().getUser());
            media.setPassword(service.getHost().getPassword());
            player.addToPlaylist(media);
        }
    }
    
    private void onShowSelectedResourceProperties() {
        ComponentUtils.showResourcePropertiesDialog(
                table.getSelectionModel().getSelectedItem().getDavRes());
    }
    
    private void onAddSelectedResourceToPlaylist() {
        WebDAVResource res = table.getSelectionModel().getSelectedItem().getDavRes();
        if (player != null) {
            if (res.isDirectory()) {

                List<WebDAVResource> files;
                try {
                    
                    // TODO This should be an async task
                    files = service.listFiles(res.getAbsolutePath());
                } catch (WebDAVServiceException e) {
                    LOG.log(Level.WARNING, "Listing files failed", e);
                    return;
                }

                files.stream().forEachOrdered(this::addResourceToPlaylistIfMedia);
            } else {
                addResourceToPlaylistIfMedia(res);
            }
        }
    }
    
    private void onDeleteSelectedResource() {
        ResourceTableItem item = table.getSelectionModel().getSelectedItem();
        
        // Check for empty row
        if(item == null) {
            return;
        }
        
        WebDAVResource res = item.getDavRes();
        Alert dialog = ComponentUtils.createResourceDeletionDialog(res.getAbsolutePath());
        Optional<ButtonType> input = dialog.showAndWait();
        if (input.get() == ButtonType.OK){
            try {
                service.delete(res);
                table.getItems().remove(item);
            } catch (WebDAVServiceException e) {
                String msg = format("Failed to delete ''{0}''", res.getAbsolutePath());
                LOG.log(Level.WARNING, msg, e);
                
                Alert alert = ComponentUtils.createAlertDialog(AlertType.ERROR, "Error", msg, e.getMessage());
                alert.showAndWait();
            }
        }
    }
    
    private void onCreateDirectory() {
        ResourceTableItem item = table.getSelectionModel().getSelectedItem();
        
        // If row is empty create in parent directory
        WebDAVResource res = item != null ? 
                item.getDavRes() : tree.getSelectionModel().getSelectedItem().getValue();
        
        TextInputDialog dialog = ComponentUtils.createTextInputDialog("Create Directory", 
                format("Create in ''{0}''", res.getAbsolutePath()), "Directory Name:", "New Folder");
        dialog.showAndWait().ifPresent(name -> {
            try {
                
                // Execute create
                String dirPath = service.createDirectory(res, name);
                
                // Show created directory
                List<WebDAVResource> dirRes = service.list(dirPath, 0);
                fileList.add(new ResourceTableItem(dirRes.get(0)));
                
            } catch (WebDAVServiceException e) {
                String msg = format("Failed to create directory ''{0}''", res.getAbsolutePath());
                LOG.log(Level.WARNING, msg, e);
                
                Alert alert = ComponentUtils.createAlertDialog(AlertType.ERROR, "Error", msg, e.getMessage());
                alert.showAndWait();
            }
        });
    }
    
    /* Row drag & drop */
    
    private void onRowDragDetected(TableRow<ResourceTableItem> row, MouseEvent event) {
        if(!row.isEmpty()) {

            WebDAVResource res = row.getItem().getDavRes();
            LOG.fine(format("Drag from row ''{0}'' detected", res.getAbsolutePath()));

            Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent cb = new ClipboardContent();
            cb.put(WEBDAV_RESOURCE_PATH, res.getAbsolutePath());
            db.setContent(cb);
            event.consume();
        }
    }
    
    private void onRowDragEntered(TableRow<ResourceTableItem> row, DragEvent event) {
        
        // Indicate drag is happening on directories only
        if(!row.isEmpty() && row.getItem().getDavRes().isDirectory()) {
            table.getSelectionModel().select(row.getIndex());
            event.consume();
        }
    }
    
    private void onRowDragOver(TableRow<ResourceTableItem> row, DragEvent event) {
        Dragboard db = event.getDragboard();
        
        // Allow drops from OS by not excluding empty rows
        if(row.isEmpty()) {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            event.consume();
        } else if(row.getItem().getDavRes().isDirectory()) {
            if(db.hasContent(WEBDAV_RESOURCE_PATH) &&
                    !((String) db.getContent(WEBDAV_RESOURCE_PATH)).equals(row.getItem().getDavRes().getAbsolutePath())) {
                event.acceptTransferModes(TransferMode.MOVE);
                event.consume();
            } else {
                // TODO Accept drop OS file in directory represented by this row
            }
        }
    }
    
    private void onRowDragDropped(TableRow<ResourceTableItem> row, DragEvent event) {
        Dragboard db = event.getDragboard();
        if(row.isEmpty()) {
            if (db.hasFiles()) {

                // Upload OS file
                boolean success = false;
                if (db.hasFiles()) {

                    for (File file : db.getFiles()) {
                        LOG.fine(format("''{0}'' dropped on empty row -> attempt UPLOAD", file.getAbsolutePath()));

                        UploadFileTask task = new UploadFileTask(service,
                                tree.getSelectionModel().getSelectedItem().getValue(), file);
                        tpv.getTasks().add(0, task);
                        WebDAVUtil.startTask(task);

                        // Do stuff when upload is done
                        task.setOnSucceeded(value -> {
                            LOG.fine(format("Upload of ''{0}'' finished at ''{1}''", file.getAbsolutePath(),
                                    task.getMessage()));

                            // Show uploaded resource
                            try {
                                List<WebDAVResource> res = service.list(task.getMessage(), 0);
                                fileList.add(new ResourceTableItem(res.get(0)));
                            } catch (WebDAVServiceException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                    success = true;
                }

                event.setDropCompleted(success);
                event.consume();
            }
            
        } else if(row.getItem().getDavRes().isDirectory()) {
            WebDAVResource dest = row.getItem().getDavRes();
            
            // Probe move resource or copy OS file
            if(db.hasContent(WEBDAV_RESOURCE_PATH) &&
                !((String) db.getContent(WEBDAV_RESOURCE_PATH)).equals(dest.getAbsolutePath())) {
                
                // Move
                String srcAbsolutePath = (String) event.getDragboard().getContent(WEBDAV_RESOURCE_PATH);
                LOG.fine(format("''{0}'' dropped on row ''{1}'' -> attempt MOVE", srcAbsolutePath, row.getItem()));
                
                Alert dialog = ComponentUtils.createResourceMoveDialog(srcAbsolutePath);
                Optional<ButtonType> input = dialog.showAndWait();
                if (input.get() == ButtonType.OK){
                    
                    // Do move operation
                    try {
                        List<WebDAVResource> list = service.list(srcAbsolutePath, 0);
                        if(list.size() == 1) {
                            WebDAVResource src = list.get(0);
                            service.move(src, row.getItem().getDavRes());
                            Optional.of(findResourceByName(src.getName())).ifPresent(r -> fileList.remove(r));
                            
                        } else {
                            throw new WebDAVServiceException(format("Resource ''{0}'' not found", srcAbsolutePath));
                        }
                    } catch (WebDAVServiceException e) {
                        LOG.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
                
                event.setDropCompleted(true);
                event.consume();
                
            } else {
                // TODO Upload in directory
            }
        }
    }
}
