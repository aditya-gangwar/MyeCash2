package in.myecash.commonbase.utilities;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

/**
 * Created by adgangwa on 17-07-2016.
 */
public abstract class BackgroundProcessor<T> extends HandlerThread {
    private final static String TAG = "BackgroundProcessor";

    protected Handler mRequestHandler;
    protected Handler mResponseHandler;
    protected BackgroundProcessorListener mListener;

    public interface BackgroundProcessorListener {
        void onResult(int errorCode, int operationCode);
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
                final int errorCode = handleMsg(msg);
                mResponseHandler.post(new Runnable() {
                    public void run() {
                        mListener.onResult(errorCode, currOperation);
                    }
                });
            }
        };
    }

    protected abstract int handleMsg(Message msg);

}

