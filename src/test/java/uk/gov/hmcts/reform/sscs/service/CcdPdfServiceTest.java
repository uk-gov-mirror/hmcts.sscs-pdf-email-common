package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

public class CcdPdfServiceTest {

    @InjectMocks
    private CcdPdfService service;

    @Mock
    PdfStoreService pdfStoreService;

    @Mock
    CcdService ccdService;

    @Mock
    PDFServiceClient pdfServiceClient;

    @Mock
    IdamService idamService;

    SscsCaseData caseData = buildCaseData().toBuilder().ccdCaseId("123").build();

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void mergeValidPdfAndStoreInDocumentStore() {
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("Test.jpg").build()).build());

        when(pdfStoreService.store(any(), any(), eq("dl6"))).thenReturn(sscsDocuments);
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().data(caseData).build());

        byte[] pdf = {};
        service.mergeDocIntoCcd("Myfile.pdf", pdf,1L, caseData, IdamTokens.builder().build(), "dl6");

        verify(pdfStoreService).store(any(), any(), eq("dl6"));
        verify(ccdService).updateCase(any(), any(), any(), eq("SSCS - upload document event"), eq("Uploaded document into SSCS"), any());
        assertEquals("Test.jpg", caseData.getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    @Test
    public void mergeValidPdfAndStoreInDocumentStoreWithDescription() {
        byte[] pdf = {};
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().data(caseData).build());

        service.mergeDocIntoCcd("Myfile.pdf", pdf,1L, caseData, IdamTokens.builder().build(), "My description", "dl6");

        verify(pdfStoreService).store(any(), any(), eq("dl6"));
        verify(ccdService).updateCase(any(), any(), any(), eq("SSCS - upload document event"), eq("My description"), any());
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
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(SscsDocument.builder().value(SscsDocumentDetails.builder().
                documentFileName("Test.jpg")
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

        service.mergeCorrespondenceIntoCcd(caseData, correspondence);
        verify(pdfServiceClient).generateFromHtml(any(), any());
        verify(pdfStoreService).store(any(), eq("event 20 04 2019 11:00:00.pdf"), eq(CorrespondenceType.Email.name()));
        verify(ccdService).updateCase(any(), any(), any(), eq("SSCS - upload document event"), eq("added correspondence"), any());

    }
}
