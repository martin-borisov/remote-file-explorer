package mb.client.webdav.tasks;

import static java.text.MessageFormat.format;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import mb.client.webdav.model.ResourceTableItem;
import mb.client.webdav.service.ImageThumbService;
import mb.client.webdav.service.WebDAVService;
import mb.client.webdav.service.WebDAVServiceException;

public class LoadThumbsTask extends Task<Map<ResourceTableItem, Image>> {
    
    private static final Logger LOG = Logger.getLogger(LoadThumbsTask.class.getName());
    
    private WebDAVService service;
    private List<ResourceTableItem> items;
    private int width;
    
    public LoadThumbsTask(WebDAVService service, List<ResourceTableItem> items, int width) {
        this.service = service;
        this.items = items;
        this.width = width;
    }

    @Override
    protected Map<ResourceTableItem, Image> call() throws Exception {
        
        Map<ResourceTableItem, Image> map = new HashMap<>(items.size());
        for (ResourceTableItem item : items) {
            
            // Check if cancelled
            if(isCancelled()) {
                LOG.fine("Task cancelled");
                break;
            }
            
            if(item.isImage()) {
                LOG.fine(format("Start processing thumb for {0}", item));
                
                Image image = null;
                ImageThumbService its = ImageThumbService.getInstance();
                if(its.thumbExists(item.getDavRes())) {
                    
                    image = its.getThumb(item.getDavRes());
                    LOG.fine(format("Fetched existing thumb for {0}", item));
                } else {
                    
                    InputStream is = null;
                    try {
                        is = service.getContent(item.getDavRes());
                        
                        // TODO "Smooth" should be a configurable property
                        image = new Image(is, width, width, true, true);
                        File file = its.saveThumb(item.getDavRes(), image);
                        LOG.fine(format("Saved new thumb for {0}, at {1}", item, file.getAbsolutePath()));
                    } catch (WebDAVServiceException e) {
                        LOG.log(Level.WARNING, e.getMessage(), e);
                    } finally {
                        IOUtils.closeQuietly(is);
                    }
                }
                
                map.put(item, image);
                
                // Try to update during processing, but this is not guaranteed to succeed
                updateValue(map);
                updateProgress(map.size(), items.size());
                updateMessage("Loaded thumb for " + item.getName());
            }
        }
        return map;
    }

}
