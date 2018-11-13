package uk.gov.hmcts.reform.sscs.domain.email;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EmailAttachmentTest {
    @Test
    public void file() {
        Map<String, String> allowedContentTypes = new HashMap();

        allowedContentTypes.put("doc", "application/msword");
        allowedContentTypes.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        allowedContentTypes.put("jpeg", "image/jpeg");
        allowedContentTypes.put("jpg", "image/jpeg");
        allowedContentTypes.put("pdf", "application/pdf");
        allowedContentTypes.put("png", "image/png");
        allowedContentTypes.put("ppt", "application/vnd.ms-powerpoint");
        allowedContentTypes.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        allowedContentTypes.put("txt", "text/plain");
        allowedContentTypes.put("xls", "application/vnd.ms-excel");
        allowedContentTypes.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        byte[] data = {};

        for (String contentType : allowedContentTypes.keySet()) {
            String filename = "somefile." + contentType;

            EmailAttachment actual = EmailAttachment.file(data, filename);

            assertEquals(filename, actual.getFilename());
            assertEquals(allowedContentTypes.get(contentType), actual.getContentType());
            assertNotNull(actual.getData());
        }
    }

    @Test(expected = RuntimeException.class)
    public void unknownContentTypeForFile() {
        byte[] data = {};

        EmailAttachment.file(data, "somefile.unknown");
    }

    @Test(expected = RuntimeException.class)
    public void noExtensionInFileName() {
        byte[] data = {};

        EmailAttachment.file(data, "somefile");
    }

    @Test
    public void pdf() {
        byte[] data = {};
        String filename = "somefile.pdf";

        EmailAttachment actual = EmailAttachment.pdf(data, filename);

        assertEquals(filename, actual.getFilename());
        assertEquals("application/pdf", actual.getContentType());
        assertNotNull(actual.getData());
    }

    @Test
    public void json() {
        byte[] data = {};
        String filename = "somefile.json";

        EmailAttachment actual = EmailAttachment.json(data, filename);

        assertEquals(filename, actual.getFilename());
        assertEquals("application/json", actual.getContentType());
        assertNotNull(actual.getData());
    }
}
