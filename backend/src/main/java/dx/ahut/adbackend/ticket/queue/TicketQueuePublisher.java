package dx.ahut.adbackend.ticket.queue;

import dx.ahut.adbackend.ticket.Ticket;

public interface TicketQueuePublisher {

    void publishCreated(Ticket ticket);
}
