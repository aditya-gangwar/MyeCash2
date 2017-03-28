package in.myecash.customerbase;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import in.myecash.appbase.BaseDialog;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppAlarms;
import in.myecash.common.constants.CommonConstants;
import in.myecash.appbase.entities.MyCashback;
import in.myecash.common.MyMerchant;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.common.constants.DbConstants;
import in.myecash.customerbase.entities.CustomerUser;
import in.myecash.customerbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 21-05-2016.
 */
public class MchntDetailsDialogCustApp extends BaseDialog {
    private static final String TAG = "CustApp-MerchantDetailsDialog";
    private static final String ARG_CB_MCHNTID = "mchntId";

    private MerchantDetailsDialogIf mCallback;
    private SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

    public interface MerchantDetailsDialogIf {
        MyRetainedFragment getRetainedFragment();
        void getMchntTxns(String id, String name);
    }

    public static MchntDetailsDialogCustApp newInstance(String merchantId) {
        LogMy.d(TAG, "Creating new MerchantDetailsDialog instance: "+merchantId);
        Bundle args = new Bundle();
        args.putString(ARG_CB_MCHNTID, merchantId);

        MchntDetailsDialogCustApp fragment = new MchntDetailsDialogCustApp();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (MerchantDetailsDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement MerchantDetailsDialogIf");
        }

        String mchntId = getArguments().getString(ARG_CB_MCHNTID, null);
        MyCashback cb = mCallback.getRetainedFragment().mCashbacks.get(mchntId);
        if(cb==null) {
            // I shouldn't be here
            //raise alarm
            Map<String,String> params = new HashMap<>();
            params.put("CustomerId", CustomerUser.getInstance().getCustomer().getPrivate_id());
            params.put("MerchantId",mchntId);
            AppAlarms.invalidCardState(CustomerUser.getInstance().getCustomer().getPrivate_id(),
                    DbConstants.USER_TYPE_CUSTOMER,"MerchantDetailsDialog:onActivityCreated",params);

            AppCommonUtil.toast(getActivity(), "Error. Please try again later.");
            // dismiss dialog
            getDialog().dismiss();
        } else {
            initDialogView(cb);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_mchnt_details_for_cust, null);

        bindUiResources(v);

        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setView(v)
                .setPositiveButton(android.R.string.ok, this)
                .setNeutralButton("Get Txns", this)
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(MchntDetailsDialogCustApp.this, (AlertDialog) dialog);
            }
        });

        return dialog;
    }

    @Override
    public void handleBtnClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                //Do nothing here because we override this button in OnShowListener to change the close behaviour.
                //However, we still need this because on older versions of Android unless we
                //pass a handler the button doesn't get instantiated
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                dialog.dismiss();
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                mCallback.getMchntTxns(mInputMchntId.getText().toString(),
                        mName.getText().toString());
                dialog.dismiss();
                break;
        }
    }

    @Override
    public boolean handleTouchUp(View v) {
        return false;
    }

    private void initDialogView(MyCashback cb) {
        MyMerchant merchant = cb.getMerchant();

        if(merchant != null) {
            if(merchant.getStatus()== DbConstants.USER_STATUS_UNDER_CLOSURE) {
                mLayoutExpNotice.setVisibility(View.VISIBLE);
                mInputExpNotice.setText(String.format(getString(R.string.mchnt_remove_notice_to_cust),
                        AppCommonUtil.getMchntRemovalDate(merchant.getRemoveReqDate())));
            } else {
                mLayoutExpNotice.setVisibility(View.GONE);
            }

            if(cb.getDpMerchant()!=null) {
                mImgMerchant.setImageBitmap(cb.getDpMerchant());
            }
            mName.setText(merchant.getName());
            String txt = merchant.getBusinessCategory()+", "+merchant.getCity();
            mCategoryNdCity.setText(txt);

            String textCbRate = "";
            if(Integer.valueOf(merchant.getPpCbRate()) <= 0) {
                textCbRate = merchant.getCbRate() + "%";
                mPpCbDetails.setVisibility(View.GONE);
            } else {
                textCbRate = merchant.getCbRate()+"% + "+merchant.getPpCbRate()+"% *";
                mPpCbDetails.setVisibility(View.VISIBLE);
                String ppCbDetails = "* Extra "+merchant.getPpCbRate()+"% when Prepaid amount > "+AppCommonUtil.getAmtStr(merchant.getPpMinAmt());
                mPpCbDetails.setText(ppCbDetails);
            }
            mCbRate.setText(textCbRate);

            Date time = cb.getLastTxnTime();
            if(time==null) {
                time = cb.getCreateTime();
            }
            mLastTxnTime.setText(mSdfDateWithTime.format(time));

            mInputTotalBill.setText(AppCommonUtil.getAmtStr(cb.getBillAmt()));

            if(cb.getClCredit()==0 && cb.getClDebit()==0) {
                mLabelAcc.setVisibility(View.GONE);
                mLayoutBalAcc.setVisibility(View.GONE);
                mLayoutAddAcc.setVisibility(View.GONE);
                mLayoutDebitAcc.setVisibility(View.GONE);
            } else {
                mInputAccAvailable.setText(AppCommonUtil.getAmtStr(cb.getCurrClBalance()));
                mInputAccTotalAdd.setText(AppCommonUtil.getAmtStr(cb.getClCredit()));
                mInputAccTotalDebit.setText(AppCommonUtil.getAmtStr(cb.getClDebit()));
            }

            mInputCbAvailable.setText(AppCommonUtil.getAmtStr(cb.getCurrCbBalance()));
            mInputCbTotalAward.setText(AppCommonUtil.getAmtStr(cb.getCbCredit()));
            mInputCbTotalRedeem.setText(AppCommonUtil.getAmtStr(cb.getCbRedeem()));

            mInputMchntId.setText(merchant.getId());
            String phone = AppConstants.PHONE_COUNTRY_CODE+merchant.getContactPhone();
            mInputContactPhone.setText(phone);
            mInputStatus.setText(DbConstants.userStatusDesc[merchant.getStatus()]);
            mAddressLine1.setText(merchant.getAddressLine1());
            mAddressCity.setText(merchant.getCity());
            mAddressState.setText(merchant.getState());

        } else {
            LogMy.wtf(TAG, "Merchant object is null !!");
            getDialog().dismiss();
        }
    }

    private ImageView mImgMerchant;
    private EditText mName;
    private EditText mCategoryNdCity;
    private EditText mCbRate;
    private EditText mPpCbDetails;

    private EditText mLastTxnTime;
    private EditText mInputTotalBill;

    private EditText mInputAccAvailable;
    private EditText mInputAccTotalAdd;
    private EditText mInputAccTotalDebit;

    private View mLabelAcc;
    private View mLayoutBalAcc;
    private View mLayoutAddAcc;
    private View mLayoutDebitAcc;

    private EditText mInputCbAvailable;
    private EditText mInputCbTotalAward;
    private EditText mInputCbTotalRedeem;

    private EditText mInputMchntId;
    private EditText mInputContactPhone;
    private EditText mInputStatus;
    private EditText mAddressLine1;
    private EditText mAddressCity;
    private EditText mAddressState;

    private View mLayoutExpNotice;
    private EditText mInputExpNotice;

    private void bindUiResources(View v) {

        mImgMerchant = (ImageView) v.findViewById(R.id.img_merchant);;

        mName = (EditText) v.findViewById(R.id.input_brand_name);;
        mCategoryNdCity = (EditText) v.findViewById(R.id.input_category_city);;
        mCbRate = (EditText) v.findViewById(R.id.input_cb_rate);;
        mPpCbDetails = (EditText) v.findViewById(R.id.input_pp_cb_details);;
        mLastTxnTime = (EditText) v.findViewById(R.id.input_last_txn_time);;

        mInputTotalBill = (EditText) v.findViewById(R.id.input_total_bill);

        mInputCbAvailable = (EditText) v.findViewById(R.id.input_cb_balance);
        mInputCbTotalAward = (EditText) v.findViewById(R.id.input_cb_award);
        mInputCbTotalRedeem = (EditText) v.findViewById(R.id.input_cb_redeem);

        mInputAccAvailable = (EditText) v.findViewById(R.id.input_acc_balance);
        mInputAccTotalAdd = (EditText) v.findViewById(R.id.input_acc_add);
        mInputAccTotalDebit = (EditText) v.findViewById(R.id.input_acc_debit);

        mLabelAcc = v.findViewById(R.id.label_acc);
        mLayoutBalAcc = v.findViewById(R.id.layout_bal_acc);
        mLayoutAddAcc = v.findViewById(R.id.layout_add_acc);
        mLayoutDebitAcc = v.findViewById(R.id.layout_debit_acc);

        mInputMchntId = (EditText) v.findViewById(R.id.input_merchant_id);
        mInputContactPhone = (EditText) v.findViewById(R.id.input_mobile);
        mInputStatus = (EditText) v.findViewById(R.id.input_status);
        mAddressLine1 = (EditText) v.findViewById(R.id.input_address);
        mAddressCity = (EditText) v.findViewById(R.id.input_city);
        mAddressState = (EditText) v.findViewById(R.id.input_state);

        mLayoutExpNotice = v.findViewById(R.id.layout_expiry_notice);
        mInputExpNotice = (EditText) v.findViewById(R.id.input_expiry_notice);
    }
}
