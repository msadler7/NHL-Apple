import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class Main {
    final static String URL = "https://www.nhl.com/player/";
    final static List<Player> players = Collections.synchronizedList(new ArrayList<>());
    final static List<String> failed = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {

        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);

        try {
            String[] names = Files.readString(Path.of("players.txt"), StandardCharsets.UTF_8).split("\n");
            ExecutorService service = Executors.newFixedThreadPool(5);

            for (String name : names) {
                service.execute(new Task(name));
            }
            service.shutdown();
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            writeToFile(players, failed);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    static class Task implements Runnable {
        String name;
        WebClient webClient;

        public Task(String name) {
            this.name = name;
            this.webClient = new WebClient();
        }

        @Override
        public void run() {

            try {
                webClient.getOptions().setCssEnabled(false);
                webClient.getOptions().setPopupBlockerEnabled(true);
                webClient.getOptions().setThrowExceptionOnScriptError(false);

                HtmlPage playerPage = getPlayerPage(URL);
                Player details = getPlayerDetails(playerPage);
                System.out.println("Found " + name);
                players.add(details);
            } catch (Exception e) {
                System.out.println("Could not fetch details for " + name);
                failed.add(name);
            }
        }

        public HtmlPage getPlayerPage(String URL) throws Exception {

            final HtmlPage page = webClient.getPage(URL);
            webClient.waitForBackgroundJavaScript(35000);

            HtmlTextInput input = page.getHtmlElementById("searchTerm");
            input.setValue(name);

            webClient.waitForBackgroundJavaScript(35000);

            List<HtmlAnchor> anchors = page.getByXPath("//td[@class='results-table__player-td']//a[@class='name-link']");
            HtmlPage playerPage = null;
            String href = name.replace(" ", "-").toLowerCase();

            for (HtmlAnchor anchor : anchors) {
                if (anchor.getHrefAttribute().contains(href)) {
                    playerPage = anchor.click();
                    break;
                }
            }

            webClient.waitForBackgroundJavaScript(15000);
            if (playerPage == null) {
                throw new NullPointerException();
            }

            return playerPage;
        }

        public Player getPlayerDetails(HtmlPage page) throws IndexOutOfBoundsException {
            List<HtmlSpan> attributesWrapper = page.getByXPath("//div[@class='player-jumbotron-vitals__attributes']//span");
            List<HtmlListItem> bioWrapper = page.getByXPath("//ul[@class='player-bio__list']//li");


            String position = attributesWrapper.get(0).asNormalizedText();
            String age = attributesWrapper.get(3).asNormalizedText();
            String team = attributesWrapper.get(6).getTextContent();
            String born = bioWrapper.get(2).asNormalizedText();
            String shoots = bioWrapper.get(3).asNormalizedText();

            return new Player(name, position, age.substring(4), team, born.substring(born.length() - 4), shoots.substring(7));
        }
    }

    public static void writeToFile(List<Player> players, List<String> failed) throws IOException {
        Gson gson = new Gson();
        String playerJSON = gson.toJson(players);
        Files.writeString(Path.of("details.json"), playerJSON);
        Files.writeString(Path.of("failed.txt"), failed.toString());
    }

    static class Player {

        String name;
        String position;
        String age;
        String team;
        String born;
        String shoots;

        public Player(String name, String position, String age, String team, String born, String shoots) {
            this.name = name;
            this.position = position;
            this.age = age;
            this.team = team;
            this.born = born;
            this.shoots = shoots;
        }

        @Override
        public String toString() {
            return "Name: " + this.name + "\n" +
                    "Position: " + this.position + "\n" +
                    "Age: " + this.age + "\n" +
                    "Team: " + this.team + "\n" +
                    "Born: " + this.born + "\n" +
                    "Shoots: " + this.shoots + "\n";
        }
    }
}
