package uk.gov.hmcts.reform.sscs.helper;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

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
        document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() + 100, pageSize.getHeight() + 100)));

        boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
        assertFalse(result);
    }

    @Test
    public void returnFalseWhenPdfHasPageGreaterThanAllowedSizeLandscape() {
        PDRectangle pageSize = PDRectangle.A4;
        PDDocument document = new PDDocument();
        document.addPage(new PDPage(new PDRectangle(pageSize.getHeight() + 100, pageSize.getWidth() + 100)));

        boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
        assertFalse(result);
    }

    @Test
    public void returnFalseWhenPdfHasPageLowerThanAllowedSizePortrait() {

        PDRectangle pageSize = PDRectangle.A4;
        PDDocument document = new PDDocument();
        document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() - 100, pageSize.getHeight() - 100)));

        boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
        assertFalse(result);
    }

    @Test
    public void returnFalseWhenPdfHasPageLowerThanAllowedSizeLandscape() {
        PDRectangle pageSize = PDRectangle.A4;
        PDDocument document = new PDDocument();
        document.addPage(new PDPage(new PDRectangle(pageSize.getHeight() - 100, pageSize.getWidth() - 100)));

        boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
        assertFalse(result);
    }

    @Test
    public void returnTrueWhenPdfHasNoPagesGreaterThanAllowedSizePortrait() {
        PDRectangle pageSize = PDRectangle.A4;
        PDDocument document = new PDDocument();
        document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() + 5, pageSize.getHeight())));

        boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
        assertTrue(result);
    }

    @Test
    public void returnTrueWhenPdfHasNoPagesGreaterThanAllowedSizeLandscape() {
        PDRectangle pageSize = PDRectangle.A4;
        PDDocument document = new PDDocument();
        document.addPage(new PDPage(new PDRectangle(pageSize.getHeight() + 5, pageSize.getWidth())));

        boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
        assertTrue(result);
    }

    @Test
    public void returnTrueWhenPdfHasNoPagesLowerThanAllowedSizePortrait() {
        PDRectangle pageSize = PDRectangle.A4;
        PDDocument document = new PDDocument();
        document.addPage(new PDPage(new PDRectangle(pageSize.getWidth() - 5, pageSize.getHeight())));

        boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
        assertTrue(result);
    }

    @Test
    public void returnTrueWhenPdfHasNoPagesLowerThanAllowedSizeLandscape() {
        PDRectangle pageSize = PDRectangle.A4;
        PDDocument document = new PDDocument();
        document.addPage(new PDPage(new PDRectangle(pageSize.getHeight() - 5, pageSize.getWidth())));

        boolean result = pdfHelper.isDocumentWithinSizeTolerance(document, pageSize);
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
    public void scalePageUpCorrectlyPortrait() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("another-test.pdf"));

        PDRectangle pageSize = PDRectangle.A2;
        PDDocument document = PDDocument.load(pdfBytes);

        PDDocument result = pdfHelper.scaleUpPageSize(document, pageSize);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        result.save(baos);

        FileOutputStream fos = new FileOutputStream(new File("upsized.pdf"));

        baos.writeTo(fos);
        fos.close();

        PDRectangle mediaBox = result.getPage(0).getMediaBox();

        assertTrue(pageSize.getHeight() == mediaBox.getHeight());
        assertTrue(pageSize.getWidth() == mediaBox.getWidth());
    }

    @Test
    public void scalePageUpCorrectlyLandscape() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("A3 Landscape.pdf"));

        PDRectangle pageSize = PDRectangle.A2;
        PDDocument document = PDDocument.load(pdfBytes);

        PDDocument result = pdfHelper.scaleUpPageSize(document, pageSize);

        PDRectangle mediaBox = result.getPage(0).getMediaBox();

        assertTrue(pageSize.getHeight() == mediaBox.getWidth());
        assertTrue(pageSize.getWidth() == mediaBox.getHeight());
    }

    @Test
    public void doesResizeDocumentWhichDoesNeedResizing() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("another-test.pdf"));

        PDDocument document = PDDocument.load(pdfBytes);
        PDRectangle pageSize = PDRectangle.A4;
        Optional<PDDocument> result = pdfHelper.scaleToPageSize(document, pageSize);

        assertTrue(result.isPresent());
        PDRectangle mediaBox = result.get().getPage(0).getMediaBox();

        assertTrue(pageSize.getHeight() >= mediaBox.getHeight());
        assertTrue(pageSize.getWidth() >= mediaBox.getWidth());
    }

    @Test
    public void doesNotResizeDocumentWhichDoesNotNeedResizing() throws Exception {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("A3 Portrait.pdf"));

        PDDocument document = PDDocument.load(pdfBytes);

        PDRectangle pageSize = PDRectangle.A3;
        Optional<PDDocument> result = pdfHelper.scaleToPageSize(document, pageSize);

        assertTrue(result.isEmpty());
    }

    @Test
    public void doesResizeA4DocumentWhichDoesNeedResizing() throws Exception {

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("another-test.pdf"));

        PDDocument document = PDDocument.load(pdfBytes);

        Optional<PDDocument> result = pdfHelper.scaleToA4(document);

        assertTrue(result.isPresent());
        PDRectangle mediaBox = result.get().getPage(0).getMediaBox();
        PDRectangle pageSize = PDRectangle.A4;
        assertTrue(pageSize.getHeight() >= mediaBox.getHeight());
        assertTrue(pageSize.getWidth() >= mediaBox.getWidth());
    }
}
