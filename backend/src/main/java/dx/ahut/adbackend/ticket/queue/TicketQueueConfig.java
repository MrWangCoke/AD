package dx.ahut.adbackend.ticket.queue;

import java.util.concurrent.Executor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties(TicketQueueProperties.class)
public class TicketQueueConfig {

    @Bean
    public Executor ticketQueueExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("ticket-queue-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean(TicketQueuePublisher.class)
    public TicketQueuePublisher noopTicketQueuePublisher() {
        return new NoopTicketQueuePublisher();
    }
}
