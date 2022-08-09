package mb.client.webdav;

import static java.text.MessageFormat.format;

import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import mb.client.webdav.model.WebDAVHost;
import mb.client.webdav.model.WebDAVResource;
import mb.client.webdav.service.HostPersistenceService;
import mb.client.webdav.service.WebDAVService;
import mb.client.webdav.service.WebDAVServiceException;
import mb.client.webdav.service.WebDAVUtil;

public class HostMgmtHelper {
    
    private ObservableList<WebDAVHost> hosts;
    
    public HostMgmtHelper() {
        hosts = FXCollections.observableArrayList(
                HostPersistenceService.getInstance().getHosts());
    }
    
    public ObservableList<WebDAVHost> getHosts() {
        return hosts;
    }

    public void triggerAddNewHost(Consumer<WebDAVHost> handler) {
        createAndShowAddNewHostDialog().showAndWait().ifPresent(host -> {
            HostPersistenceService.getInstance().addHost(host);
            hosts.add(host);
            handler.accept(host);
        });
    }
    
    public boolean triggerRemoveHost(WebDAVHost host) {
        boolean result = false;
        
        Alert dialog = new Alert(AlertType.CONFIRMATION);
        dialog.setTitle("Host Removal Confirmation");
        dialog.setHeaderText(format("Are you sure you want to remove host ''{0}''?", host.getBaseURI()));

        Optional<ButtonType> input = dialog.showAndWait();
        if (input.get() == ButtonType.OK){
            result = HostPersistenceService.getInstance().deleteHost(host);
            if(result) {
                hosts.remove(host);
            }
        }
        return result;
    }
    
    private Dialog<WebDAVHost> createAndShowAddNewHostDialog() {
        
        Dialog<WebDAVHost> dlg = new Dialog<>();
        dlg.setTitle("Add Host");
        dlg.setHeaderText("WebDAV Connection Properties");
        
        ButtonType saveButtonType = new ButtonType("Connect & Save", ButtonData.APPLY);
        dlg.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField baseUri = new TextField();
        baseUri.setPromptText("Base URI");
        TextField root = new TextField();
        root.setPromptText("Root directory");
        TextField user = new TextField();
        user.setPromptText("Username");
        PasswordField pass = new PasswordField();
        pass.setPromptText("Password");
        
        grid.add(new Label("Base URI"), 0, 0);
        grid.add(baseUri, 1, 0);
        grid.add(new Label("Root Directory"), 0, 1);
        grid.add(root, 1, 1);
        grid.add(new Label("Username"), 0, 2);
        grid.add(user, 1, 2);
        grid.add(new Label("Password (stored insecurely)"), 0, 3);
        grid.add(pass, 1, 3);
        
        dlg.getDialogPane().setContent(grid);
        
        // Request focus on the baseUri field by default.
        Platform.runLater(() -> baseUri.requestFocus());

        // Create new WebDAVHost instance when save is clicked
        dlg.setResultConverter(button -> {
            if (button == saveButtonType) {
                return new WebDAVHost(URI.create(baseUri.getText()), 
                        root.getText(), user.getText(), pass.getText());
            }
            return null;
        });
        
        // Validate user input
        Button buttonSave = (Button) dlg.getDialogPane().lookupButton(saveButtonType);
        buttonSave.addEventFilter(ActionEvent.ACTION, event -> {
            
            // Make sure there are some values
            if (baseUri.getText().isEmpty() || root.getText().isEmpty()) {
                event.consume();
                return;
            }
            
            // Try to connect
            WebDAVHost host = new WebDAVHost(URI.create(baseUri.getText()), 
                    root.getText(), user.getText(), pass.getText());
            WebDAVService svc = new WebDAVService(host);
            svc.connect();
            WebDAVResource res = new WebDAVResource(host.getBaseURI().toString() + host.getRoot(), host.getRoot());
            try {
                svc.list(res.getAbsolutePath());
            } catch (WebDAVServiceException e) {
                
                // Show connection error
                Alert alert = new Alert(AlertType.ERROR);
                alert.setHeaderText("Connecting to host failed");
                alert.setContentText(WebDAVUtil.getExceptionMessageChain(e)
                        .stream()
                        .filter(el -> el != null)
                        .map(el -> {
                            return el != null ? String.valueOf(el) : "";
                            })
                        .collect(Collectors.joining(",", "[", "]")));
                alert.showAndWait();
                event.consume();
                return;
                
            } finally {
                try {
                    svc.disconnect();
                } catch (WebDAVServiceException e) {
                    // Ignore
                }
            }
        });
        
        return dlg;
    }

}
