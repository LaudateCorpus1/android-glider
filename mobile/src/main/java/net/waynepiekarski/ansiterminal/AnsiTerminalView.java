package net.waynepiekarski.ansiterminal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class AnsiTerminalView extends SurfaceView implements SurfaceHolder.Callback {

    // Thread object that does most of the work here, AnsiTerminalView is just a wrapper
    RenderThread mRenderThread;

    class RenderThread extends Thread {
        public SurfaceHolder mSurfaceHolder;
        public Context mContext;
        public boolean mRunning = true;
        public Paint mPaintBounds;
        public Paint mPaintText;
        public int mCanvasWidth;
        public int mCanvasHeight;
        public int mCharWidth;
        public int mCharHeight;
        public static final int mCharSpacing = 1;

        public RenderThread (SurfaceHolder holder, Context context) {
            mSurfaceHolder = holder;
            mContext = context;

            mPaintText = new Paint();
            mPaintText.setTypeface(Typeface.create("Monospace", Typeface.BOLD));
            mPaintText.setTextSize(50);
            mPaintText.setARGB(0xFF, 0x0, 0x0, 0xFF);
            Rect rect = new Rect();
            mPaintText.getTextBounds("X", 0, 1, rect);
            mCharWidth = rect.width() + mCharSpacing;
            mCharHeight = rect.height() + mCharSpacing;
            Logging.debug("Calculated bounds width=" + mCharWidth + " height=" + mCharHeight);

            mPaintBounds = new Paint();
            mPaintBounds.setARGB(0xFF, 0x0, 0xFF, 0x00);
        }

        public void drawFixedChar (Canvas canvas, char ch, int row, int col) {
            int x = col * mCharWidth;
            int y = (row+1) * mCharHeight;
            canvas.drawText(String.valueOf(ch), x, y, mPaintText);
        }

        public void drawFixedString(Canvas canvas, String str, int row, int col) {
            int c = col;
            for (char ch : str.toCharArray()) {
                drawFixedChar(canvas, ch, row, c);
                c++;
            }
        }

        int tempX = 0;
        public void doDraw(Canvas canvas) {
            // Logging.debug("onDraw temp=" + tempX);
            canvas.drawRGB(0xC0, 0xC0, 0xC0); // Gray terminal background
            canvas.drawText("Hello world " + tempX, tempX, tempX, mPaintText);
            tempX += 5;
            if (tempX >= 500)
                tempX = 0;
            drawFixedString(canvas, "123abcXYZ", 0, 0);
            drawFixedString(canvas, "ABC123***", 1, 0);
            for (int r = 0; r < 10; r++) {
                drawFixedString(canvas, "ROW" + r, r, 10);
            }
        }

        public void run() {
            while (mRunning) {
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        doDraw(c);
                    }
                } finally {
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }

        public void setSurfaceSize(int width, int height) {
            synchronized (mSurfaceHolder) {
                // Change bitmap attributes atomically
                mCanvasWidth = width;
                mCanvasHeight = height;
            }
        }
    }

    public AnsiTerminalView (Context context) {
        super(context);

        // Callback to tell us when the surface is changed
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // Drawing is done on a separate thread, so create it here but wait until surfaceCreated()
        // before we actually start the drawing
        mRenderThread = new RenderThread(holder, context);

        // Get key events
        setFocusable(true);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Logging.debug("surfaceChanged");
        mRenderThread.setSurfaceSize(width, height);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // Start the thread here so that we know the surface is ready before we start drawing
        Logging.debug("surfaceCreated");
        mRenderThread.start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Logging.debug("surfaceDestroyed");
        Logging.fatal("surfaceDestroyed not implemented yet");
        // TODO: Stop the rendering thread
    }
}