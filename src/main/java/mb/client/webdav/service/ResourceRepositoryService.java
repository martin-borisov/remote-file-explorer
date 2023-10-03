package mb.client.webdav.service;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import com.github.sardine.DavResource;

import javafx.util.Callback;
import mb.client.webdav.model.ResourceHost;
import mb.client.webdav.model.WebDAVResource;

public interface ResourceRepositoryService {

    void connect();

    List<WebDAVResource> list(String path) throws WebDAVServiceException;

    List<WebDAVResource> list(String path, int depth) throws WebDAVServiceException;

    List<WebDAVResource> listDirs(String path) throws WebDAVServiceException;

    List<WebDAVResource> listFiles(String path) throws WebDAVServiceException;

    InputStream getContent(WebDAVResource res) throws WebDAVServiceException;

    File download(WebDAVResource res, Callback<Integer, Void> callback) throws WebDAVServiceException;

    String upload(WebDAVResource parent, File localFile) throws WebDAVServiceException;

    List<DavResource> search(WebDAVResource parent, String query) throws WebDAVServiceException;

    void delete(WebDAVResource res) throws WebDAVServiceException;

    void move(WebDAVResource src, WebDAVResource dest) throws WebDAVServiceException;

    String createDirectory(WebDAVResource parent, String dirName) throws WebDAVServiceException;

    void disconnect() throws WebDAVServiceException;

    ResourceHost getHost();

}