package ca.yorku.eecs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import com.sun.net.httpserver.HttpExchange;

import com.sun.net.httpserver.*;
import org.neo4j.driver.v1.*;
import org.json.*;
import java.net.*;
import java.util.*;
import java.io.*;
import java.lang.*;



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
                    String age = null;
                    try {
                        JSONObject json = new JSONObject(requestBody);
                        name = json.getString("name");
                        actorId = json.getString("actorId");
                        age = json.getString("age");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (name == null || name.isEmpty() || actorId == null || actorId.isEmpty()) {
                        sendResponseCode(exchange, 400);
                    } else {
                        String checkQuery = "MATCH (a:actor {actorId: $actorId}) RETURN a";
                        StatementResult result = session.run(checkQuery, Values.parameters("actorId", actorId));
                        if (result != null && result.hasNext()) {
                            sendResponseCode(exchange, 400);
                        } else {
                            try {
                                createActorNode(name, actorId, age);
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
                    	String checkQuery = "MATCH (m:movie {movieId: $movieId}) RETURN m";
                        StatementResult result = session.run(checkQuery, Values.parameters("movieId", movieId));
                        if (result != null && result.hasNext()) {
                            sendResponseCode(exchange, 400);
                        } else {
                            try {
                                createMovieNode(name, movieId);
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

                        StatementResult result =  movieOrActorExists(actorId, movieId);
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

                            StatementResult result2 = bondExists(actorId, movieId);
                            
                            Record record = result2.single();
                            Value countValue = record.get("count");
                            int count = (countValue != null && !countValue.isNull()) ? countValue.asInt() : 0;

                            System.out.print(count);
                            
                            if (count > 0) {
                                System.out.println(count);
                                sendResponseCode(exchange, 400);
                            } else if (count == 0) {
                                try {
                                    createBond(actorId, movieId);
                                    sendResponseCode(exchange, 200);
                                } catch (Exception e) {
                                    sendResponseCode(exchange, 500);
                                }
                            }
                        }
                    }
                }
            }else if ("GET".equalsIgnoreCase(requestMethod)){
            	if (requestPath.equals("/api/v1/getActorsByAge")) {
            		String query = exchange.getRequestURI().getQuery();
                    Map<String, String> queryParams = splitQuery(query);

                    // Get the age from the query parameters
                    String ageStr = queryParams.get("age");

                    if (ageStr == null || ageStr.isEmpty()) {
                        sendResponseCode(exchange, 400);
                        return;
                    }

                    try {
                        StatementResult result = getActorsByAge(ageStr);

                        if (result.hasNext()) {
                            JSONArray actorsArray = new JSONArray();
                            while (result.hasNext()) {
                                Record record = result.next();
                                actorsArray.put(record.get("actorName").asString());
                            }

                            // Create the JSON object
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("actors", actorsArray);

                            // Convert JSON object to a string
                            String jsonOutput = jsonObject.toString();
                            

                            exchange.sendResponseHeaders(200, jsonOutput.length());
                            OutputStream os = exchange.getResponseBody();
                            os.write(jsonOutput.getBytes());
                            os.close();
                        } else {
                            sendResponseCode(exchange, 404);
                        }
                    } catch (NumberFormatException e) {
                        sendResponseCode(exchange, 400);
                    }
            	}else if (requestPath.equals("/api/v1/getActor")){
                	String query = exchange.getRequestURI().getQuery();
                    Map<String, String> queryParams = splitQuery(query);

                    // Get the actorId from the query parameters
                    String actorId = queryParams.get("actorId");

                    if (actorId == null || actorId.isEmpty()) {
                        sendResponseCode(exchange, 400);
                        return;
                    }else {
                    
            	        try {
            	        	StatementResult check = checkActorExists(actorId);
            	        	
            	        	Record record0 = check.single();
                            Value countValue = record0.get("countActor");
                            int count = countValue.asInt();
            	        	
                            System.out.print(count);
                            
                            if (count > 0) {
                            	StatementResult getname = getActorName( actorId);
                	        	StatementResult result = getActorNode(actorId);
                		        ArrayList<String> movies = new ArrayList<>();
                		        String name = getname.single().get("name").asString();
                		
                		        while (result.hasNext()) {
                		            Record record = result.next();
                		            // Extract the name only if it's not already set
                		            movies.add(record.get("movieId").asString());
                		        }
                		        
                		        
                		        
                		        
                		        Map<String, Object> data = new LinkedHashMap<>();
                		        data.put("actorId", actorId);
                		        data.put("name", name);
                		        data.put("movies", new JSONArray(movies));

                		        JSONObject jsonObject = new JSONObject(data);
                		       
                		        // Convert JSON object to a string
                		        String jsonOutput = jsonObject.toString();
                		        
                		        
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
                    	StatementResult check = checkMovieExists(movieId);
        	        	
        	        	Record record0 = check.single();
                        Value countValue = record0.get("countMovie");
                        int count = countValue.asInt();
        	        	
                        System.out.print(count);
                        
                        if (count > 0) {
                        	
                        	StatementResult result = getMovieNode(movieId);

            		        String movieName = result.single().get("name").asString();

            		        System.out.print(movieName);
                        	
            		        
                            StatementResult result2 = getActorsinMovieNode(movieId);
                            JSONArray actorIds = new JSONArray();
                        
                            Record record;
                            while (result2.hasNext()) {
                                record = result2.next();
                                actorIds.put(record.get("actorId").asString());
                            }
                            
                            Map<String, Object> data = new LinkedHashMap<>();
            		        data.put("movieId", movieId);
            		        data.put("name", movieName);
            		        data.put("actors", actorIds);

            		        JSONObject jsonObject = new JSONObject(data);
            		       
            		        // Convert JSON object to a string
            		        String jsonOutput = jsonObject.toString();
            		        
            		        
                            
            		        
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
                    	
                    	StatementResult check =  movieOrActorExists(actorId, movieId);
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
                    	
                    	
                        StatementResult result = getHasRelationNode(actorId, movieId);
                        
                        

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

                        StatementResult result = getBaconNumber(actorId);

                        if (result.hasNext()) {
                            Record record = result.next();
                            int baconNumber = record.get("baconNumber").asInt();

                            // Create the JSON object
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("baconNumber", baconNumber);

                            // Convert JSON object to a string
                            String jsonOutput = jsonObject.toString();

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
                    

                }else if (requestPath.equals("/api/v1/computeBaconPath")) {
                    String query = exchange.getRequestURI().getQuery();
                    Map<String, String> queryParams = splitQuery(query);

                    // Get the actorId from the query parameters
                    String actorId = queryParams.get("actorId");

                    if (actorId == null || actorId.isEmpty()) {
                        sendResponseCode(exchange, 400);
                        return;
                    }
                    try {
                        StatementResult result = getBaconPath(actorId);

                        if (result != null && result.hasNext()) {
                        	
                            Record record = result.next();
                           
                            List<Object> path = record.get("baconPath").asList();
                            
                            JSONArray jsonArray = new JSONArray();
                            int i = 0;
                            while(i < path.size()) {
                            	jsonArray.put(path.get(i).toString());
                            	i++;
                            }

                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("baconPath", jsonArray);

                            String jsonOutput = jsonObject.toString();

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
    
    //GET COMPUTEBACONPATH QUERIES ///////////////////////////////
    
    public static StatementResult getBaconPath(String actorId) {
        StatementResult result = null;
        String baconPathQuery;

        if (actorId.equals("nm0000102")) {
            // Special case: Kevin Bacon himself
            baconPathQuery = "RETURN ['nm0000102'] AS baconPath";
        } else {
            baconPathQuery = "MATCH (start:actor {actorId: $actorId}), (end:actor {actorId: 'nm0000102'}) "
            		+ "MATCH p=shortestPath((start)-[:ACTED_IN*]-(end))"
            		+ " WITH nodes(p) AS nodes "
            		+ "RETURN [node in nodes | CASE WHEN node.actorId IS NOT NULL THEN node.actorId ELSE node.movieId END] AS baconPath";
        }

        try (Session session = driver.session()) {
            result = session.run(baconPathQuery, Values.parameters("actorId", actorId));
        } 
        return result;
    }

    
    //GET COMPUTEBACONNUMBER QUERIES ///////////////////////////////
    
    public static StatementResult getBaconNumber(String actorId) {
    	StatementResult Result;
    	String baconNumberQuery;
        if (actorId.equals("nm0000102")) {
            // Special case: Kevin Bacon himself
            baconNumberQuery = "RETURN 0 AS baconNumber";
        } else {
            baconNumberQuery = "MATCH (a:actor {actorId: $actorId}), (k:actor {actorId: 'nm0000102'}), " +
                               "p=shortestPath((a)-[:ACTED_IN*]-(k)) " +
                               "RETURN length(p) / 2 AS baconNumber";
        }        
        try (Session session = driver.session()) {
            Result = session.run(baconNumberQuery, Values.parameters("actorId", actorId));
        } 
		return Result;
    }
        
        
    //GET HASRELATIONSHIP QUERIES ///////////////////////////////
    
    public static StatementResult getHasRelationNode(String actorId, String movieId) {
    	StatementResult Result;
        String getHasRelation = "MATCH (a:actor {actorId: $actorId})-[r:ACTED_IN]->(m:movie {movieId: $movieId}) RETURN COUNT(r) AS relationshipCount";
        try (Session session = driver.session()) {
            Result = session.run(getHasRelation, Values.parameters("actorId", actorId, "movieId", movieId));
        } 
		return Result;
    }
    

    //GET MOVIE QUERIES ///////////////////////////////
    
    public static StatementResult getMovieNode(String movieId) {
    	StatementResult Result;
        String getMovieQuery = "MATCH (m:movie {movieId: $movieId}) RETURN m.name AS name";
        try (Session session = driver.session()) {
            Result = session.run(getMovieQuery, Values.parameters("movieId", movieId));
        } 
		return Result;
    }
    
    public static StatementResult getActorsinMovieNode(String movieId) {
    	StatementResult Result;
        String getMovieQuery = "MATCH (a:actor)-[:ACTED_IN]->(m:movie {movieId: $movieId}) RETURN a.actorId AS actorId";
        try (Session session = driver.session()) {
            Result = session.run(getMovieQuery, Values.parameters("movieId", movieId));
        } 
		return Result;
    }
    
    public static StatementResult checkMovieExists(String movieId) {
    	StatementResult Result;
    	String checkQuery = "MATCH (a:movie {movieId: $movieId}) RETURN COUNT(a) AS countMovie";
        try (Session session = driver.session()) {
            Result = session.run(checkQuery, Values.parameters("movieId", movieId));
        } 
		return Result;
    }

    //GET ACTOR QUERIES ///////////////////////////////

    public static StatementResult getActorNode(String actorId) {
    	StatementResult Result;
        String getActorQuery = "MATCH (a:actor {actorId: $actorId})-[r:ACTED_IN]->(m:movie) RETURN a.name AS name, m.movieId AS movieId";
        try (Session session = driver.session()) {
            Result = session.run(getActorQuery, Values.parameters("actorId", actorId));
        } 
		return Result;
    }
    
    public static StatementResult getActorName(String actorId) {
    	StatementResult Result;
    	String checkQuery = "MATCH (a:actor {actorId: $actorId}) RETURN a.name AS name";
        try (Session session = driver.session()) {
            Result = session.run(checkQuery, Values.parameters("actorId", actorId));
        } 
		return Result;
    }
    
    public static StatementResult checkActorExists(String actorId) {
    	StatementResult Result;
    	String checkQuery = "MATCH (a:actor {actorId: $actorId}) RETURN COUNT(a) AS countActor";
        try (Session session = driver.session()) {
            Result = session.run(checkQuery, Values.parameters("actorId", actorId));
        } 
		return Result;
    }


    //PUT ADD ACTOR QUERIES ///////////////////////////////
    
    public static StatementResult getActorsByAge(String ageStr) {
    	 
        String query = "MATCH (a:actor {age: $age}) RETURN a.name AS actorName";

        try (Session session = driver.session()) {
            return session.run(query, Values.parameters("age", ageStr));
        }
    }

    public static void createActorNode(String name, String actorId, String age) {
        String createQuery = "CREATE (a:actor {name: $name, actorId: $actorId, age: $age})";
        try (Session session = driver.session()) {
            session.run(createQuery, Values.parameters("name", name, "actorId", actorId, "age", age));
        } 
    }
    
    //PUT ADD MOVIE QUERIES
    
    public static void createMovieNode(String name, String movieId) {
        String createQuery = "CREATE (m:movie {name: $name, movieId: $movieId})";

        try (Session session = driver.session()) {
            session.run(createQuery, Values.parameters("name", name, "movieId", movieId));

        }
    }
    
    
    //PUT ADD RELATIONSHIP QUERIES ///////////////////////////////

    
    public static void createBond(String actorId, String movieId) {
    	String createQuery = "MATCH (a:actor), (m:movie) "
                + "WHERE a.actorId = $actorId AND m.movieId = $movieId "
                + "CREATE (a)-[r:ACTED_IN]->(m) "
                + "RETURN type(r)";
    	try (Session session = driver.session()) {
            session.run(createQuery, Values.parameters("actorId", actorId, "movieId", movieId));

        }
        
    }

    public static StatementResult movieOrActorExists(String actorId, String movieId) {
    	StatementResult Result;
        String ifExistsQuery = "MATCH (a:actor {actorId: $actorId}), (m:movie {movieId: $movieId}) RETURN COUNT(a) AS countActor, COUNT(m) AS countMovie";
    	try (Session session = driver.session()) {
        	Result = session.run(ifExistsQuery, Values.parameters("actorId", actorId, "movieId", movieId));

        }
    	
		return Result;
    }
    
    public static StatementResult bondExists(String actorId, String movieId) {
    	StatementResult Result;
        String checkQuery = "MATCH (a:actor {actorId: $actorId})-[:ACTED_IN]->(m:movie {movieId: $movieId}) RETURN COUNT(*) AS count";
    	try (Session session = driver.session()) {
        	Result = session.run(checkQuery, Values.parameters("actorId", actorId, "movieId", movieId));

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
    
    public static void sendGETResponseCode(HttpExchange request, int code, String responseMessage) throws IOException {
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
