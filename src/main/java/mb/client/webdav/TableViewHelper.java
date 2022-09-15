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
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Dragboard;
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
        
        // Row double click
        table.setRowFactory(tv -> {
            TableRow<ResourceTableItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onRowDoubleClick(row.getItem());
                }
            });
            return row ;
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
        
        // Drag and drop support
        /*
        table.setOnDragDetected((event) -> {
            System.out.println("drag detected");
            
            //WebDAVResource res = table.getSelectionModel().getSelectedItem();
            
            // TODO Allow drag and drop of downloaded files
            Dragboard db = table.startDragAndDrop(TransferMode.COPY);
            ClipboardContent cb = new ClipboardContent();
            cb.putFiles(Arrays.asList(new File("/Users/mborisov/20210703_203655.jpg")));
            db.setContent(cb);
            
            event.consume();
        });
        */
        
//        table.setOnDragDone((event) -> {
//            System.out.println("drag done -> " + event.getTarget());
//        });
        
//        table.setOnDragEntered((event) -> {
//            System.out.println("drag entered -> " + event.getTransferMode() + 
//                    " " + event.getDragboard().getFiles().size());
//            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
//            event.consume();
//        });
        
        table.setOnDragOver((event) -> {
            if(event.getGestureSource() != table && 
                    event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        
        // External file is dropped
        table.setOnDragDropped((event) -> {
            boolean success = false;
            Dragboard db = event.getDragboard();
            if(db.hasFiles()) {
                
                for (File file : db.getFiles()) {
                    UploadFileTask task = new UploadFileTask(service,  
                            tree.getSelectionModel().getSelectedItem().getValue(), file);
                    tpv.getTasks().add(0, task);
                    WebDAVUtil.startTask(task);
                }
                success = true;
            }
            
            event.setDropCompleted(success);
            event.consume();
        });
        
//        table.setOnDragExited((event) -> {
//            System.out.println("drag exited");
//        });
        
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
                event -> {
                    ComponentUtils.showResourcePropertiesDialog(
                            table.getSelectionModel().getSelectedItem().getDavRes());
                    }, 
                event -> {
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
                },
                event -> {
                    
                    ResourceTableItem item = table.getSelectionModel().getSelectedItem();
                    WebDAVResource res = item.getDavRes();
                    
                    Alert dialog = new Alert(AlertType.CONFIRMATION);
                    dialog.setTitle("Delete Resource Confirmation");
                    dialog.setHeaderText(format("Are you sure you want to delete resource ''{0}''?", 
                            res.getAbsolutePath()));
                    dialog.setContentText("Note that this cannot be undone!");

                    Optional<ButtonType> input = dialog.showAndWait();
                    if (input.get() == ButtonType.OK){

                        try {
                            service.delete(res);
                            table.getItems().remove(item);
                        } catch (WebDAVServiceException e) {
                            
                            // Show alert
                            Alert alert = new Alert(AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText(format("Failed to delete ''{0}''", res.getAbsolutePath()));
                            alert.showAndWait();
                        }
                    }
                }));
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
}
