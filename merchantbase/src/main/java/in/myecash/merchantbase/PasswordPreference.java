package in.myecash.merchantbase;

import android.app.AlertDialog;
import android.app.FragmentBreadCrumbs;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.appbase.utilities.ValidationHelper;

/**
 * Created by adgangwa on 25-04-2016.
 */
public class PasswordPreference extends DialogPreference
        implements View.OnClickListener {
    private static final String TAG = "MchntApp-PasswordPreference";

    public interface PasswordPreferenceIf {
        void changePassword(String oldPasswd, String newPassword);
    }

    private PasswordPreferenceIf mCallback;

    EditText inputCurrPasswd;
    EditText inputNewPasswd;
    EditText inputNewPasswd2;

    public PasswordPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        CashbackActivity activity = (CashbackActivity)context;
        mCallback = (PasswordPreferenceIf)activity;
        setDialogLayoutResource(R.layout.dialog_password_change);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogIcon(null);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        LogMy.d(TAG, "In onPrepareDialogBuilder");
        super.onPrepareDialogBuilder(builder);    //To change body of overridden methods use File | Settings | File Templates.
        builder.setTitle(null);
    }

    @Override
    protected void onBindDialogView (View view) {
        inputCurrPasswd = (EditText) view.findViewById(R.id.input_current_passwd);
        inputNewPasswd = (EditText) view.findViewById(R.id.input_new_passwd);
        inputNewPasswd2 = (EditText) view.findViewById(R.id.input_new_passwd_2);
        super.onBindDialogView(view);
    }

    @Override
    protected void showDialog(Bundle bundle) {
        LogMy.d(TAG, "In showDialog");
        super.showDialog(bundle);
        if(AppConstants.IS_PROD_BUILD) {
            getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        getDialog().setCanceledOnTouchOutside(false);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        Button pos = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
        pos.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        String currPasswd = inputCurrPasswd.getText().toString();
        String newPassword = inputNewPasswd.getText().toString();

        int errorCode = ValidationHelper.validatePassword(currPasswd);
        if(errorCode!= ErrorCodes.NO_ERROR) {
            inputCurrPasswd.setError(AppCommonUtil.getErrorDesc(errorCode));
        }
        errorCode = ValidationHelper.validatePassword(newPassword);
        if(errorCode!= ErrorCodes.NO_ERROR) {
            inputNewPasswd.setError(AppCommonUtil.getErrorDesc(errorCode));
        }

        String newPassword2 = inputNewPasswd2.getText().toString();
        if(!newPassword.equals(newPassword2)) {
            inputNewPasswd2.setError("Does not match with new password above.");
            errorCode = ErrorCodes.GENERAL_ERROR;
        }

        if(errorCode==ErrorCodes.NO_ERROR) {
            mCallback.changePassword(currPasswd,newPassword);
            getDialog().dismiss();
        }
    }
}
