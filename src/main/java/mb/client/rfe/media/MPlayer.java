package mb.client.rfe.media;

import static java.text.MessageFormat.format;

import java.io.ByteArrayInputStream;
import java.net.Authenticator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tbee.javafx.scene.layout.MigPane;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import mb.client.rfe.components.ComponentUtils;
import mb.client.rfe.components.Icons;
import mb.client.rfe.media.audio.AudioPlayer;
import mb.client.rfe.media.audio.AudioPlayerException;
import mb.client.rfe.media.audio.AudioPlayerListener;
import mb.client.rfe.media.audio.AudioSource;
import mb.client.rfe.service.ConfigService;

public class MPlayer extends Stage {
    private static final Logger LOG = Logger.getLogger(MPlayer.class.getName());
    
    private Button playButton;
    private Slider timeSlider, volumeSlider;
    private Label playTime;
    private ToggleButton loopToggle;
    private ListView<MPMedia> playlist;
    private MPMedia currentlyPlayingMedia;
    private MediaPreProcessor currentlyPlayingMpp;
    private AudioPlayer player;
    private SimpleObjectProperty<Map<String, Object>> currMediaAttribsProperty;
    
    // TODO 1) Mp3 tags/properties 2) Duration of local mp3 and wav 3) Ability to seek for local media

    public MPlayer() {
        super();
        setTitle("Audio Player");
        createProperties();
        createAudioPlayer();
        setupScene();
        loadStoredPlaylist();
    }
    
    public void addToPlaylist(MPMedia media) {
        playlist.getItems().add(media);
    }
    
    public boolean removeFromPlaylist(MPMedia media) {
        return playlist.getItems().remove(media);
    }
    
    public void clearPlaylist() {
        playlist.getItems().clear();
    }
    
    private void createProperties() {
        currMediaAttribsProperty = new SimpleObjectProperty<Map<String,Object>>(
                Collections.emptyMap());
    }
    
    private void createAudioPlayer() {
        player = new AudioPlayer();
        player.addListener(new AudioPlayerListener() {
            public void onOpen(Map<String, Object> properties) {
                onPlayerOpened(properties);
            }
            public void onStart() {
                onPlayerStarted();
            }
            public void onStop() {
                onPlayerStopped();
            }
            public void onEndOfMedia() {
                MPlayer.this.onEndOfMedia();
            }
            public void onProgress(int elapsedSeconds) {
                // NB: This is called roughly once per second
                onPlayerProgress(elapsedSeconds);
            }
            public void onPause() {
                onPlayerStopped();
            }
            public void onResume() {
                onPlayerStarted();
            }
        });
    }
    
    public void destroy() {
        player.stop();
        
        // Keep playlist
        PlaylistPersistenceService.getInstance().savePlaylist(new ArrayList<MPMedia>(playlist.getItems()));
    }
    
    /* Create and setup UI */
    
    private void setupScene() {
        BorderPane borderPane = new BorderPane();
        
        // Playlist
        ScrollPane scrollPane = new ScrollPane(createPlaylist());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        borderPane.setCenter(scrollPane);
        
        // Control layout
        MigPane mig = new MigPane("fill, wrap 2",
                "[left, grow][]", "top");
        
        // Title
        Label titleLabel = new Label();
        titleLabel.setFont(Font.font(null, FontWeight.BOLD, 18));
        titleLabel.textProperty().bind(Bindings.createObjectBinding(() -> {
            String title = (String) currMediaAttribsProperty.get().get("title");
            if(title == null) {
                if(currentlyPlayingMedia != null) {
                    title = currentlyPlayingMedia.getName();
                }
            }
            return title;
        }, currMediaAttribsProperty));
        
        // Artist & Album
        Label artistAlbumLabel = new Label();
        artistAlbumLabel.textProperty().bind(Bindings.createObjectBinding(() -> {
            
            StringBuilder buf = new StringBuilder();
            Map<String, Object> props = currMediaAttribsProperty.get();
            if(props.containsKey("author") || props.containsKey("artist")) {
                buf.append("By '").append(
                        Optional.ofNullable(props.get("author")).orElse(props.get("artist"))).append("' ");
            }
            if(props.containsKey("album")) {
                buf.append("from album '").append(props.get("album")).append("'");
            }
            return buf.toString();
        }, currMediaAttribsProperty));
        
        // Format
        Label formatLabel = new Label();
        formatLabel.setFont(Font.font(null, FontPosture.ITALIC, -1));
        formatLabel.textProperty().bind(Bindings.createObjectBinding(() -> {
            
            Map<String, Object> tags = currMediaAttribsProperty.get();
            StringBuilder buf = new StringBuilder();
            
            if(tags.containsKey("audio.type")) {
                buf.append(tags.get("audio.type").toString()).append(" | ");
                buf.append(format("{0,number,#} Hz | {1} bit | {2} channels", 
                        tags.get("audio.samplerate.hz"), tags.get("audio.samplesize.bits"), tags.get("audio.channels")));
                
                if(tags.containsKey("bitrate")) {
                    buf.append(format(" | {0,number,#} kbs", Integer.valueOf(tags.get("bitrate").toString()) / 1000));
                }
                
                if(Boolean.valueOf(String.valueOf(tags.get("vbr")))) {
                    buf.append(" vbr");                
                }
            }
            return buf.toString();
        }, currMediaAttribsProperty));
        
        VBox tagsBox = new VBox(titleLabel, artistAlbumLabel, formatLabel);
        mig.add(tagsBox);
        
        // Album art
        ImageView imageView = new ImageView();
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        imageView.imageProperty().bind(Bindings.createObjectBinding(() -> {
            Image image = null;
            
            // Try to fetch cover image from file and as a second option from the ID3 tag
            if(currentlyPlayingMedia != null) {
                image = MPUtils.fetchMediaCoverArt(currentlyPlayingMedia);
                if(image == null) {
                    image = MPUtils.imageFromID3Tag(
                            (ByteArrayInputStream) currMediaAttribsProperty.getValue().get("mp3.id3tag.v2"));
                }
            }
            return image;
        }, currMediaAttribsProperty));
        mig.add(imageView);
        
        // Playback controls
        mig.add(new Separator(Orientation.HORIZONTAL), "span 2, growx");
        HBox mediaBarBox = new HBox();
        mediaBarBox.setPadding(new Insets(5, 10, 5, 10));
        mediaBarBox.setSpacing(5);
        mediaBarBox.setAlignment(Pos.CENTER);
        mig.add(mediaBarBox, "span 2, growx");
        borderPane.setBottom(mig);

        // Buttons
        mediaBarBox.getChildren().addAll(
                createFBButton(), createPlayButton(), createFFButton());
        
        // Transport slider
        mediaBarBox.getChildren().add(new Label("Time: "));
        mediaBarBox.getChildren().add(createTimeSlider());
        HBox.setHgrow(timeSlider, Priority.ALWAYS);

        // Play time label
        playTime = new Label();
        playTime.setPrefWidth(130);
        playTime.setMinWidth(50);
        mediaBarBox.getChildren().add(playTime);

        // Volume slider
        mediaBarBox.getChildren().add(new Label("Vol: "));
        mediaBarBox.getChildren().add(createVolumeSlider());
        
        // Info
        Button infoButton = new Button("", Icons.info());
        infoButton.disableProperty().bind(Bindings.createObjectBinding(() -> {
            return currMediaAttribsProperty.getValue().isEmpty();
        }, currMediaAttribsProperty));
        infoButton.setOnAction(event -> {
            ComponentUtils.showMapPropertiesDialog(currMediaAttribsProperty.getValue(), 
                    format("Properties of ''{0}''", currentlyPlayingMedia.getName()));
        });
        mediaBarBox.getChildren().add(infoButton);
        
        // Clear
        Button clear = new Button("", new FontIcon(FontAwesomeRegular.TRASH_ALT));
        clear.setOnAction(event -> {
            clearPlaylist();
        });
        
        // Loop
        loopToggle = new ToggleButton("", new FontIcon(FontAwesomeSolid.REDO));
        loopToggle.setSelected(Boolean.valueOf(
                ConfigService.getInstance().getOrCreateProperty("mplayer.loop", Boolean.FALSE.toString())));
        loopToggle.setOnAction(event -> {
            ConfigService.getInstance().setProperty("mplayer.loop", String.valueOf(loopToggle.isSelected()));
        });
        mediaBarBox.getChildren().addAll(new Separator(Orientation.VERTICAL), clear, loopToggle);
        
        // Scene
        setWidth(600);
        setHeight(400);
        setScene(new Scene(borderPane));
    }
    
    private Button createPlayButton() {
        playButton = new Button("", Icons.play());
        playButton.setOnAction(event -> {
            onPlayButtonClicked();
        });
        return playButton;
    }
    
    private Button createFFButton() {
        Button b = new Button("", Icons.fastForward());
        b.setOnAction(event -> {
            try {
                playNext();
            } catch (Exception e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
            }
        });
        return b;
    }
    
    private Button createFBButton() {
        Button b = new Button("", Icons.fastBackward());
        b.setOnAction(event -> {
            try {
                playPrev();
            } catch (Exception e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
            }
        });
        return b;
    }
    
    private Slider createTimeSlider() {
        timeSlider = new Slider();
        timeSlider.setMinWidth(50);
        timeSlider.setMaxWidth(Double.MAX_VALUE);
        timeSlider.setMax(1);
        timeSlider.setDisable(true);
        timeSlider.setOnMouseReleased(event -> {
            onTimeSliderValueChangedByUser();
        });
        return timeSlider;
    }
    
    private Slider createVolumeSlider() {
        volumeSlider = new Slider();
        volumeSlider.setPrefWidth(70);
        volumeSlider.setMaxWidth(Region.USE_PREF_SIZE);
        volumeSlider.setMinWidth(30);
        volumeSlider.setDisable(true);
        return volumeSlider;
    }
    
    private ListView<MPMedia> createPlaylist() {
        playlist = new ListView<>();
        
        // Custom list cell
        playlist.setCellFactory(value -> {
            return new MPMediaListCell() {
                protected void updateItem(MPMedia item, boolean empty) {
                    updateItem(item, empty, currentlyPlayingMedia);
                }
            };
        });
        
        // Double click
        playlist.setOnMouseClicked(event -> {
            MPMedia media = playlist.getSelectionModel().getSelectedItem();
            if (event.getClickCount() == 2 && media != null) {
                playMedia(media);
            }
        });
        
        // Context menu
        MenuItem removeMenuItem = new MenuItem("Remove");
        removeMenuItem.setOnAction((event) -> {
            removeSelectedFromPlaylist();
        });
        playlist.setContextMenu(new ContextMenu(removeMenuItem));
        
        return playlist;
    }
    
    private void loadStoredPlaylist() {
        List<MPMedia> pl = PlaylistPersistenceService.getInstance().loadPlaylist();
        if(pl != null) {
            playlist.getItems().addAll(pl);
        }
    }
    
    /* Event Handlers */
    
    private void onPlayButtonClicked() {
        
        // Pause current, resume current or play first media in playlist
        try {
            if(player.isPlaying()) {
                player.pause();
            } else if(player.isPaused()) {
                player.resume();
            } else if(currentlyPlayingMedia == null && !playlist.getItems().isEmpty()){
                MPMedia selected = playlist.getItems().get(0);
                if(selected != null) {
                    playMedia(selected);
                }
            } 
        } catch (AudioPlayerException e) {
            LOG.log(Level.WARNING, "Audio player error", e);
        }
    }
    
    private void onTimeSliderValueChangedByUser() {
        /*
        if(currMediaDurationKnown()) {
            try {
                sp.seekTo((int) (timeSlider.getValue() * currMediaDurSec));
            } catch (Exception e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
            }
        }
        */
    }
    
    private void onPlayerOpened(Map<String, Object> properties) {
        Platform.runLater(() -> {
            currMediaAttribsProperty.set(properties);
        });
    }
    
    private void onPlayerStarted() {
        Platform.runLater(() -> {
            playButton.setGraphic(Icons.pause());
        });
    }
    
    private void onPlayerStopped() {
        Platform.runLater(() -> {
            playButton.setGraphic(Icons.play());
        });
    }
    
    private void onPlayerProgress(int elapsedSeconds) {
        Platform.runLater(() -> {
            
            // Update elapsed time label
            int hrs = (elapsedSeconds / 60) / 60;
            int min = (elapsedSeconds / 60) % 60;
            int sec = elapsedSeconds % 60;
            playTime.setText(MessageFormat.format("{0}{1}:{2}{3}:{4}{5}", 
                    hrs < 10 ? "0" : "", hrs, 
                    min < 10 ? "0" : "", min, 
                    sec < 10 ? "0" : "", sec));
            
            // Update progress slider (make sure user is not touching the slider and update)
            if(currentlyPlayingMpp.getDurationSec() > 0 && !timeSlider.isValueChanging()) {
                timeSlider.setValue((float) (elapsedSeconds) / currentlyPlayingMpp.getDurationSec());
            }
        });
    }
    
    private void onEndOfMedia() {
        playNext();
    }
    
    /* Utilities */
    
    private void playNext() {
        ObservableList<MPMedia> items = playlist.getItems();
        int idx = items.indexOf(currentlyPlayingMedia);
        if(idx > -1 && idx < items.size() - 1) {
            playMedia(items.get(++idx));
        } else if(idx == items.size() - 1 && loopToggle.isSelected()){
            playMedia(items.get(0));
        }
    }
    
    private void playPrev() {
        ObservableList<MPMedia> items = playlist.getItems();
        int idx = items.indexOf(currentlyPlayingMedia);
        if(idx > 0) {
            playMedia(items.get(--idx));
        }
    }
    
    public void playMedia(MPMedia media) {
        currentlyPlayingMedia = media;
        currentlyPlayingMpp = new MediaPreProcessor(media);
        
        // Set credentials for non-local sources, i.e. URLs
        if(!media.isLocal()) {
            setGlobalCredentials(media);
        }
        
        // Open and start player
        try {
            player.open(new AudioSource(media.getSource()));
            player.play();
        } catch (AudioPlayerException e) {
            LOG.log(Level.WARNING, "Audio playback failed", e);
            return;
        }
        
        Platform.runLater(() -> {
            
            // Force list cells refresh 
            playlist.refresh();
            
            // Reset progress
            timeSlider.setValue(0);
        });
        
    }
    
    private boolean removeSelectedFromPlaylist() {
        MPMedia media = playlist.getSelectionModel().getSelectedItem();
        return removeFromPlaylist(media);
    }
    
    private void setGlobalCredentials(MPMedia media) {
        if(media.getUser() != null && media.getPassword() != null) {
            Authenticator.setDefault(MPUtils.createAuthenticator(media));
        }
    }
}
