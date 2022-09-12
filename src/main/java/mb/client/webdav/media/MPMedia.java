package mb.client.webdav.media;

public class MPMedia {
    
    public enum Location {
        LOCAL, REMOTE
    }
    
    public enum Type {
        AUDIO, VIDEO
    }
    
    private String name, source, user, password;
    private Type type;
    
    public MPMedia(String name, String source, Type type) {
        this.name = name;
        this.source = source;
        this.type = type;
    }

    public MPMedia(String name, String source, String user, String password, Type type) {
        this(name, source, type);
        this.user = user;
        this.password = password;
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
        return source != null && source.startsWith("file");
    }

    @Override
    public String toString() {
        return "MPMedia [name=" + name + ", source=" + source + ", type=" + type + "]";
    }
}
