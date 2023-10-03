package mb.client.webdav.model;

import java.net.URI;

public class ResourceHost {
    
    private URI baseURI;
    private String root, user, password, lastAccessedPath;
    private boolean local;
    
    public ResourceHost() {
    }
    
    public ResourceHost(URI baseURI, String root, String user, String password) {
        this.baseURI = baseURI;
        this.root = root;
        this.user = user;
        this.password = password;
    }

    public URI getBaseURI() {
        return baseURI;
    }
    
    public void setBaseURI(URI baseURI) {
        this.baseURI = baseURI;
    }
    
    public String getBaseUriString() {
        return baseURI != null ? baseURI.toString() : null;
    }
    
    public void setBaseUriString(String baseURI) {
        if(baseURI != null) {
            this.baseURI = URI.create(baseURI);
        }
    }
    
    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String getUser() {
        return user;
    }
    
    public void setUser(String user) {
        this.user = user;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }
    
    public String getLastAccessedPath() {
        return lastAccessedPath;
    }

    public void setLastAccessedPath(String lastAccessedPath) {
        this.lastAccessedPath = lastAccessedPath;
    }

    @Override
    public String toString() {
        return baseURI.toString();
    }
}
