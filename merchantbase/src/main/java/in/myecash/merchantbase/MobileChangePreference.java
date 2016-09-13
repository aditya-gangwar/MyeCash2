package in.myecash.merchantbase;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.utilities.ValidationHelper;
import in.myecash.merchantbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 07-06-2016.
 */
public class MobileChangePreference extends DialogPreference
        implements View.OnClickListener {
    private static final String TAG = "MobileChangePreference";

    public interface MobileChangePreferenceIf {
        void changeMobileNumOk(String oldMobile, String newMobile);
        void changeMobileNumOtp(String otp);
        void changeMobileNumReset(boolean showMobilePref);
        //MerchantOps getMobileChangeMerchantOp();
        MyRetainedFragment getRetainedFragment();
    }

    private MobileChangePreferenceIf mCallback;

    EditText labelCurrMobile;
    EditText labelNewMobile;
    EditText labelNewOtp;

    EditText inputCurrMobile;
    EditText inputNewMobile;
    EditText inputNewOtp;

    public MobileChangePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        CashbackActivity activity = (CashbackActivity)context;
        mCallback = (MobileChangePreferenceIf)activity;
        setDialogLayoutResource(R.layout.dialog_mobile_change);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogIcon(null);
    }

    /*
    public void show() {
        showDialog(null);
    }*/

    @Override
    protected void onBindDialogView (View view) {
        labelCurrMobile = (EditText) view.findViewById(R.id.label_current_mobile);
        labelNewMobile = (EditText) view.findViewById(R.id.label_new_mobile);
        labelNewOtp = (EditText) view.findViewById(R.id.label_otp);

        inputCurrMobile = (EditText) view.findViewById(R.id.input_current_mobile);
        inputNewMobile = (EditText) view.findViewById(R.id.input_new_mobile);
        inputNewOtp = (EditText) view.findViewById(R.id.input_otp);

        super.onBindDialogView(view);
    }

    @Override
    protected void showDialog(Bundle bundle) {
        super.showDialog(bundle);
        getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getDialog().setCanceledOnTouchOutside(false);

        //MerchantOps op = mCallback.getMobileChangeMerchantOp();
        String oldMobile = mCallback.getRetainedFragment().mInputCurrMobile;
        String newMobile = mCallback.getRetainedFragment().mNewMobileNum;

        //if(op == null || !op.getOp_status().equals(DbConstants.MERCHANT_OP_STATUS_OTP_GENERATED)) {
        if(oldMobile==null || newMobile==null) {
            // first run, otp not generated yet
            // disable OTP and ask for parameters
            labelNewOtp.setText("OTP will be sent on the New mobile number for verification.");
            inputNewOtp.setVisibility(View.GONE);
        } else {
            // second run, OTP generated
            // disable and show parameter values and ask for otp
            labelNewMobile.setEnabled(false);
            //inputNewMobile.setText(op.getExtra_op_params());
            inputNewMobile.setText(newMobile);
            inputNewMobile.setEnabled(false);

            labelCurrMobile.setEnabled(false);
            //inputCurrMobile.setText(op.getMobile_num());
            inputCurrMobile.setText(oldMobile);
            inputCurrMobile.setEnabled(false);
        }

        /*
        ((AlertDialog) getDialog()).setButton(DialogInterface.BUTTON_NEUTRAL, "Reset", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCallback.changeMobileNumReset();
                dialog.dismiss();
            }
        });*/

        Button pos = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
        pos.setOnClickListener(this);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder)
    {
        super.onPrepareDialogBuilder(builder);
        builder.setNeutralButton("Reset", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCallback.changeMobileNumReset(true);
                dialog.dismiss();
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (validate()) {
            // OTP is only visible in second run
            String otp = null;
            if (inputNewOtp.getVisibility() == View.VISIBLE) {
                otp = inputNewOtp.getText().toString();
                mCallback.changeMobileNumOtp(otp);

            } else {
                // check old and new numbers are not same
                if(inputCurrMobile.getText().toString().equals(inputNewMobile.getText().toString())) {
                    inputNewMobile.setError("Same as given current number.");
                    return;
                } else {
                    mCallback.changeMobileNumOk(
                            inputCurrMobile.getText().toString(),
                            inputNewMobile.getText().toString());
                }
            }
            getDialog().dismiss();
        }
    }

    private boolean validate() {
        boolean retValue = true;
        int errorCode;

        if(inputCurrMobile.isEnabled()) {
            errorCode = ValidationHelper.validateMobileNo(inputCurrMobile.getText().toString());
            if(errorCode != ErrorCodes.NO_ERROR) {
                inputCurrMobile.setError(ErrorCodes.appErrorDesc.get(errorCode));
                retValue = false;
            }
        }

        if( inputNewMobile.isEnabled()) {
            errorCode = ValidationHelper.validateMobileNo(inputNewMobile.getText().toString());
            if(errorCode != ErrorCodes.NO_ERROR) {
                inputNewMobile.setError(ErrorCodes.appErrorDesc.get(errorCode));
                retValue = false;
            }
        }

        if(inputNewOtp.getVisibility() == View.VISIBLE) {
            errorCode = ValidationHelper.validatePinOtp(inputNewOtp.getText().toString());
            if(errorCode != ErrorCodes.NO_ERROR) {
                inputNewOtp.setError(ErrorCodes.appErrorDesc.get(errorCode));
                retValue = false;
            }
        }

        return retValue;
    }

}
