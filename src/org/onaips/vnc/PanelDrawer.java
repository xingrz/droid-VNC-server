package org.onaips.vnc;

import android.content.Context;
import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class PanelDrawer implements SurfaceHolder.Callback {

    private static final String TAG = "PanelDrawer";

    private SurfaceHolder mHolder;
    private PanelView mView;

    public PanelDrawer(Context context) {
        mView = new PanelView(context);
        mView.setListener(new PanelView.ChangeListener() {
            @Override
            public void onChange() {
                if (mHolder != null) {
                    draw();
                }
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mHolder = holder;
        draw();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        draw();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHolder = null;
    }

    public void draw() {
        Canvas canvas;
        try {
            canvas = mHolder.lockCanvas();
        } catch (Exception e) {
            return;
        }
        if (canvas != null) {
            mView.draw(canvas);
            mHolder.unlockCanvasAndPost(canvas);
        }
    }
}
