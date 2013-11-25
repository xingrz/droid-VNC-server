package org.onaips.vnc;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;

public class VNCService extends Service {

    private static final String TAG = "VNCService";
    private static final String LIVE_CARD_ID = "vnc";

    public class VNCBinder extends Binder {
        public VNCService getService() {
            return VNCService.this;
        }
    }

    private final VNCBinder mBinder = new VNCBinder();

    private TimelineManager mTimelineManager;
    private LiveCard mLiveCard;
    private PanelDrawer mDrawer;

	@Override
	public void onCreate() {
		super.onCreate();
        mTimelineManager = TimelineManager.from(this);
	}

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mLiveCard == null) {
            Log.d(TAG, "Publishing LiveCard");

            mLiveCard = mTimelineManager.getLiveCard(LIVE_CARD_ID);
            mDrawer = new PanelDrawer(this);

            // Keep track of the callback to remove it before unpublishing.
            mLiveCard.enableDirectRendering(true).getSurfaceHolder().addCallback(mDrawer);
            mLiveCard.setNonSilent(true);

            Intent menuIntent = new Intent(this, MenuActivity.class);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));

            mLiveCard.publish();

            Log.d(TAG, "Done publishing LiveCard");
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mLiveCard != null && mLiveCard.isPublished()) {
            Log.d(TAG, "Unpublishing LiveCard");

            mLiveCard.unpublish();
            mLiveCard.getSurfaceHolder().removeCallback(mDrawer);
            mLiveCard = null;
        }

        super.onDestroy();
    }

}
