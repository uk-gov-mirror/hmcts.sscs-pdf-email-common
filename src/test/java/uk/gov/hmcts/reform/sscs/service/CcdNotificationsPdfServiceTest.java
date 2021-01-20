package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class CcdNotificationsPdfServiceTest {

    @InjectMocks
    private CcdNotificationsPdfService service;

    @Mock
    PdfStoreService pdfStoreService;

    @Mock
    CcdService ccdService;

    @Mock
    PDFServiceClient pdfServiceClient;

    @Mock
    IdamService idamService;

    SscsCaseData caseData = buildCaseData().toBuilder().ccdCaseId("123").build();
    private List<SscsDocument> sscsDocuments;

    @Before
    public void setup() {
        initMocks(this);
        sscsDocuments = new ArrayList<>();
        sscsDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("Test.jpg")
                .documentLink(DocumentLink.builder()
                        .documentUrl("aUrl")
                        .documentBinaryUrl("aUrl/binary")
                        .build())
                .build())
                .build());

        when(pdfStoreService.store(any(), any(), eq("dl6"))).thenReturn(sscsDocuments);
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().data(caseData).build());
        when(pdfServiceClient.generateFromHtml(any(), any())).thenReturn("bytes".getBytes());
    }

    @Test
    public void mergeCorrespondenceIntoCcd() {

        Correspondence correspondence = Correspondence.builder().value(
                CorrespondenceDetails.builder()
                        .sentOn("20 04 2019 11:00:00")
                        .from("from")
                        .to("to")
                        .body("the body")
                        .subject("a subject")
                        .eventType("event")
                        .correspondenceType(CorrespondenceType.Email)
                        .build()).build();

        service.mergeCorrespondenceIntoCcd(caseData, correspondence);
        verify(pdfServiceClient).generateFromHtml(any(), any());
        verify(pdfStoreService).store(any(), eq("event 20 04 2019 11:00:00.pdf"), eq(CorrespondenceType.Email.name()));
        verify(ccdService).updateCase(any(), any(), any(), eq("Notification sent"), eq("Notification sent via Gov Notify"), any());
    }

    @Test
    public void mergeLetterCorrespondenceIntoCcd() {
        byte[] bytes = "String".getBytes();
        Long caseId = Long.valueOf(caseData.getCcdCaseId());
        Correspondence correspondence = Correspondence.builder().value(
                CorrespondenceDetails.builder()
                        .sentOn("20 04 2019 11:00:00")
                        .from("from")
                        .to("to")
                        .subject("a subject")
                        .eventType("event")
                        .correspondenceType(CorrespondenceType.Email)
                        .build()).build();


        when(ccdService.getByCaseId(eq(caseId), eq(IdamTokens.builder().build()))).thenReturn(SscsCaseDetails.builder().data(caseData).build());
        service.mergeLetterCorrespondenceIntoCcd(bytes, caseId, correspondence);
        verify(pdfStoreService).store(any(), eq("event 20 04 2019 11:00:00.pdf"), eq(CorrespondenceType.Email.name()));
        verify(ccdService).updateCase(any(), any(), any(), eq("Notification sent"), eq("Notification sent via Gov Notify"), any());
    }

    @Test
    public void mergeReasonableAdjustmentsCorrespondenceIntoCcdWithPdfList() {
        byte[] bytes = "String".getBytes();
        Pdf pdf = new Pdf(bytes, "adocument");
        List<Pdf> pdfs = Collections.singletonList(pdf);
        Long caseId = Long.valueOf(caseData.getCcdCaseId());
        DocumentLink documentLink = DocumentLink.builder().documentUrl("Http://document").documentFilename("evidence-document.pdf").build();
        Correspondence correspondence = Correspondence.builder().value(
                CorrespondenceDetails.builder()
                        .sentOn("20 04 2019 11:00:00")
                        .from("from")
                        .to("to")
                        .subject("a subject")
                        .eventType("event")
                        .documentLink(documentLink)
                        .correspondenceType(CorrespondenceType.Letter)
                        .reasonableAdjustmentStatus(ReasonableAdjustmentStatus.REQUIRED)
                        .build()).build();


        when(ccdService.getByCaseId(eq(caseId), eq(IdamTokens.builder().build()))).thenReturn(SscsCaseDetails.builder().data(caseData).build());
        service.mergeReasonableAdjustmentsCorrespondenceIntoCcd(pdfs, caseId, correspondence);
        verify(pdfStoreService).store(any(), eq("event 20 04 2019 11:00:00.pdf"), eq(CorrespondenceType.Letter.name()));
        verify(ccdService).updateCase(any(), any(), any(), eq("Notification sent"), eq("Stopped for reasonable adjustment to be sent"), any());
    }

    @Test
    public void mergeReasonableAdjustmentsCorrespondenceIntoCcd() {
        byte[] bytes = "String".getBytes();
        Long caseId = Long.valueOf(caseData.getCcdCaseId());
        DocumentLink documentLink = DocumentLink.builder().documentUrl("Http://document").documentFilename("evidence-document.pdf").build();
        Correspondence correspondence = Correspondence.builder().value(
                CorrespondenceDetails.builder()
                        .sentOn("20 04 2019 11:00:00")
                        .from("from")
                        .to("to")
                        .subject("a subject")
                        .eventType("event")
                        .documentLink(documentLink)
                        .correspondenceType(CorrespondenceType.Letter)
                        .reasonableAdjustmentStatus(ReasonableAdjustmentStatus.REQUIRED)
                        .build()).build();


        when(ccdService.getByCaseId(eq(caseId), eq(IdamTokens.builder().build()))).thenReturn(SscsCaseDetails.builder().data(caseData).build());
        service.mergeReasonableAdjustmentsCorrespondenceIntoCcd(bytes, caseId, correspondence);
        verify(pdfStoreService).store(any(), eq("event 20 04 2019 11:00:00.pdf"), eq(CorrespondenceType.Letter.name()));
        verify(ccdService).updateCase(any(), any(), any(), eq("Notification sent"), eq("Stopped for reasonable adjustment to be sent"), any());
    }
}
