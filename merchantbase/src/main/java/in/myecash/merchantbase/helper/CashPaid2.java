package in.myecash.merchantbase.helper;

import android.app.Activity;
import android.app.FragmentManager;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.merchantbase.NumberInputDialog;
import in.myecash.merchantbase.R;

import java.io.Serializable;
import java.util.TreeSet;

/**
 * Created by adgangwa on 16-06-2016.
 */
public class CashPaid2 implements Serializable, View.OnTouchListener {
    public static final String TAG = "MchntApp-CashPaid2";

    // show next 4 values
    private static final int UI_SLOT_COUNT = 5;
    private static int[] currency = {10, 50, 100, 500, 2000};

    int mMinCashToPay;
    int mBillAmt;
    Activity mActivity;
    CashPaid2If mCallback;
    TreeSet<Integer> mValues;
    boolean[] markedStatus;
    long lastCustomAmtClickTime;

    public interface CashPaid2If {
        void onAmountEnterFinal(int value, boolean clearCase);
        void collectCustomAmount(String curValue, int minValue);
    }

    public CashPaid2(int minCashToPay, int billAmt, CashPaid2If callback, Activity activity) {
        mActivity = activity;
        mCallback = callback;
        mMinCashToPay = minCashToPay;
        mBillAmt = billAmt;
        mValues = new TreeSet<>();
        mInputCashPay = new EditText[UI_SLOT_COUNT];
        markedStatus = new boolean[UI_SLOT_COUNT];
    }

    public void initView(View v) {
        LogMy.d(TAG,"In initView");
        initUiResources(v);
        buildValueSet();
        setValuesInUi();
        setListeners();
    }

    public void refreshValues(int minCashToPay, int oldValue, int billAmt) {
        LogMy.d(TAG,"In refreshValues: "+minCashToPay+" ,"+oldValue);
        mMinCashToPay = minCashToPay;
        mBillAmt = billAmt;

        buildValueSet();
        setValuesInUi();

        // restore earlier selected amount - if still valid
        boolean foundMatch = false;
        LogMy.d(TAG,"In refreshValues, oldValue: "+oldValue);

        if(oldValue!=0 && oldValue >= minCashToPay) {

            // first check if old amount matches old custom amount
            String val = mInputAmt.getText().toString();
            if(!val.isEmpty() &&
                    oldValue == AppCommonUtil.getValueAmtStr(mInputAmt.getText().toString())) {
                foundMatch = true;
                markInputAmt(mInputAmt);

            } else {
                clearCustomAmt();

                // also check if matches any of slots
                for(int i=0; i<UI_SLOT_COUNT; i++) {
                    if(mInputCashPay[i].isEnabled() &&
                            oldValue == AppCommonUtil.getValueAmtStr(mInputCashPay[i].getText().toString())) {
                        foundMatch = true;
                        markInputAmt(i);
                    }
                }
            }
        }

        if(!foundMatch) {
            LogMy.d(TAG,"Match not found with old value: "+oldValue);
            // remove/reset tag
            clearCustomAmt();
            mCallback.onAmountEnterFinal(0, false);
        }
    }

    public void onCustomAmtEnter(String newValue) {
        setCustomAmtText(newValue);
        handleCustomAmtEnter();
    }

    private void buildValueSet() {
        LogMy.d(TAG, "In buildValueSet");
        int rem = 0, tempNewAmt = 0, lastSetAmt = 0;
        mValues.clear();

        // add 'min cash to pay' and 'bill amount'
        if(mMinCashToPay!=0) {
            mValues.add(mMinCashToPay);
            mValues.add(mBillAmt);
        }
        // now iterate and fill remaining values
        for (int n : currency) {
            // check for free slot
            if (mValues.size() < UI_SLOT_COUNT) {
                if(mMinCashToPay<=0) {
                    tempNewAmt = n;
                } else {
                    rem = mMinCashToPay % n;
                    tempNewAmt = (rem == 0) ? mMinCashToPay : (mMinCashToPay + (n - rem));
                }
                mValues.add(tempNewAmt);
            }
        }
    }

    private void setValuesInUi() {
        LogMy.d(TAG, "In setValuesInUi");

        // Set calculated values
        int index = 0;
        for (Integer val : mValues) {
            if(index<UI_SLOT_COUNT) {
                mInputCashPay[index].setEnabled(true);
                mInputCashPay[index].setAlpha(1.0f);

                //unmarkInputAmt(mInputCashPay[index]);
                unmarkInputAmt(index);

                mInputCashPay[index].setText(AppCommonUtil.getAmtStr(val));
                index++;
            }
        }

        // disable remaining slots - if any
        for(int i=index; i<UI_SLOT_COUNT; i++) {
            unmarkInputAmt(i);
            mInputCashPay[i].setText(AppCommonUtil.getAmtStr(0));
            //mInputCashPay[i].setText(AppConstants.SYMBOL_RS);
            mInputCashPay[i].setEnabled(false);
            mInputCashPay[i].setAlpha(0.4f);
        }
    }

    private void setListeners() {
        LogMy.d(TAG, "In setListeners");

        for(int i=0; i<UI_SLOT_COUNT; i++) {
            if(mInputCashPay[i].isEnabled()) {
                mInputCashPay[i].setOnTouchListener(this);
            }
        }

        mInputAmt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    long currentClickTime= SystemClock.uptimeMillis();
                    long elapsedTime=currentClickTime-lastCustomAmtClickTime;
                    lastCustomAmtClickTime=currentClickTime;

                    if(elapsedTime >= AppConstants.MIN_CLICK_INTERVAL) {
                        String uiVal = mInputAmt.getText().toString();
                        if (uiVal.isEmpty()) {
                            uiVal = "0";
                        } else {
                            //remove rupee symbol
                            uiVal = uiVal.replace(AppConstants.SYMBOL_RS, "");
                        }
                        mCallback.collectCustomAmount(uiVal, mMinCashToPay);
                    }
                }
                return false;
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP) {
            LogMy.d(TAG,"In onTouch: "+v.getId());

            int vId = v.getId();
            for(int i=0; i<UI_SLOT_COUNT; i++) {
                if( vId==mInputCashPay[i].getId() && mInputCashPay[i].isEnabled()) {
                    if(markedStatus[i]) {
                        // already marked - clear it
                        unmarkInputAmt(i);
                        mCallback.onAmountEnterFinal(0,true);
                    } else {
                        // not marked - select it
                        clearAllMarks();
                        markInputAmt(i);
                        int value = AppCommonUtil.getValueAmtStr(mInputCashPay[i].getText().toString());
                        mCallback.onAmountEnterFinal(value, false);
                    }
                }
            }
        }
        return true;
    }

    private void clearAllMarks() {
        clearCustomAmt();
        // reset highlight on other enabled slots
        for(int i=0; i<UI_SLOT_COUNT; i++) {
            if(mInputCashPay[i].isEnabled()) {
                unmarkInputAmt(i);
            }
        }
    }

    private void handleCustomAmtEnter() {
        LogMy.d(TAG,"In handleCustomAmtEnter");

        if(mInputAmt.getText().toString().isEmpty() ||
                mInputAmt.getText().toString().equals("0")) {
            // to do other tasks - like clearFocus, unmark etc
            clearCustomAmt();
            if(!anyFixedValueMarked()) {
                mCallback.onAmountEnterFinal(0, true);
            }
            return;
        }

        int value = Integer.parseInt(mInputAmt.getText().toString());
        if(value >= mMinCashToPay) {
            setCustomAmtText(AppCommonUtil.getAmtStr(value));
            markInputAmt(mInputAmt);
            mCallback.onAmountEnterFinal(value, false);

            for(int i=0; i<UI_SLOT_COUNT; i++) {
                if(mInputCashPay[i].isEnabled()) {
                    unmarkInputAmt(i);
                }
            }
        } else if(value==0) {
            clearCustomAmt();
            if(!anyFixedValueMarked()) {
                mCallback.onAmountEnterFinal(0, true);
            }
        }else {
            //mInputAmt.setCursorVisible(true);
            mInputAmt.setError("Minimum "+AppCommonUtil.getAmtStr(mMinCashToPay)+" required");
        }
    }

    private boolean anyFixedValueMarked() {
        for(int i=0; i<UI_SLOT_COUNT; i++) {
            if(mInputCashPay[i].isEnabled() && markedStatus[i]) {
                return true;
            }
        }
        return false;
    }

    private void markInputAmt(int i) {
        LogMy.d(TAG,"In markInputAmt: "+i);
        markInputAmt(mInputCashPay[i]);
        markedStatus[i] = true;
    }
    private void markInputAmt(EditText et) {
        LogMy.d(TAG,"In markInputAmt");
        et.setTextColor(ContextCompat.getColor(mActivity, R.color.green_positive));
        et.setTypeface(null, Typeface.BOLD);
        et.setBackgroundResource(R.drawable.round_rect_border_selected);
    }

    private void unmarkInputAmt(int i) {
        LogMy.d(TAG,"In unmarkInputAmt: "+i);
        unmarkInputAmt(mInputCashPay[i]);
        markedStatus[i] = false;
    }
    private void unmarkInputAmt(EditText et) {
        LogMy.d(TAG,"In unmarkInputAmt");
        et.setTextColor(ContextCompat.getColor(mActivity, R.color.secondary_text));
        et.setTypeface(null, Typeface.NORMAL);
        et.setBackgroundResource(0);
    }

    private void clearCustomAmt() {
        LogMy.d(TAG,"In clearCustomAmt");
        mInputAmt.setHint(R.string.cash_paid_other_label);
        unmarkInputAmt(mInputAmt);
        setCustomAmtText("");
        //mInputAmt.setCursorVisible(false);
        //mInputAmt.clearFocus();
        mInputAmt.setError(null);
    }

    private void setCustomAmtText(String str) {
        // to avoid loop with textChangedListener
        mInputAmt.setText(str);
    }

    private EditText[] mInputCashPay;
    private EditText mInputAmt;

    private void initUiResources(View v) {
        LogMy.d(TAG,"In initUiResources");
        mInputCashPay[0] = (EditText) v.findViewById(R.id.choice_cash_pay_1);
        mInputCashPay[1] = (EditText) v.findViewById(R.id.choice_cash_pay_2);
        mInputCashPay[2] = (EditText) v.findViewById(R.id.choice_cash_pay_3);
        mInputCashPay[3] = (EditText) v.findViewById(R.id.choice_cash_pay_4);
        mInputCashPay[4] = (EditText) v.findViewById(R.id.choice_cash_pay_5);
        mInputAmt = (EditText) v.findViewById(R.id.choice_cash_pay_custom);
    }
}
