package rs.luka;

import com.sun.istack.internal.Nullable;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import javax.swing.JOptionPane;

public class Retriever {
    /**
     * When to print progress, should be >=10
     */
    static final int PRINT_EVERY = 10;
    /**
     * Determines the pause between in {@link this#resolveLikes()}
     */
    static final int LIKES_PAUSE = 200;
    /**
     * Strings used for matching
     */
    /**
     * Regular fields (appear in every question)
     */
    private static final String questionBeginLtr = "span class=\\\"text-bold\\\"><span dir=\\\"ltr\\\">";
    private static final String questionBeginRtl = "span class=\\\"text-bold\\\"><span dir=\\\"rtl\\\">";
    private static final String questionEnd = "</sp"; //za end: sto krace, to brze
    private static final String answerBeginLtr = "div class=\\\"answer\\\" dir=\\\"ltr\\\">\\n      ";
    private static final String answerBeginRtl = "div class=\\\"answer\\\" dir=\\\"rtl\\\">\\n      ";
    private static final String answerEnd = "\\n    </div>";
    private static final String dateBegin = "class=\\\"link-time hintable inverse\\\" data-rlt-aid=\\\"answer_time\\\" hint=\\\"";
    private static final String dateEnd = "GMT\\\">";
    /**
     * Optional fields (may not appear in every question)
     */
    private static final String authorBegin = "<span class=\\\"author nowrap\\\">&nbsp;&nbsp;<a href=\\\"/";
    private static final String authorEnd = "\\\"";
    private static final String likesBegin = "return false\\\" href=\\\"/likes";
    private static final String likesEnd = "\\\">";
    /**
     * Strings for images in links, appear inside question and answer text
     */
    private static final String imgBegin = "<a href=";
    private static final String imgSrcBegin = "src=\\\"";
    private static final String imgEnd = "\\\" />";
    private static final String linkBegin = "<a class=\\\"link";
    private static final String linkHrefBegin = "\\\">";
    private static final String linkHttpsHrefBegin = "\\\">https:";
    private static final String linkEnd = "</a>";

    /**
     * User for which the {@link #go()} method retrieves data, the part after ask.fm/ in hyperlink
     */
    private final String username;
    /**
     * Directory in which the file is stored
     */
    private final File dir;
    /**
     * User's data
     */
    private final ArrayList<Question> questions = new ArrayList<>();

    /**
     * Basic constructor
     * @param username user
     * @param dir directory
     * @see this#username
     * @see this#dir
     */
    Retriever(String username, File dir) {
        this.username = username;
        this.dir = dir;
    }

    /**
     * Method that does all the work
     */
    public void go() {
        if (username == null || username.isEmpty()) return; //if you changed your mind
        String page;
        Integer i = 0;
        do {
            if(i%PRINT_EVERY==0) //trying not to flood the System.out
                System.out.println(i.toString() + "...");
            page = loadPage("http://ask.fm/" + username + "/more?time=&page=" + i.toString());
            if(page == null) //if there has been an error, ignore it (happens if some link/username is corrupted)
                return;
            if(page.startsWith("<!DOCTYPE")) { //if I get regular page instead of API response, something is wrong
                System.out.println(username + " failed; could be deactivated (or I got blocked, gj ask)");
                return;
            }
            formatInOnePass(page); //where magic happens
            i++;
        } while (!page.equals("$(\"#more-container\").hide();")); //empty response (no more questions)
        //resolveLikes(); //leaving this disabled, too slow and risky
        writeReversed(new File(dir, validateFilename(findRealName(username) + " - " + username) + ".askfm"), questions);
        questions.clear(); //keeping memory footprint on minimum, probably unnecessary
    }

    /**
     * Turns /likes links into usernames. Makes request to a regular request, which can also trigger the alarm, so avoiding
     * it when isn't really needed. To make it work properly, it needs to wait for some time between requests and you should
     * be generating some regular traffic from the same IP.
     * I might be wrong about the masking technique though, I haven't tested it thoroughly.
     */
    public void resolveLikes() {
        System.out.println("Resolving likes for " + username);
        StringBuilder username = new StringBuilder(16);
        questions.stream().filter((Question q) -> q.likes!=null).forEach((Question q) -> {
            try {
                Thread.sleep(LIKES_PAUSE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String page = loadPage("http://ask.fm/likes" + q.likes);
            q.likes = null;
            for (int j = 0; j < page.length(); j++) {
                if (page.charAt(j) == '@' && page.charAt(j + 1) != '<') {
                    username.delete(0, username.length());
                    j++;
                    while (page.charAt(j) != ' ') {
                        username.append(page.charAt(j));
                        j++;
                    }
                    j++;
                    Main.queue.add(username.toString().trim());
                    if (q.likes == null) {
                        q.likes = username.toString();
                    } else {
                        q.likes = q.likes + ", " + username.toString();
                    }
                }
            }
        });
    }

    /**
     * Finds real name (the one user supplied) for the given username. Makes request to the public page, so should be
     * used with care. In case of blocking, returns "No robots allowed"
     * @param username well, username
     * @return name user supplied
     */
    public String findRealName(String username) {
        StringBuilder name = new StringBuilder(16);
        String page = loadPage("http://ask.fm/" + username);
        for(int i=0; i<page.length(); i++) {
            if(isSubstring(page, i, "<title>")) {
                i+=7;
                while(!isSubstring(page, i, " | ")) {
                    name.append(page.charAt(i));
                    i++;
                }
                return name.toString();
            }
        }
        return null;
    }

    /**
     * Cleans up filename for Windows, else just returns given string. Doesn't match reserved name, but only special
     * characters (also doesn't handle names like . and .. for Linuxes)
     * @param name filename
     * @return filename without special chars
     */
    public String validateFilename(String name) {
        if(System.getProperty("os.name").toLowerCase().contains("win"))
            return name.replaceAll("\\Q:\\E|\\Q*\\E|\\Q/\\E|\\Q\\\\E|\\Q\"\\E|\\Q?\\E|\\Q<\\E|\\Q>\\E|\\Q|\\E", "");
        else return name;
    }

    /**
     * Loads the page and return the result in String
     *
     * @param address URL of the page
     * @return page content
     */
    public String loadPage(String address) {
        String line;
        StringBuilder page = new StringBuilder();
        try(BufferedReader br = new BufferedReader(new InputStreamReader((new URL(address).openStream())))) {
            while ((line = br.readLine()) != null) {
                page.append(line);
            }
        } catch (java.net.UnknownHostException ex) {
            System.out.println("Internet failure");

            System.exit(1);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return page.toString();
    }

    /**
     * Writes given list to the given file in reverse orded
     * @param list do I really need to document every param?
     * @return canonical path to file
     */
    public String writeReversed(File file, List<Question> list) {
        if(list.size() == 0)
            return null;
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")))) {
            file.delete(); //brise fajl sa istim imenom, ako vec postoji
            file.createNewFile(); //kreira novi fajl sa datim imenom
            ListIterator<Question> it = list.listIterator(list.size()-1); //black magic
            while(it.hasPrevious()) {
                w.append(it.previous().toString()).append("\n");
            }
            return file.getCanonicalPath();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "IO greska", "IOException", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Pulls out data from the page. Name comes from the need to distinguish this and old method (which did the same in
     * two passes), which is now gone. This one isn't so well tested, so you might encounter some quirks. Sometimes
     * it fails to load the last question on page (or is it the last for user, if page isn't filled?).
     * Order: question - (author) - answer - date - (likes)
     * Okay, it's basically one-and-a-half passes because {@link this#formatText(String)} isn't integrated
     * @param page content of the page with questions, obtained from the /more
     */
    public void formatInOnePass(String page) {
        int index=0;
        int questionBegin, questionEnd, answerBegin, answerEnd, authorBegin, authorEnd, dateBegin, dateEnd, likesBegin, likesEnd;
        String question, answer, author, date, likes;
        for(int i=0; i<25; i++) {
            author = likes = null;
            questionBegin = iterateUntilMatchesEither(page, index, questionBeginLtr, questionBeginRtl);
            if(questionBegin == Integer.MAX_VALUE) return;
            questionBegin = Math.abs(questionBegin) + questionBeginLtr.length();
            questionEnd = iterateUntilMatches(page, questionBegin, Retriever.questionEnd);
            question = page.substring(questionBegin, questionEnd);

            authorBegin = iterateUntilMatchesEither(page, questionEnd + Retriever.questionEnd.length(), Retriever.authorBegin, Retriever.answerBeginLtr, Retriever.answerBeginRtl);
            if(authorBegin > offset) {
                answerBegin = authorBegin - offset + Retriever.answerBeginLtr.length();
                answerEnd = iterateUntilMatches(page, answerBegin, Retriever.answerEnd);
                answer = page.substring(answerBegin, answerEnd);
            } else if(authorBegin < 0) {
                answerBegin = Math.abs(authorBegin) + Retriever.answerBeginLtr.length();
                answerEnd = iterateUntilMatches(page, answerBegin, Retriever.answerEnd);
                answer = page.substring(answerBegin, answerEnd);
            } else {
                authorBegin += Retriever.authorBegin.length();
                authorEnd = iterateUntilMatches(page, authorBegin, Retriever.authorEnd);
                author = page.substring(authorBegin, authorEnd);
                answerBegin = iterateUntilMatchesEither(page, authorEnd + Retriever.authorEnd.length(), Retriever.answerBeginLtr, Retriever.answerBeginRtl);
                answerBegin = Math.abs(answerBegin) + Retriever.answerBeginLtr.length();
                answerEnd =iterateUntilMatches(page, answerBegin, Retriever.answerEnd);
                answer = page.substring(answerBegin, answerEnd);
            }

            dateBegin = iterateUntilMatches(page, answerEnd + Retriever.answerEnd.length(), Retriever.dateBegin) + Retriever.dateBegin.length();
            dateEnd = iterateUntilMatches(page, dateBegin, Retriever.dateEnd);
            date = page.substring(dateBegin, dateEnd);

            likesBegin = iterateUntilMatchesEither(page, dateEnd, Retriever.questionBeginLtr, Retriever.likesBegin, Retriever.questionBeginRtl);
            if(likesBegin < 0) {
                likesBegin = likesBegin * -1 + Retriever.likesBegin.length();
                likesEnd = iterateUntilMatches(page, likesBegin, Retriever.likesEnd);
                likes = page.substring(likesBegin, likesEnd);
                index = likesEnd + Retriever.likesEnd.length();
            }
            questions.add(new Question(formatText(question), formatText(answer), formatText(date), author, likes));
            if(likesBegin == Integer.MAX_VALUE) {
                return; //should be done here
            } else if(likesBegin > offset) {
                index = likesBegin - offset;
            } else if(likesBegin > 0) {
                index = likesBegin;
            }
        }
    }

    /**
     * offset when I have to distinguish three values
     */
    private static final int offset = 1_000_000_000;

    /**
     * Straightforward, but used heavily. Also trying not to mess with the indices in the bigger method, last time it
     * didn't end well (it's practically impossible to improve and debug after some time passes)
     * @params obvious
     */
    public int iterateUntilMatches(String page, int start, String s) {
        for(int i=start; i<page.length(); i++) {
            if(isSubstring(page, i, s)) return i;
        }
        return Integer.MAX_VALUE;
    }

    /**
     * {@inheritDoc this#iterateUntilMatches}
     * It's ugly, but I prefer speed over Java-prettiness here.
     * @params obvious as well
     */
    public int iterateUntilMatchesEither(String page, int start, String s1, String s2) {
        for(int i=start; i<page.length(); i++) {
            if(isSubstring(page, i, s1)) return i;
            if(isSubstring(page, i, s2)) return -i;
        }
        return Integer.MAX_VALUE;
    }

    /**
     * {@inheritDoc this#iterateUntilMatchesEither}
     * It's even uglier, especially the offset part.
     */
    public int iterateUntilMatchesEither(String page, int start, String s1, String s2, String s3) {
        for(int i=start; i<page.length(); i++) {
            if(isSubstring(page, i, s1)) return i;
            if(isSubstring(page, i, s2)) return -i;
            if(isSubstring(page, i, s3)) return offset + i;
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Basically s1.substring(i).beginsWith(s2) without object creation and memory allocation.
     */
    public static boolean isSubstring(String s1, int begin, String s2) {
        if (begin + s2.length() > s1.length()) {
            return false;
        }
        for (int i = begin, j = 0; j < s2.length(); i++, j++) {
            if (s1.charAt(i) != s2.charAt(j)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sorry
     */
    public String formatText(String s) {
        return parseUnicode(parseSpecialChars(parseLinks(s)));
    }

    /**
     * Replaces \\u escaped sequences with proper characters
     * @param s String. Keep it short please.
     * @return String with proper characters
     */
    public String parseUnicode(String s) {
        StringBuilder parsed = new StringBuilder();
        boolean parsing = false;
        int ch = 0;
        for (int i = 0; i < s.length(); i++) {
            for (int j = 0; j < 4 && parsing; j++, i++) {
                    if (Character.isDigit(s.charAt(i))) {
                        ch = ch * 16 + s.charAt(i) - '0';
                    } else if (i < s.length() && s.charAt(i) >= 'a' && s.charAt(i) <= 'f') {
                        ch = ch * 16 + s.charAt(i) - 'a' + 10;
                    }
                if (j == 3) {
                    parsed.append(Character.toChars(ch));
                    parsing = false;
                    ch = 0;
                }
            }
            if (i == s.length()) { //whoops
                break;
            }
            if (isSubstring(s, i, "\\u") && s.length() > i + 5) { //there actually are cases where \\u doesn't represent Unicode sequence, but regular text
                parsing = true;
                i++;
            } else {
                parsed.append(s.charAt(i));
            }
        }
        return parsed.toString();
    }

    /**
     * Replaces HTML entities and newlines with proper characters
     * @param s raw text
     * @return formatted text
     */
    public String parseSpecialChars(String s) {
        return s.replace("&gt;", ">").replace("&lt;", "<").replace("&amp;", "&")
                .replace("&#39;", "\'").replace("&quot;", "\"")
                .replaceAll("\\Q<br />\\E|\\Q<br/>\\E|\\Q\\n\\n\"\\E|\\Q\\n\\E|\\Q<div class=\\\"answer-paragraph\\\"></div>\\E", "\n   ")
                // ^ newline na 5 nacina
                .replace("\\\\", "\\");
    }

    /**
     * Replaces links and images in plaintext. Slooow. Also created before iterateUntil methods, so does index manipulation
     * and iteration by itself.
     * @param s raw text
     * @return formatted text
     */
    public String parseLinks(String s) {
        StringBuilder parsed = new StringBuilder();
        StringBuilder link = new StringBuilder(32);
        for (int i = 0; i < s.length(); i++) {
            if (isSubstring(s, i, linkBegin)) {
                link.delete(0, link.length());
                i += linkBegin.length();
                while (!isSubstring(s, i, linkHrefBegin) && !isSubstring(s, i, linkHttpsHrefBegin)) {
                    i++;
                }
                i += linkHrefBegin.length();
                while (!isSubstring(s, i, linkEnd)) {
                    link.append(s.charAt(i));
                    i++;
                }
                Main.queue.addLink(link.toString());
                parsed.append(link);
                i += linkEnd.length();
            }
            if (isSubstring(s, i, imgBegin)) {
                i += imgBegin.length();
                while (!isSubstring(s, i, imgSrcBegin)) {
                    i++;
                }
                i += imgSrcBegin.length();
                while (!isSubstring(s, i, imgEnd)) {
                    parsed.append(s.charAt(i));
                    i++;
                }
                i += imgEnd.length();
            }
            if (i < s.length()) {
                parsed.append(s.charAt(i));
            }
        }
        return parsed.toString();
    }

    /**
     * wannabe struct
     */
    static class Question {
        String question;
        String answer;
        String date;
        String author;
        String likes;

        Question(String question, String answer, String date, @Nullable String author, @Nullable String likes) {
            this.question = question;
            this.answer = answer;
            this.date = date;
            this.author = author;
            this.likes = likes;
        }

        @Override
        public String toString() {
            if(author==null && likes == null) {
                return "Q: " + question + " @ " + date + "\nA: " + answer;
            } else if(likes == null) {
                return "Q: " + question + " - " + author + " @ " + date + "\nA: " + answer;
            } else if(author == null) {
                return "Q: " + question + " @ " + date + "\nA: " + answer + "\nL: " + likes;
            } else {
                return "Q: " + question + " - " + author + " @ " + date + "\nA: " + answer + "\nL: " + likes;
            }
        }
    }
}
