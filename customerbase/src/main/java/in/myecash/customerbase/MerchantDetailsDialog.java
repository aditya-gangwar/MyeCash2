package in.myecash.customerbase;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Date;

import in.myecash.common.constants.CommonConstants;
import in.myecash.appbase.entities.MyCashback;
import in.myecash.appbase.entities.MyMerchant;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.customerbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 21-05-2016.
 */
public class MerchantDetailsDialog extends DialogFragment  {
    private static final String TAG = "MerchantDetailsDialog";
    private static final String ARG_CB_MCHNTID = "mchntId";

    private MerchantDetailsDialogIf mCallback;
    private SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

    public interface MerchantDetailsDialogIf {
        MyRetainedFragment getRetainedFragment();
        //void getCustTxns(String id);
    }

    public static MerchantDetailsDialog newInstance(String merchantId) {
        LogMy.d(TAG, "Creating new MerchantDetailsDialog instance: "+merchantId);
        Bundle args = new Bundle();
        args.putString(ARG_CB_MCHNTID, merchantId);

        MerchantDetailsDialog fragment = new MerchantDetailsDialog();
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

        MyCashback cb = null;
        String mchntId = getArguments().getString(ARG_CB_MCHNTID, null);
        cb = mCallback.getRetainedFragment().mCashbacks.get(mchntId);
        initDialogView(cb);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_mchnt_details_for_cust, null);

        bindUiResources(v);

        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setView(v)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(MerchantDetailsDialog.this, (AlertDialog) dialog);
            }
        });

        return dialog;
    }

    private void initDialogView(MyCashback cb) {
        MyMerchant merchant = cb.getMerchant();

        if(merchant != null) {
            mName.setText(merchant.getName());
            String txt = merchant.getBusinessCategory()+", "+merchant.getCity();
            mCategoryNdCity.setText(txt);
            mCbRate.setText(merchant.getCbRate());
            Date time = cb.getUpdateTime();
            if(time==null) {
                time = cb.getCreateTime();
            }
            mLastTxnTime.setText(mSdfDateWithTime.format(time));

            mInputTotalBill.setText(cb.getBillAmt());

            mInputAccAvailable.setText(AppCommonUtil.getAmtStr(cb.getCurrClBalance()));
            mInputAccTotalAdd.setText(AppCommonUtil.getAmtStr(cb.getClCredit()));
            mInputAccTotalDebit.setText(AppCommonUtil.getAmtStr(cb.getClDebit()));

            mInputCbAvailable.setText(AppCommonUtil.getAmtStr(cb.getCurrCbBalance()));
            mInputCbTotalAward.setText(AppCommonUtil.getAmtStr(cb.getCbCredit()));
            mInputCbTotalRedeem.setText(AppCommonUtil.getAmtStr(cb.getCbRedeem()));

            mInputMobileNum.setText(merchant.getMobileNum());
            mInputStatus.setText(merchant.getStatus());
            mAddressLine1.setText(merchant.getAddressLine1());
            mAddressCity.setText(merchant.getCity());
            mAddressState.setText(merchant.getState());

        } else {
            LogMy.wtf(TAG, "Merchant object is null !!");
            getDialog().dismiss();
        }
    }

    private EditText mName;
    private EditText mCategoryNdCity;
    private EditText mCbRate;
    private EditText mLastTxnTime;

    private EditText mInputTotalBill;

    private EditText mInputAccAvailable;
    private EditText mInputAccTotalAdd;
    private EditText mInputAccTotalDebit;

    private EditText mInputCbAvailable;
    private EditText mInputCbTotalAward;
    private EditText mInputCbTotalRedeem;

    private EditText mInputMobileNum;
    private EditText mInputStatus;
    private EditText mAddressLine1;
    private EditText mAddressCity;
    private EditText mAddressState;

    private void bindUiResources(View v) {

        mName = (EditText) v.findViewById(R.id.input_brand_name);;
        mCategoryNdCity = (EditText) v.findViewById(R.id.input_category_city);;
        mCbRate = (EditText) v.findViewById(R.id.input_cb_rate);;
        mLastTxnTime = (EditText) v.findViewById(R.id.input_last_txn_time);;

        mInputTotalBill = (EditText) v.findViewById(R.id.input_total_bill);

        mInputCbAvailable = (EditText) v.findViewById(R.id.input_cb_balance);
        mInputCbTotalAward = (EditText) v.findViewById(R.id.input_cb_award);
        mInputCbTotalRedeem = (EditText) v.findViewById(R.id.input_cb_redeem);

        mInputAccAvailable = (EditText) v.findViewById(R.id.input_acc_balance);
        mInputAccTotalAdd = (EditText) v.findViewById(R.id.input_acc_add);
        mInputAccTotalDebit = (EditText) v.findViewById(R.id.input_acc_debit);

        mInputMobileNum = (EditText) v.findViewById(R.id.input_mobile);
        mInputStatus = (EditText) v.findViewById(R.id.input_status);
        mAddressLine1 = (EditText) v.findViewById(R.id.input_address);
        mAddressCity = (EditText) v.findViewById(R.id.input_city);
        mAddressState = (EditText) v.findViewById(R.id.input_state);
    }
}
