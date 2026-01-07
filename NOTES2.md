#### Shared State & Race Conditions
- Each client has its own socket
- Each client has its own thread
- The moment we add: `Map<String,String> store`
    - that maps becomes:
        - shared
        - accessed by **multiple threads**
        - mutable


What is race condition?
- A race condition is the correctness of the program depends on the timing of threads
- If the result changes based on _who runs first_, you have a race.


##### The classic example
Think about this command:
`INCR counter`

- What does it _actually_ do?

Break it down mentally:
1. Read current value of `counter`
2. Convert to number
3. Add 1
4. Write value back

```
Thread A reads 0
Thread B reads 0
Thread A writes 1
Thread B writes 1
```


HashMap make it worse
- **not thread-safe** -- (Refers to the entire data structure being safe for multiple threads to use simultaneously)
- **not atomic** -- Refers to a *single action* (like a single update) being uninterruptible.
- not protected against concurrent modification

ConcurrentHashMap is thread safe
- not atomic

Mental Model:
If you see code that:
    1. reads shared state
    2. modifies it
    3. writes it back
`Can another thread sneak in between these steps?`



### ConcurrentHashMap
- is a thread-safe implementation of the Map interface.

    - Provides thread-safe operations without locking the entire map.
    - Allows multiple threads to operate concurrently by dividing the map into segments.
    - Support atomic operations like putIfAbsent(), replace() and remove()


- It will Bucket Locking mechanism to insert or update" without using synchronized.
- Instead of a global lock, ConcurrentHashMap uses a hybrid approach based on the state of the specific hash bucket (bin):
    - The thread acquires a lock strictly on the Head Node of that specific bucket.
    - Only threads attempting to write to this specific bucket are blocked. Threads accessing other buckets proceed in parallel.