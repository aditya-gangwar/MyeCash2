package in.myecash.merchantbase;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import in.myecash.appbase.constants.AppConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.MyGlobalSettings;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.Transaction;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.merchantbase.entities.MerchantUser;
import in.myecash.merchantbase.entities.MyTransaction;
import in.myecash.merchantbase.entities.OrderItem;
import in.myecash.merchantbase.helper.CashPaid;
import in.myecash.merchantbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 04-03-2016.
 */
public class CashTransactionFragment extends Fragment implements
        View.OnClickListener, CashPaid.CashPaidIf, View.OnTouchListener {
    private static final String TAG = "CashTransactionFragment";

    private static final int REQUEST_CASH_PAY = 1;
    private static final int REQ_CONFIRM_TRANS_COMMIT = 2;
    private static final int REQ_NEW_BILL_AMT = 3;
    private static final int REQ_NEW_ADD_CL = 4;
    private static final int REQ_NEW_REDEEM_CL = 5;
    private static final int REQ_NEW_REDEEM_CB = 6;
    private static final int REQ_NOTIFY_ERROR = 7;

    private static final String DIALOG_NUM_INPUT = "NumberInput";
    private static final String DIALOG_CASH_PAY = "CashPay";
    private static final String DIALOG_CONFIRM_TXN = "ConfirmTxn";

    private static final int STATUS_DISABLED = 0;
    // temporary disabled - state can be changed only explicitly clicking by user
    private static final int STATUS_CLEARED = 1;
    private static final int STATUS_AUTO = 2;
    // same as 'auto' for calculations - but as 'cleared' for visibility
    private static final int STATUS_AUTO_CLEARED = 3;
    private static final int STATUS_NO_BALANCE = 4;
    private static final int STATUS_QR_CARD_NOT_USED = 5;
    private static final int STATUS_BALANCE_BELOW_LIMIT = 6;
    private static final int STATUS_CASH_PAID_NOT_SET = 7;
    private static final int STATUS_MANUAL_SET = 8;

    private CashTransactionFragmentIf mCallback;
    private MyRetainedFragment mRetainedFragment;
    private MerchantUser mMerchantUser;
    private CashPaid mCashPaidHelper;

    // These members are not necessarily required to be stored as part of fragment state
    // As, either they represent values on screen, or can be calculated again.
    // But, at this advance stage, it is easier to save-restore them - instead of changing code otherwise
    private int mRedeemCashload;
    private int mRedeemCashback;
    private int mAddCashload;
    private int mAddCashback;
    private int mCashPaid;
    private int mToPayCash;
    private int mReturnCash;

    private int mClBalance;
    private int mCbBalance;
    private int mMinCashToPay;

    // Part of instance state: to be restored in event of fragment recreation
    private int mAddCbStatus;
    private int mAddClStatus;
    private int mRedeemClStatus;
    private int mRedeemCbStatus;

    // Container Activity must implement this interface
    public interface CashTransactionFragmentIf {
        MyRetainedFragment getRetainedFragment();
        void onTransactionSubmit();
        void setDrawerState(boolean isEnabled);
        void restartTxn();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_cash_txn, container, false);

        // access to UI elements
        bindUiResources(v);
        //setup all listeners
        initListeners();

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (CashTransactionFragmentIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement CashTransactionFragmentIf");
        }

        mRetainedFragment = mCallback.getRetainedFragment();
        mMerchantUser = MerchantUser.getInstance();

        if(savedInstanceState == null) {
            setAmtUiStates();
        } else {
            mClBalance = savedInstanceState.getInt("mClBalance");
            mCbBalance = savedInstanceState.getInt("mCbBalance");
            mMinCashToPay = savedInstanceState.getInt("mMinCashToPay");

            setAddCbStatus(savedInstanceState.getInt("mAddCbStatus"));
            setAddClStatus(savedInstanceState.getInt("mAddClStatus"));
            setRedeemClStatus(savedInstanceState.getInt("mRedeemClStatus"));
            setRedeemCbStatus(savedInstanceState.getInt("mRedeemCbStatus"));
        }

        // Init view - only to be done after states are set above
        setAmtUiVisibility();
        // both of below to be done twice - 1) at init here 2) if bill amount is changed
        displayInputBillAmt();
        calcAndSetAddCb();

        if(savedInstanceState == null) {
            calcAndSetAmts();
        } else {
            setRedeemCashload(savedInstanceState.getInt("mRedeemCashload"));
            setRedeemCashback(savedInstanceState.getInt("mRedeemCashback"));
            setAddCashload(savedInstanceState.getInt("mAddCashload"));
            setAddCashback(savedInstanceState.getInt("mAddCashback"));
            setCashPaid(savedInstanceState.getInt("mCashPaid"));

            mToPayCash = savedInstanceState.getInt("mToPayCash");
            mReturnCash = savedInstanceState.getInt("mReturnCash");
            setCashBalance();
        }
    }

    private void displayInputBillAmt() {
        mInputBillAmt.setText(AppCommonUtil.getSignedAmtStr(mRetainedFragment.mBillTotal, true));
    }

    private void setRedeemCashload(int value) {
        this.mRedeemCashload = value;
        mInputRedeemCl.setText(AppCommonUtil.getSignedAmtStr(mRedeemCashload, false));
    }

    private void setRedeemCashback(int value) {
        this.mRedeemCashback = value;
        mInputRedeemCb.setText(AppCommonUtil.getSignedAmtStr(mRedeemCashback, false));
    }

    private void setAddCashload(int value) {
        this.mAddCashload = value;
        mInputAddCl.setText(AppCommonUtil.getSignedAmtStr(mAddCashload, true));

        if( (mRetainedFragment.mCurrCashback.getCurrClBalance()+value) > MyGlobalSettings.getCashAccLimit()) {
            mInputAddCl.setError(AppCommonUtil.getErrorDesc(ErrorCodes.CASH_ACCOUNT_LIMIT_RCHD));
        }
    }
    private String getAddClError() {
        return mInputAddCl.getError()==null ? null : mInputCashPaid.getError().toString();
    }

    private void setAddCashback(int value) {
        this.mAddCashback = value;
        mInputAddCb.setText(AppCommonUtil.getAmtStr(mAddCashback));
    }

    private void setCashBalance() {
        if(mReturnCash > 0) {
            String str = "Return     "+ AppCommonUtil.getSignedAmtStr(mReturnCash, false);
            mInputToPayCash.setText(str);
            mInputToPayCash.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
            mDividerInputToPayCash.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
        } else {
            String str = "Collect     "+ AppCommonUtil.getSignedAmtStr(mToPayCash, true);
            mInputToPayCash.setText(str);
            mInputToPayCash.setTextColor(ContextCompat.getColor(getActivity(), R.color.green_positive));
            mDividerInputToPayCash.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.green_positive));
        }
    }

    private void setCashPaid(int value) {
        //String str = CASH_PAID_STR+AndroidUtil.getAmtStr(mCashPaid);
        mCashPaid = value;
        mInputCashPaid.setText(String.valueOf(mCashPaid));
        if(mAddClStatus == STATUS_CASH_PAID_NOT_SET) {
            setAddClStatus(STATUS_AUTO);
        }
    }

    private void setCashPaidError(String error) {
        if(mCashPaidHelper == null) {
            mInputCashPaid.setError(error);
        } else {
            mCashPaidHelper.setError(error);
        }
    }
    private String getCashPaidError() {
        if(mCashPaidHelper == null) {
            return mInputCashPaid.getError()==null ? null : mInputCashPaid.getError().toString();
        } else {
            return mCashPaidHelper.getError();
        }
    }


    /*
     * Calculate: mRedeemCashback, mRedeemCashload, mAddCashload, mAddCashback, and mToPayCash.
     * Input: mRetainedFragment.mBillTotal, mCashPaid, cbBalance, clBalance, statuses(mRedeemClStatus,mAddClStatus,mRedeemCbStatus)
     */
    private void calcAndSetAmts() {
        LogMy.d(TAG,"Entering calcAndSetAmts: Bill:"+mRetainedFragment.mBillTotal+", cashPaid:"+mCashPaid);

        // calculate add/redeem amounts fresh
        reCalculateAmts();

        // Merge 'redeem cashload' and 'add cashload' values
        // do not touch if value is manually set
        if(mAddClStatus!=STATUS_MANUAL_SET && mRedeemClStatus!=STATUS_MANUAL_SET) {
            if(mRedeemCashload > mAddCashload && mAddCashload > 0) {
                setRedeemCashload(mRedeemCashload - mAddCashload);
                setAddCashload(0);
            } else if(mAddCashload > mRedeemCashload && mRedeemCashload > 0) {
                setAddCashload(mAddCashload - mRedeemCashload);
                setRedeemCashload(0);
            }
        }
        LogMy.d(TAG,"After merge cashload: "+mAddCashload+", "+mRedeemCashload);

        // try merging 'add cashload' and 'redeem cashback'
        // do not touch if value is manually set
        if(mAddClStatus!=STATUS_MANUAL_SET && mRedeemCbStatus!=STATUS_MANUAL_SET) {
            if (mAddCashload > 0 && mRedeemCashback > 0) {
                if (mRedeemCashback > mAddCashload) {
                    setRedeemCashback(mRedeemCashback - mAddCashload);
                    setAddCashload(0);
                } else {
                    setAddCashload(mAddCashload - mRedeemCashback);
                    setRedeemCashback(0);
                }
            }
        }
        LogMy.d(TAG,"After merge cashload, cashback: "+mAddCashload+", "+mRedeemCashback);

        // calculate cash to pay
        int effectiveToPay = mRetainedFragment.mBillTotal - mRedeemCashback - mRedeemCashload;
        mToPayCash = effectiveToPay + mAddCashload;
        mReturnCash = (mCashPaid==0)?0:(mCashPaid - mToPayCash);

        // if any change to be returned - try to round it off
        if(mReturnCash > 0) {
            // round off to 10s - adjust redeem amounts for the same
            // do only when no mAddCashload involved - to keep simple
            int rem = mReturnCash %10;
            if(rem != 0 && mAddCashload<=0) {
                if(mRedeemCashback >= rem && mRedeemCbStatus != STATUS_MANUAL_SET) {
                    // redeem cashback itself is enough for round-off
                    setRedeemCashback(mRedeemCashback - rem);
                }
                else if((mRedeemCashback+mRedeemCashload) >= rem) {
                    // redeem cashback alone is not enough
                    // but combined redeem cashback+cashload is enough for round off
                    if(mRedeemCashback > 0 && mRedeemCbStatus != STATUS_MANUAL_SET) {
                        rem = rem - mRedeemCashback;
                        setRedeemCashback(0);
                    }
                    if(mRedeemCashload > 0 && mRedeemClStatus != STATUS_MANUAL_SET) {
                        setRedeemCashload(mRedeemCashload - rem);
                    }
                }
                // calculate values again
                effectiveToPay = mRetainedFragment.mBillTotal - mRedeemCashback - mRedeemCashload;
                mToPayCash = effectiveToPay + mAddCashload;
                mReturnCash = (mCashPaid==0)?0:(mCashPaid - mToPayCash);
            }
        }

        setCashBalance();

        /*
        mInputToPayCash.setText(mRsSymbolStr);
        mInputToPayCash.append(String.valueOf(mToPayCash));
        int returnCash = (mCashPaid==0)?0:(mCashPaid - mToPayCash);
        mInputToreturnCash.setText( String.format(getResources().getString(R.string.return_cash_detail), returnCash) );
        LogMy.d(TAG, "Return cash: " + returnCash);*/

        // re-calculate states based on 'new value' and old state
        if(mAddCashload==0) {
            if(mAddClStatus==STATUS_AUTO)
                setAddClStatus(STATUS_AUTO_CLEARED);
        } else if(mAddClStatus==STATUS_AUTO_CLEARED) {
            setAddClStatus(STATUS_AUTO);
        }

        if(mRedeemCashload==0) {
            if(mRedeemClStatus==STATUS_AUTO)
                setRedeemClStatus(STATUS_AUTO_CLEARED);
        } else if(mRedeemClStatus==STATUS_AUTO_CLEARED) {
            setRedeemClStatus(STATUS_AUTO);
        }

        if(mRedeemCashback==0) {
            if(mRedeemCbStatus==STATUS_AUTO)
                setRedeemCbStatus(STATUS_AUTO_CLEARED);
        } else if(mRedeemCbStatus==STATUS_AUTO_CLEARED) {
            setRedeemCbStatus(STATUS_AUTO);
        }

        // re-calculate 'minimum cash to be paid' and
        // refresh 'cash choice' values if shown on main cash txn screen
        //mMinCashToPay = mRetainedFragment.mBillTotal + mAddCashload - mRedeemCashload - mRedeemCashback;
        if(mCashPaidHelper != null) {
            // 'payment' is visible on main screen itself
            //mCashPaidHelper.refreshValues(mToPayCash);
            mCashPaidHelper.refreshValues(mMinCashToPay);
        }

        // Mark if previously set 'cash paid' value not enough, due to recalculations of amount
        if(mCashPaid < mToPayCash && mCashPaid > 0) {
            //mCashPaidOk = false;
            //mInputCashPaid.setError("'Cash Paid' to be either 0 or more than 'collect' value");
            setCashPaidError("Minimum cash required: " + AppCommonUtil.getAmtStr(mToPayCash));
        } else {
            //mCashPaidOk = true;
            //mInputCashPaid.setError(null);
            setCashPaidError(null);
        }

    }

    private void reCalculateAmts() {
        LogMy.d(TAG,"Amount status: mRedeemClStatus:"+mRedeemClStatus+", mAddClStatus:"+mAddClStatus+", mRedeemCbStatus:"+mRedeemCbStatus);
        // reset values for all
        //mRedeemCashback = mRedeemCashload = mAddCashload = mAddCashback = mToPayCash = 0;

        // will be = mBillTotal at this point
        int effectiveToPay = 0;

        // 'Cashload redeem' has priority over 'Cashback redeem'
        if(mRedeemClStatus==STATUS_AUTO || mRedeemClStatus==STATUS_AUTO_CLEARED) {
            mRedeemCashload = 0;
            effectiveToPay = mRetainedFragment.mBillTotal - mRedeemCashback - mRedeemCashload;

            if( mCashPaid > effectiveToPay ) {
                // no point of any redeem, if cashPaid is already more than the amount
                // but as user have intentionally tried to enable(status_auto), set to maximum possible
                setRedeemCashload(Math.min(mClBalance, effectiveToPay));
            } else {
                // mRedeemCashback will always be 0 - but added here just for completeness of formulae
                setRedeemCashload(Math.min(mClBalance, (effectiveToPay - mCashPaid)));
            }
        } else if (mRedeemClStatus==STATUS_CLEARED) {
            setRedeemCashload(0);
        }
        LogMy.d(TAG,"mRedeemCashload: "+mRedeemCashload+", "+effectiveToPay);

        if(mRedeemCbStatus==STATUS_AUTO || mRedeemCbStatus==STATUS_AUTO_CLEARED) {
            mRedeemCashback = 0;
            effectiveToPay = mRetainedFragment.mBillTotal - mRedeemCashback - mRedeemCashload;

            if( mCashPaid > effectiveToPay ) {
                setRedeemCashback(Math.min(mCbBalance, effectiveToPay));
            } else {
                setRedeemCashback(Math.min(mCbBalance, (effectiveToPay - mCashPaid)));
            }
        } else if (mRedeemCbStatus==STATUS_CLEARED) {
            setRedeemCashback(0);
        }
        LogMy.d(TAG,"mRedeemCashback: "+mRedeemCashback+", "+effectiveToPay);

        // calculate 'add cashload'
        if(mAddClStatus==STATUS_AUTO || mAddClStatus==STATUS_AUTO_CLEARED) {
            mAddCashload = 0;
            effectiveToPay = mRetainedFragment.mBillTotal - mRedeemCashback - mRedeemCashload;
            setAddCashload( (mCashPaid < effectiveToPay)?0:(mCashPaid - effectiveToPay) );
        } else if (mAddClStatus==STATUS_CLEARED || mAddClStatus==STATUS_CASH_PAID_NOT_SET) {
            setAddCashload(0);
        }
        LogMy.d(TAG,"mAddCashload: "+mAddCashload+", "+effectiveToPay);
    }

    private void calcAndSetAddCb() {
        // calculate add cashback
        if(STATUS_DISABLED != mAddCbStatus) {
            int cbEligibleAmt = mRetainedFragment.mBillTotal - mRetainedFragment.mCbExcludedTotal;
            float cbRate = Float.parseFloat(mMerchantUser.getMerchant().getCb_rate());
            setAddCashback((int)(cbEligibleAmt * cbRate) / 100);
            LogMy.d(TAG, "mAddCashback: " + mAddCashback);

            // display cashback details
            String str = "@ "+mMerchantUser.getMerchant().getCb_rate()+"% of "+ AppCommonUtil.getAmtStr(cbEligibleAmt);
            mSubHeadAddCb.setText(str);
            /*
            StringBuilder sb = new StringBuilder("@ ");
            sb.append(mMerchantUser.getMerchant().getCb_rate());
            sb.append("% of ").append(AndroidUtil.getAmtStr(cbEligibleAmt));
            if(mRetainedFragment.mCbExcludedTotal > 0) {
                LogMy.d(TAG, "Cashback excluded amount: "+mRetainedFragment.mCbExcludedTotal);
                sb.append(" (").append(mRetainedFragment.mBillTotal).append(" - ").append(mRetainedFragment.mCbExcludedTotal).append(")");
                //mSubHeadAddCb.setText( String.format(getResources().getString(R.string.cb_detail_with_exl),
                //        mMerchantSettings.getCb_rate(), cbEligibleAmt, mRetainedFragment.mBillTotal, mRetainedFragment.mCbExcludedTotal) );
            }
            mSubHeadAddCb.setText(sb.toString());*/
        }
    }

    private boolean billAmtEditAllowed() {
        // To avoid inconsistancy, editing allowed only if:
        // 1) single item in order
        // 2) that item has single quantity
        // basically, only when merchant have probably entered final order cost only
        if(mRetainedFragment.mOrderItems != null &&
                mRetainedFragment.mOrderItems.size() == 1 &&
                mRetainedFragment.mOrderItems.get(0).getQuantity() == 1) {
            return true;
        } else {
            Toast.makeText(getActivity(), "Use billing screen to edit", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogMy.d(TAG, "In onActivityResult :" + requestCode + ", " + resultCode);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch(requestCode) {
            case REQUEST_CASH_PAY:
                setCashPaid((int) data.getSerializableExtra(CashPaidDialog.EXTRA_CASH_PAID));
                calcAndSetAmts();
                break;

            case REQ_CONFIRM_TRANS_COMMIT:
                LogMy.d(TAG, "Received commit transaction confirmation.");
                mCallback.onTransactionSubmit();
                break;

            case REQ_NEW_BILL_AMT:
                LogMy.d(TAG, "Received new bill amount.");
                String newBillAmt = (String)data.getSerializableExtra(InputNumberDialog.EXTRA_INPUT_HUMBER);
                mRetainedFragment.mBillTotal = Integer.parseInt(newBillAmt);
                displayInputBillAmt();
                // update order item amount
                OrderItem item = mRetainedFragment.mOrderItems.get(0);
                item.setUnitPriceStr(newBillAmt);

                if (item.isCashbackExcluded()) {
                    mRetainedFragment.mCbExcludedTotal = Integer.parseInt(newBillAmt);
                }
                // re-calculate all amounts
                calcAndSetAmts();
                calcAndSetAddCb();
                break;

            case REQ_NEW_ADD_CL:
                String newValue = (String) data.getSerializableExtra(InputNumberDialog.EXTRA_INPUT_HUMBER);
                setAddCashload(Integer.parseInt(newValue));
                setAddClStatus(STATUS_MANUAL_SET);
                calcAndSetAmts();
                break;

            case REQ_NEW_REDEEM_CL:
                String newRedeemCl = (String) data.getSerializableExtra(InputNumberDialog.EXTRA_INPUT_HUMBER);
                setRedeemCashload(Integer.parseInt(newRedeemCl));
                setRedeemClStatus(STATUS_MANUAL_SET);
                calcAndSetAmts();
                break;

            case REQ_NEW_REDEEM_CB:
                String newRedeemCB = (String) data.getSerializableExtra(InputNumberDialog.EXTRA_INPUT_HUMBER);
                setRedeemCashback(Integer.parseInt(newRedeemCB));
                setRedeemCbStatus(STATUS_MANUAL_SET);
                calcAndSetAmts();
                break;

            case REQ_NOTIFY_ERROR:
                mCallback.restartTxn();
                break;

        }
    }

    @Override
    public void onAmountEnter(int value) {
        LogMy.d(TAG, "In onAmountEnter");

        if(value >= mToPayCash || value==0) {
            setCashPaid(value);
            calcAndSetAmts();
        } else {
            setCashPaidError("Minimum cash required: "+ AppCommonUtil.getAmtStr(mToPayCash));
        }
    }

    @Override
    public void onAmountEnterFinal() {
        AppCommonUtil.hideKeyboard(getActivity());
        mInputToPayCash.requestFocus();
        /*
        LogMy.d(TAG,"In onAmountEnterFinal");
        int amt = mCashPaidHelper.getCashPaidAmt();
        if(amt >= mToPayCash || amt==0) {
            onAmountEnter(amt);
        } else {
            mCashPaidHelper.setError("Minimum cash required: "+AndroidUtil.getAmtStr(mToPayCash));
        }*/
    }

    private void showCashPaidDialog() {
        // minimum cash to be paid
        // i.e. Bill amount + Add Cash - 'max redeem possible'
        FragmentManager manager = getFragmentManager();

        //CashPaidDialog dialog = CashPaidDialog.newInstance(mToPayCash, mInputCashPaid.getText().toString());
        CashPaidDialog dialog = CashPaidDialog.newInstance(mMinCashToPay, mInputCashPaid.getText().toString());
        dialog.setTargetFragment(CashTransactionFragment.this, REQUEST_CASH_PAY);
        dialog.show(manager, DIALOG_CASH_PAY);
    }

    private void setTransactionValues() {
        LogMy.d(TAG, "In setTransactionValues");
        Transaction trans = new Transaction();
        // Set only the amounts
        trans.setTotal_billed(mRetainedFragment.mBillTotal);
        trans.setCb_billed(mRetainedFragment.mBillTotal - mRetainedFragment.mCbExcludedTotal);
        trans.setCl_credit(mAddCashload);
        trans.setCl_debit(mRedeemCashload);
        trans.setCb_credit(mAddCashback);
        trans.setCb_debit(mRedeemCashback);
        trans.setCb_percent(mMerchantUser.getMerchant().getCb_rate());
        trans.setCustomer_id(mRetainedFragment.mCurrCustomer.getMobileNum());
        if(isCardPresentedAndUsable()) {
            trans.setUsedCardId(mRetainedFragment.mCustCardId);
        } else {
            trans.setUsedCardId("");
        }
        mRetainedFragment.mCurrTransaction = new MyTransaction(trans);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if(event.getAction() == MotionEvent.ACTION_UP) {
            LogMy.d(TAG,"In onTouch: "+v.getId());

            int i = v.getId();
            if (i == R.id.input_trans_bill_amt) {
                // open 'bill amount' for editing
                if (billAmtEditAllowed()) {
                    startNumInputDialog(REQ_NEW_BILL_AMT, "Bill Amount:", mInputBillAmt, 0);
                }

            }
            // manually change the values

        /*else if (i == R.id.input_trans_add_cl) {
            int effectiveToPay = mRetainedFragment.mBillTotal - mRedeemCashback - mRedeemCashload;
            int curMaxValue = (mCashPaid < effectiveToPay) ? 0 : (mCashPaid - effectiveToPay);
            if (curMaxValue > 0) {
                startNumInputDialog(REQ_NEW_ADD_CL, "Account Add:", mInputAddCl, curMaxValue);
            }

        } else if (i == R.id.input_trans_redeem_cl) {
            startNumInputDialog(REQ_NEW_REDEEM_CL, "Account Debit:", mInputRedeemCl, mClBalance);

        } else if (i == R.id.input_trans_redeem_cb) {
            startNumInputDialog(REQ_NEW_REDEEM_CB, "Cashback Redeem:", mInputRedeemCb, mCbBalance);

        } */

            else if (i == R.id.radio_add_cl || i == R.id.layout_add_cl || i==R.id.label_trans_add_cl) {

                switch (mAddClStatus) {
                    case STATUS_AUTO_CLEARED:
                    case STATUS_CLEARED:
                        setAddClStatus(STATUS_AUTO);
                        if (mRedeemClStatus == STATUS_AUTO || mRedeemClStatus == STATUS_MANUAL_SET) {
                            //mRedeemCashload = 0;
                            setRedeemClStatus(STATUS_CLEARED);
                        }
                        calcAndSetAmts();
                        break;
                    case STATUS_MANUAL_SET:
                    case STATUS_AUTO:
                        setAddClStatus(STATUS_CLEARED);
                        calcAndSetAmts();
                        break;
                    case STATUS_CASH_PAID_NOT_SET:
                        AppCommonUtil.toast(getActivity(), "Cash Paid value not set");
                        break;
                    case STATUS_DISABLED:
                        AppCommonUtil.toast(getActivity(), "Disabled in settings");
                        break;
                }

            } else if (i == R.id.radio_redeem_cl || i == R.id.layout_redeem_cl || i==R.id.label_trans_redeem_cl) {
                //case R.id.label_trans_redeem_cl:
                //case R.id.input_trans_redeem_cl:

                switch (mRedeemClStatus) {
                    case STATUS_AUTO_CLEARED:
                    case STATUS_CLEARED:
                        setRedeemClStatus(STATUS_AUTO);
                        if (mAddClStatus == STATUS_AUTO || mAddClStatus == STATUS_MANUAL_SET) {
                            //mAddCashload = 0;
                            setAddClStatus(STATUS_CLEARED);
                        }
                        calcAndSetAmts();
                        break;
                    case STATUS_MANUAL_SET:
                    case STATUS_AUTO:
                        setRedeemClStatus(STATUS_CLEARED);
                        calcAndSetAmts();
                        break;
                    case STATUS_NO_BALANCE:
                        AppCommonUtil.toast(getActivity(), AppConstants.toastNoBalance);
                        break;
                    case STATUS_QR_CARD_NOT_USED:
                        AppCommonUtil.toast(getActivity(), "Valid Member card not used");
                        break;
                }

            } else if (i == R.id.checkbox_redeem_cb || i == R.id.layout_redeem_cb || i==R.id.label_trans_redeem_cb) {

                switch (mRedeemCbStatus) {
                    case STATUS_AUTO_CLEARED:
                    case STATUS_CLEARED:
                        setRedeemCbStatus(STATUS_AUTO);
                        calcAndSetAmts();
                        break;
                    case STATUS_MANUAL_SET:
                    case STATUS_AUTO:
                        setRedeemCbStatus(STATUS_CLEARED);
                        calcAndSetAmts();
                        break;
                    case STATUS_NO_BALANCE:
                        AppCommonUtil.toast(getActivity(), AppConstants.toastNoBalance);
                        break;
                    case STATUS_QR_CARD_NOT_USED:
                        AppCommonUtil.toast(getActivity(), "Valid Member card not used");
                        break;
                    case STATUS_BALANCE_BELOW_LIMIT:
                        AppCommonUtil.toast(getActivity(), "Cashback balance below redeem limit of " +
                                AppCommonUtil.getAmtStr(MyGlobalSettings.getCbRedeemLimit()));
                        break;
                }

            } else if (i == R.id.checkbox_add_cb || i == R.id.layout_add_cb || i==R.id.label_trans_add_cb) {

                if (mAddCbStatus == STATUS_DISABLED) {
                    AppCommonUtil.toast(getActivity(), "Set Cashback rate(%) in settings");
                } else if (mAddCbStatus == STATUS_AUTO) {
                    AppCommonUtil.toast(getActivity(), "Use settings to disable");
                }

            } else if (i == R.id.label_cash_paid || i == R.id.input_cash_paid) {
                LogMy.d(TAG, "Clicked cash paid");
                //AppCommonUtil.hideKeyboard(getActivity());
                showCashPaidDialog();
            }
        }

        return true;
    }

    @Override
    public void onClick(View v) {
        LogMy.d(TAG, "In onClick: " + v.getId());

        int i = v.getId();
        if (i == R.id.btn_collect_cash) {
            LogMy.d(TAG, "Clicked Process txn button");
            if (mAddClStatus == STATUS_AUTO ||
                    mRedeemClStatus == STATUS_AUTO ||
                    mAddCbStatus == STATUS_AUTO ||
                    mRedeemCbStatus == STATUS_AUTO) {
                if (getCashPaidError() == null) {
                    if(getAddClError()==null) {
                        setTransactionValues();
                        // Show confirmation dialog
                        TxnConfirmDialog dialog = TxnConfirmDialog.newInstance(mRetainedFragment.mCurrTransaction.getTransaction(), mCashPaid);
                        dialog.setTargetFragment(CashTransactionFragment.this, REQ_CONFIRM_TRANS_COMMIT);
                        dialog.show(getFragmentManager(), DIALOG_CONFIRM_TXN);
                    } else {
                        AppCommonUtil.toast(getActivity(), getAddClError());
                    }
                } else {
                    AppCommonUtil.toast(getActivity(), getCashPaidError());
                }
            } else {
                AppCommonUtil.toast(getActivity(), "No MyeCash data to process !");
            }

        }

    }

    private void startNumInputDialog(int reqCode, String label, EditText input, int maxValue) {
        FragmentManager manager = getFragmentManager();
        String amount = input.getText().toString().replace(AppConstants.SYMBOL_RS,"");
        InputNumberDialog dialog = InputNumberDialog.newInstance(label, amount, true, maxValue);
        dialog.setTargetFragment(this, reqCode);
        dialog.show(manager, DIALOG_NUM_INPUT);
    }

    private void setAddClStatus(int status) {
        LogMy.d(TAG, "In setAddClStatus: " + status);
        mAddClStatus = status;
        switch(status) {
            case STATUS_AUTO_CLEARED:
            case STATUS_CLEARED:
            case STATUS_CASH_PAID_NOT_SET:
            case STATUS_DISABLED:
                mRadioAddCl.setChecked(false);
                if(status != STATUS_CLEARED && status != STATUS_AUTO_CLEARED) {
                    // disable only for cases, if cant be enabled back for now
                    mRadioAddCl.setEnabled(false);
                }
                // make label appear as disabled - but dont disable it - in order to catch click event
                //mLabelAddCl.setEnabled(false);
                mLabelAddCl.setTextColor(ContextCompat.getColor(getActivity(), R.color.disabled));
                mInputAddCl.setEnabled(false);
                // change the green/red color
                mInputAddCl.setTextColor(ContextCompat.getColor(getActivity(), R.color.disabled));
                break;
            case STATUS_AUTO:
                mRadioAddCl.setChecked(true);
                mRadioAddCl.setEnabled(true);
                //mLabelAddCl.setEnabled(true);
                // restore text color - in case changed while disabling it in above case
                mLabelAddCl.setTextColor(ContextCompat.getColor(getActivity(), R.color.primary_text));
                mInputAddCl.setEnabled(true);
                mInputAddCl.setTextColor(ContextCompat.getColor(getActivity(), R.color.green_positive));
                break;
            case STATUS_MANUAL_SET:
                // do nothing - it was already in auto - so all are already enabled
                break;
            default:
                // no other status is valid for 'add cb'
                LogMy.e(TAG, "Inavlid add cashload status: " + status);
                break;
        }
    }

    private void setRedeemClStatus(int status) {
        LogMy.d(TAG, "In setRedeemClStatus: "+status);
        mRedeemClStatus = status;
        switch(status) {
            case STATUS_AUTO_CLEARED:
            case STATUS_CLEARED:
            case STATUS_QR_CARD_NOT_USED:
            case STATUS_NO_BALANCE:
                mRadioRedeemCl.setChecked(false);
                if(status!=STATUS_CLEARED && status!=STATUS_AUTO_CLEARED) {
                    mRadioRedeemCl.setEnabled(false);
                }
                //mLabelRedeemCl.setEnabled(false);
                mLabelRedeemCl.setTextColor(ContextCompat.getColor(getActivity(), R.color.disabled));
                mInputRedeemCl.setEnabled(false);
                mInputRedeemCl.setTextColor(ContextCompat.getColor(getActivity(), R.color.disabled));
                break;
            case STATUS_AUTO:
                mRadioRedeemCl.setChecked(true);
                mRadioRedeemCl.setEnabled(true);
                //mLabelRedeemCl.setEnabled(true);
                mLabelRedeemCl.setTextColor(ContextCompat.getColor(getActivity(), R.color.primary_text));
                mInputRedeemCl.setEnabled(true);
                mInputRedeemCl.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
                break;
            case STATUS_MANUAL_SET:
                // do nothing - it was already in auto - so all are already enabled
                break;
            default:
                // no other status is valid for 'redeem cb'
                LogMy.e(TAG, "Inavlid redeem cashload status: " + status);
                break;
        }
    }

    private void setRedeemCbStatus(int status) {
        LogMy.d(TAG, "In setRedeemCbStatus: "+status);
        mRedeemCbStatus = status;
        switch(status) {
            case STATUS_AUTO_CLEARED:
            case STATUS_CLEARED:
            case STATUS_BALANCE_BELOW_LIMIT:
            case STATUS_QR_CARD_NOT_USED:
            case STATUS_NO_BALANCE:
                mCheckboxRedeemCb.setChecked(false);
                if(status!=STATUS_CLEARED && status!=STATUS_AUTO_CLEARED)
                    mCheckboxRedeemCb.setEnabled(false);
                // set onTouchListener - as onClickListener doesnt work in disabled editext
                //mLabelRedeemCb.setEnabled(false);
                mLabelRedeemCb.setTextColor(ContextCompat.getColor(getActivity(), R.color.disabled));
                mInputRedeemCb.setEnabled(false);
                mInputRedeemCb.setTextColor(ContextCompat.getColor(getActivity(), R.color.disabled));
                // ignore mCbBalance
                mMinCashToPay = mRetainedFragment.mBillTotal - mClBalance;
                break;
            case STATUS_AUTO:
                mCheckboxRedeemCb.setChecked(true);
                //mLabelRedeemCb.setEnabled(true);
                mLabelRedeemCb.setTextColor(ContextCompat.getColor(getActivity(), R.color.primary_text));
                mInputRedeemCb.setEnabled(true);
                mInputRedeemCb.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
                mMinCashToPay = mRetainedFragment.mBillTotal - mClBalance - mCbBalance;
                break;
            case STATUS_MANUAL_SET:
                // do nothing - it was already in auto - so all are already enabled
                break;
            default:
                // no other status is valid for 'redeem cb'
                LogMy.e(TAG, "Inavlid redeem cashback status: " + status);
                break;
        }
    }

    private void setAddCbStatus(int status) {
        LogMy.d(TAG, "In setAddCbStatus: " + status);
        mAddCbStatus = status;
        switch(status) {
            case STATUS_DISABLED:
                mCheckboxAddCb.setChecked(false);
                mCheckboxAddCb.setEnabled(false);
                //mLabelAddCb.setEnabled(false);
                mLabelAddCb.setTextColor(ContextCompat.getColor(getActivity(), R.color.disabled));
                mInputAddCb.setEnabled(false);
                mSubHeadAddCb.setEnabled(false);
                break;
            case STATUS_AUTO:
                mCheckboxAddCb.setChecked(true);
                mCheckboxAddCb.setEnabled(false);
                break;
            default:
                // no other status is valid for 'add cb'
                LogMy.e(TAG, "Inavlid add cashback status: " + status);
                break;
        }
    }

    private void initListeners() {
        // can change bill amount
        mInputBillAmt.setOnTouchListener(this);

        //mInputAddCl.setOnTouchListener(this);
        mLayoutAddCl.setOnTouchListener(this);
        mLabelAddCl.setOnTouchListener(this);

        //mInputRedeemCl.setOnTouchListener(this);
        mLayoutRedeemCl.setOnTouchListener(this);
        mLabelRedeemCl.setOnTouchListener(this);

        //mInputRedeemCb.setOnTouchListener(this);
        mLayoutRedeemCb.setOnTouchListener(this);
        mLabelRedeemCb.setOnTouchListener(this);

        //mInputAddCb.setOnTouchListener(this);
        mLayoutAddCb.setOnTouchListener(this);
        mLabelAddCb.setOnTouchListener(this);

        if(mLayoutCashPaidLink.getVisibility()==View.VISIBLE) {
            mLabelCashPaid.setOnTouchListener(this);
            mInputCashPaid.setOnTouchListener(this);
        }

        mInputToPayCash.setOnClickListener(this);
    }

    private void setAmtUiStates() {

        // Init 'add cash' status
        if(mMerchantUser.getMerchant().getCl_add_enable()) {
            if(mCashPaid > 0) {
                setAddClStatus(STATUS_AUTO);
            } else {
                setAddClStatus(STATUS_CASH_PAID_NOT_SET);
            }
        } else {
            setAddClStatus(STATUS_DISABLED);
        }

        // Init 'debit cash' status
        if(mRetainedFragment.mCurrCashback.getCurrClBalance() <= 0) {
            setRedeemClStatus(STATUS_NO_BALANCE);
        } else if(!isCardPresentedAndUsable() && MyGlobalSettings.getCardReqAccDebit()) {
            setRedeemClStatus(STATUS_QR_CARD_NOT_USED);
        } else {
            // change status by clicking the image/label
            mClBalance = mRetainedFragment.mCurrCashback.getCurrClBalance();
            setRedeemClStatus(STATUS_AUTO);
        }

        // Init 'debit cashback' status
        int cbBalance = mRetainedFragment.mCurrCashback.getCurrCbBalance();
        if(cbBalance<= 0) {
            setRedeemCbStatus(STATUS_NO_BALANCE);
        } else if( cbBalance < MyGlobalSettings.getCbRedeemLimit()) {
            setRedeemCbStatus(STATUS_BALANCE_BELOW_LIMIT);
        } else if(!isCardPresentedAndUsable() && MyGlobalSettings.getCardReqCbRedeem()) {
            setRedeemCbStatus(STATUS_QR_CARD_NOT_USED);
        } else if(mRetainedFragment.mBillTotal <= 0) {
            setRedeemCbStatus(STATUS_DISABLED);
        } else {
            // change status by clicking the image/label
            mCbBalance = cbBalance;
            setRedeemCbStatus(STATUS_CLEARED);
        }

        // Init 'add cashback' status
        float cbRate = Float.parseFloat(mMerchantUser.getMerchant().getCb_rate());
        if(cbRate > 0 && mRetainedFragment.mBillTotal > 0) {
            setAddCbStatus(STATUS_AUTO);
        } else {
            setAddCbStatus(STATUS_DISABLED);
        }
    }

    private void setAmtUiVisibility() {
        float extraSpace = 0.0f;
        boolean cashAccountViewGone = false;
        boolean cashBackViewGone = false;

        // if both 'add' and 'redeem' cash are in not enabled state - remove the view
        if( mAddClStatus==STATUS_DISABLED && mRedeemClStatus==STATUS_NO_BALANCE ) {
            mLayoutCashAccount.setVisibility(View.GONE);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mSpaceCashAccount.getLayoutParams();
            extraSpace = extraSpace + params.weight;
            mSpaceCashAccount.setVisibility(View.GONE);
            cashAccountViewGone = true;
            /*
            // disable other common elements of the 'cash account' card view
            mLabelCashAccount.setEnabled(false);
            // change image color
            int color = ContextCompat.getColor(getActivity(), R.color.disabled);
            mImageCashAccount.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            // change card background color
            mCardCashAccount.setCardBackgroundColor(R.color.disabled);*/
        }

        // if both 'add' and 'redeem' cashback are in not enabled state - remove the view
        if( mAddCbStatus!=STATUS_AUTO && mAddCbStatus!=STATUS_CLEARED &&
                mRedeemCbStatus!=STATUS_AUTO && mRedeemCbStatus!=STATUS_CLEARED ) {
            mLayoutCashBack.setVisibility(View.GONE);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mSpaceCashBack.getLayoutParams();
            extraSpace = extraSpace + params.weight;
            mSpaceCashBack.setVisibility(View.GONE);
            cashBackViewGone = true;
        }

        if(cashAccountViewGone && cashBackViewGone) {
            // both functionality not available - no point proceeding further
            DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification("No functionality enabled",
                    "Please enable 'Cashback' and/or 'Add Cash' functionality from the settings.", true, true);
            notDialog.setTargetFragment(CashTransactionFragment.this,REQ_NOTIFY_ERROR);
            notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }

        if(cashAccountViewGone || cashBackViewGone) {
            mLayoutCashPaid.setVisibility(View.VISIBLE);
            mSpaceCashPaid.setVisibility(View.VISIBLE);
            mLayoutCashPaidLink.setVisibility(View.GONE);

            // mToPayCash = 0 for now, will be updated in calcAndSetAmts()
            //mToPayCash = 0;
            //mCashPaidHelper = new CashPaid(mToPayCash, "0", this, getActivity());
            mCashPaidHelper = new CashPaid(mMinCashToPay, "0", this, getActivity());
            mCashPaidHelper.initView(getView());
        } else {
            mLayoutCashPaid.setVisibility(View.GONE);
            mSpaceCashPaid.setVisibility(View.GONE);
            mLayoutCashPaidLink.setVisibility(View.VISIBLE);
        }

        if(extraSpace > 0.0f) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mSpaceAboveButton.getLayoutParams();
            params.weight = params.weight + extraSpace;
            mSpaceAboveButton.setLayoutParams(params);
        }
    }

    private boolean isCardPresentedAndUsable() {

        return (mRetainedFragment.mCardPresented &&
                mRetainedFragment.mCurrCustomer.getCardStatus()==DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED);
    }

    // UI Resources data members
    private EditText mInputBillAmt;

    private View mLayoutCashAccount;

    private View mLayoutAddCl;
    private AppCompatRadioButton mRadioAddCl;
    private EditText mLabelAddCl;
    private EditText mInputAddCl;

    private View mLayoutRedeemCl;
    private AppCompatRadioButton mRadioRedeemCl;
    private EditText mLabelRedeemCl;
    private EditText mInputRedeemCl;

    private View mLayoutCashBack;

    private View mLayoutRedeemCb;
    private AppCompatCheckBox mCheckboxRedeemCb;
    private EditText mLabelRedeemCb;
    private EditText mInputRedeemCb;

    private View mLayoutAddCb;
    private AppCompatCheckBox mCheckboxAddCb;
    private EditText mLabelAddCb;
    private EditText mInputAddCb;
    private EditText mSubHeadAddCb;

    private View mLayoutCashPaid;

    private View mLayoutCashPaidLink;
    private EditText mLabelCashPaid;
    private EditText mInputCashPaid;

    private View mDividerInputToPayCash;
    private AppCompatButton mInputToPayCash;

    private View mSpaceCashAccount;
    private View mSpaceCashBack;
    private View mSpaceCashPaid;
    private View mSpaceAboveButton;

    private void bindUiResources(View v) {

        mInputBillAmt = (EditText) v.findViewById(R.id.input_trans_bill_amt);

        mLayoutCashAccount = v.findViewById(R.id.layout_cash_account);

        mLayoutAddCl = v.findViewById(R.id.layout_add_cl);
        //mCheckboxAddCl = (AppCompatCheckBox) v.findViewById(R.id.checkbox_add_cl);
        mRadioAddCl = (AppCompatRadioButton) v.findViewById(R.id.radio_add_cl);
        mLabelAddCl = (EditText) v.findViewById(R.id.label_trans_add_cl);
        mInputAddCl = (EditText) v.findViewById(R.id.input_trans_add_cl);

        mLayoutRedeemCl = v.findViewById(R.id.layout_redeem_cl);
        //mCheckboxRedeemCl = (AppCompatCheckBox) v.findViewById(R.id.checkbox_debit_cl);
        mRadioRedeemCl = (AppCompatRadioButton) v.findViewById(R.id.radio_redeem_cl);
        mLabelRedeemCl = (EditText) v.findViewById(R.id.label_trans_redeem_cl);
        mInputRedeemCl = (EditText) v.findViewById(R.id.input_trans_redeem_cl);

        mSpaceCashAccount = v.findViewById(R.id.space_cash_account);
        mSpaceCashBack = v.findViewById(R.id.space_cashback);
        mSpaceCashPaid = v.findViewById(R.id.space_cash_paid);
        mSpaceAboveButton = v.findViewById(R.id.space_above_button);

        mLayoutCashBack = v.findViewById(R.id.layout_cashback);
        mLayoutRedeemCb = v.findViewById(R.id.layout_redeem_cb);
        mCheckboxRedeemCb = (AppCompatCheckBox) v.findViewById(R.id.checkbox_redeem_cb);
        mLabelRedeemCb = (EditText) v.findViewById(R.id.label_trans_redeem_cb);
        mInputRedeemCb = (EditText) v.findViewById(R.id.input_trans_redeem_cb);

        mLayoutAddCb = v.findViewById(R.id.layout_add_cb);
        mCheckboxAddCb = (AppCompatCheckBox) v.findViewById(R.id.checkbox_add_cb);
        mLabelAddCb = (EditText) v.findViewById(R.id.label_trans_add_cb);
        mInputAddCb = (EditText) v.findViewById(R.id.input_trans_add_cb);
        mSubHeadAddCb = (EditText) v.findViewById(R.id.label_trans_add_cb_sub);

        mLayoutCashPaid = v.findViewById(R.id.layout_cash_paid);

        mLayoutCashPaidLink = v.findViewById(R.id.layout_cash_paid_link);
        mLabelCashPaid = (EditText) v.findViewById(R.id.label_cash_paid);
        mInputCashPaid = (EditText) v.findViewById(R.id.input_cash_paid);

        mDividerInputToPayCash = v.findViewById(R.id.divider_btn_collect_cash);
        mInputToPayCash = (AppCompatButton) v.findViewById(R.id.btn_collect_cash);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCallback.setDrawerState(false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("mClBalance", mClBalance);
        outState.putInt("mCbBalance", mCbBalance);
        outState.putInt("mMinCashToPay", mMinCashToPay);

        outState.putInt("mAddCbStatus", mAddCbStatus);
        outState.putInt("mAddClStatus", mAddClStatus);
        outState.putInt("mRedeemClStatus", mRedeemClStatus);
        outState.putInt("mRedeemCbStatus", mRedeemCbStatus);

        outState.putInt("mRedeemCashload", mRedeemCashload);
        outState.putInt("mRedeemCashback", mRedeemCashback);
        outState.putInt("mAddCashload", mAddCashload);
        outState.putInt("mAddCashback", mAddCashback);
        outState.putInt("mCashPaid", mCashPaid);
        outState.putInt("mToPayCash", mToPayCash);
        outState.putInt("mReturnCash", mReturnCash);
    }
}

            /*

// mMerchantSettings.getCl_roundoff is not checked, as on lower priority
                    /*
                    if(!mMerchantSettings.getCl_load_enable()) {
                        // Show error notification dialog
                        DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification(AppConstants.cashloadAddDisabledTitle, AppConstants.cashloadAddDisabledMsg, true);
                        notDialog.setTargetFragment(CashTransactionFragment.this,REQ_NOTIFY_ADD_CL_DISABLED);
                        notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                    } else {*/

// Show soft keyboard for the user to enter the value.
                /*
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText2, InputMethodManager.SHOW_IMPLICIT);*/

    /*
    private void initAmtMembers() {
        mAddCashload = Integer.parseInt(mInputAddCl.getText().toString());
        mRedeemCashload = Integer.parseInt(mInputRedeemCl.getText().toString());
        mAddCashback = Integer.parseInt(mInputAddCb.getText().toString());
        mRedeemCashback = Integer.parseInt(mInputRedeemCb.getText().toString());
        mToPayCash = Integer.parseInt(mBtnToPayCash.getText().toString());
    }

    private void setAmtUiResValues() {
        mInputBillAmt.setText(String.valueOf(mRetainedFragment.mBillTotal));
        mInputAddCl.setText(String.valueOf(mAddCashload));
        mInputRedeemCl.setText(String.valueOf(mRedeemCashload));
        mInputRedeemCb.setText(String.valueOf(mRedeemCashback));
        mBtnToPayCash.setText(String.valueOf(mToPayCash));
        mInputAddCb.setText(String.valueOf(mAddCashback));
        mSubHeadAddCb.setText("@ " + mMerchantSettings.getCb_rate() + "% of " + mRsSymbolStr + mRetainedFragment.mBillTotal);
    } */

    /*
    private void updateAdjustBtns() {
        boolean showNegativeBtnOnly= false;
        boolean showPositiveBtnOnly = false;

        // enable/disable adjust +/- buttons
        if(returnCash > 0) {
            // some cash to be returned
            // means: (mAddCashload ==0) && (mCashPaid > mToPayCash) && (any redeem > 0)
            int rem = returnCash%CASH_PAY_ROUNDOFF_VALUE;
            int roundoffValue = ((rem==0)?CASH_PAY_ROUNDOFF_VALUE:rem);

            LogMy.d(TAG,"roundoffValue: "+roundoffValue);

            // check there's enough total redeem value to roundoff
            if( (mRedeemCashback + mRedeemCashload) >= roundoffValue ) {
                showNegativeBtnOnly = true;
            }
        } else if(returnCash == 0) {
            // means: (cash paid = cash collect) || (bill = redeem total) || cashload > 0
            if(mAddCashload > CASH_PAY_ROUNDOFF_VALUE) {
                showNegativeBtnOnly = true;
            }
        }else {
            int rem = Math.abs(returnCash) % CASH_PAY_ROUNDOFF_VALUE;
            int roundoffValue = (rem==0) ? CASH_PAY_ROUNDOFF_VALUE : (CASH_PAY_ROUNDOFF_VALUE - rem);
            if((mRedeemCashback + mRedeemCashload) >= roundoffValue) {
                showPositiveBtnOnly = true;
            }
        }

        if(showNegativeBtnOnly) {
            enableImageButton(mBtnMinusCollectCash);
            disableImageButton(mBtnAddCollectCash);

        } else if(showPositiveBtnOnly) {
            enableImageButton(mBtnAddCollectCash);
            disableImageButton(mBtnMinusCollectCash);

        } else {
            disableImageButton(mBtnMinusCollectCash);
            disableImageButton(mBtnAddCollectCash);
        }
    }

    private void disableImageButton(AppCompatImageButton btn) {
        if(btn.getVisibility()==View.VISIBLE) {
            btn.setOnClickListener(null);
            btn.setClickable(false);
            btn.setEnabled(false);
            btn.setVisibility(View.GONE);
            //btn.setImageDrawable(AndroidUtil.getTintedDrawable(getActivity(), R.drawable.ic_remove_circle_outline_white_24dp, R.color.disabled));
            //AndroidUtil.tintView(btn, R.color.disabled);
        }
    }

    private void enableImageButton(AppCompatImageButton btn) {
        if(btn.getVisibility()==View.GONE) {
            btn.setOnClickListener(this);
            btn.setClickable(true);
            btn.setEnabled(true);
            btn.setVisibility(View.VISIBLE);
            //Drawable drawable = ContextCompat.getDrawable(getActivity(), R.drawable.ic_remove_circle_outline_white_24dp);
            //btn.setImageDrawable(drawable);
        }
    }

    private void adjustAmts(boolean isPositive) {
        LogMy.d(TAG,"In adjustAmts: "+isPositive+","+mAddCashload+","+mRedeemCashback+","+mRedeemCashload);

        if(isPositive) {
            int rem = Math.abs(returnCash) % CASH_PAY_ROUNDOFF_VALUE;
            int roundoffValue = ((rem==0) ? CASH_PAY_ROUNDOFF_VALUE : (CASH_PAY_ROUNDOFF_VALUE - rem));

            if(returnCash > 0) {
                Log.wtf(TAG,"Cannot do positive roundoff when cash is to be returned");

            } else if( (mRedeemCashback+mRedeemCashload) >= roundoffValue) {
                // minus from 'cashback redeem' first and then try 'cashload redeem'
                if(mRedeemCashback >= roundoffValue) {
                    mRedeemCashback = mRedeemCashback - roundoffValue;
                    mInputRedeemCb.setText(AndroidUtil.getSignedAmtStr(mRedeemCashload, false));
                } else {
                    if(mRedeemCashback > 0) {
                        roundoffValue = roundoffValue - mRedeemCashback;
                        mRedeemCashback = 0;
                        mInputRedeemCb.setText(AndroidUtil.getSignedAmtStr(mRedeemCashload, false));
                    }

                    mRedeemCashload = mRedeemCashload - roundoffValue;
                    mInputRedeemCl.setText(AndroidUtil.getSignedAmtStr(mRedeemCashload, false));
                }
            } else {
                Log.wtf(TAG,"Total redeem is less than roundoff value: "+roundoffValue);
            }
        } else {
            int rem = returnCash%CASH_PAY_ROUNDOFF_VALUE;
            int roundoffValue = ((rem==0) ? CASH_PAY_ROUNDOFF_VALUE : rem);

            if(mAddCashload > 0) {
                mAddCashload = mAddCashload - roundoffValue;
                mInputAddCl.setText(AndroidUtil.getSignedAmtStr(mAddCashload, true));

            } else if( (mRedeemCashback+mRedeemCashload) >= roundoffValue) {
                if(mRedeemCashback > 0) {
                    roundoffValue = roundoffValue - mRedeemCashback;
                    mRedeemCashback = 0;
                    mInputRedeemCb.setText(AndroidUtil.getSignedAmtStr(mRedeemCashload, false));
                }

                mRedeemCashload = mRedeemCashload - roundoffValue;
                mInputRedeemCl.setText(AndroidUtil.getSignedAmtStr(mRedeemCashload, false));
            }
            else {
                Log.wtf(TAG,"Invalid -ve round off case: "+roundoffValue);
            }
        }

        LogMy.d(TAG,"Exiting adjustAmts: "+isPositive+","+mAddCashload+","+mRedeemCashback+","+mRedeemCashload);
    }
    */