package in.myecash.merchantbase;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import in.myecash.appbase.barcodeReader.BarcodeCaptureActivity;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.ValidationHelper;

/**
 * Created by adgangwa on 13-04-2016.
 */
public class CustomerRegDialog extends DialogFragment
        implements View.OnTouchListener {

    private static final String TAG = "CustomerRegDialog";
    public static final int RC_BARCODE_CAPTURE_REG_DIALOG = 9002;

    private static final String ARG_MOBILE_NUM = "mobile_num";
    private static final String ARG_QRCODE = "qrcode";

    private CustomerRegFragmentIf mCallback;

    public interface CustomerRegFragmentIf {
        void onCustomerRegOk(String name, String mobileNum, String qrCode);
        void onCustomerRegReset();
    }

    public static CustomerRegDialog newInstance(String mobileNo, String cardId) {
        Bundle args = new Bundle();
        if(mobileNo != null) {
            args.putString(ARG_MOBILE_NUM, mobileNo);
        }
        if(cardId != null) {
            args.putString(ARG_QRCODE, cardId);
        }
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

        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_register_customer, null);

        bindUiResources(v);

        // Any null means OTP not generated yet
        if(mobileNum==null || mobileNum.isEmpty() ||
                cardId == null || cardId.isEmpty()) {
            //mLabelOtp.setEnabled(false);
            //mInputOtp.setEnabled(false);
            mInputOtp.setText("");
            mLayoutOtp.setVisibility(View.GONE);
            mLabelInfoOtp.setVisibility(View.GONE);
        } else {
            mLabelInfoMobile.setVisibility(View.GONE);
            mInputOtp.requestFocus();
        }

        // When the dialog is opened from 'mobile number screen' (i.e. not from Menu)
        // Then it will receive 'mobile number' but not the card id

        // Set mobile num
        if(mobileNum!=null && !mobileNum.isEmpty()) {
            mInputMobileNum.setText(mobileNum);
            AppCommonUtil.makeEditTextOnlyView(mInputMobileNum);
            mInputMobileNum.clearFocus();
            mInputMobileNum.setEnabled(false);
            mLabelMobile.setEnabled(false);
            mImageMobile.setAlpha(0.5f);
        } else {
            mInputMobileNum.requestFocus();
        }

        // Set card Id
        if(cardId!=null && !cardId.isEmpty()) {
            mInputQrCard.setText(cardId);
            mInputQrCard.setClickable(false);
            mInputQrCard.setEnabled(false);
            mLabelCard.setEnabled(false);
            mImageCard.setAlpha(0.5f);
        } else {
            mInputQrCard.setOnTouchListener(this);
        }

        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setView(v)
                //.setPositiveButton(android.R.string.ok, this)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //Do nothing here because we override this button later to change the close behaviour.
                                //However, we still need this because on older versions of Android unless we
                                //pass a handler the button doesn't get instantiated
                            }
                        })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNeutralButton("Reset", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mCallback.onCustomerRegReset();
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(CustomerRegDialog.this, (AlertDialog) dialog);
            }
        });

        return dialog;
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
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Boolean wantToCloseDialog = false;

                    if(validate()) {
                        mCallback.onCustomerRegOk(
                                mInputMobileNum.getText().toString(),
                                mInputQrCard.getText().toString(),
                                mInputOtp.getText().toString());
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
            errorCode = ValidationHelper.validateCustQrCode(mInputQrCard.getText().toString());
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

        return retValue;
    }

    @Override
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
    }

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

    private EditText mInputMobileNum;
    private EditText mInputOtp;
    private EditText mInputQrCard;

    private EditText mLabelMobile;
    private EditText mLabelCard;
    private EditText mLabelInfoOtp;
    private EditText mLabelInfoMobile;

    private View mImageMobile;
    private View mImageCard;
    private View mImageOtp;

    private View mLayoutOtp;

    private void bindUiResources(View v) {
        mInputMobileNum = (EditText) v.findViewById(R.id.input_customer_mobile);
        mInputQrCard = (EditText) v.findViewById(R.id.input_qr_card);
        mInputOtp = (EditText) v.findViewById(R.id.input_otp);

        mLabelMobile = (EditText) v.findViewById(R.id.label_mobile);
        mLabelCard = (EditText) v.findViewById(R.id.label_card);
        mLabelInfoOtp = (EditText) v.findViewById(R.id.label_info_otp);
        mLabelInfoMobile = (EditText) v.findViewById(R.id.label_info_mobile);

        mImageMobile = v.findViewById(R.id.image_mobile);
        mImageCard = v.findViewById(R.id.image_card);
        mImageOtp = v.findViewById(R.id.image_otp);

        mLayoutOtp = v.findViewById(R.id.layout_otp);
    }

    private void setQrCode(String qrCode) {
        if(ValidationHelper.validateCustQrCode(qrCode) == ErrorCodes.NO_ERROR) {
            mInputQrCard.setText(qrCode);
        } else {
            AppCommonUtil.toast(getActivity(),"Invalid Member Card");
        }
    }
}
