import java.io.*;
import java.net.*;
import java.lang.Runnable;

public class Server {
    public static void main(String args[]) throws IOException {
       ServerSocket serverSocket = new ServerSocket(6379);
        System.out.println("Server is listening on port 6379");

       while(true) {
            try{
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }catch(Exception e){
                        System.out.println("Exception: " + e.getMessage());
                }
            }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;


    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        //Handle the client communication here
        try (
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
        ){
            String inputLine;
            while((inputLine = in.readLine()) != null){
                out.println("Server received: " + inputLine);
            }
            clientSocket.close();
        }catch(IOException e){
            System.err.println("IOException in ClientHandler: " + e.getMessage());
        }
    }
}