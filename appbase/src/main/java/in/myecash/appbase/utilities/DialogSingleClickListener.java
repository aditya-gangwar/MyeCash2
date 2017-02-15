package in.myecash.appbase.utilities;

import android.content.DialogInterface;
import android.os.SystemClock;
import android.view.View;

/**
 * Created by adgangwa on 15-02-2017.
 */

public abstract class DialogSingleClickListener implements DialogInterface.OnClickListener {

    private static final long MIN_CLICK_INTERVAL=2000;
    private long mLastClickTime;

    public abstract void onSingleClick(DialogInterface dialog, int which);

    @Override
    public void onClick(DialogInterface dialog, int which) {
        long currentClickTime= SystemClock.uptimeMillis();
        long elapsedTime=currentClickTime-mLastClickTime;

        mLastClickTime=currentClickTime;

        if(elapsedTime<=MIN_CLICK_INTERVAL)
            return;

        onSingleClick(dialog, which);
    }
}
