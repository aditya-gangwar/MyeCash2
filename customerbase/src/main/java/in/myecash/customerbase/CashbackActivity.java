package in.myecash.customerbase;

/**
 * Created by adgangwa on 23-02-2016.
 */

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import in.myecash.commonbase.OtpPinInputDialog;
import in.myecash.customerbase.entities.CustomerUser;
import in.myecash.customerbase.helper.MyRetainedFragment;
import in.myecash.commonbase.PasswdChangeDialog;
import in.myecash.commonbase.constants.AppConstants;
import in.myecash.commonbase.constants.CommonConstants;
import in.myecash.commonbase.constants.DbConstants;
import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.models.Customers;
import in.myecash.commonbase.utilities.AppAlarms;
import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.DateUtil;
import in.myecash.commonbase.utilities.DialogFragmentWrapper;
import in.myecash.commonbase.utilities.LogMy;


public class CashbackActivity extends AppCompatActivity implements
        MyRetainedFragment.RetainedFragmentIf, DialogFragmentWrapper.DialogFragmentWrapperIf,
        PasswdChangeDialog.PasswdChangeDialogIf, MobileChangeDialog.MobileChangeDialogIf,
        OtpPinInputDialog.OtpPinInputDialogIf {

    private static final String TAG = "CashbackActivity";

    public static final String INTENT_EXTRA_USER_TOKEN = "extraUserToken";

    private static final String RETAINED_FRAGMENT = "workCashback";
    private static final String MERCHANT_LIST_FRAG = "CustomerListFrag";

    private static final String DIALOG_BACK_BUTTON = "dialogBackButton";
    private static final String DIALOG_CHANGE_PASSWORD = "dialogChangePasswd";
    private static final String DIALOG_CHANGE_MOBILE = "dialogChangeMobile";
    private static final String DIALOG_PIN_CHANGE_MOBILE = "dialogPinChangeMobile";

    private static final String DIALOG_LOGOUT = "dialogLogout";
    private static final String DIALOG_CUSTOMER_OP_RESET_PIN = "dialogResetPin";
    private static final String DIALOG_CUSTOMER_OP_OTP = "dialogCustomerOpOtp";
    private static final String DIALOG_CUSTOMER_DETAILS = "dialogCustomerDetails";
    private static final String DIALOG_MERCHANT_DETAILS = "dialogMerchantDetails";

    private final SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

    MyRetainedFragment mWorkFragment;
    FragmentManager mFragMgr;
    // this will never be null, as it only gets destroyed with cashback activity itself
    MobileNumberFragment mMobileNumFragment;

    private Toolbar mToolbar;
    private DrawerLayout mDrawer;
    ActionBarDrawerToggle mDrawerToggle;
    NavigationView mNavigationView;

    private AppCompatImageView mTbImage;
    private EditText mTbTitle;
    private EditText mTbTitle2;
    private LinearLayout mTbLayoutSubhead1;
    private EditText mTbSubhead1Text1;
    private EditText mTbSubhead1Text2;

    private CustomerUser mCustomerUser;
    private Customers mCustomer;

    // Activity state members: These are to be saved for restore in event of activity recreation
    boolean mExitAfterLogout;
    int mLastMenuItemId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cashback);

        mCustomerUser = CustomerUser.getInstance();
        mCustomer = mCustomerUser.getCustomer();
        mFragMgr = getFragmentManager();

        // Initialize retained fragment before other fragments
        // Check to see if we have retained the worker fragment.
        mWorkFragment = (MyRetainedFragment)mFragMgr.findFragmentByTag(RETAINED_FRAGMENT);
        // If not retained (or first time running), we need to create it.
        if (mWorkFragment == null) {
            mWorkFragment = new MyRetainedFragment();
            // Tell it who it is working with.
            mFragMgr.beginTransaction().add(mWorkFragment, RETAINED_FRAGMENT).commit();
        }

        // store passed logged in user token
        mWorkFragment.mUserToken = getIntent().getStringExtra(INTENT_EXTRA_USER_TOKEN);

        if(savedInstanceState!=null) {
            mExitAfterLogout = savedInstanceState.getBoolean("mExitAfterLogout");
            mLastMenuItemId = savedInstanceState.getInt("mLastMenuItemId");
        }

        // Setup a toolbar to replace the action bar.
        initToolbar();
        updateTbForCustomer();

        // Setup navigation drawer
        initNavDrawer();

        // setup mobile number fragment
        startMobileNumFragment();
    }

    @Override
    public void setDrawerState(boolean isEnabled) {
        LogMy.d(TAG, "In setDrawerState: " + isEnabled);

        if ( isEnabled ) {
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            mDrawerToggle.setDrawerIndicatorEnabled(true);
            mDrawerToggle.syncState();
        }
        else {
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            mDrawerToggle.setDrawerIndicatorEnabled(false);
            // show back arrow
            mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);

            mDrawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mDrawerToggle.isDrawerIndicatorEnabled()) {
                        onBackPressed();
                    }
                }
            });
            mDrawerToggle.syncState();
        }
    }

    private void initNavDrawer() {
        LogMy.d(TAG, "In initNavDrawer");

        // Find our drawer view
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        //NavigationView navigationView;
        mNavigationView = (NavigationView) findViewById(R.id.nav_view);

        View headerLayout = mNavigationView.getHeaderView(0);
        TextView headerTitle = (TextView)headerLayout.findViewById(R.id.drawer_header_title);
        headerTitle.setText(mCustomer.getName());
        TextView headerMobile = (TextView)headerLayout.findViewById(R.id.drawer_header_mobile);
        headerMobile.setText(mCustomer.getMobile_num());

        // Tie DrawerLayout events to the ActionBarToggle
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawer, mToolbar, R.string.drawer_open,  R.string.drawer_close);
        mDrawer.addDrawerListener(mDrawerToggle);

        mNavigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
                });

        setDrawerState(true);
    }

    private void initToolbar() {
        LogMy.d(TAG, "In initToolbar");
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        initTbViews();

        mTbImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // show customer details dialog
                CustomerDetailsDialog dialog = CustomerDetailsDialog.newInstance(-1);
                dialog.show(mFragMgr, DIALOG_CUSTOMER_DETAILS);
            }
        });

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void selectDrawerItem(MenuItem item) {

        mLastMenuItemId = item.getItemId();
        int i = item.getItemId();

        // Not able to use switch() - as not allowed in library modules
        if (i == R.id.menu_dashboard) {

        } else if (i == R.id.menu_txns) {

        } else if (i == R.id.menu_change_mobile) {
            MobileChangeDialog dialog = new MobileChangeDialog();
            dialog.show(mFragMgr, DIALOG_CHANGE_MOBILE);

        } else if (i == R.id.menu_change_pin) {

        } else if (i == R.id.menu_change_passwd) {
            PasswdChangeDialog dialog = new PasswdChangeDialog();
            dialog.show(getFragmentManager(), DIALOG_CHANGE_PASSWORD);

        } else if (i == R.id.menu_faq) {
        } else if (i == R.id.menu_terms) {
        } else if (i == R.id.menu_contact_us) {
        } else {
        }

        // Highlight the selected item has been done by NavigationView
        //item.setChecked(true);
        // Set action bar title
        //setTitle(item.getTitle());
        // Close the navigation drawer
        mDrawer.closeDrawers();

    }

    private void updateTbForCustomer() {
        mTbLayoutSubhead1.setVisibility(View.VISIBLE);

        // no error case: all cashback values available
        mTbTitle.setText(mCustomer.getMobile_num());
        mTbTitle2.setVisibility(View.GONE);
        if(mCustomer.getAdmin_status()!=DbConstants.USER_STATUS_ACTIVE ) {
            mTbTitle2.setVisibility(View.VISIBLE);
            //String custName = "~ "+mWorkFragment.mCurrCashback.getCashback().getCustomer().getName();
            mTbTitle2.setText(DbConstants.userStatusDesc[mCustomer.getAdmin_status()]);

        } else if(mCustomer.getMembership_card().getStatus() != DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED) {

            switch(mCustomer.getMembership_card().getStatus()) {
                case DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED:
                    // do nothing
                    break;
                case DbConstants.CUSTOMER_CARD_STATUS_REMOVED:
                    mTbTitle2.setVisibility(View.VISIBLE);
                    mTbTitle2.setText(DbConstants.cardStatusDescriptions[mCustomer.getMembership_card().getStatus()]);
                    break;
                default:
                    //raise alarm
                    Map<String,String> params = new HashMap<>();
                    params.put("CustomerId",mCustomer.getMobile_num());
                    params.put("CardId",mCustomer.getMembership_card().getCard_id());
                    params.put("CardStatus",String.valueOf(mCustomer.getMembership_card().getStatus()));
                    AppAlarms.invalidCardState(mCustomer.getPrivate_id(),DbConstants.USER_TYPE_CUSTOMER,"updateTbForCustomer",params);
            }
        }

        mTbSubhead1Text1.setText(AppCommonUtil.getAmtStr(mWorkFragment.mCurrCashback.getCurrClBalance()));
        //mTbSubhead1Divider.setVisibility(View.VISIBLE);
        //mTbSubhead1Text2.setVisibility(View.VISIBLE);
        mTbSubhead1Text2.setText(AppCommonUtil.getAmtStr(mWorkFragment.mCurrCashback.getCurrCbBalance()));
    }

    private void initTbViews() {
        mTbImage = (AppCompatImageView) mToolbar.findViewById(R.id.tb_image) ;
        mTbTitle = (EditText) mToolbar.findViewById(R.id.tb_title) ;
        mTbTitle2 = (EditText) mToolbar.findViewById(R.id.tb_title_2) ;
        mTbLayoutSubhead1 = (LinearLayout) mToolbar.findViewById(R.id.tb_layout_subhead1) ;
        mTbSubhead1Text1 = (EditText) mToolbar.findViewById(R.id.tb_curr_cashload) ;
        mTbSubhead1Text2 = (EditText) mToolbar.findViewById(R.id.tb_curr_cashback) ;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBgProcessResponse(int errorCode, int operation) {
        switch(operation) {
            case MyRetainedFragment.REQUEST_LOGOUT:
                onLogoutResponse(errorCode);
                break;
            case MyRetainedFragment.REQUEST_CHANGE_PASSWD:
                passwordChangeResponse(errorCode);
                break;
        }
    }

    @Override
    public void onPinOtp(String pinOrOtp, String tag) {
        if(tag.equals(DIALOG_PIN_CHANGE_MOBILE)) {
            mWorkFragment.mVerifyParamMobileChange = pinOrOtp;
            // dispatch customer op for execution
            changeMobileNum();
        }
    }

    @Override
    public void changeMobileNumOk(String newMobile) {
        mWorkFragment.mNewMobileNum = newMobile;

        // ask for customer PIN
        String txnDetail = String.format(AppConstants.msgChangeCustMobilePin,newMobile);
        OtpPinInputDialog dialog = OtpPinInputDialog.newInstance(AppConstants.titleChangeCustMobilePin, txnDetail, "Enter PIN");
        dialog.show(mFragMgr, DIALOG_PIN_CHANGE_MOBILE);
    }

    @Override
    public void changeMobileNumOtp(String otp) {
        LogMy.d(TAG, "In changeMobileNumOtp: " + otp);
        mWorkFragment.mOtpMobileChange = otp;
        changeMobileNum();
    }

    private void changeMobileNum() {
        int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(this);
        if ( resultCode != ErrorCodes.NO_ERROR) {
            // Show error notification dialog
            DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, ErrorCodes.appErrorDesc.get(resultCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        } else {
            AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
            mWorkFragment.changeMobileNum();
        }
    }

    private void logoutCustomer() {
        int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(this);
        if ( resultCode != ErrorCodes.NO_ERROR) {
            // Show error notification dialog
            DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, ErrorCodes.appErrorDesc.get(resultCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        } else {
            // show progress dialog
            AppCommonUtil.showProgressDialog(this, AppConstants.progressLogout);
            mWorkFragment.logoutCustomer();
        }
    }

    private void onLogoutResponse(int errorCode) {
        LogMy.d(TAG, "In onLogoutResponse: " + errorCode);

        AppCommonUtil.cancelProgressDialog(true);
        CustomerUser.reset();
        finish();
    }

    @Override
    public void onPasswdChangeData(String oldPasswd, String newPassword) {
        AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
        mWorkFragment.changePassword(oldPasswd, newPassword);
    }

    private void passwordChangeResponse(int errorCode) {
        LogMy.d(TAG, "In passwordChangeResponse: " + errorCode);
        AppCommonUtil.cancelProgressDialog(true);

        if(errorCode==ErrorCodes.NO_ERROR) {
            DialogFragmentWrapper.createNotification(AppConstants.pwdChangeSuccessTitle, AppConstants.pwdChangeSuccessMsg, false, false)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
            logoutCustomer();

        } else {
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, ErrorCodes.appErrorDesc.get(errorCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    public void onDialogResult(String tag, int indexOrResultCode, ArrayList<Integer> selectedItemsIndexList) {
        LogMy.d(TAG, "In onDialogResult: " + tag);

        if (tag.equals(DIALOG_BACK_BUTTON)) {
            mExitAfterLogout = true;
            logoutCustomer();
        }/* else if(tag.equals(DIALOG_LOGOUT)) {
            mExitAfterLogout = false;
            logoutMerchant();
        }*/
    }

    private void startMerchantListFrag() {
        if (mFragMgr.findFragmentByTag(MERCHANT_LIST_FRAG) == null) {
            //setDrawerState(false);

            Fragment fragment = new CustomerListFragment();
            FragmentTransaction transaction = mFragMgr.beginTransaction();

            // Add over the existing fragment
            transaction.replace(R.id.fragment_container_1, fragment, MERCHANT_LIST_FRAG);
            transaction.addToBackStack(MERCHANT_LIST_FRAG);

            // Commit the transaction
            transaction.commit();
        }
    }

    @Override
    public MyRetainedFragment getRetainedFragment() {
        return mWorkFragment;
    }

    @Override
    public void onBackPressed() {
        LogMy.d(TAG,"In onBackPressed: "+mFragMgr.getBackStackEntryCount());

        if (this.mDrawer.isDrawerOpen(GravityCompat.START)) {
            this.mDrawer.closeDrawer(GravityCompat.START);
            return;
        }

        if (mMobileNumFragment.isVisible()) {
            DialogFragmentWrapper.createConfirmationDialog(AppConstants.exitGenTitle, AppConstants.exitAppMsg, false, false)
                    .show(mFragMgr, DIALOG_BACK_BUTTON);
        } else {
            mFragMgr.popBackStackImmediate();
            if(mMobileNumFragment.isVisible()) {
                LogMy.d(TAG,"Mobile num fragment visible");
                getReadyForNewTransaction();
            }
        }
    }

    // `onPostCreate` called when activity start-up is complete after `onStart()`
    // NOTE! Make sure to override the method with only a single `Bundle` argument
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        LogMy.d(TAG,"In onPostCreate: ");
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();

        if(savedInstanceState==null) {
            // activity is re-created and not just re-started
            // Archive txns (all but today's) once a day
            DateUtil todayMidnight = new DateUtil();
            todayMidnight.toMidnight();

            if(mCustomer.getLast_txn_archive()==null ||
                    mCustomer.getLast_txn_archive().getTime() < todayMidnight.getTime().getTime()) {
                mWorkFragment.archiveTxns();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        LogMy.d(TAG, "In onResume: ");
        super.onResume();
        if(AppCommonUtil.getProgressDialogMsg()!=null) {
            AppCommonUtil.showProgressDialog(this, AppCommonUtil.getProgressDialogMsg());
        }
        setDrawerState(true);
    }

    @Override
    protected void onPause() {
        LogMy.d(TAG,"In onPause: ");
        super.onPause();
        AppCommonUtil.cancelProgressDialog(false);
        setDrawerState(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CustomerUser.reset();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("mExitAfterLogout", mExitAfterLogout);
        outState.putInt("mLastMenuItemId", mLastMenuItemId);
    }
}
