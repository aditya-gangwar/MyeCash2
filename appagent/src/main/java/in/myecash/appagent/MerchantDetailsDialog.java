package in.myecash.appagent;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import in.myecash.appagent.helper.MyRetainedFragment;
import in.myecash.commonbase.constants.CommonConstants;
import in.myecash.commonbase.constants.DbConstants;
import in.myecash.commonbase.models.Merchants;
import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.LogMy;

import java.text.SimpleDateFormat;

/**
 * Created by adgangwa on 30-07-2016.
 */
public class MerchantDetailsDialog extends DialogFragment
    implements View.OnClickListener {
    private static final String TAG = "MerchantDetailsDialog";

    private final SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

    private MerchantDetailsDialogIf mCallback;
    public interface MerchantDetailsDialogIf {
        MyRetainedFragment getRetainedFragment();
        void disableMerchant();
        void launchMchntApp();
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
        initDialogView();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_mchnt_details_internal, null);

        bindUiResources(v);

        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setView(v)
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                /*
                                int selectedId = mRadioGroup.getCheckedRadioButtonId();
                                switch (selectedId) {
                                    case R.id.radioButtonStatus:
                                        if(mStatusChange.isEnabled()) {
                                            mCallback.disableMerchant();
                                        }
                                        break;
                                    case R.id.radioButtonTxns:
                                        mCallback.getMerchantTxns();
                                        break;
                                    case R.id.radioButtonCb:
                                        mCallback.getMerchantCbs();
                                        break;
                                }*/

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

    private void initDialogView() {
        Merchants merchant = mCallback.getRetainedFragment().mCurrMerchant;

        mStoreName.setText(merchant.getName());
        mStoreCategory.setText(merchant.getBuss_category().getCategory_name());
        mMerchantId.setText(merchant.getAuto_id());

        int status = merchant.getAdmin_status();
        mInputStatus.setText(DbConstants.userStatusDesc[status]);
        mInputStatusDate.setText(mSdfDateWithTime.format(merchant.getStatus_update_time()));
        mInputReason.setText(DbConstants.statusReasonDescriptions[merchant.getStatus_reason()]);
        if(merchant.getAdmin_remarks()!=null) {
            mInputRemarks.setText(merchant.getAdmin_remarks());
        }

        mInputMobileNum.setText(merchant.getMobile_num());
        mInputEmail.setText(merchant.getEmail());

        mAddress.setText(merchant.getAddress().getLine_1());
        mCity.setText(merchant.getAddress().getCity().getCity());
        mState.setText(merchant.getAddress().getCity().getState());

        mCbRate.setText(merchant.getCb_rate());
        mAddCashStatus.setText(merchant.getCl_add_enable().toString());

        mAccStatus.setOnClickListener(this);
        mLaunchApp.setOnClickListener(this);

        if(status!=DbConstants.USER_STATUS_ACTIVE) {
            mAccStatus.setEnabled(false);
            mAccStatus.setOnClickListener(null);
            mAccStatus.setAlpha(0.4f);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_acc_status:
                LogMy.d(TAG,"Clicked change acc status button.");
                mCallback.disableMerchant();
                getDialog().dismiss();
                break;

            case R.id.btn_launch_app:
                LogMy.d(TAG,"Clicked launch merchant app button.");
                mCallback.launchMchntApp();
                getDialog().dismiss();
                break;
        }
    }

    private EditText mMerchantId;
    private EditText mStoreName;
    private EditText mStoreCategory;

    private EditText mInputStatus;
    private EditText mInputStatusDate;
    private EditText mInputReason;
    private EditText mInputRemarks;

    private EditText mInputMobileNum;
    private EditText mInputEmail;

    private EditText mAddress;
    private EditText mCity;
    private EditText mState;

    private EditText mCbRate;
    private EditText mAddCashStatus;

    private AppCompatButton mAccStatus;
    private AppCompatButton mLaunchApp;

    /*
    private RadioGroup mRadioGroup;
    private RadioButton mStatusChange;
    private RadioButton mGetTxns;
    private RadioButton mGetCustomers;*/

    private void bindUiResources(View v) {
        mMerchantId = (EditText) v.findViewById(R.id.input_merchant_id);
        mStoreName = (EditText) v.findViewById(R.id.input_store_name);
        mStoreCategory = (EditText) v.findViewById(R.id.input_store_category);

        mInputStatus = (EditText) v.findViewById(R.id.input_status);
        mInputReason = (EditText) v.findViewById(R.id.input_status_reason);
        mInputStatusDate = (EditText) v.findViewById(R.id.input_status_date);
        mInputRemarks = (EditText) v.findViewById(R.id.input_status_remarks);

        mInputMobileNum = (EditText) v.findViewById(R.id.input_merchant_mobile);
        mInputEmail = (EditText) v.findViewById(R.id.input_merchant_email);

        mAddress = (EditText) v.findViewById(R.id.input_address);
        mCity = (EditText) v.findViewById(R.id.input_city);
        mState = (EditText) v.findViewById(R.id.input_state);

        mCbRate = (EditText) v.findViewById(R.id.input_cbrate);
        mAddCashStatus = (EditText) v.findViewById(R.id.input_addcash);

        mAccStatus = (AppCompatButton) v.findViewById(R.id.btn_acc_status);
        mLaunchApp = (AppCompatButton) v.findViewById(R.id.btn_launch_app);

        /*
        mRadioGroup = (RadioGroup) v.findViewById(R.id.radioGroupActions);
        mStatusChange = (RadioButton) v.findViewById(R.id.radioButtonStatus);
        mGetTxns = (RadioButton) v.findViewById(R.id.radioButtonTxns);
        mGetCustomers = (RadioButton) v.findViewById(R.id.radioButtonCb);*/
    }

}

