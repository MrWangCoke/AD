package dx.ahut.adbackend.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RateLimitService {

    private static final int CLEANUP_EVERY_REQUESTS = 256;

    private final Clock clock;
    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
    private int requestCount;

    public RateLimitService() {
        this(Clock.systemUTC());
    }

    RateLimitService(Clock clock) {
        this.clock = clock;
    }

    public void check(String key, int maxRequests, Duration window, String message) {
        long now = clock.millis();
        Window current = windows.compute(key, (ignored, existing) -> {
            if (existing == null || now >= existing.resetAtMillis) {
                return new Window(now + window.toMillis(), 1);
            }
            existing.count++;
            return existing;
        });

        cleanupExpiredWindows(now);
        if (current.count > maxRequests) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, message);
        }
    }

    private synchronized void cleanupExpiredWindows(long now) {
        requestCount++;
        if (requestCount % CLEANUP_EVERY_REQUESTS != 0) {
            return;
        }

        Iterator<Map.Entry<String, Window>> iterator = windows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Window> entry = iterator.next();
            if (now >= entry.getValue().resetAtMillis) {
                iterator.remove();
            }
        }
    }

    private static final class Window {
        private final long resetAtMillis;
        private int count;

        private Window(long resetAtMillis, int count) {
            this.resetAtMillis = resetAtMillis;
            this.count = count;
        }
    }
}
