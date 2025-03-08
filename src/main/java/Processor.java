package src.main.java;
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
        System.out.println("Input Stream: " + input);
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String line = reader.readLine();

        line = reader.readLine();
        String[] parts = line.split("\\s+");

        // Check if we have all required parts of the request line
        if (parts.length < 3) {
            throw new IOException("Malformed HTTP request line");
        }
        
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
     * Processes an HTTP request and routes it to the appropriate system, network, hardware, or utility endpoint.
     * 
     * This method handles different URL paths and HTTP methods, executing system commands 
     * to retrieve various types of system information. It supports GET requests for multiple 
     * endpoints related to system diagnostics and monitoring.
     * 
     * Supported endpoints include:
     * <ul>
     *   <li>/: Root endpoint with basic response</li>
     *   <li>/system/info: Operating system release information</li>
     *   <li>/system/memory: Memory usage details</li>
     *   <li>/system/disk: Disk space information</li>
     *   <li>/network/iface: Network interface details</li>
     *   <li>/network/ip: IP route information</li>
     *   <li>/network/ping: Network connectivity test</li>
     *   <li>/hardware/cpu: CPU usage statistics</li>
     *   <li>/hardware/load: System load average</li>
     *   <li>/hardware/processes: Running processes list</li>
     *   <li>/util/time: System time and date information</li>
     *   <li>/util/logs: Recent system logs</li>
     *   <li>/health: Comprehensive system health overview</li>
     * </ul>
     * 
     * For unsupported HTTP methods, a 405 Method Not Allowed status is returned.
     * For unrecognized paths, a 404 Not Found status is returned.
     * 
     * @param response The HttpResponse object containing request details and to be populated with response data
     * @return The modified HttpResponse object with status code, body, and other relevant information
     */
    public HttpResponse processRequest(HttpResponse response) {
        switch(response.getUrlPath()) {
            case "/":
                if (response.getMethod().equals("GET")) {
                    response.setStatusCode("200 OK");
                    response.setBody("Successful GET Request");
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/system/info":
                if (response.getMethod().equals("GET")) {
                    ProcessBuilder systemInfo = new ProcessBuilder("cat", "/etc/os-release");
                    executeCommand(systemInfo, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/system/memory":
                if (response.getMethod().equals("GET")) {
                    ProcessBuilder memoryInfo = new ProcessBuilder("free", "-m");
                    executeCommand(memoryInfo, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/system/disk":
                if (response.getMethod().equals("GET")) {
                    ProcessBuilder diskSpace = new ProcessBuilder("df", "-h");
                    executeCommand(diskSpace, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                };
                break;
            case "/network/iface":
                if (response.getMethod().equals("GET")) {
                    ProcessBuilder networkInterface = new ProcessBuilder("ip", "addr");
                    executeCommand(networkInterface, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/network/ip":
                if (response.getMethod().equals("GET")) {
                    ProcessBuilder ipAddress = new ProcessBuilder("ip", "route get 1");
                    executeCommand(ipAddress, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/network/ping":
                if (response.getMethod().equals("GET")) {
                    ProcessBuilder ping = new ProcessBuilder("ping", "-c 4 8.8.8.8");
                    executeCommand(ping, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/hardware/cpu":
                if (response.getMethod().equals("GET")) {
                    ProcessBuilder cpu = new ProcessBuilder("top", "-bn1 | grep \"Cpu(s)\"");
                    executeCommand(cpu, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/hardware/load":
                if (response.getMethod().equals("GET")) {
                    ProcessBuilder load = new ProcessBuilder("cat", "/proc/loadavg");
                    executeCommand(load, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/hardware/processes":
                if (response.getMethod().equals("GET")) {
                    ProcessBuilder proc = new ProcessBuilder("ps", "aux");
                    executeCommand(proc, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/util/time":
                if (response.getMethod().equals("GET")) {
                    ProcessBuilder time = new ProcessBuilder("timedatectl");
                    executeCommand(time, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/util/logs":
                if (response.getMethod().equals("GET")) {
                    ProcessBuilder logs = new ProcessBuilder("journalctl", "-n 50");
                    executeCommand(logs, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/health":
                if (response.getMethod().equals("GET")) {
                    ProcessBuilder systemHealth = new ProcessBuilder("bash", "-c", "uptime && free -h && df -h && top -bn1 | grep 'Cpu(s)'");
                    executeCommand(systemHealth, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            default:
                response.setStatusCode("404 NOT FOUND");
                response.setBody("Cannot find what you are looking for.");        
        }

        return response;
    }

    /**
     * Executes a system command and processes its output.
     * 
     * <p>This method performs the following operations:</p>
     * <ul>
     *   <li>Starts the specified system command using ProcessBuilder</li>
     *   <li>Reads the command's output stream, line by line</li>
     *   <li>Captures the entire output in a StringBuilder</li>
     *   <li>Sets the HTTP response status to 200 OK on successful execution</li>
     *   <li>Sets the response body to the command's output</li>
     * </ul>
     * 
     * @param command The ProcessBuilder containing the system command to execute
     * @param response the HttpResponse object containing response information
     */
    private void executeCommand(ProcessBuilder command, HttpResponse response) {
        try {
            Process process = command.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                response.setStatusCode("200 OK");
                response.setBody(sb.toString());
            } else {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                StringBuilder errorSb = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorSb.append(errorLine).append("\n");
                }
                response.setStatusCode("500 Internal Server Error");
                response.setBody("Command failed " + errorSb);
            }
        } catch (IOException | InterruptedException e) {
            response.setStatusCode("500 Internal Server Error");
            response.setBody("Error executing command: " + e.getMessage());
        }
    }
}
