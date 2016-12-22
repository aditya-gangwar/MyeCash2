package in.myecash.merchantbase;

import android.app.Dialog;
import android.app.DialogFragment;
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

import in.myecash.common.MyGlobalSettings;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.ValidationHelper;

/**
 * Created by adgangwa on 26-04-2016.
 */
public class PasswdResetDialog extends DialogFragment implements DialogInterface.OnClickListener {
public static final String TAG = "MchntApp-PasswdResetDialog";

private PasswdResetDialogIf mListener;

public interface PasswdResetDialogIf {
    void onPasswdResetData(String dob);
}

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mListener = (PasswdResetDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement PasswdResetDialogIf");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateDialog");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_passwd_reset, null);
        initUiResources(v);

        String txt = String.format(getActivity().getString(R.string.mchnt_reset_passwd_info), MyGlobalSettings.getMchntPasswdResetMins().toString());
        mInfo.setText(txt);

        // return new dialog
        final AlertDialog alertDialog =  new AlertDialog.Builder(getActivity()).setView(v)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppCommonUtil.hideKeyboard(getDialog());
                        mListener.onPasswdResetData(null);
                        dialog.dismiss();
                    }
                })
                .create();
        alertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(PasswdResetDialog.this, (AlertDialog) dialog);

                final Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AppCommonUtil.hideKeyboard(getDialog());

                        if(validate()) {
                            //mListener.onPasswdResetData(mInputDob.getText().toString(), mInputStoreName.getText().toString());
                            mListener.onPasswdResetData(mInputDob.getText().toString());
                            getDialog().dismiss();
                        }
                    }
                });

                mInputDob.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            b.performClick();
                            return true;
                        }
                        return false;
                    }
                });
            }
        });

        alertDialog.setCanceledOnTouchOutside(false);
        return alertDialog;
    }

    private boolean validate() {
        int error = ValidationHelper.validateDob(mInputDob.getText().toString());
        if(error != ErrorCodes.NO_ERROR) {
            mInputDob.setError(AppCommonUtil.getErrorDesc(error));
            return false;
        }

        /*error = ValidationHelper.validateBrandName(mInputStoreName.getText().toString());
        if(error != ErrorCodes.NO_ERROR) {
            mInputStoreName.setError(AppCommonUtil.getErrorDesc(error));
            return false;
        }*/

        return true;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        //Do nothing here because we override this button in OnShowListener to change the close behaviour.
        //However, we still need this because on older versions of Android unless we
        //pass a handler the button doesn't get instantiated
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        mListener.onPasswdResetData(null);
    }

    private EditText mInputDob;
    private EditText mInfo;
    //private EditText mInputStoreName;
    private void initUiResources(View v) {
        mInputDob = (EditText) v.findViewById(R.id.input_dob);
        mInfo = (EditText) v.findViewById(R.id.labelInfo);
        //mInputStoreName = (EditText) v.findViewById(R.id.input_storeName);
    }
}
