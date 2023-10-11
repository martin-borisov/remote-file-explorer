package mb.client.rfe.tasks;

import java.io.File;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.concurrent.Task;
import mb.client.rfe.model.WebDAVResource;
import mb.client.rfe.service.ResourceRepositoryService;

public class UploadFileTask extends Task<File> {
    
    private static final Logger LOG = Logger.getLogger(UploadFileTask.class.getName());
    
    private ResourceRepositoryService service;
    private WebDAVResource res;
    private File file;
    
    public UploadFileTask(ResourceRepositoryService service, WebDAVResource res, File file) {
        this.service = service;
        this.res = res;
        this.file = file;
    }

    @Override
    protected File call() throws Exception {
        
        // Initial status
        updateTitle(MessageFormat.format("Uploading ''{0}''", file));
        
        String path;
        try {
            path = service.upload(res, file);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error uploading file", e);
            updateProgress(1, 1);
            return null;
        }
        
        // Final status
        updateMessage(path);
        updateProgress(1, 1);
        return file;
    }
}
