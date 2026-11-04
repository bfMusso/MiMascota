package uy.mimascota.placas;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Hoja A4 para imprimir un lote: un QR por placa con su código debajo, 4 por fila. */
final class LotePdf {

    private static final Color PRINCIPAL = new Color(0x7C, 0x5C, 0xE0);
    private static final Color TEXTO = new Color(0x26, 0x21, 0x3A);
    private static final Color SUAVE = new Color(0x6B, 0x65, 0x85);
    private static final Color LINEA = new Color(0xE7, 0xE2, 0xF2);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Montevideo"));
    private static final int COLUMNAS = 4;

    private LotePdf() {
    }

    static byte[] generar(String lote, List<Placa> placas, String urlWeb) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 32, 32, 36, 36);
        PdfWriter.getInstance(doc, out);
        doc.addTitle("Lote " + lote);
        doc.addCreator("MiMascota");
        doc.open();

        doc.add(new Paragraph("MiMascota · Lote " + lote, new Font(Font.HELVETICA, 16, Font.BOLD, PRINCIPAL)));
        Paragraph sub = new Paragraph(placas.size() + (placas.size() == 1 ? " placa" : " placas") + " · creado el "
                + FECHA.format(placas.get(0).getFechaAlta()), new Font(Font.HELVETICA, 10, Font.NORMAL, SUAVE));
        sub.setSpacingAfter(14);
        doc.add(sub);

        PdfPTable tabla = new PdfPTable(COLUMNAS);
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(0);
        Font fuenteCodigo = new Font(Font.COURIER, 13, Font.BOLD, TEXTO);
        Font fuenteUrl = new Font(Font.HELVETICA, 6.5f, Font.NORMAL, SUAVE);
        Font fuenteEstado = new Font(Font.HELVETICA, 7, Font.BOLD, PRINCIPAL);

        for (Placa p : placas) {
            String url = urlWeb + "/p/" + p.getCodigo();
            PdfPCell celda = new PdfPCell();
            celda.setBorderColor(LINEA);
            celda.setBorderWidth(0.75f);
            celda.setPadding(8);
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);

            Image qr = qr(url);
            qr.scaleAbsolute(96, 96);
            qr.setAlignment(Element.ALIGN_CENTER);
            celda.addElement(qr);

            Paragraph codigo = new Paragraph(p.getCodigo().substring(0, 4) + " " + p.getCodigo().substring(4), fuenteCodigo);
            codigo.setAlignment(Element.ALIGN_CENTER);
            celda.addElement(codigo);

            Paragraph dir = new Paragraph(url, fuenteUrl);
            dir.setAlignment(Element.ALIGN_CENTER);
            celda.addElement(dir);

            if (p.getEstado() != EstadoPlaca.SIN_ACTIVAR) {
                Paragraph estado = new Paragraph(ESTADOS.get(p.getEstado()), fuenteEstado);
                estado.setAlignment(Element.ALIGN_CENTER);
                celda.addElement(estado);
            }
            tabla.addCell(celda);
        }
        // Completa la última fila para que la grilla quede pareja
        int resto = placas.size() % COLUMNAS;
        for (int i = 0; resto != 0 && i < COLUMNAS - resto; i++) {
            PdfPCell vacia = new PdfPCell(new Phrase(""));
            vacia.setBorder(Rectangle.NO_BORDER);
            tabla.addCell(vacia);
        }
        doc.add(tabla);
        doc.close();
        return out.toByteArray();
    }

    private static final Map<EstadoPlaca, String> ESTADOS = Map.of(
            EstadoPlaca.ACTIVA, "YA ACTIVADA",
            EstadoPlaca.BLOQUEADA, "BLOQUEADA");

    private static Image qr(String texto) {
        try {
            BitMatrix m = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, 400, 400,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 1));
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(m.getWidth(), m.getHeight(),
                    java.awt.image.BufferedImage.TYPE_BYTE_BINARY);
            for (int x = 0; x < m.getWidth(); x++) {
                for (int y = 0; y < m.getHeight(); y++) {
                    img.setRGB(x, y, m.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            return Image.getInstance(img, null);
        } catch (WriterException e) {
            throw new IllegalStateException("No se pudo generar el QR", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
