package rs.luka;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {
    /**
     * Directory for all files created by this program, modify if needed. Windows is weird though, you better set it by hand.
     */
    static final File dir = new File(System.getProperty("user.home"), "askfm");
    /**
     * In case of multiple roots (or manual setting of index), this keeps already downloaded usernames.
     * ArrayList might be a tad faster for binarySearch/sort, but in the end, it'll be dumped to array anyway
     */
    static final List<String> existingFilenames = new ArrayList<>();
    /**
     * Queue for usernames
     */
    static UniQueue queue;

    /**
     * Will run forever, don't forget to stop it when you get what you wanted
     * @param args will be ignored
     */
    public static void main(String[] args) {
        if(!dir.isDirectory())
            dir.mkdir();
        setExisting();
        String username = JOptionPane.showInputDialog("Enter username:");
        queue = new UniQueue(username);
        System.out.println(queue.peek());
        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while(queue.hasNext()) {
            queue.addSeparator();
            username = queue.next();
            if(Collections.binarySearch(existingFilenames, username) < 0 || !queue.isBuilt()) {
                System.out.println("Starting " + username);
                new Retriever(username, dir).go();
            } else {
                System.out.println(username + " already downloaded");
            }
        }
    }

    /**
     * Setting existing files
     * @see this#existingFilenames
     */
    public static void setExisting() {
        try {
            Files.walk(dir.toPath(), FileVisitOption.FOLLOW_LINKS).forEach((Path p) -> {
                String[] tokens = p.getFileName().toString().split("\\Q - \\E");
                existingFilenames.add(tokens[tokens.length - 1].split("\\.")[0]);
            });
            Collections.sort(existingFilenames);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
