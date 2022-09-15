package mb.client.webdav.media;

import static java.text.MessageFormat.format;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
    
    public static Image fetchMediaCoverArt(MPMedia media) throws IOException {
        
        Image image = null;
        String source = media.getSource();
        int idx = source.lastIndexOf('/');
        if(idx > -1) {
            String path = source.substring(0, idx + 1); // Keep the forward slash
        
            if(media.isLocal()) {
                
                Path fullPath = Paths.get(path, "cover.jpg");
                if(Files.exists(fullPath)) {
                    image = new Image(Files.newInputStream(fullPath), 100, 100, true, true);
                } else {
                    LOG.fine(format("Cover image not found at: {0}", fullPath));
                }

            } else {
                
                URL url = new URL(path + "cover.jpg");
                LOG.fine(format("Trying to fetch cover image at URL: {0}", url));
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setAuthenticator(createAuthenticator(media));

                if (con.getResponseCode() == 200) {
                    try (InputStream is = con.getInputStream()) {
                        image = new Image(is, 100, 100, true, true);
                    }
                } else {
                    LOG.fine(format("Cover image missing or connection failed with response code {0}",
                            con.getResponseCode()));
                }
            }
        }
        return image;
    }
    
    public static Authenticator createAuthenticator(MPMedia media) {
        return new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(media.getUser(), media.getPassword().toCharArray());
            }
        };
    }
    
    public static Optional<String> getFileExtension(File file) {
        return Optional.ofNullable(file.getName())
                .filter(name -> name.contains("."))
                .map(name -> name.substring(name.lastIndexOf(".") + 1));
    }
    
    public static boolean isMedia(File file) {
        final List<String> extensions = Arrays.asList("mp3", "flac", "wav");
        Optional<String> ext = getFileExtension(file);
        return extensions.contains(ext.map(s -> s.toLowerCase()).orElse(""));
    }
}
