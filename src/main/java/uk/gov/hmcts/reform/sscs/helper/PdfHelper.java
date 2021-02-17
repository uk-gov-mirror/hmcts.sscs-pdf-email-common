package uk.gov.hmcts.reform.sscs.helper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PdfHelper {

    public PDDocument scaleToA4(PDDocument document) throws Exception {
        return scaleToPageSize(document, PDRectangle.A4);
    }

    public PDDocument scaleToPageSize(PDDocument document, PDRectangle size) throws Exception {

        boolean isWithinPageSize = isDocumentWithinSize(document, size);

        if (isWithinPageSize) {
            return document;
        } else {
            BigDecimal scalingFactor = calculateScalingFactor(document, size);
            PDDocument resizedDoc = scaleDownDocumentToPageSize(document, scalingFactor, size);
            return resizedDoc;
        }
    }

    public boolean isDocumentWithinSize(PDDocument document, PDRectangle size) {

        for (PDPage page: document.getPages()) {
            float pageHeight = page.getMediaBox().getHeight();
            float pageWidth = page.getMediaBox().getWidth();

            if (pageHeight > pageWidth) {
                if (pageHeight > size.getHeight() || pageWidth > size.getWidth()) {
                    return false;
                }
            } else {
                if (pageWidth > size.getHeight() || pageHeight > size.getWidth()) {
                    return false;
                }
            }
        }
        return true;
    }

    public BigDecimal calculateScalingFactor(PDDocument document, PDRectangle size) {

        BigDecimal maxHeightScaling = new BigDecimal(0);
        BigDecimal maxWidthScaling = new BigDecimal(0);
        float heightOverage;
        float widthOverage;

        for (PDPage page: document.getPages()) {
            float pageHeight = page.getMediaBox().getHeight();
            float pageWidth = page.getMediaBox().getWidth();
            float sizeHeight;
            float sizeWidth;

            if (pageHeight > pageWidth) {
                sizeHeight = size.getHeight();
                sizeWidth = size.getWidth();
            } else {
                sizeHeight = size.getWidth();
                sizeWidth = size.getHeight();
            }

            log.debug("A4 height limit = " + sizeHeight);
            log.debug("A4 width limit = " + sizeWidth);

            log.debug("Page Height = " + pageHeight);
            log.debug("Page Width = " + pageWidth);

            heightOverage = pageHeight - sizeHeight;
            log.debug("height overage = " + heightOverage);
            widthOverage = pageWidth - sizeWidth;
            log.debug("width overage = " + widthOverage);

            maxHeightScaling = maxScaleFactor(heightOverage, pageHeight, maxHeightScaling);
            log.debug("max height scaling = " + maxHeightScaling);
            maxWidthScaling = maxScaleFactor(widthOverage, pageWidth, maxWidthScaling);
            log.debug("max width scaling = " + maxWidthScaling);
        }

        BigDecimal scalingFactor = maxHeightScaling.compareTo(maxWidthScaling) > 0 ? maxHeightScaling : maxWidthScaling;

        return new BigDecimal(1).subtract(scalingFactor).setScale(4, RoundingMode.HALF_EVEN);
    }

    private BigDecimal maxScaleFactor(float overage, float pageAxisDimension, BigDecimal maxScaleFactor) {
        if (overage > 0) {
            BigDecimal scaleFactor = new BigDecimal(overage / pageAxisDimension);
            maxScaleFactor = scaleFactor.compareTo(maxScaleFactor) > 0 ? scaleFactor : maxScaleFactor;
        }
        return  maxScaleFactor;
    }

    public PDDocument scaleDownDocumentToPageSize(PDDocument document, BigDecimal scaleDownFactor, PDRectangle size) throws Exception {
        try {
            for (PDPage page : document.getPages()) {
                PDRectangle newSize = page.getMediaBox().getHeight() > page.getMediaBox().getWidth() ? size : new PDRectangle(size.getHeight(), size.getWidth());
                page.setMediaBox(newSize);
                page.setCropBox(newSize);
                page.setBleedBox(newSize);
                page.setTrimBox(newSize);
                page.setArtBox(newSize);
                scaleContent(document, page, scaleDownFactor.floatValue());
            }
        } catch (Exception e) {
            throw e;
        }
        return document;
    }

    private void scaleContent(PDDocument document, PDPage page, float percentage) throws IOException {

        // First create a content stream before all others, add a matrix transformation to scale:
        try (PDPageContentStream contentStream =
                     new PDPageContentStream(document, page, PDPageContentStream.AppendMode.PREPEND, false)) {

            contentStream.saveGraphicsState(); // 'q' in PDF commands
            contentStream.transform(new Matrix(percentage, 0, 0, percentage, 0, 0));
            contentStream.saveGraphicsState();
        }

        // Now add a closing command to remove the scale effect by restoring the graphics states:
        try (PDPageContentStream contentStream =
                     new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, false)) {
            // In raw PDF this equates to: "\nQ\nQ\n" - we saved it twice so we have to restore twice
            contentStream.restoreGraphicsState();
            contentStream.restoreGraphicsState();
        }
    }
}
