package mb.client.webdav.tasks;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.concurrent.Task;
import mb.client.webdav.model.WebDAVResource;
import mb.client.webdav.service.WebDAVService;
import mb.client.webdav.service.WebDAVServiceException;

public class ListDirsTask extends Task<List<WebDAVResource> > {
    
    private static final Logger LOG = Logger.getLogger(ListDirsTask.class.getName());
    
    private WebDAVService service;
    private String path;
    
    public ListDirsTask(WebDAVService service, String path) {
        this.service = service;
        this.path = path;
    }

    @Override
    protected List<WebDAVResource> call() throws Exception {
        List<WebDAVResource> dirs = null;
        try {
            dirs = service.listDirs(path);
        } catch (WebDAVServiceException e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            updateMessage(e.getMessage());
        }
        return dirs;
    }

}
