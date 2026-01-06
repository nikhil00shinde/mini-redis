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


