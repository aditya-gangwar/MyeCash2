package in.myecash.appbase.utilities;

/**
 * Created by adgangwa on 15-02-2017.
 */

import android.os.SystemClock;
import android.view.View;

import in.myecash.appbase.constants.AppConstants;

public abstract class OnSingleClickListener implements View.OnClickListener {
    private long mLastClickTime;

    public abstract void onSingleClick(View v);

    @Override
    public final void onClick(View v) {
        long currentClickTime=SystemClock.uptimeMillis();
        long elapsedTime=currentClickTime-mLastClickTime;

        mLastClickTime=currentClickTime;

        if(elapsedTime<= AppConstants.MIN_CLICK_INTERVAL)
            return;

        onSingleClick(v);
    }
}