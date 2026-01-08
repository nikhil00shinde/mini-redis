import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.Runnable;

public class Server {

    final static ConcurrentHashMap<String, Value> shared = new ConcurrentHashMap<>();
    public static void main(String args[]) throws IOException {
       ServerSocket serverSocket = new ServerSocket(6379);
        System.out.println("Server is listening on port 6379");
        // ExecutorService pool = Executors.newFixedThreadPool(8); // hides important details
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            8, 8, 
            2, 
            TimeUnit.SECONDS, 
            new ArrayBlockingQueue<>(100),
            new ThreadPoolExecutor.AbortPolicy());
       while(true) {
           Socket clientSocket = serverSocket.accept();
           try{
                executor.execute(new ClientHandler(clientSocket));
            }catch(RejectedExecutionException rex){
                //Backpressure: tell client and close
                try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)){
                    out.println("ERR server busy");
                } catch(IOException ignored){

                } finally{
                    try {
                        clientSocket.close();
                    }catch(IOException ignored){}
                }
            }
        }
        // executor.shutdown();
    }
}

class Value {
    int val;
    long expireAt; // 0 = no expiry, otherwise epoch millis
    Value(int v, long e){
        this.val = v;
        this.expireAt = e;
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private static final ConcurrentHashMap<String, Value> shared = Server.shared;

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
                inputLine = inputLine.trim();
                if(inputLine.isEmpty()) continue;
                String[] parts = inputLine.split("\\s+");

                String cmd = parts[0].toUpperCase();

                switch (cmd) {
                    case "SET": {
                        if(parts.length != 3) {out.println("ERR wrong number of arguments"); break;}
                        String key = parts[1];
                        int value;
                        try {
                            value = Integer.parseInt(parts[2]);
                        } catch(NumberFormatException nfe){
                            out.println("ERR value is not an integer");
                            break;
                        }

                        shared.put(key, new Value(value,0));
                        out.println("OK");
                        break;
                    }
                    case "GET": {
                        if(parts.length != 2) {
                            out.println("ERR wrong number of arguments");
                            break;
                        }

                        String key = parts[1];
                        Value v = getAlive(key);

                        if(v == null) out.println("NULL");
                        else out.println(v.val);
                        break;
                    }
                    case "INCR": {
                        if (parts.length != 2) {
                            out.println("ERR wrong number of arguments");
                            break;
                        }
                        String key = parts[1];

                        long now = System.currentTimeMillis();
                        Value updated = shared.compute(key, (k, old) -> {
                            if(old == null) return new Value(1,0);

                            //if expired, treat as missing
                            if (old.expireAt != 0 && now >= old.expireAt){
                                return new Value(1, 0);
                            }

                            old.val += 1;
                            return old;
                        });

                        out.println(updated.val);
                        break;
                    }

                    case "EXPIRE": {
                        if (parts.length != 3) {
                            out.println("ERR wrong number of arguments");
                            break;
                        }

                        String key = parts[1];
                        int seconds;
                        try {
                            seconds = Integer.parseInt(parts[2]);
                        }catch (NumberFormatException nfe){
                            out.println("ERR seconds is not an integer");
                            break;
                        }

                        long now = System.currentTimeMillis();
                        final long expireAt = now + (seconds * 1000L);

                        Value updated = shared.compute(key, (k, old) -> {
                            if(old == null) return null;
                            if(old.expireAt != 0 && now >= old.expireAt) return null;
                            old.expireAt = expireAt;
                            return old;
                        });

                        out.println(updated == null ? "0" : "1");
                        break;
                    }

                    case "TTL": {
                        if (parts.length != 2) {
                            out.println("ERR wrong number of arguments");
                            break;
                        }

                        String key = parts[1];

                        long now = System.currentTimeMillis();

                        Value v = shared.get(key);
                        if(v == null) {
                            out.println("-2");
                            break;
                        }

                        if(v.expireAt != 0 && now >= v.expireAt){
                            shared.remove(key, v);
                            out.println("-2");
                            break;
                        }

                        // No expiry
                        if (v.expireAt == 0) {out.println("-1"); break;}

                        long remainingMs = v.expireAt - now;
                        long remainingSec = remainingMs / 1000L;
                        out.println(Long.toString(remainingSec));
                        break;
                    }

                    default:
                        out.println("ERR unknown command");
                }
            }
        }   catch(IOException e){
            // discomnect client
        } finally {
            try {clientSocket.close();} catch(IOException ignored){}
        }
    }

    private Value getAlive(String key) {
        Value v = shared.getOrDefault(key,null);
        if(v == null) return null; 
        
        long now = System.currentTimeMillis();
        if(v.expireAt != 0 && v.expireAt <= now){
            shared.remove(key);
            return null;
        }
        // out.println(val.val);
        return v;
    }
}