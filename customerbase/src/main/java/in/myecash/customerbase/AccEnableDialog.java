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

import in.myecash.appbase.BaseDialog;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.OnSingleClickListener;
import in.myecash.appbase.utilities.ValidationHelper;
import in.myecash.common.constants.ErrorCodes;

/**
 * Created by adgangwa on 25-11-2016.
 */
public class AccEnableDialog extends BaseDialog {
    private static final String TAG = "CustApp-AccEnableDialog";

    private static final String ARG_CARD_NUM = "ArgCardNum";
    private static final String ARG_PIN = "ArgPin";

    public interface AccEnableDialogIf {
        void enableAccOk(String otp, String cardNum, String pin);
    }

    private AccEnableDialogIf mCallback;

    public static AccEnableDialog newInstance(String cardNum, String pin) {
        LogMy.d(TAG, "Creating new AccEnableDialog instance: "+cardNum+", "+pin);
        Bundle args = new Bundle();
        if(cardNum!=null) {
            args.putString(ARG_CARD_NUM, cardNum);
        }
        if(pin!=null) {
            args.putString(ARG_PIN, pin);
        }

        AccEnableDialog fragment = new AccEnableDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (AccEnableDialogIf) getActivity();

            //MyRetainedFragment retFragment = mCallback.getRetainedFragment();

            // If available from last time, fill the same
            /*
            if(retFragment.mAccEnableCardNum!=null && !retFragment.mAccEnableCardNum.isEmpty()) {
                inputCardNum.setText(retFragment.mAccEnableCardNum);
            }
            if(retFragment.mAccEnablePin!=null && !retFragment.mAccEnablePin.isEmpty()) {
                inputPin.setText(retFragment.mAccEnablePin);
            }
            */

            /*if( (retFragment.mAccEnableCardNum!=null && !retFragment.mAccEnableCardNum.isEmpty()) ||
                    (retFragment.mAccEnablePin!=null && !retFragment.mAccEnablePin.isEmpty()) ) {
                // first run, otp not generated yet
                // disable OTP and ask for parameters
                labelInfo2.setVisibility(View.VISIBLE);
                labelInfo3.setVisibility(View.GONE);
                layoutOtp.setAlpha(0.5f);
                inputOtp.setEnabled(false);

            } else {
                // second run, OTP generated
                // disable and show parameter values and ask for otp

                labelInfo2.setVisibility(View.GONE);
                labelInfo3.setVisibility(View.VISIBLE);
                layoutOtp.setAlpha(1.0f);
                inputOtp.setEnabled(true);

                boolean cardNumVisible = false;
                if(retFragment.mAccEnableCardNum!=null && !retFragment.mAccEnableCardNum.isEmpty()) {
                    layoutCardNum.setVisibility(View.VISIBLE);
                    inputCardNum.setText(retFragment.mAccEnableCardNum);
                    cardNumVisible = true;
                } else {
                    layoutCardNum.setVisibility(View.GONE);
                }

                labelOr.setVisibility(View.GONE);
                if(retFragment.mAccEnablePin!=null && !retFragment.mAccEnablePin.isEmpty()) {
                    layoutPin.setVisibility(View.VISIBLE);
                    inputPin.setText(retFragment.mAccEnablePin);

                    // show OR label - if we are showing both cardNumk and PIN
                    // we have to - if user eneterd both for OTP generation, even if not required
                    if(cardNumVisible) {
                        labelOr.setVisibility(View.VISIBLE);
                    }
                } else {
                    layoutPin.setVisibility(View.GONE);
                }
            }*/

        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement AccEnableDialogIf");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateDialog");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_acc_enable, null);
        initUiResources(v);

        // get arguments
        String cardNum = getArguments().getString(ARG_CARD_NUM,null);
        String pin = getArguments().getString(ARG_PIN,null);

        // set values if available
        if(cardNum!=null && !cardNum.isEmpty()) {
            inputCardNum.setText(cardNum);
        }
        if(pin!=null && !pin.isEmpty()) {
            inputPin.setText(pin);
        }

        // return new dialog
        final android.support.v7.app.AlertDialog alertDialog =  new android.support.v7.app.AlertDialog.Builder(getActivity()).setView(v)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, this)
                .create();
        alertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {

                Button b = alertDialog.getButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new OnSingleClickListener() {
                    @Override
                    public void onSingleClick(View v) {
                        AppCommonUtil.hideKeyboard(getDialog());

                        if (validate()) {
                            // OTP is only visible in second run
                            /*String otp = null;
                            if (inputOtp.getVisibility() == View.VISIBLE) {
                                otp = inputOtp.getText().toString();
                                mCallback.enableAccOtp(otp);

                            } else {
                                mCallback.enableAccOk(inputCardNum.getText().toString(), inputPin.getText().toString());
                            }*/

                            // here means - atleast one of OTP, Card# or PIN is available
                            mCallback.enableAccOk(inputOtp.getText().toString(),
                                    inputCardNum.getText().toString(),
                                    inputPin.getText().toString());

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
    public void handleBtnClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                //Do nothing here because we override this button in OnShowListener to change the close behaviour.
                //However, we still need this because on older versions of Android unless we
                //pass a handler the button doesn't get instantiated
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                AppCommonUtil.hideKeyboard(getDialog());
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

    /*@Override
    public void onClick(DialogInterface dialog, int which) {
        //Do nothing here because we override this button in OnShowListener to change the close behaviour.
        //However, we still need this because on older versions of Android unless we
        //pass a handler the button doesn't get instantiated
    }*/

    private EditText labelInfo1;
    private EditText labelInfo2;
    private EditText labelInfo3;

    private View layoutCardNum;
    private EditText inputCardNum;

    private View labelOr;

    private View layoutPin;
    private EditText inputPin;

    private View layoutOtp;
    private EditText inputOtp;

    private void initUiResources(View view) {
        labelInfo1 = (EditText) view.findViewById(R.id.label_info1);
        labelInfo2 = (EditText) view.findViewById(R.id.label_info2);
        labelInfo3 = (EditText) view.findViewById(R.id.label_info3);

        layoutCardNum = view.findViewById(R.id.layout_cardNum);
        inputCardNum = (EditText) view.findViewById(R.id.input_card_num);

        labelOr = view.findViewById(R.id.label_or);

        layoutPin = view.findViewById(R.id.layout_pin);
        inputPin = (EditText) view.findViewById(R.id.input_pin);

        layoutOtp = view.findViewById(R.id.layout_otp);
        inputOtp = (EditText) view.findViewById(R.id.input_otp);
    }

    private boolean validate() {
        boolean retValue = true;
        int errorCode;

        boolean otpEntered = false;
        if( !inputOtp.getText().toString().isEmpty()) {
            otpEntered = true;
            // OTP is entered
            errorCode = ValidationHelper.validateOtp(inputOtp.getText().toString());
            if(errorCode != ErrorCodes.NO_ERROR) {
                inputOtp.setError(AppCommonUtil.getErrorDesc(errorCode));
                retValue = false;
            }
        }

        boolean cardEntered = false;
        if( !inputCardNum.getText().toString().isEmpty()) {
            cardEntered = true;
            errorCode = ValidationHelper.validateCardNum(inputCardNum.getText().toString());
            if(errorCode != ErrorCodes.NO_ERROR) {
                inputCardNum.setError(AppCommonUtil.getErrorDesc(errorCode));
                retValue = false;
            }
        }

        boolean pinEntered = false;
        if( !inputPin.getText().toString().isEmpty()) {
            pinEntered = true;
            errorCode = ValidationHelper.validatePin(inputPin.getText().toString());
            if(errorCode != ErrorCodes.NO_ERROR) {
                inputPin.setError(AppCommonUtil.getErrorDesc(errorCode));
                retValue = false;
            }
        }

        if(!otpEntered && !cardEntered && !pinEntered) {
            // if otp not entered - atleast one of Card# or PIN shud be provided
            AppCommonUtil.toast(getActivity(), "No OTP. Provide Card# or PIN to generate");
            retValue = false;
        }

        /*boolean cardNumEmpty = true;
        if( !inputCardNum.getText().toString().isEmpty()) {
            cardNumEmpty = false;
            errorCode = ValidationHelper.validateCardId(inputCardNum.getText().toString());
            if(errorCode != ErrorCodes.NO_ERROR) {
                inputCardNum.setError(AppCommonUtil.getErrorDesc(errorCode));
                retValue = false;
            }
        }

        if( !inputPin.getText().toString().isEmpty()) {
            errorCode = ValidationHelper.validatePin(inputPin.getText().toString());
            if(errorCode != ErrorCodes.NO_ERROR) {
                inputPin.setError(AppCommonUtil.getErrorDesc(errorCode));
                retValue = false;
            }
        } else if(cardNumEmpty) {
            // none of cardNum or PIN provided
            AppCommonUtil.toast(getActivity(), "Provide either Card# or PIN");
            retValue = false;
        }

        if( inputOtp.isEnabled()) {
            errorCode = ValidationHelper.validateOtp(inputOtp.getText().toString());
            if(errorCode != ErrorCodes.NO_ERROR) {
                inputOtp.setError(AppCommonUtil.getErrorDesc(errorCode));
                retValue = false;
            }
        }*/

        return retValue;
    }

}
