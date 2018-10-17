package uk.gov.hmcts.reform.sscs.pdf;

import java.time.LocalDate;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;

public class PdfUtil {

    private PdfUtil() {

    }

    public static LocalDate findCurrentDate() {
        return LocalDate.now();
    }

    public static String getRepFullName(Representative representative) {
        if (representative != null) {
            return representative.getName().getFullName();
        } else {
            return null;
        }
    }
}
