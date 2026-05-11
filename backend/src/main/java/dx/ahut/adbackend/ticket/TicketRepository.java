package dx.ahut.adbackend.ticket;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    boolean existsByTicketNo(String ticketNo);

    List<Ticket> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Ticket> findByStatusOrderByCreatedAtAsc(Integer status, Pageable pageable);

    Optional<Ticket> findFirstByUserIdAndStudentIdAndPhoneAndTicketTypeAndStatusInOrderByCreatedAtDesc(
            Long userId,
            String studentId,
            String phone,
            Integer ticketType,
            Collection<Integer> statuses
    );
}
