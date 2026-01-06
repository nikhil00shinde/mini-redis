import java.io.*;
import java.net.*;

public class Server {
    public static void main(String args[]) throws IOException {
       ServerSocket serverSocket = new ServerSocket(6379);

       
       
       Socket clientSocket = serverSocket.accept();
       BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
       PrintWriter out = new PrintWriter(clientSocket.getOutputStream(),true);

       String message;
       while((message = in.readLine()) != null) {

            System.out.println("Received: " + message);

             out.println("PONG");
             
       }
       
             clientSocket.close();
             serverSocket.close();
    }
}