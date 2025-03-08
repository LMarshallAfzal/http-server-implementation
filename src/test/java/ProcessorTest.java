import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

import src.main.java.Processor;
import src.main.java.HttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ProcessorTest {
    private Processor processor;

    @BeforeEach
    public void setUp() {
        processor = new Processor();
    }

    @Test
    void testParseRequest_NoHeaders() throws IOException {
        String httpRequest = "GET / HTTP/1.1\r\n\r\n";
        
        InputStream inputStream = new ByteArrayInputStream(httpRequest.getBytes(StandardCharsets.UTF_8));

        assertThrows(IOException.class, () -> {
            processor.parseRequest(inputStream);
        }, "No headers should throw an IOException.");
    }

    @Test
    void testParseRequest_EmptyRequest_ThrowsException() {
        String httpRequest = "";
        
        InputStream inputStream = new ByteArrayInputStream(httpRequest.getBytes(StandardCharsets.UTF_8));
        
        assertThrows(NullPointerException.class, () -> {
            processor.parseRequest(inputStream);
        }, "Empty request should throw an NullPointerException.");
    }
}