package cn.admilk.dglabweb.client.ui;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;

import java.util.HashMap;
import java.util.Map;

public final class QrCodeHelper extends AbstractGui {
    public static final class QrMatrix {
        private final int size;
        private final boolean[] cells;

        private QrMatrix(int size, boolean[] cells) {
            this.size = size;
            this.cells = cells;
        }

        public int getSize() {
            return this.size;
        }

        public boolean isDark(int x, int y) {
            return x >= 0 && y >= 0 && x < this.size && y < this.size && this.cells[(y * this.size) + x];
        }
    }

    private QrCodeHelper() {
    }

    public static QrMatrix encode(String text) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, Integer.valueOf(1));
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);

        BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 256, 256, hints);
        int[] bounds = matrix.getEnclosingRectangle();
        int left = bounds == null ? 0 : bounds[0];
        int top = bounds == null ? 0 : bounds[1];
        int width = bounds == null ? matrix.getWidth() : bounds[2];
        int height = bounds == null ? matrix.getHeight() : bounds[3];
        int size = Math.max(width, height);
        boolean[] cells = new boolean[size * size];
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int matrixX = left + col;
                int matrixY = top + row;
                cells[(row * size) + col] = matrixX < matrix.getWidth() && matrixY < matrix.getHeight() && matrix.get(matrixX, matrixY);
            }
        }
        return new QrMatrix(size, cells);
    }

    public static void draw(MatrixStack matrixStack, QrMatrix matrix, int x, int y, int size) {
        if (matrix == null || size <= 0) {
            return;
        }
        int matrixSize = matrix.getSize();
        int cell = Math.max(1, size / matrixSize);
        int actualSize = cell * matrixSize;
        int startX = x + ((size - actualSize) / 2);
        int startY = y + ((size - actualSize) / 2);

        fill(matrixStack, x, y, x + size, y + size, 0xFFF8FAFC);
        fill(matrixStack, x, y, x + size, y + 1, 0xFFCBD5E1);
        fill(matrixStack, x, y + size - 1, x + size, y + size, 0xFFCBD5E1);
        fill(matrixStack, x, y, x + 1, y + size, 0xFFCBD5E1);
        fill(matrixStack, x + size - 1, y, x + size, y + size, 0xFFCBD5E1);

        for (int row = 0; row < matrixSize; row++) {
            for (int col = 0; col < matrixSize; col++) {
                if (!matrix.isDark(col, row)) {
                    continue;
                }
                int left = startX + (col * cell);
                int top = startY + (row * cell);
                fill(matrixStack, left, top, left + cell, top + cell, 0xFF020617);
            }
        }
    }
}
