package uk.gov.hmcts.reform.sscs.helper;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.Test;

public class PdfHelperTest {

    PdfHelper pdfHelper = new PdfHelper();

    @Test
    public void returnFalseWhenPdfHasPageGreaterThanAllowedSizePortrait() {

        PDRectangle pageSize = PDRectangle.A4;
        PDDocument document = new PDDocument();
        document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() + 1, pageSize.getHeight() + 1)));

        boolean result = pdfHelper.isDocumentWithinSize(document, pageSize);
        assertFalse(result);
    }

    @Test
    public void returnFalseWhenPdfHasPageGreaterThanAllowedSizeLandscape() {
        PDRectangle pageSize = PDRectangle.A4;
        PDDocument document = new PDDocument();
        document.addPage(new PDPage(new PDRectangle(pageSize.getHeight() + 1, pageSize.getWidth() + 1)));

        boolean result = pdfHelper.isDocumentWithinSize(document, pageSize);
        assertFalse(result);
    }

    @Test
    public void returnTrueWhenPdfHasNoPagesGreaterThanAllowedSizePortrait() {
        PDRectangle pageSize = PDRectangle.A4;
        PDDocument document = new PDDocument();
        document.addPage(new PDPage(new PDRectangle(pageSize.getWidth(), pageSize.getHeight())));

        boolean result = pdfHelper.isDocumentWithinSize(document, pageSize);
        assertTrue(result);
    }

    @Test
    public void returnTrueWhenPdfHasNoPagesGreaterThanAllowedSizeLandscape() {
        PDRectangle pageSize = PDRectangle.A4;
        PDDocument document = new PDDocument();
        document.addPage(new PDPage(new PDRectangle(pageSize.getHeight(), pageSize.getWidth())));

        boolean result = pdfHelper.isDocumentWithinSize(document, pageSize);
        assertTrue(result);
    }

    @Test
    public void returnCorrectMaxScalingFactorForOversizedPortraitWidth() {
        PDRectangle pageSize = PDRectangle.A4;
        PDDocument document = new PDDocument();
        document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() + 100, pageSize.getHeight() + 50)));

        BigDecimal result = pdfHelper.calculateScalingFactor(document, pageSize);
        assertEquals(new BigDecimal(0.8562).setScale(4, RoundingMode.HALF_EVEN), result);
    }

    @Test
    public void returnCorrectMaxScalingFactorForOversizedPortraitWidthExtraLarge() {
        PDRectangle pageSize = PDRectangle.A4;
        PDDocument document = new PDDocument();
        document.addPage(new PDPage(new PDRectangle(PDRectangle.A1.getWidth(), PDRectangle.A1.getHeight())));

        BigDecimal result = pdfHelper.calculateScalingFactor(document, pageSize);
        assertEquals(new BigDecimal(0.3532).setScale(4, RoundingMode.HALF_EVEN), result);
    }

    @Test
    public void returnCorrectMaxScalingFactorForOversizedPortraitHeight() {
        PDRectangle pageSize = PDRectangle.A4;
        PDDocument document = new PDDocument();
        document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() + 50, pageSize.getHeight() + 100)));

        BigDecimal result = pdfHelper.calculateScalingFactor(document, pageSize);
        assertEquals(new BigDecimal(0.8938).setScale(4, RoundingMode.HALF_EVEN), result);
    }

    @Test
    public void scaleDownLandscapeDocumentCorrectly() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("A3 Landscape.pdf"));

        PDRectangle pageSize = PDRectangle.A4;
        PDDocument document = PDDocument.load(pdfBytes);

        BigDecimal scaleDownFactor = pdfHelper.calculateScalingFactor(document, pageSize);

        PDDocument result = pdfHelper.scaleDownDocumentToPageSize(document, scaleDownFactor, pageSize);

        PDRectangle mediaBox = result.getPage(0).getMediaBox();

        assertTrue(pageSize.getHeight() >= mediaBox.getWidth());
        assertTrue(pageSize.getWidth() >= mediaBox.getHeight());
    }

    @Test
    public void scaleDownPortraitDocumentCorrectly() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("another-test.pdf"));

        PDRectangle pageSize = PDRectangle.A4;
        PDDocument document = PDDocument.load(pdfBytes);

        BigDecimal scaleDownFactor = pdfHelper.calculateScalingFactor(document, pageSize);

        PDDocument result = pdfHelper.scaleDownDocumentToPageSize(document, scaleDownFactor, pageSize);

        PDRectangle mediaBox = result.getPage(0).getMediaBox();

        assertTrue(pageSize.getHeight() >= mediaBox.getHeight());
        assertTrue(pageSize.getWidth() >= mediaBox.getWidth());
    }

    @Test
    public void doesResizeDocumentWhichDoesNeedResizing() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("another-test.pdf"));

        PDDocument document = PDDocument.load(pdfBytes);
        PDRectangle pageSize = PDRectangle.A4;
        PDDocument result = pdfHelper.scaleToPageSize(document, pageSize);

        PDRectangle mediaBox = result.getPage(0).getMediaBox();

        assertTrue(pageSize.getHeight() >= mediaBox.getHeight());
        assertTrue(pageSize.getWidth() >= mediaBox.getWidth());
    }

    @Test
    public void doesNotResizeDocumentWhichDoesNotNeedResizing() throws Exception {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("another-test.pdf"));

        PDDocument document = PDDocument.load(pdfBytes);

        PDRectangle pageSize = PDRectangle.A3;
        PDDocument result = pdfHelper.scaleToPageSize(document, pageSize);

        assertEquals(document, result);
    }

    @Test
    public void doesResizeA4DocumentWhichDoesNeedResizing() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("another-test.pdf"));

        PDDocument document = PDDocument.load(pdfBytes);

        PDDocument result = pdfHelper.scaleToA4(document);

        PDRectangle mediaBox = result.getPage(0).getMediaBox();
        PDRectangle pageSize = PDRectangle.A4;
        assertTrue(pageSize.getHeight() >= mediaBox.getHeight());
        assertTrue(pageSize.getWidth() >= mediaBox.getWidth());
    }
}
