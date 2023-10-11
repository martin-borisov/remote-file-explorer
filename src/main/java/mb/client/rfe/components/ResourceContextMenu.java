package mb.client.rfe.components;


import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

public class ResourceContextMenu extends ContextMenu {

    public ResourceContextMenu(EventHandler<ActionEvent> propsHandler, EventHandler<ActionEvent> playlistHandler, 
            EventHandler<ActionEvent> deleteHandler, EventHandler<ActionEvent> createDirHandler) {
        super();
        createMenuItems(propsHandler, playlistHandler, deleteHandler, createDirHandler);
    }
    
    private void createMenuItems(EventHandler<ActionEvent> propsHandler, EventHandler<ActionEvent> playlistHandler, 
            EventHandler<ActionEvent> deleteHandler, EventHandler<ActionEvent> createDirHandler) {
        createMenuItem("Add to Playlist", Icons.play(), playlistHandler);
        createMenuItem("Create Directory", Icons.createFoler(), createDirHandler);
        getItems().add(new SeparatorMenuItem());
        createMenuItem("Delete", Icons.delete(), deleteHandler);
        getItems().add(new SeparatorMenuItem());
        createMenuItem("Properties", Icons.properties(), propsHandler);
       
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
    
    private void createMenuItem(String text, Node graphic, EventHandler<ActionEvent> handler) {
        if(handler != null) {
            MenuItem item = new MenuItem(text, graphic);
            item.setOnAction(handler);
            getItems().add(item);
        }
    }
}
