 package com.app;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

 /**
 * The com.app.Processor class handles HTTP request parsing and processing.
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
     * and creates an HttpRequest object with the parsed information. Note that
     * the HTTP method is stored in the statusCode field of the HttpRequest.</p>
     * 
     * @param input the InputStream containing the HTTP request
     * @return an HttpRequest object containing the parsed request information
     * @throws IOException if an I/O error occurs while reading from the input stream
     */
    public HttpRequest parseRequest(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String line = reader.readLine();

        if (line == null || line.trim().isEmpty()) {
            throw new IOException("Empty or invalid HTTP request");
        }

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
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                headers.put(key, value);
            } else {
                throw new IOException("Malformed HTTP header: " + line);
            }

        }

        // Debug logs for request headers
        System.out.println("Request Headers:");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            System.out.println(key + ": " + value);
        }
        System.out.println("\n");

        return new HttpRequest(method, protocolVersion, urlPath, headers);
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
     * @param request The HttpRequest object containing request details and to be populated with response data
     * @return The modified HttpResponse object with status code, body, and other relevant information
     */
    public HttpResponse processRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse(request.getProtocolVersion());

        switch(request.getUrlPath()) {
            case "/":
                if (request.getMethod().equals("GET")) {
                    response.setStatusCode("200 OK");
                    response.setBody("Successful GET Request");
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/system/info":
                if (request.getMethod().equals("GET")) {
                    ProcessBuilder systemInfo = new ProcessBuilder("cat", "/etc/os-release");
                    executeCommand(systemInfo, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/system/memory":
                if (request.getMethod().equals("GET")) {
                    ProcessBuilder memoryInfo = new ProcessBuilder("free", "-m");
                    executeCommand(memoryInfo, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/system/disk":
                if (request.getMethod().equals("GET")) {
                    ProcessBuilder diskSpace = new ProcessBuilder("df", "-h");
                    executeCommand(diskSpace, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                };
                break;
            case "/network/iface":
                if (request.getMethod().equals("GET")) {
                    ProcessBuilder networkInterface = new ProcessBuilder("ip", "addr");
                    executeCommand(networkInterface, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/network/ip":
                if (request.getMethod().equals("GET")) {
                    ProcessBuilder ipAddress = new ProcessBuilder("ip", "route get 1");
                    executeCommand(ipAddress, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/network/ping":
                if (request.getMethod().equals("GET")) {
                    ProcessBuilder ping = new ProcessBuilder("ping", "-c 4 8.8.8.8");
                    executeCommand(ping, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/hardware/cpu":
                if (request.getMethod().equals("GET")) {
                    ProcessBuilder cpu = new ProcessBuilder("top", "-bn1 | grep \"Cpu(s)\"");
                    executeCommand(cpu, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/hardware/load":
                if (request.getMethod().equals("GET")) {
                    ProcessBuilder load = new ProcessBuilder("cat", "/proc/loadavg");
                    executeCommand(load, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/hardware/processes":
                if (request.getMethod().equals("GET")) {
                    ProcessBuilder proc = new ProcessBuilder("ps", "aux");
                    executeCommand(proc, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/util/time":
                if (request.getMethod().equals("GET")) {
                    ProcessBuilder time = new ProcessBuilder("timedatectl");
                    executeCommand(time, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/util/logs":
                if (request.getMethod().equals("GET")) {
                    ProcessBuilder logs = new ProcessBuilder("journalctl", "-n 50");
                    executeCommand(logs, response);
                } else {
                    response.setStatusCode("405 Method Not Allowed");
                    response.setBody("Method not supported");
                }
                break;
            case "/health":
                if (request.getMethod().equals("GET")) {
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

        compressResponse(request, response);

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
     * @param response the com.app.HttpResponse object containing response information
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

     /**
      * Compresses the HTTP response body using GZIP compression if applicable.
      *
      * <p>This method determines if compression should be applied based on multiple criteria:</p>
      * <ul>
      *   <li>Client must support GZIP compression (via Accept-Encoding header)</li>
      *   <li>Response body must not be null or empty</li>
      *   <li>Response body size must be at least 1KB (1024 bytes)</li>
      *   <li>Content type must be compressible (text, JSON, XML, JavaScript)</li>
      * </ul>
      *
      * @param request The HTTP request containing client capabilities
      * @param response The HTTP response to potentially compress
      */
    private void compressResponse(HttpRequest request, HttpResponse response) {
        String acceptEncoding = request.getRequestHeaders().get("Accept-Encoding");

        if (acceptEncoding == null || !acceptEncoding.toLowerCase().contains("gzip")) {
            return;
        }

        if (response.getBody() == null || response.getBody().isEmpty()) {
            System.out.println("Is null");
            return;
        }

        byte[] body = response.getBody().getBytes(StandardCharsets.UTF_8);
        if (body.length < 1024) {
            return;
        }

        String contentType = response.getHeaders().get("Content-Type");
        boolean isCompressible = contentType != null && ((contentType.startsWith("text/")) || contentType.contains("json") || contentType.contains("xml") || contentType.contains("javascript"));

        if (!isCompressible) {
            return;
        }

        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(body.length);

            GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream);

            gzipStream.write(body);
            gzipStream.finish();
            gzipStream.close();

            byte[] compressedBody = byteStream.toByteArray();

            response.setCompressedBody(compressedBody);
            response.setHeader("Content-Encoding", "gzip");
            response.setHeader("Content-Length", String.valueOf(compressedBody.length));
            response.setHeader("Vary", "Accept-Encoding");

        } catch (IOException e) {
            System.err.println("Compression failed: " + e.getMessage());
        }
    }
}
