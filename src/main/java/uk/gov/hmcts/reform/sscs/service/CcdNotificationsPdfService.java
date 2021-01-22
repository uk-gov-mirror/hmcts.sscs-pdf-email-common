package uk.gov.hmcts.reform.sscs.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class CcdNotificationsPdfService {

    @Autowired
    private PdfStoreService pdfStoreService;

    @Autowired
    private PDFServiceClient pdfServiceClient;

    @Autowired
    private CcdService ccdService;

    @Autowired
    private IdamService idamService;


    public SscsCaseData mergeCorrespondenceIntoCcd(SscsCaseData sscsCaseData, Correspondence correspondence) {
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("body", correspondence.getValue().getBody());
        placeholders.put("subject", correspondence.getValue().getSubject());
        placeholders.put("sentOn", correspondence.getValue().getSentOn());
        placeholders.put("from", correspondence.getValue().getFrom());
        placeholders.put("to", correspondence.getValue().getTo());

        byte[] template;
        try {
            template = getSentEmailTemplate();
        } catch (IOException e) {
            throw new PdfGenerationException("Error getting template", e);
        }

        byte[] pdf = pdfServiceClient.generateFromHtml(template, placeholders);
        String filename = String.format("%s %s.pdf", correspondence.getValue().getEventType(), correspondence.getValue().getSentOn());
        List<SscsDocument> pdfDocuments = pdfStoreService.store(pdf, filename, correspondence.getValue().getCorrespondenceType().name());
        final List<Correspondence> correspondences = pdfDocuments.stream().map(doc ->
                correspondence.toBuilder().value(correspondence.getValue().toBuilder()
                        .documentLink(doc.getValue().getDocumentLink())
                        .build()).build()
        ).collect(Collectors.toList());

        List<Correspondence> existingCorrespondence = sscsCaseData.getCorrespondence() == null ? new ArrayList<>() : sscsCaseData.getCorrespondence();
        List<Correspondence> allCorrespondence = new ArrayList<>(existingCorrespondence);
        allCorrespondence.addAll(correspondences);
        allCorrespondence.sort(Comparator.reverseOrder());
        sscsCaseData.setCorrespondence(allCorrespondence);

        IdamTokens idamTokens = idamService.getIdamTokens();
        SscsCaseDetails caseDetails = updateCaseInCcd(sscsCaseData, Long.parseLong(sscsCaseData.getCcdCaseId()), EventType.NOTIFICATION_SENT.getCcdType(),
                idamTokens, "Notification sent via Gov Notify");

        return caseDetails.getData();
    }

    public SscsCaseData mergeLetterCorrespondenceIntoCcd(byte[] pdf, Long ccdCaseId, Correspondence correspondence) {
        String filename = String.format("%s %s.pdf", correspondence.getValue().getEventType(), correspondence.getValue().getSentOn());
        List<SscsDocument> pdfDocuments = pdfStoreService.store(pdf, filename, correspondence.getValue().getCorrespondenceType().name());
        final List<Correspondence> correspondences = pdfDocuments.stream().map(doc ->
                correspondence.toBuilder().value(correspondence.getValue().toBuilder()
                        .documentLink(doc.getValue().getDocumentLink())
                        .build()).build()
        ).collect(Collectors.toList());

        IdamTokens idamTokens = idamService.getIdamTokens();
        final SscsCaseDetails sscsCaseDetails = ccdService.getByCaseId(ccdCaseId, idamTokens);
        final SscsCaseData sscsCaseData = sscsCaseDetails.getData();

        List<Correspondence> existingCorrespondence = sscsCaseData.getCorrespondence() == null ? new ArrayList<>() : sscsCaseData.getCorrespondence();
        List<Correspondence> allCorrespondence = new ArrayList<>(existingCorrespondence);
        allCorrespondence.addAll(correspondences);
        allCorrespondence.sort(Comparator.reverseOrder());
        sscsCaseData.setCorrespondence(allCorrespondence);

        SscsCaseDetails caseDetails = updateCaseInCcd(sscsCaseData, Long.parseLong(sscsCaseData.getCcdCaseId()), EventType.NOTIFICATION_SENT.getCcdType(),
                idamTokens, "Notification sent via Gov Notify");

        return caseDetails.getData();
    }

    public SscsCaseData mergeReasonableAdjustmentsCorrespondenceIntoCcd(List<Pdf> pdfs, Long ccdCaseId, Correspondence correspondence) {
        PDFMergerUtility merger = new PDFMergerUtility();
        for (Pdf pdf : pdfs) {
            merger.addSource(new ByteArrayInputStream(pdf.getContent()));
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        merger.setDestinationStream(baos);
        try {
            merger.mergeDocuments(null);
        } catch (IOException e) {
            log.error("Failed to create pdf of letter for {}", ccdCaseId, e);
        }

        byte[] letterDocument = baos.toByteArray();

        return mergeReasonableAdjustmentsCorrespondenceIntoCcd(letterDocument, ccdCaseId, correspondence);
    }

    public SscsCaseData mergeReasonableAdjustmentsCorrespondenceIntoCcd(byte[] letterDocument, Long ccdCaseId, Correspondence correspondence) {
        String filename = String.format("%s %s.pdf", correspondence.getValue().getEventType(), correspondence.getValue().getSentOn());

        List<SscsDocument> pdfDocuments = pdfStoreService.store(letterDocument, filename, correspondence.getValue().getCorrespondenceType().name());

        final List<Correspondence> correspondences = pdfDocuments.stream().map(doc ->
                correspondence.toBuilder().value(correspondence.getValue().toBuilder()
                        .documentLink(doc.getValue().getDocumentLink())
                        .build()).build()
        ).collect(Collectors.toList());

        IdamTokens idamTokens = idamService.getIdamTokens();
        final SscsCaseDetails sscsCaseDetails = ccdService.getByCaseId(ccdCaseId, idamTokens);
        final SscsCaseData sscsCaseData = sscsCaseDetails.getData();

        List<Correspondence> existingCorrespondence = sscsCaseData.getReasonableAdjustmentsLetters() == null ? new ArrayList<>() : sscsCaseData.getReasonableAdjustmentsLetters();
        List<Correspondence> allCorrespondence = new ArrayList<>(existingCorrespondence);
        allCorrespondence.addAll(correspondences);
        allCorrespondence.sort(Comparator.reverseOrder());
        sscsCaseData.setReasonableAdjustmentsLetters(allCorrespondence);
        sscsCaseData.setReasonableAdjustmentsOutstanding(YesNo.YES);
        log.info("Creating a reasonable adjustment for {}", ccdCaseId);

        SscsCaseDetails caseDetails = updateCaseInCcd(sscsCaseData, Long.parseLong(sscsCaseData.getCcdCaseId()), EventType.NOTIFICATION_SENT.getCcdType(),
                idamTokens, "Stopped for reasonable adjustment to be sent");

        return caseDetails.getData();
    }

    private SscsCaseDetails updateCaseInCcd(SscsCaseData caseData, Long caseId, String eventId, IdamTokens idamTokens, String description) {
        try {
            return ccdService.updateCase(caseData, caseId, eventId, "Notification sent", description, idamTokens);
        } catch (CcdException ccdEx) {
            log.error("Failed to update ccd case but carrying on [" + caseId + "] ["
                    + caseData.getCaseReference() + "] with event [" + eventId + "]", ccdEx);
            return SscsCaseDetails.builder().build();
        }
    }

    private byte[] getSentEmailTemplate() throws IOException {
        InputStream in = getClass().getResourceAsStream("/templates/sent_notification.html");
        return IOUtils.toByteArray(in);
    }

}
