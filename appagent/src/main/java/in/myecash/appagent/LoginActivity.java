package in.myecash.appagent;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import in.myecash.appagent.entities.AgentUser;
import in.myecash.appagent.helper.MyRetainedFragment;
import in.myecash.commonbase.constants.AppConstants;
import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.DialogFragmentWrapper;
import in.myecash.commonbase.utilities.LogMy;
import in.myecash.commonbase.utilities.ValidationHelper;

import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity implements
        MyRetainedFragment.RetainedFragmentIf, DialogFragmentWrapper.DialogFragmentWrapperIf,
        PasswdResetDialog.PasswdResetDialogIf {

    private static final String TAG = "LoginActivity";

    private static final String RETAINED_FRAGMENT_TAG = "workLogin";
    private static final String DIALOG_PASSWD_RESET = "dialogPaswdReset";

    public static final String PREF_INSTANCE_ID = "agentInstanceId";

    MyRetainedFragment      mWorkFragment;
    private String          mPassword;
    private String          mLoginId;
    private String          mInstanceId;
    boolean                 mProcessingResetPasswd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        // setting off fullscreen mode as 'screen pan on keyboard' doesnt work fine with fullscreen
        // http://stackoverflow.com/questions/7417123/android-how-to-adjust-layout-in-full-screen-mode-when-softkeyboard-is-visible
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_login);
        // show the keyboard and adjust screen for the same
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // local activity initializations
        bindUiResources();
        makeForgotPasswordLink();

        // Check to see if we have retained the worker fragment.
        FragmentManager fm = getFragmentManager();
        mWorkFragment = (MyRetainedFragment) fm.findFragmentByTag(RETAINED_FRAGMENT_TAG);
        // If not retained (or first time running), we need to create it.
        if (mWorkFragment == null) {
            LogMy.d(TAG, "Creating retained fragment");
            mWorkFragment = new MyRetainedFragment();
            fm.beginTransaction().add(mWorkFragment, RETAINED_FRAGMENT_TAG).commit();
        }

        // Fill old login id
        String oldId = PreferenceManager.getDefaultSharedPreferences(this).getString(AppConstants.PREF_LOGIN_ID, null);
        if( oldId != null) {
            mIdTextRes.setText(oldId);
            mPasswdTextRes.requestFocus();
        }

        // Get instanceId
        mInstanceId = PreferenceManager.getDefaultSharedPreferences(this).getString(PREF_INSTANCE_ID, null);
        if( mInstanceId == null) {
            LogMy.d(TAG,"Creating new instance id for agent");
            // first run - generate and store
            // TODO: for production use randomUUID - instead of deviceId
            //mInstanceId = UUID.randomUUID().toString();
            mInstanceId = AppCommonUtil.getDeviceId(this);
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString(PREF_INSTANCE_ID, mInstanceId)
                    .apply();
        }


        mPasswdTextRes.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mLoginButton.performClick();
                    return true;
                }
                return false;
            }
        });

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // check internet connectivity
                int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(LoginActivity.this);
                if (resultCode != ErrorCodes.NO_ERROR) {
                    // Show error notification dialog
                    DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, ErrorCodes.appErrorDesc.get(resultCode), false, true)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                } else {
                    initOperationData();
                    loginAgent();
                }
            }
        });
    }

    private void initOperationData() {
        // instance may have been destroyed due to wrong login / account disabled etc
        AppCommonUtil.hideKeyboard(LoginActivity.this);
        getUiResourceValues();
    }

    public void loginAgent() {
        LogMy.d(TAG, "In loginAgent");

        // validate complete form and mark errors
        if (validate()) {
            // disable login button
            mLoginButton.setEnabled(false);
            // show progress dialog
            AppCommonUtil.showProgressDialog(this, AppConstants.progressLogin);
            mWorkFragment.loginAgent(mLoginId, mPassword, mInstanceId);
        }
    }

    private boolean validate() {
        boolean valid = true;
        // validate all fields and mark ones with error
        // return false if any invalid
        int errorCode = ValidationHelper.validateAgentId(mLoginId);
        if(errorCode!=ErrorCodes.NO_ERROR) {
            valid = false;
            mIdTextRes.setError(ErrorCodes.appErrorDesc.get(errorCode));
        }

        errorCode = ValidationHelper.validatePassword(mPassword);
        if(errorCode!=ErrorCodes.NO_ERROR) {
            valid = false;
            mPasswdTextRes.setError(ErrorCodes.appErrorDesc.get(errorCode));
        }

        return valid;
    }

    @Override
    public void onBgProcessResponse(int errorCode, int operation) {
        LogMy.d(TAG, "In onBgProcessResponse");

        if(operation==MyRetainedFragment.REQUEST_LOGIN)
        {
            AppCommonUtil.cancelProgressDialog(true);
            if(errorCode == ErrorCodes.NO_ERROR) {
                onLoginSuccess();
            } else {
                mLoginButton.setEnabled(true);
                // Show error notification dialog
                DialogFragmentWrapper.createNotification(AppConstants.loginFailureTitle, ErrorCodes.appErrorDesc.get(errorCode), false, true)
                        .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            }
        } else if(operation== MyRetainedFragment.REQUEST_GENERATE_PWD) {
            AppCommonUtil.cancelProgressDialog(true);
            if(errorCode == ErrorCodes.NO_ERROR) {
                // Show success notification dialog
                DialogFragmentWrapper.createNotification(AppConstants.pwdGenerateSuccessTitle, AppConstants.genericPwdGenerateSuccessMsg, false, false)
                        .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            } else {
                // Show error notification dialog
                DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, ErrorCodes.appErrorDesc.get(errorCode), false, true)
                        .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            }
            mProcessingResetPasswd = false;
        }
    }

    private void makeForgotPasswordLink()
    {
        mProcessingResetPasswd = false;
        SpannableString forgotPrompt = new SpannableString( getString( R.string.Forgot_passwd_label ) );

        ClickableSpan clickableSpan = new ClickableSpan()
        {
            @Override
            public void onClick( View widget )
            {
                if(!mProcessingResetPasswd) {
                    mProcessingResetPasswd = true;
                    // read and set values
                    initOperationData();
                    // validate
                    int errorCode = ValidationHelper.validateAgentId(mLoginId);
                    if (errorCode == ErrorCodes.NO_ERROR) {
                        // Ask for confirmation and the PIN too
                        PasswdResetDialog dialog = new PasswdResetDialog();
                        dialog.show(getFragmentManager(), DIALOG_PASSWD_RESET);
                    } else {
                        mIdTextRes.setError(ErrorCodes.appErrorDesc.get(errorCode));
                        mProcessingResetPasswd = false;
                    }
                } else {
                    AppCommonUtil.toast(LoginActivity.this, "Already in progress. Please wait.");
                }
            }
        };

        String linkText = getString( R.string.Forgot_passwd_link );
        int linkStartIndex = forgotPrompt.toString().indexOf( linkText );
        int linkEndIndex = linkStartIndex + linkText.length();
        forgotPrompt.setSpan(clickableSpan, linkStartIndex, linkEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView promptView = (TextView) findViewById( R.id.link_forgot_passwd );
        promptView.setText(forgotPrompt);
        promptView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void onPasswdResetData(String secret1) {
        if(secret1!=null) {
            // show progress dialog
            AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
            mWorkFragment.generatePassword(mLoginId, secret1);
        } else {
            mProcessingResetPasswd = false;
        }
    }

    /**
     * Sends a request for registration to RegistrationActivity,
     * expects for result in onActivityResult.
     */
    private void onLoginSuccess() {
        LogMy.d(TAG, "In onLoginSuccess");
        // Store latest succesfull login userid to preferences
        PreferenceManager.getDefaultSharedPreferences(LoginActivity.this)
                .edit()
                .putString(AppConstants.PREF_LOGIN_ID, AgentUser.getInstance().getUser_id())
                .apply();

        // turn on fullscreen mode, which was set off in OnCreate
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        //Start Cashback Activity
        Intent intent = new Intent( this, ActionsActivity.class );
        // clear Login activity from backstack
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void getUiResourceValues() {
        mLoginId = mIdTextRes.getText().toString();
        mPassword = mPasswdTextRes.getText().toString();
    }

    // ui resources
    private EditText    mIdTextRes;
    private EditText    mPasswdTextRes;
    private Button      mLoginButton;

    private void bindUiResources() {
        mIdTextRes = (EditText) findViewById(R.id.input_user_id);
        mPasswdTextRes = (EditText) findViewById(R.id.input_password);
        mLoginButton = (Button) findViewById(R.id.btn_login);
    }

    @Override
    public void onDialogResult(String tag, int position, ArrayList<Integer> selectedItemsIndexList) {
        // empty callback - nothing to do
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);
    }
}
