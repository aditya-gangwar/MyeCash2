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
import android.support.v7.widget.AppCompatImageButton;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;

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
import in.myecash.merchantbase.helper.CashPaid2;
import in.myecash.merchantbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 04-03-2016.
 */
public class CashTransactionFragment extends Fragment implements
        View.OnClickListener, CashPaid2.CashPaid2If, View.OnTouchListener {
    private static final String TAG = "MchntApp-CashTransactionFragment";

    private static final int REQUEST_CASH_PAY = 1;
    private static final int REQ_CONFIRM_TRANS_COMMIT = 2;
    private static final int REQ_NEW_BILL_AMT = 3;
    private static final int REQ_CASH_PAID_AMT = 4;
    private static final int REQ_NEW_ADD_CL = 4;
    private static final int REQ_NEW_REDEEM_CL = 5;
    private static final int REQ_NEW_REDEEM_CB = 6;
    private static final int REQ_NOTIFY_ERROR = 7;
    private static final int REQ_NOTIFY_ERROR_EXIT = 8;

    private static final String DIALOG_NUM_INPUT = "NumberInput";
    private static final String DIALOG_CASH_PAY = "CashPay";
    private static final String DIALOG_CONFIRM_TXN = "ConfirmTxn";

    private static final int STATUS_DISABLED = 0;
    // temporary disabled - state can be changed only explicitly clicking by user
    private static final int STATUS_CLEARED = 1;
    private static final int STATUS_AUTO = 2;
    // same as 'auto' for calculations - but as 'cleared' for visibility
    // Used when corresponding amount becomes 0 - not because cleared by user - but due to calculations
    //TODO: STATUS_AUTO_CLEARED can be removed - is actually not set anywhere
    private static final int STATUS_AUTO_CLEARED = 3;
    private static final int STATUS_NO_BALANCE = 4;
    private static final int STATUS_QR_CARD_NOT_USED = 5;
    private static final int STATUS_BALANCE_BELOW_LIMIT = 6;
    private static final int STATUS_CASH_PAID_NOT_SET = 7;
    private static final int STATUS_MANUAL_SET = 8;
    private static final int STATUS_NO_BILL_AMT = 9;

    private CashTransactionFragmentIf mCallback;
    private MyRetainedFragment mRetainedFragment;
    private MerchantUser mMerchantUser;
    private CashPaid2 mCashPaidHelper;

    // These members are not necessarily required to be stored as part of fragment state
    // As, either they represent values on screen, or can be calculated again.
    // But, at this advance stage, it is easier to save-restore them - instead of changing code otherwise
    private int mDebitCashload;
    private int mDebitCashback;
    private int mAddCashload;
    private int mAddCbOnBill;
    private int mAddCbOnAcc;
    private int mCashPaid;
    //private int mToPayCash;
    private int mReturnCash;

    private int mClBalance;
    private int mCbBalance;
    private int mMinCashToPay;

    private float mCbRate;
    private float mPpCbRate;

    // Part of instance state: to be restored in event of fragment recreation
    private int mAddClStatus;
    private int mDebitClStatus;
    private int mAddCbStatus;
    private int mDebitCbStatus;
    private boolean mDebitCbOnPriority;

    // Container Activity must implement this interface
    public interface CashTransactionFragmentIf {
        MyRetainedFragment getRetainedFragment();
        void onTransactionSubmit(int cashPaid);
        void setDrawerState(boolean isEnabled);
        void restartTxn();
        void onViewOrderList();
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

            mCbRate = Float.parseFloat(mMerchantUser.getMerchant().getCb_rate());
            mPpCbRate = Float.parseFloat(mMerchantUser.getMerchant().getPrepaidCbRate());

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

                    setAddCbStatus(savedInstanceState.getInt("mAddCbStatus"));
                    setAddClStatus(savedInstanceState.getInt("mAddClStatus"));
                    setDebitClStatus(savedInstanceState.getInt("mDebitClStatus"));
                    setDebitCbStatus(savedInstanceState.getInt("mDebitCbStatus"));
                    mDebitCbOnPriority = savedInstanceState.getBoolean("mDebitCbOnPriority");
                }
            } else {
                // these fxs update onscreen view also, so need to be run for backstack scenario too
                setAddCbStatus(mAddCbStatus);
                setAddClStatus(mAddClStatus);
                setDebitClStatus(mDebitClStatus);
                setDebitCbStatus(mDebitCbStatus);
            }

            // Init view - only to be done after states are set above
            initAmtUiVisibility(false);
            initCashUiVisibility(false);
            // both of below to be done twice - 1) at init here 2) if bill amount is changed
            displayInputBillAmt();
            calcAndSetAddCb();

            if (!isBackstackCase) {
                if (savedInstanceState == null) {
                    calcAndSetAmts(false);

                } else {
                    // restore earlier calculated values
                    setDebitCashload(savedInstanceState.getInt("mDebitCashload"));
                    setRedeemCashback(savedInstanceState.getInt("mDebitCashback"));
                    setAddCashload(savedInstanceState.getInt("mAddCashload"));
                    //setAddCashback(savedInstanceState.getInt("mAddCashback"));
                    setAddCashback(savedInstanceState.getInt("mAddCbOnBill"),
                            savedInstanceState.getInt("mAddCbOnAcc"));
                    setCashPaid(savedInstanceState.getInt("mCashPaid"));

                    //mToPayCash = savedInstanceState.getInt("mToPayCash");
                    mReturnCash = savedInstanceState.getInt("mReturnCash");
                }
                //mCashPaidHelper.refreshValues(mMinCashToPay, mCashPaid);
            }
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in CustomerTransactionFragment:onActivityCreated", e);
            DialogFragmentWrapper dialog = DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true);
            dialog.setTargetFragment(this, REQ_NOTIFY_ERROR_EXIT);
            dialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            //throw e;
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
        mInputDebitCl.setText(AppCommonUtil.getSignedAmtStr(mDebitCashload, false));
    }

    private void setRedeemCashback(int value) {
        this.mDebitCashback = value;
        mInputDebitCb.setText(AppCommonUtil.getSignedAmtStr(mDebitCashback, false));
        // recalculate cashback
        calcAndSetAddCb();
    }

    private void setAddCashload(int value) {
        this.mAddCashload = value;
        mInputAddCl.setText(AppCommonUtil.getSignedAmtStr(mAddCashload, true));

        // Calling, as 'Prepaid extra cashback' may need to be applied
        calcAndSetAddCb();

        /*if( (mRetainedFragment.mCurrCashback.getCurrClBalance()+value) > MyGlobalSettings.getCashAccLimit()) {
            mInputAddCl.setError(AppCommonUtil.getErrorDesc(ErrorCodes.CASH_ACCOUNT_LIMIT_RCHD));
        }*/
    }
    private String getAddClError() {
        return mInputAddCl.getError()==null ? null : mInputAddCl.getError().toString();
    }

    private void setAddCashback(int onBill, int onAcc) {
        mAddCbOnBill = onBill;
        mAddCbOnAcc = onAcc;
        int total = mAddCbOnBill + mAddCbOnAcc;
        //this.mAddCashback = value;
        mInputAddCb.setText(AppCommonUtil.getAmtStr(total));
    }

    private void setCashBalance() {
        LogMy.d(TAG,"In setCashBalance: "+mReturnCash);
        if(mReturnCash > 0) {
            String str = "Balance     "+ AppCommonUtil.getSignedAmtStr(mReturnCash, false);
            mInputToPayCash.setText(str);
            mInputToPayCash.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
            mDividerInputToPayCash.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.red_negative));

        } else if(mReturnCash == 0) {
            String str = "Balance      "+AppConstants.SYMBOL_RS+" 0";
            mInputToPayCash.setText(str);
            mInputToPayCash.setTextColor(ContextCompat.getColor(getActivity(), R.color.green_positive));
            mDividerInputToPayCash.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.green_positive));

        } else {
            String str = "Balance      "+ AppCommonUtil.getSignedAmtStr(Math.abs(mReturnCash), true);
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
     * Calculate: mDebitCashback, mDebitCashload, mAddCashload, mAddCashback, and mToPayCash.
     * Input: mRetainedFragment.mBillTotal, mCashPaid, cbBalance, clBalance, statuses(mDebitClStatus,mAddClStatus,mDebitCbStatus)
     *      forCashPaidChange = true, if called after change in 'cash paid' value - this to avoid recursive call
     */
    private void calcAndSetAmts(boolean forCashPaidChange) {
        LogMy.d(TAG,"Entering calcAndSetAmts: Bill:"+mRetainedFragment.mBillTotal+", cashPaid:"+mCashPaid);
        LogMy.d(TAG,"Amount status: mDebitClStatus:"+ mDebitClStatus +", mAddClStatus:"+mAddClStatus+", mDebitCbStatus:"+ mDebitCbStatus);

        // calculate add/redeem amounts fresh
        // We may have some values set, from earlier calculation
        // Reset those, and calculate all values again, based on new status
        mDebitCashback = mDebitCashload = mAddCashload = 0;
        // Calculate both debit and add values
        if(mDebitCbOnPriority) {
            calcRedeemCb();
            calcDebitCl();
        } else {
            calcDebitCl();
            calcRedeemCb();
        }
        calcAddCl();

        // For merging - first try to adjust 'mDebitCashback' and then 'mDebitCashload'

        // try merging 'add cashload' and 'redeem cashback'
        // do not touch if value is manually set
        //if(mAddClStatus!=STATUS_MANUAL_SET && mDebitCbStatus!=STATUS_MANUAL_SET) {
        if (mAddCashload > 0 && mDebitCashback > 0) {
            if (mDebitCashback > mAddCashload) {
                setRedeemCashback(mDebitCashback - mAddCashload);
                setAddCashload(0);
            } else {
                setAddCashload(mAddCashload - mDebitCashback);
                setRedeemCashback(0);
            }
        }
        //}
        LogMy.d(TAG,"After merge cashload & cashback: "+mAddCashload+", "+ mDebitCashback);

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
        int effectiveToPay = mRetainedFragment.mBillTotal - mDebitCashback - mDebitCashload;
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
                //if(mDebitCashback >= rem && mDebitCbStatus != STATUS_MANUAL_SET) {
                if(mDebitCashback >= rem) {
                    // redeem cashback itself is enough for round-off
                    setRedeemCashback(mDebitCashback - rem);
                }
                else if((mDebitCashback + mDebitCashload) >= rem) {
                    // redeem cashback alone is not enough
                    // but combined redeem cashback+cashload is enough for round off
                    //if(mDebitCashback > 0 && mDebitCbStatus != STATUS_MANUAL_SET) {
                    if(mDebitCashback > 0) {
                        rem = rem - mDebitCashback;
                        setRedeemCashback(0);
                    }
                    //if(mDebitCashload > 0 && mDebitClStatus != STATUS_MANUAL_SET) {
                    if(mDebitCashload > 0) {
                        setDebitCashload(mDebitCashload - rem);
                    }
                }
                // calculate values again
                effectiveToPay = mRetainedFragment.mBillTotal - mDebitCashback - mDebitCashload;
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
                //setAddClStatus(STATUS_AUTO_CLEARED);
                setAddClStatus(STATUS_CLEARED);
        } else if(mAddClStatus==STATUS_AUTO_CLEARED) {
            setAddClStatus(STATUS_AUTO);
        }

        if(mDebitCashload ==0) {
            if(mDebitClStatus ==STATUS_AUTO)
                //setDebitClStatus(STATUS_AUTO_CLEARED);
                setDebitClStatus(STATUS_CLEARED);
        } else if(mDebitClStatus ==STATUS_AUTO_CLEARED) {
            setDebitClStatus(STATUS_AUTO);
        }

        if(mDebitCashback ==0) {
            if(mDebitCbStatus ==STATUS_AUTO)
                //setDebitCbStatus(STATUS_AUTO_CLEARED);
                setDebitCbStatus(STATUS_CLEARED);
        } else if(mDebitCbStatus ==STATUS_AUTO_CLEARED) {
            setDebitCbStatus(STATUS_AUTO);
        }

        // re-calculate 'minimum cash to be paid' and
        // refresh 'cash choice' values if shown on main cash txn screen
        //mMinCashToPay = mRetainedFragment.mBillTotal + mAddCashload - mDebitCashload - mDebitCashback;
        if(mCashPaidHelper != null && !forCashPaidChange) {
            // 'payment' is visible on main screen itself
            //mCashPaidHelper.refreshValues(mToPayCash);
            calcMinCashToPay();
            mCashPaidHelper.refreshValues(mMinCashToPay, mCashPaid, mRetainedFragment.mBillTotal);
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
            // mDebitCashback will always be 0 - but added here just for completeness of formulae
            effectiveToPay = mRetainedFragment.mBillTotal - mDebitCashback - mDebitCashload;

            if( mCashPaid > effectiveToPay ) {
                // no point of any redeem, if cashPaid is already more than the amount
                // but as user have intentionally tried to enable(status_auto), set to maximum possible
                setDebitCashload(Math.min(mClBalance, effectiveToPay));
            } else {
                setDebitCashload(Math.min(mClBalance, (effectiveToPay - mCashPaid)));
            }
        } else if (mDebitClStatus ==STATUS_CLEARED || mDebitClStatus ==STATUS_NO_BILL_AMT) {
            setDebitCashload(0);
        }
        LogMy.d(TAG,"mDebitCashload: "+ mDebitCashload +", "+effectiveToPay);
    }

    private void calcRedeemCb() {
        int effectiveToPay = 0;
        if(mDebitCbStatus ==STATUS_AUTO || mDebitCbStatus ==STATUS_AUTO_CLEARED) {
            //mDebitCashback = 0;
            effectiveToPay = mRetainedFragment.mBillTotal - mDebitCashback - mDebitCashload;

            if( mCashPaid > effectiveToPay ) {
                setRedeemCashback(Math.min(mCbBalance, effectiveToPay));
            } else {
                setRedeemCashback(Math.min(mCbBalance, (effectiveToPay - mCashPaid)));
            }
        } else if (mDebitCbStatus ==STATUS_CLEARED || mDebitClStatus ==STATUS_NO_BILL_AMT) {
            setRedeemCashback(0);
        }
        LogMy.d(TAG,"mDebitCashback: "+ mDebitCashback +", "+effectiveToPay);
    }

    private void calcAddCl() {
        int effectiveToPay = 0;
        if(mAddClStatus==STATUS_AUTO || mAddClStatus==STATUS_AUTO_CLEARED) {
            //mAddCashload = 0;
            effectiveToPay = mRetainedFragment.mBillTotal - mDebitCashback - mDebitCashload;
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
        if(mDebitCbStatus ==STATUS_AUTO) {
            mMinCashToPay = mMinCashToPay - Math.min(mCbBalance, mMinCashToPay);
        }
        LogMy.d(TAG,"Exiting calcMinCashToPay: "+mMinCashToPay);
    }

    private void calcAndSetAddCb() {
        // calculate add cashback
        if(STATUS_DISABLED != mAddCbStatus) {

            boolean cbApply = (mRetainedFragment.mBillTotal > 0);
            boolean cbExtraApply = (mAddCashload >= mMerchantUser.getMerchant().getPrepaidCbMinAmt() && mPpCbRate>0);

            // calculate cashbacks
            int cbEligibleAmt = mRetainedFragment.mBillTotal - mRetainedFragment.mCbExcludedTotal - mDebitCashback;
            mAddCbOnBill = (int)(cbEligibleAmt * mCbRate) / 100;
            mAddCbOnAcc = (int)(mAddCashload * mPpCbRate) / 100;
            setAddCashback(mAddCbOnBill, mAddCbOnAcc);
            LogMy.d(TAG, "mAddCbOnBill: " + mAddCbOnBill+", mAddCbOnAcc: "+mAddCbOnAcc);

            // show cashback details
            String str = "";
            if(cbApply && cbExtraApply) {
                // both CB appl - show only rates - else string will be too long to display in single line
                str = "("+mCbRate+"% + "+mPpCbRate+"%)";
            } else if(cbApply) {
                str = "("+mCbRate+"% of  "+ AppCommonUtil.getAmtStr(cbEligibleAmt)+")";
            } else if(cbExtraApply) {
                str = "("+mPpCbRate+"% of  "+ AppCommonUtil.getAmtStr(mAddCashload)+")";
            }
            mSubHeadAddCb.setText(str);

            /*int cbAmt = 0;
            //int cbPrepaid = 0;
            String str1 = "";

            // mPpCbRate only applies for pure 'Add Cash' txns
            // i.e. when Bill Amount is 0
            if(mRetainedFragment.mBillTotal > 0) {
                int cbEligibleAmt = mRetainedFragment.mBillTotal - mRetainedFragment.mCbExcludedTotal - mDebitCashback;
                mAddCbOnBill = (int)(cbEligibleAmt * mCbRate) / 100;
                str1 = mCbRate+"% of "+ cbEligibleAmt;
                //str1 = "("+mCbRate+"% of  "+ AppCommonUtil.getAmtStr(cbEligibleAmt)+")";

            }

            if(mAddCashload >= mMerchantUser.getMerchant().getPrepaidCbMinAmt() && mPpCbRate>0) {
                mAddCbOnAcc = (int)(mAddCashload * mPpCbRate) / 100;
                if(!str1.isEmpty()) {
                    str1 = str1+" + ";
                }
                str1 = str1+mPpCbRate+"% of "+ mAddCashload;
                //str1 = "("+mPpCbRate+"% of  "+ AppCommonUtil.getAmtStr(mAddCashload)+")";
            }

            setAddCashback(mAddCbOnBill, mAddCbOnAcc);
            LogMy.d(TAG, "mAddCbOnBill: " + mAddCbOnBill+", mAddCbOnAcc: "+mAddCbOnAcc);

            // display cashback details
            //String str = "("+mMerchantUser.getMerchant().getCb_rate()+"% of  "+ AppCommonUtil.getAmtStr(cbEligibleAmt)+")";
            String str = "(" + str1 + ")";
            mSubHeadAddCb.setText(str);*/
        }
    }

    private boolean billAmtEditAllowed() {
        // To avoid inconsistancy, editing allowed only if:
        // 0) No item added
        // 1) single item in order
        // 2) that item has single quantity
        // basically, only when merchant have probably entered final order cost only
        if(mRetainedFragment.mOrderItems == null ||
                mRetainedFragment.mOrderItems.size() == 0) {
            return true;
        } else {
            if(mRetainedFragment.mOrderItems.size() == 1 &&
                    mRetainedFragment.mOrderItems.get(0).getQuantity() == 1) {
                return true;
            } else {
                return false;
            }
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
                    if(mRetainedFragment.mOrderItems==null ||
                            mRetainedFragment.mOrderItems.size()==0) {
                        if(mRetainedFragment.mOrderItems==null) {
                            mRetainedFragment.mOrderItems = new ArrayList<>();
                        }
                        mRetainedFragment.mOrderItems.add(new OrderItem(1, mRetainedFragment.mBillTotal, false));

                    } else {
                        // update order item amount
                        OrderItem item = mRetainedFragment.mOrderItems.get(0);
                        item.setUnitPriceStr(newBillAmt);

                        if (item.isCashbackExcluded()) {
                            mRetainedFragment.mCbExcludedTotal = Integer.parseInt(newBillAmt);
                        }
                    }

                    // except 'Add Cl', status of all others is impacted by 'Bill Amount'
                    // i.e. when Bill amount was 0 earlier, but not now
                    // or it was not 0 earlier, but it is now
                    initAmtUiStates();
                    initAmtUiVisibility(false);

                    // re-calculate all amounts
                    calcAndSetAmts(false);
                    calcAndSetAddCb();
                    break;

                case REQ_CASH_PAID_AMT:
                    LogMy.d(TAG, "Received new cash paid amount.");
                    String newCashAmt = (String) data.getSerializableExtra(NumberInputDialog.EXTRA_INPUT_HUMBER);
                    if(mCashPaidHelper==null) {
                        mCashPaidHelper = new CashPaid2(mMinCashToPay, mRetainedFragment.mBillTotal, this, getActivity());
                    }
                    mCashPaidHelper.onCustomAmtEnter(newCashAmt);

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
                setDebitClStatus(STATUS_MANUAL_SET);
                calcAndSetAmts();
                break;

            case REQ_NEW_REDEEM_CB:
                String newRedeemCB = (String) data.getSerializableExtra(NumberInputDialog.EXTRA_INPUT_HUMBER);
                setRedeemCashback(Integer.parseInt(newRedeemCB));
                setDebitCbStatus(STATUS_MANUAL_SET);
                calcAndSetAmts();
                break;*/

                case REQ_NOTIFY_ERROR:
                    //mCallback.restartTxn();
                    // do nothing
                    break;

                case REQ_NOTIFY_ERROR_EXIT:
                    //getActivity().onBackPressed();
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

    @Override
    public void collectCustomAmount(String curValue, int minValue) {
        startNumInputDialog(REQ_CASH_PAID_AMT, "Cash Paid:", curValue, minValue, 0);
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

        trans.setCb_credit(mAddCbOnBill);
        trans.setExtra_cb_credit(mAddCbOnAcc);
        trans.setCb_debit(mDebitCashback);

        trans.setCb_percent(String.valueOf(mCbRate));
        trans.setExtra_cb_percent(String.valueOf(mPpCbRate));

        /*if(mRetainedFragment.mBillTotal > 0) {
            trans.setCb_percent(String.valueOf(mCbRate));
        } else {
            trans.setCb_percent(String.valueOf(mPpCbRate));
        }*/

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
        if(!mCallback.getRetainedFragment().getResumeOk())
            return true;

        try {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                LogMy.d(TAG, "In onTouch: " + v.getId());

                int i = v.getId();
                if (i == R.id.input_trans_bill_amt) {
                    // open 'bill amount' for editing
                    if (billAmtEditAllowed()) {
                        String amount = mInputBillAmt.getText().toString().replace(AppConstants.SYMBOL_RS,"").replace("+","").replace(" ","");
                        startNumInputDialog(REQ_NEW_BILL_AMT, "Bill Amount:", amount, 0, 0);
                    } else {
                        mCallback.onViewOrderList();
                    }

                }
                // manually change the values

        /*else if (i == R.id.input_trans_add_cl) {
            int effectiveToPay = mRetainedFragment.mBillTotal - mDebitCashback - mDebitCashload;
            int curMaxValue = (mCashPaid < effectiveToPay) ? 0 : (mCashPaid - effectiveToPay);
            if (curMaxValue > 0) {
                startNumInputDialog(REQ_NEW_ADD_CL, "Account Add:", mInputAddCl, curMaxValue);
            }

        } else if (i == R.id.input_trans_redeem_cl) {
            startNumInputDialog(REQ_NEW_REDEEM_CL, "Account Debit:", mInputDebitCl, mClBalance);

        } else if (i == R.id.input_trans_redeem_cb) {
            startNumInputDialog(REQ_NEW_REDEEM_CB, "Cashback Redeem:", mInputDebitCb, mCbBalance);

        } */

                else if (i == R.id.radio_add_cl || i == R.id.layout_add_cl || i == R.id.label_trans_add_cl) {

                    switch (mAddClStatus) {
                        case STATUS_AUTO_CLEARED:
                        case STATUS_CLEARED:
                            if (mCashPaid <= 0) {
                                AppCommonUtil.toast(getActivity(), "Cash Paid value not set");
                            } else {
                                setAddClStatus(STATUS_AUTO);
                                calcAndSetAmts(false);
                            }
                            break;
                        case STATUS_AUTO:
                            setAddClStatus(STATUS_CLEARED);
                            calcAndSetAmts(false);
                            break;
                        case STATUS_DISABLED:
                            AppCommonUtil.toast(getActivity(), "Disabled in settings");
                            break;
                    }

                } else if (i == R.id.radio_redeem_cl || i == R.id.layout_redeem_cl || i == R.id.label_trans_redeem_cl) {

                    switch (mDebitClStatus) {
                        case STATUS_AUTO_CLEARED:
                        case STATUS_CLEARED:
                            mDebitCbOnPriority = false;
                            setDebitClStatus(STATUS_AUTO);
                            calcAndSetAmts(false);
                            break;
                        case STATUS_AUTO:
                            setDebitClStatus(STATUS_CLEARED);
                            calcAndSetAmts(false);
                            break;
                        case STATUS_NO_BALANCE:
                            AppCommonUtil.toast(getActivity(), AppConstants.toastNoBalance);
                            break;
                        case STATUS_QR_CARD_NOT_USED:
                            AppCommonUtil.toast(getActivity(), "Member card Not Scanned");
                            break;
                        case STATUS_NO_BILL_AMT:
                            AppCommonUtil.toast(getActivity(), "Billing Amount is 0");
                            break;
                    }

                } else if (i == R.id.checkbox_redeem_cb || i == R.id.layout_redeem_cb || i == R.id.label_trans_redeem_cb) {

                    switch (mDebitCbStatus) {
                        case STATUS_AUTO_CLEARED:
                        case STATUS_CLEARED:
                            mDebitCbOnPriority = true;
                            setDebitCbStatus(STATUS_AUTO);
                            calcAndSetAmts(false);
                            break;
                        case STATUS_AUTO:
                            setDebitCbStatus(STATUS_CLEARED);
                            calcAndSetAmts(false);
                            break;
                        case STATUS_NO_BALANCE:
                            AppCommonUtil.toast(getActivity(), AppConstants.toastNoBalance);
                            break;
                        case STATUS_QR_CARD_NOT_USED:
                            AppCommonUtil.toast(getActivity(), "Valid Member card not used");
                            break;
                        case STATUS_BALANCE_BELOW_LIMIT:
                            AppCommonUtil.toast(getActivity(), "Cashback balance below " +
                                    AppCommonUtil.getAmtStr(MyGlobalSettings.getCbRedeemLimit()));
                            break;
                        case STATUS_NO_BILL_AMT:
                            AppCommonUtil.toast(getActivity(), "Billing Amount is 0");
                            break;
                    }

                } else if (i == R.id.checkbox_add_cb || i == R.id.layout_add_cb || i == R.id.label_trans_add_cb) {

                    switch (mAddCbStatus) {
                        case STATUS_NO_BILL_AMT:
                            AppCommonUtil.toast(getActivity(), "Billing Amount is 0");
                            break;
                        case STATUS_DISABLED:
                            if(mRetainedFragment.mBillTotal > 0) {
                                AppCommonUtil.toast(getActivity(), "Cashback Rate(%) is 0");

                            } else {
                                if(mPpCbRate <= 0) {
                                    AppCommonUtil.toast(getActivity(), "Extra Cashback Rate(%) is 0");

                                } else if (mAddCashload <= mMerchantUser.getMerchant().getPrepaidCbMinAmt()) {
                                    AppCommonUtil.toast(getActivity(), "'Add Cash' is less than "+
                                            AppCommonUtil.getAmtStr(mMerchantUser.getMerchant().getPrepaidCbMinAmt()));
                                }
                            }
                            break;
                    }

                } else {
                    return false;
                }
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
        if(!mCallback.getRetainedFragment().getResumeOk())
            return;

        int i = v.getId();
        try {
            if (i == R.id.btn_collect_cash) {
                LogMy.d(TAG, "Clicked Process txn button");
                if (mAddClStatus == STATUS_AUTO ||
                        mDebitClStatus == STATUS_AUTO ||
                        mAddCbStatus == STATUS_AUTO ||
                        mDebitCbStatus == STATUS_AUTO) {
                    // If all 0 - no point going ahead
                    // This may happen, if this txn involves only cashback and
                    // that cashback is less than 1 rupee - which will be rounded of to 0
                    if ((mAddCbOnBill+mAddCbOnAcc) <= 0 && mAddCashload <= 0 && mDebitCashback <= 0 && mDebitCashload <= 0) {
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
                        return;
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

            } else if(i == R.id.btn_expand_acc) {
                initAccUiVisibility(true);
            } else if(i == R.id.btn_expand_cb) {
                initCbUiVisibility(true);
            } else if(i == R.id.btn_expand_cash) {
                initCashUiVisibility(true);
            }
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in CashTxnFragment:onClick", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }

    }

    private void startNumInputDialog(int reqCode, String label, String curValue, int minValue, int maxValue) {
        FragmentManager manager = getFragmentManager();
        NumberInputDialog dialog = NumberInputDialog.newInstance(label, curValue, true, minValue, maxValue);
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

    private void setDebitClStatus(int status) {
        LogMy.d(TAG, "In setDebitClStatus: "+status);
        mDebitClStatus = status;
        switch(status) {
            case STATUS_AUTO_CLEARED:
            case STATUS_CLEARED:
            case STATUS_QR_CARD_NOT_USED:
            case STATUS_NO_BALANCE:
            case STATUS_NO_BILL_AMT:
                mRadioDebitCl.setChecked(false);
                if(status!=STATUS_CLEARED && status!=STATUS_AUTO_CLEARED) {
                    mRadioDebitCl.setEnabled(false);
                }
                //mLabelDebitCl.setEnabled(false);
                mLabelDebitCl.setTextColor(ContextCompat.getColor(getActivity(), R.color.disabled));
                mInputDebitCl.setEnabled(false);
                mInputDebitCl.setTextColor(ContextCompat.getColor(getActivity(), R.color.disabled));
                break;
            case STATUS_AUTO:
                mRadioDebitCl.setChecked(true);
                mRadioDebitCl.setEnabled(true);
                //mLabelDebitCl.setEnabled(true);
                mLabelDebitCl.setTextColor(ContextCompat.getColor(getActivity(), R.color.primary_text));
                mInputDebitCl.setEnabled(true);
                mInputDebitCl.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
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

    private void setDebitCbStatus(int status) {
        LogMy.d(TAG, "In setDebitCbStatus: "+status);
        mDebitCbStatus = status;
        switch(status) {
            case STATUS_AUTO_CLEARED:
            case STATUS_CLEARED:
            case STATUS_BALANCE_BELOW_LIMIT:
            case STATUS_QR_CARD_NOT_USED:
            case STATUS_NO_BALANCE:
            case STATUS_NO_BILL_AMT:
                mCheckboxDebitCb.setChecked(false);
                if(status!=STATUS_CLEARED && status!=STATUS_AUTO_CLEARED) {
                    mCheckboxDebitCb.setEnabled(false);
                }
                // set onTouchListener - as onClickListener doesnt work in disabled editext
                //mLabelDebitCb.setEnabled(false);
                mLabelDebitCb.setTextColor(ContextCompat.getColor(getActivity(), R.color.disabled));
                mInputDebitCb.setEnabled(false);
                mInputDebitCb.setTextColor(ContextCompat.getColor(getActivity(), R.color.disabled));
                // ignore mCbBalance
                //mMinCashToPay = mRetainedFragment.mBillTotal - mClBalance;
                break;
            case STATUS_AUTO:
                mCheckboxDebitCb.setChecked(true);
                mCheckboxDebitCb.setEnabled(true);
                //mLabelDebitCb.setEnabled(true);
                mLabelDebitCb.setTextColor(ContextCompat.getColor(getActivity(), R.color.primary_text));
                mInputDebitCb.setEnabled(true);
                mInputDebitCb.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
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

    private void setAddCbStatus(int status) {
        LogMy.d(TAG, "In setAddCbStatus: " + status);
        mAddCbStatus = status;
        switch(status) {
            case STATUS_DISABLED:
            case STATUS_NO_BILL_AMT:
                mCheckboxAddCb.setChecked(false);
                mCheckboxAddCb.setEnabled(false);
                //mLabelAddCb.setEnabled(false);
                mLabelAddCb.setTextColor(ContextCompat.getColor(getActivity(), R.color.disabled));
                mInputAddCb.setEnabled(false);
                //mSubHeadAddCb.setEnabled(false);
                mSubHeadAddCb.setTextColor(ContextCompat.getColor(getActivity(), R.color.disabled));
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

        //mInputDebitCl.setOnTouchListener(this);
        mLayoutDebitCl.setOnTouchListener(this);
        mLabelDebitCl.setOnTouchListener(this);

        //mInputDebitCb.setOnTouchListener(this);
        mLayoutDebitCb.setOnTouchListener(this);
        mLabelDebitCb.setOnTouchListener(this);

        //mInputAddCb.setOnTouchListener(this);
        mLayoutAddCb.setOnTouchListener(this);
        mLabelAddCb.setOnTouchListener(this);

        mInputToPayCash.setOnClickListener(this);
        mAccExpand.setOnClickListener(this);
        mCbExpand.setOnClickListener(this);
        mCashExpand.setOnClickListener(this);
    }

    private void initAmtUiStates() {
        LogMy.d(TAG, "In initAmtUiStates");

        // Init 'add cash' status
        if(mMerchantUser.getMerchant().getCl_add_enable()) {
            // by default, dont try add cash
            if(mRetainedFragment.mBillTotal==0) {
                setAddClStatus(STATUS_AUTO);
            } else {
                setAddClStatus(STATUS_CLEARED);
            }
        } else {
            setAddClStatus(STATUS_DISABLED);
        }

        // Init 'debit cash' status
        mClBalance = mRetainedFragment.mCurrCashback.getCurrClBalance();
        if(mRetainedFragment.mCurrCashback.getCurrClBalance() <= 0) {
            setDebitClStatus(STATUS_NO_BALANCE);
        } else if(!isCardPresentedAndUsable() && MyGlobalSettings.getCardReqAccDebit()) {
            setDebitClStatus(STATUS_QR_CARD_NOT_USED);
        } else if(mRetainedFragment.mBillTotal <= 0) {
            setDebitClStatus(STATUS_NO_BILL_AMT);
        } else {
            // by default, debit if available
            setDebitClStatus(STATUS_AUTO);
        }

        // Init 'debit cashback' status
        int cbBalance = mRetainedFragment.mCurrCashback.getCurrCbBalance();
        if(cbBalance<= 0) {
            setDebitCbStatus(STATUS_NO_BALANCE);
        } else if( cbBalance < MyGlobalSettings.getCbRedeemLimit()) {
            setDebitCbStatus(STATUS_BALANCE_BELOW_LIMIT);
        } else if(!isCardPresentedAndUsable() && MyGlobalSettings.getCardReqCbRedeem()) {
            setDebitCbStatus(STATUS_QR_CARD_NOT_USED);
        } else if(mRetainedFragment.mBillTotal <= 0) {
            setDebitCbStatus(STATUS_NO_BILL_AMT);
        } else {
            mCbBalance = cbBalance;
            // by default, dont try cb debit
            // change status by clicking the image/label
            setDebitCbStatus(STATUS_CLEARED);
        }

        // Init 'add cashback' status
        if( (mCbRate > 0 && mRetainedFragment.mBillTotal > 0) ||
                (mPpCbRate > 0 && mMerchantUser.getMerchant().getCl_add_enable()) ) {
            setAddCbStatus(STATUS_AUTO);
        } else {
            setAddCbStatus(STATUS_DISABLED);
        }
    }

    private void initAmtUiVisibility(boolean expandClickCase) {
        LogMy.d(TAG, "In initAmtUiVisibility");

        // Cash Account section
        initAccUiVisibility(expandClickCase);

        // Cashback section
        initCbUiVisibility(expandClickCase);

        // Cash Paid section
        if(mCashPaidHelper==null) {
            calcMinCashToPay();
            //mCashPaidHelper = new CashPaid(mMinCashToPay, mRetainedFragment.mBillTotal, this, getActivity());
            mCashPaidHelper = new CashPaid2(mMinCashToPay, mRetainedFragment.mBillTotal, this, getActivity());
        }
        // initView for 'backstack' scenarios also - as it contain pointers to onscreen views
        mCashPaidHelper.initView(getView());
    }

    private void initAccUiVisibility(boolean expandClickCase) {
        // Add account row
        if(mAddClStatus==STATUS_DISABLED) {
            // this can be hidden
            hideIfReq(mLayoutAddCl, expandClickCase);
        } else {
            // expand icon click have no meaning, if this cant be hidden
            mLayoutAddCl.setVisibility(View.VISIBLE);
        }
        // Debit account row
        if(mDebitClStatus!=STATUS_AUTO && mDebitClStatus!=STATUS_CLEARED && mDebitClStatus!=STATUS_AUTO_CLEARED) {
            // this can be hidden
            hideIfReq(mLayoutDebitCl, expandClickCase);
        } else {
            mLayoutDebitCl.setVisibility(View.VISIBLE);
        }
        // Change expand icon and layour visibility - based on final status
        if(mLayoutAddCl.getVisibility()==View.GONE &&
                mLayoutDebitCl.getVisibility()==View.GONE) {
            //mLayoutCashAccount.setAlpha(0.4f);
            mAccExpand.setEnabled(true);
            mAccExpand.setAlpha(1.0f);
            mAccExpand.setVisibility(View.VISIBLE);
            mAccExpand.setImageResource(R.drawable.ic_expand_more_white_18dp);

        } else if(mLayoutAddCl.getVisibility()==View.GONE ||
                mLayoutDebitCl.getVisibility()==View.GONE) {
            // one row is hidden, show expand icon
            //mLayoutCashAccount.setAlpha(1.0f);
            mAccExpand.setEnabled(true);
            mAccExpand.setAlpha(1.0f);
            mAccExpand.setVisibility(View.VISIBLE);
            mAccExpand.setImageResource(R.drawable.ic_expand_more_white_18dp);
        } else {
            // all visible - dont show expand icon if this is not 'expand icon' click case
            if(expandClickCase) {
                //mLayoutCashAccount.setAlpha(1.0f);
                mAccExpand.setEnabled(true);
                mAccExpand.setAlpha(1.0f);
                mAccExpand.setVisibility(View.VISIBLE);
                mAccExpand.setImageResource(R.drawable.ic_expand_less_white_18dp);
            } else {
                //mAccExpand.setVisibility(View.GONE);
                mAccExpand.setEnabled(false);
                mAccExpand.setAlpha(0.3f);
            }
        }
    }

    private void initCbUiVisibility(boolean expandClickCase) {
        // Add cashback row
        if(mAddCbStatus==STATUS_NO_BILL_AMT) {
            // this can be hidden
            hideIfReq(mLayoutAddCb, expandClickCase);
        } else {
            // expand icon click have no meaning, if this cant be hidden
            mLayoutAddCb.setVisibility(View.VISIBLE);
        }
        // Debit cashback row
        if(mDebitCbStatus!=STATUS_AUTO && mDebitCbStatus!=STATUS_CLEARED && mDebitCbStatus!=STATUS_AUTO_CLEARED) {
            // this can be hidden
            hideIfReq(mLayoutDebitCb, expandClickCase);
        } else {
            mLayoutDebitCb.setVisibility(View.VISIBLE);
        }
        // Change expand icon and layout visibility - based on final status
        if(mLayoutAddCb.getVisibility()==View.GONE &&
                mLayoutDebitCb.getVisibility()==View.GONE) {
            //mLayoutCashBack.setAlpha(0.4f);
            //mCbDiv1.setVisibility(View.INVISIBLE);
            //mCbLabel.setVisibility(View.VISIBLE);
            //mCbDiv2.setVisibility(View.GONE);
            mCbExpand.setEnabled(true);
            mCbExpand.setAlpha(1.0f);
            mCbExpand.setVisibility(View.VISIBLE);
            mCbExpand.setImageResource(R.drawable.ic_expand_more_white_18dp);

        } else if(mLayoutAddCb.getVisibility()==View.GONE ||
                mLayoutDebitCb.getVisibility()==View.GONE) {
            // one row is hidden, show expand icon
            //mLayoutCashBack.setAlpha(1.0f);
            //mCbDiv1.setVisibility(View.VISIBLE);
            //mCbLabel.setVisibility(View.VISIBLE);
            //mCbDiv2.setVisibility(View.VISIBLE);
            mCbExpand.setEnabled(true);
            mCbExpand.setAlpha(1.0f);
            mCbExpand.setVisibility(View.VISIBLE);
            mCbExpand.setImageResource(R.drawable.ic_expand_more_white_18dp);
        } else {
            // all visible - disable expand icon if this is not 'expand icon' click case
            //mLayoutCashBack.setAlpha(1.0f);
            if(expandClickCase) {
                //mCbDiv1.setVisibility(View.VISIBLE);
                //mCbLabel.setVisibility(View.VISIBLE);
                //mCbDiv2.setVisibility(View.VISIBLE);
                mCbExpand.setEnabled(true);
                mCbExpand.setAlpha(1.0f);
                mCbExpand.setVisibility(View.VISIBLE);
                mCbExpand.setImageResource(R.drawable.ic_expand_less_white_18dp);
            } else {
                //mCbExpand.setVisibility(View.GONE);
                mCbExpand.setEnabled(false);
                mCbExpand.setAlpha(0.3f);
            }
        }
    }

    private void hideIfReq(View layout, boolean expandClickCase) {
        if(expandClickCase) {
            // reverse the current status
            if(layout.getVisibility()==View.GONE) {
                layout.setVisibility(View.VISIBLE);
            } else {
                layout.setVisibility(View.GONE);
            }
        } else {
            layout.setVisibility(View.GONE);
        }
    }

    private void initCashUiVisibility(boolean expandClickCase) {
        // Add cashback row
        if(expandClickCase) {
            hideIfReq(mCashRow1, expandClickCase);
            hideIfReq(mCashRow2, expandClickCase);
        }

        if(mCashRow1.getVisibility()==View.GONE ||
                mCashRow2.getVisibility()==View.GONE) {
            mCashExpand.setImageResource(R.drawable.ic_expand_more_white_18dp);
        } else {
            mCashExpand.setImageResource(R.drawable.ic_expand_less_white_18dp);
        }
    }

    /*private void initAmtUiVisibility() {
        LogMy.d(TAG, "In initAmtUiVisibility");

        float extraSpace = 0.0f;
        boolean cashAccountViewGone = false;
        boolean cashBackViewGone = false;

        // if both 'add' and 'redeem' cash account are not in enabled state - remove the view
        if( mAddClStatus==STATUS_DISABLED && mDebitClStatus ==STATUS_NO_BALANCE ) {
            mLayoutCashAccount.setVisibility(View.GONE);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mSpaceCashAccount.getLayoutParams();
            extraSpace = extraSpace + params.weight;
            mSpaceCashAccount.setVisibility(View.GONE);
            cashAccountViewGone = true;
        }

        // if both 'add' and 'redeem' cashback are in not enabled state - remove the view
        if( mAddCbStatus !=STATUS_AUTO &&
                mDebitCbStatus !=STATUS_AUTO && mDebitCbStatus !=STATUS_CLEARED ) {
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

        mLayoutCashPaid.setVisibility(View.VISIBLE);
        mSpaceCashPaid.setVisibility(View.VISIBLE);

        if(mCashPaidHelper==null) {
            calcMinCashToPay();
            //mCashPaidHelper = new CashPaid(mMinCashToPay, mRetainedFragment.mBillTotal, this, getActivity());
            mCashPaidHelper = new CashPaid2(mMinCashToPay, mRetainedFragment.mBillTotal, this, getActivity());
        }
        // initView for 'backstack' scenarios also - as it contain pointers to onscreen views
        mCashPaidHelper.initView(getView());

        if(extraSpace > 0.0f) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mSpaceAboveButton.getLayoutParams();
            params.weight = params.weight + extraSpace;
            mSpaceAboveButton.setLayoutParams(params);
        }
    }*/

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

    private View mLayoutDebitCl;
    private AppCompatCheckBox mRadioDebitCl;
    private EditText mLabelDebitCl;
    private EditText mInputDebitCl;

    private View mLayoutCashBack;

    private View mLayoutDebitCb;
    private AppCompatCheckBox mCheckboxDebitCb;
    private EditText mLabelDebitCb;
    private EditText mInputDebitCb;

    private View mCbDiv1;
    private View mCbLabel;
    //private View mCbDiv2;
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

    private AppCompatImageButton mAccExpand;
    private AppCompatImageButton mCbExpand;
    private AppCompatImageButton mCashExpand;

    private View mCashRow1;
    private View mCashRow2;

    private void bindUiResources(View v) {

        mInputBillAmt = (EditText) v.findViewById(R.id.input_trans_bill_amt);

        mLayoutCashAccount = v.findViewById(R.id.layout_cash_account);

        mLayoutAddCl = v.findViewById(R.id.layout_add_cl);
        //mCheckboxAddCl = (AppCompatCheckBox) v.findViewById(R.id.checkbox_add_cl);
        mRadioAddCl = (AppCompatCheckBox) v.findViewById(R.id.radio_add_cl);
        mLabelAddCl = (EditText) v.findViewById(R.id.label_trans_add_cl);
        mInputAddCl = (EditText) v.findViewById(R.id.input_trans_add_cl);

        mLayoutDebitCl = v.findViewById(R.id.layout_redeem_cl);
        //mCheckboxRedeemCl = (AppCompatCheckBox) v.findViewById(R.id.checkbox_debit_cl);
        mRadioDebitCl = (AppCompatCheckBox) v.findViewById(R.id.radio_redeem_cl);
        mLabelDebitCl = (EditText) v.findViewById(R.id.label_trans_redeem_cl);
        mInputDebitCl = (EditText) v.findViewById(R.id.input_trans_redeem_cl);


        mCbDiv1 = v.findViewById(R.id.cb_divider_1);
        mCbLabel = v.findViewById(R.id.label_cash_back);
        //mCbDiv2 = v.findViewById(R.id.cb_divider_2);
        mSpaceCashAccount = v.findViewById(R.id.space_cash_account);
        mSpaceCashBack = v.findViewById(R.id.space_cashback);
        mSpaceCashPaid = v.findViewById(R.id.space_cash_paid);
        mSpaceAboveButton = v.findViewById(R.id.space_above_button);

        mLayoutCashBack = v.findViewById(R.id.layout_cashback);
        mLayoutDebitCb = v.findViewById(R.id.layout_redeem_cb);
        mCheckboxDebitCb = (AppCompatCheckBox) v.findViewById(R.id.checkbox_redeem_cb);
        mLabelDebitCb = (EditText) v.findViewById(R.id.label_trans_redeem_cb);
        mInputDebitCb = (EditText) v.findViewById(R.id.input_trans_redeem_cb);

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

        mAccExpand = (AppCompatImageButton) v.findViewById(R.id.btn_expand_acc);
        mCbExpand = (AppCompatImageButton) v.findViewById(R.id.btn_expand_cb);
        mCashExpand = (AppCompatImageButton) v.findViewById(R.id.btn_expand_cash);

        mCashRow1 = v.findViewById(R.id.layout_cash_row1);
        mCashRow2 = v.findViewById(R.id.layout_cash_row2);
    }

    @Override
    public void onResume() {
        //LogMy.d(TAG, "In onResume");
        super.onResume();
        mCallback.setDrawerState(false);

        try {
            displayInputBillAmt();
            // re-calculate all amounts
            calcAndSetAmts(false);
            calcAndSetAddCb();
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in CustomerTransactionFragment:onResume", e);
            DialogFragmentWrapper dialog = DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true);
            dialog.setTargetFragment(this, REQ_NOTIFY_ERROR_EXIT);
            dialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            //throw e;
        }

        //mCashPaidHelper.refreshValues(mMinCashToPay, mCashPaid, mRetainedFragment.mBillTotal);
        //setCashBalance();
        mCallback.getRetainedFragment().setResumeOk(true);
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

        outState.putInt("mAddCbStatus", mAddCbStatus);
        outState.putInt("mAddClStatus", mAddClStatus);
        outState.putInt("mDebitClStatus", mDebitClStatus);
        outState.putInt("mDebitCbStatus", mDebitCbStatus);
        outState.putBoolean("mDebitCbOnPriority", mDebitCbOnPriority);

        outState.putInt("mDebitCashload", mDebitCashload);
        outState.putInt("mDebitCashback", mDebitCashback);
        outState.putInt("mAddCashload", mAddCashload);
        //outState.putInt("mAddCashback", mAddCashback);
        outState.putInt("mAddCbOnBill", mAddCbOnBill);
        outState.putInt("mAddCbOnAcc", mAddCbOnAcc);
        outState.putInt("mCashPaid", mCashPaid);
        //outState.putInt("mToPayCash", mToPayCash);
        outState.putInt("mReturnCash", mReturnCash);
    }
}