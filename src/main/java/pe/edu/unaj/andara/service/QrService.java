package pe.edu.unaj.andara.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;
import java.util.Map;

public final class QrService {

    private QrService() {
    }

    public static BitMatrix matriz(String contenido, int tamanio) {
        try {
            return new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, tamanio, tamanio,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 1));
        } catch (WriterException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Image imagen(String contenido, int tamanio) {
        BitMatrix m = matriz(contenido, tamanio);
        WritableImage img = new WritableImage(m.getWidth(), m.getHeight());
        PixelWriter pw = img.getPixelWriter();
        Color oscuro = Color.web("#1D1D1F");
        for (int y = 0; y < m.getHeight(); y++) {
            for (int x = 0; x < m.getWidth(); x++) {
                pw.setColor(x, y, m.get(x, y) ? oscuro : Color.WHITE);
            }
        }
        return img;
    }

    public static BufferedImage imagenAwt(String contenido, int tamanio) {
        return MatrixToImageWriter.toBufferedImage(matriz(contenido, tamanio));
    }
}
