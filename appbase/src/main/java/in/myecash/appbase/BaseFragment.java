package in.myecash.appbase;

import android.app.Fragment;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.LogMy;

/**
 * Created by adgangwa on 15-02-2017.
 */

public abstract class BaseFragment extends Fragment implements View.OnTouchListener, View.OnClickListener {

    private static final String TAG = "BaseApp-BaseFragment";

    /*
     * Below code is same in BaseFragment also
     */
    private long mLastClickTime;
    private long mLastTouchTime;

    // Anstract fxs to handle 'Btn Click' and 'Touch on Text fields'
    public abstract boolean handleTouchUp(View v);
    public abstract void handleBtnClick(View v);

    @Override
    public void onClick(View v) {
        long currentClickTime= SystemClock.uptimeMillis();
        long elapsedTime=currentClickTime-mLastClickTime;
        mLastClickTime=currentClickTime;

        if(elapsedTime<= AppConstants.MIN_CLICK_INTERVAL) {
            LogMy.d(TAG,"Double Click Case");
            return;
        }

        handleBtnClick(v);
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
