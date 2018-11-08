package uk.gov.hmcts.reform.sscs.domain.email;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class EmailAttachmentTest {
    @Test
    public void file() {
        byte[] data = {};
        String filename = "somefile.doc";

        EmailAttachment actual = EmailAttachment.file(data, filename);

        assertEquals(filename, actual.getFilename());
        assertNull(actual.getContentType());
        assertNotNull(actual.getData());
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
