package in.myecash.appbase;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.LogMy;

/**
 * Created by adgangwa on 15-02-2017.
 */

public abstract class BaseDialog extends DialogFragment implements
        DialogInterface.OnClickListener, View.OnTouchListener {
    private static final String TAG = "BaseApp-BaseDialog";

    /*
     * Below code is same in BaseFragment also
     */
    private long mLastClickTime;
    private long mLastTouchTime;

    // Anstract fxs to handle 'Btn Click' and 'Touch on Text fields'
    public abstract boolean handleTouchUp(View v);
    public abstract void handleBtnClick(DialogInterface dialog, int which);

    @Override
    public void onClick(DialogInterface dialog, int which) {
        long currentClickTime= SystemClock.uptimeMillis();
        long elapsedTime=currentClickTime-mLastClickTime;

        mLastClickTime=currentClickTime;

        if(elapsedTime<=AppConstants.MIN_CLICK_INTERVAL)
            return;

        handleBtnClick(dialog, which);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        long currentClickTime= SystemClock.uptimeMillis();
        long elapsedTime=currentClickTime-mLastTouchTime;
        mLastTouchTime=currentClickTime;

        if(elapsedTime<=AppConstants.MIN_CLICK_INTERVAL || event.getAction()!=MotionEvent.ACTION_UP) {
            LogMy.d(TAG,"Double Touch Case");
            return true;
        }

        return handleTouchUp(v);
    }
}
