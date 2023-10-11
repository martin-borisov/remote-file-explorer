package mb.client.rfe.service;

import static java.text.MessageFormat.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.sardine.DavResource;

import javafx.util.Callback;
import mb.client.rfe.model.ResourceHost;
import mb.client.rfe.model.WebDAVResource;

public class LocalFileSystemService implements ResourceRepositoryService {
    private static final Logger LOG = Logger.getLogger(LocalFileSystemService.class.getName());

    @Override
    public void connect() {
    }

    @Override
    public List<WebDAVResource> list(String path) throws WebDAVServiceException {
        return list(path, 1);
    }

    @Override
    public List<WebDAVResource> list(String path, int depth) throws WebDAVServiceException {
        try {
            if(depth > 0) { 
                return Files.list(Paths.get(path)).filter(p -> Files.exists(p)).map(p -> {
                    WebDAVResource res = null;
                    try {
                        res = resourceFromPath(p);
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Reading file attributes", e);
                    }
                    return res;
                }).collect(Collectors.toList());
            } else {
                return Arrays.asList(resourceFromPath(Paths.get(path)));
            }
        } catch (Exception e) {
            throw new WebDAVServiceException(e);
        }
    }
    
    private WebDAVResource resourceFromPath(Path p) throws IOException, URISyntaxException {
        BasicFileAttributes attribs = Files.readAttributes(p, BasicFileAttributes.class);
        String contentType = Files.probeContentType(p);
        return new WebDAVResource(p.toString(), p.getFileName().toString(),
                contentType != null ? contentType : "application/octet-stream", Files.size(p),
                new Date(attribs.creationTime().toMillis()),
                new Date(attribs.lastModifiedTime().toMillis()), 
                attribs.isDirectory(), true, new URI(WebDAVUtil.encodeUrlPath(p.getParent().toString())));
    }

    @Override
    public List<WebDAVResource> listDirs(String path) throws WebDAVServiceException {
        return list(path).stream().filter(r -> r.isDirectory()).collect(Collectors.toList());
    }

    @Override
    public List<WebDAVResource> listFiles(String path) throws WebDAVServiceException {
        return list(path).stream().filter(r -> !r.isDirectory()).collect(Collectors.toList());
    }

    @Override
    public InputStream getContent(WebDAVResource res) throws WebDAVServiceException {
        try {
            return new FileInputStream(new File(res.getAbsolutePath()));
        } catch (FileNotFoundException e) {
            throw new WebDAVServiceException(e);
        }
    }

    @Override
    public File download(WebDAVResource res, Callback<Integer, Void> callback) throws WebDAVServiceException {
        return new File(res.getAbsolutePath());
    }

    @Override
    public String upload(WebDAVResource parent, File localFile) throws WebDAVServiceException {
        Path srcPath = Paths.get(localFile.getAbsolutePath());
        Path destPath = Paths.get(parent.getAbsolutePath() + "/" + localFile.getName());
        try {
            Files.copy(srcPath, destPath);
        } catch (IOException e) {
            throw new WebDAVServiceException(format("Copying ''{0}'' to ''{1}'' failed", srcPath, destPath), e);
        }
        return destPath.toString();
    }

    @Override
    public List<DavResource> search(WebDAVResource parent, String query) throws WebDAVServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(WebDAVResource res) throws WebDAVServiceException {
        try {
            Files.delete(Paths.get(res.getAbsolutePath()));
        } catch (IOException e) {
            throw new WebDAVServiceException(e);
        }
    }

    @Override
    public void move(WebDAVResource src, WebDAVResource dest) throws WebDAVServiceException {
        Path srcPath = Paths.get(src.getAbsolutePath());
        Path destPath = Paths.get(dest.getAbsolutePath() + "/" + src.getName());
        try {
            Files.move(srcPath, destPath);
        } catch (IOException e) {
            throw new WebDAVServiceException(format("Moving ''{0}'' to ''{1}'' failed", srcPath, destPath), e);
        }
    }

    @Override
    public String createDirectory(WebDAVResource parent, String dirName) throws WebDAVServiceException {
        try {
            return Files.createDirectory(Paths.get(parent.getAbsolutePath(), dirName)).toString();
        } catch (IOException e) {
            throw new WebDAVServiceException(e);
        }
    }

    @Override
    public void disconnect() throws WebDAVServiceException {
    }

    @Override
    public ResourceHost getHost() {
        return null;
    }

}
