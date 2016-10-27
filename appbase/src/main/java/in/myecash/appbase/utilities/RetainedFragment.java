package in.myecash.appbase.utilities;

import android.app.Fragment;
import android.os.Bundle;

/**
 * Created by adgangwa on 17-07-2016.
 */
public abstract class RetainedFragment extends Fragment {
    private static final String TAG = "RetainedFragment";

    // Container Activity must implement this interface
    public interface RetainedFragmentIf {
        void onBgThreadCreated();
        void onBgProcessResponse(int errorCode, int operation);
    }

    // Activity callback
    protected RetainedFragmentIf mCallback;

    protected boolean mReady = false;
    protected boolean mQuiting = false;
    // Single background worker thread hosted in this retained fragment
    //protected BackgroundProcessor<String> mBackgroundProcessor;

    final BackgroundProcessor.BackgroundProcessorListener mListener = new BackgroundProcessor.BackgroundProcessorListener() {
        @Override
        public void onResult(int errorCode, int operation) {

            // Update our shared state with the UI.
            synchronized (this) {
                // Our thread will not process response, if the UI is not ready
                while (!mReady) {
                    if (mQuiting) {
                        return;
                    }
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }

                LogMy.d(TAG,"In onResult: "+operation);
                mCallback.onBgProcessResponse(errorCode, operation);
            }
        }
    };

    /**
     * Fragment initialization.  We way we want to be retained and start our thread.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Tell the framework to try to keep this fragment around
        // during a configuration change.
        setRetainInstance(true);

        // setup background thread
        // to be done only once (as setRetainInstance = true), so doing it here
        BackgroundProcessor<String> bgProcessor = getBackgroundProcessor();
        if(bgProcessor!=null) {
            LogMy.d(TAG,"Initializing background thread.");
            //Handler responseHandler = new Handler();
            //mBackgroundProcessor = new MyBackgroundProcessor<>(responseHandler, this);
            //mBackgroundProcessor = getBackgroundProcessor();
            bgProcessor.setOnResultListener(mListener);
            bgProcessor.start();
            bgProcessor.getLooper();
        }


    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (RetainedFragmentIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement RetainedFragmentIf");
        }

        doOnActivityCreated();
        // background thread will be initiated fully by now
        mCallback.onBgThreadCreated();

        // We are ready for any waiting thread response to go.
        synchronized (mListener) {
            mReady = true;
            mListener.notify();
        }

    }

    protected abstract void doOnActivityCreated();
    protected abstract BackgroundProcessor<String> getBackgroundProcessor();

    /**
     * This is called when the fragment is going away.  It is NOT called
     * when the fragment is being propagated between activity instances.
     */
    @Override
    public void onDestroy() {
        synchronized (mListener) {
            mReady = false;
            mQuiting = true;
            mListener.notify();
        }

        // Make the thread go away.
        getBackgroundProcessor().quit();

        doOnDestroy();

        LogMy.i(TAG, "Background thread destroyed");
        super.onDestroy();
    }

    protected abstract void doOnDestroy();

    /**
     * This is called right before the fragment is detached from its
     * current activity instance.
     */
    @Override
    public void onDetach() {
        LogMy.d(TAG,"In onDetach");
        // This fragment is being detached from its activity.  We need
        // to make sure its thread is not going to touch any activity
        // state after returning from this function.
        synchronized (mListener) {
            mCallback = null;
            mReady = false;
            mListener.notify();
        }

        super.onDetach();
    }
}

