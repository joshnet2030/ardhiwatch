package com.josh.ardhi.utils;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationHighlight;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

public class PdfHighlighter extends PDFTextStripper {

    private String[] criteria;

    public PdfHighlighter() throws IOException {
        super();
    }

    public byte[] highlightToBytes(File sourceFile, String[] wordsToHighlight) throws IOException {
        this.criteria = wordsToHighlight;

        try (PDDocument document = Loader.loadPDF(sourceFile)) {
            this.setSortByPosition(true);
            this.setStartPage(0);
            this.setEndPage(document.getNumberOfPages());

            Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
            this.writeText(document, dummy);

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                document.save(out);
                return out.toByteArray();
            }
        }
    }

    @Override
    protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        if (criteria == null || string == null || textPositions == null || textPositions.isEmpty()) {
            return;
        }

        String lowerString = string.toLowerCase();

        for (int i = 0; i < criteria.length; i++) {
            if (criteria[i] == null || criteria[i].isBlank()) {
                continue;
            }
            String target = criteria[i].trim().toLowerCase();
            if (target.isEmpty()) {
                continue;
            }

            int searchFrom = 0;
            boolean matchedThisCriterion = false;
            while (searchFrom < lowerString.length()) {
                int startPosition = lowerString.indexOf(target, searchFrom);
                if (startPosition < 0 || startPosition >= textPositions.size()) {
                    break;
                }

                int endPosition = Math.min(textPositions.size(), startPosition + target.length());
                if (endPosition <= startPosition) {
                    break;
                }

                float posXInit = textPositions.get(startPosition).getXDirAdj();
                float posXEnd = textPositions.get(endPosition - 1).getXDirAdj() + textPositions.get(endPosition - 1).getWidth();
                float posYInit = textPositions.get(startPosition).getPageHeight() - textPositions.get(startPosition).getYDirAdj();
                float posYEnd = textPositions.get(startPosition).getPageHeight() - textPositions.get(endPosition - 1).getYDirAdj();
                float height = textPositions.get(startPosition).getHeightDir();

                float[] quadPoints = new float[]{
                        posXInit, posYEnd + height + 2,
                        posXEnd, posYEnd + height + 2,
                        posXInit, posYInit - 2,
                        posXEnd, posYEnd - 2
                };

                List<PDAnnotation> annotations = document.getPage(this.getCurrentPageNo() - 1).getAnnotations();
                PDAnnotationHighlight highlight = new PDAnnotationHighlight();


                PDRectangle position = new PDRectangle();
                position.setLowerLeftX(posXInit);
                position.setLowerLeftY(posYEnd);
                position.setUpperRightX(posXEnd);
                position.setUpperRightY(posYEnd + height);

                highlight.setRectangle(position);
                highlight.setQuadPoints(quadPoints);

                float[] components = new float[]{1f, 1f, 100 / 255f};
                PDColor yellow = new PDColor(components, PDDeviceRGB.INSTANCE);
                highlight.setColor(yellow);
                annotations.add(highlight);

                matchedThisCriterion = true;
                searchFrom = endPosition;
            }

            // If the full phrase (first criterion) matched in this string, no need to also highlight its sub-words
            if (i == 0 && matchedThisCriterion) {
                break;
            }
        }
    }
}
