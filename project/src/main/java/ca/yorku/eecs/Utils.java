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

import org.neo4j.driver.v1.*;
import org.json.*;
import com.sun.net.httpserver.HttpExchange;

class Utils {       
	
	private static final String NEO4J_URI = "http://localhost:7687";
    private static final String NEO4J_USERNAME = "neo4j";
    private static final String NEO4J_PASSWORD = "12345678";
	
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
    	Driver driver = GraphDatabase.driver(NEO4J_URI , AuthTokens.basic(NEO4J_USERNAME, NEO4J_PASSWORD));
        String requestMethod = request.getRequestMethod();
        String requestPath = request.getRequestURI().getPath();

        try(Session session = driver.session()) {
        	if(requestMethod.equals("PUT")) {
        		if (requestPath.equals("addActor")) {
                    //addActor(request);
                    System.out.println("PUT");
                } else if(requestPath.equals("addMovie")) {
                	System.out.println("PUT");
                	addMovie(request, session);
                } else if(requestPath.equals("addRelationship")) {
                	System.out.println("PUT");
                	//addRelationship(request);				
                }
        	}
        	if(requestMethod.equals("GET")) {
        		 if(requestPath.equals("getActor")) {
                 	System.out.println("GET");
                 	//getActor(request);		
                 }
                 else if(requestPath.equals("getMovie")) {
                 	System.out.println("GET");
                 	//getMovie(request);		
                 }
                 else if(requestPath.equals("hasRelationship")) {
                 	System.out.println("GET");
                 	//hasRelationship(request);		
                 }
                 else if(requestPath.equals("computeBaconNumber")) {
                 	System.out.println("GET");
                 	//computeBacon(request);		
                 }
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
    
    @SuppressWarnings("restriction")
	private void sendResponseCode(HttpExchange request, int code) throws IOException {
        String responseMessage = null;
        switch(code) {
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
    
 private void addActor(HttpExchange request, Session session) throws IOException {
    	
    	String createQuery = "CREATE (a:Actor {name:$name, actorId: $actorId }";
    	
    	Map<String, String> queryParam = splitQuery(getBody(request));
    	
    	String name = null, actorId = null;
		name = queryParam.get("name");
		actorId = queryParam.get("actorId");
		
		if (name == null || name.isEmpty() || actorId == null || actorId.isEmpty()) {
			sendResponseCode(request, 400);
		}
		else {
			String checkQuery = "MATCH (a:Actor {actorId: $actorId}) RETURN a";
			StatementResult result = session.run(checkQuery, Values.parameters("actorId", actorId));
			if(result != null) {
				sendResponseCode(request, 400);
			}
			else {
				try {
					session.run(createQuery, Values.parameters("name", name, "actorId", actorId));
					sendResponseCode(request, 200);
				}
				catch(Exception e) {
					sendResponseCode(request, 500);
				}
				
			}
		}
    }
    
    private void addMovie(HttpExchange request, Session session) throws IOException {
    	
    	String createQuery = "CREATE (m:Movie {name:$name, movieId: $movieId }";
    	
    	Map<String, String> queryParam = splitQuery(getBody(request));
    	
    	String name = null, movieId = null;
		name = queryParam.get("name");
		movieId = queryParam.get("movieId");
		
		if (name == null || name.isEmpty() || movieId == null || movieId.isEmpty()) {
			sendResponseCode(request, 400);
		}
		else {
			String checkQuery = "MATCH (m:Movie {movieId: $movieId}) RETURN m";
			StatementResult result = session.run(checkQuery, Values.parameters("movieId", movieId));
			if(result != null) {
				sendResponseCode(request, 400);
			}
			else {
				try {
					session.run(createQuery, Values.parameters("name", name, "movieId", movieId));
					sendResponseCode(request, 200);
				}
				catch(Exception e) {
					sendResponseCode(request, 500);
				}
				
			}
		}
    }
    
    private void addRelationship(HttpExchange request, Session session) throws IOException {
    	
    	String relationQuery = "CREATE (MATCH a:Actor, m:Movie "
    			+ "WHERE a.actorId = '$actorId' AND m.movieId = '$movieId'"
    			+ "CREATE (a)-[r:ACTED_IN]->m"
    			+ "RETURN type(r)";
    	
    	Map<String, String> queryParam = splitQuery(getBody(request));
    	
    	String actorId = null, movieId = null;
		actorId = queryParam.get("actorId");
		movieId = queryParam.get("movieId");
		
		if (actorId == null || actorId.isEmpty() || movieId == null || movieId.isEmpty()) {
			sendResponseCode(request, 400);
		}
		else {
			String checkQuery = "MATCH (a:Actor {actorId:$actorId})-[r:ACTED_IN]->(m:Movie {movieId:$movieId})"
					+ "RETURN COUNT(r)";
			StatementResult result = session.run(checkQuery, Values.parameters("movieId", movieId));
			if(result != null) {
				sendResponseCode(request, 400);
			}
			else {
				try {
					session.run(relationQuery, Values.parameters("name", actorId, "movieId", movieId));
					sendResponseCode(request, 200);
				}
				catch(Exception e) {
					sendResponseCode(request, 500);
				}
				
			}
		}
    }
    
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
