package in.myecash.merchantbase;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
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

import java.util.ArrayList;

/**
 * Created by adgangwa on 03-03-2016.
 */
public class BillingFragment extends BaseFragment {
    private static final String TAG = "MchntApp-BillingFragment";

    private static final String MULTIPLY_STR = " x ";

    private BillingFragmentIf mCallback;
    private MyRetainedFragment mRetainedFragment;

    // fragment state: these too be stored across configuration changes
    private boolean mTempCbExcluded;
    private int mOldColor;
    private boolean mCalcMode;

    // Container Activity must implement this interface
    public interface BillingFragmentIf {
        MyRetainedFragment getRetainedFragment();
        void onTotalBill();
        void onViewOrderList();
        void setDrawerState(boolean isEnabled);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateView");
        View v = inflater.inflate(R.layout.fragment_billing, container, false);

        // access to UI elements
        bindUiResources(v);
        // setup edittext and their listeners
        initInputItemAmt();
        // setup keyboard listeners
        initKeyboard();
        //setup buttons
        initButtons();

        mInputMoreInfo.setVisibility(View.INVISIBLE);

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LogMy.d(TAG, "In onActivityCreated");

        try {
            mCallback = (BillingFragmentIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement BillingFragmentIf");
        }

        if(savedInstanceState != null) {
            LogMy.d(TAG,"Restoring saved state");
            mTempCbExcluded = savedInstanceState.getBoolean("mTempCbExcluded");
            mOldColor = savedInstanceState.getInt("mOldColor");
            mCalcMode = savedInstanceState.getBoolean("mCalcMode");
        }

        mRetainedFragment = mCallback.getRetainedFragment();
        if( (mRetainedFragment.mCustMobile==null || mRetainedFragment.mCustMobile.isEmpty()) &&
                (mRetainedFragment.mCustCardId ==null || mRetainedFragment.mCustCardId.isEmpty()) ) {
            LogMy.d(TAG, "Customer ids not available");
            // Skip case
            disableFurtherProcess();
        }
        if(mRetainedFragment.mOrderItems == null) {
            mRetainedFragment.mOrderItems = new ArrayList<>();
        }
    }

    @Override
    public void onResume() {
        LogMy.d(TAG, "In onResume");
        super.onResume();
        // important when returning from 'order list fragment' or 'cash transaction fragment' and
        // bill amount is updated by these
        setTotalAmt();
        setItemCnt();
        mCallback.setDrawerState(false);
        mCallback.getRetainedFragment().setResumeOk(true);
    }

    private void setItemCnt() {
        String str = "Items : "+String.valueOf(mRetainedFragment.mOrderItems.size());
        mLabelItemCnt.setText(str);
    }

    private void setTotalAmt() {
        String str = null;
        if(mCalcMode) {
            str = "Total    " + AppConstants.SYMBOL_RS + String.valueOf(mRetainedFragment.mBillTotal);
        } else {
            str = "Bill    " + AppConstants.SYMBOL_RS + String.valueOf(mRetainedFragment.mBillTotal);
        }
        mBtnTotal.setText(str);
    }

    public void disableFurtherProcess() {
        mInputMoreInfo.setVisibility(View.VISIBLE);
        mInputMoreInfo.setText("* Calculator Mode *");
        mBtnTotal.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.bg_filters));
        mCalcMode = true;
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

            /*if (resId == R.id.btn_bill_total) {
                if (mCalcMode) {
                    AppCommonUtil.toast(getActivity(), "In Calculator Mode");
                    return;
                }

                if(mRetainedFragment.mCurrCustomer!=null &&
                        mRetainedFragment.mCurrCustomer.getStatus() != DbConstants.USER_STATUS_ACTIVE &&
                        mRetainedFragment.mCurrCustomer.getStatus() != DbConstants.USER_STATUS_LIMITED_CREDIT_ONLY ) {
                    AppCommonUtil.toast(getActivity(), "Customer Not Active");
                    return;
                }

                // do processing for +, just in case user forgets to press it in end
                handlePlus(effectiveStr);
                mCallback.onTotalBill();

            } else */if (resId == R.id.input_kb_plus) {
                handlePlus(effectiveStr);

            } else if (resId == R.id.input_kb_X) {// not allowed as first character, also only single multiply is allowed
                if (!(effectiveStr.isEmpty() || effectiveStr.contains(MULTIPLY_STR) || actualStr.equals(AppConstants.SYMBOL_RS_0))) {
                    mInputItemAmt.append(MULTIPLY_STR);
                }

            } else if (resId == R.id.input_kb_bs) {
                mInputItemAmt.setText("");
                if (effectiveStr.length() > 1) {
                    // if not 'last character removal' case
                    if (actualStr.endsWith(MULTIPLY_STR)) {
                        // remove complete multiply string
                        mInputItemAmt.setText(actualStr.toCharArray(), 0, (actualStr.length() - MULTIPLY_STR.length()));
                    } else {
                        mInputItemAmt.setText(actualStr.toCharArray(), 0, (actualStr.length() - 1));
                    }
                } else {
                    mInputItemAmt.setText(AppConstants.SYMBOL_RS_0);
                }

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
            LogMy.d(TAG,"In handlePlus, curStr: "+curStr);
            int unitPrice=0, qty=1;
            if(curStr.contains(MULTIPLY_STR)) {
                // this item has multiplication included
                // format: 'unit price' x 'quantity'
                int indx = curStr.lastIndexOf(MULTIPLY_STR);
                LogMy.d(TAG,"Handle plus: Multiple quantity case, Indx: "+indx);
                if(indx != -1) {
                    // get unit price
                    unitPrice = Integer.parseInt(curStr.substring(0,indx));
                    // get quantity
                    qty = Integer.parseInt(curStr.substring( (indx + MULTIPLY_STR.length()), curStr.length() ));
                }
            } else {
                //mTempQty = 1;
                unitPrice = Integer.parseInt(curStr);
            }
            LogMy.d(TAG,"Handle plus: Unit price: "+unitPrice+", Qty: "+qty);

            if(unitPrice > 0 && qty > 0) {
                // add to cashback excluded total
                if(mTempCbExcluded) {
                    mRetainedFragment.mCbExcludedTotal = mRetainedFragment.mCbExcludedTotal + (qty*unitPrice);
                    LogMy.d(TAG,"Handle plus: Cb excluded case: "+mRetainedFragment.mCbExcludedTotal);
                }
                // update total amt
                mRetainedFragment.mBillTotal = mRetainedFragment.mBillTotal + (qty*unitPrice);
                //mBtnLabelTotal.setText(String.valueOf(mRetainedFragment.mBillTotal));
                setTotalAmt();

                // add item to the list and update btn display
                mRetainedFragment.mOrderItems.add(new OrderItem(qty, unitPrice, mTempCbExcluded));
                //mBtnLabelItemList.setText(String.valueOf(mRetainedFragment.mOrderItems.size()));
                setItemCnt();

                // reset state of last execution
                //mTempQty = 1;
                if (mTempCbExcluded) {
                    removeItemAmtExclusion();
                }
                mInputItemAmt.setText(AppConstants.SYMBOL_RS_0);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        LogMy.d(TAG, "In onSaveInstanceState");
        outState.putBoolean("mTempCbExcluded", mTempCbExcluded);
        outState.putInt("mOldColor", mOldColor);
        outState.putBoolean("mCalcMode", mCalcMode);
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
        mKeyPlus.setOnClickListener(this);
        mKeyX.setOnClickListener(this);
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

                    if (mCalcMode) {
                        AppCommonUtil.toast(getActivity(), "In Calculator Mode");
                        return;
                    }

                    if (mRetainedFragment.mCurrCustomer != null &&
                            mRetainedFragment.mCurrCustomer.getStatus() != DbConstants.USER_STATUS_ACTIVE &&
                            mRetainedFragment.mCurrCustomer.getStatus() != DbConstants.USER_STATUS_LIMITED_CREDIT_ONLY) {
                        AppCommonUtil.toast(getActivity(), "Customer Not Active");
                        return;
                    }

                    // do processing for +, just in case user forgets to press it in end
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
        mLabelItemCnt.setOnTouchListener(this);
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
    public boolean onTouch(View view, MotionEvent motionEvent) {
    //public boolean handleTouchUp(View view) {
        if(!mCallback.getRetainedFragment().getResumeOk())
            return true;

        if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
            if(view.getId()==mInputItemAmt.getId()) {
                if(!mCalcMode) {
                    if (mTempCbExcluded) {
                        removeItemAmtExclusion();
                    } else {
                        setItemAmtExclusion();
                    }
                }
            } else if(view.getId()==mLabelItemCnt.getId()) {
                if (mRetainedFragment.mOrderItems.size() > 0) {
                    //handlePlus(effectiveStr);
                    mCallback.onViewOrderList();
                } else {
                    AppCommonUtil.toast(getActivity(), "No Items added");
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private void setItemAmtExclusion() {
        mTempCbExcluded = true;
        mOldColor = mInputItemAmt.getCurrentTextColor();
        mInputItemAmt.setTextColor(ContextCompat.getColor(getActivity(), R.color.cb_exclusion));

        mInputMoreInfo.setVisibility(View.VISIBLE);
        mInputMoreInfo.setText("* No Cashback Item *");
    }
    private void removeItemAmtExclusion() {
        mTempCbExcluded = false;
        mInputItemAmt.setTextColor(mOldColor);

        mInputMoreInfo.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();
        AppCommonUtil.cancelToast();
    }

    private EditText mInputMoreInfo;
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
    private AppCompatButton mKeyX;
    private AppCompatButton mKeyPlus;
    private AppCompatImageButton mKeyBspace;

    private AppCompatButton mBtnTotal;
    private EditText mLabelItemCnt;

    private void bindUiResources(View v) {
        mInputMoreInfo = (EditText) v.findViewById(R.id.input_more_info);
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
        mKeyX = (AppCompatButton) v.findViewById(R.id.input_kb_X);
        mKeyPlus = (AppCompatButton) v.findViewById(R.id.input_kb_plus);
        mKeyBspace = (AppCompatImageButton) v.findViewById(R.id.input_kb_bs);

        mBtnTotal = (AppCompatButton) v.findViewById(R.id.btn_bill_total);
        mLabelItemCnt = (EditText) v.findViewById(R.id.label_item_cnt);
    }
}
