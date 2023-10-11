package mb.client.rfe.test.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.github.sardine.DavAce;
import com.github.sardine.DavAcl;
import com.github.sardine.DavPrincipal;
import com.github.sardine.DavQuota;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.report.SardineReport;

public class MockSardineImpl implements Sardine {
    
    private List<DavResource> davResListToReturn;
    private InputStream isToReturn;
    
    public void setDavResListToReturn(List<DavResource> davResListToReturn) {
        this.davResListToReturn = davResListToReturn;
    }
    
    public void setIsToReturn(InputStream isToReturn) {
        this.isToReturn = isToReturn;
    }

    @Override
    public void setCredentials(String username, String password) {
    }

    @Override
    public void setCredentials(String username, String password, String domain, String workstation) {
    }

    @Override
    public List<DavResource> getResources(String url) throws IOException {
        return null;
    }

    @Override
    public List<DavResource> list(String url) throws IOException {
        return davResListToReturn;
    }

    @Override
    public List<DavResource> list(String url, int depth) throws IOException {
        return list(url);
    }

    @Override
    public List<DavResource> list(String url, int depth, Set<QName> props) throws IOException {
        return list(url);
    }

    @Override
    public List<DavResource> list(String url, int depth, boolean allProp) throws IOException {
        return list(url);
    }

    @Override
    public List<DavResource> propfind(String url, int depth, Set<QName> props) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T report(String url, int depth, SardineReport<T> report) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DavResource> search(String url, String language, String query) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setCustomProps(String url, Map<String, String> addProps, List<String> removeProps) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public List<DavResource> patch(String url, Map<QName, String> addProps) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DavResource> patch(String url, Map<QName, String> addProps, List<QName> removeProps)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DavResource> patch(String url, List<Element> addProps, List<QName> removeProps) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream get(String url) throws IOException {
        return isToReturn;
    }

    @Override
    public InputStream get(String url, Map<String, String> headers) throws IOException {
        return get(url);
    }

    @Override
    public void put(String url, byte[] data) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void put(String url, InputStream dataStream) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void put(String url, byte[] data, String contentType) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void put(String url, InputStream dataStream, String contentType) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void put(String url, InputStream dataStream, String contentType, boolean expectContinue) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void put(String url, InputStream dataStream, String contentType, boolean expectContinue, long contentLength)
            throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void put(String url, InputStream dataStream, Map<String, String> headers) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void put(String url, File localFile, String contentType) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void put(String url, File localFile, String contentType, boolean expectContinue) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(String url) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void createDirectory(String url) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void move(String sourceUrl, String destinationUrl) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void move(String sourceUrl, String destinationUrl, boolean overwrite) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void copy(String sourceUrl, String destinationUrl) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void copy(String sourceUrl, String destinationUrl, boolean overwrite) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean exists(String url) throws IOException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String lock(String url) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String refreshLock(String url, String token, String file) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void unlock(String url, String token) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public DavAcl getAcl(String url) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DavQuota getQuota(String url) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setAcl(String url, List<DavAce> aces) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public List<DavPrincipal> getPrincipals(String url) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getPrincipalCollectionSet(String url) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void enableCompression() {
        // TODO Auto-generated method stub

    }

    @Override
    public void disableCompression() {
        // TODO Auto-generated method stub

    }

    @Override
    public void ignoreCookies() {
        // TODO Auto-generated method stub

    }

    @Override
    public void enablePreemptiveAuthentication(String hostname) {
        // TODO Auto-generated method stub

    }

    @Override
    public void enablePreemptiveAuthentication(URL url) {
        // TODO Auto-generated method stub

    }

    @Override
    public void enablePreemptiveAuthentication(String hostname, int httpPort, int httpsPort) {
        // TODO Auto-generated method stub

    }

    @Override
    public void disablePreemptiveAuthentication() {
        // TODO Auto-generated method stub

    }

    @Override
    public void shutdown() throws IOException {
        // TODO Auto-generated method stub

    }

}
