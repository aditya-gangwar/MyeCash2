package in.myecash.merchantbase;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import in.myecash.appbase.BaseDialog;
import in.myecash.common.CommonUtils;
import in.myecash.common.DateUtil;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.MyGlobalSettings;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.entities.MyCashback;
import in.myecash.common.MyCustomer;
import in.myecash.merchantbase.entities.MerchantUser;
import in.myecash.merchantbase.helper.MyRetainedFragment;

import java.text.SimpleDateFormat;

/**
 * Created by adgangwa on 21-05-2016.
 */
public class CustomerDetailsDialog extends BaseDialog {
    private static final String TAG = "MchntApp-CustomerDetailsDialog";
    private static final String ARG_CB_POSITION = "cbPosition";
    private static final String ARG_GETTXNS_BTN = "getTxnsBtn";

    private CustomerDetailsDialogIf mCallback;
    private SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

    public interface CustomerDetailsDialogIf {
        MyRetainedFragment getRetainedFragment();
        void getCustTxns(String id);
    }

    public static CustomerDetailsDialog newInstance(int position, boolean showGetTxnsBtn) {
        LogMy.d(TAG, "Creating new CustomerDetailsDialog instance: "+position);
        Bundle args = new Bundle();
        args.putInt(ARG_CB_POSITION, position);
        args.putBoolean(ARG_GETTXNS_BTN, showGetTxnsBtn);

        CustomerDetailsDialog fragment = new CustomerDetailsDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (CustomerDetailsDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement CustomerDetailsDialogIf");
        }

        //MyCustomer cust = mCallback.getRetainedFragment().mCurrCustomer;
        MyCashback cb = mCallback.getRetainedFragment().mCurrCashback;
        int position = getArguments().getInt(ARG_CB_POSITION, -1);
        if(position>=0) {
            cb = mCallback.getRetainedFragment().mLastFetchCashbacks.get(position);
            //cust = cb.getCustomer();
        }
        initDialogView(cb);
    }

    @Override
    public void handleBtnClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                dialog.dismiss();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                dialog.dismiss();
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                mCallback.getCustTxns(mInputCustomerId.getText().toString());
                dialog.dismiss();
                break;
        }
    }

    @Override
    public boolean handleTouchUp(View v) {
        return false;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_customer_details, null);

        bindUiResources(v);

        boolean showGetTxns = getArguments().getBoolean(ARG_GETTXNS_BTN, true);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.MyAnimatedDialog)
                .setView(v)
                .setPositiveButton(android.R.string.ok,this);

        if(showGetTxns) {
            builder.setNeutralButton("Get Txns", this);
        }

        Dialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(CustomerDetailsDialog.this, (AlertDialog) dialog);
            }
        });

        return dialog;
    }

    private void initDialogView(MyCashback cb) {
        MyCustomer cust = cb.getCustomer();

        if(cust != null) {
            mInputCustomerId.setText(cust.getPrivateId());
            mInputMobileNum.setText(CommonUtils.getPartialVisibleStr(cust.getMobileNum()));
            if(cb.getLastTxnTime()!=null) {
                mLastUsedHere.setText(mSdfDateWithTime.format(cb.getLastTxnTime()));
            } else {
                mLastUsedHere.setText("-");
            }
            mFirstUsedHere.setText(mSdfDateWithTime.format(cb.getCreateTime()));

            if(cust.getCardId()==null || cust.getCardId().isEmpty()) {
                mLayoutCard.setVisibility(View.GONE);
            } else {
                mLayoutCard.setVisibility(View.VISIBLE);
                mInputQrCard.setText(CommonUtils.getPartialVisibleStr(cust.getCardId()));
                mInputCardStatus.setText(DbConstants.cardStatusDesc[cust.getCardStatus()]);
                mLayoutCardStatusDate.setVisibility(View.GONE);
                if (cust.getCardStatus() != DbConstants.CUSTOMER_CARD_STATUS_ACTIVE) {
                    mInputCardStatus.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
                    mLayoutCardStatusDate.setVisibility(View.VISIBLE);
                    mCardStatusDate.setText(cust.getCardStatusUpdateTime());
                }
            }
            int status = cust.getStatus();
            mInputStatus.setText(DbConstants.userStatusDesc[status]);
            if(status != DbConstants.USER_STATUS_ACTIVE) {
                mLayoutStatusDetails.setVisibility(View.VISIBLE);
                mInputStatus.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
                mInputStatusDate.setText(cust.getStatusUpdateTime());
                mInputReason.setText(cust.getStatusReason());

                if(status==DbConstants.USER_STATUS_LOCKED) {
                    mInputStatusDetails.setVisibility(View.VISIBLE);
                    DateUtil time = new DateUtil(cust.getStatusUpdateDate());
                    time.addMinutes(MyGlobalSettings.getAccBlockMins(DbConstants.USER_TYPE_CUSTOMER));
                    String detail = "Will be Unlocked at "+mSdfDateWithTime.format(time.getTime());
                    mInputStatusDetails.setText(detail);

                } else if(status==DbConstants.USER_STATUS_LIMITED_CREDIT_ONLY) {
                    mInputStatusDetails.setVisibility(View.VISIBLE);
                    DateUtil time = new DateUtil(cust.getStatusUpdateDate());
                    time.addMinutes(MyGlobalSettings.getCustAccLimitModeMins());
                    String detail = "Will be Active again at "+mSdfDateWithTime.format(time.getTime());
                    mInputStatusDetails.setText(detail);

                } else {
                    mInputStatusDetails.setVisibility(View.GONE);
                }
            } else {
                mLayoutStatusDetails.setVisibility(View.GONE);
            }

            mInputTotalBill.setText(AppCommonUtil.getAmtStr(cb.getBillAmt()));
            //mInputCbBill.setText(AppCommonUtil.getAmtStr(cb.getCbBillAmt()));

            if(cb.getClCredit()==0 && cb.getClDebit()==0) {
                mLayoutAccBalance.setVisibility(View.GONE);
            } else {
                mLayoutAccBalance.setVisibility(View.VISIBLE);
                mInputAccAvailable.setText(AppCommonUtil.getAmtStr(cb.getCurrClBalance()));
                mInputAccTotalAdd.setText(AppCommonUtil.getAmtStr(cb.getClCredit()));
                mInputAccTotalDebit.setText(AppCommonUtil.getAmtStr(cb.getClDebit()));
            }

            mInputCbAvailable.setText(AppCommonUtil.getAmtStr(cb.getCurrCbBalance()));
            mInputCbTotalAward.setText(AppCommonUtil.getAmtStr(cb.getCbCredit()));
            mInputCbTotalRedeem.setText(AppCommonUtil.getAmtStr(cb.getCbRedeem()));

            /*if(mCallback.getRetainedFragment().mMerchantUser.isPseudoLoggedIn()) {
                // set cust care specific fields too
                //mName.setText(cust.getName());
                mCreatedOn.setText(cust.getCustCreateTime());
                mFirstLogin.setText(cust.isFirstLoginOk().toString());
                //mInputAdminRemarks.setText(cust.getRemarks());
            } else {
                // hide fields for customer care logins only
                //mLayoutName.setVisibility(View.GONE);
                mLayoutCreated.setVisibility(View.GONE);
                mLayoutFirstLogin.setVisibility(View.GONE);
                //mLayoutRemarks.setVisibility(View.GONE);
                //mLayoutCardStatusDate.setVisibility(View.GONE);
            }*/

        } else {
            LogMy.wtf(TAG, "Customer or Cashback object is null !!");
            getDialog().dismiss();
        }
    }

    private TextView mInputCustomerId;
    private TextView mInputMobileNum;
    // TextView mName;
    private TextView mLastUsedHere;
    private TextView mFirstUsedHere;
    //private TextView mCreatedOn;
    //private TextView mFirstLogin;

    private TextView mInputQrCard;
    //private View mLayoutCardDetails;
    private TextView mInputCardStatus;
    private TextView mCardStatusDate;

    private TextView mInputStatus;
    private View mLayoutStatusDetails;
    private TextView mInputReason;
    private TextView mInputStatusDate;
    private TextView mInputStatusDetails;
    //private TextView mInputAdminRemarks;

    private TextView mInputTotalBill;
    //private TextView mInputCbBill;

    private TextView mInputAccAvailable;
    private TextView mInputAccTotalAdd;
    private TextView mInputAccTotalDebit;

    private TextView mInputCbAvailable;
    private TextView mInputCbTotalAward;
    private TextView mInputCbTotalRedeem;

    // layouts for optional fields
    //private View mLayoutName;
    private View mLayoutCreated;
    private View mLayoutFirstLogin;
    //private View mLayoutRemarks;
    private View mLayoutCardStatusDate;
    private View mLayoutAccBalance;
    private View mLayoutCard;

    private void bindUiResources(View v) {

        mInputCustomerId = (TextView) v.findViewById(R.id.input_customer_id);;
        mInputMobileNum = (TextView) v.findViewById(R.id.input_customer_mobile);
        //mName = (TextView) v.findViewById(R.id.input_cust_name);;
        mLastUsedHere = (TextView) v.findViewById(R.id.input_cust_last_activity);;
        mFirstUsedHere = (TextView) v.findViewById(R.id.input_cust_register_on);;
        //mCreatedOn = (TextView) v.findViewById(R.id.input_cust_created_on);;
        //mFirstLogin = (TextView) v.findViewById(R.id.input_first_login);;

        mLayoutCard = v.findViewById(R.id.layout_card);
        mInputQrCard = (TextView) v.findViewById(R.id.input_qr_card);
        //mLayoutCardDetails = v.findViewById(R.id.layout_card_details);
        mInputCardStatus = (TextView) v.findViewById(R.id.input_card_status);
        mCardStatusDate = (TextView) v.findViewById(R.id.input_card_status_date);

        mInputStatus = (TextView) v.findViewById(R.id.input_status);
        mLayoutStatusDetails = v.findViewById(R.id.layout_status_details);
        mInputReason = (TextView) v.findViewById(R.id.input_reason);
        mInputStatusDate = (TextView) v.findViewById(R.id.input_status_date);
        mInputStatusDetails = (TextView) v.findViewById(R.id.input_activation);
        //mInputAdminRemarks = (TextView) v.findViewById(R.id.input_status_remarks);

        mInputTotalBill = (TextView) v.findViewById(R.id.input_total_bill);
        //mInputCbBill = (TextView) v.findViewById(R.id.input_cb_bill);

        mInputAccAvailable = (TextView) v.findViewById(R.id.input_acc_balance);
        mInputAccTotalAdd = (TextView) v.findViewById(R.id.input_acc_add);
        mInputAccTotalDebit = (TextView) v.findViewById(R.id.input_acc_debit);

        mInputCbAvailable = (TextView) v.findViewById(R.id.input_cb_balance);
        mInputCbTotalAward = (TextView) v.findViewById(R.id.input_cb_award);
        mInputCbTotalRedeem = (TextView) v.findViewById(R.id.input_cb_redeem);

        // layouts for optional fields
        //mLayoutName = v.findViewById(R.id.layout_cust_name);
        //mLayoutCreated = v.findViewById(R.id.layout_cust_created_on);
        //mLayoutFirstLogin = v.findViewById(R.id.layout_first_login);
        //mLayoutRemarks = v.findViewById(R.id.layout_status_remarks);
        mLayoutCardStatusDate = v.findViewById(R.id.layout_card_status_date);
        mLayoutAccBalance = v.findViewById(R.id.layout_acc_balance);

    }
}
