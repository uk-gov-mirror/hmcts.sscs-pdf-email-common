package uk.gov.hmcts.reform.sscs.domain.email;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
@Builder
public class SubmitYourAppealEmailTemplate {
    private final String from;
    private final String to;
    private final String message;

    public Email generateEmail(String subject, List<EmailAttachment> attachments) {
        return new Email(
                from,
                to,
                subject,
                message,
                attachments
        );
    }
}
