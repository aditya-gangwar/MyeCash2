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

import in.myecash.common.CommonUtils;
import in.myecash.common.DateUtil;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.MyGlobalSettings;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.entities.MyCashback;
import in.myecash.common.MyCustomer;
import in.myecash.merchantbase.helper.MyRetainedFragment;

import java.text.SimpleDateFormat;

/**
 * Created by adgangwa on 21-05-2016.
 */
public class CustomerDetailsDialog extends DialogFragment  {
    private static final String TAG = "CustomerDetailsDialog";
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
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_customer_details, null);

        bindUiResources(v);

        boolean showGetTxns = getArguments().getBoolean(ARG_GETTXNS_BTN, true);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setView(v)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

        if(showGetTxns) {
            builder.setNeutralButton("Get Txns", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mCallback.getCustTxns(mInputCustomerId.getText().toString());
                    dialog.dismiss();
                }
            });
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
            if(cb.getUpdateTime()!=null) {
                mLastUsedHere.setText(mSdfDateWithTime.format(cb.getUpdateTime()));
            } else {
                mLastUsedHere.setText("-");
            }
            mFirstUsedHere.setText(mSdfDateWithTime.format(cb.getCreateTime()));

            mInputQrCard.setText(CommonUtils.getPartialVisibleStr(cust.getCardId()));
            mInputCardStatus.setText(DbConstants.cardStatusDescriptions[cust.getCardStatus()]);
            if(cust.getCardStatus() != DbConstants.CUSTOMER_CARD_STATUS_ACTIVE) {
                mInputCardStatus.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
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
                    time.addMinutes(MyGlobalSettings.getAccBlockHrs(DbConstants.USER_TYPE_CUSTOMER) * 60);
                    String detail = "Will be Unlocked at "+mSdfDateWithTime.format(time.getTime());
                    mInputStatusDetails.setText(detail);

                } else if(status==DbConstants.USER_STATUS_LIMITED_CREDIT_ONLY) {
                    mInputStatusDetails.setVisibility(View.VISIBLE);
                    DateUtil time = new DateUtil(cust.getStatusUpdateDate());
                    time.addMinutes(MyGlobalSettings.getCustAccLimitModeHrs() * 60);
                    String detail = "Only 'Credit' txns allowed until "+mSdfDateWithTime.format(time.getTime());
                    mInputStatusDetails.setText(detail);

                } else {
                    mInputStatusDetails.setVisibility(View.GONE);
                }
            } else {
                mLayoutStatusDetails.setVisibility(View.GONE);
            }

            mInputTotalBill.setText(AppCommonUtil.getAmtStr(cb.getBillAmt()));
            mInputCbBill.setText(AppCommonUtil.getAmtStr(cb.getCbBillAmt()));

            mInputAccAvailable.setText(AppCommonUtil.getAmtStr(cb.getCurrClBalance()));
            mInputAccTotalAdd.setText(AppCommonUtil.getAmtStr(cb.getClCredit()));
            mInputAccTotalDebit.setText(AppCommonUtil.getAmtStr(cb.getClDebit()));

            mInputCbAvailable.setText(AppCommonUtil.getAmtStr(cb.getCurrCbBalance()));
            mInputCbTotalAward.setText(AppCommonUtil.getAmtStr(cb.getCbCredit()));
            mInputCbTotalRedeem.setText(AppCommonUtil.getAmtStr(cb.getCbRedeem()));

            if(mCallback.getRetainedFragment().mMerchantUser.isPseudoLoggedIn()) {
                // set cust care specific fields too
                //mName.setText(cust.getName());
                mCreatedOn.setText(cust.getCustCreateTime());
                mFirstLogin.setText(cust.isFirstLoginOk().toString());
                mCardStatusDate.setText(cust.getCardStatusUpdateTime());
                //mInputAdminRemarks.setText(cust.getRemarks());
            } else {
                // hide fields for customer care logins only
                //mLayoutName.setVisibility(View.GONE);
                mLayoutCreated.setVisibility(View.GONE);
                mLayoutFirstLogin.setVisibility(View.GONE);
                //mLayoutRemarks.setVisibility(View.GONE);
                mLayoutCardStatusDate.setVisibility(View.GONE);
            }

        } else {
            LogMy.wtf(TAG, "Customer or Cashback object is null !!");
            getDialog().dismiss();
        }
    }

    private EditText mInputCustomerId;
    private EditText mInputMobileNum;
    // EditText mName;
    private EditText mLastUsedHere;
    private EditText mFirstUsedHere;
    private EditText mCreatedOn;
    private EditText mFirstLogin;

    private EditText mInputQrCard;
    //private View mLayoutCardDetails;
    private EditText mInputCardStatus;
    private EditText mCardStatusDate;

    private EditText mInputStatus;
    private View mLayoutStatusDetails;
    private EditText mInputReason;
    private EditText mInputStatusDate;
    private EditText mInputStatusDetails;
    //private EditText mInputAdminRemarks;

    private EditText mInputTotalBill;
    private EditText mInputCbBill;

    private EditText mInputAccAvailable;
    private EditText mInputAccTotalAdd;
    private EditText mInputAccTotalDebit;

    private EditText mInputCbAvailable;
    private EditText mInputCbTotalAward;
    private EditText mInputCbTotalRedeem;

    // layouts for optional fields
    //private View mLayoutName;
    private View mLayoutCreated;
    private View mLayoutFirstLogin;
    //private View mLayoutRemarks;
    private View mLayoutCardStatusDate;

    private void bindUiResources(View v) {

        mInputCustomerId = (EditText) v.findViewById(R.id.input_customer_id);;
        mInputMobileNum = (EditText) v.findViewById(R.id.input_customer_mobile);
        //mName = (EditText) v.findViewById(R.id.input_cust_name);;
        mLastUsedHere = (EditText) v.findViewById(R.id.input_cust_last_activity);;
        mFirstUsedHere = (EditText) v.findViewById(R.id.input_cust_register_on);;
        mCreatedOn = (EditText) v.findViewById(R.id.input_cust_created_on);;
        mFirstLogin = (EditText) v.findViewById(R.id.input_first_login);;

        mInputQrCard = (EditText) v.findViewById(R.id.input_qr_card);
        //mLayoutCardDetails = v.findViewById(R.id.layout_card_details);
        mInputCardStatus = (EditText) v.findViewById(R.id.input_card_status);
        mCardStatusDate = (EditText) v.findViewById(R.id.input_card_status_date);

        mInputStatus = (EditText) v.findViewById(R.id.input_status);
        mLayoutStatusDetails = v.findViewById(R.id.layout_status_details);
        mInputReason = (EditText) v.findViewById(R.id.input_reason);
        mInputStatusDate = (EditText) v.findViewById(R.id.input_status_date);
        mInputStatusDetails = (EditText) v.findViewById(R.id.input_activation);
        //mInputAdminRemarks = (EditText) v.findViewById(R.id.input_status_remarks);

        mInputTotalBill = (EditText) v.findViewById(R.id.input_total_bill);
        mInputCbBill = (EditText) v.findViewById(R.id.input_cb_bill);

        mInputAccAvailable = (EditText) v.findViewById(R.id.input_acc_balance);
        mInputAccTotalAdd = (EditText) v.findViewById(R.id.input_acc_add);
        mInputAccTotalDebit = (EditText) v.findViewById(R.id.input_acc_debit);

        mInputCbAvailable = (EditText) v.findViewById(R.id.input_cb_balance);
        mInputCbTotalAward = (EditText) v.findViewById(R.id.input_cb_award);
        mInputCbTotalRedeem = (EditText) v.findViewById(R.id.input_cb_redeem);

        // layouts for optional fields
        //mLayoutName = v.findViewById(R.id.layout_cust_name);
        mLayoutCreated = v.findViewById(R.id.layout_cust_created_on);
        mLayoutFirstLogin = v.findViewById(R.id.layout_first_login);
        //mLayoutRemarks = v.findViewById(R.id.layout_status_remarks);
        mLayoutCardStatusDate = v.findViewById(R.id.layout_card_status_date);

    }
}
