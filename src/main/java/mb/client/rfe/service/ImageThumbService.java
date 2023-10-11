package mb.client.rfe.service;

import static java.text.MessageFormat.format;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import javafx.scene.image.Image;
import mb.client.rfe.model.WebDAVResource;

public class ImageThumbService {
    
    private static final String DEFAULT_EXTENSION = "jpg";
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
        return thumbToFile(res).exists();
    }
    
    public File saveThumb(WebDAVResource res, BufferedImage image) {
        
        File file = thumbToFile(res);
        if(!file.exists()) {
            
            String path = res.getAbsolutePath();
            LOG.info(format("Saving thumb for: {0}", path));
            
            FileOutputStream os = null;
            try {
                ImageIO.write(image, DEFAULT_EXTENSION, 
                        os = FileUtils.openOutputStream(file));
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
        File file = thumbToFile(res);
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
    
    private static File thumbToFile(WebDAVResource res) {
        
        // Take care of local FS where host is missing
        String baseDir = res.getBaseURI().getHost();
        if(baseDir == null) {
            baseDir = "local";
        }
        
        String filePath = FilenameUtils.removeExtension(res.getAbsolutePath()) + "." + DEFAULT_EXTENSION;
        
        // Take care of Windows paths
        if(filePath.contains(":")) {
            filePath = filePath.replace(":", "");
            baseDir = baseDir + "/";
        }
        
        return new File("thumbs/" + baseDir + filePath);
    }
}
 