import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import java.util.List;
import java.util.logging.Level;

public class Main {
    static WebClient webClient = null;

    public static void main(String[] args) {

        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
        final String URL = "https://www.nhl.com/player/";

        try {
            webClient = new WebClient();
            webClient.getOptions().setThrowExceptionOnScriptError(false);

            HtmlPage playerPage = getPlayerPage(URL, "Alex Ovechkin");
            Player details = getPlayerDetails(playerPage);

            System.out.println(details);


        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    public static HtmlPage getPlayerPage(String URL, String name) throws Exception {

        final HtmlPage page = webClient.getPage(URL);
        webClient.waitForBackgroundJavaScript(10000);

        HtmlTextInput input = page.getHtmlElementById("searchTerm");
        input.setValue(name);

        webClient.waitForBackgroundJavaScript(10000);

        List<HtmlAnchor> anchors = page.getAnchors();
        HtmlPage playerPage = null;

        for (HtmlAnchor anchor : anchors) {
            if (anchor.getHrefAttribute().contains("alex-ovechkin")) {
                playerPage = anchor.click();
                break;
            }
        }

        webClient.waitForBackgroundJavaScript(10000);

        if (playerPage == null) {
            throw new Exception("Failed to load player page");
        }

        return playerPage;
    }

    public static Player getPlayerDetails(HtmlPage page){
        List<HtmlSpan> attributesWrapper = page.getByXPath("//div[@class='player-jumbotron-vitals__attributes']//span");
        List<HtmlListItem> bioWrapper = page.getByXPath("//ul[@class='player-bio__list']//li");


        String position = attributesWrapper.get(0).asNormalizedText();
        String age = attributesWrapper.get(3).asNormalizedText();
        String team = attributesWrapper.get(6).getTextContent();
        String born = bioWrapper.get(2).asNormalizedText();
        String shoots = bioWrapper.get(3).asNormalizedText();

        return new Player(position, age.substring(4), team, born.substring(born.length() - 4), shoots.substring(7));
    }

    static class Player{
        String position;
        String age;
        String team;
        String born;
        String shoots;

        public Player(String position, String age, String team, String born, String shoots){
            this.position = position;
            this.age = age;
            this.team = team;
            this.born = born;
            this.shoots = shoots;
        }

        @Override
        public String toString(){
            return "Position: " + this.position + "\n" +
                    "Age: " + this.age + "\n" +
                    "Team: " + this.team + "\n" +
                    "Born: " + this.born + "\n" +
                    "Shoots: " + this.shoots;
        }
    }
}
