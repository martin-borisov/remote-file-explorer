package mb.client.webdav.media;

import static java.text.MessageFormat.format;

import java.io.ByteArrayInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mpatric.mp3agic.AbstractID3v2Tag;
import com.mpatric.mp3agic.ID3v2TagFactory;

import javafx.scene.image.Image;

public class MPUtils {
    
    private static final Logger LOG = Logger.getLogger(MPUtils.class.getName());
    
    public static Image imageFromID3Tag(ByteArrayInputStream bytes) {
        Image image = null;
        if(bytes != null) {
            try {
                AbstractID3v2Tag tag = ID3v2TagFactory.createTag(bytes.readAllBytes());
                byte[] imageData = tag.getAlbumImage();
                if(imageData != null) {
                    image = new Image(new ByteArrayInputStream(imageData), 100, 100, true, true);
                    
                    // Dump some useful data
                    LOG.fine(format("Album art mime type: {0}", tag.getAlbumImageMimeType()));
                    LOG.fine(format("Album art size: {0}x{1}px", image.getWidth(), image.getHeight()));
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
            }
        }
        return image;
    }
}
