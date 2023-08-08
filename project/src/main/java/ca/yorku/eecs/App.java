package ca.yorku.eecs;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

import ca.yorku.eecs.Utils.AddPersonHandler;

public class App 
{
    static int PORT = 8080;
    public static void main(String[] args) throws IOException
    {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        // TODO: two lines of code are expected to be added here
        Utils adder = new Utils("http://localhost:7474/2", "neo4j", "12345678");
        server.createContext("/addPerson", new AddPersonHandler(adder));
        // please refer to the HTML server example 
        server.start();
        System.out.printf("Server started on port %d...\n", PORT);
    }
}
