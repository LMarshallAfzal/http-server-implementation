import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * The Processor class handles HTTP request parsing and processing.
 * It reads the incoming HTTP request, extracts relevant information,
 * and generates an appropriate HTTP response.
 * 
 * <p>This class is responsible for:</p>
 * <ul>
 *   <li>Parsing raw HTTP request data from an input stream</li>
 *   <li>Processing requests based on their HTTP method</li>
 *   <li>Generating appropriate responses</li>
 *   <li>Handling query parameters (currently commented out)</li>
 * </ul>
 */
public class Processor {

    /**
     * Parses an HTTP request from the given input stream.
     * 
     * <p>This method reads the request line and headers from the input stream
     * and creates an HttpResponse object with the parsed information. Note that
     * the HTTP method is stored in the statusCode field of the HttpResponse.</p>
     * 
     * @param input the InputStream containing the HTTP request
     * @return an HttpResponse object containing the parsed request information
     * @throws IOException if an I/O error occurs while reading from the input stream
     */
    public HttpResponse parseRequest(InputStream input) throws IOException {
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


        return response;
    }

    /**
     * Processes an HTTP request and generates a response.
     * 
     * <p>This method determines the appropriate action based on the HTTP method
     * stored in the statusCode field of the HttpResponse. Currently, only GET
     * requests to the root path ("/") are fully implemented.</p>
     * 
     * @param response the HttpResponse object containing the parsed request information
     * @return an HttpResponse object containing the response to be sent back to the client
     */
    public HttpResponse processRequest(HttpResponse response) {
        switch(response.getStatusCode()) {
            case "GET":
                // String[] urlParts = response.getUrlPath().split("\\?");

                // if (urlParts.length > 1) {
                //     String queryString = urlParts[1];
                //     HashMap<String, String> parameters = parseQueryString(queryString);
                // }

                return generateResponse(response);
            case "POST":
            case "PUT":
            case "PATCH":
            case "DELETE":                
        }

        return response;
    }

    /**
     * Parses a query string into a map of parameter name-value pairs.
     * 
     * <p>This method splits the query string on '&' characters to separate
     * individual parameters, then splits each parameter on '=' to separate
     * the name and value.</p>
     * 
     * @param queryString the query string to parse (e.g., "name=value&key=data")
     * @return a HashMap mapping parameter names to their values
     */
    private HashMap<String, String> parseQueryString(String queryString) {
        HashMap<String, String> parameters = new HashMap<>();
        for (String keyValue : queryString.split("&")) {
            String[] parts = keyValue.split("=");
            if (parts.length == 2) {
                parameters.put(parts[0], parts[1]);
            }
        }
        return parameters;
    }

    /**
     * Generates an HTTP response for the given request.
     * 
     * <p>Currently, this method only handles requests to the root path ("/"),
     * for which it generates a 200 OK response with a simple message.</p>
     * 
     * @param response the HttpResponse object containing the parsed request information
     * @return an HttpResponse object with the appropriate status code and body
     */
    private HttpResponse generateResponse(HttpResponse response) {
        if (response.getUrlPath().equals("/")) {
            response.setStatusCode("200 OK");
            response.setBody("Successful GET Request");

        }
        return response;
    }
}
