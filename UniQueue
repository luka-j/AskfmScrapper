package rs.luka;

import java.io.*;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Unique queue for Strings. Also does File I/O when necessary
 */
public class UniQueue {
    /**
     * Okay, I lied, it's not {@link java.util.Queue}, it's {@link java.util.LinkedList}
     */
    private List<String> queue = new LinkedList<>();
    /**
     * Iterator would have been faster, but I find this easier to work with. Also, saving progress.
     */
    private int i;
    /**
     * Save file for queue
     */
    private final File save;
    /**
     * Save file for index
     */
    private final File index;

    /**
     * Creates queue for given username and puts it on the first place. Call {@link #next()} from now on.
     */
    public UniQueue(String username) {
        save = new File(Main.dir, "queue - "+username);
        index = new File(Main.dir, "index - "+username);
        if(!save.exists()) { //if it doesn't exist, initializes new queue
            try {
                if(!save.createNewFile()) throw new AssertionError("file not created"); //VERY slim chances of happening
                add(username);
                addSeparator();
                i=-1; //should always be behind by one, so this is legit (not an error code)
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else { //if it does, loads it from file and continuing from where it left off
            try (BufferedReader saveReader = new BufferedReader(new FileReader(save));
                    BufferedReader indexReader = new BufferedReader(new FileReader(index))) {
                saveReader.lines().filter((String s) -> !s.equals("---")).forEach((String s) -> add(s, false));
                i = Integer.parseInt(indexReader.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void add(String username) {
        add(username, true);
    }

    /**
     * @param username != null, whitespace is ignored
     * @param appendToFile should it be written to file (false when loading existing queue)
     */
    public void add(String username, boolean appendToFile) {
        Objects.requireNonNull(username);
        if(username.isEmpty())
            System.out.println("empty username");
        if(!queue.contains(username.trim())) {
            queue.add(username.trim());
            if(appendToFile) appendLastToFile();
        }
    }

    /**
     * Just to make it more readable. Also to find which account led to which afterwards. Ignored by queue.
     */
    public void addSeparator() {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(save, true), Charset.forName("UTF-8")))) {
            bw.append("---\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a link if there's a chance it contains askfm username, otherwise ignores it. Could lead to invalid usernames/links
     * @param link link
     */
    public void addLink(String link) {
        if(link.startsWith("http://ask.fm/")) {
            add(link.substring(14).split("/")[0]);
        }
    }

    /**
     * Fuck you Schlemiel
     */
    public String next() {
        i++;
        return queue.get(i);
    }

    public String peek() {
        return queue.get(i+1);
    }

    public boolean hasNext() {
        return i<queue.size()-1;
    }


    public void appendLastToFile() {
        try (BufferedWriter saveWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(save, true), Charset.forName("UTF-8")));
             BufferedWriter indexWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(index, false), Charset.forName("UTF-8")))) {
            saveWriter.append(queue.get(queue.size() - 1) + "\n");
            indexWriter.write(String.valueOf(i-1)); //need to redo last, it probably isn't finished (and if it it, it'll just get skipped when launched again)
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isBuilt() {
        return queue.size() > 1;
    }
}
