package in.myecash.appagent;

import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import in.myecash.appagent.entities.AgentUser;
import in.myecash.appagent.helper.MyRetainedFragment;
import in.myecash.commonbase.constants.AppConstants;
import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.DialogFragmentWrapper;
import in.myecash.commonbase.utilities.LogMy;
import in.myecash.merchantbase.CashbackActivity;
import in.myecash.merchantbase.DashboardFragment;
import in.myecash.merchantbase.MerchantOpListFrag;
import in.myecash.merchantbase.entities.MerchantUser;

import java.util.ArrayList;

/**
 * Created by adgangwa on 18-07-2016.
 */
public class ActionsActivity extends AppCompatActivity implements
        MyRetainedFragment.RetainedFragmentIf, DialogFragmentWrapper.DialogFragmentWrapperIf,
        ActionsFragment.ActionsFragmentIf, PasswdChangeDialog.PasswdChangeDialogIf,
        SearchMerchantDialog.SearchMerchantDialogIf, MerchantDetailsFragment.MerchantDetailsFragmentIf,
        DisableMchntDialog.DisableMchntDialogIf
{

    private static final String TAG = "ActionsActivity";
    private static final String RETAINED_FRAGMENT = "retainedFragActions";
    private static final String ACTIONS_FRAGMENT = "actionsFragment";
    private static final String MCHNT_DETAILS_FRAGMENT = "mchntDetailsFragment";
    private static final String SETTINGS_LIST_FRAGMENT = "settingsListFragment";

    private static final String DIALOG_BACK_BUTTON = "dialogBackButton";
    private static final String DIALOG_CHANGE_BUTTON = "dialogChangeButton";
    private static final String DIALOG_SEARCH_MCHNT = "searchMchnt";

    // this will never be null, as it only gets destroyed with cashback activity itself
    ActionsFragment mActionsFragment;

    FragmentManager mFragMgr;
    MyRetainedFragment mWorkFragment;
    //private AgentUser mUser;

    boolean mExitAfterLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actions);

        //mUser = AgentUser.getInstance();

        if(savedInstanceState==null) {
            initToolbar(true);
        }

        // Initialize retained fragment before other fragments
        // Check to see if we have retained the worker fragment.
        mFragMgr = getFragmentManager();
        mWorkFragment = (MyRetainedFragment)mFragMgr.findFragmentByTag(RETAINED_FRAGMENT);
        // If not retained (or first time running), we need to create it.
        if (mWorkFragment == null) {
            LogMy.d(TAG, "Creating retained fragment instance");
            mWorkFragment = new MyRetainedFragment();
            mFragMgr.beginTransaction().add(mWorkFragment, RETAINED_FRAGMENT).commit();
        }

        startActionsFragment();
    }

    private void initToolbar(boolean isNewActivity) {
        LogMy.d(TAG, "In initToolbar");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actions);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);

        if(isNewActivity) {
            getSupportActionBar().setTitle("Home");
        }
    }

    @Override
    public void onBgProcessResponse(int errorCode, int operation) {
        switch(operation) {
            case MyRetainedFragment.REQUEST_LOGOUT:
                AppCommonUtil.cancelProgressDialog(true);
                AgentUser.reset();
                //Start Login Activity
                if(!mExitAfterLogout) {
                    Intent intent = new Intent( this, LoginActivity.class );
                    // clear this activity from backstack
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                finish();
                break;

            case MyRetainedFragment.REQUEST_CHANGE_PASSWD:
                AppCommonUtil.cancelProgressDialog(true);
                if(errorCode==ErrorCodes.NO_ERROR) {
                    DialogFragmentWrapper.createNotification(AppConstants.pwdChangeSuccessTitle, AppConstants.pwdChangeSuccessMsg, false, false)
                            .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
                    logoutAgent();
                } else {
                    DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, ErrorCodes.appErrorDesc.get(errorCode), false, true)
                            .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
                }
                break;

            case MyRetainedFragment.REQUEST_SEARCH_MERCHANT:
                AppCommonUtil.cancelProgressDialog(true);
                if(errorCode==ErrorCodes.NO_ERROR) {
                    mWorkFragment.mCurrMerchant = MerchantUser.getInstance().getMerchant();
                    startMchntDetailsFragment();
                    // show merchant details
                    /*
                    mWorkFragment.mCurrMerchant = MerchantUser.getInstance().getMerchant();
                    MerchantDetailsFragment dialog = new MerchantDetailsFragment();
                    dialog.show(getFragmentManager(), DIALOG_MCHNT_DETAILS);*/
                } else {
                    DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, ErrorCodes.appErrorDesc.get(errorCode), false, true)
                            .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
                }
                break;

            case MyRetainedFragment.REQUEST_DISABLE_MERCHANT:
                AppCommonUtil.cancelProgressDialog(true);
                if(errorCode==ErrorCodes.NO_ERROR) {
                    DialogFragmentWrapper.createNotification(AppConstants.defaultSuccessTitle, AppConstants.merchantDisableSuccessMsg, false, false)
                            .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
                } else {
                    DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, ErrorCodes.appErrorDesc.get(errorCode), false, true)
                            .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
                }
                break;
        }
    }

    @Override
    public void onDialogResult(String tag, int indexOrResultCode, ArrayList<Integer> selectedItemsIndexList) {
        if (tag.equals(DIALOG_BACK_BUTTON)) {
            mExitAfterLogout = true;
            // delete all app memory data
            if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
                if(!((ActivityManager)getSystemService(ACTIVITY_SERVICE))
                        .clearApplicationUserData()) {
                    LogMy.w(TAG,"Failed to clear application user data");
                }
            } else {
                LogMy.e(TAG,"Not clearing cache data - as API level is below 19.");
            }
            logoutAgent();
        }
    }

    private void logoutAgent() {
        int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(this);
        if ( resultCode != ErrorCodes.NO_ERROR) {
            // Show error notification dialog
            DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, ErrorCodes.appErrorDesc.get(resultCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
            //TODO: do handling here
        } else {
            // show progress dialog
            AppCommonUtil.showProgressDialog(this, AppConstants.progressLogout);
            mWorkFragment.logoutAgent();
        }
    }

    @Override
    public void onBackPressed() {
        int count = getFragmentManager().getBackStackEntryCount();
        LogMy.d(TAG, "In onBackPressed: " + count);

        /*
        if (count == 0) {
            super.onBackPressed();
        } else {
            getFragmentManager().popBackStackImmediate();
        }*/

        if (mActionsFragment.isVisible()) {
            DialogFragmentWrapper.createConfirmationDialog(AppConstants.exitGenTitle, AppConstants.exitAppMsg, false, false)
                    .show(mFragMgr, DIALOG_BACK_BUTTON);
        } else {
            mFragMgr.popBackStackImmediate();
            if(mActionsFragment.isVisible()) {
                LogMy.d(TAG,"Actions fragment visible");
                //getReadyForNewTransaction();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * react to the user tapping the back/up icon in the action bar
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // this takes the user 'back', as if they pressed the left-facing triangle icon on the main android toolbar.
                // if this doesn't work as desired, another possibility is to call `finish()` here.
                onBackPressed();
                break;
            case R.id.action_change_passwd:
                // Ask for confirmation and the PIN too
                PasswdChangeDialog dialog = new PasswdChangeDialog();
                dialog.show(getFragmentManager(), DIALOG_CHANGE_BUTTON);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void disableMerchant(String ticketId, String reason, String remarks) {
        AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
        mWorkFragment.disableMerchant(ticketId, reason, remarks);
    }

    @Override
    public void onPasswdChangeData(String oldPasswd, String newPassword) {
        AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
        mWorkFragment.changePassword(oldPasswd, newPassword);
    }

    @Override
    public MyRetainedFragment getRetainedFragment() {
        return mWorkFragment;
    }


    @Override
    public void launchMchntApp() {
        // pseudo login done - launch cashback activity
        //Start Cashback Activity
        Intent intent = new Intent( this, CashbackActivity.class );
        intent.putExtra(CashbackActivity.INTENT_EXTRA_USER_TOKEN, AgentUser.getInstance().getUserToken());
        // clear Login activity from backstack
        //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void onActionBtnClick(String action) {
        switch(action) {
            case ActionsFragment.MERCHANT_REGISTER:
                startMerchantRegisterActivity();
                break;
            case ActionsFragment.MERCHANT_SEARCH:
                SearchMerchantDialog dialog = new SearchMerchantDialog();
                dialog.show(getFragmentManager(), DIALOG_SEARCH_MCHNT);
                break;
            case ActionsFragment.OTHER_GLOBAL_SETTINGS:
                startSettingsListFrag();
                break;
        }
    }

    @Override
    public void onInputData(String value, boolean searchById) {
        if(value!=null && !value.isEmpty()) {
            AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
            mWorkFragment.mCurrMerchant = null;
            mWorkFragment.searchMerchant(value, searchById);
        }
    }

    /*
     * Activity and Fragment start fxs
     */
    private void startActionsFragment() {
        if (mFragMgr.findFragmentByTag(ACTIONS_FRAGMENT) == null) {
            //setDrawerState(false);
            mActionsFragment = new ActionsFragment();
            mFragMgr.beginTransaction()
                    .add(R.id.fragment_container, mActionsFragment, ACTIONS_FRAGMENT)
                    .addToBackStack(ACTIONS_FRAGMENT)
                    .commit();
        }
    }

    private void startMchntDetailsFragment() {
        if (mFragMgr.findFragmentByTag(MCHNT_DETAILS_FRAGMENT) == null) {
            LogMy.d(TAG, "Creating new mchnt details fragment");
            Fragment fragment = new MerchantDetailsFragment();
            FragmentTransaction transaction = mFragMgr.beginTransaction();

            // Add over the existing fragment
            transaction.replace(R.id.fragment_container, fragment, MCHNT_DETAILS_FRAGMENT);
            transaction.addToBackStack(MCHNT_DETAILS_FRAGMENT);

            // Commit the transaction
            transaction.commit();
        }
    }

    private void startSettingsListFrag() {
        if (mFragMgr.findFragmentByTag(SETTINGS_LIST_FRAGMENT) == null) {
            LogMy.d(TAG, "Creating new setiings list fragment");
            Fragment fragment = new GlobalSettingsListFrag();
            FragmentTransaction transaction = mFragMgr.beginTransaction();

            // Add over the existing fragment
            transaction.replace(R.id.fragment_container, fragment, SETTINGS_LIST_FRAGMENT);
            transaction.addToBackStack(SETTINGS_LIST_FRAGMENT);

            // Commit the transaction
            transaction.commit();
        }
    }

    private void startMerchantRegisterActivity() {
        Intent registrationIntent = new Intent( this, RegisterMerchantActivity.class );
        startActivity(registrationIntent);
    }

}

