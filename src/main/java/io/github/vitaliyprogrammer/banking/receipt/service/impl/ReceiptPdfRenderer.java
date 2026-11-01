package io.github.vitaliyprogrammer.banking.receipt.service.impl;

import io.github.vitaliyprogrammer.banking.receipt.dto.response.ReceiptDocumentDto;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Draws a receipt as a one-page PDF that looks like the approved HTML mockup: white paper with a torn zigzag
 * bottom edge and a soft shadow, lying on a light desk. PDF has no CSS, so every element is placed by hand.
 * <p>
 * Coordinates: PDF counts y from the bottom of the page; this class thinks top-down, like the mockup,
 * and converts in one place ({@link Painter#pdfY}). The page is drawn twice: a dry run measures the height,
 * then the real run draws, so the page is exactly as tall as the receipt needs.
 * <p>
 * The logo is replaceable: put a square {@code receipt/logo.png} on the classpath and it is used instead of
 * the drawn "DB" mark.
 */
@Component
public class ReceiptPdfRenderer {

    private static final String FONTS = "receipt/fonts/";

    private static final String LOGO = "receipt/logo.png";

    private static final String STAMP_NAME = "Taborovets Universal Bank";

    private static final float PAGE_WIDTH = 400;

    private static final float PAPER_X = 40;

    private static final float PAPER_WIDTH = PAGE_WIDTH - 2 * PAPER_X;

    private static final float PAPER_TOP = 36;

    /** Desk below the teeth: room for the shadow. */
    private static final float DESK_BELOW = 52;

    private static final float PADDING = 24;

    private static final float CONTENT_X = PAPER_X + PADDING;

    private static final float CONTENT_RIGHT = PAPER_X + PAPER_WIDTH - PADDING;

    private static final float LABEL_WIDTH = 92;

    private static final float TOOTH_WIDTH = 16;

    private static final float TOOTH_HEIGHT = 8;

    private static final Color DESK = new Color(0xE7EBF0);

    private static final Color PAPER = Color.WHITE;

    private static final Color INK = new Color(0x1C2330);

    private static final Color MUTED = new Color(0x6A7483);

    private static final Color LINE = new Color(0xD3D9E1);

    private static final Color BRAND = new Color(0x0B5C6E);

    private static final Color DONE = new Color(0x1D8049);

    private static final Color SHADOW = new Color(0x141E2D);

    /** Stamp ink: the blue of a real bank stamp. */
    private static final Color STAMP = new Color(0x1F4FA3);

    private final byte[] regularFont;

    private final byte[] semiboldFont;

    private final byte[] monoFont;

    /** Null when no custom logo is on the classpath. */
    private final byte[] logo;

    /** Fonts are read once; each PDF embeds only the glyphs it uses, so a receipt stays around 30 KB. */
    public ReceiptPdfRenderer() {

        this.regularFont = read(FONTS + "IBMPlexSans-Regular.ttf");
        this.semiboldFont = read(FONTS + "IBMPlexSans-SemiBold.ttf");
        this.monoFont = read(FONTS + "IBMPlexMono-Medium.ttf");
        this.logo = new ClassPathResource(LOGO).exists() ? read(LOGO) : null;
    }

    public byte[] render(ReceiptDocumentDto receipt) {

        try (PDDocument pdf = new PDDocument()) {
            Fonts fonts = new Fonts(
                    PDType0Font.load(pdf, new ByteArrayInputStream(regularFont)),
                    PDType0Font.load(pdf, new ByteArrayInputStream(semiboldFont)),
                    PDType0Font.load(pdf, new ByteArrayInputStream(monoFont)));
            PDImageXObject logoImage = logo != null ? PDImageXObject.createFromByteArray(pdf, logo, "logo") : null;

            float paperHeight = new Painter(null, 0, fonts, logoImage).content(receipt);
            float pageHeight = PAPER_TOP + paperHeight + TOOTH_HEIGHT + DESK_BELOW;

            PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, pageHeight));
            pdf.addPage(page);

            PDDocumentInformation info = pdf.getDocumentInformation();
            info.setTitle("Квитанція " + receipt.receiptNumber());
            info.setAuthor("Digital Bank");

            try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                Painter painter = new Painter(stream, pageHeight, fonts, logoImage);
                painter.desk();
                painter.shadow(paperHeight);
                painter.paper(paperHeight);
                painter.content(receipt);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdf.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not render receipt " + receipt.receiptNumber(), e);
        }
    }

    private static byte[] read(String path) {

        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Receipt resource is missing: " + path, e);
        }
    }

    private record Fonts(PDFont regular, PDFont semibold, PDFont mono) {
    }

    /** Draws on one page. With a null stream it only measures: every method still returns the next y. */
    private static final class Painter {

        private final PDPageContentStream stream;

        private final float pageHeight;

        private final Fonts fonts;

        private final PDImageXObject logo;

        private Painter(PDPageContentStream stream, float pageHeight, Fonts fonts, PDImageXObject logo) {
            this.stream = stream;
            this.pageHeight = pageHeight;
            this.fonts = fonts;
            this.logo = logo;
        }

        /** Draws everything printed on the paper and returns the paper height. */
        float content(ReceiptDocumentDto receipt) throws IOException {

            float y = PAPER_TOP + PADDING;
            y = brand(y);

            y += 18;
            text(fonts.semibold(), 8.5f, MUTED, CONTENT_X, y + 9, receipt.title().toUpperCase(Locale.ROOT), 0.8f);
            status(y);
            y += 20;

            text(fonts.semibold(), 27, INK, CONTENT_X, y + 26, receipt.amount(), 0);
            y += 32;

            if (receipt.secondAmount() != null) {
                text(fonts.regular(), 14, MUTED, CONTENT_X, y + 14, receipt.secondAmount(), 0);
                y += 19;
            }

            text(fonts.regular(), 10.5f, MUTED, CONTENT_X, y + 12, receipt.operation(), 0);
            y += 16;

            y = cut(y);

            for (int i = 0; i < receipt.rows().size(); i++) {
                y = row(receipt.rows().get(i), y);
                if (i < receipt.rows().size() - 1) {
                    y += 9;
                }
            }

            y = cut(y);
            y = identifier("Номер квитанції", receipt.receiptNumber(), 10.5f, y);
            y += 8;
            y = identifier("ID транзакції", receipt.transactionId(), 8.8f, y);
            y += 16;
            y = footer(y);

            return y + PADDING - PAPER_TOP;
        }

        void desk() throws IOException {

            stream.setNonStrokingColor(DESK);
            stream.addRect(0, 0, PAGE_WIDTH, pageHeight);
            stream.fill();
        }

        /**
         * A soft shadow without a blur filter (PDF has none): the paper outline is painted many times, each copy
         * a bit lower and wider and almost transparent. Where the copies overlap the shadow is darker, so it
         * fades out towards the edges, and every tooth of the torn edge casts its own shadow.
         */
        void shadow(float paperHeight) throws IOException {

            for (int layer = 10; layer >= 1; layer--) {
                shadowLayer(paperHeight, layer * 0.7f, 3 + layer * 1.3f, 0.014f);
            }

            shadowLayer(paperHeight, 0.3f, 0.8f, 0.07f);
        }

        private void shadowLayer(float paperHeight, float expand, float shiftDown, float alpha) throws IOException {

            stream.saveGraphicsState();
            setAlpha(alpha);
            stream.setNonStrokingColor(SHADOW);
            paperOutline(paperHeight, expand, shiftDown);
            stream.fill();
            stream.restoreGraphicsState();
        }

        void paper(float paperHeight) throws IOException {

            stream.setNonStrokingColor(PAPER);
            paperOutline(paperHeight, 0, 0);
            stream.fill();
        }

        /** Rectangle with a row of downward triangles along the whole bottom: the torn-off edge. */
        private void paperOutline(float paperHeight, float expand, float shiftDown) throws IOException {

            float left = PAPER_X - expand;
            float right = PAPER_X + PAPER_WIDTH + expand;
            float top = PAPER_TOP - expand + shiftDown;
            float bottom = PAPER_TOP + paperHeight + shiftDown;
            int teeth = Math.round(PAPER_WIDTH / TOOTH_WIDTH);
            float toothWidth = (right - left) / teeth;

            stream.moveTo(left, pdfY(top));
            stream.lineTo(right, pdfY(top));
            stream.lineTo(right, pdfY(bottom));

            for (int i = teeth; i > 0; i--) {
                float x = left + i * toothWidth;
                stream.lineTo(x - toothWidth / 2, pdfY(bottom + TOOTH_HEIGHT + expand / 2));
                stream.lineTo(x - toothWidth, pdfY(bottom));
            }

            stream.closePath();
        }

        private float brand(float y) throws IOException {

            float size = 26;

            if (logo != null) {
                if (stream != null) {
                    stream.drawImage(logo, CONTENT_X, pdfY(y + size), size, size);
                }
            } else {
                roundedSquare(CONTENT_X, y, size, 6, BRAND);
                textCentered(fonts.semibold(), 11, PAPER, CONTENT_X + size / 2, y + 17, "DB");
            }

            text(fonts.semibold(), 12.5f, INK, CONTENT_X + size + 10, y + 11, "Digital Bank", 0);
            text(fonts.regular(), 7.5f, MUTED, CONTENT_X + size + 10, y + 22, "ІНТЕРНЕТ-БАНК", 0.6f);

            return y + size;
        }

        /** Green dot and "Виконано", right-aligned on the title line. */
        private void status(float y) throws IOException {

            String label = "Виконано";
            float width = width(fonts.semibold(), 9, label, 0);
            textRight(fonts.semibold(), 9, DONE, CONTENT_RIGHT, y + 9, label);

            if (stream != null) {
                stream.setNonStrokingColor(DONE);
                circle(CONTENT_RIGHT - width - 6, pdfY(y + 6), 2.6f);
                stream.fill();
            }
        }

        /** Grey label on the left, value right-aligned and wrapped, optional smaller note under it. */
        private float row(ReceiptDocumentDto.Row row, float y) throws IOException {

            PDFont font = row.total() ? fonts.semibold() : fonts.regular();
            float valueWidth = CONTENT_RIGHT - CONTENT_X - LABEL_WIDTH;

            text(font, row.total() ? 10 : 9, row.total() ? INK : MUTED, CONTENT_X, y + 10, row.label(), 0);

            // An IBAN is a little too long for 10 pt; one step smaller keeps it on one line, as people expect it.
            float size = width(font, 10, row.value(), 0) > valueWidth && width(font, 9, row.value(), 0) <= valueWidth
                    ? 9
                    : 10;

            float lineY = y;
            for (String line : wrap(row.value(), font, size, valueWidth)) {
                textRight(font, size, INK, CONTENT_RIGHT, lineY + 10, line);
                lineY += 13;
            }

            if (row.note() != null) {
                for (String line : wrap(row.note(), fonts.regular(), 8.5f, valueWidth)) {
                    textRight(fonts.regular(), 8.5f, MUTED, CONTENT_RIGHT, lineY + 9, line);
                    lineY += 11;
                }
            }

            return lineY;
        }

        /** Monospace: a receipt number or a UUID is easier to read out to support letter by letter. */
        private float identifier(String label, String value, float size, float y) throws IOException {

            text(fonts.regular(), 7.5f, MUTED, CONTENT_X, y + 8, label.toUpperCase(Locale.ROOT), 0.6f);
            text(fonts.mono(), size, INK, CONTENT_X, y + 13 + size, value, 0.2f);

            return y + 16 + size;
        }

        private float footer(float y) throws IOException {

            List<String> lines = wrap("Квитанцію сформовано автоматично, вона дійсна без підпису.",
                    fonts.regular(), 8, 150);
            float lineY = y + 52;

            for (String line : lines) {
                text(fonts.regular(), 8, MUTED, CONTENT_X, lineY, line, 0);
                lineY += 10.5f;
            }

            stamp(CONTENT_RIGHT - 36, y + 36);

            return y + 72;
        }

        /** Round "paid" stamp, rotated and see-through, like ink pressed onto the paper. */
        private void stamp(float centerX, float centerY) throws IOException {

            if (stream == null) {
                return;
            }

            stream.saveGraphicsState();
            setAlpha(0.78f);
            // From here on (0, 0) is the stamp's centre and the axes are turned by 12 degrees.
            stream.transform(Matrix.getRotateInstance(Math.toRadians(12), centerX, pdfY(centerY)));

            stream.setStrokingColor(STAMP);
            stream.setLineWidth(1.8f);
            circle(0, 0, 36);
            stream.stroke();
            stream.setLineWidth(0.8f);
            circle(0, 0, 26.5f);
            stream.stroke();

            // The bank's name runs along the ring, like on a real bank stamp; three dots close the bottom of it.
            textOnArc(fonts.semibold(), 5.8f, STAMP, 29.4f, STAMP_NAME, 0.5f);
            stream.setNonStrokingColor(STAMP);
            for (int dot = -1; dot <= 1; dot++) {
                double angle = -Math.PI / 2 + dot * 0.32;
                circle((float) (31.2 * Math.cos(angle)), (float) (31.2 * Math.sin(angle)), 0.9f);
                stream.fill();
            }

            rawTextCentered(fonts.semibold(), 7.5f, STAMP, 0, -2.6f, "ВИКОНАНО", 0.3f);

            stream.restoreGraphicsState();
        }

        /**
         * Letters along the top of a circle around (0, 0), centred at the top, read left to right.
         * Each letter is placed separately: moved to its point on the circle and turned so that its baseline
         * touches the circle and its bottom faces the centre.
         */
        private void textOnArc(PDFont font, float size, Color color, float radius, String value, float spacing)
                throws IOException {

            // Arc length = radius × angle, so a letter of width w takes w / radius radians.
            double angle = Math.PI / 2 + width(font, size, value, spacing) / 2 / radius;

            stream.setNonStrokingColor(color);

            for (int i = 0; i < value.length(); i++) {
                String letter = value.substring(i, i + 1);
                float letterWidth = width(font, size, letter, spacing);
                double middle = angle - letterWidth / 2 / radius;

                Matrix position = Matrix.getRotateInstance(middle - Math.PI / 2,
                        (float) (radius * Math.cos(middle)), (float) (radius * Math.sin(middle)));
                position.translate(-(letterWidth - spacing) / 2, 0);

                stream.beginText();
                stream.setFont(font, size);
                stream.setTextMatrix(position);
                stream.showText(letter);
                stream.endText();

                angle -= letterWidth / radius;
            }
        }

        /** A dashed line between the sections, like a perforation. */
        private float cut(float y) throws IOException {

            float lineY = y + 16;

            if (stream != null) {
                stream.setStrokingColor(LINE);
                stream.setLineWidth(1.2f);
                stream.setLineDashPattern(new float[] {3, 3}, 0);
                stream.moveTo(CONTENT_X, pdfY(lineY));
                stream.lineTo(CONTENT_RIGHT, pdfY(lineY));
                stream.stroke();
                stream.setLineDashPattern(new float[] {}, 0);
            }

            return lineY + 16;
        }

        // Low-level drawing. Everything below takes top-down y, except the raw* methods.

        private void text(PDFont font, float size, Color color, float x, float baselineY, String value,
                          float spacing) throws IOException {

            rawText(font, size, color, x, pdfY(baselineY), value, spacing);
        }

        private void textRight(PDFont font, float size, Color color, float right, float baselineY, String value)
                throws IOException {

            text(font, size, color, right - width(font, size, value, 0), baselineY, value, 0);
        }

        private void textCentered(PDFont font, float size, Color color, float centerX, float baselineY, String value)
                throws IOException {

            text(font, size, color, centerX - width(font, size, value, 0) / 2, baselineY, value, 0);
        }

        private void rawTextCentered(PDFont font, float size, Color color, float centerX, float baseline,
                                     String value, float spacing) throws IOException {

            rawText(font, size, color, centerX - width(font, size, value, spacing) / 2, baseline, value, spacing);
        }

        private void rawText(PDFont font, float size, Color color, float x, float baseline, String value,
                             float spacing) throws IOException {

            if (stream == null) {
                return;
            }

            stream.beginText();
            stream.setFont(font, size);
            stream.setNonStrokingColor(color);
            stream.setCharacterSpacing(spacing);
            stream.newLineAtOffset(x, baseline);
            stream.showText(value);
            stream.setCharacterSpacing(0);
            stream.endText();
        }

        private static float width(PDFont font, float size, String value, float spacing) throws IOException {

            return font.getStringWidth(value) / 1000 * size + spacing * value.length();
        }

        /** Greedy word wrap; a single word longer than the line is cut by characters. */
        private static List<String> wrap(String value, PDFont font, float size, float maxWidth) throws IOException {

            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();

            for (String word : value.split(" ")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (width(font, size, candidate, 0) <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                    continue;
                }
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                while (width(font, size, word, 0) > maxWidth) {
                    int cut = word.length() - 1;
                    while (cut > 1 && width(font, size, word.substring(0, cut), 0) > maxWidth) {
                        cut--;
                    }
                    lines.add(word.substring(0, cut));
                    word = word.substring(cut);
                }
                line.append(word);
            }

            if (!line.isEmpty()) {
                lines.add(line.toString());
            }

            return lines;
        }

        private void roundedSquare(float x, float topY, float size, float radius, Color color) throws IOException {

            if (stream == null) {
                return;
            }

            float k = 0.5523f * radius;
            float left = x;
            float right = x + size;
            float top = pdfY(topY);
            float bottom = pdfY(topY + size);

            stream.setNonStrokingColor(color);
            stream.moveTo(left + radius, bottom);
            stream.lineTo(right - radius, bottom);
            stream.curveTo(right - radius + k, bottom, right, bottom + radius - k, right, bottom + radius);
            stream.lineTo(right, top - radius);
            stream.curveTo(right, top - radius + k, right - radius + k, top, right - radius, top);
            stream.lineTo(left + radius, top);
            stream.curveTo(left + radius - k, top, left, top - radius + k, left, top - radius);
            stream.lineTo(left, bottom + radius);
            stream.curveTo(left, bottom + radius - k, left + radius - k, bottom, left + radius, bottom);
            stream.closePath();
            stream.fill();
        }

        /** A circle from four Bezier curves (PDF has no circle operator); the centre is in PDF coordinates. */
        private void circle(float centerX, float centerY, float radius) throws IOException {

            float k = 0.5523f * radius;

            stream.moveTo(centerX + radius, centerY);
            stream.curveTo(centerX + radius, centerY + k, centerX + k, centerY + radius, centerX, centerY + radius);
            stream.curveTo(centerX - k, centerY + radius, centerX - radius, centerY + k, centerX - radius, centerY);
            stream.curveTo(centerX - radius, centerY - k, centerX - k, centerY - radius, centerX, centerY - radius);
            stream.curveTo(centerX + k, centerY - radius, centerX + radius, centerY - k, centerX + radius, centerY);
            stream.closePath();
        }

        private void setAlpha(float alpha) throws IOException {

            PDExtendedGraphicsState state = new PDExtendedGraphicsState();
            state.setNonStrokingAlphaConstant(alpha);
            state.setStrokingAlphaConstant(alpha);
            stream.setGraphicsStateParameters(state);
        }

        private float pdfY(float topDownY) {

            return pageHeight - topDownY;
        }
    }
}
