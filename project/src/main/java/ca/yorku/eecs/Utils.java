package ca.yorku.eecs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
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
import org.neo4j.driver.internal.shaded.io.netty.handler.codec.Headers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import com.sun.net.httpserver.HttpExchange;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;

class Utils {   
		
    public void addActor(HttpExchange request, Session session) throws IOException {
        String addActorQuery = "CREATE (a:Actor {name: $name, actorId: $actorId})";

        try (InputStream inputStream = request.getRequestBody()) {
            // Parse the JSON data from the request body
            JSONObject jsonObject = new JSONObject(convert(inputStream));

            String name = jsonObject.optString("name");
            String actorId = jsonObject.optString("actorId");

            if (name == null || name.isEmpty() || actorId == null || actorId.isEmpty()) {
                sendResponseCode(request, 400);
            } else {
            	String checkQuery = "MATCH (a:Actor {actorId: $actorId}) RETURN a";
    			Result result = session.run(checkQuery, Values.parameters("actorId", actorId));
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

    

    public void addMovie(HttpExchange request, Session session) throws IOException {
        String addMovieQuery = "CREATE (m:Movie {name: $name, movieId: $movieId})";

        try (InputStream inputStream = request.getRequestBody()) {
            // Parse the JSON data from the request body
            JSONObject jsonObject = new JSONObject(convert(inputStream));

            String name = jsonObject.optString("name");
            String movieId = jsonObject.optString("movieId");

            if (name == null || name.isEmpty() || movieId == null || movieId.isEmpty()) {
                sendResponseCode(request, 400);
            } else {
            	String checkQuery = "MATCH (m:Movie {movieId: $movieId}) RETURN m";
    			Result result = session.run(checkQuery, Values.parameters("movieId", movieId));
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
    
    public void addRelationship(HttpExchange request, Session session) throws IOException {
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
                String actorExistsQuery = "MATCH (a:Actor {actorId: $actorId}) RETURN COUNT(a) AS countActor";
                String movieExistsQuery = "MATCH (m:Movie {movieId: $movieId}) RETURN COUNT(m) AS countMovie";

                Result actorResult = session.run(actorExistsQuery, Values.parameters("actorId", actorId));
                Record actorRecord = actorResult.single();
                int actorCount = actorRecord.get("countActor").asInt();

                Result movieResult = session.run(movieExistsQuery, Values.parameters("movieId", movieId));
                Record movieRecord = movieResult.single();
                int movieCount = movieRecord.get("countMovie").asInt();

                if (actorCount == 0 || movieCount == 0) {
                    sendResponseCode(request, 404);
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
                    } else if (count == 0) {
                        try {
                            session.run(relationQuery, Values.parameters("actorId", actorId, "movieId", movieId));
                            sendResponseCode(request, 200);
                        } catch (Exception e) {
                            sendResponseCode(request, 500);
                        }
                    }
                }
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
    }



    

    

     

    public void getActor(HttpExchange request, Session session) throws IOException, JSONException {
        String getActorQuery = "MATCH (a:Actor {actorId: $actorId})-[r:ACTED_IN]->(m:Movie) RETURN a.name AS name, m.movieId AS movieId";
        
        // Extract the query parameters from the request URI
        String query = request.getRequestURI().getQuery();
        Map<String, String> queryParams = splitQuery(query);

        // Get the actorId from the query parameters
        String actorId = queryParams.get("actorId");

        if (actorId == null || actorId.isEmpty()) {
            sendResponseCode(request, 400);
            return;
        }else {
        
		        
	        try {
	        	String checkQuery = "MATCH (a:Actor {actorId: $actorId})\r\n"
	        			+ "RETURN a.name AS name";
	        	Result getname = session.run(checkQuery, Values.parameters("actorId", actorId));
	        	Result result = session.run(getActorQuery, Values.parameters("actorId", actorId));
		        ArrayList<String> movies = new ArrayList<>();
		        String name = getname.single().get("name").asString();
		
		        while (result.hasNext()) {
		            Record record = result.next();
		            // Extract the name only if it's not already set
		            movies.add(record.get("movieId").asString());
		        }
		        
		        // Create the JSON object
		        JSONObject jsonObject = new JSONObject();
		        jsonObject.put("actorId", actorId);
		        jsonObject.put("name", name);
		        jsonObject.put("movies", new JSONArray(movies));
		       
		        // Convert JSON object to a string
		        String jsonOutput = jsonObject.toString();
		        jsonOutput += "\n200 OK";
		        System.out.print(jsonOutput);
		        
		        request.sendResponseHeaders(200, jsonOutput.length());
		        OutputStream os = request.getResponseBody();
		        os.write(jsonOutput.getBytes());
		        os.close();
            } catch (Exception e) {
                sendResponseCode(request, 500);
            }
        }
        
         
    }



    public void getMovie(HttpExchange request, Session session) throws IOException {
        String getMovieQuery = "MATCH (m:Movie {movieId: $movieId}) RETURN m.name AS name";

        // Extract the query parameters from the request URI
        String query = request.getRequestURI().getQuery();
        Map<String, String> queryParams = splitQuery(query);

        // Get the movieId from the query parameters
        String movieId = queryParams.get("movieId");

        if (movieId == null || movieId.isEmpty()) {
            sendResponseCode(request, 400);
            return;
        }

        try {
            Result result = session.run(getMovieQuery, Values.parameters("movieId", movieId));

            if (result.hasNext()) {
                Record record = result.next();
                String movieName = record.get("name").asString();

                // Now, query the actors related to the movie
                String getActorsQuery = "MATCH (a:Actor)-[:ACTED_IN]->(m:Movie {movieId: $movieId}) RETURN a.actorId AS actorId";
                result = session.run(getActorsQuery, Values.parameters("movieId", movieId));
                JSONArray actorIds = new JSONArray();

                while (result.hasNext()) {
                    record = result.next();
                    // Extract the actor's ID from the record and add it to the actorIds array
                    actorIds.put(record.get("actorId").asString());
                }

                // Create the JSON object
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("movieId", movieId);
                jsonObject.put("name", movieName);
                jsonObject.put("actors", actorIds);

                // Convert JSON object to a string
                String jsonOutput = jsonObject.toString();
		        jsonOutput += "\n200 OK";
		        System.out.print(jsonOutput);
		        
                request.sendResponseHeaders(200, jsonOutput.length());
                OutputStream os = request.getResponseBody();
                os.write(jsonOutput.getBytes());
                os.close();
            } else {
                sendResponseCode(request, 404); // Movie not found
            }
        } catch (Exception e) {
            sendResponseCode(request, 500);
        }
    }
    
    public void hasRelationship(HttpExchange request, Session session) throws IOException {
        // Extract the query parameters from the request URI
        String query = request.getRequestURI().getQuery();
        Map<String, String> queryParams = splitQuery(query);

        // Get the actorId and movieId from the query parameters
        String actorId = queryParams.get("actorId");
        String movieId = queryParams.get("movieId");

        if (actorId == null || actorId.isEmpty() || movieId == null || movieId.isEmpty()) {
            sendResponseCode(request, 400);
            return;
        }

        try {
            String checkQuery = "MATCH (a:Actor {actorId: $actorId})-[r:ACTED_IN]->(m:Movie {movieId: $movieId}) RETURN COUNT(r) AS relationshipCount";
            Result result = session.run(checkQuery, Values.parameters("actorId", actorId, "movieId", movieId));

            if (result.hasNext()) {
                Record record = result.next();
                int relationshipCount = record.get("relationshipCount").asInt();

                // Create the JSON object
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("actorId", actorId);
                jsonObject.put("movieId", movieId);
                jsonObject.put("hasRelationship", relationshipCount > 0);

                // Convert JSON object to a string
                String jsonOutput = jsonObject.toString();
		        jsonOutput += "\n200 OK";
		        System.out.print(jsonOutput);

                request.sendResponseHeaders(200, jsonOutput.length());
                OutputStream os = request.getResponseBody();
                os.write(jsonOutput.getBytes());
                os.close();
            } else {
                sendResponseCode(request, 404); // Actor or movie not found
            }
        } catch (Exception e) {
            sendResponseCode(request, 500);
        }
    }

    


    



	 
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
	                    getActor(request, session);
	                    System.out.println("GET");
	                } else if (requestPath.equals("/api/v1/getMovie")) {
	                    System.out.println("GET");
	                    getMovie(request, session);
	                } else if (requestPath.equals("/api/v1/hasRelationship")) {
	                    System.out.println("GET");
	                    hasRelationship(request, session);
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
                responseMessage = "200 OK";
                break;
            case 400:
                responseMessage = "400 BAD REQUEST";
                break;
            case 404:
                responseMessage = "404 DOES NOT EXIST";
                break;
            case 500:
                responseMessage = "500 INTERNAL SERVER ERROR";
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
