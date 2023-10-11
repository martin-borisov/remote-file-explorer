package mb.client.rfe.model;

import java.net.URI;
import java.util.Date;

public class WebDAVResource {

    private String absolutePath, name, type;
    private long size;
    private Date created, modified;
    private boolean directory;
    private boolean empty;
    private URI baseURI;

    public WebDAVResource(String name, String absolutePath) {
        this.name = name;
        this.absolutePath = absolutePath;
    }
    
    public WebDAVResource(String absolutePath, String name, String type, long size, Date created, Date modified,
            boolean directory, boolean empty, URI baseURI) {
        this.absolutePath = absolutePath;
        this.name = name;
        this.type = type;
        this.size = size;
        this.created = created;
        this.modified = modified;
        this.directory = directory;
        this.empty = empty;
        this.baseURI = baseURI;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDirectory() {
        return directory;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public String toString() {
        return name;
    }
    
    public URI getBaseURI() {
        return baseURI;
    }

    public void setBaseURI(URI baseURI) {
        this.baseURI = baseURI;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((absolutePath == null) ? 0 : absolutePath.hashCode());
        result = prime * result + ((baseURI == null) ? 0 : baseURI.hashCode());
        result = prime * result + ((created == null) ? 0 : created.hashCode());
        result = prime * result + (directory ? 1231 : 1237);
        result = prime * result + (empty ? 1231 : 1237);
        result = prime * result + ((modified == null) ? 0 : modified.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (int) (size ^ (size >>> 32));
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WebDAVResource other = (WebDAVResource) obj;
        if (absolutePath == null) {
            if (other.absolutePath != null)
                return false;
        } else if (!absolutePath.equals(other.absolutePath))
            return false;
        if (baseURI == null) {
            if (other.baseURI != null)
                return false;
        } else if (!baseURI.equals(other.baseURI))
            return false;
        if (created == null) {
            if (other.created != null)
                return false;
        } else if (!created.equals(other.created))
            return false;
        if (directory != other.directory)
            return false;
        if (empty != other.empty)
            return false;
        if (modified == null) {
            if (other.modified != null)
                return false;
        } else if (!modified.equals(other.modified))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (size != other.size)
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }
}
