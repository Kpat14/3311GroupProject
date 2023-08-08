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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.*;

import com.sun.net.httpserver.HttpExchange;

public class Utils {
    private static Driver driver;

    public Utils(String uri, String user, String password) {
        String uriDb = "bolt://localhost:7687";
        Config config = Config.builder().withoutEncryption().build();
        driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j", "12345678"), config);
    }

    public void addPerson(String name) {
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> tx.run("MERGE (a:Person {name: $x})", parameters("x", name)));
        }
    }

    public static void close() {
        driver.close();
    }
    
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
    			StatementResult result = session.run(checkQuery, Values.parameters("actorId", actorId));
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
    
    static class AddPersonHandler implements HttpHandler {
        private Utils utils;

        public AddPersonHandler(Utils utils) {
            this.utils = utils;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("PUT".equals(exchange.getRequestMethod())) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
                    String requestBody = reader.lines().collect(Collectors.joining());
                    String name = requestBody.trim();

                    utils.addPerson(name);

                    String response = "Person added: " + name;
                    exchange.sendResponseHeaders(200, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                }
            } else {
                exchange.sendResponseHeaders(405, 0); // Method not allowed
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
