### Lab 6 description
- Understand why time is hard in systems
- Implement EXPIRE and TTL
- Learn Lazy vs active expiration
- Add background cleanup task
- Avoid common time bugs (clock drift, blocking cleanup)


In systems:
    - threads run concurrently
    - cleanup can block normal work
    - clocks can change
    - scanning data can be expensive

So expiration must be
    - cheap
    - non-blocking
    - eventually consistent

```
SET k v
EXPIRE k 5
```

So what you store is not "seconds", but an **Absolute timestamp**
`ttl = 5`
`expireAt   =   currentTime  +  5000ms`


#### Two Expiration strategies
- **Lazy expiration**
Key is checked when *accessed*
Remove when someone tocuhes the key (`GET/INCR/TTL/EXPIRE`)
Example:
    -  `GET k`
    - if  `now >= expireAt`
        - delete key
        - return NULL
- Cheap
- Simple
- Expired keys may sit in memory until accessed


- **Active expiration**
Background task deletes expired keys periodically
 A background task:
    - peridically scans key
    - removes expired ones

- Frees memory proactively
- Must be careful not to block

- `System.currentTimeMillis() ` -- for expiration timestamps.



- `key -->  (value, expireAtMillis | none)`
    - creating a small class 



- **EXPIRE** --> Write a deadline
- **TTL**    --> read the remaining time until that deadline

### Lab 7
- Understand **why in-memory systems still need persistence**
- Implement **Append-only File (AOF)** logging
- Recover state on server restart
- Reason about **crash consistency**


#### Why persistence exists
- Currently our server is:
    - fast
    - concurrent 
    - supports TTL

But if the process crashes or restart:
- All data is gone

In-memory speed + durability is the core tension of databases
Redis solves this with:
    - AOF (Append only file)
    - RDB snapshots


##### What is AOF?
- It means Every write command is appended to a log file in order
- If the server crashes:
    - restart
    - read file from top
    - replay commands
    - rebuild in-memory state

Only state-changing commands:
- `SET`
- `INCR`
- `EXPIRE`


#### When to write to disk
Three strategies (Redis supports all):
    1. Always fsync (slow, safest) `fsync --> File Synchronize` `Stop what you are doing and flush this specific data out of the RAM and onto the physical disk right now.`
    2. Every N seconds (balanced)
    3. Never fsync (fast, risky)

For the lab:
- write to file
- flush (`flush()`)
- no need to call `fsync()` 

- flush --> means forcefully empty the bucket.

**Write to AOF only after the in-memory update succeeds**

Only one thread in the whole program may touch the file.

**Now, every client thread to disk**
- Instead of letting every client thread write to disk:
    - client thread enqueues a log entry
    - **one dedicated logger thread** reads from the queue and writes to the file

- `synchronized` around a single shared writer
    - Create one shared `BufferWriter` in `Server`
    - Every time a client wants to append a line, it calls a helper
    - That helper uses a lock so *only one thread write at a time*

`static BlockingQueue<String> aofQueue = new LinkedlockingQueue<>();`
    - BlockingQueue - A thread safe queue that multiple threads can safely access.
    - LinkedBlockingQueue: Implements the queue using a linked list structure
    - Purpose: Stores log messages waiting to be written to the file
    
`static volatile boolean running = true;`
    - volatile: Ensures all threads see the most up-to-date value (prevent caching issues)
- **The Problem without** `volatile`
    - CPU caching
        - When you have multiple threads, each thread might keep it own cached copy of variables for performance:

`volatile` tells Java: **'Always read/write this variable direclty from main memory, never cache it."**

### With volatile:
```
Main Thread              Background Thread
running = true    ←──────→ running = true (main memory)
     ↓                            ↓
running = false   ←──────→ running = false (sees change immediately)
```




