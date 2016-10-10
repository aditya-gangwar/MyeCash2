package in.myecash.customerbase;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import java.text.SimpleDateFormat;

import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.MyGlobalSettings;
import in.myecash.common.database.Customers;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.customerbase.entities.CustomerUser;

/**
 * Created by adgangwa on 21-05-2016.
 */
public class CustomerDetailsDialog extends DialogFragment  {
    private static final String TAG = "CustomerDetailsDialog";
    private static final String ARG_CB_POSITION = "cbPosition";

    private SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
    private SimpleDateFormat mSdfDateOnly = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_DISPLAY, CommonConstants.DATE_LOCALE);

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_customer_details, null);

        bindUiResources(v);
        initDialogView();

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
                AppCommonUtil.setDialogTextSize(CustomerDetailsDialog.this, (AlertDialog) dialog);
            }
        });

        return dialog;
    }

    private void initDialogView() {
        Customers cust = CustomerUser.getInstance().getCustomer();

        //mName.setText(cust.getName());
        mInputMobileNum.setText(AppCommonUtil.getPartialVisibleStr(cust.getMobile_num()));
        mCreatedOn.setText(mSdfDateOnly.format(cust.getCreated()));
        mExpiringOn.setText(mSdfDateOnly.format(AppCommonUtil.getExpiryDate(cust)));

        mInputQrCard.setText(cust.getCardId());
        mInputCardStatus.setText(DbConstants.cardStatusDescriptions[cust.getMembership_card().getStatus()]);
        if(cust.getMembership_card().getStatus() != DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED) {
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
                String detail = "Will be unlocked automatically after "+MyGlobalSettings.getAccBlockHrs(DbConstants.USER_TYPE_CUSTOMER)+" hours.";
                mInputStatusDetails.setText(detail);
            } else {
                mInputStatusDetails.setVisibility(View.GONE);
            }
        } else {
            mLayoutStatusDetails.setVisibility(View.GONE);
        }
    }

    //private EditText mName;
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
        //mName = (EditText) v.findViewById(R.id.input_cust_name);
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
