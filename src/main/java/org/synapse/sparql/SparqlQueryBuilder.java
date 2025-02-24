package org.synapse.sparql;

    import org.apache.jena.query.QueryExecution;
    import org.apache.jena.query.QuerySolution;
    import org.apache.jena.query.ResultSet;
    import org.apache.jena.rdfconnection.RDFConnection;
    import org.apache.jena.rdfconnection.RDFConnectionRemote;

    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;

    /**
     * Builder class for creating and executing SPARQL queries against a GraphDB instance.
     * This class handles the creation of semantic queries for a DME (Durable Medical Equipment) product catalog.
     */
    public class SparqlQueryBuilder {
        private final String graphDBEndpoint;

        /**
         * Common SPARQL prefixes used in all queries.
         * ex: represents the DME ontology namespace
         * rdf: represents the RDF syntax namespace
         */
        private static final String PREFIX = """
                PREFIX ex: <http://synapsehealth.com/dme/>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                """;

        /**
         * Common SELECT clause parts used across queries
         */
        private static final String PRODUCT_ATTRIBUTES = """
                ?product ex:hasName ?productName ;
                    ex:hasPrice ?price ;
                    ex:hasSupplier ?supplier ;
                    ex:hasHCPCSCode ?hcpcsCode ;
                    ex:hasDXCode ?dxCode""";


        public SparqlQueryBuilder(String graphDBEndpoint) {
            this.graphDBEndpoint = graphDBEndpoint;
        }

        /**
         * Builds a SPARQL query to retrieve product details by product ID.
         *
         * @param productId The unique identifier of the product
         * @return A SPARQL query string
         */
        public String buildQueryByProduct(String productId) {
            return buildBaseQuery("""
                SELECT ?productName ?price ?supplierName ?hcpcsCode ?dxCode
                WHERE {
                    ex:product/%s %s .
                    ?supplier ex:hasName ?supplierName .
                }
                """.formatted(productId, PRODUCT_ATTRIBUTES));
        }

        /**
         * Builds a SPARQL query to retrieve products by HCPCS code.
         *
         * @param hcpcsCode The HCPCS (Healthcare Common Procedure Coding System) code
         * @return A SPARQL query string
         */
        public String buildQueryByHCPCS(String hcpcsCode) {
            return buildBaseQuery("""
                SELECT ?productName ?price ?supplierName ?dxCode
                WHERE {
                    %s .
                    ?supplier ex:hasName ?supplierName .
                    FILTER(?hcpcsCode = "%s")
                }
                """.formatted(PRODUCT_ATTRIBUTES, hcpcsCode));
        }

        /**
         * Builds a SPARQL query to retrieve products by diagnosis code.
         *
         * @param dxCode The diagnosis (DX) code
         * @return A SPARQL query string
         */
        public String buildQueryByDX(String dxCode) {
            return buildBaseQuery("""
                SELECT ?productName ?price ?supplierName ?hcpcsCode
                WHERE {
                    %s .
                    ?supplier ex:hasName ?supplierName .
                    FILTER(?dxCode = "%s")
                }
                """.formatted(PRODUCT_ATTRIBUTES, dxCode));
        }

        /**
         * Combines the PREFIX with a query body to create a complete SPARQL query.
         *
         * @param queryBody The main body of the SPARQL query
         * @return A complete SPARQL query string
         */
        private String buildBaseQuery(String queryBody) {
            return PREFIX + queryBody;
        }

        /**
         * Executes a SPARQL query and returns the results as a list of key-value maps.
         *
         * @param sparqlQuery The SPARQL query to execute
         * @return A list of maps containing the query results
         */
        public List<Map<String, String>> executeQuery(String sparqlQuery) {
            List<Map<String, String>> results = new ArrayList<>();

            try (RDFConnection conn = createConnection();
                 QueryExecution qexec = conn.query(sparqlQuery)) {

                results = processResults(qexec.execSelect());
            }
            return results;
        }

        /**
         * Creates a connection to the GraphDB endpoint.
         *
         * @return An RDFConnection instance
         */
        private RDFConnection createConnection() {
            return RDFConnectionRemote.create()
                    .destination(graphDBEndpoint)
                    .build();
        }

        /**
         * Processes the ResultSet from a SPARQL query into a list of maps.
         *
         * @param rs The ResultSet from the query execution
         * @return A list of maps containing the processed results
         */
        private List<Map<String, String>> processResults(ResultSet rs) {
            List<Map<String, String>> results = new ArrayList<>();
            while (rs.hasNext()) {
                QuerySolution solution = rs.next();
                Map<String, String> row = new HashMap<>();
                solution.varNames().forEachRemaining(varName -> {
                    if (solution.get(varName) != null) {
                        row.put(varName, solution.get(varName).toString());
                    }
                });
                results.add(row);
            }
            return results;
        }
    }