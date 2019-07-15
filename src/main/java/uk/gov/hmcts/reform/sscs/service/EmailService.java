package uk.gov.hmcts.reform.sscs.service;

import javax.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.domain.email.Email;
import uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment;
import uk.gov.hmcts.reform.sscs.exception.EmailSendFailedException;

@Service
@Slf4j
public class EmailService {

    private static final String ID_FORMAT = "%s_%s";

    private final JavaMailSender javaMailSender;

    @Autowired
    public EmailService(final JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Retryable(value = EmailSendFailedException.class,
            backoff = @Backoff(delay = 100, maxDelay = 500))
    public void sendEmail(final Email email) {
        try {
            final MimeMessage message = javaMailSender.createMimeMessage();
            final MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(message, true);

            mimeMessageHelper.setFrom(email.getFrom());
            mimeMessageHelper.setTo(email.getTo());
            mimeMessageHelper.setSubject(email.getSubject());
            mimeMessageHelper.setText(email.getMessage());

            long attachmentsSize = 0;
            if (email.hasAttachments()) {
                for (EmailAttachment emailAttachment : email.getAttachments()) {
                    InputStreamSource data = emailAttachment.getData();
                    if (data instanceof ByteArrayResource) {
                        attachmentsSize += ((ByteArrayResource) data).contentLength();
                    } else {
                        log.error("Cannot calculate attachment size as not a ByteArrayResource when expected to be.");
                    }
                    mimeMessageHelper.addAttachment(emailAttachment.getFilename(),
                            emailAttachment.getData(),
                            emailAttachment.getContentType());
                }
            }

            log.info("Sending email with subject [" + email.getSubject() + "] " +
                    "of [" + message.getSize() + "] bytes " +
                    "with [" + attachmentsSize + "] bytes of attachments.");
            javaMailSender.send(message);
        } catch (Exception e) {
            log.error("Error while sending email {} ", e);
            throw new EmailSendFailedException("Error while sending email", e);
        }
    }

    public String generateUniqueEmailId(Appellant appellant) {
        String appellantLastName = appellant.getName().getLastName();
        String nino = appellant.getIdentity().getNino();
        return String.format(ID_FORMAT, appellantLastName, nino.substring(nino.length() - 3));
    }
}
