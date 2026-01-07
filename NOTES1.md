## Notes


- A TCP server is a process that:
    - opens a port
    - waits forever
    - accepts connections
    - read bytes
    - writed bytes


- Redis
    - Connection stays open
    - Raw commands
    - Stateful connection
    - Extremely low latency


### Blocking I/O
- This threads stops and waits until data arrives

- When you call:
    - `read()`: thread sleeps until data comes
    - `accept()`: thread sleeps until a client connects


### Thread
- A thread is an independent path of execution inside the same process.

- Java program:
    - is one process
    - can have many threads 
    - all threads share the same heap memory

- Each thread has
    - its own call stack
    - its own program counter

```
Main thread:
    accept client
    start new thread
    accept next client

Client thread
    read from client    <---- BLOCKS (only this client)
```

All thread share:
- heap objects
- static variables
- collections


##### Thread lifecycle
    1. is created
    2. start running
    3. blocks / run
    4. finishes
