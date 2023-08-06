package ca.yorku.eecs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

class Utils {   
	
	static int PORT = 8080;
	
    public static void main(String[] args) throws IOException {
        // Create an HTTP server listening on all available network interfaces on the specified port
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);

        server.createContext("/api/v1/addActor", new MyHandler());
        server.createContext("/api/v1/addMovie", new MyHandler());
        server.createContext("/api/v1/addRelationship", new MyHandler());

        // Start the HTTP server
        server.start();

        // Print a message to indicate that the server has started
        System.out.printf("Server started on port %d...\n", PORT);
    }

    public static void addActor(HttpExchange request, Session session) throws IOException {
        String addActorQuery = "CREATE (a:Actor {name: $name, actorId: $actorId})";

        try (InputStream inputStream = request.getRequestBody()) {
            // Parse the JSON data from the request body
            JSONObject jsonObject = new JSONObject(convert(inputStream));

            String name = jsonObject.optString("name");
            String actorId = jsonObject.optString("actorId");

            if (name == null || name.isEmpty() || actorId == null || actorId.isEmpty()) {
                sendResponseCode(request, 400);
            } else {
                String checkQuery = "MATCH (a:Actor {name: $name}) RETURN a";
                Result result = session.run(checkQuery, Values.parameters("name", name));
                if (result != null && result.hasNext()) {
                    sendResponseCode(request, 400);
                } else {
                    try {
                        session.run(addActorQuery, Values.parameters("name", name, "actorId", actorId));
                        sendResponseCode(request, 200);
                    } catch (Exception e) {
                        sendResponseCode(request, 500);
                    }

                }
            }
        } catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    }

    

    public static void addMovie(HttpExchange request, Session session) throws IOException {
        String addMovieQuery = "CREATE (m:Movie {name: $name, movieId: $movieId})";

        try (InputStream inputStream = request.getRequestBody()) {
            // Parse the JSON data from the request body
            JSONObject jsonObject = new JSONObject(convert(inputStream));

            String name = jsonObject.optString("name");
            String movieId = jsonObject.optString("movieId");

            if (name == null || name.isEmpty() || movieId == null || movieId.isEmpty()) {
                sendResponseCode(request, 400);
            } else {
                String checkQuery = "MATCH (m:Movie {name: $name}) RETURN m";
                Result result = session.run(checkQuery, Values.parameters("name", name));
                if (result != null && result.hasNext()) {
                    sendResponseCode(request, 400);
                } else {
                    try {
                        session.run(addMovieQuery, Values.parameters("name", name, "movieId", movieId));
                        sendResponseCode(request, 200);
                    } catch (Exception e) {
                        sendResponseCode(request, 500);
                    }
                }
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
    }
    
    public static void addRelationship(HttpExchange request, Session session) throws IOException {
        String relationQuery = "MATCH (a:Actor), (m:Movie) "
                + "WHERE a.actorId = $actorId AND m.movieId = $movieId "
                + "CREATE (a)-[r:ACTED_IN]->(m) "
                + "RETURN type(r)";

        try (InputStream inputStream = request.getRequestBody()) {
            JSONObject jsonObject = new JSONObject(convert(inputStream));
            String actorId = jsonObject.optString("actorId");
            String movieId = jsonObject.optString("movieId");

            if (actorId == null || actorId.isEmpty() || movieId == null || movieId.isEmpty()) {
                sendResponseCode(request, 400);
            } else {
                String checkQuery = "MATCH (a:Actor {actorId: $actorId})-[:ACTED_IN]->(m:Movie {movieId: $movieId}) "
                        + "RETURN COUNT(*) AS count";
                Result result = session.run(checkQuery, Values.parameters("actorId", actorId, "movieId", movieId));
                Record record = result.single();
                Value countValue = record.get("count");
                int count = (countValue != null && !countValue.isNull()) ? countValue.asInt() : 0;
                if (count > 0) {
                	System.out.println(count);
                    sendResponseCode(request, 400);
                } else if (count == 0){
                    try {
                        session.run(relationQuery, Values.parameters("actorId", actorId, "movieId", movieId));
                        sendResponseCode(request, 200);
                    } catch (Exception e) {
                        sendResponseCode(request, 500);
                    }
                }
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
    }







	 
	static class MyHandler implements HttpHandler {
	    public void handle(HttpExchange request) throws IOException {
	        String uri = "bolt://localhost:7687";
	        String username = "neo4j";
	        String password = "12345678";

	        Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
	        String requestMethod = request.getRequestMethod();
	        String requestPath = request.getRequestURI().getPath();

	        try (Session session = driver.session()) {
	            if (requestMethod.equals("PUT")) {
	                if (requestPath.equals("/api/v1/addActor")) {
	                    addActor(request, session);
	                    System.out.println("PUT");
	                } else if (requestPath.equals("/api/v1/addMovie")) {
	                    addMovie(request, session);
	                    System.out.println("PUT");
	                } else if (requestPath.equals("/api/v1/addRelationship")) {
	                    addRelationship(request, session);
	                    System.out.println("PUT");
	                    // addRelationship(request);
	                }
	            } else if (requestMethod.equals("GET")) {
	                if (requestPath.equals("/api/v1/getActor")) {
	                    System.out.println("GET");
	                    // getActor(request);
	                } else if (requestPath.equals("/api/v1/getMovie")) {
	                    System.out.println("GET");
	                    // getMovie(request);
	                } else if (requestPath.equals("/api/v1/hasRelationship")) {
	                    System.out.println("GET");
	                    // hasRelationship(request);
	                } else if (requestPath.equals("/api/v1/computeBaconNumber")) {
	                    System.out.println("GET");
	                    // computeBacon(request);
	                }
	            } else {
	                System.out.println("Unrecognized command");
	                // sendResponse(request, "Unimplemented method\n", 501);
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	            // sendResponse(request, "Server error\n", 500);
	            System.out.println("Server error");
	        }
	    }
	}

	
  
	
	
    // use for extracting query params
    public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) {
            return query_pairs;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0 && idx < pair.length() - 1) {
                query_pairs.put(
                    URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                    URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                );
            }
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
    
    public static void sendResponseCode(HttpExchange request, int code) throws IOException {
        String responseMessage = null;
        switch (code) {
            case 200:
                responseMessage = "OK";
                break;
            case 400:
                responseMessage = "BAD REQUEST";
                break;
            case 500:
                responseMessage = "INTERNAL SERVER ERROR";
                break;
            default:
                responseMessage = "NEW ERROR DETECTED";
                break;
        }
        request.sendResponseHeaders(code, responseMessage.length());
        OutputStream os = request.getResponseBody();
        os.write(responseMessage.getBytes());
        os.close();
    }
}
