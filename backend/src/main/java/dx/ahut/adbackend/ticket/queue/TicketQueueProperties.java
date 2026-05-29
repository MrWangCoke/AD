package dx.ahut.adbackend.ticket.queue;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ticket-queue.redis")
public class TicketQueueProperties {

    private boolean enabled;
    private String streamKey = "tickets:created";
    private long maxLen = 10_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getStreamKey() {
        return streamKey;
    }

    public void setStreamKey(String streamKey) {
        this.streamKey = streamKey;
    }

    public long getMaxLen() {
        return maxLen;
    }

    public void setMaxLen(long maxLen) {
        this.maxLen = maxLen;
    }
}
