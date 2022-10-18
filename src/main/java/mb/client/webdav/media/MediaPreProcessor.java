package mb.client.webdav.media;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import mb.jflac.FLACDecoder;
import mb.jflac.metadata.StreamInfo;

public class MediaPreProcessor {
    
    private static final Logger LOG = Logger.getLogger(MediaPreProcessor.class.getName());
    private MPMedia media;
    private long totalSamples, sampleRate, durationSec;

    public MediaPreProcessor(MPMedia media) {
        this.media = media;
        process();
    }
    
    public long getTotalSamples() {
        return totalSamples;
    }
    
    public long getSampleRate() {
        return sampleRate;
    }
    
    public long getDurationSec() {
        return durationSec;
    }

    private void process() {
        if(media.isLocal()) {
            if(media.getSource().endsWith("flac")) {
                try(FileInputStream fis = new FileInputStream(new File(URI.create(media.getSource())))) {
                    
                    FLACDecoder decoder = new FLACDecoder(fis);
                    decoder.decode();
                    StreamInfo info = decoder.getStreamInfo();
                    if(info != null) {
                        totalSamples = info.getTotalSamples();
                        sampleRate = info.getSampleRate();
                        
                        if(totalSamples > 0 && sampleRate > 0) {
                            durationSec = (long) (totalSamples / sampleRate);
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Media pre processing failed", e);
                } 
            } else if(media.getSource().endsWith("mp3")) {
                // TODO
            } else if(media.getSource().endsWith("wav")) {
                // TODO
            }
        }
    }

}
