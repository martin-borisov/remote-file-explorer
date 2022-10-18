package mb.client.webdav.media.audio;

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import mb.jflac.sound.spi.FlacAudioFileReader;

public class AudioSystemWrapper {

    public static AudioFileFormat getAudioFileFormat(final InputStream stream)
            throws UnsupportedAudioFileException, IOException {
        try {
            // Fix for provider ordering and MPEG provider consuming FLAC streams
            return new FlacAudioFileReader().getAudioFileFormat(stream);
        } catch (Exception e) {
            return AudioSystem.getAudioFileFormat(stream);
        }
    }
    
    public static AudioInputStream getAudioInputStream(final InputStream stream)
            throws UnsupportedAudioFileException, IOException {
        try {
            // Fix for provider ordering and MPEG provider consuming FLAC streams
            return new FlacAudioFileReader().getAudioInputStream(stream);
        } catch (Exception e) {
            return AudioSystem.getAudioInputStream(stream);
        }
    }

}
