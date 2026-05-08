package dx.ahut.adbackend.ticket;

import dx.ahut.adbackend.ticket.TicketDtos.CreateTicketRequest;
import dx.ahut.adbackend.ticket.TicketDtos.TicketResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/new-user-bind")
    public TicketResponse createNewUserBindTicket(@Valid @RequestBody CreateTicketRequest request) {
        return ticketService.createNewUserBindTicket(request);
    }

    @GetMapping("/users/{userId}")
    public List<TicketResponse> listUserTickets(@PathVariable Long userId) {
        return ticketService.listByUser(userId);
    }
}
