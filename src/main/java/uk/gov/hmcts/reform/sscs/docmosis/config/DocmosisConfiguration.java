package uk.gov.hmcts.reform.sscs.docmosis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocmosisPdfGenerationService;

@Configuration
@ConditionalOnProperty("service.pdf-service.uri")
public class DocmosisConfiguration {

    @Value("${service.pdf-service.uri}")
    private String pdfServiceEndpoint;

    @Value("${service.pdf-service.accessKey}")
    private String pdfServiceAccessKey;

    @Bean
    public DocmosisPdfGenerationService docmosisTemplate(RestTemplate restTemplate) {
        return DocmosisPdfGenerationService.createDefaultService(pdfServiceEndpoint,
                pdfServiceAccessKey, restTemplate);
    }
}
