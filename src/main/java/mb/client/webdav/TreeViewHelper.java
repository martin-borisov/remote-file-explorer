package mb.client.webdav;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import mb.client.webdav.components.ComponentUtils;
import mb.client.webdav.components.Icons;
import mb.client.webdav.components.ResourceContextMenu;
import mb.client.webdav.model.ResourceTableItem;
import mb.client.webdav.model.WebDAVHost;
import mb.client.webdav.model.WebDAVResource;
import mb.client.webdav.service.WebDAVService;
import mb.client.webdav.service.WebDAVServiceException;
import mb.client.webdav.service.WebDAVUtil;
import mb.client.webdav.tasks.ListDirsTask;

public class TreeViewHelper {
    
    private static final Logger LOG = Logger.getLogger(TreeViewHelper.class.getName());
    
    private WebDAVService service;
    private ObservableList<ResourceTableItem> fileList;
    private HostMgmtHelper hostsHelper;
    private TreeView<WebDAVResource> tree;
    
    public TreeViewHelper(WebDAVService service, 
            ObservableList<ResourceTableItem> fileList, HostMgmtHelper hostsHelper) {
        this.service = service;
        this.fileList = fileList;
        this.hostsHelper = hostsHelper;
        tree = createTree(
                (obs, oldVal, newVal) -> onTreeItemSelect(newVal),
                (event) -> onTreeNodeExpand(event.getSource()),
                (event) -> onTreeNodeCollapse(event.getSource()));
    }
    
    public TreeView<WebDAVResource> getTree() {
        return tree;
    }
    
    public void setService(WebDAVService service) {
        this.service = service;
    }
    
    public void updateRoot(WebDAVHost host, WebDAVService service) {
        this.service = service;
        TreeItem<WebDAVResource> rootItem = createRootItem();
        rootItem.setValue(WebDAVUtil.webDAVResourceFromHost(host));
        tree.setRoot(rootItem);
    }
    
    public void clearTree() {
        this.service = null;
        tree.setRoot(null);
    }
    
    public void selectParent() {
        TreeItem<WebDAVResource> selected = tree.getSelectionModel().getSelectedItem();
        if (selected != null) {
            TreeItem<WebDAVResource> parent = 
                    tree.getSelectionModel().getSelectedItem().getParent();
            if (parent != null) {
                tree.getSelectionModel().select(parent);
            }
        }
    }
    
    public WebDAVResource getCurrentDirectory() {
        WebDAVResource res = null;
        TreeItem<WebDAVResource> selected = tree.getSelectionModel().getSelectedItem();
        if(selected != null) {
            res = selected.getValue();
        }
        return res;
    }
    
    public boolean navigateTo(String absolutePath) {
        String[] splitPath = WebDAVUtil.pathToElements(absolutePath);
        if(splitPath.length > 0) {
            TreeItem<WebDAVResource> root = tree.getRoot();
            if(root.getValue().getAbsolutePath().equals(splitPath[0])) {
                expandItemSync(root, splitPath, 1);
            }
        }
        return true;
    }
    
    private void expandItemSync(TreeItem<WebDAVResource> item, String[] childItemNames, int idx) {
        ListChangeListener<TreeItem<WebDAVResource>> listener = new ListChangeListener<TreeItem<WebDAVResource>>() {
            public void onChanged(Change<? extends TreeItem<WebDAVResource>> c) {
                c.next();
                if(c.wasAdded()) {
                
                    // Done loading child items, remove listener
                    item.getChildren().removeListener(this);
                
                    // Find and expand child or select item if last in chain
                    if(idx < childItemNames.length) {
                        String childItemName = URLDecoder.decode(childItemNames[idx], Charset.defaultCharset());
                        for (TreeItem<WebDAVResource> child : item.getChildren()) {
                            if(childItemName.equals(child.getValue().getName())) {
                                expandItemSync(child, childItemNames, idx + 1);
                            }
                        }
                    }
                }
            }
        };
        item.getChildren().addListener(listener);
        item.setExpanded(true);
        tree.getSelectionModel().select(item);
    }
    
    private TreeItem<WebDAVResource> createRootItem() {
        TreeItem<WebDAVResource> rootItem = new TreeItem<WebDAVResource>() {
            public boolean isLeaf() {
                return false;
            }
        };
        rootItem.setGraphic(Icons.server());
        
        // Node expand and collapse handler
        rootItem.addEventHandler(TreeItem.branchExpandedEvent(), 
                (EventHandler<TreeItem.TreeModificationEvent<WebDAVResource>>) (event) -> onTreeNodeExpand(event.getSource()));
        rootItem.addEventHandler(TreeItem.branchCollapsedEvent(), 
                (EventHandler<TreeItem.TreeModificationEvent<WebDAVResource>>) (event) -> onTreeNodeCollapse(event.getSource()));
        
        return rootItem;
    }

    private TreeView<WebDAVResource> createTree(ChangeListener<TreeItem<WebDAVResource>> selectionListener,
            EventHandler<TreeItem.TreeModificationEvent<WebDAVResource>> nodeExpandHandler,
            EventHandler<TreeItem.TreeModificationEvent<WebDAVResource>> nodeCollapseHandler) {
        
        // Tree
        tree = new TreeView<WebDAVResource>();
        
        // Node selection listener
        tree.getSelectionModel().selectedItemProperty().addListener(selectionListener);
        createContextMenu();
        return tree;
    }
    
    private void createContextMenu() {   
        tree.setContextMenu(new ResourceContextMenu(event -> {
            TreeItem<WebDAVResource> treeItem = tree.getSelectionModel().getSelectedItem();
            ComponentUtils.showResourcePropertiesDialog(treeItem.getValue());
        }, 
        null, 
        event -> {
            TreeItem<WebDAVResource> treeItem = tree.getSelectionModel().getSelectedItem();
            if(treeItem.equals(tree.getRoot())) {
                
                // Delete host
                if(hostsHelper.triggerRemoveHost(service.getHost())) {
                    
                    // Clear tree
                    clearTree();
                    
                    // Clear table
                    fileList.clear();
                }
            }
        }));
        
        // Show/hide menu items based on node type
        /*
        tree.setOnContextMenuRequested((event) -> {
            TreeItem<WebDAVResource> treeItem = tree.getSelectionModel().getSelectedItem();
            if(treeItem.equals(tree.getRoot())) {
                menu.getItems().get(menu.getItems().indexOf(defaultMenuItem)).setVisible(true);
            } else {
                menu.getItems().get(menu.getItems().indexOf(defaultMenuItem)).setVisible(false);
            }
        });
        */
    }
    
    private void onTreeItemSelect(TreeItem<WebDAVResource> treeItem) {
        if(treeItem != null && treeItem.getValue() != null) {
            
            // Fetch child resources of three item
            List<WebDAVResource> files;
            try {
                
                // TODO This should be an async task!
                files = service.list(treeItem.getValue().getAbsolutePath());
            } catch (WebDAVServiceException e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
                return;
            }
            
            // Refresh file list
            fileList.clear();
            
            // Add all elements at once to optimize event handling
            fileList.addAll(files.stream().map(ResourceTableItem::fromDavRes).collect(Collectors.toList()));
        }
    }
    
    private void onTreeNodeExpand(TreeItem<WebDAVResource> treeItem) {
        if(treeItem != null && treeItem.getValue() != null) {
            WebDAVResource res = (WebDAVResource) treeItem.getValue();
        
            final ListDirsTask task = new ListDirsTask(service,  res.getAbsolutePath(), true);
            task.valueProperty().addListener((obs, oldVal, newVal) -> {
            
                ObservableList<TreeItem<WebDAVResource>> nodeChildren = treeItem.getChildren();
            
                // NB: Make sure all elements get added at once, 
                // needed by double clicking on a folder in the resource table
                nodeChildren.addAll(newVal.stream().map(this::createTreeNode).collect(Collectors.toList()));
            
                // Update icon when done
                if(treeItem != tree.getRoot()) {
                    treeItem.setGraphic(Icons.folderOpen());
                } else {
                    treeItem.setGraphic(Icons.server());
                }
            
            });
            WebDAVUtil.startTask(task);
        
            // Set special icon if loading for too long
            WebDAVUtil.startLoadTimer(new Runnable() {
                public void run() {
                    if (task.isRunning()) {
                        treeItem.setGraphic(Icons.hourglass());
                    }
                }
            }, 2000);
        }
    }
    
    private void onTreeNodeCollapse(TreeItem<WebDAVResource> treeItem) {
        treeItem.getChildren().clear();
        
        // Update icon
        if(treeItem != tree.getRoot()) {
            treeItem.setGraphic(Icons.folder());
        }
    }
    
    private TreeItem<WebDAVResource> createTreeNode(WebDAVResource res) {
        TreeItem<WebDAVResource> nodeItem = new TreeItem<WebDAVResource>(res) {
            public boolean isLeaf() {
                return !res.isDirectory();
            }
        };
        
        nodeItem.setGraphic(Icons.folder());
        return nodeItem;
    }
}
