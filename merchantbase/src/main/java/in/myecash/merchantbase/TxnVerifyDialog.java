package in.myecash.merchantbase;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import in.myecash.appbase.BaseDialog;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.entities.MyCashback;
import in.myecash.appbase.utilities.OnSingleClickListener;

/**
 * Created by adgangwa on 15-09-2016.
 */
public class TxnVerifyDialog extends BaseDialog {
    public static final String TAG = "MchntApp-TxnVerifyDialog";

    private TxnVerifyDialogIf mListener;
    public interface TxnVerifyDialogIf {
        void startTxnVerify(int sortType);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mListener = (TxnVerifyDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement TxnVerifyDialogIf");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateDialog");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_txn_verify, null);
        initUiResources(v);

        // return new dialog
        final AlertDialog alertDialog =  new AlertDialog.Builder(getActivity()).setView(v)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, this)
                .create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(TxnVerifyDialog.this, (AlertDialog) dialog);

                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new OnSingleClickListener() {
                    @Override
                    public void onSingleClick(View v) {

                        int selectedId = mRadioGroup.getCheckedRadioButtonId();
                        int selectedType = -1;

                        if (selectedId == R.id.radioCard) {
                            selectedType = AppConstants.TXN_VERIFY_CARD;

                        } else if (selectedId == R.id.radioPIN) {
                            selectedType = AppConstants.TXN_VERIFY_PIN;

                        } else if (selectedId == R.id.radioOTP) {
                            selectedType = AppConstants.TXN_VERIFY_OTP;
                        }

                        if(selectedType==-1) {
                            AppCommonUtil.toast(getActivity(), "Select Verification Method");
                        } else {
                            mListener.startTxnVerify(selectedType);
                            getDialog().dismiss();
                        }
                    }
                });
            }
        });

        alertDialog.setCanceledOnTouchOutside(false);
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

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
    }

    private RadioGroup mRadioGroup;
    private RadioButton mRadioCard;
    private RadioButton mRadioPin;
    private RadioButton mRadioOtp;

    private void initUiResources(View v) {
        mRadioGroup = (RadioGroup) v.findViewById(R.id.custSortRadioGroup);
        mRadioCard = (RadioButton) v.findViewById(R.id.radioCard);
        mRadioPin = (RadioButton) v.findViewById(R.id.radioPIN);
        mRadioOtp = (RadioButton) v.findViewById(R.id.radioOTP);
    }
}
