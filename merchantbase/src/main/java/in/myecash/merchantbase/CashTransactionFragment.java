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
import in.myecash.appbase.entities.MyTransaction;
import in.myecash.merchantbase.entities.OrderItem;
import in.myecash.merchantbase.helper.CashPaid;
import in.myecash.merchantbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 04-03-2016.
 */
public class CashTransactionFragment extends Fragment implements
        View.OnClickListener, CashPaid.CashPaidIf, View.OnTouchListener {
    private static final String TAG = "MchntApp-CashTransactionFragment";

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
    // Used when corresponding amount becomes 0 - not because cleared by user - but due to calculations
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
    private int mDebitCashload;
    private int mRedeemCashback;
    private int mAddCashload;
    private int mAwardCashback;
    private int mCashPaid;
    //private int mToPayCash;
    private int mReturnCash;

    private int mClBalance;
    private int mCbBalance;
    private int mMinCashToPay;

    // Part of instance state: to be restored in event of fragment recreation
    private int mAddClStatus;
    private int mDebitClStatus;
    private int mAwardCbStatus;
    private int mRedeemCbStatus;
    private boolean mRedeemCbOnPriority;

    // Container Activity must implement this interface
    public interface CashTransactionFragmentIf {
        MyRetainedFragment getRetainedFragment();
        void onTransactionSubmit(int cashPaid);
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

        /*
         * Instead of checking for 'savedInstanceState==null', checking
         * for any 'not saved member' value (here, mCashPaidHelper)
         * The reason being, that for scenarios wherein fragment was stored in backstack and
         * has come to foreground again - like after pressing 'back' from 'txn confirm fragment'
         * then, the savedInstanceState will be NULL only.
         * In backstack cases, only view is destroyed, while the fragment is saved as it is
         * Thus not even onSaveInstance() gets called.
         *
         * mCashPaidHelper will be null - for both 'fragment create' and 'fragment re-create' scenarios
         * but not for 'backstack' scenarios
         */
        try {
            boolean isBackstackCase = false;
            if (mCashPaidHelper != null) {
                isBackstackCase = true;
            }

            if (!isBackstackCase) {
                // either of fragment 'create' or 'recreate' scenarios
                if (savedInstanceState == null) {
                    // fragment create case
                    initAmtUiStates();
                } else {
                    // fragment re-create case
                    LogMy.d(TAG, "Fragment re-create case");
                    // restore status from stored values
                    mClBalance = savedInstanceState.getInt("mClBalance");
                    mCbBalance = savedInstanceState.getInt("mCbBalance");
                    mMinCashToPay = savedInstanceState.getInt("mMinCashToPay");

                    setAwardCbStatus(savedInstanceState.getInt("mAwardCbStatus"));
                    setAddClStatus(savedInstanceState.getInt("mAddClStatus"));
                    setRedeemClStatus(savedInstanceState.getInt("mDebitClStatus"));
                    setRedeemCbStatus(savedInstanceState.getInt("mRedeemCbStatus"));
                    mRedeemCbOnPriority = savedInstanceState.getBoolean("mRedeemCbOnPriority");
                }
            } else {
                // these fxs update onscreen view also, so need to be run for backstack scenario too
                setAwardCbStatus(mAwardCbStatus);
                setAddClStatus(mAddClStatus);
                setRedeemClStatus(mDebitClStatus);
                setRedeemCbStatus(mRedeemCbStatus);
            }

            // Init view - only to be done after states are set above
            initAmtUiVisibility();
            // both of below to be done twice - 1) at init here 2) if bill amount is changed
            displayInputBillAmt();
            calcAndSetAddCb();

            if (!isBackstackCase) {
                if (savedInstanceState == null) {
                    calcAndSetAmts(false);

                } else {
                    // restore earlier calculated values
                    setDebitCashload(savedInstanceState.getInt("mDebitCashload"));
                    setRedeemCashback(savedInstanceState.getInt("mRedeemCashback"));
                    setAddCashload(savedInstanceState.getInt("mAddCashload"));
                    setAddCashback(savedInstanceState.getInt("mAwardCashback"));
                    setCashPaid(savedInstanceState.getInt("mCashPaid"));

                    //mToPayCash = savedInstanceState.getInt("mToPayCash");
                    mReturnCash = savedInstanceState.getInt("mReturnCash");
                }
                //mCashPaidHelper.refreshValues(mMinCashToPay, mCashPaid);
            }
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in CustomerTransactionFragment:onActivityCreated", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            getActivity().onBackPressed();
        }

        // both of below concerns view - so run again in all cases
        //mCashPaidHelper.refreshValues(mMinCashToPay, mCashPaid);
        //setCashBalance();
    }

    private void displayInputBillAmt() {
        mInputBillAmt.setText(AppCommonUtil.getSignedAmtStr(mRetainedFragment.mBillTotal, true));
    }

    private void setDebitCashload(int value) {
        this.mDebitCashload = value;
        mInputRedeemCl.setText(AppCommonUtil.getSignedAmtStr(mDebitCashload, false));
    }

    private void setRedeemCashback(int value) {
        this.mRedeemCashback = value;
        mInputRedeemCb.setText(AppCommonUtil.getSignedAmtStr(mRedeemCashback, false));
    }

    private void setAddCashload(int value) {
        this.mAddCashload = value;
        mInputAddCl.setText(AppCommonUtil.getSignedAmtStr(mAddCashload, true));

        /*if( (mRetainedFragment.mCurrCashback.getCurrClBalance()+value) > MyGlobalSettings.getCashAccLimit()) {
            mInputAddCl.setError(AppCommonUtil.getErrorDesc(ErrorCodes.CASH_ACCOUNT_LIMIT_RCHD));
        }*/
    }
    private String getAddClError() {
        return mInputAddCl.getError()==null ? null : mInputAddCl.getError().toString();
    }

    private void setAddCashback(int value) {
        this.mAwardCashback = value;
        mInputAddCb.setText(AppCommonUtil.getAmtStr(mAwardCashback));
    }

    private void setCashBalance() {
        LogMy.d(TAG,"In setCashBalance: "+mReturnCash);
        if(mReturnCash > 0) {
            String str = "Return     "+ AppCommonUtil.getSignedAmtStr(mReturnCash, false);
            mInputToPayCash.setText(str);
            mInputToPayCash.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
            mDividerInputToPayCash.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.red_negative));

        } else if(mReturnCash == 0) {
            String str = "Collect      "+AppConstants.SYMBOL_RS+" 0";
            mInputToPayCash.setText(str);
            mInputToPayCash.setTextColor(ContextCompat.getColor(getActivity(), R.color.green_positive));
            mDividerInputToPayCash.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.green_positive));

        } else {
            String str = "Collect      "+ AppCommonUtil.getSignedAmtStr(Math.abs(mReturnCash), true);
            mInputToPayCash.setText(str);
            mInputToPayCash.setTextColor(ContextCompat.getColor(getActivity(), R.color.green_positive));
            mDividerInputToPayCash.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.green_positive));
        }
    }

    private void setCashPaid(int value) {
        //String str = CASH_PAID_STR+AndroidUtil.getAmtStr(mCashPaid);
        mCashPaid = value;
        //mInputCashPaid.setText(String.valueOf(mCashPaid));
        /*if(mAddClStatus == STATUS_CASH_PAID_NOT_SET) {
            setAddClStatus(STATUS_AUTO);
        } else if(value==0) {
            setAddClStatus(STATUS_CASH_PAID_NOT_SET);
        }*/
    }

    /*private void setCashPaidError(String error) {
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
    }*/


    /*
     * Calculate: mRedeemCashback, mDebitCashload, mAddCashload, mAwardCashback, and mToPayCash.
     * Input: mRetainedFragment.mBillTotal, mCashPaid, cbBalance, clBalance, statuses(mDebitClStatus,mAddClStatus,mRedeemCbStatus)
     *      forCashPaidChange = true, if called after change in 'cash paid' value - this to avoid recursive call
     */
    private void calcAndSetAmts(boolean forCashPaidChange) {
        LogMy.d(TAG,"Entering calcAndSetAmts: Bill:"+mRetainedFragment.mBillTotal+", cashPaid:"+mCashPaid);
        LogMy.d(TAG,"Amount status: mDebitClStatus:"+ mDebitClStatus +", mAddClStatus:"+mAddClStatus+", mRedeemCbStatus:"+mRedeemCbStatus);

        // calculate add/redeem amounts fresh
        // We may have some values set, from earlier calculation
        // Reset those, and calculate all values again, based on new status
        mRedeemCashback = mDebitCashload = mAddCashload = 0;
        // Calculate both debit and add values
        if(mRedeemCbOnPriority) {
            calcRedeemCb();
            calcDebitCl();
        } else {
            calcDebitCl();
            calcRedeemCb();
        }
        calcAddCl();

        // For merging - first try to adjust 'mRedeemCashback' and then 'mDebitCashload'

        // try merging 'add cashload' and 'redeem cashback'
        // do not touch if value is manually set
        //if(mAddClStatus!=STATUS_MANUAL_SET && mRedeemCbStatus!=STATUS_MANUAL_SET) {
        if (mAddCashload > 0 && mRedeemCashback > 0) {
            if (mRedeemCashback > mAddCashload) {
                setRedeemCashback(mRedeemCashback - mAddCashload);
                setAddCashload(0);
            } else {
                setAddCashload(mAddCashload - mRedeemCashback);
                setRedeemCashback(0);
            }
        }
        //}
        LogMy.d(TAG,"After merge cashload & cashback: "+mAddCashload+", "+mRedeemCashback);

        // Merge 'redeem cashload' and 'add cashload' values
        // do not touch if value is manually set
        //if(mAddClStatus!=STATUS_MANUAL_SET && mDebitClStatus !=STATUS_MANUAL_SET) {
        if(mDebitCashload > mAddCashload && mAddCashload > 0) {
            setDebitCashload(mDebitCashload - mAddCashload);
            setAddCashload(0);
        } else if(mAddCashload > mDebitCashload && mDebitCashload > 0) {
            setAddCashload(mAddCashload - mDebitCashload);
            setDebitCashload(0);
        }
        //}
        LogMy.d(TAG,"After merge cashload: "+mAddCashload+", "+ mDebitCashload);

        // calculate cash to pay
        int effectiveToPay = mRetainedFragment.mBillTotal - mRedeemCashback - mDebitCashload;
        //mToPayCash = effectiveToPay + mAddCashload;
        //mReturnCash = (mCashPaid==0)?0:(mCashPaid - mToPayCash);
        mReturnCash = mCashPaid - (effectiveToPay + mAddCashload);

        // if any change to be returned - try to round it off
        if(mReturnCash > 0) {
            // round off to 10s - adjust redeem amounts for the same
            // do only when no mAddCashload involved - to keep simple
            //int rem = mReturnCash %10;

            // try to round off, so as mReturnCash becomes 0
            int rem = mReturnCash;
            if(mAddCashload<=0) {
                //if(mRedeemCashback >= rem && mRedeemCbStatus != STATUS_MANUAL_SET) {
                if(mRedeemCashback >= rem) {
                    // redeem cashback itself is enough for round-off
                    setRedeemCashback(mRedeemCashback - rem);
                }
                else if((mRedeemCashback+ mDebitCashload) >= rem) {
                    // redeem cashback alone is not enough
                    // but combined redeem cashback+cashload is enough for round off
                    //if(mRedeemCashback > 0 && mRedeemCbStatus != STATUS_MANUAL_SET) {
                    if(mRedeemCashback > 0) {
                        rem = rem - mRedeemCashback;
                        setRedeemCashback(0);
                    }
                    //if(mDebitCashload > 0 && mDebitClStatus != STATUS_MANUAL_SET) {
                    if(mDebitCashload > 0) {
                        setDebitCashload(mDebitCashload - rem);
                    }
                }
                // calculate values again
                effectiveToPay = mRetainedFragment.mBillTotal - mRedeemCashback - mDebitCashload;
                //mToPayCash = effectiveToPay + mAddCashload;
                //mReturnCash = (mCashPaid==0)?0:(mCashPaid - mToPayCash);
                mReturnCash = mCashPaid - (effectiveToPay + mAddCashload);
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

        if(mDebitCashload ==0) {
            if(mDebitClStatus ==STATUS_AUTO)
                setRedeemClStatus(STATUS_AUTO_CLEARED);
        } else if(mDebitClStatus ==STATUS_AUTO_CLEARED) {
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
        //mMinCashToPay = mRetainedFragment.mBillTotal + mAddCashload - mDebitCashload - mRedeemCashback;
        if(mCashPaidHelper != null && !forCashPaidChange) {
            // 'payment' is visible on main screen itself
            //mCashPaidHelper.refreshValues(mToPayCash);
            calcMinCashToPay();
            mCashPaidHelper.refreshValues(mMinCashToPay, mCashPaid);
        }

        // Mark if previously set 'cash paid' value not enough, due to recalculations of amount
        /*if(mCashPaid < mToPayCash && mCashPaid > 0) {
            //mCashPaidOk = false;
            //mInputCashPaid.setError("'Cash Paid' to be either 0 or more than 'collect' value");
            setCashPaidError("Minimum cash required: " + AppCommonUtil.getAmtStr(mToPayCash));
        } else {
            //mCashPaidOk = true;
            //mInputCashPaid.setError(null);
            setCashPaidError(null);
        }*/

    }

    private void calcDebitCl() {
        int effectiveToPay = 0;
        if(mDebitClStatus ==STATUS_AUTO || mDebitClStatus ==STATUS_AUTO_CLEARED) {
            //mDebitCashload = 0;
            // mRedeemCashback will always be 0 - but added here just for completeness of formulae
            effectiveToPay = mRetainedFragment.mBillTotal - mRedeemCashback - mDebitCashload;

            if( mCashPaid > effectiveToPay ) {
                // no point of any redeem, if cashPaid is already more than the amount
                // but as user have intentionally tried to enable(status_auto), set to maximum possible
                setDebitCashload(Math.min(mClBalance, effectiveToPay));
            } else {
                setDebitCashload(Math.min(mClBalance, (effectiveToPay - mCashPaid)));
            }
        } else if (mDebitClStatus ==STATUS_CLEARED) {
            setDebitCashload(0);
        }
        LogMy.d(TAG,"mDebitCashload: "+ mDebitCashload +", "+effectiveToPay);
    }

    private void calcRedeemCb() {
        int effectiveToPay = 0;
        if(mRedeemCbStatus==STATUS_AUTO || mRedeemCbStatus==STATUS_AUTO_CLEARED) {
            //mRedeemCashback = 0;
            effectiveToPay = mRetainedFragment.mBillTotal - mRedeemCashback - mDebitCashload;

            if( mCashPaid > effectiveToPay ) {
                setRedeemCashback(Math.min(mCbBalance, effectiveToPay));
            } else {
                setRedeemCashback(Math.min(mCbBalance, (effectiveToPay - mCashPaid)));
            }
        } else if (mRedeemCbStatus==STATUS_CLEARED) {
            setRedeemCashback(0);
        }
        LogMy.d(TAG,"mRedeemCashback: "+mRedeemCashback+", "+effectiveToPay);
    }

    private void calcAddCl() {
        int effectiveToPay = 0;
        if(mAddClStatus==STATUS_AUTO || mAddClStatus==STATUS_AUTO_CLEARED) {
            //mAddCashload = 0;
            effectiveToPay = mRetainedFragment.mBillTotal - mRedeemCashback - mDebitCashload;
            int addCash = (mCashPaid < effectiveToPay)?0:(mCashPaid - effectiveToPay);
            // check for limit
            int currAccBal = mRetainedFragment.mCurrCashback.getCurrClBalance();
            if( (currAccBal + addCash) > MyGlobalSettings.getCashAccLimit()) {
                // old balance + new will cross the account cash limit
                // update addCash value accordingly
                addCash = MyGlobalSettings.getCashAccLimit() - currAccBal;
            }
            setAddCashload(addCash);

            //} else if (mAddClStatus==STATUS_CLEARED || mAddClStatus==STATUS_CASH_PAID_NOT_SET) {
        } else if (mAddClStatus==STATUS_CLEARED) {
            setAddCashload(0);
        }
        LogMy.d(TAG,"mAddCashload: "+mAddCashload+", "+effectiveToPay);
    }

    private void calcMinCashToPay() {
        // Min cash to pay = 'Bill amt' - 'all enabled debit amts'
        // If 'any one or both combined enabled debit amount' > 'bill amt', then mMinCashToPay = 0
        mMinCashToPay = mRetainedFragment.mBillTotal;
        if(mDebitClStatus==STATUS_AUTO) {
            mMinCashToPay = mMinCashToPay - Math.min(mClBalance, mMinCashToPay);
        }
        if(mRedeemCbStatus==STATUS_AUTO) {
            mMinCashToPay = mMinCashToPay - Math.min(mCbBalance, mMinCashToPay);
        }
        LogMy.d(TAG,"Exiting calcMinCashToPay: "+mMinCashToPay);
    }

    private void calcAndSetAddCb() {
        // calculate add cashback
        if(STATUS_DISABLED != mAwardCbStatus) {
            int cbEligibleAmt = mRetainedFragment.mBillTotal - mRetainedFragment.mCbExcludedTotal;
            float cbRate = Float.parseFloat(mMerchantUser.getMerchant().getCb_rate());
            setAddCashback((int)(cbEligibleAmt * cbRate) / 100);
            LogMy.d(TAG, "mAwardCashback: " + mAwardCashback);

            // display cashback details
            String str = "("+mMerchantUser.getMerchant().getCb_rate()+"% of  "+ AppCommonUtil.getAmtStr(cbEligibleAmt)+")";
            mSubHeadAddCb.setText(str);
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
            AppCommonUtil.toast(getActivity(), "Use billing screen to edit");
            return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogMy.d(TAG, "In onActivityResult :" + requestCode + ", " + resultCode);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        try {
            switch (requestCode) {
            /*case REQUEST_CASH_PAY:
                setCashPaid((int) data.getSerializableExtra(CashPaidDialog.EXTRA_CASH_PAID));
                calcAndSetAmts();
                break;

            case REQ_CONFIRM_TRANS_COMMIT:
                LogMy.d(TAG, "Received commit transaction confirmation.");
                mCallback.onTransactionSubmit();
                break;*/

                case REQ_NEW_BILL_AMT:
                    LogMy.d(TAG, "Received new bill amount.");
                    String newBillAmt = (String) data.getSerializableExtra(NumberInputDialog.EXTRA_INPUT_HUMBER);
                    mRetainedFragment.mBillTotal = Integer.parseInt(newBillAmt);
                    displayInputBillAmt();
                    // update order item amount
                    OrderItem item = mRetainedFragment.mOrderItems.get(0);
                    item.setUnitPriceStr(newBillAmt);

                    if (item.isCashbackExcluded()) {
                        mRetainedFragment.mCbExcludedTotal = Integer.parseInt(newBillAmt);
                    }
                    // re-calculate all amounts
                    calcAndSetAmts(false);
                    calcAndSetAddCb();
                    break;

            /*case REQ_NEW_ADD_CL:
                String newValue = (String) data.getSerializableExtra(NumberInputDialog.EXTRA_INPUT_HUMBER);
                setAddCashload(Integer.parseInt(newValue));
                setAddClStatus(STATUS_MANUAL_SET);
                calcAndSetAmts();
                break;

            case REQ_NEW_REDEEM_CL:
                String newRedeemCl = (String) data.getSerializableExtra(NumberInputDialog.EXTRA_INPUT_HUMBER);
                setDebitCashload(Integer.parseInt(newRedeemCl));
                setRedeemClStatus(STATUS_MANUAL_SET);
                calcAndSetAmts();
                break;

            case REQ_NEW_REDEEM_CB:
                String newRedeemCB = (String) data.getSerializableExtra(NumberInputDialog.EXTRA_INPUT_HUMBER);
                setRedeemCashback(Integer.parseInt(newRedeemCB));
                setRedeemCbStatus(STATUS_MANUAL_SET);
                calcAndSetAmts();
                break;*/

                case REQ_NOTIFY_ERROR:
                    mCallback.restartTxn();
                    break;

            }
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in Fragment: ", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    /*@Override
    public void onAmountEnter(int value) {
        LogMy.d(TAG, "In onAmountEnter");

        if(value >= mToPayCash || value==0) {
            setCashPaid(value);
            calcAndSetAmts();
        } else {
            setCashPaidError("Minimum cash required: "+ AppCommonUtil.getAmtStr(mToPayCash));
        }
    }*/

    @Override
    public void onAmountEnterFinal(int value, boolean clearCase) {
        LogMy.d(TAG,"In onAmountEnterFinal: "+value);
        //AppCommonUtil.hideKeyboard(getActivity());
        setCashPaid(value);
        if(clearCase) {
            // in case clear button was clicked, we want refreshValues() to be called
            // as minCashToPay will be re-calculated
            calcAndSetAmts(false);
        } else {
            calcAndSetAmts(true);
        }

        //mInputToPayCash.requestFocus();
        /*
        LogMy.d(TAG,"In onAmountEnterFinal");
        int amt = mCashPaidHelper.getCashPaidAmt();
        if(amt >= mToPayCash || amt==0) {
            onAmountEnter(amt);
        } else {
            mCashPaidHelper.setError("Minimum cash required: "+AndroidUtil.getAmtStr(mToPayCash));
        }*/
    }

    /*private void showCashPaidDialog() {
        // minimum cash to be paid
        // i.e. Bill amount + Add Cash - 'max redeem possible'
        FragmentManager manager = getFragmentManager();

        //CashPaidDialog dialog = CashPaidDialog.newInstance(mToPayCash, mInputCashPaid.getText().toString());
        CashPaidDialog dialog = CashPaidDialog.newInstance(mMinCashToPay, mInputCashPaid.getText().toString());
        dialog.setTargetFragment(CashTransactionFragment.this, REQUEST_CASH_PAY);
        dialog.show(manager, DIALOG_CASH_PAY);
    }*/

    private void setTransactionValues() {
        LogMy.d(TAG, "In setTransactionValues");
        Transaction trans = new Transaction();
        // Set only the amounts
        trans.setTotal_billed(mRetainedFragment.mBillTotal);
        trans.setCb_billed(mRetainedFragment.mBillTotal - mRetainedFragment.mCbExcludedTotal);
        trans.setCl_credit(mAddCashload);
        trans.setCl_debit(mDebitCashload);
        trans.setCb_credit(mAwardCashback);
        trans.setCb_debit(mRedeemCashback);
        trans.setCb_percent(mMerchantUser.getMerchant().getCb_rate());
        trans.setCust_private_id(mRetainedFragment.mCurrCustomer.getPrivateId());
        if(isCardPresentedAndUsable()) {
            trans.setUsedCardId(mRetainedFragment.mCustCardId);
        } else {
            trans.setUsedCardId("");
        }

        mRetainedFragment.mCurrTransaction = new MyTransaction(trans);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        try {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                LogMy.d(TAG, "In onTouch: " + v.getId());

                int i = v.getId();
                if (i == R.id.input_trans_bill_amt) {
                    // open 'bill amount' for editing
                    if (billAmtEditAllowed()) {
                        startNumInputDialog(REQ_NEW_BILL_AMT, "Bill Amount:", mInputBillAmt, 0);
                    }

                }
                // manually change the values

        /*else if (i == R.id.input_trans_add_cl) {
            int effectiveToPay = mRetainedFragment.mBillTotal - mRedeemCashback - mDebitCashload;
            int curMaxValue = (mCashPaid < effectiveToPay) ? 0 : (mCashPaid - effectiveToPay);
            if (curMaxValue > 0) {
                startNumInputDialog(REQ_NEW_ADD_CL, "Account Add:", mInputAddCl, curMaxValue);
            }

        } else if (i == R.id.input_trans_redeem_cl) {
            startNumInputDialog(REQ_NEW_REDEEM_CL, "Account Debit:", mInputRedeemCl, mClBalance);

        } else if (i == R.id.input_trans_redeem_cb) {
            startNumInputDialog(REQ_NEW_REDEEM_CB, "Cashback Redeem:", mInputRedeemCb, mCbBalance);

        } */

                else if (i == R.id.radio_add_cl || i == R.id.layout_add_cl || i == R.id.label_trans_add_cl) {

                    switch (mAddClStatus) {
                        case STATUS_AUTO_CLEARED:
                        case STATUS_CLEARED:
                            // clear 'debit cl' - as you cant have both together
                            // calcAndSetAmts() will merge the values
                            //if (mDebitClStatus == STATUS_AUTO || mDebitClStatus == STATUS_MANUAL_SET) {
                        /*if (mDebitClStatus == STATUS_AUTO) {
                            setRedeemClStatus(STATUS_CLEARED);
                        }*/
                            if (mCashPaid <= 0) {
                                AppCommonUtil.toast(getActivity(), "Cash Paid value not set");
                            } else {
                                setAddClStatus(STATUS_AUTO);
                                calcAndSetAmts(false);
                            }
                            break;
                        //case STATUS_MANUAL_SET:
                        case STATUS_AUTO:
                            setAddClStatus(STATUS_CLEARED);
                            calcAndSetAmts(false);
                            break;
                    /*case STATUS_CASH_PAID_NOT_SET:
                        AppCommonUtil.toast(getActivity(), "Cash Paid value not set");
                        break;*/
                        case STATUS_DISABLED:
                            AppCommonUtil.toast(getActivity(), "Disabled in settings");
                            break;
                    }

                } else if (i == R.id.radio_redeem_cl || i == R.id.layout_redeem_cl || i == R.id.label_trans_redeem_cl) {
                    //case R.id.label_trans_redeem_cl:
                    //case R.id.input_trans_redeem_cl:

                    switch (mDebitClStatus) {
                        case STATUS_AUTO_CLEARED:
                        case STATUS_CLEARED:
                            mRedeemCbOnPriority = false;
                            setRedeemClStatus(STATUS_AUTO);
                        /*if (mAddClStatus == STATUS_AUTO || mAddClStatus == STATUS_MANUAL_SET) {
                            //mAddCashload = 0;
                            setAddClStatus(STATUS_CLEARED);
                        }*/
                            calcAndSetAmts(false);
                            break;
                        //case STATUS_MANUAL_SET:
                        case STATUS_AUTO:
                            setRedeemClStatus(STATUS_CLEARED);
                            calcAndSetAmts(false);
                            break;
                        case STATUS_NO_BALANCE:
                            AppCommonUtil.toast(getActivity(), AppConstants.toastNoBalance);
                            break;
                        case STATUS_QR_CARD_NOT_USED:
                            AppCommonUtil.toast(getActivity(), "Valid Member card not used");
                            break;
                    }

                } else if (i == R.id.checkbox_redeem_cb || i == R.id.layout_redeem_cb || i == R.id.label_trans_redeem_cb) {

                    switch (mRedeemCbStatus) {
                        case STATUS_AUTO_CLEARED:
                        case STATUS_CLEARED:
                            mRedeemCbOnPriority = true;
                            setRedeemCbStatus(STATUS_AUTO);
                            calcAndSetAmts(false);
                            break;
                        //case STATUS_MANUAL_SET:
                        case STATUS_AUTO:
                            setRedeemCbStatus(STATUS_CLEARED);
                            calcAndSetAmts(false);
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

                } else if (i == R.id.checkbox_add_cb || i == R.id.layout_add_cb || i == R.id.label_trans_add_cb) {

                    if (mAwardCbStatus == STATUS_DISABLED) {
                        AppCommonUtil.toast(getActivity(), "Set Cashback rate(%) in settings");
                    } else if (mAwardCbStatus == STATUS_AUTO) {
                        AppCommonUtil.toast(getActivity(), "Use settings to disable");
                    }

                } /*else if (i == R.id.label_cash_paid || i == R.id.input_cash_paid) {
                LogMy.d(TAG, "Clicked cash paid");
                //AppCommonUtil.hideKeyboard(getActivity());
                showCashPaidDialog();
            }*/
            }
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in CashTxnFragment:onTouch", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }

        return true;
    }

    @Override
    public void onClick(View v) {
        LogMy.d(TAG, "In onClick: " + v.getId());

        int i = v.getId();
        try {
            if (i == R.id.btn_collect_cash) {
                LogMy.d(TAG, "Clicked Process txn button");
                if (mAddClStatus == STATUS_AUTO ||
                        mDebitClStatus == STATUS_AUTO ||
                        mAwardCbStatus == STATUS_AUTO ||
                        mRedeemCbStatus == STATUS_AUTO) {
                    // If all 0 - no point going ahead
                    // This may happen, if this txn involves only cashback and
                    // that cashback is less than 1 rupee - which will be rounded of to 0
                    if (mAwardCashback <= 0 && mAddCashload <= 0 && mRedeemCashback <= 0 && mDebitCashload <= 0) {
                        //AppCommonUtil.toast(getActivity(), "No MyeCash data to process !!");
                        String msg = null;
                        if (mRetainedFragment.mBillTotal <= 0) {
                            msg = "All Credit/Debit amounts are 0, for both Cashback and Account";
                        } else {
                            msg = "'Award Cashback' is less than 1 Rupee. Other credit/debit amount are 0.";
                        }
                        DialogFragmentWrapper dialog = DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, msg, true, true);
                        dialog.setTargetFragment(this, REQ_NOTIFY_ERROR);
                        dialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                    }
                    //if (getCashPaidError() == null) {
                    if (mCashPaid >= 0) {
                        if (mReturnCash != 0) {
                            AppCommonUtil.toast(getActivity(), "Balance not 0 yet");
                        } else {
                            setTransactionValues();
                            mCallback.onTransactionSubmit(mCashPaid);
                            // Show confirmation dialog
                        /*TxnConfirmDialog dialog = TxnConfirmDialog.newInstance(mRetainedFragment.mCurrTransaction.getTransaction(), mCashPaid);
                        dialog.setTargetFragment(CashTransactionFragment.this, REQ_CONFIRM_TRANS_COMMIT);
                        dialog.show(getFragmentManager(), DIALOG_CONFIRM_TXN);*/
                        }
                    /*if(getAddClError()==null) {
                        setTransactionValues();
                        // Show confirmation dialog
                        TxnConfirmDialog dialog = TxnConfirmDialog.newInstance(mRetainedFragment.mCurrTransaction.getTransaction(), mCashPaid);
                        dialog.setTargetFragment(CashTransactionFragment.this, REQ_CONFIRM_TRANS_COMMIT);
                        dialog.show(getFragmentManager(), DIALOG_CONFIRM_TXN);
                    } else {
                        AppCommonUtil.toast(getActivity(), getAddClError());
                    }*/
                    } else {
                        AppCommonUtil.toast(getActivity(), "Set Cash Paid");
                    }
                } else {
                    AppCommonUtil.toast(getActivity(), "No MyeCash data to process !!");
                }
            }
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in CashTxnFragment:onClick", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }

    }

    private void startNumInputDialog(int reqCode, String label, EditText input, int maxValue) {
        FragmentManager manager = getFragmentManager();
        String amount = input.getText().toString().replace(AppConstants.SYMBOL_RS,"");
        NumberInputDialog dialog = NumberInputDialog.newInstance(label, amount, true, maxValue);
        dialog.setTargetFragment(this, reqCode);
        dialog.show(manager, DIALOG_NUM_INPUT);
    }

    private void setAddClStatus(int status) {
        LogMy.d(TAG, "In setAddClStatus: " + status);
        mAddClStatus = status;
        switch(status) {
            case STATUS_AUTO_CLEARED:
            case STATUS_CLEARED:
            //case STATUS_CASH_PAID_NOT_SET:
            case STATUS_DISABLED:
                mRadioAddCl.setChecked(false);
                // for 'cleared' scenarios - dont disable radio button
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
            //case STATUS_MANUAL_SET:
                // do nothing - it was already in auto - so all are already enabled
                //break;
            default:
                // no other status is valid for 'add cb'
                LogMy.e(TAG, "Inavlid add cashload status: " + status);
                break;
        }
    }

    private void setRedeemClStatus(int status) {
        LogMy.d(TAG, "In setRedeemClStatus: "+status);
        mDebitClStatus = status;
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
            //case STATUS_MANUAL_SET:
                // do nothing - it was already in auto - so all are already enabled
               // break;
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
                if(status!=STATUS_CLEARED && status!=STATUS_AUTO_CLEARED) {
                    mCheckboxRedeemCb.setEnabled(false);
                }
                // set onTouchListener - as onClickListener doesnt work in disabled editext
                //mLabelRedeemCb.setEnabled(false);
                mLabelRedeemCb.setTextColor(ContextCompat.getColor(getActivity(), R.color.disabled));
                mInputRedeemCb.setEnabled(false);
                mInputRedeemCb.setTextColor(ContextCompat.getColor(getActivity(), R.color.disabled));
                // ignore mCbBalance
                //mMinCashToPay = mRetainedFragment.mBillTotal - mClBalance;
                break;
            case STATUS_AUTO:
                mCheckboxRedeemCb.setChecked(true);
                //mLabelRedeemCb.setEnabled(true);
                mLabelRedeemCb.setTextColor(ContextCompat.getColor(getActivity(), R.color.primary_text));
                mInputRedeemCb.setEnabled(true);
                mInputRedeemCb.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
                //mMinCashToPay = mRetainedFragment.mBillTotal - mClBalance - mCbBalance;
                break;
            //case STATUS_MANUAL_SET:
                // do nothing - it was already in auto - so all are already enabled
                //break;
            default:
                // no other status is valid for 'redeem cb'
                LogMy.e(TAG, "Invalid redeem cashback status: " + status);
                break;
        }
    }

    private void setAwardCbStatus(int status) {
        LogMy.d(TAG, "In setAwardCbStatus: " + status);
        mAwardCbStatus = status;
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

        mInputToPayCash.setOnClickListener(this);
    }

    private void initAmtUiStates() {
        LogMy.d(TAG, "In initAmtUiStates");

        // Init 'add cash' status
        if(mMerchantUser.getMerchant().getCl_add_enable()) {
            // by default, dont try add cash
            // change status by clicking the image/label
            setAddClStatus(STATUS_CLEARED);
            /*if(mCashPaid > 0) {
                setAddClStatus(STATUS_AUTO);
            } else {
                setAddClStatus(STATUS_CASH_PAID_NOT_SET);
            }*/
        } else {
            setAddClStatus(STATUS_DISABLED);
        }

        // Init 'debit cash' status
        if(mRetainedFragment.mCurrCashback.getCurrClBalance() <= 0) {
            setRedeemClStatus(STATUS_NO_BALANCE);
        } else if(!isCardPresentedAndUsable() && MyGlobalSettings.getCardReqAccDebit()) {
            setRedeemClStatus(STATUS_QR_CARD_NOT_USED);
        } else {
            mClBalance = mRetainedFragment.mCurrCashback.getCurrClBalance();
            // by default, debit if available
            // change status by clicking the image/label
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
            mCbBalance = cbBalance;
            // by default, dont try cb debit
            // change status by clicking the image/label
            setRedeemCbStatus(STATUS_CLEARED);
        }

        // Init 'add cashback' status
        float cbRate = Float.parseFloat(mMerchantUser.getMerchant().getCb_rate());
        if(cbRate > 0 && mRetainedFragment.mBillTotal > 0) {
            setAwardCbStatus(STATUS_AUTO);
        } else {
            setAwardCbStatus(STATUS_DISABLED);
        }
    }

    private void initAmtUiVisibility() {
        LogMy.d(TAG, "In initAmtUiVisibility");

        float extraSpace = 0.0f;
        boolean cashAccountViewGone = false;
        boolean cashBackViewGone = false;

        // if both 'add' and 'redeem' cash are in not enabled state - remove the view
        if( mAddClStatus==STATUS_DISABLED && mDebitClStatus ==STATUS_NO_BALANCE ) {
            mLayoutCashAccount.setVisibility(View.GONE);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mSpaceCashAccount.getLayoutParams();
            extraSpace = extraSpace + params.weight;
            mSpaceCashAccount.setVisibility(View.GONE);
            cashAccountViewGone = true;
        }

        // if both 'add' and 'redeem' cashback are in not enabled state - remove the view
        if( mAwardCbStatus !=STATUS_AUTO &&
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

        // TODO: review - for now showing always on main screen
        mLayoutCashPaid.setVisibility(View.VISIBLE);
        mSpaceCashPaid.setVisibility(View.VISIBLE);

        if(mCashPaidHelper==null) {
            calcMinCashToPay();
            mCashPaidHelper = new CashPaid(mMinCashToPay, mRetainedFragment.mBillTotal, this, getActivity());
        }
        // initView for 'backstack' scenarios also - as it contain pointers to onscreen views
        mCashPaidHelper.initView(getView());

        /*if(cashAccountViewGone || cashBackViewGone) {
            mLayoutCashPaid.setVisibility(View.VISIBLE);
            mSpaceCashPaid.setVisibility(View.VISIBLE);
            mLayoutCashPaidLink.setVisibility(View.GONE);

            // mToPayCash = 0 for now, will be updated in calcAndSetAmts()
            //mToPayCash = 0;
            //mCashPaidHelper = new CashPaid(mToPayCash, "0", this, getActivity());
            mCashPaidHelper = new CashPaid(mMinCashToPay, mRetainedFragment.mBillTotal, this, getActivity());
            mCashPaidHelper.initView(getView());
        } else {
            mLayoutCashPaid.setVisibility(View.GONE);
            mSpaceCashPaid.setVisibility(View.GONE);
            mLayoutCashPaidLink.setVisibility(View.VISIBLE);
        }*/

        if(extraSpace > 0.0f) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mSpaceAboveButton.getLayoutParams();
            params.weight = params.weight + extraSpace;
            mSpaceAboveButton.setLayoutParams(params);
        }
    }

    private boolean isCardPresentedAndUsable() {

        return (mRetainedFragment.mCardPresented &&
                mRetainedFragment.mCurrCustomer.getCardStatus()==DbConstants.CUSTOMER_CARD_STATUS_ACTIVE);
    }

    // UI Resources data members
    private EditText mInputBillAmt;

    private View mLayoutCashAccount;

    private View mLayoutAddCl;
    private AppCompatCheckBox mRadioAddCl;
    private EditText mLabelAddCl;
    private EditText mInputAddCl;

    private View mLayoutRedeemCl;
    private AppCompatCheckBox mRadioRedeemCl;
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

    //private View mLayoutCashPaidLink;
    //private EditText mLabelCashPaid;
    //private EditText mInputCashPaid;

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
        mRadioAddCl = (AppCompatCheckBox) v.findViewById(R.id.radio_add_cl);
        mLabelAddCl = (EditText) v.findViewById(R.id.label_trans_add_cl);
        mInputAddCl = (EditText) v.findViewById(R.id.input_trans_add_cl);

        mLayoutRedeemCl = v.findViewById(R.id.layout_redeem_cl);
        //mCheckboxRedeemCl = (AppCompatCheckBox) v.findViewById(R.id.checkbox_debit_cl);
        mRadioRedeemCl = (AppCompatCheckBox) v.findViewById(R.id.radio_redeem_cl);
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

        /*mLayoutCashPaidLink = v.findViewById(R.id.layout_cash_paid_link);
        mLabelCashPaid = (EditText) v.findViewById(R.id.label_cash_paid);
        mInputCashPaid = (EditText) v.findViewById(R.id.input_cash_paid);*/

        mDividerInputToPayCash = v.findViewById(R.id.divider_btn_collect_cash);
        mInputToPayCash = (AppCompatButton) v.findViewById(R.id.btn_collect_cash);
    }

    @Override
    public void onResume() {
        //LogMy.d(TAG, "In onResume");
        super.onResume();
        mCallback.setDrawerState(false);
        mCashPaidHelper.refreshValues(mMinCashToPay, mCashPaid);
        setCashBalance();
    }

    @Override
    public void onPause() {
        super.onPause();
        AppCommonUtil.cancelToast();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        LogMy.d(TAG, "In onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putInt("mClBalance", mClBalance);
        outState.putInt("mCbBalance", mCbBalance);
        outState.putInt("mMinCashToPay", mMinCashToPay);

        outState.putInt("mAwardCbStatus", mAwardCbStatus);
        outState.putInt("mAddClStatus", mAddClStatus);
        outState.putInt("mDebitClStatus", mDebitClStatus);
        outState.putInt("mRedeemCbStatus", mRedeemCbStatus);
        outState.putBoolean("mRedeemCbOnPriority",mRedeemCbOnPriority);

        outState.putInt("mDebitCashload", mDebitCashload);
        outState.putInt("mRedeemCashback", mRedeemCashback);
        outState.putInt("mAddCashload", mAddCashload);
        outState.putInt("mAwardCashback", mAwardCashback);
        outState.putInt("mCashPaid", mCashPaid);
        //outState.putInt("mToPayCash", mToPayCash);
        outState.putInt("mReturnCash", mReturnCash);
    }
}