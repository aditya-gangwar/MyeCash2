package in.myecash.merchantbase;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
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
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.ValidationHelper;

/**
 * Created by adgangwa on 28-04-2016.
 */
public class CustomerCardDialog  extends DialogFragment
        implements View.OnClickListener {

    private static final String TAG = "CustomerCardDialog";
    public static final int RC_BARCODE_CAPTURE_CARD_DIALOG = 9003;

    private static final String ARG_MOBILE_NUM = "mobile_num";
    private static final String DIALOG_REASON = "dialogReason";

    private static final int REQUEST_REASON = 1;

    private CustomerCardDialogIf mCallback;

    public interface CustomerCardDialogIf {
        void onCustomerCardOk(String reason, String mobileNum, String qrCode);
    }

    public static CustomerCardDialog newInstance(String mobileNo) {
        Bundle args = new Bundle();
        if(mobileNo != null) {
            args.putString(ARG_MOBILE_NUM, mobileNo);
        }
        CustomerCardDialog fragment = new CustomerCardDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (CustomerCardDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement CustomerCardDialogIf");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String mobileNum = getArguments().getString(ARG_MOBILE_NUM, null);

        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_new_card, null);

        bindUiResources(v);
        initChoiceReasons();

        if(mobileNum==null || mobileNum.isEmpty()) {
            mInputMobileNum.requestFocus();
        } else {
            mInputMobileNum.setText(mobileNum);
            mInputMobileNum.clearFocus();
        }
        mInputQrCard.setOnClickListener(this);

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
                }).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(CustomerCardDialog.this, (AlertDialog) dialog);
            }
        });
        return dialog;
    }

    /*
    @Override
    public void onClick(DialogInterface dialog, int which) {

        if(validate()) {
            mCallback.onCustomerCardOk(
                    mInputReason.getText().toString(),
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
                        mCallback.onCustomerCardOk(
                                mInputReason.getText().toString(),
                                mInputMobileNum.getText().toString(),
                                mInputQrCard.getText().toString());
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

        int errorCode = ValidationHelper.validateMobileNo(mInputMobileNum.getText().toString());
        if(errorCode != ErrorCodes.NO_ERROR) {
            mInputMobileNum.setError(AppCommonUtil.getErrorDesc(errorCode));
            retValue = false;
        }

        errorCode = ValidationHelper.validateCustQrCode(mInputQrCard.getText().toString());
        if(errorCode != ErrorCodes.NO_ERROR) {
            mInputQrCard.setError(AppCommonUtil.getErrorDesc(errorCode));
            retValue = false;
        }
        return retValue;
    }

    @Override
    public void onClick(View v) {
        int vId = v.getId();
        LogMy.d(TAG, "In onClick: " + vId);

        if (vId == R.id.input_qr_card) {// launch barcode activity.
            Intent intent = new Intent(getActivity(), BarcodeCaptureActivity.class);
            intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
            intent.putExtra(BarcodeCaptureActivity.UseFlash, false);

            startActivityForResult(intent, RC_BARCODE_CAPTURE_CARD_DIALOG);

        }
    }

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

    private EditText mInputMobileNum;
    private EditText mInputReason;
    private EditText mInputQrCard;

    private void bindUiResources(View v) {
        mInputMobileNum = (EditText) v.findViewById(R.id.input_customer_mobile);
        mInputReason = (EditText) v.findViewById(R.id.input_reason);
        mInputQrCard = (EditText) v.findViewById(R.id.input_qr_card);
    }

    private void initChoiceReasons() {
        mInputReason.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                FragmentManager fragManager = getFragmentManager();
                if ((event.getAction() == MotionEvent.ACTION_UP) && (fragManager.findFragmentByTag(DIALOG_REASON) == null)) {
                    //LogMy.d(TAG, "In onTouch");
                    AppCommonUtil.hideKeyboard(getActivity());
                    String reasons[] = getResources().getStringArray(R.array.new_card_reason_array);
                    DialogFragmentWrapper dialog = DialogFragmentWrapper.createSingleChoiceDialog("Reason", reasons, -1, true);
                    dialog.setTargetFragment(CustomerCardDialog.this,REQUEST_REASON);
                    dialog.show(fragManager, DIALOG_REASON);
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
                    AppCommonUtil.hideKeyboard(getActivity());
                    String reasons[] = getResources().getStringArray(R.array.new_card_reason_array);
                    DialogFragmentWrapper dialog = DialogFragmentWrapper.createSingleChoiceDialog("Reason", reasons, -1, true);
                    dialog.setTargetFragment(CustomerCardDialog.this,REQUEST_REASON);
                    dialog.show(fragManager, DIALOG_REASON);
                }
            }
        });
    }

    private void setQrCode(String qrCode) {
        if(ValidationHelper.validateCustQrCode(qrCode) == ErrorCodes.NO_ERROR) {
            mInputQrCard.setText(qrCode);
        } else {
            Toast.makeText(getActivity(), "Invalid member QR code: " + qrCode, Toast.LENGTH_LONG).show();
        }
    }
}
