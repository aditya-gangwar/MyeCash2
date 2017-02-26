package in.myecash.merchantbase;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.Toast;

import in.myecash.appbase.BaseDialog;
import in.myecash.appbase.barcodeReader.BarcodeCaptureActivity;
import in.myecash.appbase.utilities.OnSingleClickListener;
import in.myecash.common.CommonUtils;
import in.myecash.common.MyGlobalSettings;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.ValidationHelper;
import in.myecash.merchantbase.entities.MyCustomerOps;

/**
 * Created by adgangwa on 21-05-2016.
 */
public class CustomerOpDialog extends BaseDialog  {
    private static final String TAG = "MchntApp-CustomerOpDialog";
    public static final int RC_BARCODE_CAPTURE_CARD_DIALOG = 9003;

    private static final String ARG_OP_CODE = "argOpCode";
    private static final String ARG_OTP_GENERATED = "argOldDialog";
    private static final String ARG_MOBILE_NUM = "argMobileNum";
    private static final String ARG_CARD_NUM = "argCardNum";
    //private static final String ARG_CUST_NAME = "argCustName";
    private static final String ARG_EXTRA_PARAMS = "argExtraParams";

    private static final int REQUEST_REASON = 1;
    private static final String DIALOG_REASON = "dialogReason";

    private CustomerOpDialogIf mCallback;
    private String mImgFilename;
    private String scannedCardId;

    public interface CustomerOpDialogIf {
        void onCustomerOpOk(String tag, String mobileNum, String qrCode, String extraParam, String imgFilename);
        void onCustomerOpOtp(String otp);
        void onCustomerOpReset(String tag);
    }

    public static CustomerOpDialog newInstance(String opCode, MyCustomerOps custOp) {
        Bundle args = new Bundle();
        args.putString(ARG_OP_CODE, opCode);
        if(custOp != null) {
            if(custOp.getOp_status() != null &&
                    custOp.getOp_status().equals(MyCustomerOps.CUSTOMER_OP_STATUS_OTP_GENERATED)) {
                args.putBoolean(ARG_OTP_GENERATED,true);
            } else {
                args.putBoolean(ARG_OTP_GENERATED,false);
            }
            // old dialog to ask for OTP
            args.putString(ARG_MOBILE_NUM, custOp.getMobile_num());
            args.putString(ARG_CARD_NUM, custOp.getQr_card());
            if(DbConstants.OP_NEW_CARD.equals(opCode) ||
                    DbConstants.OP_CHANGE_MOBILE.equals(opCode) ) {
                args.putString(ARG_EXTRA_PARAMS, custOp.getExtra_op_params());
            }
        }else {
            args.putBoolean(ARG_OTP_GENERATED,false);
        }
        CustomerOpDialog fragment = new CustomerOpDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (CustomerOpDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement CustomerOpDialogIf");
        }
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
                mCallback.onCustomerOpReset(getTag());
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
            String opCode = getArguments().getString(ARG_OP_CODE);
            mImgFilename = getTempImgFilename(opCode);
            intent.putExtra(BarcodeCaptureActivity.ImageFileName, mImgFilename);

            startActivityForResult(intent, RC_BARCODE_CAPTURE_CARD_DIALOG);
        }
        return true;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String opCode = getArguments().getString(ARG_OP_CODE);
        Boolean isOtpGenerated = getArguments().getBoolean(ARG_OTP_GENERATED);

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_customer_op, null);

        bindUiResources(v);
        if(savedInstanceState!=null) {
            LogMy.d(TAG,"Restoring");
            scannedCardId = savedInstanceState.getString("scannedCardId");
            mImgFilename = savedInstanceState.getString("mImgFilename");
        }

        initDialogView(opCode, isOtpGenerated);

        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setView(v)
                .setPositiveButton(android.R.string.ok,this)
                .setNegativeButton(android.R.string.cancel, this)
                .setNeutralButton("Restart", this)
                .create();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(CustomerOpDialog.this, (AlertDialog) dialog);
            }
        });

        return dialog;
    }

    private void initDialogView(String opCode, boolean isOtpGenerated) {
        // Set title
        String title = "Customer: "+opCode;
        mTitle.setText(title);
        mInfoEnd.setVisibility(View.GONE);
        //mInfoNewMobile.setVisibility(View.GONE);

        // Disable OTP if OTP not generated
        if(!isOtpGenerated) {
            mLabelOTP.setEnabled(false);
            mInputOTP.setEnabled(false);
            mLayoutOTP.setVisibility(View.GONE);
            //mImageOtp.setAlpha(0.5f);
            //mInfoOtp.setVisibility(View.GONE);
            switch (opCode) {
                case DbConstants.OP_RESET_PIN:
                case DbConstants.OP_NEW_CARD:
                    mInfoOtp.setText("OTP will be sent on registered mobile for verification.");
                    break;
                case DbConstants.OP_CHANGE_MOBILE:
                    mInfoOtp.setText("OTP will be sent on New to be registered mobile for verification.");
                    break;
                default:
                    mInfoOtp.setVisibility(View.GONE);
            }
        } else {
            mLayoutOTP.setVisibility(View.VISIBLE);
        }

        String mobileNum = getArguments().getString(ARG_MOBILE_NUM, null);
        if(mobileNum != null) {
            mInputMobileNum.setText(mobileNum);
            if(isOtpGenerated) {
                mLabelMobileNum.setEnabled(false);
                mInputMobileNum.setEnabled(false);
                mImageMobile.setAlpha(0.5f);
            }
        }
        String cardNum = getArguments().getString(ARG_CARD_NUM, null);
        if(cardNum != null) {
            //mInputQrCard.setText(CommonUtils.getPartialVisibleStr(cardNum));
            scannedCardId = cardNum;
            mInputQrCard.setText("OK");
            mInputQrCard.setTextColor(ContextCompat.getColor(getActivity(), R.color.green_positive));
            if(isOtpGenerated) {
                mLabelQrCard.setEnabled(false);
                mInputQrCard.setEnabled(false);
                mImageCard.setAlpha(0.5f);
            }
        } else {
            mInputQrCard.setOnTouchListener(this);
        }

        if(opCode.equals(DbConstants.OP_NEW_CARD)) {
            mLabelQrCard.setText("New Card");
            String reason = getArguments().getString(ARG_EXTRA_PARAMS, null);
            if(reason != null) {
                mInputReason.setText(reason);
                if(isOtpGenerated) {
                    mLabelReason.setEnabled(false);
                    mInputReason.setEnabled(false);
                    mImageReason.setAlpha(0.5f);
                }
            } else {
                initChoiceReasons(null);
            }
        } else {
            mInputReason.setEnabled(false);
            //mSpaceReason.setVisibility(View.GONE);
            mLayoutReason.setVisibility(View.GONE);
        }

        if(opCode.equals(DbConstants.OP_CHANGE_MOBILE)) {
            mInfoEnd.setVisibility(View.VISIBLE);
            mInfoEnd.setText(String.format(getString(R.string.cust_mobile_change_info), MyGlobalSettings.getCustAccLimitModeMins().toString()));

            mLabelMobileNum.setText("Old Mobile");
            String newMobile = getArguments().getString(ARG_EXTRA_PARAMS, null);
            if(newMobile != null) {
                mInputNewMobile.setText(newMobile);
                if(isOtpGenerated) {
                    mLabelNewMobile.setEnabled(false);
                    mInputNewMobile.setEnabled(false);
                    mImageNewMobile.setAlpha(0.5f);
                } /*else {
                    mInfoNewMobile.setVisibility(View.VISIBLE);
                }*/
            }
        } else {
            mInputNewMobile.setEnabled(false);
            //mSpaceNewMobile.setVisibility(View.GONE);
            mLayoutNewMobile.setVisibility(View.GONE);
        }

        if(opCode.equals(DbConstants.OP_RESET_PIN)) {
            mInfoEnd.setVisibility(View.VISIBLE);
            mInfoEnd.setText(String.format(getString(R.string.cust_pin_reset_info), MyGlobalSettings.getCustPasswdResetMins().toString()));
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point
        final AlertDialog d = (AlertDialog)getDialog();
        final String opCode = getArguments().getString(ARG_OP_CODE);

        if(d != null)
        {
            Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new OnSingleClickListener() {
                @Override
                public void onSingleClick(View v) {
                    Boolean wantToCloseDialog = false;

                    if(validate()) {
                        // OTP is only enabled for old dialogs
                        String otp = null;
                        if(mInputOTP.isEnabled()) {
                            otp = mInputOTP.getText().toString();
                            mCallback.onCustomerOpOtp(otp);

                        } else {
                            String extraParam = null;

                            if( mInputReason.isEnabled() ) {
                                extraParam = mInputReason.getText().toString();
                            } else if( mInputNewMobile.isEnabled() ) {
                                extraParam = mInputNewMobile.getText().toString();
                            }

                            mCallback.onCustomerOpOk(
                                    getTag(),
                                    mInputMobileNum.getText().toString(),
                                    scannedCardId,
                                    extraParam,
                                    mImgFilename);
                        }
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
            if(errorCode != ErrorCodes.NO_ERROR) {
                mInputMobileNum.setError(AppCommonUtil.getErrorDesc(errorCode));
                retValue = false;
            }
        }

        if(mInputQrCard.isEnabled()){
            errorCode = ValidationHelper.validateCardId(scannedCardId);
            if(errorCode != ErrorCodes.NO_ERROR) {
                mInputQrCard.setError(AppCommonUtil.getErrorDesc(errorCode));
                retValue = false;
            }
        }

        if( mInputReason.isEnabled()
                && mInputReason.getText().toString().isEmpty() ) {
            mInputReason.setError(AppCommonUtil.getErrorDesc(ErrorCodes.EMPTY_VALUE));
            retValue = false;
        }

        if( mInputNewMobile.isEnabled()) {
            errorCode = ValidationHelper.validateMobileNo(mInputNewMobile.getText().toString());
            if(errorCode != ErrorCodes.NO_ERROR) {
                mInputNewMobile.setError(AppCommonUtil.getErrorDesc(errorCode));
                retValue = false;
            }
            // check that it is different from current number
            else if(mInputNewMobile.getText().toString().equals(mInputMobileNum.getText().toString())) {
                    mInputNewMobile.setError("Same as given current mobile number");
                    retValue = false;
            }
        }

        if(mInputOTP.isEnabled()) {
            errorCode = ValidationHelper.validateOtp(mInputOTP.getText().toString());
            if(errorCode != ErrorCodes.NO_ERROR) {
                mInputOTP.setError(AppCommonUtil.getErrorDesc(errorCode));
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
                String opCode = getArguments().getString(ARG_OP_CODE);
                mImgFilename = getTempImgFilename(opCode);
                intent.putExtra(BarcodeCaptureActivity.ImageFileName, mImgFilename);

                startActivityForResult(intent, RC_BARCODE_CAPTURE_CASH_TXN);

            }
        }
        return true;
    }*/

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogMy.d(TAG,"In onActivityResult"+requestCode+","+resultCode);
        if (requestCode == RC_BARCODE_CAPTURE_CARD_DIALOG) {
            if (resultCode == ErrorCodes.NO_ERROR) {
                String qrCode = data.getStringExtra(BarcodeCaptureActivity.BarcodeObject);
                LogMy.d(TAG,"Read customer QR code: "+qrCode);
                setQrCode(qrCode);
            } else {
                LogMy.e(TAG,"Failed to read barcode");
            }
        } else if (requestCode == REQUEST_REASON) {
            if (resultCode == ErrorCodes.NO_ERROR) {
                String reason = data.getStringExtra(DialogFragmentWrapper.EXTRA_SELECTION);
                mInputReason.setText(reason);
                mInputReason.setError(null);
            }
        }
    }

    private EditText mTitle;
    private EditText mLabelMobileNum;
    private EditText mInputMobileNum;
    private EditText mLabelQrCard;
    private EditText mInputQrCard;

    //private Space mSpaceNewMobile;
    private LinearLayout mLayoutNewMobile;
    private EditText mLabelNewMobile;
    private EditText mInputNewMobile;

    //private Space mSpaceOtp;
    private LinearLayout mLayoutOTP;
    private EditText mLabelOTP;
    private EditText mInputOTP;
    /*
    private Space mSpaceName;
    private LinearLayout mLayoutName;
    private EditText mInputName;*/
    //private Space mSpaceReason;
    private LinearLayout mLayoutReason;
    private EditText mLabelReason;
    private EditText mInputReason;

    private EditText mInfoOtp;
    private EditText mInfoEnd;
    //private EditText mInfoNewMobile;

    private View mImageOtp;
    private View mImageMobile;
    private View mImageCard;
    private View mImageNewMobile;
    private View mImageReason;


    private void bindUiResources(View v) {
        mTitle = (EditText) v.findViewById(R.id.label_cust_op_title);
        mLabelMobileNum = (EditText) v.findViewById(R.id.label_customer_mobile);
        mInputMobileNum = (EditText) v.findViewById(R.id.input_customer_mobile);
        mLabelQrCard = (EditText) v.findViewById(R.id.label_qr_card);
        mInputQrCard = (EditText) v.findViewById(R.id.input_qr_card);

        //mSpaceOtp = (Space) v.findViewById(R.id.space_otp);
        mLayoutOTP = (LinearLayout) v.findViewById(R.id.layout_otp);
        mLabelOTP = (EditText) v.findViewById(R.id.label_cust_otp);
        mInputOTP = (EditText) v.findViewById(R.id.input_cust_otp);

        //mSpaceNewMobile = (Space) v.findViewById(R.id.space_new_mobile);
        mLayoutNewMobile = (LinearLayout) v.findViewById(R.id.layout_new_mobile);
        mLabelNewMobile = (EditText) v.findViewById(R.id.label_new_mobile);
        mInputNewMobile = (EditText) v.findViewById(R.id.input_new_mobile);

        /*mSpaceName = (Space) v.findViewById(R.id.space_customer_name);
        mLayoutName = (LinearLayout) v.findViewById(R.id.layout_customer_name);
        mInputName = (EditText) v.findViewById(R.id.input_customer_name);*/

        //mSpaceReason = (Space) v.findViewById(R.id.space_reason);
        mLayoutReason = (LinearLayout) v.findViewById(R.id.layout_reason);
        mLabelReason = (EditText) v.findViewById(R.id.label_reason);
        mInputReason = (EditText) v.findViewById(R.id.input_reason);

        mInfoEnd = (EditText) v.findViewById(R.id.label_info);
        mInfoOtp = (EditText) v.findViewById(R.id.label_info_otp);
        //mInfoNewMobile = (EditText) v.findViewById(R.id.label_info_newMobile);

        mImageOtp = v.findViewById(R.id.image_otp);
        mImageMobile = v.findViewById(R.id.image_mobile);
        mImageCard = v.findViewById(R.id.image_card);
        mImageNewMobile = v.findViewById(R.id.image_newMobile);
        mImageReason = v.findViewById(R.id.image_reason);

    }

    private void initChoiceReasons(final String selectedReason) {
        mInputReason.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                FragmentManager fragManager = getFragmentManager();
                if ((event.getAction() == MotionEvent.ACTION_UP) && (fragManager.findFragmentByTag(DIALOG_REASON) == null)) {
                    //LogMy.d(TAG, "In onTouch");
                    setReasonValues(selectedReason, fragManager);
                    return true;
                }
                return false;
            }
        });
        mInputReason.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                FragmentManager fragManager = getFragmentManager();
                if (hasFocus && (fragManager.findFragmentByTag(DIALOG_REASON) == null)) {
                    //LogMy.d(TAG, "In onFocusChange");
                    setReasonValues(selectedReason, fragManager);
                }
            }
        });
    }

    private void setReasonValues(String selectedReason, FragmentManager fragManager) {
        AppCommonUtil.hideKeyboard(getActivity());
        String reasons[] = getResources().getStringArray(R.array.new_card_reason_array);
        int checkedItem = -1;
        if(selectedReason != null) {
            for(int i=0; i<reasons.length; i++) {
                if(reasons[i].equals(selectedReason)) {
                    checkedItem = i;
                }
            }
        }
        DialogFragmentWrapper dialog = DialogFragmentWrapper.createSingleChoiceDialog("Reason", reasons, checkedItem, true);
        dialog.setTargetFragment(CustomerOpDialog.this,REQUEST_REASON);
        dialog.show(fragManager, DIALOG_REASON);
    }

    private void setQrCode(String qrCode) {
        if(ValidationHelper.validateCardId(qrCode) == ErrorCodes.NO_ERROR) {
            scannedCardId = qrCode;
            mInputQrCard.setText("OK");
            mInputQrCard.setTextColor(ContextCompat.getColor(getActivity(), R.color.green_positive));
            mInputQrCard.setError(null);
        } else {
            Toast.makeText(getActivity(), "Invalid Member Card", Toast.LENGTH_LONG).show();
        }
    }

    private String getTempImgFilename(String opCode) {
        LogMy.d(TAG,"In getTempImgFilename: "+opCode);
        String filename = opCode+"_"+Long.toString(System.currentTimeMillis())+"."+ CommonConstants.PHOTO_FILE_FORMAT;
        return filename.replace(" ","_");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("scannedCardId", scannedCardId);
        outState.putString("mImgFilename", mImgFilename);
    }
}
