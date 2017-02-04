package in.myecash.customerbase;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
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

import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import in.myecash.appbase.utilities.AppAlarms;
import in.myecash.appbase.utilities.RootUtil;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.GlobalSettingConstants;
import in.myecash.customerbase.entities.CustomerUser;
import in.myecash.customerbase.helper.MyRetainedFragment;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.MyGlobalSettings;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.ValidationHelper;

public class LoginCustActivity extends AppCompatActivity implements
        MyRetainedFragment.RetainedFragmentIf, DialogFragmentWrapper.DialogFragmentWrapperIf,
        PasswdResetDialog.PasswdResetDialogIf, AccEnableDialog.AccEnableDialogIf {

    private static final String TAG = "CustApp-LoginActivity";

    // permission request codes need to be < 256
    private static final int RC_HANDLE_STORAGE_PERM = 10;

    private static final String RETAINED_FRAGMENT_TAG = "workLogin";
    private static final String DIALOG_PASSWD_RESET = "dialogPaswdReset";
    private static final String DIALOG_ENABLE_ACC = "dialogEnableAcc";
    private static final String DIALOG_SESSION_TIMEOUT = "dialogSessionTimeout";

    MyRetainedFragment      mWorkFragment;

    private String          mPassword;
    private String          mLoginId;
    private boolean         mProcessingResetPasswd;
    //private boolean         mRememberMe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check to see if we have retained the worker fragment.
        FragmentManager fm = getFragmentManager();
        mWorkFragment = (MyRetainedFragment) fm.findFragmentByTag(RETAINED_FRAGMENT_TAG);

        // If not retained (or first time running), we need to create it.
        if (mWorkFragment == null) {
            LogMy.d(TAG, "Creating retained fragment");
            mWorkFragment = new MyRetainedFragment();
            fm.beginTransaction().add(mWorkFragment, RETAINED_FRAGMENT_TAG).commit();
        }

        /*AsyncCallback<Boolean> isValidLoginCallback = new AsyncCallback<Boolean>()
        {
            @Override
            public void handleResponse( Boolean response )
            {
                LogMy.d(TAG, "isValidLogin response: "+response);
                if(response) {
                    int status = CustomerUser.tryAutoLogin();
                    if (status == ErrorCodes.NO_ERROR) {
                        LogMy.d(TAG, "Auto login success");
                        onLoginSuccess();
                        return;
                    }
                }
                // auto login not success
                LogMy.d(TAG, "Auto login not ok");
                processManualLogin();
            }

            @Override
            public void handleFault( BackendlessFault fault )
            {
                LogMy.d(TAG, "In handleFault");
                processManualLogin();
            }

        };
        CustomerUser.isValidLogin(isValidLoginCallback);*/

        // if valid login - no need to further load activity
        /*try {
            if (CustomerUser.isValidLogin()) {
                int status = CustomerUser.tryAutoLogin();
                if (status == ErrorCodes.NO_ERROR) {
                    LogMy.d(TAG, "Auto login success");
                    onLoginSuccess();
                    return;
                }
            }
        } catch (Exception e) {
            // ignore any exception
            LogMy.d(TAG,"Exception while checking for valid login: "+e.getMessage());
        }*/
    }

    @Override
    public void onBgThreadCreated() {
        /*
         * Auto login was creating some session hang issue on backendless backend - so not using it for now
         */
        processManualLogin();

        /*if(checkPermissions()) {
            // If permissions not available - proceed with normal login
            AppCommonUtil.showProgressDialog(this, AppConstants.progressLogin);
            mWorkFragment.tryAutoLogin();
        } else {
            processManualLogin();
        }*/
    }

    private void processManualLogin() {
        // setting off fullscreen mode as 'screen pan on keyboard' doesn't work fine with fullscreen
        // http://stackoverflow.com/questions/7417123/android-how-to-adjust-layout-in-full-screen-mode-when-softkeyboard-is-visible
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_login);
        // show the keyboard and adjust screen for the same
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // local activity initializations
        bindUiResources();
        makeForgotPasswordLink();
        makeHelpTnCLink();

        // Fill old login id
        String oldId = PreferenceManager.getDefaultSharedPreferences(this).getString(AppConstants.PREF_LOGIN_ID, null);
        if( oldId != null) {
            mIdTextRes.setText(oldId);
            mPasswdTextRes.requestFocus();
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
                int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(LoginCustActivity.this);
                if (resultCode != ErrorCodes.NO_ERROR) {
                    // Show error notification dialog
                    DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, AppCommonUtil.getErrorDesc(resultCode), false, true)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                } else {
                    if(checkPermissions()) {
                        initOperationData();
                        loginCustomer();
                    } else {
                        requestStoragePermission();
                    }
                }
            }
        });
    }

    private boolean checkPermissions() {
        // check external storage permission
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(rc != PackageManager.PERMISSION_GRANTED) {
            //requestStoragePermission();
            return false;
        }
        return true;
    }

    public void requestStoragePermission() {
        final String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_STORAGE_PERM);
            return;
        }

        final Activity thisActivity = this;
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions, RC_HANDLE_STORAGE_PERM);
            }
        };

        Snackbar.make(mIdTextRes, R.string.permission_write_storage_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_STORAGE_PERM) {
            LogMy.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            LogMy.d(TAG, "Permission granted: "+requestCode);
            // we have permission, re-trigger the login button
            mLoginButton.performClick();
            return;
        }

        LogMy.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        String msg = null;
        switch (requestCode) {
            case RC_HANDLE_STORAGE_PERM:
                msg = getString(R.string.no_write_storage_permission);
                break;
        }
        DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification(AppConstants.noPermissionTitle,
                msg, false, true);
        notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
    }

    private void initOperationData() {
        // instance may have been destroyed due to wrong login / account disabled etc
        AppCommonUtil.hideKeyboard(LoginCustActivity.this);
        getUiResourceValues();
    }

    public void loginCustomer() {
        LogMy.d(TAG, "In loginCustomer");

        // validate complete form and mark errors
        if (validate()) {
            // Check if device is rooted
            // Intentionally doing only on button press and not earlier - as we intend to collect user id too
            if(RootUtil.isDeviceRooted()) {
                // Show error notification dialog
                DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppConstants.msgInsecureDevice, false, true)
                        .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                Map<String,String> params = new HashMap<>();
                params.put("IMEI",AppCommonUtil.getIMEI(this));
                params.put("Manufacturer",AppCommonUtil.getDeviceManufacturer());
                params.put("Model",AppCommonUtil.getDeviceModel());
                params.put("AndroidVersion",AppCommonUtil.getAndroidVersion());
                AppAlarms.deviceRooted(mLoginId, DbConstants.USER_TYPE_CUSTOMER,"loginCustomer",params);
            }

            // disable login button
            mLoginButton.setEnabled(false);
            // show progress dialog
            AppCommonUtil.showProgressDialog(this, AppConstants.progressLogin);
            mWorkFragment.loginCustomer(mLoginId, mPassword);
        }
    }

    private boolean validate() {
        boolean valid = true;
        // validate all fields and mark ones with error
        // return false if any invalid
        int errorCode = ValidationHelper.validateMobileNo(mLoginId);
        if(errorCode!=ErrorCodes.NO_ERROR) {
            valid = false;
            mIdTextRes.setError(AppCommonUtil.getErrorDesc(errorCode));
        }

        errorCode = ValidationHelper.validatePassword(mPassword);
        if(errorCode!=ErrorCodes.NO_ERROR) {
            valid = false;
            mPasswdTextRes.setError(AppCommonUtil.getErrorDesc(errorCode));
        }

        return valid;
    }

    @Override
    public void onBgProcessResponse(int errorCode, int operation) {
        LogMy.d(TAG, "In onBgProcessResponse: "+operation+", "+errorCode);

        // Session timeout case - show dialog and logout - irrespective of invoked operation
        /*if(errorCode==ErrorCodes.SESSION_TIMEOUT &&
                operation != MyRetainedFragment.REQUEST_AUTO_LOGIN ) {
            AppCommonUtil.cancelProgressDialog(true);
            DialogFragmentWrapper.createNotification(AppConstants.notLoggedInTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                    .show(getFragmentManager(), DIALOG_SESSION_TIMEOUT);
            return;
        }*/

        try {

            if (operation == MyRetainedFragment.REQUEST_AUTO_LOGIN) {
                AppCommonUtil.cancelProgressDialog(true);
                if (errorCode == ErrorCodes.NO_ERROR) {
                    mLoginId = CustomerUser.getInstance().getCustomer().getMobile_num();
                    onLoginSuccess();
                } else {
                    processManualLogin();
                }

            } else if (operation == MyRetainedFragment.REQUEST_LOGIN) {
                AppCommonUtil.cancelProgressDialog(true);
                if (errorCode == ErrorCodes.NO_ERROR) {
                    onLoginSuccess();

                } else if (errorCode == ErrorCodes.USER_ACC_DISABLED) {
                    mLoginButton.setEnabled(true);
                    // reset Enable account parameters
                    mWorkFragment.mAccEnableCardNum = null;
                    mWorkFragment.mAccEnablePin = null;
                    mWorkFragment.mAccEnableOtp = null;
                    // Show Enable account dialog
                    AccEnableDialog dialog = AccEnableDialog.newInstance(null, null);
                    dialog.show(getFragmentManager(), DIALOG_ENABLE_ACC);

                } else if (errorCode == ErrorCodes.SERVICE_GLOBAL_DISABLED) {
                    mLoginButton.setEnabled(true);
                    // Add time at the end of error message
                    Date disabledUntil = MyGlobalSettings.getServiceDisabledUntil();
                    SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
                    String errorStr = AppCommonUtil.getErrorDesc(ErrorCodes.SERVICE_GLOBAL_DISABLED)
                            + mSdfDateWithTime.format(disabledUntil);
                    // Show error notification dialog
                    DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, errorStr, false, true)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);

                } else if (errorCode == ErrorCodes.UNDER_DAILY_DOWNTIME) {
                    mLoginButton.setEnabled(true);
                    // Show error notification dialog
                    String errorStr = String.format(AppCommonUtil.getErrorDesc(ErrorCodes.UNDER_DAILY_DOWNTIME),
                            MyGlobalSettings.getDailyDownStartHour(),
                            MyGlobalSettings.getDailyDownEndHour());

                    DialogFragmentWrapper.createNotification(AppConstants.serviceNATitle, errorStr, false, true)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);

                } else {
                    mLoginButton.setEnabled(true);
                    // Show error notification dialog
                    DialogFragmentWrapper.createNotification(AppConstants.loginFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                }

            } else if (operation == MyRetainedFragment.REQUEST_GENERATE_PWD) {
                AppCommonUtil.cancelProgressDialog(true);
                if (errorCode == ErrorCodes.NO_ERROR) {
                    // Show success notification dialog
                    DialogFragmentWrapper.createNotification(AppConstants.pwdGenerateSuccessTitle, AppConstants.genericPwdGenerateSuccessMsg, false, false)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                } else if (errorCode == ErrorCodes.OP_SCHEDULED) {
                    // Show success notification dialog
                    //Integer mins = MyGlobalSettings.getCustPasswdResetMins() + GlobalSettingConstants.CUSTOMER_PASSWORD_RESET_TIMER_INTERVAL;
                    String msg = String.format(AppConstants.pwdGenerateSuccessMsg, String.valueOf(AppCommonUtil.mErrorParams.opScheduledMins));
                    DialogFragmentWrapper.createNotification(AppConstants.pwdGenerateSuccessTitle, msg, false, false)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                } else if (errorCode == ErrorCodes.DUPLICATE_ENTRY) {
                    // Old request is already pending
                    Integer mins = MyGlobalSettings.getCustPasswdResetMins() + GlobalSettingConstants.CUSTOMER_PASSWORD_RESET_TIMER_INTERVAL;
                    String msg = String.format(AppConstants.pwdGenerateSuccessMsg, mins);
                    DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, msg, false, true)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                } else {
                    // Show error notification dialog
                    DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                }
                mProcessingResetPasswd = false;

            } else if (operation == MyRetainedFragment.REQUEST_ENABLE_ACC) {
                AppCommonUtil.cancelProgressDialog(true);
                if (errorCode == ErrorCodes.NO_ERROR) {
                    // Show success notification dialog
                    DialogFragmentWrapper.createNotification(AppConstants.defaultSuccessTitle, AppConstants.enableAccSuccessMsg, false, false)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);

                } else if (errorCode == ErrorCodes.OTP_GENERATED) {
                    // OTP sent successfully to registered mobile, ask for the same
                    // show the 'enable account dialog' again
                    AccEnableDialog dialog = AccEnableDialog.newInstance(mWorkFragment.mAccEnableCardNum, mWorkFragment.mAccEnablePin);
                    dialog.show(getFragmentManager(), DIALOG_ENABLE_ACC);

                } else {
                    // Show error notification dialog
                    DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                }
            }
        } catch (Exception e) {
            AppCommonUtil.cancelProgressDialog(true);
            LogMy.e(TAG, "Exception in LoginCustActivity:onBgProcessResponse: "+operation+": "+errorCode, e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
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
                    int errorCode = ValidationHelper.validateMobileNo(mLoginId);
                    if (errorCode == ErrorCodes.NO_ERROR) {
                        // Ask for confirmation and the PIN too
                        PasswdResetDialog dialog = new PasswdResetDialog();
                        dialog.show(getFragmentManager(), DIALOG_PASSWD_RESET);
                    } else {
                        mIdTextRes.setError(AppCommonUtil.getErrorDesc(errorCode));
                        mProcessingResetPasswd = false;
                    }
                } else {
                    AppCommonUtil.toast(LoginCustActivity.this, "Already in progress. Please wait.");
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

    private void makeHelpTnCLink()
    {
        SpannableString helpTnCPrompt = new SpannableString( getString( R.string.Help_TnC_label ) );

        ClickableSpan clickableSpanHelp = new ClickableSpan()
        {
            @Override
            public void onClick( View widget )
            {
                // read and set values
                initOperationData();
                // validate
                int errorCode = ValidationHelper.validateMerchantId(mLoginId);
                if(errorCode==ErrorCodes.NO_ERROR) {
                    // TODO: show 'HELP Page'
                } else {
                    mIdTextRes.setError(AppCommonUtil.getErrorDesc(errorCode));
                }
            }
        };

        String linkHelp = getString( R.string.Help_link );
        int linkStartIndex = helpTnCPrompt.toString().indexOf( linkHelp );
        int linkEndIndex = linkStartIndex + linkHelp.length();
        helpTnCPrompt.setSpan(clickableSpanHelp, linkStartIndex, linkEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Cliackable span for 'Help'
        ClickableSpan clickableSpanTnC = new ClickableSpan()
        {
            @Override
            public void onClick( View widget )
            {
                // TODO: redirect to T&C section for Customers on the website
            }
        };

        String linkTnC = getString( R.string.TnC_link );
        int linkStartIndexId = helpTnCPrompt.toString().indexOf( linkTnC );
        int linkEndIndexId = linkStartIndexId + linkTnC.length();
        helpTnCPrompt.setSpan(clickableSpanTnC, linkStartIndexId, linkEndIndexId, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView promptView = (TextView) findViewById( R.id.link_help_TnC );
        promptView.setText(helpTnCPrompt);
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

    @Override
    public void enableAccOk(String otp, String cardNum, String pin) {
        // update values
        mWorkFragment.mAccEnableOtp = otp;
        mWorkFragment.mAccEnableCardNum = cardNum;
        mWorkFragment.mAccEnablePin = pin;

        // show progress dialog
        AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
        mWorkFragment.enableAccount(mLoginId, mPassword);
    }

    /**
     * Sends a request for registration to RegistrationActivity,
     * expects for result in onActivityResult.
     */
    private void onLoginSuccess() {
        LogMy.d(TAG, "In onLoginSuccess");
        // Store latest succesfull login userid to preferences
        PreferenceManager.getDefaultSharedPreferences(LoginCustActivity.this)
                .edit()
                .putString(AppConstants.PREF_LOGIN_ID, mLoginId)
                .apply();

        // Global settings should be available by now
        AppCommonUtil.storeGSLocally(LoginCustActivity.this);
        // Delete local files - if required
        AppCommonUtil.delLocalFiles(CustomerUser.getInstance().getCustomer().getDelLocalFilesReq(), this);
        startCbFrag();
    }

    private void startCbFrag() {
        // turn on fullscreen mode, which was set off in OnCreate
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        //Start Cashback Activity
        Intent intent = new Intent( this, CashbackActivityCust.class );
        intent.putExtra(CashbackActivityCust.INTENT_EXTRA_USER_TOKEN, CustomerUser.getInstance().getUserToken());
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
    //private AppCompatCheckBox mRadioRemember;

    private void bindUiResources() {
        mIdTextRes = (EditText) findViewById(R.id.input_cust_mobile);
        mPasswdTextRes = (EditText) findViewById(R.id.input_password);
        mLoginButton = (Button) findViewById(R.id.btn_login);

        /*mRadioRemember = (AppCompatCheckBox) findViewById(R.id.radio_remember);
        mRadioRemember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //is chkIos checked?
                if (((AppCompatCheckBox) v).isChecked()) {
                    mRememberMe = true;
                } else {
                    mRememberMe = false;
                }
            }
        });*/
    }

    @Override
    public void onDialogResult(String tag, int position, ArrayList<Integer> selectedItemsIndexList) {
        // empty callback - nothing to do
    }

    @Override
    protected void onPause() {
        LogMy.d(TAG,"In onPause: ");
        super.onPause();
        AppCommonUtil.cancelProgressDialog(false);
        mWorkFragment.setResumeOk(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(AppCommonUtil.getProgressDialogMsg()!=null) {
            AppCommonUtil.showProgressDialog(this, AppCommonUtil.getProgressDialogMsg());
        }
        if(AppConstants.IS_PROD_BUILD) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        AppCommonUtil.setUserType(DbConstants.USER_TYPE_CUSTOMER);
        mWorkFragment.setResumeOk(true);
    }

    @Override
    public void onBackPressed() {
        if(!mWorkFragment.getResumeOk()) {
            return;
        }
        super.onBackPressed();
    }
}
