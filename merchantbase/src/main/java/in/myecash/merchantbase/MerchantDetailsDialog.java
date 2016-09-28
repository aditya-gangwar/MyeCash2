package in.myecash.merchantbase;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.appbase.entities.MyGlobalSettings;
import in.myecash.common.database.Merchants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.merchantbase.entities.MerchantUser;

import java.text.SimpleDateFormat;

/**
 * Created by adgangwa on 30-07-2016.
 */
public class MerchantDetailsDialog extends DialogFragment {
    private static final String TAG = "MerchantDetailsDialog";

    private final SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
    private final SimpleDateFormat mSdfOnlyDate = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_DISPLAY, CommonConstants.DATE_LOCALE);

    /*
    private MerchantDetailsDialogIf mCallback;
    public interface MerchantDetailsDialogIf {
        MyRetainedFragment getRetainedFragment();
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
    }*/

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_merchant_details, null);

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
                AppCommonUtil.setDialogTextSize(MerchantDetailsDialog.this, (AlertDialog) dialog);
            }
        });

        return dialog;
    }

    private void initDialogView() {
        MerchantUser merchantUser = MerchantUser.getInstance();
        Merchants merchant = MerchantUser.getInstance().getMerchant();

        if(merchantUser.getDisplayImage()!=null) {
            mDisplayImage.setImageBitmap(merchantUser.getDisplayImage());
        }

        mStoreName.setText(merchant.getName());
        mStoreCategory.setText(merchant.getBuss_category().getCategory_name());
        mMerchantId.setText(merchant.getAuto_id());
        mRegisteredOn.setText(mSdfOnlyDate.format(merchant.getCreated()));
        mExpiringOn.setText(mSdfOnlyDate.format(AppCommonUtil.getExpiryDate(merchant)));

        int status = merchant.getAdmin_status();
        mInputStatus.setText(DbConstants.userStatusDesc[status]);
        if(status != DbConstants.USER_STATUS_ACTIVE) {
            mLayoutStatusDetails.setVisibility(View.VISIBLE);
            mInputStatus.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
            mInputStatusDate.setText(mSdfDateWithTime.format(merchant.getStatus_update_time()));
            mInputReason.setText(merchant.getStatus_reason());

            if(status==DbConstants.USER_STATUS_READY_TO_REMOVE ) {
                mLayoutActivation.setVisibility(View.VISIBLE);
                mInputActivation.setText(AppCommonUtil.getMchntExpiryMsg(merchant));

            } else if(status==DbConstants.USER_STATUS_LOCKED) {
                mLayoutActivation.setVisibility(View.VISIBLE);
                String detail = "Will be auto unlocked after "+MyGlobalSettings.getMchntAccBlockHrs()+" hours from given time.";
                mInputActivation.setText(detail);

            } else {
                mLayoutActivation.setVisibility(View.GONE);
            }
        } else {
            mLayoutStatusDetails.setVisibility(View.GONE);
        }

        mInputMobileNum.setText(merchant.getMobile_num());
        mInputEmail.setText(merchant.getEmail());

        mAddress.setText(merchant.getAddress().getLine_1());
        mCity.setText(merchant.getAddress().getCity());
        mState.setText(merchant.getAddress().getState());
    }

    private ImageView mDisplayImage;
    private EditText mStoreName;
    private EditText mStoreCategory;

    private EditText mMerchantId;
    private EditText mRegisteredOn;
    private EditText mExpiringOn;

    private EditText mInputStatus;
    private View mLayoutStatusDetails;
    private EditText mInputReason;
    private EditText mInputStatusDate;
    private View mLayoutActivation;
    private EditText mInputActivation;

    private EditText mInputMobileNum;
    private EditText mInputEmail;

    private EditText mAddress;
    private EditText mCity;
    private EditText mState;


    private void bindUiResources(View v) {

        mDisplayImage = (ImageView) v.findViewById(R.id.display_image);
        mStoreName = (EditText) v.findViewById(R.id.input_store_name);
        mStoreCategory = (EditText) v.findViewById(R.id.input_store_category);

        mMerchantId = (EditText) v.findViewById(R.id.input_merchant_id);
        mRegisteredOn = (EditText) v.findViewById(R.id.input_registered_on);
        mExpiringOn = (EditText) v.findViewById(R.id.input_expiring_on);

        mInputStatus = (EditText) v.findViewById(R.id.input_status);
        mLayoutStatusDetails = v.findViewById(R.id.layout_status_details);
        mInputReason = (EditText) v.findViewById(R.id.input_reason);
        mInputStatusDate = (EditText) v.findViewById(R.id.input_status_date);
        mLayoutActivation = v.findViewById(R.id.layout_activation);
        mInputActivation = (EditText) v.findViewById(R.id.input_activation);

        mInputMobileNum = (EditText) v.findViewById(R.id.input_merchant_mobile);
        mInputEmail = (EditText) v.findViewById(R.id.input_merchant_email);

        mAddress = (EditText) v.findViewById(R.id.input_address);
        mCity = (EditText) v.findViewById(R.id.input_city);
        mState = (EditText) v.findViewById(R.id.input_state);
    }
}

