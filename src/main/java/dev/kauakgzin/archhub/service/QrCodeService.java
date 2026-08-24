package dev.kauakgzin.archhub.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Renders a QR code as a plain SVG string (no image library / AWT needed),
 * so scanning it opens the Hub's dashboard straight from a phone camera.
 */
@Service
public class QrCodeService {

    private static final String LIGHT = "#ffffff";
    private static final String DARK = "#0f1115";

    public String toSvg(String content) {
        BitMatrix matrix;
        try {
            matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.MARGIN, 1));
        } catch (WriterException ex) {
            throw new IllegalStateException("Could not generate QR code for: " + content, ex);
        }

        int width = matrix.getWidth();
        int height = matrix.getHeight();

        StringBuilder svg = new StringBuilder(width * height / 2);
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ")
                .append(width).append(' ').append(height)
                .append("\" shape-rendering=\"crispEdges\">");
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"").append(LIGHT).append("\"/>");

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix.get(x, y)) {
                    svg.append("<rect x=\"").append(x).append("\" y=\"").append(y)
                            .append("\" width=\"1\" height=\"1\" fill=\"").append(DARK).append("\"/>");
                }
            }
        }

        svg.append("</svg>");
        return svg.toString();
    }
}
