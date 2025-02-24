package org.synapse.functions;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import org.synapse.sparql.SparqlQueryBuilder;

import java.util.*;

/**
 * Azure Function that provides a REST API interface for querying the DME Product Catalog.
 * This function acts as a bridge between HTTP requests and the GraphDB SPARQL endpoint.
 */
public class HttpTriggerJava {
    private final SparqlQueryBuilder queryBuilder;
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    /**
     * Initializes the function with a GraphDB connection.
     * The GraphDB URL is retrieved from environment variables.
     */
    public HttpTriggerJava() {
        String graphdbUrl = requireEnvVariable("GRAPHDB_URL");
        this.queryBuilder = new SparqlQueryBuilder(graphdbUrl);
    }

    /**
     * HTTP endpoint for querying DME product catalog.
     * Supports querying by:
     * - Product ID (/api/products?productId=1234)
     * - HCPCS code (/api/products?hcpcs=K0001)
     * - Diagnosis code (/api/products?dx=M17.11)
     *
     * @param request The HTTP request containing query parameters
     * @param context The Azure Function execution context for logging
     * @return HTTP response containing the query results in JSON format
     */
    @FunctionName("QueryProducts")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                authLevel = AuthorizationLevel.ANONYMOUS,
                route = "products"
            ) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Processing DME product catalog query");

        try {
            QueryParameters params = new QueryParameters(request.getQueryParameters());

            if (!params.isValid()) {
                return createErrorResponse(request, HttpStatus.BAD_REQUEST,
                    "Please provide one of: productId, hcpcs, or dx as a query parameter");
            }

            String query = buildQueryFromParameters(params);
            List<Map<String, String>> results = queryBuilder.executeQuery(query);

            return createSuccessResponse(request, results);

        } catch (Exception e) {
            context.getLogger().severe("Error processing request: " + e.getMessage());
            return createErrorResponse(request, HttpStatus.INTERNAL_SERVER_ERROR,
                "Error processing request: " + e.getMessage());
        }
    }

    /**
     * Helper class to encapsulate and validate query parameters.
     */
    private static class QueryParameters {
        private final Map<String, String> params;

        public QueryParameters(Map<String, String> queryParams) {
            this.params = queryParams;
        }

        public boolean isValid() {
            return params.containsKey("productId") ||
                   params.containsKey("hcpcs") ||
                   params.containsKey("dx");
        }

        public String getProductId() {
            return params.get("productId");
        }

        public String getHcpcs() {
            return params.get("hcpcs");
        }

        public String getDx() {
            return params.get("dx");
        }
    }

    /**
     * Builds the appropriate SPARQL query based on the provided parameters.
     *
     * @param params The validated query parameters
     * @return A SPARQL query string
     */
    private String buildQueryFromParameters(QueryParameters params) {
        if (params.getProductId() != null) {
            return queryBuilder.buildQueryByProduct(params.getProductId());
        } else if (params.getHcpcs() != null) {
            return queryBuilder.buildQueryByHCPCS(params.getHcpcs());
        } else {
            return queryBuilder.buildQueryByDX(params.getDx());
        }
    }

    /**
     * Creates a success response with JSON content.
     *
     * @param request The original HTTP request
     * @param body The response body
     * @return An HTTP response with OK status
     */
    private HttpResponseMessage createSuccessResponse(
            HttpRequestMessage<Optional<String>> request,
            Object body) {
        return request.createResponseBuilder(HttpStatus.OK)
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .body(body)
            .build();
    }

    /**
     * Creates an error response with the specified status and message.
     *
     * @param request The original HTTP request
     * @param status The HTTP status code
     * @param message The error message
     * @return An HTTP response with the specified error status
     */
    private HttpResponseMessage createErrorResponse(
            HttpRequestMessage<Optional<String>> request,
            HttpStatus status,
            String message) {
        return request.createResponseBuilder(status)
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .body(Map.of("error", message))
            .build();
    }

    /**
     * Retrieves a required environment variable.
     *
     * @param name The name of the environment variable
     * @return The value of the environment variable
     * @throws IllegalStateException if the variable is not set
     */
    protected String requireEnvVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                "Required environment variable '" + name + "' is not set");
        }
        return value;
    }
}