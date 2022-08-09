package mb.client.webdav.media;

public class MPMedia {
    
    public enum Location {
        LOCAL, REMOTE
    }
    
    public enum Type {
        AUDIO, VIDEO
    }
    
    private String name;
    private String source;
    private Type type;
    
    public MPMedia(String name, String source, Type type) {
        this.name = name;
        this.source = source;
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
    
    public boolean isLocal() {
        return source != null && source.startsWith("file");
    }

    @Override
    public String toString() {
        return "MPMedia [name=" + name + ", source=" + source + ", type=" + type + "]";
    }
}
