import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Responder {
    public void sendResponse(HttpResponse response, OutputStream outputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(response.getProtocolVersion()).append(" ").append(response.getStatusCode()).append("\r\n");

        for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
            sb.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }

        sb.append("\r\n");
        sb.append(response.getBody());

        System.out.println(sb);

        outputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }
}
