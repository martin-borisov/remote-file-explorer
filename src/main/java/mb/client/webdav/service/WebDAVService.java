package mb.client.webdav.service;

import static java.text.MessageFormat.format;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;

import javafx.util.Callback;
import mb.client.webdav.model.WebDAVHost;
import mb.client.webdav.model.WebDAVResource;

public class WebDAVService {
    
    private WebDAVHost host;
    private Sardine sardine;

    public WebDAVService(WebDAVHost host) {
        this.host = host;
    }
    
    public void connect() {
        sardine = SardineFactory.begin(host.getUser(), host.getPassword());
    }
    
    public List<WebDAVResource> list(String path) throws WebDAVServiceException {
        try {
            List<DavResource> list = sardine.list(buildURI(path));
            List<WebDAVResource> resources = new ArrayList<WebDAVResource>(list.size());
            for (DavResource sardineResource : list) {
                
                // Filter out parent, as it's returned with the list of children
                if(!path.equals(sardineResource.getHref().toString())) {
                    resources.add(WebDAVUtil.webDAVResourceFromSardineResource(sardineResource, host));
                }
            }
            return resources;
        } catch (IOException e) {
            throw new WebDAVServiceException(e);
        }
    }
    
    public List<WebDAVResource> listDirs(String path) throws WebDAVServiceException {
        return list(path).stream().filter(r -> r.isDirectory()).collect(Collectors.toList());
    }
    
    public List<WebDAVResource> listFiles(String path) throws WebDAVServiceException {
        return list(path).stream().filter(r -> !r.isDirectory()).collect(Collectors.toList());
    }
    
    public InputStream getContent(WebDAVResource res) throws WebDAVServiceException {
        
        InputStream is;
        try {
            is = sardine.get(buildURI(res.getAbsolutePath()));
        } catch (IOException e) {
            throw new WebDAVServiceException("Fetching file from server failed", e);
        }
        return is;
    }
    
    public File download(WebDAVResource res, Callback<Integer, Void> callback) throws WebDAVServiceException {
        
        InputStream is = getContent(res);
        
        File file;
        FileOutputStream fos = null;
        try {

            final int buffSize = 8 * 1024;
            
            file = new File("downloads/" + res.getName());
            
            // Lazily create "downloads" directory
            FileUtils.createParentDirectories(file);
            
            fos = new FileOutputStream(file);
            
            byte[] buffer = new byte[buffSize];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                callback.call(bytesRead);
            }
            
        } catch (IOException e) {
            throw new WebDAVServiceException("Copying file failed", e);
        } finally {
            IOUtils.closeQuietly(is, fos);
        }
        
        return file;
    }
    
    public void upload(WebDAVResource parent, File localFile) throws WebDAVServiceException {
        
        try {
            
            // Prepare upload URI
            String parentPath = parent.getAbsolutePath();
            if(!parentPath.endsWith("/")) {
                parentPath = parentPath + "/";
            }
            
            String fileName = URLEncoder.encode(localFile.getName(), Charset.defaultCharset());
            
            // Upload
            sardine.put(buildURI(parentPath + fileName), localFile, 
                    Files.probeContentType(localFile.toPath()));
        } catch (IOException e) {
            throw new WebDAVServiceException("File upload failed", e);
        }
    }
    
    public void search(WebDAVResource parent, String query) throws WebDAVServiceException {
        try {
            sardine.search(buildURI(parent.getAbsolutePath()), "EN", query);
        } catch (IOException e) {
            throw new WebDAVServiceException("Search error", e);
        }
    }
    
    public void delete(WebDAVResource res) throws WebDAVServiceException {
        try {
            sardine.delete(buildURI(res.getAbsolutePath()));
        } catch (IOException e) {
            throw new WebDAVServiceException(format("Failed to delete resource ''{0}''", res.getAbsolutePath()), e);
        }
    }
    
    public void disconnect() throws WebDAVServiceException {
        if(sardine != null) {
            try {
                sardine.shutdown();
            } catch (IOException e) {
                throw new WebDAVServiceException(e);
            }
        }
    }
    
    public WebDAVHost getHost() {
        return host;
    }

    private String buildURI(String path) {
        return host.getBaseURI() + path;
    }

}
