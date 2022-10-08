import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import java.util.List;
import java.util.logging.Level;

public class Main {
    public static void main(String[] args) {

        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
        final String URL = "https://www.nhl.com/player";

        try(final WebClient webClient = new WebClient()) {

            webClient.getOptions().setThrowExceptionOnScriptError(false);
            final HtmlPage page = webClient.getPage(URL);

            webClient.waitForBackgroundJavaScript(10000);
            HtmlTextInput input = page.getHtmlElementById("searchTerm");
            input.setValue("Alex Ovechkin");
            webClient.waitForBackgroundJavaScript(10000);

            List<HtmlAnchor> anchors = page.getAnchors();
            HtmlPage playerPage = null;

            for (HtmlAnchor anchor : anchors) {
                if (anchor.getHrefAttribute().contains("alex-ovechkin")){
                    playerPage = anchor.click();
                    break;
                }
            }

            if (playerPage != null){
                webClient.waitForBackgroundJavaScript(5000);
                System.out.println(playerPage.getTitleText());
            }



        }catch(Exception e){
            System.out.println(e);
        }



    }
}
