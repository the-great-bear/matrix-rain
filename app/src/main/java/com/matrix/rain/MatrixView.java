package com.matrix.rain;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

public class MatrixView extends View {

    private static final int FRAME_DELAY_MS = 60;
    private static final int FONT_SIZE_SP = 18;
    private static final float TRAIL_ALPHA = 0.05f;

    // Mix of ASCII printable + katakana-range Unicode for that Matrix feel
    private static final String CHARS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz" +
        "0123456789@#$%^&*()[]{}<>/\\|~`" +
        "ｦｧｨｩｪｫｬｭｮｯｰｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝ";

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fadePaint = new Paint();
    private final Random rng = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int[] columnY;   // current row index for each column's head
    private char[] columnChar; // current character shown at the head
    private int cellW, cellH;
    private int cols, rows;
    private boolean ready = false;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            step();
            invalidate();
            handler.postDelayed(this, FRAME_DELAY_MS);
        }
    };

    public MatrixView(Context ctx) { super(ctx); init(); }
    public MatrixView(Context ctx, AttributeSet a) { super(ctx, a); init(); }
    public MatrixView(Context ctx, AttributeSet a, int d) { super(ctx, a, d); init(); }

    private void init() {
        float density = getResources().getDisplayMetrics().scaledDensity;
        float fontSize = FONT_SIZE_SP * density;

        textPaint.setColor(Color.GREEN);
        textPaint.setTextSize(fontSize);
        textPaint.setTypeface(Typeface.MONOSPACE);

        headPaint.setColor(Color.WHITE);
        headPaint.setTextSize(fontSize);
        headPaint.setTypeface(Typeface.MONOSPACE);

        // Semi-transparent black overlay creates the trailing fade effect
        fadePaint.setColor(Color.BLACK);
        fadePaint.setAlpha((int) (TRAIL_ALPHA * 255));
        fadePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        cellH = (int) (fm.descent - fm.ascent + 2);
        cellW = (int) textPaint.measureText("M");

        cols = w / cellW;
        rows = h / cellH + 1;

        columnY = new int[cols];
        columnChar = new char[cols];
        for (int i = 0; i < cols; i++) {
            columnY[i] = -rng.nextInt(rows); // stagger start positions
            columnChar[i] = randomChar();
        }
        ready = true;
    }

    private void step() {
        if (!ready) return;
        for (int i = 0; i < cols; i++) {
            columnChar[i] = randomChar();
            columnY[i]++;
            if (columnY[i] > rows + rng.nextInt(20)) {
                columnY[i] = -rng.nextInt(rows / 2);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Fade previous frame rather than clearing — this gives the comet tail
        canvas.drawRect(0, 0, getWidth(), getHeight(), fadePaint);

        if (!ready) return;

        for (int col = 0; col < cols; col++) {
            int headRow = columnY[col];
            float x = col * cellW;
            float y = headRow * cellH;

            // White head character
            if (headRow >= 0 && headRow < rows) {
                canvas.drawText(String.valueOf(columnChar[col]), x, y, headPaint);
            }

            // Green character one step behind (gives depth)
            int prev = headRow - 1;
            if (prev >= 0 && prev < rows) {
                canvas.drawText(String.valueOf(randomChar()), x, prev * cellH, textPaint);
            }
        }
    }

    private char randomChar() {
        return CHARS.charAt(rng.nextInt(CHARS.length()));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.post(tick);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(tick);
    }
}
