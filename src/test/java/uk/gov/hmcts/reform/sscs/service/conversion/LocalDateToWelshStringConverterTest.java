package uk.gov.hmcts.reform.sscs.service.conversion;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LocalDateToWelshStringConverterTest {

    @InjectMocks
    private LocalDateToWelshStringConverter localDateToWelshStringConverter;

    @Test
    public void testLocalDateConvertedToWelsh() {
        String localDate = "2020-12-27";
        String welshDate = localDateToWelshStringConverter.convert(localDate);
        Assert.assertEquals("27 Rhagfyr 2020", welshDate);
    }

    @Test
    public void testLocalDateTimeConvertedToWelsh() {
        String localDate = "2020-12-27T17:13:21.569";
        String welshDate = localDateToWelshStringConverter.convertDateTime(localDate);
        Assert.assertEquals("27 Rhagfyr 2020", welshDate);
    }
}
