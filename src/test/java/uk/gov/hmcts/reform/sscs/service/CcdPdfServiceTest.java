package uk.gov.hmcts.reform.sscs.service;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPLOAD_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class CcdPdfServiceTest {

    @InjectMocks
    private CcdPdfService service;

    @Mock
    PdfStoreService pdfStoreService;

    @Mock
    CcdService ccdService;

    private SscsCaseData caseData = buildCaseData();

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void mergeValidPdfAndStoreInDocumentStore() {
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentFileName("Test.jpg")
                .build())
            .build());

        when(pdfStoreService.store(any(), any(), eq("dl6"))).thenReturn(sscsDocuments);
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any()))
            .thenReturn(SscsCaseDetails.builder().data(caseData).build());

        byte[] pdf = {};
        service.mergeDocIntoCcd("Myfile.pdf", pdf, 1L, caseData, IdamTokens.builder().build(),
            "dl6");

        verify(pdfStoreService).store(any(), any(), eq("dl6"));
        verify(ccdService).updateCase(any(), any(), any(), eq("SSCS - upload document event"),
            eq("Uploaded document into SSCS"), any());
        assertEquals("Test.jpg", caseData.getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    @Test
    @Parameters(method = "generateScenariosForSscsDocuments")
    public void givenAppellantStatement_shouldMergeDocIntoCcd(List<SscsDocument> sscsDocuments,
                                                              int expectedNumberOfScannedDocs) {
        when(pdfStoreService.store(any(), any(), anyString())).thenReturn(sscsDocuments);
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any()))
            .thenReturn(SscsCaseDetails.builder().data(caseData).build());

        service.mergeDocIntoCcd("Appellant statement 1 - SC0011111.pdf", new byte[0], 1L,
            caseData, IdamTokens.builder().build(), "Other evidence");

        verify(pdfStoreService, times(1))
            .store(any(), eq("Appellant statement 1 - SC0011111.pdf"), eq("Other evidence"));

        ArgumentCaptor<SscsCaseData> caseDataCaptor = ArgumentCaptor.forClass(SscsCaseData.class);
        verify(ccdService, times(1))
            .updateCase(caseDataCaptor.capture(), eq(1L), eq(UPLOAD_DOCUMENT.getCcdType()), anyString(),
                anyString(), any(IdamTokens.class));

        assertThat(caseDataCaptor.getValue().getScannedDocuments().size(), is(expectedNumberOfScannedDocs));
        Optional<ScannedDocument> scannedDocument = caseDataCaptor.getValue().getScannedDocuments().stream()
            .filter(doc -> "Appellant statement 1 - SC0011111.pdf" .equals(doc.getValue().getFileName()))
            .findFirst();
        if (scannedDocument.isPresent()) {
            ScannedDocument expectedScannedDoc = ScannedDocument.builder()
                .value(ScannedDocumentDetails.builder()
                    .fileName("Appellant statement 1 - SC0011111.pdf")
                    .url(DocumentLink.builder().documentUrl("http://dm-store").build())
                    .type("other")
                    .build())
                .build();
            assertThat(scannedDocument.get(), is(expectedScannedDoc));
        }
    }

    private Object[] generateScenariosForSscsDocuments() {
        SscsDocumentDetails sscsDocumentDetails = SscsDocumentDetails.builder()
            .documentFileName("Appellant statement 1 - SC0011111.pdf")
            .documentDateAdded(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
            .documentLink(DocumentLink.builder().documentUrl("http://dm-store").build())
            .documentType("Other evidence")
            .build();

        SscsDocument pdfDocument = SscsDocument.builder().value(sscsDocumentDetails).build();
        List<SscsDocument> sscsDocumentsWithOneSingleDoc = singletonList(pdfDocument);

        int expectedNumberOfScannedDocsIsOne = 1;
        int expectedNumberOfScannedDocsIsZero = 0;
        return new Object[]{
            new Object[]{sscsDocumentsWithOneSingleDoc, expectedNumberOfScannedDocsIsOne},
            new Object[]{Collections.emptyList(), expectedNumberOfScannedDocsIsZero}
        };
    }

    @Test
    public void mergeValidPdfAndStoreInDocumentStoreWithDescription() {
        byte[] pdf = {};
        when(ccdService.updateCase(any(), any(), any(), any(), any(), any())).thenReturn(SscsCaseDetails.builder().data(caseData).build());

        service.mergeDocIntoCcd("Myfile.pdf", pdf, 1L, caseData, IdamTokens.builder().build(), "My description", "dl6");

        verify(pdfStoreService).store(any(), any(), eq("dl6"));
        verify(ccdService).updateCase(any(), any(), any(), eq("SSCS - upload document event"), eq("My description"), any());
    }
}
