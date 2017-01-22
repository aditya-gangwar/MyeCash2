package in.myecash.merchantbase.helper;

import android.app.Activity;
import android.graphics.Typeface;
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

import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.merchantbase.R;

import java.io.Serializable;
import java.util.TreeSet;

/**
 * Created by adgangwa on 16-06-2016.
 */
public class CashPaid implements Serializable, View.OnTouchListener {
    public static final String TAG = "MchntApp-CashPaid";

    // show next 4 values
    private static final int UI_SLOT_COUNT = 4;
    private static int[] currency = {10, 50, 100, 500, 1000, 2000};

    private static final Integer TAG_BTN_AS_CLEAR = 100;
    private static final Integer TAG_BTN_AS_DONE = 200;

    //private Dialog mDialog;
    int mMinCashToPay;
    int mBillAmt;
    //String mLastSetAmt;
    //int mCashPaid;
    Activity mActivity;
    CashPaidIf mCallback;
    //private int mValues[];
    TreeSet<Integer> mValues;

    public interface CashPaidIf {
        //void onAmountEnter(int value);
        void onAmountEnterFinal(int value, boolean clearCase);
    }

    /*
    public static int getLayoutResId() {
        return R.layout.dialog_cash_paid_3;
    }*/

    public CashPaid(int minCashToPay, int billAmt, CashPaidIf callback, Activity activity) {
        mActivity = activity;
        mCallback = callback;
        mMinCashToPay = minCashToPay;
        mBillAmt = billAmt;
        //mLastSetAmt = lastSetAmt;
        //mValues = new int[UI_SLOT_COUNT];
        mValues = new TreeSet<>();
        mInputCashPay = new EditText[UI_SLOT_COUNT];
    }
    
    public void initView(View v) {
        LogMy.d(TAG,"In initView");
        initUiResources(v);
        //if(mBtnClear.getTag()==null) {
            // not yet set - means first init of the screen and not fragment restore case
            // set to default i.e. to clear
            setBtnAsClear();
        //}
        buildValueSet();
        setValuesInUi();
        setListeners();
    }

    /*public int getCashPaidAmt() {
        return mInputAmt.getText().toString().isEmpty() ? 0 : Integer.parseInt(mInputAmt.getText().toString());
    }*/

    /*public void setError(String error) {
        mInputAmt.setError(error);
    }

    public String getError() {
        return mInputAmt.getError()==null ? null : mInputAmt.getError().toString();
    }*/

    public void refreshValues(int minCashToPay, int oldValue, int billAmt) {
        LogMy.d(TAG,"In refreshValues: "+minCashToPay+" ,"+oldValue);
        mMinCashToPay = minCashToPay;
        mBillAmt = billAmt;

        buildValueSet();
        setValuesInUi();

        // restore earlier selected amount - if still valid
        boolean foundMatch = false;
        //Integer oldValue = (Integer)mInputAmt.getTag();
        LogMy.d(TAG,"In refreshValues, oldValue: "+oldValue);

        if(oldValue!=0 && oldValue >= minCashToPay) {

            // first check if old amount matches old custom amount
            String val = mInputAmt.getText().toString();
            if(!val.isEmpty() &&
                    oldValue == AppCommonUtil.getValueAmtStr(mInputAmt.getText().toString())) {
                foundMatch = true;
                markInputAmt(mInputAmt);
                setBtnAsClear(); //not required, but jst to be safe
                //mCallback.onAmountEnterFinal(AppCommonUtil.getValueAmtStr(mInputAmt.getText().toString()));

            } else {
                clearCustomAmt();

                // also check if matches any of slots
                for(int i=0; i<UI_SLOT_COUNT; i++) {
                    if(mInputCashPay[i].isEnabled() &&
                            oldValue == AppCommonUtil.getValueAmtStr(mInputCashPay[i].getText().toString())) {
                        foundMatch = true;
                        markInputAmt(mInputCashPay[i]);
                        //mCallback.onAmountEnterFinal(AppCommonUtil.getValueAmtStr(mInputCashPay[i].getText().toString()));
                    }
                }
            }
        }

        if(!foundMatch) {
            LogMy.d(TAG,"Match not found with old value: "+oldValue);
            // remove/reset tag
            //mInputAmt.setTag(-1);
            clearCustomAmt();
            mCallback.onAmountEnterFinal(0, false);
        }
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
                    //tempNewAmt = mMinCashToPay;
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

                unmarkInputAmt(mInputCashPay[index]);

                mInputCashPay[index].setText(AppCommonUtil.getAmtStr(val));
                index++;
            }
        }

        // disable remaining slots - if any
        for(int i=index; i<UI_SLOT_COUNT; i++) {
            //mInputCashPay[i].setText(AppCommonUtil.getAmtStr(0));
            mInputCashPay[i].setText("");
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
        //mInputAmt.setOnClickListener(this);

        mInputAmt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final Activity activity = mActivity;
                if(event.getAction() == MotionEvent.ACTION_UP) {

                    // show the keyboard and adjust screen for the same
                    activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

                    // remove rupee symbol
                    String uiVal = mInputAmt.getText().toString();
                    if(!uiVal.isEmpty()) {
                        //setCustomAmtText( String.valueOf(AppCommonUtil.getValueAmtStr(mInputAmt.getText().toString())) );
                        setCustomAmtText("");
                        mInputAmt.setHint(String.valueOf(AppCommonUtil.getValueAmtStr(uiVal)));
                    }
                    mInputAmt.setCursorVisible(true);
                }
                return false;
            }
        });

        mInputAmt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                LogMy.d(TAG,"In initUiResources");
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    handleCustomAmtEnter();
                }
                return false;
            }
        });

        mInputAmt.addTextChangedListener(textWatcher);

        mBtnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogMy.d(TAG,"In initUiResources: "+ mBtnClear.getTag());
                if(isBtnAsClear()) {
                    // remove any value in mInputAmt
                    clearCustomAmt();
                    // reset highlight on other enabled slots
                    for(int i=0; i<UI_SLOT_COUNT; i++) {
                        if(mInputCashPay[i].isEnabled()) {
                            unmarkInputAmt(mInputCashPay[i]);
                        }
                    }
                    // remove/reset tag
                    //mInputAmt.setTag(-1);
                    mCallback.onAmountEnterFinal(0,true);

                } else {
                    handleCustomAmtEnter();
                }
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP) {
            LogMy.d(TAG,"In onTouch: "+v.getId());

            int vId = v.getId();
            for(int i=0; i<UI_SLOT_COUNT; i++) {
                if( vId==mInputCashPay[i].getId() ) {
                    // remove any value in mInputAmt
                    clearCustomAmt();

                    // highlight touched amount
                    markInputAmt(mInputCashPay[i]);
                    int value = AppCommonUtil.getValueAmtStr(mInputCashPay[i].getText().toString());
                    mCallback.onAmountEnterFinal(value, false);
                    // store selected value as tag
                    //mInputAmt.setTag(value);

                } else if(mInputCashPay[i].isEnabled()) {
                    unmarkInputAmt(mInputCashPay[i]);
                }
            }
        }
        return true;
    }

    private void handleCustomAmtEnter() {
        LogMy.d(TAG,"In handleCustomAmtEnter");

        if(mInputAmt.getText().toString().isEmpty()) {
            // to do other tasks - like clearFocus, unmark etc
            clearCustomAmt();
            return;
        }

        int value = Integer.parseInt(mInputAmt.getText().toString());
        if(value >= mMinCashToPay) {
            AppCommonUtil.hideKeyboard(mActivity);

            // add rupee symbol
            setCustomAmtText(AppCommonUtil.getAmtStr(value));
            mInputAmt.setCursorVisible(false);
            mInputAmt.clearFocus();

            markInputAmt(mInputAmt);
            mCallback.onAmountEnterFinal(value, false);
            // store selected value as tag
            //mInputAmt.setTag(value);

            setBtnAsClear();
            for(int i=0; i<UI_SLOT_COUNT; i++) {
                if(mInputCashPay[i].isEnabled()) {
                    unmarkInputAmt(mInputCashPay[i]);
                }
            }
        } else {
            mInputAmt.setCursorVisible(true);
            mInputAmt.setError("Minimum "+AppCommonUtil.getAmtStr(mMinCashToPay)+" required");
        }
    }

    private void setBtnAsClear() {
        LogMy.d(TAG,"In setBtnAsClear");
        if(TAG_BTN_AS_CLEAR != mBtnClear.getTag()) {
            mBtnClear.setTag(TAG_BTN_AS_CLEAR);
            mBtnClear.setImageDrawable(AppCommonUtil.getTintedDrawable(mActivity,R.drawable.ic_cancel_white_24dp,R.color.icon_grey));
        }
    }

    private void setBtnAsDone() {
        LogMy.d(TAG,"In setBtnAsDone");
        if(TAG_BTN_AS_DONE != mBtnClear.getTag()) {
            mBtnClear.setTag(TAG_BTN_AS_DONE);
            mBtnClear.setImageDrawable(AppCommonUtil.getTintedDrawable(mActivity, R.drawable.ic_check_circle_white_24dp, R.color.green_positive));
        }
    }

    private boolean isBtnAsClear() {
        LogMy.d(TAG,"In isBtnAsClear");
        return (TAG_BTN_AS_CLEAR == mBtnClear.getTag());
    }

    private void markInputAmt(EditText et) {
        LogMy.d(TAG,"In markInputAmt");
        et.setTextColor(ContextCompat.getColor(mActivity, R.color.green_positive));
        et.setTypeface(null, Typeface.BOLD);
        //et.setBackgroundResource(R.drawable.round_rect_border_selected);
    }

    private void unmarkInputAmt(EditText et) {
        LogMy.d(TAG,"In unmarkInputAmt");
        et.setTextColor(ContextCompat.getColor(mActivity, R.color.secondary_text));
        et.setTypeface(null, Typeface.NORMAL);
        //et.setBackgroundResource(0);
    }

    private void clearCustomAmt() {
        LogMy.d(TAG,"In clearCustomAmt");
        mInputAmt.setHint(R.string.cash_paid_other_label);
        unmarkInputAmt(mInputAmt);
        setCustomAmtText("");
        mInputAmt.setCursorVisible(false);
        mInputAmt.clearFocus();

        setBtnAsClear();
    }

    private void setCustomAmtText(String str) {
        // to avoid loop with textChangedListener
        mInputAmt.removeTextChangedListener(textWatcher);
        mInputAmt.setText(str);
        mInputAmt.addTextChangedListener(textWatcher);
    }

    private EditText[] mInputCashPay;
    private EditText mInputAmt;
    private AppCompatImageButton mBtnClear;

    private void initUiResources(View v) {
        LogMy.d(TAG,"In initUiResources");
        mInputCashPay[0] = (EditText) v.findViewById(R.id.choice_cash_pay_1);
        mInputCashPay[1] = (EditText) v.findViewById(R.id.choice_cash_pay_2);
        mInputCashPay[2] = (EditText) v.findViewById(R.id.choice_cash_pay_3);
        mInputCashPay[3] = (EditText) v.findViewById(R.id.choice_cash_pay_4);
        mInputAmt = (EditText) v.findViewById(R.id.choice_cash_pay_custom);
        mBtnClear = (AppCompatImageButton) v.findViewById(R.id.btn_clear);
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        public void afterTextChanged(Editable s) {
            if (s.length() > 0) {
                setBtnAsDone();
                /*int value = Integer.parseInt(s.toString());
                if(mCallback != null && (value >= mMinCashToPay || value==0) ) {
                    //mCallback.onAmountEnter(value);
                    setBtnAsDone();
                } else {
                    unmarkInputAmt(mInputAmt);
                    setBtnAsClear();
                }*/
            } else {
                //clearCustomAmt();
                mInputAmt.setHint(R.string.cash_paid_other_label);
                unmarkInputAmt(mInputAmt);
                setBtnAsClear();
                //mCallback.onAmountEnter(0);
            }
        }
    };

}
