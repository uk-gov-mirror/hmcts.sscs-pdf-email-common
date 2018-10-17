package uk.gov.hmcts.reform.sscs.pdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;

public class PdfUtilTest {

    @Test
    public void findRepresentativeNameWhenExists() {
        assertEquals("Mr Harry Potter", PdfUtil.getRepFullName(Representative.builder().name(Name.builder().title("Mr").firstName("Harry").lastName("Potter").build()).build()));
    }

    @Test
    public void returnNullWhenRepresentativeDoesNotExist() {
        assertNull(PdfUtil.getRepFullName(null));
    }
}
