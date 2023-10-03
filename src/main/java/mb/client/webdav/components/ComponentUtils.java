package mb.client.webdav.components;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.util.Map;

import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.property.BeanPropertyUtils;

import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
        
        PropertySheet sheet = new PropertySheet(props);
        sheet.setModeSwitcherVisible(false);
        sheet.setSearchBoxVisible(false);
        
        // Custom editor factory for enhanced presentation
        sheet.setPropertyEditorFactory(new ResourcePropertyEditorFactory());
        
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
        return createConfirmationDialog("Move Resource Confirmation", 
                format("Are you sure you want to move resource ''{0}''?", resName), 
                "Note that this cannot be undone!");
    }
    
    /**
     * Creates a dialog to confirm resource deletion. Reused by table view and grid view.
     * @param resName Resource name to be shown to the user
     * @return Dialog to be used for deletion confirmation
     */
    public static Alert createResourceDeletionDialog(String resName) {
        return createConfirmationDialog("Delete Resource Confirmation", 
                format("Are you sure you want to delete resource ''{0}''?", resName), 
                "Note that this cannot be undone!");
    }
    
    /**
     * Creates a generic confirmation dialog
     */
    public static Alert createConfirmationDialog(String title, String header, String content) {
        return createAlertDialog(AlertType.CONFIRMATION, title, header, content);
    }
    
    /**
     * Creates a generic alert or warning dialog
     */
    public static Alert createWarningDialog(String title, String header, String content) {
        return createAlertDialog(AlertType.WARNING, title, header, content);
    }
    
    /**
     * Creates a generic dialog
     */
    public static Alert createAlertDialog(AlertType type, String title, String header, String content) {
        Alert dialog = new Alert(type);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);
        return dialog;
    }
    
    /**
     * Creates a prompt dialog
     */
    public static TextInputDialog createTextInputDialog(
            String title, String header, String content, String defaultVal) {
        TextInputDialog dialog = new TextInputDialog(defaultVal);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);
        return dialog;
    }
    
    /**
     * Creates a simple growing spacer that can be used in a {@link HBox}
     */
    public static Region createHBoxSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
}
