package mb.client.webdav.media;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import mb.client.webdav.components.Icons;

public class MPControl extends BorderPane {
    
    private static final Logger LOG = Logger.getLogger(MPControl.class.getName());

    private Button playButton;
    private MediaPlayer player;
    private Duration duration;
    private Slider timeSlider;
    private Label playTime;
    private Slider volumeSlider;
    private Runnable endOfMediaRunner, onPlayerReadyRunner;
    
    private final boolean repeat = false;
    private boolean atEndOfMedia = false;

    public MPControl() {
        
        // Layout
        HBox mediaBarBox = new HBox();
        mediaBarBox.setPadding(new Insets(5, 10, 5, 10));
        mediaBarBox.setSpacing(5);
        setBottom(mediaBarBox);

        // Play button
        mediaBarBox.getChildren().addAll(
                new Button("", Icons.fastBackward()), createPlayButton(), new Button("", Icons.fastForward()));
        
        // Time slider
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
    }
    
    public void setPlayer(MediaPlayer p) {
        
        if(p == null) {
            throw new IllegalArgumentException("Need valid player instance");
        }
        
        LOG.fine(format("Adding player instance with media: ''{0}''", p.getMedia().getSource()));
        
        // Stop and remove current player if it exists
        if(player != null) {
            player.stop();
            player.dispose();
        }
        
        // Cleanup
        playTime.textProperty().unbind();
        timeSlider.valueProperty().unbind();
        
        player = p;
        player.setOnReady(() -> {
            onPlayerReady();
        });
        player.setOnPlaying(() -> {
            onPlayerStartingToPlay();
        });
        player.setOnPaused(() -> {
            onPlayerPaused();
        });
        player.setOnEndOfMedia(() -> {
            onPlayerEndOfMedia();
        });
        player.setCycleCount(repeat ? MediaPlayer.INDEFINITE : 1);
        player.volumeProperty().bind(volumeSlider.valueProperty().divide(100));
    }
    
    public final void setOnEndOfMedia(Runnable endOfMediaRunner) {
        this.endOfMediaRunner = endOfMediaRunner;
    }
    
    public void setOnPlayerReady(Runnable onPlayerReadyRunner) {
        this.onPlayerReadyRunner = onPlayerReadyRunner;
    }

    public void play() {
        if(player != null) {
            Status status = player.getStatus();
            if (status == Status.PAUSED
                    || status == Status.READY
                    || status == Status.STOPPED) {
            
                // Rewind the media if we're sitting at the end
                /*
                if (atEndOfMedia) {
                    player.seek(player.getStartTime());
                    atEndOfMedia = false;
                }
                */
            
                player.play();
            }
        }
    }
    
    private Button createPlayButton() {
        playButton = new Button("", Icons.play());
        playButton.setOnAction(event -> {
            onPlayButtonClicked();
        });
        return playButton;
    }
    
    private Slider createTimeSlider() {
        timeSlider = new Slider();
        timeSlider.setMinWidth(50);
        timeSlider.setMaxWidth(Double.MAX_VALUE);

        /*
        timeSlider.valueProperty().addListener(obs -> {
            if (timeSlider.isValueChanging()) {
                
                // Multiply duration by percentage calculated by slider position
                player.seek(duration.multiply(timeSlider.getValue() / 100.0));
            } 
        });
        */
        return timeSlider;
    }
    
    private Slider createVolumeSlider() {
        volumeSlider = new Slider();
        volumeSlider.setPrefWidth(70);
        volumeSlider.setMaxWidth(Region.USE_PREF_SIZE);
        volumeSlider.setMinWidth(30);
        return volumeSlider;
    }
    
    /* Event Handlers */
    
    private void onPlayerReady() {
        
        // TODO Looks like duration is wrong. Could be a bug.
        duration = player.getMedia().getDuration();
        
        LOG.fine(format("Player ready, media duration: {0,number,#}s", (int) duration.toSeconds()));
            
        // Current time label binding
        playTime.textProperty().bind(
                Bindings.createStringBinding(() -> {
                    return formatTime(player.getCurrentTime(), duration);
                }, player.currentTimeProperty()));
        
        // Progress slider binding
        timeSlider.valueProperty().bind(
                Bindings.createDoubleBinding(() -> {
                    return (player.currentTimeProperty().get().toSeconds() / duration.toSeconds()) * 100;
                }, player.currentTimeProperty()));
        
        if(onPlayerReadyRunner != null) {
            onPlayerReadyRunner.run();
        }
    }
    
    private void onPlayerStartingToPlay() {
        playButton.setGraphic(Icons.pause());
    }
    
    private void onPlayerPaused() {
        playButton.setGraphic(Icons.play());
    }
    
    private void onPlayerEndOfMedia() {
        if (!repeat) {
            playButton.setGraphic(Icons.play());
            atEndOfMedia = true;
        }
        
        if(endOfMediaRunner != null) {
            endOfMediaRunner.run();
        }
    }
    
    private void onPlayButtonClicked() {
        if(player != null) {
            Status status = player.getStatus();
            
            LOG.fine(format("Valid player instance with status: {0}", status));

            // Don't do anything in these states
            if (status == Status.UNKNOWN || status == Status.HALTED) {
                LOG.fine("Doing nothing due to current status");
                return;
            }

            if (status == Status.PAUSED
                    || status == Status.READY
                    || status == Status.STOPPED) {
            
                // Rewind the media if we're sitting at the end
                if (atEndOfMedia) {
                    player.seek(player.getStartTime());
                    atEndOfMedia = false;
                }
            
                LOG.fine("Starting to play");
                player.play();
            } else {
                LOG.fine("Will pause");
                player.pause();
            }
        } else {
            LOG.fine("Player instance is null. Will not play.");
        }
    }

    private void updateValues() {
        Platform.runLater(() -> {
            
            Duration currentTime = player.getCurrentTime();
            timeSlider.setDisable(duration.isUnknown());
            if (!timeSlider.isDisabled() && duration.greaterThan(Duration.ZERO) && !timeSlider.isValueChanging()) {
                timeSlider.setValue(currentTime.divide(duration).toMillis() * 100.0);
            }
            if (!volumeSlider.isValueChanging()) {
                volumeSlider.setValue((int) Math.round(player.getVolume() * 100));
            }
        });
    }

    private static String formatTime(Duration elapsed, Duration duration) {
        int intElapsed = (int) Math.floor(elapsed.toSeconds());
        int elapsedHours = intElapsed / (60 * 60);
        if (elapsedHours > 0) {
            intElapsed -= elapsedHours * 60 * 60;
        }
        int elapsedMinutes = intElapsed / 60;
        int elapsedSeconds = intElapsed - elapsedHours * 60 * 60
                - elapsedMinutes * 60;

        if (duration.greaterThan(Duration.ZERO)) {
            int intDuration = (int) Math.floor(duration.toSeconds());
            int durationHours = intDuration / (60 * 60);
            if (durationHours > 0) {
                intDuration -= durationHours * 60 * 60;
            }
            int durationMinutes = intDuration / 60;
            int durationSeconds = intDuration - durationHours * 60 * 60
                    - durationMinutes * 60;
            if (durationHours > 0) {
                return String.format("%d:%02d:%02d / %d:%02d:%02d",
                        elapsedHours, elapsedMinutes, elapsedSeconds,
                        durationHours, durationMinutes, durationSeconds);
            } else {
                return String.format("%02d:%02d / %02d:%02d",
                        elapsedMinutes, elapsedSeconds, durationMinutes,
                        durationSeconds);
            }
        } else {
            if (elapsedHours > 0) {
                return String.format("%d:%02d:%02d", elapsedHours,
                        elapsedMinutes, elapsedSeconds);
            } else {
                return String.format("%02d:%02d", elapsedMinutes,
                        elapsedSeconds);
            }
        }
    }
}