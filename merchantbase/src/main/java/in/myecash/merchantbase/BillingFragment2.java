package in.myecash.merchantbase;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.ArrayList;

import in.myecash.appbase.BaseFragment;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.OnSingleClickListener;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.merchantbase.entities.OrderItem;
import in.myecash.merchantbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 26-03-2017.
 */

public class BillingFragment2 extends BaseFragment {
    private static final String TAG = "MchntApp-BillingFragment";

    private BillingFragment2If mCallback;
    private MyRetainedFragment mRetainedFragment;

    // Container Activity must implement this interface
    public interface BillingFragment2If {
        MyRetainedFragment getRetainedFragment();
        void onTotalBill();
        void setDrawerState(boolean isEnabled);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateView");
        View v = inflater.inflate(R.layout.fragment_billing2, container, false);

        // access to UI elements
        bindUiResources(v);
        // setup edittext and their listeners
        initInputItemAmt();
        // setup keyboard listeners
        initKeyboard();
        //setup buttons
        initButtons();
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LogMy.d(TAG, "In onActivityCreated");

        try {
            mCallback = (BillingFragment2If) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement BillingFragment2If");
        }

        mRetainedFragment = mCallback.getRetainedFragment();
        if( (mRetainedFragment.mCustMobile==null || mRetainedFragment.mCustMobile.isEmpty()) &&
                (mRetainedFragment.mCustCardId ==null || mRetainedFragment.mCustCardId.isEmpty()) ) {
            LogMy.d(TAG, "Customer ids not available");
            // Skip case
            disableFurtherProcess();
        }
    }

    @Override
    public void onResume() {
        LogMy.d(TAG, "In onResume");
        super.onResume();
        // important when returning from 'order list fragment' or 'cash transaction fragment' and
        // bill amount is updated by these
        setTotalAmt();
        mCallback.setDrawerState(false);
        mCallback.getRetainedFragment().setResumeOk(true);
    }

    private void setTotalAmt() {
        String str = "Bill    " + AppConstants.SYMBOL_RS + String.valueOf(mRetainedFragment.mBillTotal);
        mBtnTotal.setText(str);
    }

    public void disableFurtherProcess() {
        mBtnTotal.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.bg_filters));
    }

    @Override
    public void handleBtnClick(View v) {
        // do nothing
    }

    // Not using BaseFragment's onClick method
    @Override
    public void onClick(View v) {
        //public void handleBtnClick(View v) {
        if(!mCallback.getRetainedFragment().getResumeOk())
            return;

        int resId = v.getId();
        LogMy.d(TAG, "In onClick, resId: " + resId);

        try {
            String actualStr = mInputItemAmt.getText().toString();
            // remove rupee symbol for processing
            String effectiveStr = actualStr.replace(AppConstants.SYMBOL_RS, "");
            LogMy.d(TAG, "In onClick, actualStr: " + actualStr + ", effectiveStr: " + effectiveStr);

            if (resId == R.id.input_kb_bs) {
                mInputItemAmt.setText("");
                if (effectiveStr.length() > 1) {
                    // if not 'last character removal' case
                    mInputItemAmt.setText(actualStr.toCharArray(), 0, (actualStr.length() - 1));
                } else {
                    mInputItemAmt.setText(AppConstants.SYMBOL_RS_0);
                }

            } else if(resId == R.id.input_kb_clear) {
                mInputItemAmt.setText(AppConstants.SYMBOL_RS_0);
                mRetainedFragment.mBillTotal = 0;
                setTotalAmt();

            } else {// process keys 0 - 9
                // ignore 0 as first entered digit
                if (!(resId == R.id.input_kb_0 && effectiveStr.isEmpty())) {
                    AppCompatButton key = (AppCompatButton) v;
                    // AppConstants.SYMBOL_RS_0 is set after doing calculation in handlePlus
                    if (actualStr.isEmpty() || actualStr.equals(AppConstants.SYMBOL_RS_0)) {
                        // set rupee symbol as first character
                        mInputItemAmt.setText(AppConstants.SYMBOL_RS);
                    }
                    mInputItemAmt.append(key.getText());
                }
            }
        } catch (NumberFormatException e) {
            AppCommonUtil.toast(getActivity(), "Invalid Amount");
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in BillingFragment:onClick", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    // Input string is after removing leading rupee symbol
    private void handlePlus(String curStr) {
        if(curStr.length()>0) {
            mRetainedFragment.mBillTotal = Integer.parseInt(curStr);
            setTotalAmt();
        }
    }

    private void initKeyboard() {
        mKey1.setOnClickListener(this);
        mKey2.setOnClickListener(this);
        mKey3.setOnClickListener(this);
        mKey4.setOnClickListener(this);
        mKey5.setOnClickListener(this);
        mKey6.setOnClickListener(this);
        mKey7.setOnClickListener(this);
        mKey8.setOnClickListener(this);
        mKey9.setOnClickListener(this);
        mKey0.setOnClickListener(this);
        mKeyClear.setOnClickListener(this);
        mKeyBspace.setOnClickListener(this);
    }

    private void initButtons() {
        //mBtnTotal.setOnClickListener(this);
        mBtnTotal.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                try {
                    String actualStr = mInputItemAmt.getText().toString();
                    // remove rupee symbol for processing
                    String effectiveStr = actualStr.replace(AppConstants.SYMBOL_RS, "");

                    if (mRetainedFragment.mCurrCustomer != null &&
                            mRetainedFragment.mCurrCustomer.getStatus() != DbConstants.USER_STATUS_ACTIVE &&
                            mRetainedFragment.mCurrCustomer.getStatus() != DbConstants.USER_STATUS_LIMITED_CREDIT_ONLY) {
                        AppCommonUtil.toast(getActivity(), "Customer Not Active");
                        return;
                    }

                    handlePlus(effectiveStr);
                    mCallback.onTotalBill();

                } catch (NumberFormatException e) {
                    AppCommonUtil.toast(getActivity(), "Invalid Amount");
                } catch (Exception e) {
                    LogMy.e(TAG, "Exception in BillingFragment:onClick", e);
                    DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                }
            }
        });
    }

    private void initInputItemAmt() {
        mInputItemAmt.setOnTouchListener(this);
    }

    @Override
    public boolean handleTouchUp(View v) {
        // do nothing
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        AppCommonUtil.cancelToast();
    }

    private EditText mInputItemAmt;
    private AppCompatButton mKey1;
    private AppCompatButton mKey2;
    private AppCompatButton mKey3;
    private AppCompatButton mKey4;
    private AppCompatButton mKey5;
    private AppCompatButton mKey6;
    private AppCompatButton mKey7;
    private AppCompatButton mKey8;
    private AppCompatButton mKey9;
    private AppCompatButton mKey0;
    private AppCompatButton mKeyClear;
    private AppCompatImageButton mKeyBspace;

    private AppCompatButton mBtnTotal;

    private void bindUiResources(View v) {
        mInputItemAmt = (EditText) v.findViewById(R.id.input_item_amt);
        mKey1 = (AppCompatButton) v.findViewById(R.id.input_kb_1);
        mKey2 = (AppCompatButton) v.findViewById(R.id.input_kb_2);
        mKey3 = (AppCompatButton) v.findViewById(R.id.input_kb_3);
        mKey4 = (AppCompatButton) v.findViewById(R.id.input_kb_4);
        mKey5 = (AppCompatButton) v.findViewById(R.id.input_kb_5);
        mKey6 = (AppCompatButton) v.findViewById(R.id.input_kb_6);
        mKey7 = (AppCompatButton) v.findViewById(R.id.input_kb_7);
        mKey8 = (AppCompatButton) v.findViewById(R.id.input_kb_8);
        mKey9 = (AppCompatButton) v.findViewById(R.id.input_kb_9);
        mKey0 = (AppCompatButton) v.findViewById(R.id.input_kb_0);
        mKeyClear = (AppCompatButton) v.findViewById(R.id.input_kb_clear);
        mKeyBspace = (AppCompatImageButton) v.findViewById(R.id.input_kb_bs);

        mBtnTotal = (AppCompatButton) v.findViewById(R.id.btn_bill_total);
    }
}

