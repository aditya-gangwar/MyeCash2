package in.myecash.customerbase;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import in.myecash.common.MyGlobalSettings;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.ValidationHelper;
import in.myecash.customerbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 07-06-2016.
 */
public class MobileChangeDialog extends DialogFragment implements DialogInterface.OnClickListener {
    private static final String TAG = "MobileChangeDialog";

    public interface MobileChangeDialogIf {
        void changeMobileNumOk(String newMobile, String cardNum);
        void changeMobileNumOtp(String otp);
        //void changeMobileNumReset(boolean showMobilePref);
        //MerchantOps getMobileChangeMerchantOp();
        MyRetainedFragment getRetainedFragment();
    }

    private MobileChangeDialogIf mCallback;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (MobileChangeDialogIf) getActivity();

            // set values and views
            mInfoEnd.setText(String.format(getString(R.string.cust_mobile_change_info), MyGlobalSettings.getCustAccLimitModeHrs().toString()));

            String newMobile = mCallback.getRetainedFragment().mNewMobileNum;
            if(newMobile==null) {
                // first run, otp not generated yet
                // disable OTP and ask for parameters
                labelNewOtp.setText("OTP will be sent on the New Mobile Number for verification");
                inputNewOtp.setVisibility(View.GONE);
            } else {
                // second run, OTP generated
                // disable and show parameter values and ask for otp
                labelInfo1.setText("Enter received OTP and submit the request again");
                mImageMobile.setAlpha(0.5f);
                mImageCardNum.setAlpha(0.5f);

                labelNewMobile.setEnabled(false);
                inputNewMobile.setText(newMobile);
                inputNewMobile.setEnabled(false);

                labelNewMobile2.setEnabled(false);
                inputNewMobile2.setText(newMobile);
                inputNewMobile2.setEnabled(false);

                labelCardNum.setEnabled(false);
                inputCardNum.setText(mCallback.getRetainedFragment().mCardMobileChange);
                inputCardNum.setEnabled(false);
            }

        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement MobileChangeDialogIf");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateDialog");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_mobile_change_cust, null);
        initUiResources(v);

        // return new dialog
        final android.support.v7.app.AlertDialog alertDialog =  new android.support.v7.app.AlertDialog.Builder(getActivity()).setView(v)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppCommonUtil.hideKeyboard(getDialog());
                        dialog.dismiss();
                    }
                })
                .create();
        alertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {

                Button b = alertDialog.getButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AppCommonUtil.hideKeyboard(getDialog());

                        if (validate()) {
                            // OTP is only visible in second run
                            String otp = null;
                            if (inputNewOtp.getVisibility() == View.VISIBLE) {
                                otp = inputNewOtp.getText().toString();
                                mCallback.changeMobileNumOtp(otp);

                            } else {
                                if(!inputNewMobile2.getText().toString().equals(inputNewMobile.getText().toString())) {
                                    inputNewMobile2.setError("Value do not match with above");
                                    return;
                                }
                                // check old and new numbers are not same
                                if(mCallback.getRetainedFragment().mCustomerUser.getCustomer().getMobile_num().equals(inputNewMobile.getText().toString())) {
                                    inputNewMobile.setError("Same as current registered number.");
                                    return;
                                }

                                mCallback.changeMobileNumOk(inputNewMobile.getText().toString(), inputCardNum.getText().toString());
                            }
                            getDialog().dismiss();
                        }
                    }
                });
            }
        });

        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        return alertDialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        //Do nothing here because we override this button in OnShowListener to change the close behaviour.
        //However, we still need this because on older versions of Android unless we
        //pass a handler the button doesn't get instantiated
    }

    private EditText labelInfo1;
    private EditText labelNewMobile;
    private EditText labelNewMobile2;
    private EditText labelCardNum;
    private EditText labelNewOtp;

    private EditText inputNewMobile;
    private EditText inputNewMobile2;
    private EditText inputCardNum;
    private EditText inputNewOtp;

    private EditText mInfoEnd;

    private View mImageMobile;
    private View mImageCardNum;

    private void initUiResources(View view) {
        labelInfo1 = (EditText) view.findViewById(R.id.label_info1);
        labelNewMobile = (EditText) view.findViewById(R.id.label_new_mobile);
        labelNewMobile2 = (EditText) view.findViewById(R.id.label_new_mobile2);
        labelCardNum = (EditText) view.findViewById(R.id.label_card_num);
        labelNewOtp = (EditText) view.findViewById(R.id.label_otp);

        inputNewMobile = (EditText) view.findViewById(R.id.input_new_mobile);
        inputNewMobile2 = (EditText) view.findViewById(R.id.input_new_mobile2);
        inputCardNum = (EditText) view.findViewById(R.id.input_card_num);
        inputNewOtp = (EditText) view.findViewById(R.id.input_otp);

        mInfoEnd = (EditText) view.findViewById(R.id.label_info);
        mImageMobile = view.findViewById(R.id.image_mobile);
        mImageCardNum = view.findViewById(R.id.image_cardNum);
    }

    private boolean validate() {
        boolean retValue = true;
        int errorCode;

        if( inputNewMobile.isEnabled()) {
            errorCode = ValidationHelper.validateMobileNo(inputNewMobile.getText().toString());
            if(errorCode != ErrorCodes.NO_ERROR) {
                inputNewMobile.setError(AppCommonUtil.getErrorDesc(errorCode));
                retValue = false;
            }
        }

        if(inputNewOtp.getVisibility() == View.VISIBLE) {
            errorCode = ValidationHelper.validateOtp(inputNewOtp.getText().toString());
            if(errorCode != ErrorCodes.NO_ERROR) {
                inputNewOtp.setError(AppCommonUtil.getErrorDesc(errorCode));
                retValue = false;
            }
        }

        if( inputCardNum.isEnabled()) {
            errorCode = ValidationHelper.validateMemberCard(inputCardNum.getText().toString());
            if(errorCode != ErrorCodes.NO_ERROR) {
                inputCardNum.setError(AppCommonUtil.getErrorDesc(errorCode));
                retValue = false;
            }
        }

        return retValue;
    }

}
