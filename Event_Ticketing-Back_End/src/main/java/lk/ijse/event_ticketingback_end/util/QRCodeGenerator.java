package lk.ijse.event_ticketingback_end.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class QRCodeGenerator {

    /**
     * Returns raw PNG bytes — used by EventController to return image response.
     */
    public byte[] generateQRBytes(String data, int size) throws WriterException, IOException {
        return generate(data, size);
    }

    /**
     * Returns Base64 string — used by PaymentServiceImpl to embed in email HTML.
     * <img src="data:image/png;base64,XXXX..." />
     */
    public String generateQRBase64(String data, int size) throws WriterException, IOException {
        return Base64.getEncoder().encodeToString(generate(data, size));
    }

    private byte[] generate(String data, int size) throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix    matrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size, hints);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }
}