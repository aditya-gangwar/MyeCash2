package in.myecash.merchantbase;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import in.myecash.appbase.BaseDialog;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.OnSingleClickListener;
import in.myecash.appbase.utilities.ValidationHelper;
import in.myecash.common.MyGlobalSettings;
import in.myecash.common.constants.ErrorCodes;

/**
 * Created by adgangwa on 31-03-2017.
 */

public class LoadTestDialog extends BaseDialog {
    public static final String TAG = "MchntApp-LoadTestDialog";

    private LoadTestDialog.LoadTestDialogIf mListener;

    public interface LoadTestDialogIf {
        void onTestLoad(String custId, String pin, int reps);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mListener = (LoadTestDialog.LoadTestDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement LoadTestDialogIf");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateDialog");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_load_test, null);
        initUiResources(v);

        // return new dialog
        final AlertDialog alertDialog =  new AlertDialog.Builder(getActivity()).setView(v)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, this)
                .create();
        if(AppConstants.BLOCK_SCREEN_CAPTURE) {
            alertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(LoadTestDialog.this, (AlertDialog) dialog);

                final Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new OnSingleClickListener() {
                    @Override
                    public void onSingleClick(View v) {
                        AppCommonUtil.hideKeyboard(getDialog());

                        if(validate()) {
                            //mListener.onPasswdResetData(mInputCustId.getText().toString(), mInputStoreName.getText().toString());
                            mListener.onTestLoad( mInputCustId.getText().toString(),
                                    mInputPin.getText().toString(),
                                    Integer.valueOf(mInputReps.getText().toString()) );
                            getDialog().dismiss();
                        }
                    }
                });
            }
        });

        alertDialog.setCanceledOnTouchOutside(false);
        return alertDialog;
    }

    private boolean validate() {
        int error = ValidationHelper.validateCustInternalId(mInputCustId.getText().toString());
        if(error != ErrorCodes.NO_ERROR) {
            mInputCustId.setError(AppCommonUtil.getErrorDesc(error));
            return false;
        }

        error = ValidationHelper.validatePin(mInputPin.getText().toString());
        if(error != ErrorCodes.NO_ERROR) {
            mInputPin.setError(AppCommonUtil.getErrorDesc(error));
            return false;
        }

        return true;
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

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
    }

    private EditText mInputCustId;
    private EditText mInputPin;
    private EditText mInputReps;

    private void initUiResources(View v) {
        mInputCustId = (EditText) v.findViewById(R.id.input_custId);
        mInputPin = (EditText) v.findViewById(R.id.input_pin);
        mInputReps = (EditText) v.findViewById(R.id.input_reps);
    }
}

