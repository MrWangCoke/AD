package dx.ahut.adbackend.ticket.queue;

import dx.ahut.adbackend.ticket.Ticket;

public class NoopTicketQueuePublisher implements TicketQueuePublisher {

    @Override
    public void publishCreated(Ticket ticket) {
        // Redis Streams is optional; keeping DB-only behavior as the safe default.
    }
}
