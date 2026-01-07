import java.io.*;
import java.net.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.lang.Runnable;

public class Server {

    final static ConcurrentHashMap<String, Integer> shared = new ConcurrentHashMap<>();
    public static void main(String args[]) throws IOException {
       ServerSocket serverSocket = new ServerSocket(6379);
        System.out.println("Server is listening on port 6379");
        // ExecutorService pool = Executors.newFixedThreadPool(8); // hides important details
        ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 2, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100),new ThreadPoolExecutor.AbortPolicy());
       while(true) {
            try{
                Socket clientSocket = serverSocket.accept();
                try{
                    executor.execute(new ClientHandler(clientSocket));
                }catch(RejectedExecutionException e){
                    System.err.println("ERR Queue is Full");
                }
            }catch(Exception e){
                System.out.println("Exception: " + e.getMessage());
            }
        }
        // executor.shutdown();
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
                    Integer val = shared.get(key);
                    if(val == null){
                        out.println("NULL");
                    }else{
                        out.println(val);
                    }
                }else if(parts[0].equals("INCR")){
                    String key = parts[1];
                    int val = shared.merge(key, 1, Integer::sum);
                    out.println(val+" Success");
                    
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