package in.myecash.customerbase;

/**
 * Created by adgangwa on 23-02-2016.
 */

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import in.myecash.commonbase.OtpPinInputDialog;
import in.myecash.commonbase.entities.MyCashback;
import in.myecash.commonbase.entities.MyCustomer;
import in.myecash.commonbase.entities.MyGlobalSettings;
import in.myecash.commonbase.entities.MyMerchant;
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

    //TODO: change this to 100 in production
    private static final int CSV_LINES_BUFFER = 5;

    private static final String RETAINED_FRAGMENT = "workCashback";
    private static final String MERCHANT_LIST_FRAG = "CustomerListFrag";

    private static final String DIALOG_BACK_BUTTON = "dialogBackButton";
    private static final String DIALOG_CHANGE_PASSWORD = "dialogChangePasswd";
    private static final String DIALOG_CHANGE_MOBILE = "dialogChangeMobile";
    private static final String DIALOG_PIN_CHANGE_MOBILE = "dialogPinChangeMobile";
    private static final String DIALOG_CUSTOMER_DETAILS = "dialogCustomerDetails";

    private static final String DIALOG_LOGOUT = "dialogLogout";
    private static final String DIALOG_CUSTOMER_OP_RESET_PIN = "dialogResetPin";
    private static final String DIALOG_CUSTOMER_OP_OTP = "dialogCustomerOpOtp";
    private static final String DIALOG_MERCHANT_DETAILS = "dialogMerchantDetails";

    private final SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

    MyRetainedFragment mRetainedFragment;
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
    long mGetCbSince;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cashback);

        mCustomerUser = CustomerUser.getInstance();
        mCustomer = mCustomerUser.getCustomer();
        mFragMgr = getFragmentManager();

        // Initialize retained fragment before other fragments
        // Check to see if we have retained the worker fragment.
        mRetainedFragment = (MyRetainedFragment)mFragMgr.findFragmentByTag(RETAINED_FRAGMENT);
        // If not retained (or first time running), we need to create it.
        if (mRetainedFragment == null) {
            mRetainedFragment = new MyRetainedFragment();
            // Tell it who it is working with.
            mFragMgr.beginTransaction().add(mRetainedFragment, RETAINED_FRAGMENT).commit();
        }

        // store passed logged in user token
        mRetainedFragment.mUserToken = getIntent().getStringExtra(INTENT_EXTRA_USER_TOKEN);

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
        startMchntListFragment();
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
                CustomerDetailsDialog dialog = new CustomerDetailsDialog();
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
            // show mobile change dialog
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
            //String custName = "~ "+mRetainedFragment.mCurrCashback.getCashback().getCustomer().getName();
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

        mTbSubhead1Text1.setText(AppCommonUtil.getAmtStr(mRetainedFragment.mCurrCashback.getCurrClBalance()));
        //mTbSubhead1Divider.setVisibility(View.VISIBLE);
        //mTbSubhead1Text2.setVisibility(View.VISIBLE);
        mTbSubhead1Text2.setText(AppCommonUtil.getAmtStr(mRetainedFragment.mCurrCashback.getCurrCbBalance()));
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
            case MyRetainedFragment.REQUEST_CHANGE_MOBILE:
                onChangeMobileResponse(errorCode);
                break;
            case MyRetainedFragment.REQUEST_FETCH_CB:
                onFetchCbResponse(errorCode);
                break;
        }
    }

    private void onFetchCbResponse(int errorCode) {
        LogMy.d(TAG, "In onChangeMobileResponse: " + errorCode);
        AppCommonUtil.cancelProgressDialog(true);

        if(errorCode==ErrorCodes.NO_ERROR) {
            // fetched data should now be available in mRetainedFragment.mLastFetchCashbacks
            if(mRetainedFragment.mLastFetchCashbacks.size() > 0 ) {

                if(mRetainedFragment.mCashbacks == null) {
                    // no data in memory - check for the file
                    if(mGetCbSince != 0) {
                        // some data was there in file earlier - read the same
                        if(!processCbDataFile(true)) {
                            // if some error in reading file - fetch all records from scratch
                            mGetCbSince = 0;
                            fetchCbData();
                            return;
                        }
                    } else {
                        // records fetched from scratch - no need to check for file
                        LogMy.d(TAG,"All fetched Cb records are from scratch: "+mRetainedFragment.mLastFetchCashbacks.size());
                        mRetainedFragment.mCashbacks = new TreeMap<>();
                    }
                }
                // add all fetched records to the 'cashback store'
                for (MyCashback cb :
                        mRetainedFragment.mLastFetchCashbacks) {
                    mRetainedFragment.mCashbacks.put(cb.getMerchantId(), cb);
                }
                // reset to null
                mRetainedFragment.mLastFetchCashbacks = null;
                // set time for current set of records
                mRetainedFragment.mCbsUpdateTime = new Date();

                // write whole 'cashback store' to the file
                writeCbsToFile();
            }

            if(mRetainedFragment.mCashbacks == null) {
                // No data available locally - none fetched from DB
                // The customer has no cashbacks yet
                startNoCashbackFrag();
            } else {
                startMerchantListFrag();
            }

        } else {
            // Failed to fetch new data - show old one, if available
            //DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, ErrorCodes.appErrorDesc.get(errorCode), false, true)
            //        .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);

            // same as above - try to read data from file
            if(mRetainedFragment.mCashbacks == null) {
                // no data in memory - check for the file
                if(mGetCbSince != 0) {
                    // some data was there in file earlier - read the same
                    if(!processCbDataFile(true)) {
                        // if some error in reading file - fetch all records from scratch
                        mGetCbSince = 0;
                        fetchCbData();
                        return;
                    }
                }
            }

            if(mRetainedFragment.mCashbacks == null) {
                // No data available locally - error fetching data from DB
                startErrorFrag();
            } else {
                AppCommonUtil.toast(this, "Failed to fetch latest data");
                startMerchantListFrag();
            }
        }
    }

    private void writeCbsToFile() {
        LogMy.d(TAG,"In createCsvReport");

        FileOutputStream outputStream = null;
        try {
            String fileName = AppCommonUtil.getCashbackFileName(mCustomerUser.getCustomer().getMobile_num());

            StringBuilder sb = new StringBuilder();
            int cnt = mRetainedFragment.mCashbacks.size();
            for(int i=0; i<cnt; i++) {
                MyCashback cb = mRetainedFragment.mCashbacks.get(i);
                String csvStr = MyCashback.toCsvString(cb.getCurrCashback());
                sb.append(csvStr).append(CommonConstants.CSV_NEWLINE);
            }

            outputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(sb.toString().getBytes());
            outputStream.close();

        } catch(Exception e) {
            LogMy.e(TAG,"exception in writeCbsToFile: "+e.toString(),e);
            if(outputStream!=null) {
                try {
                    outputStream.close();
                } catch(Exception ex){
                    // ignore exception
                }
            }
        }
    }

    private void onChangeMobileResponse(int errorCode) {
        LogMy.d(TAG, "In onChangeMobileResponse: " + errorCode);
        AppCommonUtil.cancelProgressDialog(true);

        if(errorCode==ErrorCodes.NO_ERROR) {
            DialogFragmentWrapper.createNotification(AppConstants.defaultSuccessTitle, AppConstants.mobileChangeSuccessMsg, false, false)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
            // merchant operation success, reset to null
            changeMobileParamsReset();

        } else if(errorCode==ErrorCodes.OTP_GENERATED) {
            // OTP sent successfully to new mobile, ask for the same
            // show the 'mobile change dialog' again
            MobileChangeDialog dialog = new MobileChangeDialog();
            dialog.show(mFragMgr, DIALOG_CHANGE_MOBILE);

        } else {
            // reset in case of any error
            changeMobileParamsReset();
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, ErrorCodes.appErrorDesc.get(errorCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }
    private void changeMobileParamsReset() {
        mRetainedFragment.mNewMobileNum = null;
        mRetainedFragment.mVerifyParamMobileChange = null;
        mRetainedFragment.mOtpMobileChange = null;
    }

    @Override
    public void onPinOtp(String pinOrOtp, String tag) {
        if(tag.equals(DIALOG_PIN_CHANGE_MOBILE)) {
            mRetainedFragment.mVerifyParamMobileChange = pinOrOtp;
            // dispatch customer op for execution
            changeMobileNum();
        }
    }

    @Override
    public void changeMobileNumOk(String newMobile) {
        mRetainedFragment.mNewMobileNum = newMobile;

        // ask for customer PIN
        String txnDetail = String.format(AppConstants.msgChangeCustMobilePin,newMobile);
        OtpPinInputDialog dialog = OtpPinInputDialog.newInstance(AppConstants.titleChangeCustMobilePin, txnDetail, "Enter PIN");
        dialog.show(mFragMgr, DIALOG_PIN_CHANGE_MOBILE);
    }

    @Override
    public void changeMobileNumOtp(String otp) {
        LogMy.d(TAG, "In changeMobileNumOtp: " + otp);
        mRetainedFragment.mOtpMobileChange = otp;
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
            mRetainedFragment.changeMobileNum();
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
            mRetainedFragment.logoutCustomer();
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
        mRetainedFragment.changePassword(oldPasswd, newPassword);
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
        return mRetainedFragment;
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
                mRetainedFragment.archiveTxns();
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

        boolean fetchData = false;
        // read file, if 'cashback store' is empty
        if(mRetainedFragment.mCashbacks == null) {
            if(!processCbDataFile(false)) {
                fetchData = true;
            }
        } else if(custStatsRefreshReq(mRetainedFragment.mCbsUpdateTime.getTime())) {
            // data in memory, but has expired
            fetchData = true;
            mGetCbSince = mRetainedFragment.mCbsUpdateTime.getTime();
        }
        if(fetchData) {
            // fetch data from DB
            fetchCbData();
        }

        setDrawerState(true);
    }

    private void fetchCbData() {
        LogMy.d(TAG,"Fetching cb data from backend: "+mGetCbSince);
        int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(this);
        if ( resultCode != ErrorCodes.NO_ERROR) {
            // Show error notification dialog
            DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, ErrorCodes.appErrorDesc.get(resultCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        } else {
            AppCommonUtil.showProgressDialog(this, AppCommonUtil.getProgressDialogMsg());
            mRetainedFragment.fetchCashback(mGetCbSince);
        }
    }

    // Reads and process local cashback data file
    private boolean processCbDataFile(boolean ignoreTime) {
        LogMy.d(TAG,"In processCbDataFile: "+ignoreTime);
        String fileName = AppCommonUtil.getCashbackFileName(mCustomerUser.getCustomer().getMobile_num());

        try {
            InputStream inputStream = openFileInput(fileName);
            if ( inputStream != null ) {
                int lineCnt = 0;

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    if(lineCnt==0) {
                        // first line is header giving file creation epoch time
                        String[] csvFields = receiveString.split(CommonConstants.CSV_DELIMETER);
                        long fileCreateTime = Long.parseLong(csvFields[0]);
                        if(custStatsRefreshReq(fileCreateTime) &&
                                !ignoreTime ) {
                            // file is older than configured no refresh duration
                            mGetCbSince = fileCreateTime;
                            // don't read file further, will be done after updated records are fetched from DB
                            return false;
                        } else {
                            mRetainedFragment.mCashbacks = new TreeMap<>();
                            mRetainedFragment.mCbsUpdateTime = new Date(fileCreateTime);
                        }
                    } else {
                        // ignore empty lines
                        if(!receiveString.equals(CommonConstants.CSV_NEWLINE)) {
                            processCbCsvRecord(receiveString);
                        }
                    }

                    lineCnt++;
                }
                inputStream.close();
                LogMy.d(TAG,"Processed "+lineCnt+" lines from "+fileName);
            } else {
                String error = "openFileInput returned null for Cashback CSV file: "+fileName;
                LogMy.e(TAG, error);
                throw new FileNotFoundException(error);
            }
        } catch (Exception ex) {
            if(!(ex instanceof FileNotFoundException)) {
                LogMy.e(TAG,"Exception while reading cb file: "+ex.toString(),ex);
            }
            // reset all, fetch all data fresh
            mRetainedFragment.mCashbacks = null;
            mRetainedFragment.mCbsUpdateTime = null;
            mGetCbSince = 0;
            return false;
        }

        return true;
    }

    private void processCbCsvRecord(String csvString) {
        MyCashback cb = new MyCashback();
        cb.init(csvString, false);
        mRetainedFragment.mCashbacks.put(cb.getMerchantId(), cb);
        LogMy.d(TAG,"Added new item in cashback store: "+mRetainedFragment.mCashbacks.size());
    }

    private boolean custStatsRefreshReq(long lastUpdate) {

        if(MyGlobalSettings.getCustNoRefreshHrs()==24) {
            // 24 is treated as special case as 'once in a day'
            DateUtil todayMidnight = (new DateUtil()).toMidnight();
            if(lastUpdate < todayMidnight.getTime().getTime()) {
                // Last update was not today
                return true;
            }
        } else {
            // Check if updated in last 'cust no refresh hours'
            long timeDiff = (new Date()).getTime() - lastUpdate;
            long noRefreshDuration = 60*60*1000*MyGlobalSettings.getCustNoRefreshHrs();
            if( timeDiff > noRefreshDuration ) {
                return true;
            }
        }

        return false;
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
