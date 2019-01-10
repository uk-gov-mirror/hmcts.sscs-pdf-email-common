package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.config.PdfTemplateConstants.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.pdf.PdfWrapper;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class SscsGeneratePdfService {
    private static final String RPC = "rpc_";
    private PDFServiceClient pdfServiceClient;
    private PdfStoreService pdfStoreService;
    private CcdService ccdService;

    @Autowired
    public SscsGeneratePdfService(PDFServiceClient pdfServiceClient, PdfStoreService pdfStoreService, CcdService ccdService) {
        this.pdfServiceClient = pdfServiceClient;
        this.pdfStoreService = pdfStoreService;
        this.ccdService = ccdService;
    }

    public byte[] generateAndSavePdf(String templatePath, SscsCaseData sscsCaseData, Long caseDetailsId, Map<String, String> notificationPlaceholders, IdamTokens idamTokens) {
        byte[] generatedPdf = generatePdf(templatePath, sscsCaseData, caseDetailsId, notificationPlaceholders);

        mergeDocIntoCcd("Direction_Notice.pdf", generatedPdf, caseDetailsId, sscsCaseData, idamTokens);

        return generatedPdf;
    }

    protected byte[] generatePdf(String templatePath, SscsCaseData sscsCaseData, Long caseDetailsId, Map<String, String> notificationPlaceholders) {
        byte[] template;
        try {
            template = getTemplate(templatePath);
        } catch (IOException e) {
            throw new PdfGenerationException("Error getting template", e);
        }

        PdfWrapper pdfWrapper = PdfWrapper.builder()
            .sscsCaseData(sscsCaseData)
            .ccdCaseId(caseDetailsId)
            .currentDate(LocalDate.now())
            .build();

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("PdfWrapper", pdfWrapper);
        placeholders.put(LETTER_ADDRESS_LINE_1, notificationPlaceholders.get(LETTER_ADDRESS_LINE_1));
        placeholders.put(LETTER_ADDRESS_LINE_2, notificationPlaceholders.get(LETTER_ADDRESS_LINE_2));
        placeholders.put(LETTER_ADDRESS_LINE_3, notificationPlaceholders.get(LETTER_ADDRESS_LINE_3));
        placeholders.put(LETTER_ADDRESS_LINE_4, notificationPlaceholders.get(LETTER_ADDRESS_LINE_4));
        placeholders.put(LETTER_ADDRESS_POSTCODE, notificationPlaceholders.get(LETTER_ADDRESS_POSTCODE));
        placeholders.put(LETTER_NAME, notificationPlaceholders.get(LETTER_NAME));
        placeholders.put(RPC + REGIONAL_OFFICE_NAME_LITERAL, notificationPlaceholders.get(REGIONAL_OFFICE_NAME_LITERAL));
        placeholders.put(RPC + SUPPORT_CENTRE_NAME_LITERAL, notificationPlaceholders.get(SUPPORT_CENTRE_NAME_LITERAL));
        placeholders.put(RPC + ADDRESS_LINE_LITERAL, notificationPlaceholders.get(ADDRESS_LINE_LITERAL));
        placeholders.put(RPC + TOWN_LITERAL, notificationPlaceholders.get(TOWN_LITERAL));
        placeholders.put(RPC + COUNTY_LITERAL, notificationPlaceholders.get(COUNTY_LITERAL));
        placeholders.put(RPC + POSTCODE_LITERAL, notificationPlaceholders.get(POSTCODE_LITERAL));
        placeholders.put(RPC + PHONE_NUMBER, notificationPlaceholders.get(PHONE_NUMBER));

        return pdfServiceClient.generateFromHtml(template, placeholders);
    }

    private byte[] getTemplate(String templatePath) throws IOException {
        InputStream in = getClass().getResourceAsStream(templatePath);
        return IOUtils.toByteArray(in);
    }

    public void mergeDocIntoCcd(String fileName, byte[] pdf, Long caseId, SscsCaseData caseData, IdamTokens idamTokens) {
        List<SscsDocument> pdfDocuments = pdfStoreService.store(pdf, fileName);

        log.info("Appeal PDF stored in DM for Nino - {} and benefit type {}", caseData.getAppeal().getAppellant().getIdentity().getNino(),
            caseData.getAppeal().getBenefitType().getCode());

        if (caseId == null) {
            log.info("caseId is empty - skipping step to update CCD with PDF");
        } else {
            List<SscsDocument> allDocuments = combineEvidenceAndAppealPdf(caseData, pdfDocuments);
            SscsCaseData caseDataWithAppealPdf = caseData.toBuilder().sscsDocument(allDocuments).build();
            updateCaseInCcd(caseDataWithAppealPdf, caseId, "uploadDocument", idamTokens);
        }
    }

    private List<SscsDocument> combineEvidenceAndAppealPdf(SscsCaseData caseData, List<SscsDocument> pdfDocuments) {
        List<SscsDocument> evidenceDocuments = caseData.getSscsDocument();
        List<SscsDocument> allDocuments = new ArrayList<>();
        if (evidenceDocuments != null) {
            allDocuments.addAll(evidenceDocuments);
        }
        allDocuments.addAll(pdfDocuments);
        return allDocuments;
    }

    private SscsCaseDetails updateCaseInCcd(SscsCaseData caseData, Long caseId, String eventId, IdamTokens idamTokens) {
        try {
            return ccdService.updateCase(caseData, caseId, eventId, "SSCS - appeal updated event", "Updated SSCS", idamTokens);
        } catch (CcdException ccdEx) {
            log.error("Failed to update ccd case with direction notice but carrying on [" + caseId + "] ["
                + caseData.getCaseReference() + "] with event [" + eventId + "]", ccdEx);
            return SscsCaseDetails.builder().build();
        }
    }
}
