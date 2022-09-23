package mb.client.webdav.media;

import static java.text.MessageFormat.format;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.Authenticator;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;

import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.tbee.javafx.scene.layout.MigPane;

import com.goxr3plus.streamplayer.stream.StreamPlayer;
import com.goxr3plus.streamplayer.stream.StreamPlayerEvent;
import com.goxr3plus.streamplayer.stream.StreamPlayerException;
import com.goxr3plus.streamplayer.stream.StreamPlayerListener;
import com.goxr3plus.streamplayer.tools.TimeTool;

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
import mb.client.webdav.components.ComponentUtils;
import mb.client.webdav.components.Icons;
import mb.client.webdav.service.ConfigService;

public class MPlayer extends Stage {
  
    private static final Logger LOG = Logger.getLogger(MPlayer.class.getName());
    private static final int DURATION_UNKNOWN = -1;
    
    private Button playButton;
    private Slider timeSlider, volumeSlider;
    private Label playTime;
    private ToggleButton loopToggle;
    private ListView<MPMedia> playlist;
    private MPMedia currentlyPlayingMedia;
    private StreamPlayer sp;
    private int currMediaDurSec;
    private SimpleObjectProperty<Map<String, Object>> currMediaAttribsProperty;

    public MPlayer() {
        super();
        setTitle("Audio Player");
        createProperties();
        createStreamPlayer();
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
    
    private void createStreamPlayer() {
        sp = new StreamPlayer();
        sp.addStreamPlayerListener(new StreamPlayerListener() {
            public void opened(Object dataSource, Map<String, Object> properties) {
                onPlayerOpened(dataSource, properties);
            }
            public void statusUpdated(StreamPlayerEvent event) {
                onPlayerStatusUpdated(event);
            }
            public void progress(int nEncodedBytes, long microsecondPosition, 
                    byte[] pcmData, Map<String, Object> properties) {
                onPlayerProgress(nEncodedBytes, microsecondPosition, pcmData, properties);
            }
        });
    }
    
    public void destroy() {
        sp.stop();
        sp.reset();
        
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
            }
            
            if(tags.containsKey("basicplayer.sourcedataline")) {
                SourceDataLine line = (SourceDataLine) tags.get("basicplayer.sourcedataline");
                AudioFormat format = line.getFormat();
                buf.append(format("{0,number,#} Hz | {1} bit | {2} channels", 
                        format.getFrameRate(), format.getSampleSizeInBits(), format.getChannels()));
                
                if(tags.containsKey("bitrate")) {
                    buf.append(format(" | {0,number,#} kbs", Integer.valueOf(tags.get("bitrate").toString()) / 1000));
                }
                
                if(Boolean.valueOf(String.valueOf(tags.get("vbr")))) {
                    buf.append(" vbr");                }
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
            ComponentUtils.showMapPropertiesDialog(currMediaAttribsProperty.getValue());
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
        timeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
        });
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
        if(sp.isPlaying()) {
            sp.pause();
        } else if(sp.isPaused()) {
            sp.resume();
        } else if(currentlyPlayingMedia == null && !playlist.getItems().isEmpty()){
            MPMedia selected = playlist.getItems().get(0);
            if(selected != null) {
                playMedia(selected);
            }
        }
    }
    
    private void onTimeSliderValueChangedByUser() {
        if(currMediaDurationKnown()) {
            try {
                sp.seekTo((int) (timeSlider.getValue() * currMediaDurSec));
            } catch (Exception e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
            }
        }
    }
    
    private void onPlayerOpened(Object dataSource, Map<String, Object> properties) {
        LOG.fine(format("Player opened with media duration: {0,number,#}", 
                currMediaDurSec = sp.getDurationInSeconds()));
        
        if(currMediaDurationKnown()) {
            timeSlider.setDisable(false);
        } else {
            timeSlider.setDisable(true);
        }
        
        Platform.runLater(() -> {
            currMediaAttribsProperty.set(properties);
        });
    }
    
    private void onPlayerStatusUpdated(StreamPlayerEvent event) {
        
        switch (event.getPlayerStatus()) {
        case PAUSED:
            Platform.runLater(() -> {
                playButton.setGraphic(Icons.play());
            });
            LOG.fine(event.getPlayerStatus() + " status handled");
            break;
            
        case STOPPED:
            Platform.runLater(() -> {
                playButton.setGraphic(Icons.play());
            });
            LOG.fine(event.getPlayerStatus() + " status handled");
            break;
            
        case PLAYING:
        case RESUMED:
            Platform.runLater(() -> {
                playButton.setGraphic(Icons.pause());
            });
            LOG.fine(event.getPlayerStatus() + " status handled");
            break;
            
        case EOM:
            onEndOfMedia();
            LOG.fine(event.getPlayerStatus() + " status handled");
            break;
            
        case SEEKED:
            // TODO Perhaps do something here?
        default:
            LOG.fine(event.getPlayerStatus() + " status received but no handling needed");
        }
    }
    
    private void onPlayerProgress(int nEncodedBytes, long microsecondPosition, byte[] pcmData,
            Map<String, Object> properties) {

        long totalBytes = sp.getTotalBytes();

        // Calculate progress and elapsed time
        double progress = (nEncodedBytes > 0 && totalBytes > 0) ? (nEncodedBytes * 1.0f / totalBytes * 1.0f) : 0;
        int sec = (int) (microsecondPosition / 1000000);

        // Update UI
        // TODO This should not be called so often, but only two times per second
        Platform.runLater(() -> {

            // Make sure user is not touching the slider and update
            if (!timeSlider.isValueChanging()) {
                timeSlider.setValue(progress);
            }

            playTime.setText(TimeTool.getTimeEdited(sec)
                    + (currMediaDurationKnown() ? (" / " + TimeTool.getTimeEdited(currMediaDurSec)) : ""));
        });

    }
    
    private void onEndOfMedia() {
        
        // Play next in playlist only when the player has fully stopped, otherwise it hangs
        new Thread(() -> {
            while(!sp.isStopped()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
            }
            playNext();
        }).start();
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
        
        // Files get support for things like duration and progress
        // as opposed to remote URLs
        Object source;
        if(media.isLocal()) {
            try {
                source = new File(new URI(media.getSource()));
            } catch (URISyntaxException e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
                return;
            }
        } else {
            try {
                setGlobalCredentials(media);
                source = new URL(media.getSource());
            } catch (Exception e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
                return;
            }
        }
        
        // NB: Absolutely call stop() before open()
        // otherwise the player instance will just hang due to synchronization issues
        sp.stop();
        try {
            sp.open(source);
            sp.play();
            playlist.refresh();
        } catch (StreamPlayerException e) {
            LOG.log(Level.WARNING, "Playback failed", e);
        }
    }
    
    private boolean removeSelectedFromPlaylist() {
        MPMedia media = playlist.getSelectionModel().getSelectedItem();
        return removeFromPlaylist(media);
    }
    
    private boolean currMediaDurationKnown() {
        return currMediaDurSec != DURATION_UNKNOWN;
    }
    
    private void setGlobalCredentials(MPMedia media) {
        if(media.getUser() != null && media.getPassword() != null) {
            Authenticator.setDefault(MPUtils.createAuthenticator(media));
        }
    }
}
