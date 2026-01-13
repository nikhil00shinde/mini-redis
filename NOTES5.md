When data arrives on a TCP socket, you do **not** receive:
    - one command
    - one line
    - one message

You receive a **stream of bytes**.
    - It can have:
        - half a command
        - one command
        - multipe commands stucks together

Redis-cli --> It encodes it into RESP so every Redis server in the world can understand it:










### Lab 8
- Right now our server speaks to our own made-up text protocol:
```
SET a 10
GET a
INCR a
```

It works only because clients sends that format.

But the real Redis ecosystem (redis-cli, libraries, Kubernates probes etc) all speak one language.
| RESP - Redis Serilization Protocol

##### What is RESP?
- RESP is the **wire protocol** Redis uses on TCP.
It defines:
- How commands are sent
- How replies are formatted
- How errors, integers, null, arrays, etc. are encoded.

```
redis-cli SET a 10
SET a 10\n

*3\r\n
$3\r\nSET\r\n
$1\r\na\r\n
$2\r\n10\r\n
```

| Part | Meaning |
| ---- | ------- |
| `*3` | Array of 3 items |
| `$3` | Bulk string length 3 |
| `SET` | Command |
| `$1` | length 1 |
| `a`  | key |
| `$2` | length 2 |
| `10` | value |

It is **strcutured binary protocol** over TCP.

##### What RESP gives Redis
RESP gives Redis:
- Unambiguous parsing (no guessing where tokens end)
- Binary-safe strings
- Pipelining (multiple commands in one TCP packet)
- High performance
- Language-agnostic clients

```
Client --> "SET a 10\n"
Server --> Parses words
```

`redis-cli  -->  RESP bytes --> server --> RESP replies --> redis-cli`


<!-- CARRIAGE RETURN and LINE FEED -->
<!-- Carriage Return moves the cursor to the start of the line, and Line Feed moves it down to the next line. Together, they mark the end of a line—like pressing “Enter” on your keyboard! -->


<!-- 
When a TCP client connects to Redis server:

InputStream -- bytes comming FROM client
OutputStream -- bytes going TO client


BufferedReader -- Read bytes until you see CRLF, then gives me a string



 -->