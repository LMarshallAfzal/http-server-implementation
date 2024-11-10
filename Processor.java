import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class Processor {

    public void parseRequest(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String line;

        line = reader.readLine();
        String[] parts = line.split("\\s+");
        
        String method = parts[0];
        String urlPath = parts[1];
        String protocolVersion = parts[2];

        HashMap<String, String> headers = new HashMap<>();
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            String[] headerParts = line.split(":");
            headers.put(headerParts[0].trim(), headerParts[1].trim());
        }

        HttpResponse response = new HttpResponse(method, protocolVersion, urlPath, headers);
        processRequest(response);
    }

    private void processRequest(HttpResponse response) {
        switch(response.getStatusCode()) {
            case "GET":
                // String[] urlParts = response.getUrlPath().split("\\?");
                // String path = urlParts[0];

                // if (urlParts.length > 1) {
                //     String queryString = urlParts[1];
                //     HashMap<String, String> parameters = parseQueryString(queryString);
                // }
                System.out.println("This is a get request");
                generateResponse(response);
                break;
            case "POST":
            case "PUT":
            case "PATCH":
            case "DELETE":                

        }
    }

    public HttpResponse generateResponse(HttpResponse response) {
        if (response.getUrlPath().equals("/")) {
            response.setStatusCode("200 OK");
            response.setBody("Successful GET Request");
        }

        System.out.println(response.getBody());
        
        return response;
    }

    // private HashMap<String, String> parseQueryString(String queryString) {
    //     HashMap<String, String> parameters = new HashMap<>();
    //     for (String keyValue : queryString.split("&")) {
    //         String[] parts = keyValue.split("=");
    //         if (parts.length == 2) {
    //             parameters.put(parts[0], parts[1]);
    //         }
    //     }
    //     return parameters;
    // }

   
    
}
