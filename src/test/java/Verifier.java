import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class Verifier {
    private final CountDownLatch              latch  = new CountDownLatch(1);
    private final Map<Integer, AtomicBoolean> guards = new ConcurrentHashMap<>();

    void verify(final int group) throws Exception {
        final AtomicBoolean guard = guards.computeIfAbsent(group, _group -> new AtomicBoolean());

        try {
            if (!guard.compareAndSet(false, true)) {
                trigger();
            }
            Thread.sleep(1000);
        } finally {
            guard.set(false);
        }
    }

    void trigger() {
        latch.countDown();
    }

    void await() throws Exception {
        if (latch.await(5, TimeUnit.MINUTES)) {
            throw new Exception();
        }
    }
}
