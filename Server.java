import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.Runnable;

public class Server {

    final static ConcurrentHashMap<String, Integer> shared = new ConcurrentHashMap<>();
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
    private static final ConcurrentHashMap<String, Integer> shared = Server.shared;

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
                // out.println("Server received: " + inputLine);
                String[] parts = inputLine.split("\\s+");
                if(parts[0].equals("SET")){
                    String key = parts[1];
                    Integer value = Integer.parseInt(parts[2]);
                    shared.put(key, value);
                    out.println("OK");
                }else if(parts[0].equals("GET")){
                    String key = parts[1];
                    if(shared.containsKey(key)){
                        out.println(shared.get(key));
                    }else{
                        out.println("NULL");
                    }
                }else if(parts[0].equals("INCR")){
                    String key = parts[1];
                    if(shared.containsKey(key)){
                        int val = shared.get(key);
                        shared.put(key,val+1);
                    }else{
                        out.println("NULL");
                    }
                }else{
                    System.out.println("Unknown command: " + inputLine);
                }

            }
            clientSocket.close();
        }catch(IOException e){
            System.err.println("IOException in ClientHandler: " + e.getMessage());
        }
    }
}