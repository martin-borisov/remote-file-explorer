package mb.client.webdav.components;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.util.Map;

import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.property.BeanProperty;
import org.controlsfx.property.BeanPropertyUtils;

import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import mb.client.webdav.model.WebDAVResource;

public class ComponentUtils {
    
    /**
     * Shows a dialog displaying the properties of a {@link WebDAVResource}
     * @param res Resource instance
     */
    public static void showResourcePropertiesDialog(WebDAVResource res) {
        ObservableList<Item> props = BeanPropertyUtils.getProperties(res);
        props.stream().forEach(p -> ((BeanProperty) p).setEditable(false)); // Make properties read-only
        
        PropertySheet sheet = new PropertySheet(props);
        sheet.setModeSwitcherVisible(false);
        sheet.setSearchBoxVisible(false);
        
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Resource Properties");
        alert.setHeaderText(MessageFormat.format("Properties of resource ''{0}''", res.getName()));
        alert.getDialogPane().setPrefWidth(400);
        alert.getDialogPane().setContent(sheet);
        alert.showAndWait();
    }
    
    /**
     * Shows a dialog displaying the provided name value pairs
     * @param map Map containing name-value pairs
     * @param msg Optional message to the user
     */
    public static void showMapPropertiesDialog(Map<?, ?> map, String msg) {
        VBox layout = new VBox();
        map.entrySet().stream().forEach(entry -> {
            layout.getChildren().add(
                    new Text(String.valueOf(entry.getKey()) + " : " + String.valueOf(entry.getValue())));
        });
        
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Properties");
        
        if(msg != null) {
            alert.setHeaderText(msg);
        }
        
        alert.getDialogPane().setPrefWidth(400);
        alert.getDialogPane().setContent(layout);
        alert.showAndWait();
    }
    
    /**
     * Creates a dialog to confirm resource move. Reused by table view and grid view.
     * @param resName Resource name to be shown to the user
     * @return Dialog to be used for move confirmation
     */
    public static Alert createResourceMoveDialog(String resName) {
        return createResourceModificationDialog("Move Resource Confirmation", 
                format("Are you sure you want to move resource ''{0}''?", resName), 
                "Note that this cannot be undone!");
    }
    
    /**
     * Creates a dialog to confirm resource deletion. Reused by table view and grid view.
     * @param resName Resource name to be shown to the user
     * @return Dialog to be used for deletion confirmation
     */
    public static Alert createResourceDeletionDialog(String resName) {
        return createResourceModificationDialog("Delete Resource Confirmation", 
                format("Are you sure you want to delete resource ''{0}''?", resName), 
                "Note that this cannot be undone!");
    }
    
    /**
     * Creates a dialog to confirm resource modification. Reused by table view and grid view.
     * @param title Dialog title
     * @param header Dialog header
     * @param content Dialog content
     * @return Dialog to be used for modification confirmation
     */
    public static Alert createResourceModificationDialog(String title, String header, String content) {
        Alert dialog = new Alert(AlertType.CONFIRMATION);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);
        return dialog;
    }
    
    /**
     * Creates a generic alert or warning dialog
     * @param title Dialog title
     * @param header Dialog header
     * @param content Dialog content
     * @return Dialog used for showing an error to the user
     */
    public static Alert createWarningDialog(String title, String header, String content) {
        Alert dialog = new Alert(AlertType.WARNING);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);
        return dialog;
    }
}
