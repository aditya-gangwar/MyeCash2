package in.myecash.appbase.utilities;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import in.myecash.common.constants.ErrorCodes;

/**
 * Created by adgangwa on 17-07-2016.
 */
public abstract class BackgroundProcessor<T> extends HandlerThread {
    private final static String TAG = "BaseApp-BackgroundProcessor";

    protected Handler mRequestHandler;
    protected Handler mResponseHandler;
    protected BackgroundProcessorListener mListener;

    public interface BackgroundProcessorListener {
        void onResult(int errorCode, int operationCode);
        boolean isUiReady();
        boolean isQuitting();
    }
    public void setOnResultListener(BackgroundProcessorListener listener) {
        mListener = listener;
    }

    public BackgroundProcessor(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                // return result to calling UI thread
                final int currOperation = msg.what;
                //final int errorCode = (err==ErrorCodes.NO_ERROR) ? handleMsg(msg) : err;
                final int errorCode = handleMsg(msg);

                /*if(mListener.isQuitting()) {
                    LogMy.d(TAG,"Fragment is quitting: "+currOperation+", "+errorCode);
                    return;
                }*/

                /*while (!mListener.isUiReady()) {
                    try {
                        LogMy.d(TAG,"Before wait");
                        wait();
                        LogMy.d(TAG,"After wait");
                    } catch (InterruptedException e) {
                    }
                }*/

                if(mListener.isUiReady()) {
                    LogMy.d(TAG,"UI is ready");
                    mResponseHandler.post(new Runnable() {
                        public void run() {
                            mListener.onResult(errorCode, currOperation);
                        }
                    });
                }
            }
        };
    }

    protected abstract int handleMsg(Message msg);

}

