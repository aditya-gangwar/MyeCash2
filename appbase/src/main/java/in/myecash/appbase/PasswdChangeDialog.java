package in.myecash.appbase;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import in.myecash.common.constants.ErrorCodes;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.ValidationHelper;

/**
 * Created by adgangwa on 26-04-2016.
 */
public class PasswdChangeDialog extends DialogFragment implements DialogInterface.OnClickListener {
    public static final String TAG = "PasswdChangeDialog";

    private PasswdChangeDialogIf mListener;

    public interface PasswdChangeDialogIf {
        void onPasswdChangeData(String oldPasswd, String newPassword);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mListener = (PasswdChangeDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement PasswdChangeDialogIf");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateDialog");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_password_change, null);
        initUiResources(v);

        // return new dialog
        final AlertDialog alertDialog =  new AlertDialog.Builder(getActivity()).setView(v)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppCommonUtil.hideKeyboard(getDialog());
                        //mListener.onPasswdResetData(null);
                        dialog.dismiss();
                    }
                })
                .create();
        alertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {

                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AppCommonUtil.hideKeyboard(getDialog());

                        String currPasswd = inputCurrPasswd.getText().toString();
                        String newPassword = inputNewPasswd.getText().toString();

                        int errorCode = ValidationHelper.validatePassword(currPasswd);
                        if(errorCode!= ErrorCodes.NO_ERROR) {
                            inputCurrPasswd.setError(ErrorCodes.appErrorDesc.get(errorCode));
                        }
                        errorCode = ValidationHelper.validatePassword(newPassword);
                        if(errorCode!= ErrorCodes.NO_ERROR) {
                            inputNewPasswd.setError(ErrorCodes.appErrorDesc.get(errorCode));
                        }

                        String newPassword2 = inputNewPasswd2.getText().toString();
                        if(newPassword.equals(newPassword2)) {
                            mListener.onPasswdChangeData(currPasswd,newPassword);
                            getDialog().dismiss();
                        } else {
                            inputNewPasswd2.setError("Does not match with new password above.");
                        }
                    }
                });
            }
        });

        alertDialog.setCanceledOnTouchOutside(false);
        return alertDialog;
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
        //mListener.onPasswdChangeData(null, null);
    }

    EditText inputCurrPasswd;
    EditText inputNewPasswd;
    EditText inputNewPasswd2;

    private void initUiResources(View view) {
        inputCurrPasswd = (EditText) view.findViewById(R.id.input_current_passwd);
        inputNewPasswd = (EditText) view.findViewById(R.id.input_new_passwd);
        inputNewPasswd2 = (EditText) view.findViewById(R.id.input_new_passwd_2);
    }
}
