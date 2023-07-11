package mb.client.webdav;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.controlsfx.control.MasterDetailPane;
import org.controlsfx.control.SegmentedButton;
import org.controlsfx.control.StatusBar;
import org.controlsfx.control.TaskProgressView;
import org.controlsfx.control.ToggleSwitch;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import mb.client.webdav.components.GridView;
import mb.client.webdav.components.Icons;
import mb.client.webdav.media.MPlayer;
import mb.client.webdav.model.ResourceTableItem;
import mb.client.webdav.model.WebDAVHost;
import mb.client.webdav.model.WebDAVResource;
import mb.client.webdav.service.ConfigService;
import mb.client.webdav.service.WebDAVService;
import mb.client.webdav.service.WebDAVServiceException;
import mb.client.webdav.service.WebDAVUtil;
import mb.client.webdav.tasks.DownloadFileTask;

// TODO Drag and drop support in grid view
public class WebDAVClient extends Application {
    
    private static final Logger LOG = Logger.getLogger(WebDAVClient.class.getName());
    private static final ConfigService config = ConfigService.getInstance();
    
    private Stage stage;
    private SplitPane splitPane;
    private HostMgmtHelper hostsHelper;
    private ComboBox<WebDAVHost> hostsComboBox;
    private TreeViewHelper treeHelper;
    private TreeView<WebDAVResource> treeView;
    private TableViewHelper tableHelper;
    private TableView<ResourceTableItem> table;
    private GridView grid;
    private ScrollPane gridPane;
    private StatusBar statusBar;
    private MasterDetailPane mdp;
    private MPlayer player;
    private TaskProgressView<Task<?>> tpv;
    private WebDAVService service;
    private ObservableList<ResourceTableItem> fileList;
    
    public WebDAVClient() {
        fileList = FXCollections.observableArrayList();
    }

    @Override
    public void start(Stage stage) throws Exception {
        
        // View toggle prop
        Boolean showTable = Boolean.valueOf(config.getOrCreateProperty("resourceview.table", "true"));
        
        // Root layout
        BorderPane borderPane = new BorderPane();
        
        // Button bar with host selection and resource view toggle
        HBox buttonBar = new HBox(new Label("Host"), createHostComboBox(), createAddHostButton(),
                new Separator(Orientation.VERTICAL), createViewToggle(showTable),
                new Separator(Orientation.VERTICAL), createGoToParentButton());
        buttonBar.setSpacing(5);
        buttonBar.setPadding(new Insets(5));
        buttonBar.setAlignment(Pos.CENTER_LEFT);
        borderPane.setTop(buttonBar);
        
        // Media player
        createMediaPlayer();
        
        // Master/detail layout
        mdp = new MasterDetailPane(Side.BOTTOM);
        borderPane.setCenter(mdp);
        
        // Task progress pane
        tpv = new TaskProgressView<>();
        tpv.setRetainTasks(true);
        mdp.setDetailNode(tpv);
        
        // Split pane, tree, table and grid
        treeView = createTree();
        table = createTable();
        gridPane = createGrid();
        
        splitPane = new SplitPane();
        splitPane.setDividerPosition(0, Double.valueOf(config.getOrCreateProperty("split.pos", String.valueOf(0.25))));
        splitPane.getItems().addAll(treeView, showTable ? table : gridPane);
        mdp.setMasterNode(splitPane);
        
        // Status bar
        statusBar = new StatusBar();
        statusBar.setText("");
        statusBar.textProperty().bind(grid.thumbLoadingMessageProperty());
        statusBar.progressProperty().bind(grid.thumbLoadingProgressProperty());
        borderPane.setBottom(statusBar);
        
        // Task status list toggle
        ToggleSwitch tpvToggle = new ToggleSwitch("Show Tasks");
        tpvToggle.setSelected(Boolean.valueOf(config.getOrCreateProperty("taskslist.toggle", "true")));
        statusBar.getLeftItems().add(tpvToggle);
        statusBar.getRightItems().add(createMediaPlayerToggle());
        mdp.showDetailNodeProperty().bind(tpvToggle.selectedProperty());
        
        // Stage properties
        stage.setWidth(Double.valueOf(config.getOrCreateProperty("stage.width", String.valueOf(800))));
        stage.setHeight(Double.valueOf(config.getOrCreateProperty("stage.height", String.valueOf(600))));
        stage.setScene(new Scene(borderPane));
        stage.setTitle("WebDAV Client");
        
        // This is needed as media player is a separate stage
        stage.setOnCloseRequest(event -> {
            Platform.exit();
        });
        
        // Select last used host if available
        selectLastUsedHost();
        
        // Navigate to last used directory
        String lastDir = config.getProperty("host.last.dir");
        if(lastDir != null) {
            treeHelper.navigateTo(lastDir);
        }
        
        // Show
        stage.show();
        this.stage = stage;
    }
    
    @Override
    public void stop() throws Exception {
        
        //Preserve various properties for the next run
        config.setProperty("host.lastused", String.valueOf(hostsComboBox.getValue()));
        config.setProperty("stage.width", String.valueOf(stage.getWidth()));
        config.setProperty("stage.height", String.valueOf(stage.getHeight()));
        config.setProperty("split.pos", String.valueOf(splitPane.getDividerPositions()[0]));
        config.setProperty("col.name.width", String.valueOf(table.getColumns().get(1).getWidth()));
        config.setProperty("taskslist.toggle", String.valueOf(mdp.showDetailNodeProperty().getValue()));
        config.setProperty("resourceview.table", String.valueOf(table.equals(splitPane.getItems().get(1))));
        
        // Save last accessed directory
        WebDAVResource res = treeHelper.getCurrentDirectory();
        if(res != null) {
            config.setProperty("host.last.dir", res.getAbsolutePath());
        }
        
        destroyMediaPlayer();
        destroyServiceInstanceIfExists();
        
        // And this is needed as there are some leftover non-daemon threads in the media player library
        System.exit(0);
    }

    private void createService(WebDAVHost host) {
        destroyServiceInstanceIfExists();
        service = new WebDAVService(host);
        service.connect();
    }
    
    private void destroyServiceInstanceIfExists() {
        if(service != null) {
            try {
                service.disconnect();
            } catch (WebDAVServiceException e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
            }
            service = null;
        }
    }
    
    /* Component create methods */
    
    private TreeView<WebDAVResource> createTree() {
        treeHelper = new TreeViewHelper(service, fileList, hostsHelper);
        return treeView = treeHelper.getTree();
    }
    
    private TableView<ResourceTableItem> createTable() {
        tableHelper = new TableViewHelper(service, fileList, treeView, tpv, player);
        return table = tableHelper.getTable();
    }
    
    private SegmentedButton createViewToggle(boolean listSelected) {
        
        ToggleButton buttonList = new ToggleButton();
        buttonList.setGraphic(new FontIcon(FontAwesomeSolid.LIST));
        buttonList.setOnAction(event -> {
            double[] pos = splitPane.getDividerPositions();
            splitPane.getItems().remove(1);
            splitPane.getItems().add(table);
            splitPane.setDividerPositions(pos);
        });
        
        ToggleButton buttonGrid = new ToggleButton();
        buttonGrid.setGraphic(new FontIcon(FontAwesomeSolid.TABLE));
        buttonGrid.setOnAction(event -> {
            double[] pos = splitPane.getDividerPositions();
            splitPane.getItems().remove(1);
            splitPane.getItems().add(gridPane);
            splitPane.setDividerPositions(pos);
        });
        
        ToggleGroup group = new ToggleGroup();
        buttonList.setToggleGroup(group);
        buttonGrid.setToggleGroup(group);
        group.selectToggle(listSelected ? buttonList : buttonGrid);
        
        SegmentedButton segButton = new SegmentedButton(buttonList, buttonGrid);
        segButton.setToggleGroup(group);
        
        return segButton;
    }
    
    private ComboBox<WebDAVHost> createHostComboBox() {
        hostsHelper = new HostMgmtHelper();
        hostsComboBox = new ComboBox<>(hostsHelper.getHosts());
        hostsComboBox.setMinWidth(200);
        hostsComboBox.setOnAction(event -> {
            onHostSelected(hostsComboBox.getValue());
        });
        return hostsComboBox;
    }
    
    private Button createAddHostButton() {
        Button button = new Button("", Icons.plus());
        button.setOnAction(event -> {
            onAddNewHost();
        });
        return button;
    }
    
    
    private ScrollPane createGrid() {
        grid = new GridView(service, fileList, 10, 10);
        grid.setPadding(new Insets(10));
        
        grid.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onRowDoubleClick((ResourceTableItem) grid.getSelectedItem().getUserData());
            }
        });
        
        // Make sure the grid scrolls
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        return scrollPane;
    }
    
    private Button createGoToParentButton() {
        Button button = new Button("", Icons.arrowUp());
        button.setOnAction(event -> {
            treeHelper.selectParent();
        });
        return button;
    }
    
    private void selectLastUsedHost() {
        String prop = config.getProperty("host.lastused");
        if(prop != null && !prop.isEmpty()) {
            WebDAVHost host =  hostsComboBox.getItems().stream()
                .filter(h -> prop.equals(h.getBaseUriString()))
                .findFirst()
                .orElse(null);
            if(host != null) {
                hostsComboBox.getSelectionModel().select(host);
                onHostSelected(host);
            }
        }
    }
    
    private void createMediaPlayer() {
        player = new MPlayer();
    }
    
    private void destroyMediaPlayer() {
        if(player != null) {
            player.destroy();
        }
    }
    
    private ToggleSwitch createMediaPlayerToggle() {
        ToggleSwitch toggle = new ToggleSwitch("Media Player");
        toggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if(newVal) {
                player.show();
            } else {
                player.hide();
            }
        });
        toggle.setVisible(true);
        return toggle;
    }
    
    /* Event handlers */
    
    private void onRowDoubleClick(ResourceTableItem res) {
        if(res.isDirectory()) {
            
            // Open directories
            TreeItem<WebDAVResource> selItem = treeView.getSelectionModel().getSelectedItem();
            if(selItem != null) {
                selItem.setExpanded(true);
                
                // React when new items get added to the tree node (this happens asynchronously)
                selItem.getChildren().addListener(new ListChangeListener<TreeItem<WebDAVResource>>() {
                    public void onChanged(Change<? extends TreeItem<WebDAVResource>> change) {
                        change.next();
                        if(change.wasAdded()) {
                            
                            // Select item which was double clicked
                            for (TreeItem<WebDAVResource> childItem : selItem.getChildren()) {
                                if(res.getName().equals(childItem.getValue().getName())) {
                                    treeView.getSelectionModel().select(childItem);
                                    break;
                                }
                            }
                        }
                    }

                });
            }
            
        } else {
            
            // Download files
            DownloadFileTask task = new DownloadFileTask(service,  res.getDavRes());
            tpv.getTasks().add(0, task);
            WebDAVUtil.startTask(task);
        }
    }
    
    /**
     * Should be called when a host is selected in the list
     */
    private void onHostSelected(WebDAVHost host) {
        if(host != null) {
            createService(host);
            treeHelper.updateRoot(host, service);
            tableHelper.setService(service);
            grid.setService(service);
        }
    }
    
    private void onAddNewHost() {
        hostsHelper.triggerAddNewHost(host -> {
            
            // Update tree
            createService(host);
            treeHelper.updateRoot(host, service);
            
            // Select new host
            hostsComboBox.getSelectionModel().select(host);
        });
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
