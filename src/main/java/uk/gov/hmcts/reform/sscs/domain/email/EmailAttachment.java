package uk.gov.hmcts.reform.sscs.domain.email;

import java.util.HashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Data;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;

@Data
@Builder
public class EmailAttachment {
    private static final Map<String, String> ALLOWED_CONTENT_TYPES = new HashMap();

    static {
        ALLOWED_CONTENT_TYPES.put("doc", "application/msword");
        ALLOWED_CONTENT_TYPES.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        ALLOWED_CONTENT_TYPES.put("jpeg", "image/jpeg");
        ALLOWED_CONTENT_TYPES.put("jpg", "image/jpeg");
        ALLOWED_CONTENT_TYPES.put("pdf", "application/pdf");
        ALLOWED_CONTENT_TYPES.put("png", "image/png");
        ALLOWED_CONTENT_TYPES.put("ppt", "application/vnd.ms-powerpoint");
        ALLOWED_CONTENT_TYPES.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        ALLOWED_CONTENT_TYPES.put("txt", "text/plain");
        ALLOWED_CONTENT_TYPES.put("xls", "application/vnd.ms-excel");
        ALLOWED_CONTENT_TYPES.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

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
            getContentTypeForFileName(fileName),
            fileName
        );
    }

    protected static String getContentTypeForFileName(String fileName) {
        String fileTypeExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        if (ALLOWED_CONTENT_TYPES.containsKey(fileTypeExtension)) {
            return ALLOWED_CONTENT_TYPES.get(fileTypeExtension);
        } else {
            throw new RuntimeException("Evidence file type '" + fileTypeExtension + "' unknown");
        }
    }
}
