package mb.client.webdav.model;

import java.util.Date;

import org.kordamp.ikonli.javafx.FontIcon;

import mb.client.webdav.components.Icons;

public class ResourceTableItem {
    
    private WebDAVResource davRes;

    public ResourceTableItem(WebDAVResource davRes) {
        this.davRes = davRes;
    }
    
    public boolean isImage() {
        return getType().startsWith("image");
    }
    
    public boolean isPdf() {
        return "application/pdf".equals(getType());
    }
    
    public FontIcon getIcon() {
        FontIcon icon = Icons.file();
        if (isDirectory()) {
            icon = Icons.folder();
        } else if (getType().startsWith("audio")) {
            icon = Icons.fileAudio();
        } else if (getType().startsWith("video")) {
            icon = Icons.fileVideo();
        } else if (getType().startsWith("application/vnd.ms-excel")) {
            icon = Icons.fileSpreadsheet();
        } else if (getType().contains("presentation")) {
            icon = Icons.filePresentation();
        } else if (getType().contains("word")) {
            icon = Icons.fileWord();
        } else if (isImage()) {
            icon = Icons.fileImage();
        } else {
            switch (getType()) {
            case "application/pdf":
                icon = Icons.filePdf();
                break;
            case "application/zip":
                icon = Icons.fileArchive();
                break;
            }
        }

        return icon;
    }
    
    public WebDAVResource getDavRes() {
        return davRes;
    }

    public void setDavRes(WebDAVResource davRes) {
        this.davRes = davRes;
    }

    public String getAbsolutePath() {
        return davRes.getAbsolutePath();
    }

    public void setAbsolutePath(String absolutePath) {
        davRes.setAbsolutePath(absolutePath);
    }

    public String getName() {
        return davRes.getName();
    }

    public void setName(String name) {
        davRes.setName(name);
    }

    public boolean isDirectory() {
        return davRes.isDirectory();
    }

    public void setDirectory(boolean directory) {
        davRes.setDirectory(directory);
    }

    public boolean isEmpty() {
        return davRes.isEmpty();
    }

    public void setEmpty(boolean empty) {
        davRes.setEmpty(empty);
    }

    public String getType() {
        return davRes.getType();
    }

    public void setType(String type) {
        davRes.setType(type);
    }

    public long getSize() {
        return davRes.getSize();
    }

    public void setSize(long size) {
        davRes.setSize(size);
    }

    public Date getCreated() {
        return davRes.getCreated();
    }

    public void setCreated(Date created) {
        davRes.setCreated(created);
    }

    public Date getModified() {
        return davRes.getModified();
    }

    public void setModified(Date modified) {
        davRes.setModified(modified);
    }

    @Override
    public String toString() {
        return getAbsolutePath();
    }
    
    
    // TODO Use commons-lang
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((davRes == null) ? 0 : davRes.hashCode());
        return result;
    }

    // TODO Use commons-lang
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ResourceTableItem other = (ResourceTableItem) obj;
        if (davRes == null) {
            if (other.davRes != null)
                return false;
        } else if (!davRes.equals(other.davRes))
            return false;
        return true;
    }

    public static ResourceTableItem fromDavRes(WebDAVResource res) {
        return new ResourceTableItem(res);
    }
}
