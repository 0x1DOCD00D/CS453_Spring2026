import java.util.concurrent.locks.ReentrantLock;

public final class LockOverheadDemo {
    // Use globally reachable locks to prevent the JVM from proving "no contention ever"
    // and eliding the monitor in some cases.
    private static final Object MONITOR_LOCK = new Object();
    private static final ReentrantLock REENTRANT_LOCK = new ReentrantLock();

    // Volatile sink prevents the JIT from discarding results as dead code.
    private static volatile long sink;

    public static void main(String[] args) {
        long iterations = args.length > 0 ? Long.parseLong(args[0]) : 50_000_000L;
        int warmupRounds = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        int measureRounds = args.length > 2 ? Integer.parseInt(args[2]) : 5;

        // Warm up the JIT
        //sink ^= value is used to mix the computed result into a volatile variable
        // so the JIT compiler is less likely to treat the loop body as useless
        // and optimize it away. It is not doing encryption, it is just a cheap way
        // to keep an observable dependency.
        for (int i = 0; i < warmupRounds; i++) {
            sink ^= runNoLock(iterations);
            sink ^= runSynchronized(iterations);
            sink ^= runReentrantLock(iterations);
        }

        measure("No lock", iterations, measureRounds, LockOverheadDemo::runNoLock);
        measure("synchronized", iterations, measureRounds, LockOverheadDemo::runSynchronized);
        measure("ReentrantLock", iterations, measureRounds, LockOverheadDemo::runReentrantLock);

        // Keep sink observable
        if (sink == 119661204) {
            System.out.println("Unlikely, but hello " + sink);
        }
    }

    @FunctionalInterface
    private interface Runner {
        long run(long iters);
    }

    private static void measure(String name, long iterations, int rounds, Runner runner) {
        long bestNanos = Long.MAX_VALUE;
        long lastResult = 0;

        for (int i = 0; i < rounds; i++) {
            long t0 = System.nanoTime();
            lastResult = runner.run(iterations);
            long t1 = System.nanoTime();
            long elapsed = t1 - t0;
            if (elapsed < bestNanos) bestNanos = elapsed;
            sink ^= lastResult;
        }

        double nsPerOp = (double) bestNanos / (double) iterations;
        System.out.printf(
                "%-14s best = %8.3f ns/op, total = %.3f ms, last = %d%n",
                name, nsPerOp, bestNanos / 1_000_000.0, lastResult
        );
    }

    private static long runNoLock(long iterations) {
        long counter = 0;
        for (long i = 0; i < iterations; i++) {
            counter++;
        }
        return counter;
    }

    private static long runSynchronized(long iterations) {
        long counter = 0;
        for (long i = 0; i < iterations; i++) {
            synchronized (MONITOR_LOCK) {
                counter++;
            }
        }
        return counter;
    }

    private static long runReentrantLock(long iterations) {
        long counter = 0;
        for (long i = 0; i < iterations; i++) {
            REENTRANT_LOCK.lock();
            try {
                counter++;
            } finally {
                REENTRANT_LOCK.unlock();
            }
        }
        return counter;
    }
}
