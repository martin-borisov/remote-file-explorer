package mb.client.rfe;

import static java.text.MessageFormat.format;

import java.io.File;
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
import mb.client.rfe.components.ComponentUtils;
import mb.client.rfe.model.ResourceHost;
import mb.client.rfe.model.WebDAVResource;
import mb.client.rfe.service.HostPersistenceService;
import mb.client.rfe.service.WebDAVService;
import mb.client.rfe.service.WebDAVServiceException;
import mb.client.rfe.service.WebDAVUtil;

public class HostMgmtHelper {
    
    private ObservableList<ResourceHost> hosts;
    
    public HostMgmtHelper() {
        hosts = FXCollections.observableArrayList(
                HostPersistenceService.getInstance().getHosts());
        addLocalFsRoots();
    }
    
    public ObservableList<ResourceHost> getHosts() {
        return hosts;
    }

    public void triggerAddNewHost(Consumer<ResourceHost> handler) {
        createAndShowAddNewHostDialog().showAndWait().ifPresent(host -> {
            HostPersistenceService.getInstance().addHost(host);
            hosts.add(host);
            handler.accept(host);
        });
    }
    
    public void triggerRemoveHost(ResourceHost host, Consumer<ResourceHost> handler) {
        ComponentUtils.createConfirmationDialog("Remove Host", 
                format("Are you sure you want to remove host ''{0}''?", host), 
                "This cannot be undone!").showAndWait().ifPresent(type -> {
                    if(ButtonType.OK == type) {
                        if(HostPersistenceService.getInstance().deleteHost(host)) {
                            hosts.remove(host);
                            handler.accept(host);
                        }
                    }
                });
    }
    
    public boolean triggerRemoveHost(ResourceHost host) {
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
    
    private void addLocalFsRoots() { 
        
        // TODO Windows: this is how local root appears in Hosts dropdown "file:/C:/"
        File[] roots = File.listRoots();
        if(roots != null) {
            for (File root : roots) {
                ResourceHost host = new ResourceHost(root.toURI(), root.getAbsolutePath(), null, null);
                host.setLocal(true);
                hosts.add(host);
            }
        }
    }
    
    private Dialog<ResourceHost> createAndShowAddNewHostDialog() {
        
        Dialog<ResourceHost> dlg = new Dialog<>();
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
                return new ResourceHost(URI.create(baseUri.getText()), 
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
            ResourceHost host = new ResourceHost(URI.create(baseUri.getText()), 
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
