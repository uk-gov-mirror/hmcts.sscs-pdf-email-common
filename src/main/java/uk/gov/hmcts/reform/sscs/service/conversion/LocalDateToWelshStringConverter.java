package uk.gov.hmcts.reform.sscs.service.conversion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.docmosis.config.TemplateWelshMonthConfig;

@Component
@RequiredArgsConstructor
public class LocalDateToWelshStringConverter {
    private final TemplateWelshMonthConfig templateWelshMonthConfig;

    public String convert(LocalDate dateToConvert) {
        return Optional.ofNullable(dateToConvert).map(date -> {
            int day = dateToConvert.getDayOfMonth();
            int year = dateToConvert.getYear();
            int month = dateToConvert.getMonth().getValue();
            return String.join(" ", Integer.toString(day),
                    templateWelshMonthConfig.getWelshMonths().get(String.valueOf(month)),
                    Integer.toString(year));
        }).orElse(null);
    }

    public String convert(String localDateFormat) {
        return convert(LocalDate.parse(localDateFormat));
    }

    public String convertDateTime(String localDateTimeFormat) {
        return convert(LocalDateTime.parse(localDateTimeFormat).toLocalDate());
    }
}
