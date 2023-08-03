package ca.yorku.eecs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import com.sun.net.httpserver.HttpExchange;

class Utils {   
    //testing communication with api
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
/////
    
    // use for extracting query params
    public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }

    // one possible option for extracting JSON body as String
    public static String convert(InputStream inputStream) throws IOException {
                
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    // another option for extracting JSON body as String
    public static String getBody(HttpExchange he) throws IOException {
                InputStreamReader isr = new InputStreamReader(he.getRequestBody(), "utf-8");
            BufferedReader br = new BufferedReader(isr);
            
            int b;
            StringBuilder buf = new StringBuilder();
            while ((b = br.read()) != -1) {
                buf.append((char) b);
            }

            br.close();
            isr.close();
	    
        return buf.toString();
        }
    
    public void handle(HttpExchange request) throws IOException {
		// TODO Auto-generated method stub
		
        try {
            if (request.getRequestMethod().equals("addActor")) {
                //addActor(request);
                System.out.println("PUT");
            } else if(request.getRequestMethod().equals("addMovie")) {
            	System.out.println("PUT");
            	//addMovie(request);
            } else if(request.getRequestMethod().equals("addRelationship")) {
            	System.out.println("PUT");
            	//addRelationship(request);				
            } else if(request.getRequestMethod().equals("getActor")) {
            	System.out.println("GET");
            	//getActor(request);		
            }
            else if(request.getRequestMethod().equals("getMovie")) {
            	System.out.println("GET");
            	//getMovie(request);		
            }
            else if(request.getRequestMethod().equals("hasRelationship")) {
            	System.out.println("GET");
            	//hasRelationship(request);		
            }
            else if(request.getRequestMethod().equals("computeBaconNumber")) {
            	System.out.println("GET");
            	//computeBacon(request);		
            }
            else {
            	System.out.println("Unrecognized command");
            	//sendResponse(request, "Unimplemented method\n", 501);
            }
        } catch (Exception e) {
        	e.printStackTrace();
        	//sendResponse(request, "Server error\n", 500);
        	System.out.println("Server error");
        }
		
	}
    
//    private void sendResponse(HttpExchange request, String data, int restCode) throws IOException {
//		request.sendResponseHeaders(restCode, data.length());
//        OutputStream os = request.getResponseBody();
//        os.write(data.getBytes());
//        os.close();
//	}
    
    private void handleGet(HttpExchange request) throws IOException {
    	
        URI uri = request.getRequestURI();
        String query = uri.getQuery();
        System.out.println(query);
        Map<String, String> queryParam = splitQuery(query);
        System.out.println(queryParam);
        long first = Long.parseLong(queryParam.get("firstNumber"));
        long second = Long.parseLong(queryParam.get("secondNumber"));
        
        // add code for incorrect parameters

        /* TODO: Implement the math logic */
        long answer = first + second;
        System.out.println(first+","+second+","+answer);
        String response = Long.toString(answer) + "\n";
        //sendResponse(request, response, 200);
    }
//  private void addMovie(HttpExchange request) throws IOException {
//	
//}
    
//  private void addActor(HttpExchange request) throws IOException {
//	
//}
    
//  private void addRelationship(HttpExchange request) throws IOException {
//	
//}
    
//  private void getMovie(HttpExchange request) throws IOException {
//	
//}
    
//  private void getActor(HttpExchange request) throws IOException {
//	
//}
    
//  private void hasRelationship(HttpExchange request) throws IOException {
//	
//}
    
//  private void computeBaconNumber(HttpExchange request) throws IOException {
//	
//}

    
}
