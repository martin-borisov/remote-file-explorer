package mb.client.rfe;

/**
 * This separate launcher is needed because of how OpenJFX loads modules and native libraries
 */
public class Launcher {
    public static void main(String[] args) {
        WebDAVClient.main(args);
    }
}
