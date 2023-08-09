package ca.yorku.eecs;


import org.neo4j.driver.v1.*;

import static org.neo4j.driver.v1.Values.parameters;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.stream.Collectors;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utils{
    private static Driver driver;
    
    public void handle(HttpExchange exchange) throws IOException {
        String uriDb = "bolt://localhost:7687";
        Config config = Config.builder().withoutEncryption().build();
        driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j", "12345678"), config);
        String requestMethod = exchange.getRequestMethod();
        String requestPath = exchange.getRequestURI().getPath();

        try (Session session = driver.session()) {
            if ("PUT".equalsIgnoreCase(requestMethod)) {
                if (requestPath.equals("/api/v1/addActor")) {
                    String requestBody = Utils.getBody(exchange);

                    String name = null;
                    String actorId = null;
                    try {
                        JSONObject json = new JSONObject(requestBody);
                        name = json.getString("name");
                        actorId = json.getString("actorId");
                    } catch (Exception e) {
                    	sendResponseCode(exchange, 400);
                    }

                    if (name == null || name.isEmpty() || actorId == null || actorId.isEmpty()) {
                        sendResponseCode(exchange, 400);
                    } else {
                        String checkQuery = "MATCH (a:Actor {actorId: $actorId}) RETURN a";
                        StatementResult result = session.run(checkQuery, Values.parameters("actorId", actorId));
                        if (result != null && result.hasNext()) {
                            sendResponseCode(exchange, 400);
                        } else {
                            try {
                                createActorNode(name, actorId,exchange);
                                sendResponseCode(exchange, 200);
                            } catch (Exception e) {
                                sendResponseCode(exchange, 500);
                            }
                        }
                    }
                } else if (requestPath.equals("/api/v1/addMovie")) {
                    String requestBody = Utils.getBody(exchange);

                    String name = null;
                    String movieId = null;
                    try {
                        JSONObject json = new JSONObject(requestBody);
                        name = json.getString("name");
                        movieId = json.getString("movieId");
                    } catch (Exception e) {
                    	sendResponseCode(exchange, 400);
                    }

                    if (name == null || name.isEmpty() || movieId == null || movieId.isEmpty()) {
                        sendResponseCode(exchange, 400);
                    } else {
                    	String checkQuery = "MATCH (m:Movie {movieId: $movieId}) RETURN m";
                        StatementResult result = session.run(checkQuery, Values.parameters("movieId", movieId));
                        if (result != null && result.hasNext()) {
                            sendResponseCode(exchange, 400);
                        } else {
                            try {
                                createMovieNode(name, movieId,exchange);
                                sendResponseCode(exchange, 200);
                            } catch (Exception e) {
                                sendResponseCode(exchange, 500);
                            }
                        }
                    }
                } else if (requestPath.equals("/api/v1/addRelationship")) {
                    String requestBody = Utils.getBody(exchange);

                    String actorId = null;
                    String movieId = null;
                    try {
                        JSONObject json = new JSONObject(requestBody);
                        actorId = json.getString("actorId");
                        movieId = json.getString("movieId");
                    } catch (Exception e) {
                    	sendResponseCode(exchange, 400);
                    }

                    if (actorId == null || actorId.isEmpty() || movieId == null || movieId.isEmpty()) {
                        sendResponseCode(exchange, 400);
                    } else {

                        StatementResult result =  movieOrActorExists(actorId, movieId,exchange);
                        int countActor = -1;
                        int countMovie = -1;

                        if (result.hasNext()) {
                            Record record = result.next();
                            countActor = record.get("countActor").asInt();
                            countMovie = record.get("countMovie").asInt();

                            System.out.println("Count of Actors: " + countActor);
                            System.out.println("Count of Movies: " + countMovie);
                        }
                        
                        if (countActor == 0 || countMovie == 0) {
                            sendResponseCode(exchange, 404);
                        } else {

                            StatementResult result2 = bondExists(actorId, movieId,exchange);
                            
                            Record record = result2.single();
                            Value countValue = record.get("count");
                            int count = (countValue != null && !countValue.isNull()) ? countValue.asInt() : 0;

                            System.out.print(count);
                            
                            if (count > 0) {
                                System.out.println(count);
                                sendResponseCode(exchange, 400);
                            } else if (count == 0) {
                                try {
                                    createBond(actorId, movieId,exchange);
                                    sendResponseCode(exchange, 200);
                                } catch (Exception e) {
                                    sendResponseCode(exchange, 500);
                                }
                            }
                        }
                    }
                }
            }else if ("GET".equalsIgnoreCase(requestMethod)){
            	if (requestPath.equals("/api/v1/getActor")){
                	String query = exchange.getRequestURI().getQuery();
                    Map<String, String> queryParams = splitQuery(query);

                    // Get the actorId from the query parameters
                    String actorId = queryParams.get("actorId");

                    if (actorId == null || actorId.isEmpty()) {
                        sendResponseCode(exchange, 400);
                        return;
                    }else {
                    
            	        try {
            	        	StatementResult check = checkActorExists(actorId,exchange);
            	        	
            	        	Record record0 = check.single();
                            Value countValue = record0.get("countActor");
                            int count = countValue.asInt();
            	        	
                            System.out.print(count);
                            
                            if (count > 0) {
                            	StatementResult getname = getActorName( actorId,exchange);
                	        	StatementResult result = getActorNode(actorId,exchange);
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
                		        
                		        exchange.sendResponseHeaders(200, jsonOutput.length());
                		        OutputStream os = exchange.getResponseBody();
                		        os.write(jsonOutput.getBytes());
                		        os.close();
                		        
                            } else if (count == 0) {
                                sendResponseCode(exchange, 404);
                            }

            	        	
                        } catch (Exception e) {
                            sendResponseCode(exchange, 500);
                        }
                    }
            	}else if (requestPath.equals("/api/v1/getMovie")){
            		String query = exchange.getRequestURI().getQuery();
                    Map<String, String> queryParams = splitQuery(query);

                    // Get the movieId from the query parameters
                    String movieId = queryParams.get("movieId");

                    if (movieId == null || movieId.isEmpty()) {
                        sendResponseCode(exchange, 400);
                        return;
                    }
                    
                    
                    try {
                    	StatementResult check = checkMovieExists(movieId,exchange);
        	        	
        	        	Record record0 = check.single();
                        Value countValue = record0.get("countMovie");
                        int count = countValue.asInt();
        	        	
                        System.out.print(count);
                        
                        if (count > 0) {
                        	
                        	StatementResult result = getMovieNode(movieId,exchange);

            		        String movieName = result.single().get("name").asString();

            		        System.out.print(movieName);
                        	
            		        
                            StatementResult result2 = getActorsinMovieNode(movieId,exchange);
                            JSONArray actorIds = new JSONArray();
                        
                            Record record;
                            while (result2.hasNext()) {
                                record = result2.next();
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
            		        
                            exchange.sendResponseHeaders(200, jsonOutput.length());
                            OutputStream os = exchange.getResponseBody();
                            os.write(jsonOutput.getBytes());
                            os.close();
                        }else if (count == 0) {
                            sendResponseCode(exchange, 404);
                        }
                    } catch (Exception e) {
                        sendResponseCode(exchange, 500);
                    }

            		
            	}else if (requestPath.equals("/api/v1/hasRelationship")) {
            		String query = exchange.getRequestURI().getQuery();
                    Map<String, String> queryParams = splitQuery(query);

                    // Get the actorId and movieId from the query parameters
                    String actorId = queryParams.get("actorId");
                    String movieId = queryParams.get("movieId");

                    if (actorId == null || actorId.isEmpty() || movieId == null || movieId.isEmpty()) {
                        sendResponseCode(exchange, 400);
                        return;
                    }
                    
                    try {
                    	
                    	StatementResult check =  movieOrActorExists(actorId, movieId,exchange);
                        int countActor = -1;
                        int countMovie = -1;

                        if (check.hasNext()) {
                            Record record = check.next();
                            countActor = record.get("countActor").asInt();
                            countMovie = record.get("countMovie").asInt();

                            System.out.println("Count of Actors: " + countActor);
                            System.out.println("Count of Movies: " + countMovie);
                        }
                        
                        if (countActor == 0 || countMovie == 0) {
                            sendResponseCode(exchange, 404);
                        }
                    	
                    	
                        StatementResult result = getHasRelationNode(actorId, movieId,exchange);
                        
                        

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

                            exchange.sendResponseHeaders(200, jsonOutput.length());
                            OutputStream os = exchange.getResponseBody();
                            os.write(jsonOutput.getBytes());
                            os.close();
                        } 
                    } catch (Exception e) {
                        sendResponseCode(exchange, 500);
                    }
                    
                } else if (requestPath.equals("/api/v1/computeBaconNumber")) {
                	String query = exchange.getRequestURI().getQuery();
                    Map<String, String> queryParams = splitQuery(query);

                    // Get the actorId from the query parameters
                    String actorId = queryParams.get("actorId");

                    if (actorId == null || actorId.isEmpty()) {
                        sendResponseCode(exchange, 400);
                        return;
                    }
                    try {

                        StatementResult result = getBaconNumber(actorId, exchange);

                        if (result.hasNext()) {
                            Record record = result.next();
                            int baconNumber = record.get("baconNumber").asInt();

                            // Create the JSON object
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("baconNumber", baconNumber);

                            // Convert JSON object to a string
                            String jsonOutput = jsonObject.toString();
                            jsonOutput += "\n200 OK";
                            System.out.print(jsonOutput);

                            exchange.sendResponseHeaders(200, jsonOutput.length());
                            OutputStream os = exchange.getResponseBody();
                            os.write(jsonOutput.getBytes());
                            os.close();
                        } else {
                            sendResponseCode(exchange, 404);
                        }
                    } catch (Exception e) {
                        sendResponseCode(exchange, 500);
                    }
                    

                }
            }
        } catch (Exception e) {
        	sendResponseCode(exchange, 400);
            System.out.println("Server error");
        }

        OutputStream outputStream = exchange.getResponseBody();
        outputStream.close();
    }
    
    //GET COMPUTEBACONNUMBER QUERIES ///////////////////////////////
    
    public static StatementResult getBaconNumber(String actorId, HttpExchange exchange) throws IOException {
    	StatementResult Result = null;
    	String baconNumberQuery;
        if (actorId.equals("nm0000102")) {
            // Special case: Kevin Bacon himself
            baconNumberQuery = "RETURN 0 AS baconNumber";
        } else {
            baconNumberQuery = "MATCH (a:Actor {actorId: $actorId}), (k:Actor {actorId: 'nm0000102'}), " +
                               "p=shortestPath((a)-[:ACTED_IN*]-(k)) " +
                               "RETURN length(p) / 2 AS baconNumber";
        }        
        try (Session session = driver.session()) {
            Result = session.run(baconNumberQuery, Values.parameters("actorId", actorId));
        }catch(Exception e) {
        	sendResponseCode(exchange, 500);
        }
		return Result;
    }
        
        
    //GET HASRELATIONSHIP QUERIES ///////////////////////////////
    
    public static StatementResult getHasRelationNode(String actorId, String movieId, HttpExchange exchange) throws IOException {
    	StatementResult Result = null;
        String getHasRelation = "MATCH (a:Actor {actorId: $actorId})-[r:ACTED_IN]->(m:Movie {movieId: $movieId}) RETURN COUNT(r) AS relationshipCount";
        try (Session session = driver.session()) {
            Result = session.run(getHasRelation, Values.parameters("actorId", actorId, "movieId", movieId));
        }catch(Exception e) {
        	sendResponseCode(exchange, 500);
        }
		return Result;
    }
    

    //GET MOVIE QUERIES ///////////////////////////////
    
    public static StatementResult getMovieNode(String movieId, HttpExchange exchange) throws IOException {
    	StatementResult Result = null;
        String getMovieQuery = "MATCH (m:Movie {movieId: $movieId}) RETURN m.name AS name";
        try (Session session = driver.session()) {
            Result = session.run(getMovieQuery, Values.parameters("movieId", movieId));
        }catch(Exception e) {
        	sendResponseCode(exchange, 500);
        }
		return Result;
    }
    
    public static StatementResult getActorsinMovieNode(String movieId, HttpExchange exchange) throws IOException {
    	StatementResult Result = null;
        String getMovieQuery = "MATCH (a:Actor)-[:ACTED_IN]->(m:Movie {movieId: $movieId}) RETURN a.actorId AS actorId";
        try (Session session = driver.session()) {
            Result = session.run(getMovieQuery, Values.parameters("movieId", movieId));
        }catch(Exception e) {
        	sendResponseCode(exchange, 500);
        } 
		return Result;
    }
    
    public static StatementResult checkMovieExists(String movieId, HttpExchange exchange) throws IOException {
    	StatementResult Result = null;
    	String checkQuery = "MATCH (a:Movie {movieId: $movieId}) RETURN COUNT(a) AS countMovie";
        try (Session session = driver.session()) {
            Result = session.run(checkQuery, Values.parameters("movieId", movieId));
        }catch(Exception e) {
        	sendResponseCode(exchange, 500);
        } 
		return Result;
    }

    //GET ACTOR QUERIES ///////////////////////////////

    public static StatementResult getActorNode(String actorId, HttpExchange exchange) throws IOException {
    	StatementResult Result = null;
        String getActorQuery = "MATCH (a:Actor {actorId: $actorId})-[r:ACTED_IN]->(m:Movie) RETURN a.name AS name, m.movieId AS movieId";
        try (Session session = driver.session()) {
            Result = session.run(getActorQuery, Values.parameters("actorId", actorId));
        }catch(Exception e) {
        	sendResponseCode(exchange, 500);
        } 
		return Result;
    }
    
    public static StatementResult getActorName(String actorId, HttpExchange exchange) throws IOException {
    	StatementResult Result = null;
    	String checkQuery = "MATCH (a:Actor {actorId: $actorId}) RETURN a.name AS name";
        try (Session session = driver.session()) {
            Result = session.run(checkQuery, Values.parameters("actorId", actorId));
        }catch(Exception e) {
        	sendResponseCode(exchange, 500);
        } 
		return Result;
    }
    
    public static StatementResult checkActorExists(String actorId, HttpExchange exchange) throws IOException {
    	StatementResult Result = null;
    	String checkQuery = "MATCH (a:Actor {actorId: $actorId}) RETURN COUNT(a) AS countActor";
        try (Session session = driver.session()) {
            Result = session.run(checkQuery, Values.parameters("actorId", actorId));
        }catch(Exception e) {
        	sendResponseCode(exchange, 500);
        }
		return Result;
    }


    //PUT ADD ACTOR QUERIES ///////////////////////////////

    public static void createActorNode(String name, String actorId, HttpExchange exchange) throws IOException {
        String createQuery = "CREATE (a:Actor {name: $name, actorId: $actorId})";
        try (Session session = driver.session()) {
            session.run(createQuery, Values.parameters("name", name, "actorId", actorId));
        }catch(Exception e) {
        	sendResponseCode(exchange, 500);
        } 
    }
    
    //PUT ADD MOVIE QUERIES
    
    public static void createMovieNode(String name, String movieId, HttpExchange exchange) throws IOException {
        String createQuery = "CREATE (m:Movie {name: $name, movieId: $movieId})";

        try (Session session = driver.session()) {
            session.run(createQuery, Values.parameters("name", name, "movieId", movieId));
        }catch(Exception e) {
        	sendResponseCode(exchange, 500);
        }
    }
    
    
    //PUT ADD RELATIONSHIP QUERIES ///////////////////////////////

    
    public static void createBond(String actorId, String movieId, HttpExchange exchange) throws IOException {
    	String createQuery = "MATCH (a:Actor), (m:Movie) "
                + "WHERE a.actorId = $actorId AND m.movieId = $movieId "
                + "CREATE (a)-[r:ACTED_IN]->(m) "
                + "RETURN type(r)";
    	try (Session session = driver.session()) {
            session.run(createQuery, Values.parameters("actorId", actorId, "movieId", movieId));
        }catch(Exception e) {
        	sendResponseCode(exchange, 500);
        }
        
    }

    public static StatementResult movieOrActorExists(String actorId, String movieId, HttpExchange exchange) throws IOException {
    	StatementResult Result = null;
        String ifExistsQuery = "MATCH (a:Actor {actorId: $actorId}), (m:Movie {movieId: $movieId}) RETURN COUNT(a) AS countActor, COUNT(m) AS countMovie";
    	try (Session session = driver.session()) {
        	Result = session.run(ifExistsQuery, Values.parameters("actorId", actorId, "movieId", movieId));

        }catch(Exception e) {
        	sendResponseCode(exchange, 500);
        }
    	
		return Result;
    }
    
    public static StatementResult bondExists(String actorId, String movieId, HttpExchange exchange) throws IOException {
    	StatementResult Result = null;
        String checkQuery = "MATCH (a:Actor {actorId: $actorId})-[:ACTED_IN]->(m:Movie {movieId: $movieId}) RETURN COUNT(*) AS count";
    	try (Session session = driver.session()) {
        	Result = session.run(checkQuery, Values.parameters("actorId", actorId, "movieId", movieId));

        }catch(Exception e) {
        	sendResponseCode(exchange, 500);
        }
    	
		return Result;
    }
        
        
    ///////////////////////////////////////////////   
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
                responseMessage = "404 NOT FOUND";
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





//package ca.yorku.eecs;
//
//
//import org.neo4j.driver.v1.*;
//
//import static org.neo4j.driver.v1.Values.parameters;
//import com.sun.net.httpserver.HttpHandler;
//import com.sun.net.httpserver.HttpExchange;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.util.stream.Collectors;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.UnsupportedEncodingException;
//import java.net.URLDecoder;
//import java.util.ArrayList;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//import org.neo4j.driver.v1.*;
//
//import com.sun.net.httpserver.HttpExchange;
//
//
//class Utils {       
//	 
//    private static Driver driver;
//
//    public Utils(String uri, String user, String password) {
//        String uriDb = "bolt://localhost:7687";
//        Config config = Config.builder().withoutEncryption().build();
//        driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j", "12345678"), config);
//    }
//    
//    public static void close() {
//        driver.close();
//    }
//
//	
//    // use for extracting query params
//    public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
//        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
//        String[] pairs = query.split("&");
//        for (String pair : pairs) {
//            int idx = pair.indexOf("=");
//            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
//        }
//        return query_pairs;
//    }
//
//    // one possible option for extracting JSON body as String
//    public static String convert(InputStream inputStream) throws IOException {
//                
//        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
//            return br.lines().collect(Collectors.joining(System.lineSeparator()));
//        }
//    }
//
//    // another option for extracting JSON body as String
//    public static String getBody(HttpExchange he) throws IOException {
//                InputStreamReader isr = new InputStreamReader(he.getRequestBody(), "utf-8");
//            BufferedReader br = new BufferedReader(isr);
//            
//            int b;
//            StringBuilder buf = new StringBuilder();
//            while ((b = br.read()) != -1) {
//                buf.append((char) b);
//            }
//
//            br.close();
//            isr.close();
//	    
//        return buf.toString();
//        }
//    
//    
//    
//    static class AddPersonHandler implements HttpHandler {
//        private Utils utils;
//
//        public AddPersonHandler(Utils utils) {
//            this.utils = utils;
//        }
//
//        public void handle(HttpExchange exchange) throws IOException {
//            if ("PUT".equals(exchange.getRequestMethod())) {
//                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
//                    String requestBody = reader.lines().collect(Collectors.joining());
//                    String request = requestBody.replace('\'',' ').trim();
//                    
//                    utils.addActor(exchange);
//                    String name = null;
//                    String actorId = null;
//                    try {
//                        JSONObject json = new JSONObject(request);
//                        name = json.getString("name");
//                        actorId = json.getString("actorId");
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    String response = "Person added: " + name;
//                    //sendResponseCode(exchange, 200);
//                    exchange.sendResponseHeaders(200, response.length());
//                    try (OutputStream os = exchange.getResponseBody()) {
//                        os.write(response.getBytes());
//                    }
//                }
//            } else {
//                exchange.sendResponseHeaders(405, 0); // Method not allowed
//            }    
//        }
//    }
//    
//	private void sendResponseCode(HttpExchange request, int code) throws IOException {
//        String responseMessage = null;
//        switch(code) {
//        	case 200:
//        		responseMessage = "OK";
//        		break;
//        	case 400:
//        		responseMessage = "BAD REQUEST";
//        		break;
//        	case 500:
//        		responseMessage = "INTERNAL SERVER ERROR";
//        		break;
//        	default:
//        		responseMessage = "NEW ERROR DETECTED";
//        		break;
//        }
//        request.sendResponseHeaders(code, responseMessage.length());
//        OutputStream os = request.getResponseBody();
//        os.write(responseMessage.getBytes());
//        os.close();
//	}
//    
//    public void addActor(HttpExchange exchange) throws IOException{
//		String createQuery = "CREATE (a:Actor {name:$name, actorId: $actorId }";
//		String checkQuery = "MATCH (a:Actor {actorId: $actorId}) RETURN a";
//		try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
//            String requestBody = reader.lines().collect(Collectors.joining());
//            String request = requestBody.replace('\'',' ').trim();
//            
//            String name = null;
//            String actorId = null;
//            try {
//                JSONObject json = new JSONObject(request);
//                name = json.getString("name");
//                actorId = json.getString("actorId");
//            }catch (Exception e) {
//            	sendResponseCode(exchange, 500);
//            	}
//            
//            if (name == null || name.isEmpty() || actorId == null || actorId.isEmpty()) {
//    			sendResponseCode(exchange, 400);
//    		}else {
//    			try(Session session = driver.session()){
//    				Transaction tx = session.beginTransaction();
//    				StatementResult result = tx.run(checkQuery, Values.parameters("actorId", actorId));
//    				if(result != null) {
//    					sendResponseCode(exchange, 400);
//    				}else{
//    					try(Session session1 = driver.session()) {
//    						Transaction tx1 = session.beginTransaction();
//    						tx1.run(createQuery, Values.parameters("actorId", actorId));
//    					}
//    				}
//    			}catch(IOException e){
//    				sendResponseCode(exchange, 500);
//    				}
//    			}
//    		}catch(IOException a) {
//    			sendResponseCode(exchange, 400);
//        }
//		
//    }
//    
//    public void addPerson(String name) {
//        try (Session session = driver.session()) {
//            session.writeTransaction(tx -> tx.run("MERGE (a:Person {name: $x})", parameters("x", name)));
//        }
//    }
//    
//    
//
//
//    
////	public void handle(HttpExchange request) throws IOException {
////		// TODO Auto-generated method stub
////    	Driver driver = GraphDatabase.driver(NEO4J_URI , AuthTokens.basic(NEO4J_USERNAME, NEO4J_PASSWORD));
////        String requestMethod = request.getRequestMethod();
////        String requestPath = request.getRequestURI().getPath();
////        
////        try {
////        	if(requestMethod.equals("PUT")) {
////        		if (requestPath.equals("addActor")) {
////                    //addActor(request, driver);
////                    System.out.println("PUT");
////                } else if(requestPath.equals("addMovie")) {
////                	System.out.println("PUT");
////                	//addMovie(request);
////                } else if(requestPath.equals("addRelationship")) {
////                	System.out.println("PUT");
////                	//addRelationship(request);				
////                }
////        	}
////        
////        	if(requestMethod.equals("GET")) {
////        		 if(requestPath.equals("getActor")) {
////                 	System.out.println("GET");
////                 	//getActor(request);		
////                 }
////                 else if(requestPath.equals("getMovie")) {
////                 	System.out.println("GET");
////                 	//getMovie(request);		
////                 }
////                 else if(requestPath.equals("hasRelationship")) {
////                 	System.out.println("GET");
////                 	//hasRelationship(request);		
////                 }
////                 else if(requestPath.equals("computeBaconNumber")) {
////                 	System.out.println("GET");
////                 	//computeBacon(request);		
////                 }
////        	}
////        	else {
////            	System.out.println("Unrecognized command");
////            	//sendResponse(request, "Unimplemented method\n", 501);
////                
////                }
////            
////        } catch (Exception e) {
////        	e.printStackTrace();
////        	//sendResponse(request, "Server error\n", 500);
////        	sendResponseCode(request, 500);
////        }
////        driver.close();
////		
////	}
//    
//
//    
////    public void addActor(HttpExchange request, Driver driver) throws IOException {
////		String createQuery = "CREATE (a:Actor {name:$name, actorId: $actorId }";
////		String checkQuery = "MATCH (a:Actor {actorId: $actorId}) RETURN a";
////		Map<String, String> queryParam = splitQuery(getBody(request));
////		String name = null, actorId = null;
////		name = queryParam.get("name");
////		actorId = queryParam.get("actorId");
////		if (name == null || name.isEmpty() || actorId == null || actorId.isEmpty()) {
////			sendResponseCode(request, 400);
////		}
////		else {
////			try(Session session = driver.session()){
////				Transaction tx = session.beginTransaction();
////				StatementResult result = tx.run(checkQuery, Values.parameters("actorId", actorId));
////				if(result != null) {
////					sendResponseCode(request, 400);
////				}
////			}catch(IOException e){
////				try(Session session1 = driver.session()){
////					Transaction tx1 = session1.beginTransaction();
////					StatementResult result1 = tx1.run(checkQuery, Values.parameters("actorId", actorId));
////					sendResponseCode(request, 200);
////				}
////				catch(IOException f) {
////					sendResponseCode(request, 500);
////				}	
////			}
////		}
////    }
////    
//// 	private void addActor(HttpExchange request, Session session) throws IOException {
////    	String createQuery = "CREATE (a:Actor {name:$name, actorId: $actorId }";
////    	
////    	Map<String, String> queryParam = splitQuery(getBody(request));
////    	
////    	String name = null, actorId = null;
////		name = queryParam.get("name");
////		actorId = queryParam.get("actorId");
////		
////		if (name == null || name.isEmpty() || actorId == null || actorId.isEmpty()) {
////			sendResponseCode(request, 400);
////		}
////		else {
////			String checkQuery = "MATCH (a:Actor {actorId: $actorId}) RETURN a";
////			StatementResult result = session.run(checkQuery, Values.parameters("actorId", actorId));
////			if(result != null) {
////				sendResponseCode(request, 400);
////			}
////			else {
////				try {
////					session.run(createQuery, Values.parameters("name", name, "actorId", actorId));
////					sendResponseCode(request, 200);
////				}
////				catch(Exception e) {
////					sendResponseCode(request, 500);
////				}
////				
////			}
////		}
////    }
////    
////    private void addMovie(HttpExchange request, Session session) throws IOException {
////    	
////    	String createQuery = "CREATE (m:Movie {name:$name, movieId: $movieId }";
////    	
////    	Map<String, String> queryParam = splitQuery(getBody(request));
////    	
////    	String name = null, movieId = null;
////		name = queryParam.get("name");
////		movieId = queryParam.get("movieId");
////		
////		if (name == null || name.isEmpty() || movieId == null || movieId.isEmpty()) {
////			sendResponseCode(request, 400);
////		}
////		else {
////			String checkQuery = "MATCH (m:Movie {movieId: $movieId}) RETURN m";
////			StatementResult result = session.run(checkQuery, Values.parameters("movieId", movieId));
////			if(result != null) {
////				sendResponseCode(request, 400);
////			}
////			else {
////				try {
////					session.run(createQuery, Values.parameters("name", name, "movieId", movieId));
////					sendResponseCode(request, 200);
////				}
////				catch(Exception e) {
////					sendResponseCode(request, 500);
////				}
////				
////			}
////		}
////    }
////    
////    private void addRelationship(HttpExchange request, Session session) throws IOException {
////    	
////    	String relationQuery = "CREATE (MATCH a:Actor, m:Movie "
////    			+ "WHERE a.actorId = '$actorId' AND m.movieId = '$movieId'"
////    			+ "CREATE (a)-[r:ACTED_IN]->m"
////    			+ "RETURN type(r)";
////    	
////    	Map<String, String> queryParam = splitQuery(getBody(request));
////    	
////    	String actorId = null, movieId = null;
////		actorId = queryParam.get("actorId");
////		movieId = queryParam.get("movieId");
////		
////		if (actorId == null || actorId.isEmpty() || movieId == null || movieId.isEmpty()) {
////			sendResponseCode(request, 400);
////		}
////		else {
////			String checkQuery = "MATCH (a:Actor {actorId:$actorId})-[r:ACTED_IN]->(m:Movie {movieId:$movieId})"
////					+ "RETURN COUNT(r)";
////			StatementResult result = session.run(checkQuery, Values.parameters("movieId", movieId));
////			if(result != null) {
////				sendResponseCode(request, 400);
////			}
////			else {
////				try {
////					session.run(relationQuery, Values.parameters("name", actorId, "movieId", movieId));
////					sendResponseCode(request, 200);
////				}
////				catch(Exception e) {
////					sendResponseCode(request, 500);
////				}
////				
////			}
////		}
////    }
//    
////  private void getMovie(HttpExchange request) throws IOException {
////	
////}
//    
////  private void getActor(HttpExchange request) throws IOException {
////	
////}
//    
////  private void hasRelationship(HttpExchange request) throws IOException {
////	
////}
//    
////  private void computeBaconNumber(HttpExchange request) throws IOException {
////	
////}
//
//    
//}
