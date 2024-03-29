package mb.client.rfe.tasks;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.concurrent.Task;
import mb.client.rfe.model.WebDAVResource;
import mb.client.rfe.service.ResourceRepositoryService;
import mb.client.rfe.service.WebDAVServiceException;

public class ListDirsTask extends Task<List<WebDAVResource> > {
    
    private static final Logger LOG = Logger.getLogger(ListDirsTask.class.getName());
    
    private ResourceRepositoryService service;
    private String path;
    private boolean sorted;
    
    public ListDirsTask(ResourceRepositoryService service, String path) {
        this.service = service;
        this.path = path;
    }
    
    public ListDirsTask(ResourceRepositoryService service, String path, boolean sorted) {
        this(service, path);
        this.sorted = sorted;
    }

    @Override
    protected List<WebDAVResource> call() throws Exception {
        List<WebDAVResource> dirs = null;
        try {
            dirs = service.listDirs(path);
            
            if(sorted) {
                dirs = dirs.stream()
                    .sorted(Comparator.comparing(WebDAVResource::getName))
                    .collect(Collectors.toList());
            }
        } catch (WebDAVServiceException e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            updateMessage(e.getMessage());
        }
        return dirs;
    }

}
