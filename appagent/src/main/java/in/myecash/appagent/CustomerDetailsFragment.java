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
import in.myecash.common.database.Customers;

/**
 * Created by adgangwa on 29-11-2016.
 */
public class CustomerDetailsFragment extends Fragment
        implements View.OnClickListener {
    private static final String TAG = "CustomerDetailsFragment";

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
            mAccStatus.setVisibility(View.GONE);
            mLaunchApp.setVisibility(View.GONE);

        } else if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_CC) {
            mAccStatus.setOnClickListener(this);
            mLaunchApp.setOnClickListener(this);
        }

        return v;
    }

    private void initDialogView() {
        Customers customer = mCallback.getRetainedFragment().mCurrCustomer;

        String name = customer.getFirstName()+" "+customer.getLastName();
        mName.setText(name);
        mInternalId.setText(customer.getPrivate_id());
        mInputMobileNum.setText(customer.getMobile_num());
        mFirstLogin.setText(customer.getFirst_login_ok().toString());
        mRegisteredOn.setText(mSdfDateOnly.format(customer.getCreated()));
        mExpiringOn.setText(mSdfDateOnly.format(AppCommonUtil.getExpiryDate(customer)));

        int status = customer.getAdmin_status();
        mInputStatus.setText(DbConstants.userStatusDesc[status]);
        mInputStatusDate.setText(mSdfDateWithTime.format(customer.getStatus_update_time()));
        mInputReason.setText(customer.getStatus_reason());

        mInputQrCard.setText(CommonUtils.getPartialVisibleStr(customer.getMembership_card().getCard_id()));
        mInputCardStatus.setText(DbConstants.cardStatusDescriptions[customer.getMembership_card().getStatus()]);
        if(customer.getMembership_card().getStatus() != DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED) {
            mInputCardStatus.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
        }
        mCardStatusDate.setText(mSdfDateWithTime.format(customer.getMembership_card().getStatus_update_time()));

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
                DisableCustDialog dialog = new DisableCustDialog();
                dialog.show(getFragmentManager(), DIALOG_DISABLE_CUST);
                break;

            case R.id.btn_launch_app:
                LogMy.d(TAG,"Clicked launch merchant app button.");
                mCallback.launchCustApp();
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

    private AppCompatButton mAccStatus;
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

        mAccStatus = (AppCompatButton) v.findViewById(R.id.btn_acc_status);
        mLaunchApp = (AppCompatButton) v.findViewById(R.id.btn_launch_app);
    }

}
