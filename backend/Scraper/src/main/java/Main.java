import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.google.gson.*;
import java.io.IOException;
import java.net.MalformedURLException;
import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;

// TODO: Think about adding threading to decrease runtime

public class Main {
    static WebClient webClient = null;

    public static void main(String[] args) {

        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
        final String URL = "https://www.nhl.com/player/";
        List<Player> players = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        try {
            webClient = new WebClient();
            webClient.getOptions().setThrowExceptionOnScriptError(false);

            File file = new File("players.txt");
            Scanner s = new Scanner(file);

            String player = null;

            while (s.hasNext()) {
                try {
                    player = s.nextLine().strip();
                    HtmlPage playerPage = getPlayerPage(URL, player);
                    Player details = getPlayerDetails(playerPage, player);
                    players.add(details);
                } catch (FailingHttpStatusCodeException | MalformedURLException |
                         ElementNotFoundException | IndexOutOfBoundsException |
                         NullPointerException e) {
                    System.out.println("Could not fetch details for " + player);
                    failed.add(player);
                }
            }

            s.close();
            writeToFile(players, failed);


        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static HtmlPage getPlayerPage(String URL, String name) throws Exception{

        final HtmlPage page = webClient.getPage(URL);
        webClient.waitForBackgroundJavaScript(10000);

        HtmlTextInput input = page.getHtmlElementById("searchTerm");
        input.setValue(name);

        webClient.waitForBackgroundJavaScript(10000);

        List<HtmlAnchor> anchors = page.getAnchors();
        HtmlPage playerPage = null;

        for (HtmlAnchor anchor : anchors) {
            if (anchor.getHrefAttribute().contains(String.join("-", name.split(" ")).toLowerCase())) {
                playerPage = anchor.click();
                break;
            }
        }

        webClient.waitForBackgroundJavaScript(10000);

        if (playerPage == null) { throw new NullPointerException(); }

        return playerPage;
    }

    public static Player getPlayerDetails(HtmlPage page, String name) throws IndexOutOfBoundsException {
        List<HtmlSpan> attributesWrapper = page.getByXPath("//div[@class='player-jumbotron-vitals__attributes']//span");
        List<HtmlListItem> bioWrapper = page.getByXPath("//ul[@class='player-bio__list']//li");


        String position = attributesWrapper.get(0).asNormalizedText();
        String age = attributesWrapper.get(3).asNormalizedText();
        String team = attributesWrapper.get(6).getTextContent();
        String born = bioWrapper.get(2).asNormalizedText();
        String shoots = bioWrapper.get(3).asNormalizedText();

        return new Player(name, position, age.substring(4), team, born.substring(born.length() - 4), shoots.substring(7));
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
            return  "Name: " + this.name + "\n" +
                    "Position: " + this.position + "\n" +
                    "Age: " + this.age + "\n" +
                    "Team: " + this.team + "\n" +
                    "Born: " + this.born + "\n" +
                    "Shoots: " + this.shoots + "\n";
        }
    }
}
