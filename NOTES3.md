### Thread Pools & Backpressure
- Why `new Thread(...)` is dangerous at scale
- Replace thread-per-connection with a **thread pool**
- Add **backpressure** (so the server degrades gracefully instead of dying)
- Understanding difference between "works for 10 clients" and "works reliably under load"

- What's wrong with 'one thread per client'?
    - Each connection creates:
        - a thread stack (memory)
        - scheduling overhead (CPU context switching)
        - risk of "too many threads" --> JVM slows or crashes

- The thread pool idea
    - Instead of creating a new thread for every connection, you resuse a fixed number of worker threads.
    - A collection (pool) of pre-created threads that are ready to perform tasks, managed by a supervisor

    - Main threads
        - accepts sockets
        - submits them as tasks to a pool
    
    - Worker threads (fixed number)
        - run client handlers
    
    - Benifits
        - CPU stable 
        - memory stable
        - predictable performance


- Backpressure
    - If more clients come than your pool can handle, you must decide what happens.
    
    - Options:
        1. Queue tasks: but queue can explode memory
        2. Reject new clients (fast failure)
        3. Block acceptor (slow down accepting)

    - Use:
        1. bounded queue
         - reject policy that tells the client: "Server busy"
