package dx.ahut.adbackend.ticket.queue;

import dx.ahut.adbackend.ticket.Ticket;
import java.util.Map;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@ConditionalOnProperty(prefix = "app.ticket-queue.redis", name = "enabled", havingValue = "true")
public class RedisStreamTicketQueuePublisher implements TicketQueuePublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisStreamTicketQueuePublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final TicketQueueProperties properties;
    private final Executor executor;

    public RedisStreamTicketQueuePublisher(
            StringRedisTemplate redisTemplate,
            TicketQueueProperties properties,
            @Qualifier("ticketQueueExecutor") Executor executor
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.executor = executor;
    }

    @Override
    public void publishCreated(Ticket ticket) {
        TicketCreatedEvent event = TicketCreatedEvent.from(ticket);
        Runnable publishTask = () -> publishNow(event);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.execute(publishTask);
                }
            });
            return;
        }
        executor.execute(publishTask);
    }

    private void publishNow(TicketCreatedEvent event) {
        try {
            Map<String, String> payload = Map.of(
                    "id", event.id(),
                    "ticketNo", event.ticketNo(),
                    "userId", event.userId(),
                    "ticketType", event.ticketType(),
                    "status", event.status()
            );
            MapRecord<String, String, String> record = MapRecord.create(properties.getStreamKey(), payload);
            RecordId recordId = redisTemplate.opsForStream().add(record);
            trimStream();
            LOGGER.debug("Published ticket {} to Redis Stream {} as {}", event.id(), properties.getStreamKey(), recordId);
        } catch (RuntimeException error) {
            LOGGER.warn("Redis Stream ticket publish failed for ticket {}. DB ticket remains valid.", event.id(), error);
        }
    }

    private void trimStream() {
        long maxLen = properties.getMaxLen();
        if (maxLen > 0) {
            redisTemplate.opsForStream().trim(properties.getStreamKey(), maxLen);
        }
    }

    private record TicketCreatedEvent(
            String id,
            String ticketNo,
            String userId,
            String ticketType,
            String status
    ) {

        private static TicketCreatedEvent from(Ticket ticket) {
            Long userId = ticket.getUser() == null ? null : ticket.getUser().getId();
            return new TicketCreatedEvent(
                    String.valueOf(ticket.getId()),
                    valueOrBlank(ticket.getTicketNo()),
                    userId == null ? "" : String.valueOf(userId),
                    String.valueOf(ticket.getTicketType()),
                    String.valueOf(ticket.getStatus())
            );
        }

        private static String valueOrBlank(String value) {
            return value == null ? "" : value;
        }
    }
}
