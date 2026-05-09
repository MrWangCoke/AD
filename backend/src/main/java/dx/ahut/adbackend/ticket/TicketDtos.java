package dx.ahut.adbackend.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public final class TicketDtos {

    private TicketDtos() {
    }

    public record CreateTicketRequest(
            @NotNull Integer ticketType,
            @NotNull Long userId,
            @NotBlank String studentId,
            @NotBlank String phone,
            String broadbandAccount,
            String newPassword
    ) {
    }

    public record TicketResponse(
            Long id,
            Long userId,
            String ticketNo,
            String studentId,
            Integer ticketType,
            Integer status,
            String broadbandAccount,
            String newPassword,
            String phone,
            String resultMessage,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {

        public static TicketResponse from(Ticket ticket) {
            return new TicketResponse(
                    ticket.getId(),
                    ticket.getUser().getId(),
                    ticket.getTicketNo(),
                    ticket.getStudentId(),
                    ticket.getTicketType(),
                    ticket.getStatus(),
                    ticket.getBroadbandAccount(),
                    ticket.getNewPassword(),
                    ticket.getPhone(),
                    ticket.getResultMessage(),
                    ticket.getCreatedAt(),
                    ticket.getUpdatedAt()
            );
        }
    }
}
