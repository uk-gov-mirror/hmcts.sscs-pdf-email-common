package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.config.PDFTemplateConstants.*;

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
import uk.gov.hmcts.reform.sscs.domain.pdf.PdfWrapper;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class SscsGeneratePdfService {
    private static final String RPC = "rpc_";
    private PDFServiceClient pdfServiceClient;
    private PdfStoreService pdfStoreService;

    @Autowired
    public SscsGeneratePdfService(PDFServiceClient pdfServiceClient, PdfStoreService pdfStoreService) {
        this.pdfServiceClient = pdfServiceClient;
        this.pdfStoreService = pdfStoreService;
    }

    public byte[] generatePdf(String templatePath, SscsCaseData sscsCaseData, Long caseDetailsId, Map<String, String> notificationPlaceholders, IdamTokens idamTokens) {
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
        placeholders.put(RPC + REGIONAL_OFFICE_NAME_LITERAL, notificationPlaceholders.get(REGIONAL_OFFICE_NAME_LITERAL));
        placeholders.put(RPC + SUPPORT_CENTRE_NAME_LITERAL, notificationPlaceholders.get(SUPPORT_CENTRE_NAME_LITERAL));
        placeholders.put(RPC + ADDRESS_LINE_LITERAL, notificationPlaceholders.get(ADDRESS_LINE_LITERAL));
        placeholders.put(RPC + TOWN_LITERAL, notificationPlaceholders.get(TOWN_LITERAL));
        placeholders.put(RPC + COUNTY_LITERAL, notificationPlaceholders.get(COUNTY_LITERAL));
        placeholders.put(RPC + POSTCODE_LITERAL, notificationPlaceholders.get(POSTCODE_LITERAL));
        placeholders.put(RPC + PHONE_NUMBER, notificationPlaceholders.get(PHONE_NUMBER));

        return pdfServiceClient.generateFromHtml(template, placeholders);
    }

    // TODO: Use representative name and not Appellant name in address
    private static String getRepFullName(Representative representative) {
        if (representative != null && representative.getName() != null) {
            return representative.getName().getFullName();
        } else {
            return null;
        }
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
            SscsDocument document = SscsDocument.builder().
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

}
