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
        public boolean mCanvasDirty;
        public int mCharWidth;
        public int mCharHeight;
        public int mCharWidthOffset;
        public int mCharHeightOffset;
        public static final int mCharSpacing = 1;
        public static final int mTerminalWidth = 80;
        public static final int mTerminalHeight = 25;

        public RenderThread (SurfaceHolder holder, Context context) {
            mSurfaceHolder = holder;
            mContext = context;

            mPaintText = new Paint();
            mPaintText.setTypeface(Typeface.create("Monospace", Typeface.BOLD));
            mPaintText.setARGB(0xFF, 0x0, 0x0, 0xFF);

            mPaintBounds = new Paint();
            mPaintBounds.setARGB(0xFF, 0x0, 0xFF, 0x00);

            mCanvasDirty = true;
        }

        // Given the current canvas dimensions, find the font size which fills up the screen
        // as much as possible given the desired terminal dimensions
        public void recalculateFontSize (Canvas canvas, int terminalWidth, int terminalHeight) {
            int fontSize = 1;
            int lastWidth = -1;
            int lastHeight = -1;
            while (true) {
                if (fontSize >= 1000)
                    Logging.fatal("Unable to find suitable font size to fill canvas");
                mPaintText.setTextSize(fontSize);
                Rect rect = new Rect();
                mPaintText.getTextBounds("X", 0, 1, rect); // How big is "X" when drawn
                if ((rect.width() == 0) || (rect.height() == 0))
                    Logging.fatal("Invalid width or height from getTextBounds");
                lastWidth = mCharWidth;
                lastHeight = mCharHeight;
                mCharWidth = rect.width() + mCharSpacing;
                mCharHeight = rect.height() + mCharSpacing;
                Logging.debug("Calculated for fontsize=" + fontSize + " bounds width=" + mCharWidth + " height=" + mCharHeight);
                if ((mCharWidth * terminalWidth > mCanvasWidth) || (mCharHeight * terminalHeight > mCanvasHeight)) {
                    // This font size is too big, we need to go back one step and finish
                    fontSize--;
                    mCharWidth = lastWidth;
                    mCharHeight = lastHeight;
                    if ((mCharWidth <= 0) || (mCharHeight <= 0))
                        Logging.fatal("Failed to compute expected width and height");
                    // Compute any remaining padding to center the image on the display
                    mCharWidthOffset = (mCanvasWidth - mCharWidth*terminalWidth) / 2;
                    mCharHeightOffset = (mCanvasHeight - mCharHeight*terminalHeight) / 2;
                    return;
                }
                fontSize++;
            }
        }

        public void drawFixedChar (Canvas canvas, char ch, int row, int col) {
            int x = mCharWidthOffset + col * mCharWidth;
            int y = mCharHeightOffset + (row+1) * mCharHeight;
            canvas.drawText(String.valueOf(ch), x, y, mPaintText);
        }

        public void drawFixedString(Canvas canvas, String str, int row, int col) {
            int c = col;
            for (char ch : str.toCharArray()) {
                drawFixedChar(canvas, ch, row, c);
                c++;
            }
        }

        public void drawDebug(Canvas canvas, int width, int height) {
            for (int c = 0; c < width; c++) {
                for (int r = 0; r < height; r++) {
                    if ((c == 0) || (c == width-1) || (r == 0) || (r == height-1))
                        drawFixedChar(canvas, '#', r, c);
                    else {
                        drawFixedChar(canvas, Character.forDigit(c % 10, 10), r, c);
                    }
                }
            }
        }

        int tempR = 0;
        int tempC = 0;
        public void doDraw(Canvas canvas) {
            if (mCanvasDirty) {
                mCanvasDirty = false;
                recalculateFontSize(canvas, mTerminalWidth, mTerminalHeight);
            }

            // Logging.debug("onDraw temp=" + tempX);
            canvas.drawRGB(0xC0, 0xC0, 0xC0); // Gray terminal background

            // Debug the full layout of the display
            drawDebug(canvas, 80, 25);

            // Animated string to show things are updating
            drawFixedString(canvas, "R="+tempR+"C="+tempC, tempR, tempC);
            tempR += 1;
            if (tempR >= 25) tempR = 0;
            tempC += 1;
            if (tempC >= 80) tempC = 0;
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
                mCanvasDirty = true;
                Logging.debug("Detected change in surface size to " + width + "x" + height);
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
