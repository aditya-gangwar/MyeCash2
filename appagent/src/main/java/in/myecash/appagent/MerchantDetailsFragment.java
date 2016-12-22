package in.myecash.appagent;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import in.myecash.appagent.entities.AgentUser;
import in.myecash.appagent.helper.MyRetainedFragment;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.database.Merchants;
import in.myecash.appbase.utilities.LogMy;

import java.text.SimpleDateFormat;

/**
 * Created by adgangwa on 30-07-2016.
 */
public class MerchantDetailsFragment extends Fragment
    implements View.OnClickListener {
    private static final String TAG = "MerchantDetailsFragment";

    private static final String DIALOG_DISABLE_MCHNT = "disableMerchant";

    private final SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

    public interface MerchantDetailsFragmentIf {
        MyRetainedFragment getRetainedFragment();
        void launchMchntApp();
    }
    private MerchantDetailsFragmentIf mCallback;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (MerchantDetailsFragmentIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement MerchantDetailsFragmentIf");
        }
        initDialogView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateView");
        View v = inflater.inflate(R.layout.frag_mchnt_details_internal, container, false);

        // access to UI elements
        bindUiResources(v);
        //setup buttons
        if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_AGENT) {
            mAccStatus.setVisibility(View.GONE);
            mLaunchApp.setVisibility(View.GONE);

        } else if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_CC) {
            mAccStatus.setOnClickListener(this);
            mLaunchApp.setOnClickListener(this);
        }

        return v;
    }

    private void initDialogView() {
        Merchants merchant = mCallback.getRetainedFragment().mCurrMerchant;

        mMerchantId.setText(merchant.getAuto_id());
        mStoreName.setText(merchant.getName());
        mStoreCategory.setText(merchant.getBuss_category());
        mRegisteredOn.setText(mSdfDateWithTime.format(merchant.getCreated()));
        mFirstLogin.setText(merchant.getFirst_login_ok().toString());
        if(merchant.getFirst_login_ok()) {
            mFirstLogin.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
        }
        mDoB.setText(merchant.getDob());

        int status = merchant.getAdmin_status();
        mInputStatus.setText(DbConstants.userStatusDesc[status]);
        if(status != DbConstants.USER_STATUS_ACTIVE) {
            mInputStatus.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
        }
        mInputStatusDate.setText(mSdfDateWithTime.format(merchant.getStatus_update_time()));

        mInputReason.setText(merchant.getStatus_reason());
        /*if(merchant.getAdmin_remarks()!=null) {
            mInputRemarks.setText(merchant.getAdmin_remarks());
        }*/

        mInputMobileNum.setText(merchant.getMobile_num());
        mInputEmail.setText(merchant.getEmail());

        mAddress.setText(merchant.getAddress().getLine_1());
        mCity.setText(merchant.getAddress().getCity());
        mState.setText(merchant.getAddress().getState());

        mCbRate.setText(merchant.getCb_rate());
        mAddCashStatus.setText(merchant.getCl_add_enable().toString());
        String txt = "AC Debit: "+merchant.getCl_debit_limit_for_pin()+
                "; AC Credit: "+merchant.getCl_credit_limit_for_pin()+
                "; CB Debit: "+merchant.getCb_debit_limit_for_pin();
        mPinLimits.setText(txt);

        if( status!=DbConstants.USER_STATUS_ACTIVE && (mAccStatus.getVisibility()==View.VISIBLE) ) {
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
                // show disable dialog
                DisableMchntDialog dialog = new DisableMchntDialog();
                dialog.show(getFragmentManager(), DIALOG_DISABLE_MCHNT);
                //getDialog().dismiss();
                break;

            case R.id.btn_launch_app:
                LogMy.d(TAG,"Clicked launch merchant app button.");
                mCallback.launchMchntApp();
                //getDialog().dismiss();
                break;
        }
    }

    private EditText mMerchantId;
    private EditText mStoreName;
    private EditText mStoreCategory;
    private EditText mRegisteredOn;
    private EditText mFirstLogin;
    private EditText mDoB;

    private EditText mInputStatus;
    private EditText mInputStatusDate;
    private EditText mInputReason;
    //private EditText mInputRemarks;

    private EditText mInputMobileNum;
    private EditText mInputEmail;

    private EditText mAddress;
    private EditText mCity;
    private EditText mState;

    private EditText mCbRate;
    private EditText mAddCashStatus;
    private EditText mPinLimits;

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
        mRegisteredOn = (EditText) v.findViewById(R.id.input_registered_on);
        mFirstLogin = (EditText) v.findViewById(R.id.input_first_login);
        mDoB = (EditText) v.findViewById(R.id.input_dob);

        mInputStatus = (EditText) v.findViewById(R.id.input_status);
        mInputReason = (EditText) v.findViewById(R.id.input_status_reason);
        mInputStatusDate = (EditText) v.findViewById(R.id.input_status_date);
        //mInputRemarks = (EditText) v.findViewById(R.id.input_status_remarks);

        mInputMobileNum = (EditText) v.findViewById(R.id.input_merchant_mobile);
        mInputEmail = (EditText) v.findViewById(R.id.input_merchant_email);

        mAddress = (EditText) v.findViewById(R.id.input_address);
        mCity = (EditText) v.findViewById(R.id.input_city);
        mState = (EditText) v.findViewById(R.id.input_state);

        mCbRate = (EditText) v.findViewById(R.id.input_cbrate);
        mAddCashStatus = (EditText) v.findViewById(R.id.input_addcash);
        mPinLimits = (EditText) v.findViewById(R.id.input_pinLimits);

        mAccStatus = (AppCompatButton) v.findViewById(R.id.btn_acc_status);
        mLaunchApp = (AppCompatButton) v.findViewById(R.id.btn_launch_app);

        /*
        mRadioGroup = (RadioGroup) v.findViewById(R.id.radioGroupActions);
        mStatusChange = (RadioButton) v.findViewById(R.id.radioButtonStatus);
        mGetTxns = (RadioButton) v.findViewById(R.id.radioButtonTxns);
        mGetCustomers = (RadioButton) v.findViewById(R.id.radioButtonCb);*/
    }

}

