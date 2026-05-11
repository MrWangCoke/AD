package dx.ahut.adbackend.ticket;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class TicketNumberGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String nextTicketNo() {
        int randomPart = RANDOM.nextInt(0x1000000);
        return "TK-" + LocalDate.now().format(DATE_FORMAT) + "-" + String.format("%06X", randomPart);
    }
}
