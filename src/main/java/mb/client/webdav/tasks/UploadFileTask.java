package mb.client.webdav.tasks;

import java.io.File;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.concurrent.Task;
import mb.client.webdav.model.WebDAVResource;
import mb.client.webdav.service.WebDAVService;

public class UploadFileTask extends Task<File> {
    
    private static final Logger LOG = Logger.getLogger(UploadFileTask.class.getName());
    
    private WebDAVService service;
    private WebDAVResource res;
    private File file;
    
    public UploadFileTask(WebDAVService service, WebDAVResource res, File file) {
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
