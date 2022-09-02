package mb.client.webdav.media;

import static java.text.MessageFormat.format;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.tbee.javafx.scene.layout.MigPane;

import com.goxr3plus.streamplayer.stream.StreamPlayer;
import com.goxr3plus.streamplayer.stream.StreamPlayerEvent;
import com.goxr3plus.streamplayer.stream.StreamPlayerException;
import com.goxr3plus.streamplayer.stream.StreamPlayerListener;
import com.goxr3plus.streamplayer.tools.TimeTool;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import mb.client.webdav.components.ComponentUtils;
import mb.client.webdav.components.Icons;

public class MPlayer extends Stage {
  
    private static final Logger LOG = Logger.getLogger(MPlayer.class.getName());
    private static final int DURATION_UNKNOWN = -1;
    
    private Button playButton;
    private Slider timeSlider, volumeSlider;
    private Label playTime;
    private ListView<MPMedia> playlist;
    private int currentlyPlayingIdx = -1;
    private StreamPlayer sp;
    private int currMediaDurSec;
    private SimpleObjectProperty<Map<String, Object>> currMediaAttribsProperty;

    public MPlayer() {
        super();
        setTitle("Audio Player");
        createProperties();
        setupStreamPlayer();
        setupScene();
        loadStoredPlaylist();
    }
    
    public void addToPlaylist(MPMedia media) {
        playlist.getItems().add(media);
    }
    
    public boolean removeFromPlaylist(MPMedia media) {
        return playlist.getItems().remove(media);
    }
    
    private void createProperties() {
        currMediaAttribsProperty = new SimpleObjectProperty<Map<String,Object>>(
                Collections.emptyMap());
    }
    
    private void setupStreamPlayer() {
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
        MigPane mig = new MigPane("fill, wrap 3, debug",
                "[][right][grow]");
        
        // Album art
        ImageView imageView = new ImageView();
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        imageView.imageProperty().bind(Bindings.createObjectBinding(() -> {
            return MPUtils.imageFromID3Tag(
                    (ByteArrayInputStream) currMediaAttribsProperty.getValue().get("mp3.id3tag.v2"));
        }, currMediaAttribsProperty));
        mig.add(imageView, "span 1 3");
        
        mig.add(new Label("Artist:"));
        Label artistLabel = new Label();
        artistLabel.textProperty().bind(Bindings.createObjectBinding(() -> {
            return currMediaAttribsProperty.get().containsKey("author") ? (String) currMediaAttribsProperty.get().get("author") : "";
        }, currMediaAttribsProperty));
        mig.add(artistLabel);
        
        mig.add(new Label("Album:"));
        Label albumLabel = new Label();
        albumLabel.textProperty().bind(Bindings.createObjectBinding(() -> {
            return currMediaAttribsProperty.get().containsKey("album") ? (String) currMediaAttribsProperty.get().get("album") : "";
        }, currMediaAttribsProperty));
        mig.add(albumLabel);
        
        mig.add(new Label("Format:"));
        Label formatLabel = new Label();
        formatLabel.textProperty().bind(Bindings.createObjectBinding(() -> {
            Map<String, Object> tags = currMediaAttribsProperty.get();
            String value = "";
            if(tags.containsKey("audio.type")) {
                value = format("{0} / {1} Hz / {2} channels", tags.get("audio.type"), 
                    tags.get("audio.samplerate.hz"), tags.get("audio.channels"));
            }
            return value;
        }, currMediaAttribsProperty));
        mig.add(formatLabel);
        
        HBox mediaBarBox = new HBox();
        mediaBarBox.setPadding(new Insets(5, 10, 5, 10));
        mediaBarBox.setSpacing(5);
        mig.add(mediaBarBox, "span 3, growx");
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
        
        playlist.setCellFactory(value -> {
            return new ListCell<MPMedia>() {
                protected void updateItem(MPMedia media, boolean empty) {
                    super.updateItem(media, empty);
                    
                    if (empty) {
                        setGraphic(null);
                        setText(null);
                    } else if(media != null) {
                        
                        // Title
                        Text title = new Text(media.getName());
                        title.setStyle("-fx-font-weight: bold");
                        
                        // Source
                        Text source = new Text(media.getSource());
                        source.setStyle("-fx-fill: dimgrey");
                        
                        // Layout
                        HBox hBox = new HBox(title);
                        hBox.setSpacing(5);
                        hBox.setAlignment(Pos.BASELINE_LEFT);
                        
                        VBox vBox = new VBox(hBox, source);
                        vBox.setStyle("-fx-spacing: 5");
                        
                        // Playing
                        if(playlist.getItems().indexOf(media) == currentlyPlayingIdx) {
                            hBox.getChildren().add(0, Icons.play());
                        }
                        
                        setGraphic(vBox);
                        setText(null);
                    }
                }
            };
        });
        
        playlist.setOnMouseClicked(event -> {
                int idx = playlist.getSelectionModel().getSelectedIndex();
                if (event.getClickCount() == 2 && idx > -1) {
                    playMedia(idx);
                    playlist.refresh();
                }
            });
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
        
        currMediaAttribsProperty.set(properties);
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
            
        default:
            LOG.fine(event.getPlayerStatus() + " status received but no handling needed");
        }
    }
    
    private void onPlayerProgress(int nEncodedBytes, long microsecondPosition, 
            byte[] pcmData, Map<String, Object> properties) {
        
        // TODO This needs to be actually detected rather than hardcoded
        final String extension = "mp3";
        
        if ("mp3".equals(extension) || "wav".equals(extension)) {
            long totalBytes = sp.getTotalBytes();

            // Calculate progress and elapsed time
            double progress = (nEncodedBytes > 0 && totalBytes > 0) ? 
                    (nEncodedBytes * 1.0f / totalBytes * 1.0f) : 0;
            int sec = (int) (microsecondPosition / 1000000);
            
            // Update UI
            // TODO This should not be called so often, but only two times per second
            Platform.runLater(() -> {
                
                // Make sure user is not touching the slider and update
                if(!timeSlider.isValueChanging()) {
                    timeSlider.setValue(progress);
                }
                
                playTime.setText(TimeTool.getTimeEdited(sec) + 
                        (currMediaDurationKnown() ? (" / " + TimeTool.getTimeEdited(currMediaDurSec)) : ""));
            });

        } else {
            // System.out.println("Current time is : " + (int) (microsecondPosition /
            // 1000000) + " seconds");
        }
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
        if(currentlyPlayingIdx < (playlist.getItems().size() - 1)) {
            currentlyPlayingIdx++;
            playMedia(currentlyPlayingIdx);
            playlist.getSelectionModel().select(currentlyPlayingIdx);
        }
    }
    
    private void playPrev() {
        if(currentlyPlayingIdx > 0) {
            currentlyPlayingIdx--;
            playMedia(currentlyPlayingIdx);
            playlist.getSelectionModel().select(currentlyPlayingIdx);
        }
    }
    
    private void playMedia(int idx) {
        playMedia(playlist.getItems().get(idx));
        currentlyPlayingIdx = idx;
    }
    
    private void playMedia(MPMedia media) {
        Object source;
        
        // Files get support for things like duration and progress
        // as opposed to remote URLs
        if(media.isLocal()) {
            try {
                source = new File(new URI(media.getSource()));
            } catch (URISyntaxException e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
                return;
            }
        } else {
            try {
                source = new URL(media.getSource());
                setGlobalCredentials(media);
            } catch (MalformedURLException e) {
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
        } catch (StreamPlayerException e) {
            LOG.log(Level.WARNING, "Playback failed", e);
        }
    }
    
    private boolean currMediaDurationKnown() {
        return currMediaDurSec != DURATION_UNKNOWN;
    }
    
    private void setGlobalCredentials(MPMedia media) {
        if(media.getUser() != null && media.getPassword() != null) {
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(media.getUser(), media.getPassword().toCharArray());
                }
            });
        }
    }
    
}
