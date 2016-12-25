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

import java.text.SimpleDateFormat;

import in.myecash.appagent.entities.AgentUser;
import in.myecash.appagent.helper.MyRetainedFragment;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.common.CommonUtils;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.database.CustomerCards;
import in.myecash.common.database.Customers;

/**
 * Created by adgangwa on 29-11-2016.
 */
public class CustomerDetailsFragment extends Fragment
        implements View.OnClickListener {
    private static final String TAG = "AgentApp-CustomerDetailsFragment";

    private static final String DIALOG_DISABLE_CUST = "disableCustomer";

    private final SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
    private SimpleDateFormat mSdfDateOnly = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_DISPLAY, CommonConstants.DATE_LOCALE);

    public interface CustomerDetailsFragmentIf {
        MyRetainedFragment getRetainedFragment();
        void launchCustApp();
    }
    private CustomerDetailsFragmentIf mCallback;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (CustomerDetailsFragmentIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement CustomerDetailsFragmentIf");
        }
        initDialogView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateView");
        View v = inflater.inflate(R.layout.frag_cust_details_internal, container, false);

        // access to UI elements
        bindUiResources(v);
        //setup buttons
        if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_AGENT) {
            mAccDisable.setVisibility(View.GONE);
            mAccLimited.setVisibility(View.GONE);
            mLaunchApp.setVisibility(View.GONE);
            mCardDisable.setVisibility(View.GONE);

        } else if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_CC) {
            mAccDisable.setOnClickListener(this);
            mAccLimited.setOnClickListener(this);
            mLaunchApp.setOnClickListener(this);
            mCardDisable.setOnClickListener(this);
        }

        return v;
    }

    private void initDialogView() {
        Customers customer = mCallback.getRetainedFragment().mCurrCustomer;
        CustomerCards card = customer.getMembership_card();

        String name = customer.getFirstName()+" "+customer.getLastName();
        mName.setText(name);
        mInternalId.setText(customer.getPrivate_id());
        mInputMobileNum.setText(customer.getMobile_num());
        mFirstLogin.setText(customer.getFirst_login_ok().toString());
        if(customer.getFirst_login_ok()) {
            mFirstLogin.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
        }
        mRegisteredOn.setText(mSdfDateOnly.format(customer.getCreated()));
        mExpiringOn.setText(mSdfDateOnly.format(AppCommonUtil.getExpiryDate(customer)));

        int status = customer.getAdmin_status();
        mInputStatus.setText(DbConstants.userStatusDesc[status]);
        if(status != DbConstants.USER_STATUS_ACTIVE) {
            mInputStatus.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
        }
        mInputStatusDate.setText(mSdfDateWithTime.format(customer.getStatus_update_time()));
        mInputReason.setText(customer.getStatus_reason());

        mInputQrCard.setText(card.getCardNum());
        mInputCardStatus.setText(DbConstants.cardStatusDescInternal[card.getStatus()]);
        if(card.getStatus() != DbConstants.CUSTOMER_CARD_STATUS_ACTIVE) {
            mInputCardStatus.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
        }
        mCardStatusDate.setText(mSdfDateWithTime.format(card.getStatus_update_time()));
        mCardStatusReason.setText(mSdfDateWithTime.format(card.getStatus_reason()));

        if( status!=DbConstants.USER_STATUS_ACTIVE && (mAccDisable.getVisibility()==View.VISIBLE) ) {
            mAccDisable.setEnabled(false);
            mAccDisable.setOnClickListener(null);
            mAccDisable.setAlpha(0.4f);
        }

        if( status!=DbConstants.USER_STATUS_ACTIVE && (mAccLimited.getVisibility()==View.VISIBLE) ) {
            mAccLimited.setEnabled(false);
            mAccLimited.setOnClickListener(null);
            mAccLimited.setAlpha(0.4f);
        }

        if( card.getStatus() != DbConstants.CUSTOMER_CARD_STATUS_ACTIVE && (mCardDisable.getVisibility()==View.VISIBLE) ) {
            mCardDisable.setEnabled(false);
            mCardDisable.setOnClickListener(null);
            mCardDisable.setAlpha(0.4f);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_acc_status:
                LogMy.d(TAG,"Clicked change acc status button.");
                // show disable dialog
                DisableCustDialog dialog = DisableCustDialog.getInstance(false);
                dialog.show(getFragmentManager(), DIALOG_DISABLE_CUST);
                break;

            case R.id.btn_launch_app:
                LogMy.d(TAG,"Clicked launch merchant app button.");
                mCallback.launchCustApp();
                break;

            case R.id.btn_acc_limited:
                LogMy.d(TAG,"Clicked change acc status button.");
                // show disable dialog
                dialog = DisableCustDialog.getInstance(true);
                dialog.show(getFragmentManager(), DIALOG_DISABLE_CUST);
                break;
        }
    }

    private EditText mName;
    private EditText mInternalId;
    private EditText mInputMobileNum;
    private EditText mFirstLogin;
    private EditText mRegisteredOn;
    private EditText mExpiringOn;

    private EditText mInputStatus;
    private EditText mInputStatusDate;
    private EditText mInputReason;
    //private EditText mInputRemarks;

    private EditText mInputQrCard;
    private EditText mInputCardStatus;
    private EditText mCardStatusDate;
    private EditText mCardStatusReason;

    private AppCompatButton mAccDisable;
    private AppCompatButton mAccLimited;
    private AppCompatButton mCardDisable;
    private AppCompatButton mLaunchApp;


    private void bindUiResources(View v) {
        mName = (EditText) v.findViewById(R.id.input_cust_name);
        mInternalId = (EditText) v.findViewById(R.id.input_cust_id);

        mInputMobileNum = (EditText) v.findViewById(R.id.input_customer_mobile);
        mFirstLogin = (EditText) v.findViewById(R.id.input_first_login);
        mRegisteredOn = (EditText) v.findViewById(R.id.input_registered_on);
        mExpiringOn = (EditText) v.findViewById(R.id.input_expiring_on);

        mInputStatus = (EditText) v.findViewById(R.id.input_status);
        mInputStatusDate = (EditText) v.findViewById(R.id.input_status_date);
        mInputReason = (EditText) v.findViewById(R.id.input_status_reason);
        //mInputRemarks = (EditText) v.findViewById(R.id.input_status_remarks);

        mInputQrCard = (EditText) v.findViewById(R.id.input_card_id);
        mInputCardStatus = (EditText) v.findViewById(R.id.input_card_status);
        mCardStatusDate = (EditText) v.findViewById(R.id.input_card_status_date);
        mCardStatusReason = (EditText) v.findViewById(R.id.input_card_status_reason);

        mAccDisable = (AppCompatButton) v.findViewById(R.id.btn_acc_status);
        mAccLimited = (AppCompatButton) v.findViewById(R.id.btn_acc_limited);
        mCardDisable = (AppCompatButton) v.findViewById(R.id.btn_disable_card);
        mLaunchApp = (AppCompatButton) v.findViewById(R.id.btn_launch_app);
    }

}
