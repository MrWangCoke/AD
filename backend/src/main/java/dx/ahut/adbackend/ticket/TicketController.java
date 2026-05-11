package dx.ahut.adbackend.ticket;

import dx.ahut.adbackend.ratelimit.RateLimitService;
import dx.ahut.adbackend.ticket.TicketDtos.CreateTicketRequest;
import dx.ahut.adbackend.ticket.TicketDtos.TicketResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final RateLimitService rateLimitService;

    public TicketController(TicketService ticketService, RateLimitService rateLimitService) {
        this.ticketService = ticketService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/new-user-bind")
    public TicketResponse createNewUserBindTicket(@Valid @RequestBody CreateTicketRequest request) {
        rateLimitService.check(
                "tickets:new-user-bind:" + request.userId(),
                10,
                Duration.ofMinutes(1),
                "提交过于频繁，请稍后再试"
        );
        return ticketService.createNewUserBindTicket(request);
    }

    @PostMapping("/broadband-password-reset")
    public TicketResponse createBroadbandPasswordTicket(@Valid @RequestBody CreateTicketRequest request) {
        rateLimitService.check(
                "tickets:broadband-password-reset:" + request.userId(),
                10,
                Duration.ofMinutes(1),
                "提交过于频繁，请稍后再试"
        );
        return ticketService.createBroadbandPasswordTicket(request);
    }

    @GetMapping("/pending")
    public List<TicketResponse> listPendingTickets(
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ticketService.listPendingTickets(limit);
    }

    @GetMapping("/users/{userId}")
    public List<TicketResponse> listUserTickets(@PathVariable Long userId) {
        return ticketService.listByUser(userId);
    }
}
