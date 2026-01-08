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