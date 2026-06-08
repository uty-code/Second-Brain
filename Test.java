import java.net.http.*;
import java.net.*;
public class Test {
  public static void main(String[] args) throws Exception {
    System.out.println("Starting request...");
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/chat/completions")).build();
    client.send(req, HttpResponse.BodyHandlers.ofString());
    System.out.println("Done.");
  }
}
