package org.synapse.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpTriggerJavaUnitTest {

    @Mock
    private HttpRequestMessage<Optional<String>> request;

    @Mock
    private ExecutionContext context;

    private HttpTriggerJava function;

    @BeforeEach
    void setUp() {
        // Mock logger
        Logger logger = mock(Logger.class);
        when(context.getLogger()).thenReturn(logger);

        // Create function instance with overridden env variable
        function = new HttpTriggerJava() {
            @Override
            protected String requireEnvVariable(String name) {
                return "http://test-graphdb:7200/repositories/test";
            }
        };
    }

    @Test
    void testProductIdQuery() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("productId", "1234");

        when(request.getQueryParameters()).thenReturn(queryParams);

        HttpResponseMessage.Builder builder = mock(HttpResponseMessage.Builder.class);
        HttpResponseMessage responseMock = mock(HttpResponseMessage.class);
        when(request.createResponseBuilder(any(HttpStatus.class))).thenReturn(builder);
        when(builder.header(anyString(), anyString())).thenReturn(builder);
        when(builder.body(any())).thenReturn(builder);
        when(builder.build()).thenReturn(responseMock);

        HttpResponseMessage response = function.run(request, context);

        verify(request).getQueryParameters();
        verify(builder).header("Content-Type", "application/json");
        assertEquals(responseMock, response);
    }

    @Test
    void testInvalidQueryParameters() {
        Map<String, String> queryParams = new HashMap<>();
        when(request.getQueryParameters()).thenReturn(queryParams);

        HttpResponseMessage.Builder builder = mock(HttpResponseMessage.Builder.class);
        HttpResponseMessage responseMock = mock(HttpResponseMessage.class);
        when(request.createResponseBuilder(HttpStatus.BAD_REQUEST)).thenReturn(builder);
        when(builder.header(anyString(), anyString())).thenReturn(builder);
        when(builder.body(any())).thenReturn(builder);
        when(builder.build()).thenReturn(responseMock);

        HttpResponseMessage response = function.run(request, context);

        verify(request).getQueryParameters();
        verify(builder).header("Content-Type", "application/json");
        assertEquals(responseMock, response);
    }

    @Test
    void testHcpcsQuery() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("hcpcs", "K0001");

        setupSuccessResponse(queryParams);
        HttpResponseMessage response = function.run(request, context);

        verify(request).getQueryParameters();
        assertEquals(HttpStatus.OK, response.getStatus());
    }

    @Test
    void testDxQuery() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("dx", "M17.11");

        setupSuccessResponse(queryParams);
        HttpResponseMessage response = function.run(request, context);

        verify(request).getQueryParameters();
        assertEquals(HttpStatus.OK, response.getStatus());
    }

    @Test
    void testQueryExecutionError() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("productId", "1234");
        when(request.getQueryParameters()).thenReturn(queryParams);

        setupErrorResponse();
        HttpResponseMessage response = function.run(request, context);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
    }

    // Helper method for common response setup
    private void setupSuccessResponse(Map<String, String> queryParams) {
        when(request.getQueryParameters()).thenReturn(queryParams);

        HttpResponseMessage.Builder builder = mock(HttpResponseMessage.Builder.class);
        HttpResponseMessage responseMock = mock(HttpResponseMessage.class);
        when(request.createResponseBuilder(any(HttpStatus.class))).thenReturn(builder);
        when(builder.header(anyString(), anyString())).thenReturn(builder);
        when(builder.body(any())).thenReturn(builder);
        when(builder.build()).thenReturn(responseMock);
        when(responseMock.getStatus()).thenReturn(HttpStatus.OK);
    }

    private void setupErrorResponse() {
        HttpResponseMessage.Builder builder = mock(HttpResponseMessage.Builder.class);
        HttpResponseMessage responseMock = mock(HttpResponseMessage.class);
        when(request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)).thenReturn(builder);
        when(builder.header(anyString(), anyString())).thenReturn(builder);
        when(builder.body(any())).thenReturn(builder);
        when(builder.build()).thenReturn(responseMock);
        when(responseMock.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}