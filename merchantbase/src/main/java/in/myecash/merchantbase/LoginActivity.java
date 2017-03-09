package in.myecash.merchantbase;

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
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.helpshift.support.Support;

import in.myecash.appbase.SingleWebViewActivity;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.OnSingleClickListener;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.MyGlobalSettings;
import in.myecash.appbase.utilities.AppAlarms;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.RootUtil;
import in.myecash.appbase.utilities.ValidationHelper;
import in.myecash.appbase.OtpPinInputDialog;
import in.myecash.common.constants.GlobalSettingConstants;
import in.myecash.merchantbase.entities.MerchantUser;
import in.myecash.merchantbase.helper.MyRetainedFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity implements
        MyRetainedFragment.RetainedFragmentIf, DialogFragmentWrapper.DialogFragmentWrapperIf,
        PasswdResetDialog.PasswdResetDialogIf, OtpPinInputDialog.OtpPinInputDialogIf,
        ForgotIdDialog.ForgotIdDialogIf {
    private static final String TAG = "MchntApp-LoginActivity";

    private static final String RETAINED_FRAGMENT_TAG = "workLogin";
    private static final String DIALOG_PASSWD_RESET = "dialogPaswdReset";
    private static final String DIALOG_PIN_LOGIN_NEW_DEVICE = "dialogPinNewDevice";
    private static final String DIALOG_FORGOT_ID = "dialogForgotId";
    //private static final String DIALOG_SESSION_TIMEOUT = "dialogSessionTimeout";

    // permission request codes need to be < 256
    private static final int RC_HANDLE_STORAGE_PERM = 10;
    private static final int RC_HANDLE_CAMERA_PERM = 12;

    //private MerchantUser    mMerchantUser;
    MyRetainedFragment      mWorkFragment;

    private String          mPassword;
    private String          mLoginId;
    boolean                 mProcessingResetPasswd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // setting off fullscreen mode as 'screen pan on keyboard' doesnt work fine with fullscreen
        // http://stackoverflow.com/questions/7417123/android-how-to-adjust-layout-in-full-screen-mode-when-softkeyboard-is-visible
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_login);

        // show the keyboard and adjust screen for the same
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        bindUiResources();

        // local activity initializations
        //mMerchantUser = MerchantUser.getInstance();
        makeForgotIdPasswordLink();
        makeHelpTnCLink();

        // Check to see if we have retained the worker fragment.
        FragmentManager fm = getFragmentManager();
        mWorkFragment = (MyRetainedFragment)fm.findFragmentByTag(RETAINED_FRAGMENT_TAG);
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
            mIdTextRes.clearFocus();
            mPasswdTextRes.requestFocus();
        }

        mPasswdTextRes.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                return false;
            }
        });
        mIdTextRes.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                return false;
            }
        });

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

        mLoginButton.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                // check internet connectivity
                int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(LoginActivity.this);
                if (resultCode != ErrorCodes.NO_ERROR) {
                    // Show error notification dialog
                    DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, AppCommonUtil.getErrorDesc(resultCode), false, true)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                } else {
                    if(checkPermissions()) {
                        initOperationData();
                        loginMerchant();
                    }
                }
            }
        });
    }
    
    private boolean checkPermissions() {
        // check external storage permission
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(rc != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermission();
            return false;
        }

        // check camera permission
        rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if(rc != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
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

    public void requestCameraPermission() {
        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions, RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mIdTextRes, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_STORAGE_PERM &&
                requestCode != RC_HANDLE_CAMERA_PERM) {
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
            case RC_HANDLE_CAMERA_PERM:
                msg = getString(R.string.no_camera_permission);
                break;
        }
        DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification(AppConstants.noPermissionTitle,
                msg, false, true);
        notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
    }

    @Override
    public void onDialogResult(String tag, int position, ArrayList<Integer> selectedItemsIndexList) {
        // do nothing
    }

    private void initOperationData() {
        // instance may have been destroyed due to wrong login / account disabled etc
        //mMerchantUser = MerchantUser.getInstance();
        AppCommonUtil.hideKeyboard(LoginActivity.this);
        getUiResourceValues();

        //mMerchantUser.setUser_id(mLoginId);
        // deviceInfo format: <device id>,<manufacturer>,<model>,<os version>
        //mMerchantUser.setDeviceId(AppCommonUtil.getDeviceId(this));
        /*
        String deviceInfo = AppCommonUtil.getDeviceId(this)+","
                + AppCommonUtil.getDeviceManufacturer()+","
                + AppCommonUtil.getDeviceModel()+","
                + AppCommonUtil.getAndroidVersion();*/
        //mMerchantUser.setDeviceInfo(deviceInfo);
    }

    public void loginMerchant() {
        LogMy.d(TAG, "In loginUser");

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
                AppAlarms.deviceRooted(mLoginId,DbConstants.USER_TYPE_MERCHANT,"loginMerchant",params);
            }

            // disable login button
            mLoginButton.setEnabled(false);
            // show progress dialog
            AppCommonUtil.showProgressDialog(this, AppConstants.progressLogin);
            mWorkFragment.loginUser(mLoginId, mPassword, AppCommonUtil.getDeviceId(this), null);
        }
    }

    private boolean validate() {
        boolean valid = true;
        int errorCode = ErrorCodes.NO_ERROR;
        // validate all fields and mark ones with error
        // return false if any invalid
        errorCode = ValidationHelper.validateMerchantId(mLoginId);
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
    public void onPinOtp(String pinOrOtp, String tag) {
        if(tag.equals(DIALOG_PIN_LOGIN_NEW_DEVICE)) {
            AppCommonUtil.showProgressDialog(this, AppConstants.progressLogin);
            mWorkFragment.loginUser(mLoginId, mPassword, AppCommonUtil.getDeviceId(this), pinOrOtp);
        }
    }

    @Override
    public void onBgProcessResponse(int errorCode, int operation) {
        LogMy.d(TAG, "In onBgProcessResponse");

        // Session timeout case - show dialog and logout - irrespective of invoked operation
        /*if(errorCode==ErrorCodes.SESSION_TIMEOUT || errorCode==ErrorCodes.NOT_LOGGED_IN) {
            AppCommonUtil.cancelProgressDialog(true);
            DialogFragmentWrapper.createNotification(AppConstants.notLoggedInTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                    .show(getFragmentManager(), DIALOG_SESSION_TIMEOUT);
            return;
        }*/

        if(errorCode==ErrorCodes.INTERNET_OK_SERVICE_NOK) {
            AppCommonUtil.cancelProgressDialog(true);
            if (operation == MyRetainedFragment.REQUEST_LOGIN) {
                mLoginButton.setEnabled(true);
            }

            String url = null;
            if(MyGlobalSettings.isAvailable()) {
                url = MyGlobalSettings.getServiceNAUrl();
            } else {
                // MyGlobalSettings.getServiceNAUrl() will use constant value
                url = PreferenceManager.getDefaultSharedPreferences(this)
                        .getString(AppConstants.PREF_SERVICE_NA_URL, MyGlobalSettings.getServiceNAUrl());
            }

            Intent intent = new Intent(LoginActivity.this, SingleWebViewActivity.class );
            intent.putExtra(SingleWebViewActivity.INTENT_EXTRA_URL, url);
            startActivity(intent);
            return;
        }

        try {
            if (operation == MyRetainedFragment.REQUEST_LOGIN) {
                AppCommonUtil.cancelProgressDialog(true);
                if (errorCode == ErrorCodes.NO_ERROR) {
                    onLoginSuccess();

                } else if (errorCode == ErrorCodes.OTP_GENERATED) {
                    mLoginButton.setEnabled(true);
                    // Not trusted device - ask user for OTP
                    OtpPinInputDialog dialog = OtpPinInputDialog.newInstance(
                            AppConstants.titleAddTrustedDeviceOtp,
                            AppConstants.msgAddTrustedDeviceOtp,
                            AppConstants.hintEnterOtp,
                            CommonConstants.OTP_LEN);
                    dialog.show(getFragmentManager(), DIALOG_PIN_LOGIN_NEW_DEVICE);

                } /*else if(errorCode == ErrorCodes.FAILED_ATTEMPT_LIMIT_RCHD) {
                mLoginButton.setEnabled(true);
                DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                        .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                MerchantUser.reset();

            }*/ else {
                    mLoginButton.setEnabled(true);
                    // Show error notification dialog
                    DialogFragmentWrapper.createNotification(AppConstants.loginFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                    MerchantUser.reset();
                }

            } else if (operation == MyRetainedFragment.REQUEST_GENERATE_MERCHANT_PWD) {
                AppCommonUtil.cancelProgressDialog(true);
                if (errorCode == ErrorCodes.NO_ERROR) {
                    // Show success notification dialog
                    DialogFragmentWrapper.createNotification(AppConstants.pwdGenerateSuccessTitle, AppConstants.genericPwdGenerateSuccessMsg, false, false)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                } else if (errorCode == ErrorCodes.OP_SCHEDULED) {
                    // Show success notification dialog
                    //Integer mins = MyGlobalSettings.getMchntPasswdResetMins() + GlobalSettingConstants.MERCHANT_PASSWORD_RESET_TIMER_INTERVAL;
                    String msg = String.format(AppConstants.pwdGenerateSuccessMsg, String.valueOf(AppCommonUtil.mErrorParams.opScheduledMins));
                    DialogFragmentWrapper.createNotification(AppConstants.pwdGenerateSuccessTitle, msg, false, false)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                } else if (errorCode == ErrorCodes.DUPLICATE_ENTRY) {
                    // Old request is already pending
                    Integer mins = MyGlobalSettings.getMchntPasswdResetMins() + GlobalSettingConstants.MERCHANT_PASSWORD_RESET_TIMER_INTERVAL;
                    String msg = String.format(AppConstants.pwdGenerateDuplicateRequestMsg, mins);
                    DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, msg, false, true)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                } else {
                    // Show error notification dialog
                    DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                }
                mProcessingResetPasswd = false;

            } else if (operation == MyRetainedFragment.REQUEST_FORGOT_ID) {
                AppCommonUtil.cancelProgressDialog(true);
                if (errorCode == ErrorCodes.NO_ERROR) {
                    // Show success notification dialog
                    DialogFragmentWrapper.createNotification(AppConstants.defaultSuccessTitle, AppConstants.forgotIdSuccessMsg, false, false)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                } else {
                    // Show error notification dialog
                    DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                            .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                }
            }
        } catch (Exception e) {
            AppCommonUtil.cancelProgressDialog(true);
            LogMy.e(TAG, "Exception in LoginActivity:onBgProcessResponse: "+operation+": "+errorCode, e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    private void makeForgotIdPasswordLink()
    {
        mProcessingResetPasswd = false;
        SpannableString forgotPrompt = new SpannableString( getString( R.string.Forgot_id_passwd_label ) );

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
                    int errorCode = ValidationHelper.validateMerchantId(mLoginId);
                    if(errorCode==ErrorCodes.NO_ERROR) {
                        // Ask for confirmation and the PIN too
                        PasswdResetDialog dialog = new PasswdResetDialog();
                        dialog.show(getFragmentManager(), DIALOG_PASSWD_RESET);
                    } else {
                        mIdTextRes.setError(AppCommonUtil.getErrorDesc(errorCode));
                        mProcessingResetPasswd = false;
                    }
                } else {
                    AppCommonUtil.toast(LoginActivity.this, AppConstants.toastInProgress);
                }
            }
        };

        String linkText = getString( R.string.Forgot_passwd_link );
        int linkStartIndex = forgotPrompt.toString().indexOf( linkText );
        int linkEndIndex = linkStartIndex + linkText.length();
        forgotPrompt.setSpan(clickableSpan, linkStartIndex, linkEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Cliackable span for 'User Id'
        ClickableSpan clickableSpanId = new ClickableSpan()
        {
            @Override
            public void onClick( View widget )
            {
                ForgotIdDialog dialog = new ForgotIdDialog();
                dialog.show(getFragmentManager(), DIALOG_FORGOT_ID);
            }
        };

        String idLinkText = getString( R.string.Forgot_id_link );
        int linkStartIndexId = forgotPrompt.toString().indexOf( idLinkText );
        int linkEndIndexId = linkStartIndexId + idLinkText.length();
        forgotPrompt.setSpan(clickableSpanId, linkStartIndexId, linkEndIndexId, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView promptView = (TextView) findViewById( R.id.link_forgot_id_passwd );
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
                    // TODO: show 'FAQ' only for non-login section
                    Support.setUserIdentifier(mLoginId);
                    Support.showFAQSection(LoginActivity.this,"1");
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
                Intent intent = new Intent(LoginActivity.this, SingleWebViewActivity.class );
                intent.putExtra(SingleWebViewActivity.INTENT_EXTRA_URL, MyGlobalSettings.getTermsUrl());
                startActivity(intent);
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
    public void onPasswdResetData(String dob) {
        if(dob!=null) {
            // show progress dialog
            AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
            mWorkFragment.generatePassword(dob, AppCommonUtil.getDeviceId(this), mLoginId);
        } else {
            mProcessingResetPasswd = false;
        }
    }

    @Override
    public void onForgotIdData(String mobileNum) {
        if(mobileNum!=null) {
            // show progress dialog
            AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
            mWorkFragment.forgotId(mobileNum, AppCommonUtil.getDeviceId(this));
        }
    }

    private void onLoginSuccess() {
        LogMy.d(TAG, "In onLoginSuccess");
        // Store latest successful login userid to preferences
        PreferenceManager.getDefaultSharedPreferences(LoginActivity.this)
                .edit()
                .putString(AppConstants.PREF_LOGIN_ID, mLoginId)
                .apply();

        // Global settings should be available by now
        AppCommonUtil.storeGSLocally(LoginActivity.this);

        // delete local files - if required
        AppCommonUtil.delLocalFiles(MerchantUser.getInstance().getMerchant().getDelLocalFilesReq(), this);
        startCbFrag();
    }

    private void startCbFrag() {
        // turn on fullscreen mode, which was set off in OnCreate
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        //Start Cashback Activity
        Intent intent = new Intent( this, CashbackActivity.class );
        intent.putExtra(CashbackActivity.INTENT_EXTRA_USER_TOKEN, MerchantUser.getInstance().getUserToken());
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
        mIdTextRes = (EditText) findViewById(R.id.input_merchant_id);
        mPasswdTextRes = (EditText) findViewById(R.id.input_password);
        mLoginButton = (Button) findViewById(R.id.btn_login);
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
        AppCommonUtil.setUserType(DbConstants.USER_TYPE_MERCHANT);
        if(AppConstants.IS_PROD_BUILD) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        mWorkFragment.setResumeOk(true);
    }

    @Override
    public void onBackPressed() {
        if(!mWorkFragment.getResumeOk()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onBgThreadCreated() {
        // nothing to do
    }
}

/**
 * Makes registration link clickable and assigns it a click listener.
 */
    /*
    private void makeRegistrationLink()
    {
        SpannableString registrationPrompt = new SpannableString( getString( R.string.Register_label ) );

        ClickableSpan clickableSpan = new ClickableSpan()
        {
            @Override
            public void onClick( View widget )
            {
                startRegistrationActivity();
            }
        };

        String linkText = getString( R.string.register_link );
        int linkStartIndex = registrationPrompt.toString().indexOf( linkText );
        int linkEndIndex = linkStartIndex + linkText.length();
        registrationPrompt.setSpan(clickableSpan, linkStartIndex, linkEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView registerPromptView = (TextView) findViewById( R.id.link_register );
        registerPromptView.setText(registrationPrompt);
        registerPromptView.setMovementMethod(LinkMovementMethod.getInstance());
    }*/

/**
 * Sends a request for registration to RegistrationActivity,
 * expects for result in onActivityResult.
 */
    /*
    public void startRegistrationActivity()
    {
        Intent registrationIntent = new Intent( this, RegisterMerchantActivity.class );
        startActivityForResult(registrationIntent, REGISTER_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REGISTER_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                mMerchantUser = MerchantUser.getInstance();
                //mIdTextRes.setText(MerchantUser.getInstance().getUser_id());
                //mPasswdTextRes.requestFocus();
            }
        }
    }*/

