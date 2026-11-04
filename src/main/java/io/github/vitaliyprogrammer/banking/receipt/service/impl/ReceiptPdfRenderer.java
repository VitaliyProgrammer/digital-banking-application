package io.github.vitaliyprogrammer.banking.receipt.service.impl;

import io.github.vitaliyprogrammer.banking.receipt.dto.response.ReceiptDocumentDto;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSName;
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
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType2;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType2;
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
 * The bank's logo (three rising bars and the name) is drawn as vectors, so it stays sharp at any zoom.
 */
@Component
public class ReceiptPdfRenderer {

    private static final String FONTS = "receipt/fonts/";


    /** The bank's name in the middle of the stamp: the first line on top, the second below it at the same length. */
    private static final String STAMP_NAME_TOP = "Taborovets";

    private static final String STAMP_NAME_BOTTOM = "Universal Bank";

    /** Both lines of the name are this long: 20 to each side of the centre, inside the inner ring (radius 24). */
    private static final float STAMP_NAME_WIDTH = 40;

    /** Read along the bottom of the stamp's ring, like the country or city on a real bank stamp. */
    private static final String STAMP_BOTTOM = "УКРАЇНА";

    /** Height of a capital letter as a share of the font size, for IBM Plex Sans. */
    private static final float CAP_HEIGHT = 0.7f;

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

    // The approved logo, artboard 11 "Лайм, бірюза, зелений": three bars, each a top-to-bottom gradient.
    private static final Color[][] LOGO_BARS = {
            {new Color(0xD9F99D), new Color(0xA3E635)},
            {new Color(0x86EFAC), new Color(0x22C55E)},
            {new Color(0x4ADE80), new Color(0x16A34A)}};

    private static final Color LOGO_WORD = new Color(0x15803D);

    private static final Color LOGO_BANK = new Color(0x0F2A44);

    private static final Color DONE = new Color(0x1D8049);

    private static final Color SHADOW = new Color(0x141E2D);

    /** Stamp ink: the blue of a real bank stamp. */
    private static final Color STAMP = new Color(0x1F4FA3);

    private final byte[] regularFont;

    private final byte[] semiboldFont;

    private final byte[] monoFont;

    private final byte[] logoFont;

    /** Fonts are read once; each PDF embeds only the glyphs it uses, so a receipt stays around 30 KB. */
    public ReceiptPdfRenderer() {

        this.regularFont = read(FONTS + "IBMPlexSans-Regular.ttf");
        this.semiboldFont = read(FONTS + "IBMPlexSans-SemiBold.ttf");
        this.monoFont = read(FONTS + "IBMPlexMono-Medium.ttf");
        this.logoFont = read(FONTS + "Manrope-ExtraBold.ttf");
    }

    public byte[] render(ReceiptDocumentDto receipt) {

        try (PDDocument pdf = new PDDocument()) {
            Fonts fonts = new Fonts(
                    PDType0Font.load(pdf, new ByteArrayInputStream(regularFont)),
                    PDType0Font.load(pdf, new ByteArrayInputStream(semiboldFont)),
                    PDType0Font.load(pdf, new ByteArrayInputStream(monoFont)),
                    PDType0Font.load(pdf, new ByteArrayInputStream(logoFont)));

            float paperHeight = new Painter(null, 0, fonts).content(receipt);
            float pageHeight = PAPER_TOP + paperHeight + TOOTH_HEIGHT + DESK_BELOW;

            PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, pageHeight));
            pdf.addPage(page);

            PDDocumentInformation info = pdf.getDocumentInformation();
            info.setTitle("Квитанція " + receipt.receiptNumber());
            info.setAuthor("Taborovets Universal Bank");

            try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                Painter painter = new Painter(stream, pageHeight, fonts);
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

    private record Fonts(PDFont regular, PDFont semibold, PDFont mono, PDFont logo) {
    }

    /** Draws on one page. With a null stream it only measures: every method still returns the next y. */
    private static final class Painter {

        private final PDPageContentStream stream;

        private final float pageHeight;

        private final Fonts fonts;

        private Painter(PDPageContentStream stream, float pageHeight, Fonts fonts) {
            this.stream = stream;
            this.pageHeight = pageHeight;
            this.fonts = fonts;
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

        /**
         * The bank's logo, scaled down from the approved artboard (bars 24 wide, 8 apart, radius 8; "Taborovets"
         * 60, "Universal Bank" 37, 14 between them; 26 between the mark and the name). The tallest bar reaches the
         * top of the capitals of "Taborovets", every bar ends on the baseline of "Universal Bank", and the letters of
         * "Universal Bank" are spread to exactly the width of "Taborovets".
         */
        private float brand(float y) throws IOException {

            float scale = 0.36f;
            float wordSize = 60 * scale;
            float bankSize = 37 * scale;
            float capHeight = fonts.logo().getFontDescriptor().getCapHeight() / 1000;
            float wordCap = capHeight * wordSize;
            float bankCap = capHeight * bankSize;
            float height = wordCap + 14 * scale + bankCap;

            float barWidth = 24 * scale;
            float barGap = 8 * scale;
            float radius = 8 * scale;
            float[] shares = {0.5f, 0.75f, 1f};
            for (int bar = 0; bar < 3; bar++) {
                float barHeight = height * shares[bar];
                gradientBar(CONTENT_X + bar * (barWidth + barGap), y + height - barHeight, barWidth, barHeight, radius,
                        LOGO_BARS[bar][0], LOGO_BARS[bar][1]);
            }

            float textX = CONTENT_X + 3 * barWidth + 2 * barGap + 26 * scale;
            float spacing = -1.8f * scale;
            String word = "Taborovets";
            text(fonts.logo(), wordSize, LOGO_WORD, textX, y + wordCap, word, spacing);
            spread(fonts.logo(), bankSize, LOGO_BANK, textX, y + height, "Universal Bank",
                    width(fonts.logo(), wordSize, word, spacing) - spacing);

            return y + height;
        }

        /** Letters placed one by one so that the line is exactly {@code totalWidth} long (like flex space-between). */
        private void spread(PDFont font, float size, Color color, float x, float baselineY, String value,
                            float totalWidth) throws IOException {

            float lettersWidth = width(font, size, value, 0);
            float gap = (totalWidth - lettersWidth) / (value.length() - 1);
            for (int i = 0; i < value.length(); i++) {
                String letter = value.substring(i, i + 1);
                text(font, size, color, x, baselineY, letter, 0);
                x += width(font, size, letter, 0) + gap;
            }
        }

        /** A rounded bar filled with a top-to-bottom gradient, like CSS linear-gradient(180deg, top, bottom). */
        private void gradientBar(float x, float topY, float width, float height, float radius, Color top, Color bottom)
                throws IOException {

            if (stream == null) {
                return;
            }

            stream.saveGraphicsState();
            roundedRectPath(x, topY, width, height, radius);
            stream.clip();
            stream.shadingFill(verticalGradient(pdfY(topY), pdfY(topY + height), top, bottom));
            stream.restoreGraphicsState();
        }

        private static PDShading verticalGradient(float topPdfY, float bottomPdfY, Color top, Color bottom) {

            COSDictionary function = new COSDictionary();
            function.setInt(COSName.FUNCTION_TYPE, 2);
            function.setItem(COSName.DOMAIN, floats(0, 1));
            function.setItem(COSName.C0, floats(rgb(top)));
            function.setItem(COSName.C1, floats(rgb(bottom)));
            function.setInt(COSName.N, 1);

            PDShadingType2 shading = new PDShadingType2(new COSDictionary());
            shading.setShadingType(PDShading.SHADING_TYPE2);
            shading.setColorSpace(PDDeviceRGB.INSTANCE);
            shading.setCoords(floats(0, topPdfY, 0, bottomPdfY));
            shading.setFunction(new PDFunctionType2(function));
            return shading;
        }

        private static float[] rgb(Color color) {
            return new float[] {color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f};
        }

        private static COSArray floats(float... values) {

            COSArray array = new COSArray();
            for (float value : values) {
                array.add(new COSFloat(value));
            }
            return array;
        }

        /** Green dot and "Успішно", right-aligned on the title line. */
        private void status(float y) throws IOException {

            String label = "Успішно";
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
            circle(0, 0, 24);
            stream.stroke();

            // "ВИКОНАНО" runs along the top of the ring.
            // Baseline at 27.7: capitals (about 4.5 high) fill 27.7 to 32.2, so about 3.7 stays free to each ring.
            textOnArc(fonts.semibold(), 6.5f, STAMP, 27.7f, "ВИКОНАНО", 1.2f);

            // "УКРАЇНА" along the bottom. There the letters stand with their tops to the centre, so the baseline is
            // the outer side: 32.3, and the capitals fill 27.8 to 32.3, again about 3.7 clear.
            textOnBottomArc(fonts.semibold(), 6.5f, STAMP, 32.3f, STAMP_BOTTOM, 1.2f);

            // Two dots exactly at the ring's middle height, left and right, between the top and the bottom words.
            stream.setNonStrokingColor(STAMP);
            circle(-30, 0, 1.1f);
            stream.fill();
            circle(30, 0, 1.1f);
            stream.fill();

            // The bank's name inside the inner ring (radius 24): each line gets the font size that makes it exactly
            // STAMP_NAME_WIDTH long, and the two lines together are centred. The farthest corner is about 20.8 from
            // the centre, so the name keeps about 3.2 clear of the ring.
            float topSize = STAMP_NAME_WIDTH / width(fonts.semibold(), 1, STAMP_NAME_TOP, 0);
            float bottomSize = STAMP_NAME_WIDTH / width(fonts.semibold(), 1, STAMP_NAME_BOTTOM, 0);
            float gap = 2.2f;
            float height = CAP_HEIGHT * topSize + gap + CAP_HEIGHT * bottomSize;
            float topBaseline = height / 2 - CAP_HEIGHT * topSize;
            rawTextCentered(fonts.semibold(), topSize, STAMP, 0, topBaseline, STAMP_NAME_TOP, 0);
            rawTextCentered(fonts.semibold(), bottomSize, STAMP, 0, topBaseline - gap - CAP_HEIGHT * bottomSize,
                    STAMP_NAME_BOTTOM, 0);

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

        /**
         * Letters along the bottom of a circle around (0, 0), centred at the bottom, read left to right, standing with
         * their tops towards the centre (so the baseline is on the outer side).
         */
        private void textOnBottomArc(PDFont font, float size, Color color, float radius, String value, float spacing)
                throws IOException {

            float halfAngle = width(font, size, value, spacing) / 2 / radius;
            double angle = -Math.PI / 2 - halfAngle;

            stream.setNonStrokingColor(color);

            for (int i = 0; i < value.length(); i++) {
                String letter = value.substring(i, i + 1);
                float letterWidth = width(font, size, letter, spacing);
                double middle = angle + letterWidth / 2 / radius;

                Matrix position = Matrix.getRotateInstance(middle + Math.PI / 2,
                        (float) (radius * Math.cos(middle)), (float) (radius * Math.sin(middle)));
                position.translate(-(letterWidth - spacing) / 2, 0);

                stream.beginText();
                stream.setFont(font, size);
                stream.setTextMatrix(position);
                stream.showText(letter);
                stream.endText();

                angle += letterWidth / radius;
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

        /** The outline of a rounded rectangle, ready to fill or clip; top-down coordinates. */
        private void roundedRectPath(float x, float topY, float width, float height, float radius) throws IOException {

            float k = 0.5523f * radius;
            float left = x;
            float right = x + width;
            float top = pdfY(topY);
            float bottom = pdfY(topY + height);

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
