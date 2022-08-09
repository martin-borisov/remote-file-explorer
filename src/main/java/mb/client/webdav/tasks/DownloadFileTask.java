package mb.client.webdav.tasks;

import static java.text.MessageFormat.format;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.concurrent.Task;
import javafx.util.Callback;
import mb.client.webdav.model.WebDAVResource;
import mb.client.webdav.service.WebDAVService;
import mb.client.webdav.service.WebDAVServiceException;

public class DownloadFileTask extends Task<File> {
    
    private static final Logger LOG = Logger.getLogger(DownloadFileTask.class.getName());
    
    private WebDAVService service;
    private WebDAVResource res;
    
    public DownloadFileTask(WebDAVService service, WebDAVResource res) {
        this.service = service;
        this.res = res;
    }

    @Override
    // TODO Revisit all messages and progress updates (don't do them so often)
    protected File call() throws Exception {
        
        // Initial status
        updateTitle(MessageFormat.format("Downloading ''{0}''", res));
        
        File file = null;
        try {
            file = service.download(res, new Callback<Integer, Void>() {
                private int totalByteCount;
                public Void call(Integer bytesRead) {
                    totalByteCount += bytesRead;
                    updateProgress(totalByteCount, res.getSize());
                    updateMessage(MessageFormat.format("{0,number,#} bytes downloaded", totalByteCount));
                    return null;
                }
            });
            
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Cannot open file natively", e);
                complete(e.getMessage());
                return null;
            }
        } catch (WebDAVServiceException e) {
            LOG.log(Level.SEVERE, "Error downloading file", e);
            complete(e.getMessage());
            return null;
        }
        
        // Final status
        complete(format("''{0}'' downloaded successfully", res));
        return file;
    }
    
    private void complete(String msg) {
        updateMessage(msg);
        updateProgress(1, 1);
    }

}
