package mb.client.rfe;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.controlsfx.control.BreadCrumbBar;
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
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import mb.client.rfe.components.ComponentUtils;
import mb.client.rfe.components.GridView;
import mb.client.rfe.components.Icons;
import mb.client.rfe.media.MPlayer;
import mb.client.rfe.model.ResourceHost;
import mb.client.rfe.model.ResourceTableItem;
import mb.client.rfe.model.WebDAVResource;
import mb.client.rfe.service.ConfigService;
import mb.client.rfe.service.LocalFileSystemService;
import mb.client.rfe.service.ResourceRepositoryService;
import mb.client.rfe.service.WebDAVService;
import mb.client.rfe.service.WebDAVServiceException;

// TODO Multiselect in both views
// TODO Support for keyboard shortcuts in grid view
// TODO Drag and drop support in grid view
public class WebDAVClient extends Application {
    
    private static final Logger LOG = Logger.getLogger(WebDAVClient.class.getName());
    private static final ConfigService config = ConfigService.getInstance();
    
    private Stage stage;
    private SplitPane splitPane;
    private HostMgmtHelper hostsHelper;
    private ComboBox<ResourceHost> hostsComboBox;
    private BreadCrumbBar<WebDAVResource> breadCrumbBar;
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
    private ResourceRepositoryService service;
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
        
        // Button bar with host selection, resource view toggle, bread crumb, etc
        HBox buttonBar = new HBox(new Label("Host"), createHostComboBox(), 
                    createRemoveHostButton(), createAddHostButton(),
                new Separator(Orientation.VERTICAL), createViewToggle(showTable),
                new Separator(Orientation.VERTICAL), createBreadCrumbBar(), ComponentUtils.createHBoxSpacer(), 
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
        config.setProperty("col.name.sorttype", String.valueOf(table.getColumns().get(1).getSortType()));
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

    private void createService(ResourceHost host) {
        destroyServiceInstanceIfExists();
        if(host.isLocal()) {
            service = new LocalFileSystemService();
        } else {
            service = new WebDAVService(host);
        }
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
        
        // Bind bread crumb to tree view
        treeView = treeHelper.getTree();
        breadCrumbBar.selectedCrumbProperty().bind(treeView.getSelectionModel().selectedItemProperty());
        
        return treeView;
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
    
    private ComboBox<ResourceHost> createHostComboBox() {
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
        button.setTooltip(new Tooltip("Add new host"));
        button.setOnAction(event -> {
            onAddNewHost();
        });
        return button;
    }
    
    private Button createRemoveHostButton() {
        Button button = new Button("", Icons.minus());
        button.setTooltip(new Tooltip("Remove selected host"));
        button.setOnAction(event -> {
            onRemoveSelectedHost();
        });
        return button;
    }
    
    @SuppressWarnings("unused")
    private Button createEditHostButton() {
        Button button = new Button("", Icons.edit());
        button.setTooltip(new Tooltip("Edit selected host"));
        button.setOnAction(event -> {
            onEditSelectedHost();
        });
        return button;
    }
    
    private ScrollPane createGrid() {
        grid = new GridView(service, fileList, treeView, tpv, 10, 10);
        grid.setPadding(new Insets(10));
        
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
    
    private BreadCrumbBar<WebDAVResource> createBreadCrumbBar() {
        breadCrumbBar = new BreadCrumbBar<WebDAVResource>();
        breadCrumbBar.setAutoNavigationEnabled(false);
        return breadCrumbBar;
    }
    
    private void selectLastUsedHost() {
        String prop = config.getProperty("host.lastused");
        if(prop != null && !prop.isEmpty()) {
            ResourceHost host = hostsComboBox.getItems().stream()
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
    
    /**
     * Should be called when a host is selected in the list
     */
    private void onHostSelected(ResourceHost host) {
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
    
    private void onRemoveSelectedHost() {
        ResourceHost host = hostsComboBox.getSelectionModel().getSelectedItem();
        if(host != null) {
            hostsHelper.triggerRemoveHost(host, h -> {
                // TODO Clear tree, table and thumbs
            });
        }
    }
    
    private void onEditSelectedHost() {
        ResourceHost host = hostsComboBox.getSelectionModel().getSelectedItem();
        if(host != null) {
            // TODO Support for host editing
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
