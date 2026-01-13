import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;
import java.lang.Runnable;
import java.util.*;

public class Server {

    final static ConcurrentHashMap<String, Value> shared = new ConcurrentHashMap<>();
    static final BlockingQueue<String> aofQueue = new LinkedBlockingQueue<>();
    static volatile boolean running = true;
    
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


            // First read the content from AOF file
            FileWriter writeFile = new FileWriter("appendonly.aof",true);
            readFromAOF();

            Thread aofWriterThread = new Thread(() -> {
                while(running || !aofQueue.isEmpty()) {
                    try {
                        String cmd = aofQueue.poll(100, TimeUnit.MILLISECONDS);
                        if(cmd != null){
                            writeFile.write(cmd + "\n");
                            writeFile.flush();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        try {
                            writeFile.close();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();  
                        }
                        break;
                    }
                }
            });
            aofWriterThread.start();
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

    private static void readFromAOF(){
             try(BufferedReader br = new BufferedReader(new FileReader("appendonly.aof"))){
                String  line;
                while ((line = br.readLine()) != null){
                    if(line.isEmpty()) continue;
                    line  = line.trim();
                    String[] parts = line.split("\\s+");
                    
                    switch (parts[0].toUpperCase()){
                        case "SET": {
                            if(parts.length != 3) return;
                            String key = parts[1];
                            int value;
                            try {
                                value = Integer.parseInt(parts[2]);
                            } catch (NumberFormatException e) {
                                return;
                            }
                            shared.put(key, new Value(value,0));
                            break;
                        }

                        case "INCR": {
                            String key = parts[1];
                            if(parts.length != 2) return;
                            Value value = shared.compute(key, (k, old) -> {
                                if(old == null) return new Value(1,0);
                                old.val += 1;
                                return old;
                            });

                            if(value != null){
                                System.out.println("Updated Value");
                            }
                            break;
                        }

                        case "EXPIRE": {
                            String key = parts[1];
                            if(parts.length != 3) return;
                            long seconds;
                            try{
                                seconds = Long.parseLong(parts[2]);
                            } catch (NumberFormatException e) {
                                return;
                            }
                            long now = System.currentTimeMillis();
                            long nTime  = now + seconds*1000;

                            Value value = shared.compute(key, (k,old) -> {
                                if(old == null) return null;
                                old.expireAt = nTime;
                                return old;
                            });
                            if(value != null){
                                System.out.println("Successfull Added");
                            }
                            break;
                        }
                        default: {
                            return;
                        }
                    }
                }
             }catch (IOException e){
                e.printStackTrace();
             }
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
    private static final BlockingQueue<String> aofQueue = Server.aofQueue;

    @Override
    public void run() {
        //Handle the client communication here
        try (
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            InputStream in = clientSocket.getInputStream();
            BufferedInputStream bin = new BufferedInputStream(in);
            BufferedOutputStream bout = new BufferedOutputStream(clientSocket.getOutputStream());
            
            // OutputStream out = clientSocket.getOutputStream();
        ){
            // System.out.println(in.toString());
            List<String> list = readRespCommand(bin);
            System.out.println("Received command with " + list);
            // String inputLine;
            // while((inputLine = in.readLine()) != null){
            //     // out.println("Server received: " + inputLine);
            //     inputLine = inputLine.trim();
            //     if(inputLine.isEmpty()) continue;
            //     String[] parts = inputLine.split("\\s+");

            //     String cmd = parts[0].toUpperCase();

            //     switch (cmd) {
            //         case "SET": {
            //             if(parts.length != 3) {out.println("ERR wrong number of arguments"); break;}
            //             String key = parts[1];
            //             int value;
            //             try {
            //                 value = Integer.parseInt(parts[2]);
            //             } catch(NumberFormatException nfe){
            //                 out.println("ERR value is not an integer");
            //                 break;
            //             }                
            //             shared.put(key, new Value(value,0));
            //             out.println("OK");
            //             aofQueue.offer(inputLine);
            //             break;
            //         }
            //         case "GET": {
            //             if(parts.length != 2) {
            //                 out.println("ERR wrong number of arguments");
            //                 break;
            //             }

            //             String key = parts[1];
            //             Value v = getAlive(key);

            //             if(v == null) out.println("NULL");
            //             else out.println(v.val);
            //             break;
            //         }
            //         case "INCR": {
            //             if (parts.length != 2) {
            //                 out.println("ERR wrong number of arguments");
            //                 break;
            //             }
            //             String key = parts[1];

            //             long now = System.currentTimeMillis();
            //             Value updated = shared.compute(key, (k, old) -> {
            //                 if(old == null) return new Value(1,0);

            //                 //if expired, treat as missing
            //                 if (old.expireAt != 0 && now >= old.expireAt){
            //                     return new Value(1, 0);
            //                 }
            //                 old.val += 1;
            //                 return old;
            //             });
            //             aofQueue.offer(inputLine);
            //             out.println(updated.val);
            //             break;
            //         }

            //         case "EXPIRE": {
            //             if (parts.length != 3) {
            //                 out.println("ERR wrong number of arguments");
            //                 break;
            //             }

            //             String key = parts[1];
            //             int seconds;
            //             try {
            //                 seconds = Integer.parseInt(parts[2]);
            //             }catch (NumberFormatException nfe){
            //                 out.println("ERR seconds is not an integer");
            //                 break;
            //             }

            //             long now = System.currentTimeMillis();
            //             final long expireAt = now + (seconds * 1000L);

            //             Value updated = shared.compute(key, (k, old) -> {
            //                 if(old == null) return null;
            //                 if(old.expireAt != 0 && now >= old.expireAt) return null;
            //                 old.expireAt = expireAt;
            //                 return old;
            //             });
            //             aofQueue.offer(inputLine);
            //             out.println(updated == null ? "0" : "1");
            //             break;
            //         }

            //         case "TTL": {
            //             if (parts.length != 2) {
            //                 out.println("ERR wrong number of arguments");
            //                 break;
            //             }

            //             String key = parts[1];

            //             long now = System.currentTimeMillis();

            //             Value v = shared.get(key);
            //             if(v == null) {
            //                 out.println("-2");
            //                 break;
            //             }

            //             if(v.expireAt != 0 && now >= v.expireAt){
            //                 shared.remove(key, v);
            //                 out.println("-2");
            //                 break;
            //             }

            //             // No expiry
            //             if (v.expireAt == 0) {out.println("-1"); break;}
            //             long remainingMs = v.expireAt - now;
            //             long remainingSec = remainingMs / 1000L;
            //             out.println(Long.toString(remainingSec));
            //             break;
            //         }

            //         default:
            //             out.println("ERR unknown command");
            //     }
            // }
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
    // Carraige Return Line Feed
    private List<String> readRespCommand(BufferedInputStream in) throws IOException {
        // Why BufferedInputStream
        // 1. TCP reads can be small chunks
        // 2. buffering reduces syscalls and makes your parser faster/cleaner

        int first = readByteOrThrow(in);
        if(first != '*'){
            throw new IOException("Expected '*', got: " + (char) first);
        }

        int count = readIntLineCRLF(in); // reads "<count>\r\n"

        if(count < 0) return null;  // RESP uses -1 for null array

        List<String> args = new ArrayList<>(count);
        for(int i = 0; i < count; i++){
            args.add(readBulkString(in));
        }
        return args;
    }

    private String readBulkString(BufferedInputStream in) throws IOException{
        int first = readByteOrThrow(in);
        
        if (first != '$') {
            throw new IOException("Expected '$', got: " + (char) first);
        }

        int len = readIntLineCRLF(in);
        if (len == -1) return null;

        byte[] data = readFully(in, len); // read EXACTLY len bytes raw
        expectCRLF(in); //must be \r\n after the data

        return new String(data, StandardCharsets.UTF_8);
    }


    private int readIntLineCRLF(BufferedInputStream in) throws IOException {
        // Reads digits (and optional leading '-' ) until CRLF, return integer
        String line = readLineCRLF(in);
        return Integer.parseInt(line);
    }

    private String readLineCRLF(BufferedInputStream in) throws IOException {
        // Reads bytes until \r\n CRLF. 

        StringBuilder sb = new StringBuilder();
        while(true){
            int b = readByteOrThrow(in);
            if (b == '\r') {
                int next = readByteOrThrow(in);
                if (next != '\n') throw new IOException("Expected LF after CR");
                break;
            }
            sb.append((char) b);
        }

        return sb.toString();
    }


    private void expectCRLF(BufferedInputStream in) throws IOException {
        int r = readByteOrThrow(in);
        int n = readByteOrThrow(in);

        if(r != '\r' || n != '\n') throw new IOException("Expected CRLF after bulk data");
    }

    private byte[] readFully(BufferedInputStream in, int n) throws IOException {
        byte[] buf = new byte[n];
        int off = 0;
        while(off < n){
            int read = in.read(buf, off, n - off);
            if(read == -1) throw new EOFException("Stream ended while reading " + n + " bytes");
            off += read;
        }
        return buf;
    }

    private int readByteOrThrow(BufferedInputStream in) throws IOException {
        int b = in.read();
        if(b == -1) throw new EOFException("Client closed connection");
        return b;
    }
}