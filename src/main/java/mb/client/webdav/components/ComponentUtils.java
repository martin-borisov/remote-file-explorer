package mb.client.webdav.components;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.property.BeanProperty;
import org.controlsfx.property.BeanPropertyUtils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import mb.client.webdav.model.WebDAVResource;

public class ComponentUtils {
    
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
    
    public static void showMapPropertiesDialog(Map<?, ?> map) {
        
        VBox layout = new VBox();
        map.entrySet().stream().forEach(entry -> {
            layout.getChildren().add(
                    new Text(String.valueOf(entry.getKey()) + " : " + String.valueOf(entry.getValue())));
        });
        
        
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Properties");
        //alert.setHeaderText(MessageFormat.format("Properties of resource ''{0}''", res.getName()));
        alert.getDialogPane().setPrefWidth(400);
        alert.getDialogPane().setContent(layout);
        alert.showAndWait();
    }
}
