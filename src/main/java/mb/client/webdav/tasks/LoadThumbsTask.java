package mb.client.webdav.tasks;

import static java.text.MessageFormat.format;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import mb.client.webdav.model.ResourceTableItem;
import mb.client.webdav.service.ImageThumbService;
import mb.client.webdav.service.WebDAVService;
import mb.client.webdav.service.WebDAVServiceException;
import net.coobird.thumbnailator.Thumbnails;

public class LoadThumbsTask extends Task<Map<ResourceTableItem, Image>> {
    
    private static final Logger LOG = Logger.getLogger(LoadThumbsTask.class.getName());
    
    private WebDAVService service;
    private List<ResourceTableItem> items;
    private int width;
    private Map<ResourceTableItem, Image> thumbMap;
    
    public LoadThumbsTask(WebDAVService service, List<ResourceTableItem> items, 
            Map<ResourceTableItem, Image> thumbMap, int width) {
        this.service = service;
        this.items = items;
        this.width = width;
        this.thumbMap = thumbMap;
    }

    @Override
    protected Map<ResourceTableItem, Image> call() throws Exception {
        
        thumbMap.clear();
        for (ResourceTableItem item : items) {
            
            // Check if cancelled
            if(isCancelled()) {
                LOG.fine("Task cancelled");
                break;
            }
            
            if(item.isImage() || item.isPdf()) {
                LOG.fine(format("Start processing thumb for {0}", item));
                
                Image image = null;
                ImageThumbService its = ImageThumbService.getInstance();
                if(its.thumbExists(item.getDavRes())) {
                    
                    image = its.getThumb(item.getDavRes());
                    LOG.fine(format("Fetched existing thumb for {0}", item));
                } else {
                    
                    BufferedImage buffImg = null;
                    InputStream is = null;
                    try {
                        
                        // Fetch resource content
                        is = service.getContent(item.getDavRes());
                        if(item.isImage()) {
                            buffImg = ImageIO.read(is);
                        } else {
                            PDDocument pdf = PDDocument.load(is);
                            PDFRenderer renderer = new PDFRenderer(pdf);
                            buffImg = renderer.renderImage(0);
                            pdf.close();
                        }
                        
                        // Resize
                        buffImg = Thumbnails.of(buffImg)
                                .size(width, width)
                                .keepAspectRatio(true)
                                .asBufferedImage();
                        
                        // Save
                        File file = its.saveThumb(item.getDavRes(), buffImg);
                        LOG.fine(format("Saved new thumb for {0}, at {1}", item, file.getAbsolutePath()));
                        
                        // Convert
                        image = SwingFXUtils.toFXImage(buffImg, null);
                        
                    } catch (WebDAVServiceException e) {
                        LOG.log(Level.WARNING, e.getMessage(), e);
                    } finally {
                        IOUtils.closeQuietly(is);
                    }
                }
                
                // Update
                thumbMap.put(item, image);
                updateProgress(thumbMap.size(), items.size());
                updateMessage("Loaded thumb for " + item.getName());
            }
        }
        return thumbMap;
    }

}
