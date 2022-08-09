package mb.client.webdav.service;

import static java.text.MessageFormat.format;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import mb.client.webdav.model.WebDAVResource;

public class ImageThumbService {
    
    private static final Logger LOG = Logger.getLogger(ImageThumbService.class.getName());
    private static ImageThumbService ref;
    
    public static ImageThumbService getInstance() {
        synchronized (ImageThumbService.class) {
            if (ref == null) {
                ref = new ImageThumbService();
            }
        }
        return ref;
    }

    private ImageThumbService() {
    }
    
    public boolean thumbExists(WebDAVResource res) {
        return createFileForResource(res).exists();
    }
    
    public File saveThumb(WebDAVResource res, Image image) {
        
        File file = createFileForResource(res);
        if(!file.exists()) {
            
            LOG.info(format("Saving thumb for: {0}", res.getAbsolutePath()));
            
            FileOutputStream os = null;
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "jpg", 
                        os = FileUtils.openOutputStream(createFileForResource(res)));
            } catch (IOException e) {
                LOG.log(Level.SEVERE, e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(os);
            }
        }
        return file;
    }
    
    public Image getThumb(WebDAVResource res) {
        
        Image image = null;
        File file = createFileForResource(res);
        if(file.exists()) {
            
            InputStream is = null;
            try {
                is = FileUtils.openInputStream(file);
                image = new Image(is);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
        return image;
    }
    
    private static File createFileForResource(WebDAVResource res) {
        return new File("thumbs/" + res.getBaseURI().getHost() + res.getAbsolutePath());
    }
}
 