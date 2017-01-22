package in.myecash.appbase.utilities;

import android.app.Fragment;
import android.os.Bundle;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by adgangwa on 17-07-2016.
 */
public abstract class RetainedFragment extends Fragment {
    private static final String TAG = "BaseApp-RetainedFragment";

    // Container Activity must implement this interface
    public interface RetainedFragmentIf {
        void onBgThreadCreated();
        void onBgProcessResponse(int errorCode, int operation);
    }

    // Activity callback
    protected RetainedFragmentIf mCallback;

    // An issue is observeed in SDK 19 that when back button is pressed after onPause()
    // but before onResume(), then it was causing crash.
    // Like, pressing back immediatly after screen unlock (locked when any fragment except mobile number one was visible)
    protected Boolean resumeOk = false;
    protected boolean mReady = false;
    protected boolean mQuiting = false;
    //private final Object lock = new Object();
    final Lock lock = new ReentrantLock();
    final Condition nowReady = lock.newCondition();


    // Single background worker thread hosted in this retained fragment
    //protected BackgroundProcessor<String> mBackgroundProcessor;

    public Boolean getResumeOk() {
        return resumeOk;
    }

    public void setResumeOk(Boolean resumeOk) {
        /*synchronized (lock) {
            LogMy.d(TAG,"Resume Ok: "+resumeOk);
            this.resumeOk = resumeOk;
            lock.notifyAll();
        }*/
        lock.lock();
        try {
            this.resumeOk = resumeOk;
            LogMy.d(TAG,"Resume Ok: "+resumeOk+", "+nowReady.toString());
            nowReady.signal();
        } finally {
            lock.unlock();
        }

    }

    final BackgroundProcessor.BackgroundProcessorListener mListener = new BackgroundProcessor.BackgroundProcessorListener() {

        @Override
        public boolean isQuitting() {
            //synchronized (this) {
                return mQuiting;
            //}
        }

        @Override
        public boolean isUiReady() {
            lock.lock();
            try {
                while (!mReady || !getResumeOk()) {
                    LogMy.d(TAG,"Calling activity not available: waiting: "+mReady+", "+getResumeOk());

                    if (mQuiting) {
                        return false;
                    }

                    try {
                        LogMy.d(TAG,"Before wait: "+nowReady.toString());
                        nowReady.await();
                        LogMy.d(TAG,"After wait");
                    } catch (InterruptedException e) {
                        LogMy.d(TAG,"Exception while waiting: "+e.getMessage());
                    }
                }
                return true;
            } finally {
                lock.unlock();
            }
            /*synchronized (lock) {
                while (!mReady || !getResumeOk()) {
                    LogMy.d(TAG,"Calling activity not available: waiting: "+mReady+", "+getResumeOk());

                    if (mQuiting) {
                        return false;
                    }

                    try {
                        LogMy.d(TAG,"Before wait");
                        lock.wait();
                        LogMy.d(TAG,"After wait");
                    } catch (InterruptedException e) {
                        LogMy.d(TAG,"Exception while waiting: "+e.getMessage());
                    }
                }
                return true;
            }*/
        }

        @Override
        public void onResult(int errorCode, int operation) {

            // Update our shared state with the UI.
            //synchronized (this) {
                // Our thread will not process response, if the UI is not ready
                /*while (!mReady || !getResumeOk()) {
                    LogMy.d(TAG,"Calling activity not available: waiting: "+mReady+", "+getResumeOk());

                    if (mQuiting) {
                        return;
                    }
                    try {
                        LogMy.d(TAG,"Before wait");
                        wait();
                        LogMy.d(TAG,"After wait");
                    } catch (InterruptedException e) {
                    }
                }*/

                LogMy.d(TAG,"In onResult: "+operation);
                mCallback.onBgProcessResponse(errorCode, operation);
            //}
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

        // We are ready for any waiting thread response to go.
        /*synchronized (lock) {
            mReady = true;
            lock.notifyAll();
        }*/
        lock.lock();
        try {
            mReady = true;
            nowReady.signal();
        } finally {
            lock.unlock();
        }

        // background thread will be initiated fully by now
        mCallback.onBgThreadCreated();
    }

    protected abstract void doOnActivityCreated();
    protected abstract BackgroundProcessor<String> getBackgroundProcessor();

    /**
     * This is called when the fragment is going away.  It is NOT called
     * when the fragment is being propagated between activity instances.
     */
    @Override
    public void onDestroy() {
        /*synchronized (lock) {
            mReady = false;
            mQuiting = true;
            lock.notifyAll();
        }*/
        lock.lock();
        try {
            mReady = false;
            mQuiting = true;
            nowReady.signal();
        } finally {
            lock.unlock();
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
        /*synchronized (lock) {
            mCallback = null;
            mReady = false;
            lock.notifyAll();
        }*/
        lock.lock();
        try {
            mReady = false;
            mCallback = null;
            nowReady.signal();
        } finally {
            lock.unlock();
        }


        super.onDetach();
    }

    @Override
    public void onPause() {
        LogMy.d(TAG,"In onPause: ");
        super.onPause();
    }

    @Override
    public void onResume() {
        LogMy.d(TAG,"In onResume: ");
        super.onResume();
    }


}

