package in.myecash.customerbase;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import java.text.SimpleDateFormat;

import in.myecash.appbase.BaseDialog;
import in.myecash.common.CommonUtils;
import in.myecash.common.DateUtil;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.MyGlobalSettings;
import in.myecash.common.database.Customers;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.customerbase.entities.CustomerUser;

/**
 * Created by adgangwa on 21-05-2016.
 */
public class CustDetailsDialogCustApp extends BaseDialog {
    private static final String TAG = "CustApp-CustomerDetailsDialog";

    private SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
    private SimpleDateFormat mSdfDateOnly = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_DISPLAY, CommonConstants.DATE_LOCALE);

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_customer_details_custapp, null);

        bindUiResources(v);
        initDialogView();

        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setView(v)
                .setPositiveButton(android.R.string.ok, this)
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(CustDetailsDialogCustApp.this, (AlertDialog) dialog);
            }
        });

        return dialog;
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
                break;
        }
    }

    @Override
    public boolean handleTouchUp(View v) {
        return false;
    }

    private void initDialogView() {
        Customers cust = CustomerUser.getInstance().getCustomer();

        String name = cust.getFirstName()+" "+cust.getLastName();
        mName.setText(name);
        mInputMobileNum.setText(cust.getMobile_num());
        mCreatedOn.setText(mSdfDateOnly.format(cust.getCreated()));
        mExpiringOn.setText(mSdfDateOnly.format(AppCommonUtil.getExpiryDate(cust)));

        mInputQrCard.setText(CommonUtils.getPartialVisibleStr(cust.getMembership_card().getCardNum()));
        mInputCardStatus.setText(DbConstants.cardStatusDesc[cust.getMembership_card().getStatus()]);
        if(cust.getMembership_card().getStatus() != DbConstants.CUSTOMER_CARD_STATUS_ACTIVE) {
            mInputCardStatus.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
        }

        int status = cust.getAdmin_status();
        mInputStatus.setText(DbConstants.userStatusDesc[status]);
        if(status != DbConstants.USER_STATUS_ACTIVE) {
            mLayoutStatusDetails.setVisibility(View.VISIBLE);
            mInputStatus.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
            mInputStatusDate.setText(mSdfDateWithTime.format(cust.getStatus_update_time()));
            mInputReason.setText(cust.getStatus_reason());

            if(status==DbConstants.USER_STATUS_LOCKED) {
                mInputStatusDetails.setVisibility(View.VISIBLE);
                //String detail = "Will be unlocked automatically after "+MyGlobalSettings.getAccBlockHrs(DbConstants.USER_TYPE_CUSTOMER)+" hours.";
                //mInputStatusDetails.setText(detail);
                DateUtil time = new DateUtil(cust.getStatus_update_time());
                time.addMinutes(MyGlobalSettings.getAccBlockMins(DbConstants.USER_TYPE_CUSTOMER));
                String detail = "Will be Unlocked at "+mSdfDateWithTime.format(time.getTime());
                mInputStatusDetails.setText(detail);

            } else if(status==DbConstants.USER_STATUS_LIMITED_CREDIT_ONLY) {
                mInputStatusDetails.setVisibility(View.VISIBLE);
                //String detail = "Will be Active automatically after "+MyGlobalSettings.getCustAccLimitModeHrs()+" hours.";
                //mInputStatusDetails.setText(detail);
                DateUtil time = new DateUtil(cust.getStatus_update_time());
                time.addMinutes(MyGlobalSettings.getCustAccLimitModeMins());
                String detail = "Will be Active again at "+mSdfDateWithTime.format(time.getTime());
                mInputStatusDetails.setText(detail);

            } else {
                mInputStatusDetails.setVisibility(View.GONE);
            }
        } else {
            mLayoutStatusDetails.setVisibility(View.GONE);
        }
    }

    private EditText mName;
    private EditText mInputMobileNum;
    private EditText mCreatedOn;
    private EditText mExpiringOn;

    private EditText mInputQrCard;
    private EditText mInputCardStatus;

    private EditText mInputStatus;
    private View mLayoutStatusDetails;
    private EditText mInputReason;
    private EditText mInputStatusDate;
    private EditText mInputStatusDetails;

    private void bindUiResources(View v) {

        mInputMobileNum = (EditText) v.findViewById(R.id.input_customer_mobile);
        mName = (EditText) v.findViewById(R.id.input_cust_name);
        mCreatedOn = (EditText) v.findViewById(R.id.input_cust_created_on);
        mExpiringOn = (EditText) v.findViewById(R.id.input_expiring_on);

        mInputStatus = (EditText) v.findViewById(R.id.input_status);
        mLayoutStatusDetails = v.findViewById(R.id.layout_status_details);
        mInputReason = (EditText) v.findViewById(R.id.input_reason);
        mInputStatusDate = (EditText) v.findViewById(R.id.input_status_date);
        mInputStatusDetails = (EditText) v.findViewById(R.id.input_activation);

        mInputQrCard = (EditText) v.findViewById(R.id.input_qr_card);
        mInputCardStatus = (EditText) v.findViewById(R.id.input_card_status);
    }
}
