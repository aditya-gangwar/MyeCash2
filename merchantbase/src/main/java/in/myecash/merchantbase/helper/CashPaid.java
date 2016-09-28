package in.myecash.merchantbase.helper;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.merchantbase.R;

import java.io.Serializable;

/**
 * Created by adgangwa on 16-06-2016.
 */
public class CashPaid implements Serializable, View.OnTouchListener {
    public static final String TAG = "CashPaid";

    // show next 3 values
    private static final int UI_SLOT_COUNT = 3;
    private static int[] currency = {10, 50, 100, 500, 1000};

    //private Dialog mDialog;
    int mMinCashToPay;
    String mLastSetAmt;
    //int mCashPaid;
    Context mContext;
    CashPaidIf mCallback;
    private int mValues[];

    public interface CashPaidIf {
        void onAmountEnter(int value);
        void onAmountEnterFinal();
    }

    /*
    public static int getLayoutResId() {
        return R.layout.dialog_cash_paid_3;
    }*/

    public CashPaid(int minCashToPay, String lastSetAmt, CashPaidIf callback, Context context) {
        mContext= context;
        mCallback = callback;
        mMinCashToPay = minCashToPay;
        mLastSetAmt = lastSetAmt;
        mValues = new int[UI_SLOT_COUNT];
        mInputCashPay = new EditText[UI_SLOT_COUNT];
    }
    
    public void initView(View v) {
        initUiResources(v);
        buildValueSet();
        setValuesInUi();
        setListeners();
    }

    public int getCashPaidAmt() {
        return mInputAmt.getText().toString().isEmpty() ? 0 : Integer.parseInt(mInputAmt.getText().toString());
    }

    public void setError(String error) {
        mInputAmt.setError(error);
    }

    public String getError() {
        return mInputAmt.getError()==null ? null : mInputAmt.getError().toString();
    }

    public void refreshValues(int minCashToPay) {
        mMinCashToPay = minCashToPay;
        buildValueSet();
        setValuesInUi();
    }

    /*
    @Override
    public void onClick(View v) {
        int vId = v.getId();
        LogMy.d(TAG, "In onClick: " + vId);

        for(int i=0; i<UI_SLOT_COUNT; i++) {
            if( vId==mInputCashPay[i].getId() ) {
                mInputAmt.setText(String.valueOf(mValues[i]));
                mCallback.onAmountEnterFinal();
            }
        }
    }*/

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP) {
            LogMy.d(TAG,"In onTouch: "+v.getId());

            int vId = v.getId();
            for(int i=0; i<UI_SLOT_COUNT; i++) {
                if( vId==mInputCashPay[i].getId() ) {
                    mInputAmt.setText(String.valueOf(mValues[i]));
                    mCallback.onAmountEnterFinal();
                }
            }
        }
        return true;
    }

    private void setValuesInUi() {
        LogMy.d(TAG, "In setValuesInUi");

        // Set calculated values
        //int index = 0;
        for(int i=0; i<UI_SLOT_COUNT; i++) {
            if(mValues[i] > 0) {
                mInputCashPay[i].setText(AppCommonUtil.getAmtStr(mValues[i]));
            } else {
                // disable the slot
                mInputCashPay[i].setEnabled(false);
                mInputCashPay[i].setTextColor(ContextCompat.getColor(mContext, R.color.disabled));
                mInputCashPay[i].setBackgroundResource(R.drawable.round_rectangle_border_disabled);
            }
        }
        if(mLastSetAmt!=null && !mLastSetAmt.isEmpty() && !mLastSetAmt.equals("0")) {
            mInputAmt.setHint(mLastSetAmt);
        }
    }

    private void buildValueSet() {
        LogMy.d(TAG, "In buildValueSet");
        int rem = 0, tempNewAmt = 0, lastSetAmt = 0;

        if(mMinCashToPay<=0) {
            // either 'only cashload' case or 'bill mMinCashToPay <= redeem cash available' case
            mValues[0] = 50; mValues[1] = 100; mValues[2] = 500;
        } else {
            int index = 0;
            for (int n : currency) {
                // calculate rounded off mMinCashToPay
                rem = mMinCashToPay % n;
                tempNewAmt = (rem == 0) ? mMinCashToPay : (mMinCashToPay + (n - rem));
                // store 'rounded off mMinCashToPay' only if not already equal to earlier calculated mMinCashToPay

                if (tempNewAmt != lastSetAmt &&
                        tempNewAmt != mMinCashToPay &&
                        index < UI_SLOT_COUNT) {
                    mValues[index] = tempNewAmt;
                    lastSetAmt = tempNewAmt;
                    index++;
                }
            }
            LogMy.d(TAG, "Exiting buildValueSet: " + index);
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

        mInputAmt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                //if (actionId == EditorInfo.IME_ACTION_DONE) {
                mCallback.onAmountEnterFinal();
                    mInputAmt.clearFocus();
                    return true;
                //}
                //return false;
            }
        });

        mInputAmt.addTextChangedListener(textWatcher);

        mBtnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInputAmt.setText("");
            }
        });
    }
    
    private EditText[] mInputCashPay;
    private EditText mInputAmt;
    private AppCompatImageButton mBtnClear;

    private void initUiResources(View v) {
        mInputCashPay[0] = (EditText) v.findViewById(R.id.choice_cash_pay_1);
        mInputCashPay[1] = (EditText) v.findViewById(R.id.choice_cash_pay_2);
        mInputCashPay[2] = (EditText) v.findViewById(R.id.choice_cash_pay_3);
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
                int value = Integer.parseInt(s.toString());
                //if(mCallback != null && (value >= mMinCashToPay || value==0) ) {
                    mCallback.onAmountEnter(value);
                //}
            } else {
                mCallback.onAmountEnter(0);
            }
        }
    };

}
