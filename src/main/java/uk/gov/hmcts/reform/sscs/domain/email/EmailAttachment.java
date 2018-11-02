package uk.gov.hmcts.reform.sscs.domain.email;

import lombok.Builder;
import lombok.Data;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;

@Data
@Builder
public class EmailAttachment {

    private final InputStreamSource data;
    private final String contentType;
    private final String filename;

    public static EmailAttachment pdf(byte[] content, String fileName) {
        return new EmailAttachment(
            new ByteArrayResource(content),
            "application/pdf",
            fileName
        );
    }

    public static EmailAttachment json(byte[] content, String fileName) {
        return new EmailAttachment(
            new ByteArrayResource(content),
            "application/json",
            fileName
        );
    }

    public static EmailAttachment file(byte[] content, String fileName) {
        return new EmailAttachment(
            new ByteArrayResource(content),
            null,
            fileName
        );
    }
}
