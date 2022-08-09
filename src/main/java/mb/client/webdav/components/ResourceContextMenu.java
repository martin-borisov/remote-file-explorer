package mb.client.webdav.components;


import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class ResourceContextMenu extends ContextMenu {

    public ResourceContextMenu(EventHandler<ActionEvent> propsHandler, 
            EventHandler<ActionEvent> playlistHandler, EventHandler<ActionEvent> deleteHandler) {
        super();
        createMenuItems(propsHandler, playlistHandler, deleteHandler);
    }
    
    private void createMenuItems(EventHandler<ActionEvent> propsHandler, 
            EventHandler<ActionEvent> playlistHandler, EventHandler<ActionEvent> deleteHandler) {
        
        if(propsHandler != null) {
            MenuItem propertiesMenuItem = new MenuItem("Properties", Icons.properties());
            propertiesMenuItem.setOnAction(propsHandler);
            getItems().add(propertiesMenuItem);
        }
        
        if(playlistHandler != null) {
            MenuItem addToPlaylistMenuItem = new MenuItem("Add to Playlist", Icons.play());
            addToPlaylistMenuItem.setOnAction(playlistHandler);
            getItems().add(addToPlaylistMenuItem);
        }
        
        if(deleteHandler != null) {
            MenuItem deleteMenuItem = new MenuItem("Delete", Icons.delete());
            deleteMenuItem.setOnAction(deleteHandler);
            getItems().add(deleteMenuItem);
        }
       
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
}
