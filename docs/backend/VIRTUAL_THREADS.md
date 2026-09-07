# When to Use Virtual Threads

Java 25 gives us virtual threads. They are not a drop-in upgrade for every thread pool — the win depends
entirely on **what the thread blocks on**.

## The one rule

> Virtual threads unmount on **socket** I/O. They do **not** unmount on **file** I/O.

A virtual thread blocked on a socket releases its carrier thread, and the carrier goes off and runs
other work. A virtual thread blocked on the filesystem **holds its carrier for the whole call**. The
carrier pool defaults to `Runtime.getRuntime().availableProcessors()` — often 2–4 in a container — so a
handful of concurrent file writes can starve the scheduler for every virtual thread in the JVM.

## ✅ Good candidates

- HTTP / REST clients, webhooks, outbound API calls
- Elasticsearch / OpenSearch queries
- PostgreSQL and other **networked** JDBC (`jdbc:postgresql:…`)
- Redis / pub-sub / message queues
- Anything with a socket at the bottom of the stack, especially high fan-out with idle waits

## ❌ Poor candidates — keep these on platform threads

- **Embedded H2** (`jdbc:h2:/path/…`) — MVStore writes are file I/O
- Local disk reads/writes, `FileChannel`, `Files.*`, `File.exists()`, fsync
- Network-backed filesystems (NFS, EFS, FUSE/geesefs) — file I/O *and* slow
- Asset/binary storage on a mounted volume
- CPU-bound work — virtual threads give nothing here; use a sized pool

## Worked example: why we reverted the H22 cache migration

PR #35992 moved the H22 (embedded H2 on-disk cache) async commit executor from a fixed pool of 5
platform threads to `Executors.newThreadPerTaskExecutor(Thread.ofVirtual()…)`. It was reverted in
#36900 after causing a startup hang (#36892).

The reasoning in that PR looked sound:

> JEP 491 means `synchronized` blocks in Hikari/H2 no longer pin the carrier thread, so the classic
> "virtual threads + JDBC pinning" concern does not apply.

That statement is **correct about `synchronized`** and still the wrong conclusion. JEP 491 removed one
pinning mechanism; it did not make file I/O unmount. Embedded H2 writes to a file, so every writer kept
its carrier.

Measured — 3000 upserts of a 4KB payload, 5 concurrent writers, H2 2.2.224:

| carriers | platform pool | virtual threads | ratio |
|---|---|---|---|
| 2 (small container) | 7042 puts/sec | **20 puts/sec** | 350x |
| 4 | 6977 puts/sec | 889 puts/sec | 7.8x |
| 8 | 6000 puts/sec | 1304 puts/sec | 4.6x |
| 24 (default) | 7059 puts/sec | 1293 puts/sec | 5.5x |

Platform throughput is flat regardless of carrier count. Virtual threads were 5x slower even with 24
carriers available, and collapsed on a 2-CPU container.

The throughput loss then exposed a second-order failure: a pre-existing overflow path that ran cache
writes on the calling thread, which was unreachable at 7000 puts/sec, started firing during startup and
parked the `main` thread on H2's single-writer lock. **A latent fallback that was harmless at high
throughput became a hang at low throughput.**

## Before you migrate a pool

1. **Identify what it blocks on.** Socket, file, or CPU? If file or CPU, stop.
2. **Check for a submit-time cost change.** `newThreadPerTaskExecutor` starts a thread *immediately* on
   submit. A `ThreadPoolExecutor` with a queue holds backlog as cheap queue nodes; the virtual version
   holds it as live parked threads. Under a large backlog that is a big memory difference.
3. **Measure with a realistic carrier count.** Benchmarking on a 24-core dev laptop hides carrier
   starvation entirely — the H22 regression was 5x locally and 350x on 2 CPUs. Constrain it:
   ```bash
   java -Djdk.virtualThreadScheduler.parallelism=2 …
   ```
4. **Look for load-shedding or caller-runs fallbacks in the code you are speeding up or slowing down.**
   Throughput changes move those thresholds. Check what happens when the new code is *slower* than the
   old, not just faster.

## Diagnosing a suspected pinning problem

A thread dump showing a *platform* carrier thread inside file I/O, with many virtual threads parked
behind it, is the signature. Also useful:

```bash
# warn when a virtual thread pins its carrier
-Djdk.tracePinnedThreads=full
```

Note that a virtual thread parked on a `ReentrantLock` or `Semaphore` is **fine** — `java.util.concurrent`
locks are virtual-thread aware and unmount correctly. The problem is the file syscall, not the lock.

## Related

- [Java Standards](JAVA_STANDARDS.md)
- [Database Patterns](DATABASE_PATTERNS.md)
