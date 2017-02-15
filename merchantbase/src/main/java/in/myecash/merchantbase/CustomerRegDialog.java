package in.myecash.merchantbase;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import in.myecash.appbase.BaseDialog;
import in.myecash.appbase.barcodeReader.BarcodeCaptureActivity;
import in.myecash.appbase.utilities.OnSingleClickListener;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.ValidationHelper;

/**
 * Created by adgangwa on 13-04-2016.
 */
public class CustomerRegDialog extends BaseDialog {

    private static final String TAG = "MchntApp-CustomerRegDialog";
    public static final int RC_BARCODE_CAPTURE_REG_DIALOG = 9002;

    private static final String ARG_FIRST_NAME = "firstName";
    private static final String ARG_LAST_NAME = "lastName";
    private static final String ARG_MOBILE_NUM = "mobile_num";
    private static final String ARG_QRCODE = "qrcode";
    private static final String ARG_WRONG_OTP = "wrongOtp";

    private CustomerRegFragmentIf mCallback;
    private String scannedCardId;

    public interface CustomerRegFragmentIf {
        void onCustomerRegOk(String name, String mobileNum, String qrCode, String firstName, String lastName);
        void onCustomerRegReset();
        //void restartTxn();
    }

    public static CustomerRegDialog newInstance(String mobileNo, String cardId, String firstName, String lastName, boolean wrongOtp) {
        Bundle args = new Bundle();
        if(mobileNo != null) {
            args.putString(ARG_MOBILE_NUM, mobileNo);
        }
        if(cardId != null) {
            args.putString(ARG_QRCODE, cardId);
        }
        if(firstName != null) {
            args.putString(ARG_FIRST_NAME, firstName);
        }
        if(lastName != null) {
            args.putString(ARG_LAST_NAME, lastName);
        }
        args.putBoolean(ARG_WRONG_OTP, wrongOtp);

        CustomerRegDialog fragment = new CustomerRegDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (CustomerRegFragmentIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement CustomerRegFragmentIf");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String mobileNum = getArguments().getString(ARG_MOBILE_NUM, null);
        String cardId = getArguments().getString(ARG_QRCODE, null);
        String firstName = getArguments().getString(ARG_FIRST_NAME, null);
        String lastName = getArguments().getString(ARG_LAST_NAME, null);

        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_register_customer, null);

        bindUiResources(v);

        if(savedInstanceState!=null) {
            LogMy.d(TAG,"Restoring scannedCardId");
            scannedCardId = savedInstanceState.getString("scannedCardId");
        }

        // Any null means OTP not generated yet
        if(mobileNum==null || mobileNum.isEmpty() ||
                cardId == null || cardId.isEmpty()) {
            mInputOtp.setText("");
            mLayoutOtp.setVisibility(View.GONE);
            mLabelInfoOtp.setVisibility(View.GONE);

            mLabelInfoMobile.setVisibility(View.VISIBLE);
            mLabelInfoName.setVisibility(View.VISIBLE);

        } else {
            mLabelInfoMobile.setVisibility(View.GONE);
            mLabelInfoName.setVisibility(View.GONE);

            mLabelInfoOtp.setVisibility(View.VISIBLE);
            mLayoutOtp.setVisibility(View.VISIBLE);
            mInputOtp.requestFocus();

            boolean wrongOtpCase = getArguments().getBoolean(ARG_WRONG_OTP,false);
            if(wrongOtpCase) {
                mLabelInfoOtp.setText("Wrong OTP.  Please Try again");
                mLabelInfoOtp.setTypeface(null, Typeface.BOLD_ITALIC);
                mInputOtp.setError("Wrong OTP value");
            }
        }

        // When the dialog is opened from 'mobile number screen' (i.e. not from Menu)
        // Then it will receive 'mobile number' but not the card id

        // Set mobile num and make non-editable
        if(mobileNum!=null && !mobileNum.isEmpty()) {
            mInputMobileNum.setText(mobileNum);
            AppCommonUtil.makeEditTextOnlyView(mInputMobileNum);
            mInputMobileNum.clearFocus();
            mInputMobileNum.setEnabled(false);
            mLabelMobile.setEnabled(false);
            mImageMobile.setAlpha(0.5f);
        }

        // Set card Id and make non-editable
        if(cardId!=null && !cardId.isEmpty()) {
            scannedCardId = cardId;
            mInputQrCard.setText("OK");
            mInputQrCard.setTextColor(ContextCompat.getColor(getActivity(), R.color.green_positive));
            mInputQrCard.setError(null);
            mInputQrCard.setClickable(false);
            mInputQrCard.setEnabled(false);
            mLabelCard.setEnabled(false);
            mImageCard.setAlpha(0.5f);
        } else {
            mInputQrCard.setOnTouchListener(this);
        }

        // Set name and make non-editable
        if(firstName!=null && !firstName.isEmpty()) {
            mInputFirstName.setText(firstName);
            mInputFirstName.setClickable(false);
            mInputFirstName.setEnabled(false);
            mLabelFirstName.setEnabled(false);
            mLabelLastName.setEnabled(false);

            mInputLastName.setText(lastName);
            mInputLastName.setClickable(false);
            mInputLastName.setEnabled(false);
            mInputLastName.setEnabled(false);

            mImageName.setAlpha(0.5f);
        }

        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setView(v)
                //.setPositiveButton(android.R.string.ok, this)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setNeutralButton("Restart", this)
                .create();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(CustomerRegDialog.this, (AlertDialog) dialog);
            }
        });

        return dialog;
    }

    @Override
    public void handleBtnClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                //Do nothing here because we override this button later to change the close behaviour.
                //However, we still need this because on older versions of Android unless we
                //pass a handler the button doesn't get instantiated
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                dialog.dismiss();
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                mCallback.onCustomerRegReset();
                dialog.dismiss();
                break;
        }
    }

    @Override
    public boolean handleTouchUp(View v) {
        if (v.getId() == R.id.input_qr_card) {// launch barcode activity.
            Intent intent = new Intent(getActivity(), BarcodeCaptureActivity.class);
            intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
            intent.putExtra(BarcodeCaptureActivity.UseFlash, false);

            startActivityForResult(intent, RC_BARCODE_CAPTURE_REG_DIALOG);
        }
        return true;
    }

    /*
    @Override
    public void onClick(DialogInterface dialog, int which) {

        if(validate()) {
            mCallback.onCustomerRegOk(
                    mInputCustName.getText().toString(),
                    mInputMobileNum.getText().toString(),
                    mInputQrCard.getText().toString());
            dialog.dismiss();
        }
    }*/

    @Override
    public void onStart()
    {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point
        final AlertDialog d = (AlertDialog)getDialog();
        if(d != null)
        {
            Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new OnSingleClickListener() {
                @Override
                public void onSingleClick(View v) {
                    Boolean wantToCloseDialog = false;

                    if(validate()) {
                        mCallback.onCustomerRegOk(
                                mInputMobileNum.getText().toString(),
                                scannedCardId,
                                mInputOtp.getText().toString(),
                                mInputFirstName.getText().toString(),
                                mInputLastName.getText().toString());
                        wantToCloseDialog = true;
                    }

                    if (wantToCloseDialog)
                        d.dismiss();
                    //else dialog stays open. Make sure you have an obvious way to close the dialog especially if you set cancellable to false.
                }
            });
        }
    }

    private boolean validate() {
        boolean retValue = true;
        int errorCode;

        if(mInputMobileNum.isEnabled()) {
            errorCode = ValidationHelper.validateMobileNo(mInputMobileNum.getText().toString());
            if (errorCode != ErrorCodes.NO_ERROR) {
                mInputMobileNum.setError(AppCommonUtil.getErrorDesc(errorCode));
                retValue = false;
            }
        }

        if(mInputQrCard.isEnabled()) {
            errorCode = ValidationHelper.validateCardId(scannedCardId);
            if (errorCode != ErrorCodes.NO_ERROR) {
                mInputQrCard.setError(AppCommonUtil.getErrorDesc(errorCode));
                retValue = false;
            }
        }

        if(mInputOtp.isEnabled() &&
                mLayoutOtp.getVisibility()==View.VISIBLE) {
            errorCode = ValidationHelper.validateOtp(mInputOtp.getText().toString());
            if(errorCode != ErrorCodes.NO_ERROR) {
                mInputOtp.setError(AppCommonUtil.getErrorDesc(errorCode));
                retValue = false;
            }
        }

        if(mInputFirstName.isEnabled()) {
            errorCode = ValidationHelper.validateCustName(mInputFirstName.getText().toString());
            if (errorCode != ErrorCodes.NO_ERROR) {
                mInputFirstName.setError(AppCommonUtil.getErrorDesc(errorCode));
                retValue = false;
            }
        }

        if(mInputLastName.isEnabled()) {
            errorCode = ValidationHelper.validateCustName(mInputLastName.getText().toString());
            if (errorCode != ErrorCodes.NO_ERROR) {
                mInputLastName.setError(AppCommonUtil.getErrorDesc(errorCode));
                retValue = false;
            }
        }

        return retValue;
    }

    /*@Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP) {
            int vId = v.getId();
            LogMy.d(TAG, "In onClick: " + vId);

            if (vId == R.id.input_qr_card) {// launch barcode activity.
                Intent intent = new Intent(getActivity(), BarcodeCaptureActivity.class);
                intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
                intent.putExtra(BarcodeCaptureActivity.UseFlash, false);

                startActivityForResult(intent, RC_BARCODE_CAPTURE_REG_DIALOG);
            }
        }
        return true;
    }*/

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE_REG_DIALOG) {
            if (resultCode == ErrorCodes.NO_ERROR) {
                String qrCode = data.getStringExtra(BarcodeCaptureActivity.BarcodeObject);
                LogMy.d(TAG,"Read customer QR code: "+qrCode);
                setQrCode(qrCode);
            } else {
                LogMy.e(TAG,"Failed to read barcode");
            }
        }
    }

    /*
    @Override
    public void onCancel(DialogInterface dialog) {
        LogMy.d(TAG,"In onCancel");
        super.onCancel(dialog);
        mCallback.restartTxn();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        LogMy.d(TAG,"In onDismiss");
        super.onDismiss(dialog);
        mCallback.restartTxn();
    }*/

    private EditText mInputFirstName;
    private EditText mInputLastName;
    private EditText mInputMobileNum;
    private EditText mInputOtp;
    private EditText mInputQrCard;

    private EditText mLabelFirstName;
    private EditText mLabelLastName;
    private EditText mLabelMobile;
    private EditText mLabelCard;
    private EditText mLabelInfoOtp;
    private EditText mLabelInfoMobile;
    private EditText mLabelInfoName;

    private View mImageMobile;
    private View mImageCard;
    private View mImageName;

    private View mLayoutOtp;

    private void bindUiResources(View v) {

        mInputFirstName = (EditText) v.findViewById(R.id.input_firstName);
        mInputLastName = (EditText) v.findViewById(R.id.input_lastName);
        mInputMobileNum = (EditText) v.findViewById(R.id.input_customer_mobile);
        mInputQrCard = (EditText) v.findViewById(R.id.input_qr_card);
        mInputOtp = (EditText) v.findViewById(R.id.input_otp);

        mLabelFirstName = (EditText) v.findViewById(R.id.label_firstName);
        mLabelLastName = (EditText) v.findViewById(R.id.label_lastName);
        mLabelMobile = (EditText) v.findViewById(R.id.label_mobile);
        mLabelCard = (EditText) v.findViewById(R.id.label_card);
        mLabelInfoOtp = (EditText) v.findViewById(R.id.label_info_otp);
        mLabelInfoMobile = (EditText) v.findViewById(R.id.label_info_mobile);
        mLabelInfoName = (EditText) v.findViewById(R.id.label_info_name);

        mImageMobile = v.findViewById(R.id.image_mobile);
        mImageCard = v.findViewById(R.id.image_card);
        mImageName = v.findViewById(R.id.image_name);

        mLayoutOtp = v.findViewById(R.id.layout_otp);
    }

    private void setQrCode(String qrCode) {
        if(ValidationHelper.validateCardId(qrCode) == ErrorCodes.NO_ERROR) {
            scannedCardId = qrCode;
            mInputQrCard.setText("OK");
            mInputQrCard.setTextColor(ContextCompat.getColor(getActivity(), R.color.green_positive));
            mInputQrCard.setError(null);
        } else {
            AppCommonUtil.toast(getActivity(),"Invalid Member Card");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        AppCommonUtil.cancelToast();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("scannedCardId", scannedCardId);
    }
}
