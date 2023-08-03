/*package ca.yorku.eecs;
import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import org.neo4j.driver.*;
 

public class App 
{
    static int PORT = 8080;
    public static void main(String[] args) throws IOException
    {
        // Connect to Neo4j database
        String uri = "bolt://localhost:7687";
        String username = "neo4j"; // The default username for Neo4j is 'neo4j'
        String password = "12345678"; // Replace with your actual password
        
       
        Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
        Session session = driver.session();
        
        // Execute the Cypher query to create a new node with properties
        
        
        String createQuery = "CREATE (m:Movie {title:'Mission Impossible', released:2033})";
        session.run(createQuery);

        System.out.println("Node created in Neo4j database.");

        /*
        try (Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
             Session session = driver.session()) {

            // Execute the Cypher query to retrieve nodes with a specific property
            String query = "MATCH (m:Movie {title:'Oppenheimer'}) RETURN m";
            Result result = session.run(query);

            // Process the results
            while (result.hasNext()) {
                Record record = result.next();
                org.neo4j.driver.types.Node node = record.get("m").asNode();

                // Get properties of the node
                String title = node.get("title").asString();
                int released = node.get("released").asInt();

                // Print the node properties
                System.out.println("Movie Title: " + title);
                System.out.println("Released Year: " + released);
            }

            System.out.println("Retrieved nodes from Neo4j database.");
        }
    }
}
*/

package ca.yorku.eecs;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.json.JSONObject;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

public class App {
    static int PORT = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        server.createContext("/api/v1/addActor", new AddActorHandler());
        server.start();
        System.out.printf("Server started on port %d...\n", PORT);
    }

    static class AddActorHandler implements HttpHandler {
        private final Driver driver;

        public AddActorHandler() {
            // Neo4j database connection parameters
            String uri = "bolt://localhost:7687"; // Replace with your Neo4j URI
            String username = "neo4j"; // The default username for Neo4j is 'neo4j'
            String password = "12345678"; // Replace with your Neo4j password

            // Initialize the Neo4j driver
            driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();

            if ("PUT".equalsIgnoreCase(requestMethod)) {
                String requestBody = Utils.getBody(exchange);

                String name = null;
                String actorId = null;
                try {
                    JSONObject json = new JSONObject(requestBody);
                    name = json.getString("name");
                    actorId = json.getString("actorId");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (name != null && !name.isEmpty() && actorId != null && !actorId.isEmpty()) {
                    createActorNode(name, actorId);
                    exchange.sendResponseHeaders(200, 0);
                } else {
                    exchange.sendResponseHeaders(400, 0);
                }
            } else {
                exchange.sendResponseHeaders(405, 0);
            }

            OutputStream outputStream = exchange.getResponseBody();
            outputStream.close();
        }

        // Method to create an actor node in the database
        private void createActorNode(String name, String actorId) {
            String createQuery = "CREATE (a:Actor {name: $name, actorId: $actorId})";

            try (Session session = driver.session()) {
                session.run(createQuery, 
                    org.neo4j.driver.Values.parameters("name", name, "actorId", actorId));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
