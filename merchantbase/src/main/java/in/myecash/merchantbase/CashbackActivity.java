package in.myecash.merchantbase;

/**
 * Created by adgangwa on 23-02-2016.
 */

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import in.myecash.appbase.OtpPinInputDialog;
import in.myecash.appbase.barcodeReader.BarcodeCaptureActivity;
import in.myecash.appbase.constants.AppConstants;

import in.myecash.common.CommonUtils;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.MyGlobalSettings;
import in.myecash.common.database.Merchants;
import in.myecash.appbase.utilities.AppAlarms;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.common.DateUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.ValidationHelper;
import in.myecash.merchantbase.entities.MyCustomerOps;
import in.myecash.merchantbase.entities.MerchantUser;
import in.myecash.merchantbase.entities.MyMerchantStats;
import in.myecash.merchantbase.helper.MyRetainedFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class CashbackActivity extends AppCompatActivity implements
        MyRetainedFragment.RetainedFragmentIf, MobileNumberFragment.MobileFragmentIf,
        BillingFragment.BillingFragmentIf, CashTransactionFragment.CashTransactionFragmentIf,
        DialogFragmentWrapper.DialogFragmentWrapperIf, OrderListViewFragment.OrderListViewFragmentIf,
        CustomerRegDialog.CustomerRegFragmentIf, TxnSuccessDialog.TxnSuccessDialogIf,
        CustomerOpDialog.CustomerOpDialogIf, OtpPinInputDialog.OtpPinInputDialogIf,
        PasswordPreference.PasswordPreferenceIf, TrustedDevicesFragment.TrustedDevicesFragmentIf,
        TxnPinInputDialog.TxnPinInputDialogIf, MobileChangePreference.MobileChangePreferenceIf,
        DashboardTxnFragment.DashboardFragmentIf, DashboardFragment.DashboardSummaryFragmentIf,
        CustomerDetailsDialog.CustomerDetailsDialogIf, CustomerDataDialog.CustomerDataDialogIf,
        CustomerListFragment.CustomerListFragmentIf, MerchantOpListFrag.MerchantOpListFragIf,
        TxnConfirmFragment.TxnConfirmFragmentIf {

    private static final String TAG = "CashbackActivity";

    public static final String INTENT_EXTRA_USER_TOKEN = "extraUserToken";

    private static final int RC_BARCODE_CAPTURE = 9001;

    private static final String RETAINED_FRAGMENT = "workCashback";
    private static final String MOBILE_NUM_FRAGMENT = "MobileNumFragment";
    private static final String BILLING_FRAGMENT = "BillingFragment";
    private static final String CASH_TRANS_FRAGMENT = "CashTransFragment";
    private static final String ORDER_LIST_FRAGMENT = "OrderListFragment";
    private static final String SETTINGS_FRAGMENT = "SettingsFragment";
    private static final String TRUSTED_DEVICES_FRAGMENT = "TrustedDevicesFragment";
    private static final String DASHBOARD_FRAGMENT = "DashboardFragment";
    private static final String DASHBOARD_SUMMARY_FRAG = "DashboardSummaryFrag";
    private static final String CUSTOMER_LIST_FRAG = "CustomerListFrag";
    private static final String MERCHANT_OPS_LIST_FRAG = "MerchantOpsListFrag";
    private static final String TXN_CONFIRM_FRAGMENT = "TxnConfirmFragment";

    private static final String DIALOG_BACK_BUTTON = "dialogBackButton";
    private static final String DIALOG_LOGOUT = "dialogLogout";
    private static final String DIALOG_REG_CUSTOMER = "dialogRegCustomer";
    private static final String DIALOG_TXN_SUCCESS = "dialogSuccessTxn";
    private static final String DIALOG_PIN_CASH_TXN = "dialogPinTxn";

    private static final String DIALOG_CUSTOMER_OP_NEW_CARD = "dialogNewCard";
    private static final String DIALOG_CUSTOMER_OP_RESET_PIN = "dialogResetPin";
    private static final String DIALOG_CUSTOMER_OP_CHANGE_MOBILE = "dialogChangeMobile";

    private static final String DIALOG_PIN_CUSTOMER_OP = "dialogPinCustomerOp";
    private static final String DIALOG_CUSTOMER_OP_OTP = "dialogCustomerOpOtp";
    private static final String DIALOG_CUSTOMER_DETAILS = "dialogCustomerDetails";
    private static final String DIALOG_MERCHANT_DETAILS = "dialogMerchantDetails";

    private static final String DIALOG_CUSTOMER_DATA = "dialogCustomerData";

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

    private MerchantUser mMerchantUser;
    private Merchants mMerchant;

    // Activity state members: These are to be saved for restore in event of activity recreation
    boolean mCashTxnStartPending;
    boolean mExitAfterLogout;
    boolean mTbImageIsMerchant;
    int mLastMenuItemId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cashback);

        mMerchantUser = MerchantUser.getInstance();
        mMerchant = mMerchantUser.getMerchant();
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
            mCashTxnStartPending = savedInstanceState.getBoolean("mCashTxnStartPending");
            mExitAfterLogout = savedInstanceState.getBoolean("mExitAfterLogout");
            mTbImageIsMerchant = savedInstanceState.getBoolean("mTbImageIsMerchant");
            mLastMenuItemId = savedInstanceState.getInt("mLastMenuItemId");
        }

        // Setup a toolbar to replace the action bar.
        initToolbar();
        if(savedInstanceState==null || mTbImageIsMerchant) {
            updateTbForMerchant();
        } else {
            updateTbForCustomer();
        }

        // Setup navigation drawer
        initNavDrawer();

        // setup mobile number fragment
        startMobileNumFragment();

        // show last login time
        /*
        if(mMerchant.getLastLogin() != null) {
            String msg = "Your last login was on "+mSdfDateWithTime.format(mMerchant.getLastLogin());
            DialogFragmentWrapper.createNotification(AppConstants.defaultSuccessTitle, msg, false, false)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }*/
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
        headerTitle.setText(mMerchant.getAuto_id());
        TextView headerMobile = (TextView)headerLayout.findViewById(R.id.drawer_header_mobile);
        headerMobile.setText(mMerchant.getMobile_num());

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

    public void selectDrawerItem(MenuItem item) {

        mLastMenuItemId = item.getItemId();
        int i = item.getItemId();

        // Not able to use switch() - as not allowed in library modules
        if (i == R.id.menu_dashboard) {
            // do not fetch from backend - if last fetch was within configured duration
            // this to lessen load on server due to this function
            if (refreshMerchantStats()) {
                // fetch merchant stats from backend
                // this builds fresh 'all customer details' file too
                AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
                mWorkFragment.fetchMerchantStats();

            } else {
                onMerchantStatsResult(ErrorCodes.NO_ERROR);
            }

        } else if (i == R.id.menu_customers) {
            CustomerDataDialog dialog = new CustomerDataDialog();
            dialog.show(mFragMgr, DIALOG_CUSTOMER_DATA);

        } else if (i == R.id.menu_reports) {
            startReportsActivity(null);

        } else if(i == R.id.menu_operations) {
            AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
            mWorkFragment.fetchMerchantsOps();
        }
        else if (i == R.id.menu_settings) {
            startSettingsFragment();

        } else if (i == R.id.menu_trusted_devices) {
            startTrustedDevicesFragment();

            /*
            case R.id.menu_logout:
                DialogFragmentWrapper.createConfirmationDialog(AppConstants.logoutTitle, AppConstants.logoutMsg, false, false)
                        .show(mFragMgr, DIALOG_LOGOUT);
                break;*/
        } else if (i == R.id.menu_reg_customer) {
            askAndRegisterCustomer(false);

        } else if (i == R.id.menu_new_card) {
            if (mWorkFragment.mCustomerOp != null) {
                mWorkFragment.mCustomerOp.setOp_code(DbConstants.OP_NEW_CARD);
            }
            CustomerOpDialog.newInstance(DbConstants.OP_NEW_CARD, mWorkFragment.mCustomerOp)
                    .show(mFragMgr, DIALOG_CUSTOMER_OP_NEW_CARD);

        } else if (i == R.id.menu_change_mobile) {
            if (mWorkFragment.mCustomerOp != null) {
                mWorkFragment.mCustomerOp.setOp_code(DbConstants.OP_CHANGE_MOBILE);
            }
            CustomerOpDialog.newInstance(DbConstants.OP_CHANGE_MOBILE, mWorkFragment.mCustomerOp)
                    .show(mFragMgr, DIALOG_CUSTOMER_OP_CHANGE_MOBILE);

        } else if (i == R.id.menu_reset_pin) {
            if (mWorkFragment.mCustomerOp != null) {
                mWorkFragment.mCustomerOp.setOp_code(DbConstants.OP_RESET_PIN);
            }
            // mWorkFragment.mCustomerOp will be null initially
            CustomerOpDialog.newInstance(DbConstants.OP_RESET_PIN, mWorkFragment.mCustomerOp)
                    .show(mFragMgr, DIALOG_CUSTOMER_OP_RESET_PIN);

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

    private boolean refreshMerchantStats() {
        if(mWorkFragment.mMerchantStats==null) {
            // check if available locally
            String storedStats = getStoredMchntStats();
            if(storedStats != null && !storedStats.isEmpty()) {
                mWorkFragment.mMerchantStats = MyMerchantStats.fromCsvString(storedStats);
                return false;
            }
            return true;
        }
        Date now = new Date();
        Date updateTime = mWorkFragment.mMerchantStats.getUpdated();
        if(updateTime==null) {
            updateTime = mWorkFragment.mMerchantStats.getCreated();
        }
        long timeDiff = now.getTime() - updateTime.getTime();
        long noRefreshDuration = 60*60*1000*MyGlobalSettings.getMchntDashBNoRefreshHrs();
        if( timeDiff > noRefreshDuration ) {
            return true;
        }
        return false;
    }

    private String getStoredMchntStats() {
        String prefName = AppConstants.PREF_MCHNT_STATS_PREFIX +mMerchant.getAuto_id();
        return PreferenceManager.getDefaultSharedPreferences(this).getString(prefName, null);
    }

    private void setStoredMchntStats(String csvStats) {
        LogMy.d(TAG,"In setStoredMchntStats: "+csvStats);
        String prefName = AppConstants.PREF_MCHNT_STATS_PREFIX +mMerchant.getAuto_id();
        if(csvStats==null) {
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .remove(prefName)
                    .apply();
        } else {
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString(prefName, csvStats)
                    .apply();
        }
    }



    private void initToolbar() {
        LogMy.d(TAG, "In initToolbar");
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        initTbViews();

        mTbImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mTbImageIsMerchant) {
                    // show merchants details dialog
                    MerchantDetailsDialog dialog = new MerchantDetailsDialog();
                    dialog.show(mFragMgr, DIALOG_MERCHANT_DETAILS);
                } else {
                    // show customer details dialog
                    CustomerDetailsDialog dialog = CustomerDetailsDialog.newInstance(-1);
                    dialog.show(mFragMgr, DIALOG_CUSTOMER_DETAILS);
                }
            }
        });

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // set merchant details
        // Set merchant DP as toolbar icon
        // Check if local path available, else download from server
        boolean dwnloadImage = false;
        //String prefName = AppConstants.PREF_IMAGE_PATH_PREFIX +mMerchant.getAuto_id();
        //String imageName = PreferenceManager.getDefaultSharedPreferences(this).getString(prefName, null);
        //if( imageName != null) {
            //BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            //Bitmap bitmap = BitmapFactory.decodeFile(imagePath,bmOptions);
            //LogMy.d(TAG,"Mchnt DP available locally: "+imageName);

            File file = getFileStreamPath(mMerchant.getDisplayImage());
            if(file!=null) {
                LogMy.d(TAG,"Mchnt DP available locally: "+file.getPath());

                Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
                if(bitmap==null) {
                    dwnloadImage = true;
                } else {
                    LogMy.d(TAG,"Decoded file as bitmap: "+file.getPath());
                    mMerchantUser.setDisplayImage(bitmap);
                }
            } else {
                dwnloadImage = true;
            }
        //} else if(mMerchant.getDisplayImage()!=null) {
//        } else {
  //          dwnloadImage = true;
    //    }
        if(dwnloadImage) {
            String url = CommonConstants.BACKEND_FILE_BASE_URL+
                    CommonConstants.MERCHANT_DISPLAY_IMAGES_DIR+
                    mMerchantUser.getMerchant().getDisplayImage();
            mWorkFragment.fetchImageFile(url);
        }
    }

    private void setTbImage(int resId) {
        //Bitmap bm = BitmapFactory.decodeResource(getResources(), resId);
        //setTbImage(bm);
        mTbImage.setImageResource(resId);
    }
    private void setTbImage(int resId, int tintColor) {
        mTbImage.setImageDrawable(AppCommonUtil.getTintedDrawable(this, resId, tintColor));
    }

    private void setTbImage(Bitmap image) {
        if(image==null) {
            mTbImage.setVisibility(View.GONE);
        } else {
            mTbImage.setVisibility(View.VISIBLE);

            int radiusInDp = (int) getResources().getDimension(R.dimen.toolbar_image_width);
            int radiusInPixels = AppCommonUtil.dpToPx(radiusInDp);

            Bitmap scaledImg = Bitmap.createScaledBitmap(image,radiusInPixels,radiusInPixels,true);
            Bitmap roundImage = AppCommonUtil.getCircleBitmap(scaledImg);

            mTbImage.setImageBitmap(roundImage);
        }
    }

    private void setTbTitle(String title) {
        mTbTitle.setText(title);
    }

    private void updateTbForCustomer() {
        mTbLayoutSubhead1.setVisibility(View.VISIBLE);

        // no error case: all cashback values available
        mTbTitle.setText(CommonUtils.getPartialVisibleStr(mWorkFragment.mCustMobile));
        // display image
        //mTbLayoutImage.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_border_coconut));
        //mTbImage.setBackground(ContextCompat.getDrawable(this, R.drawable.logo_success_36dp));
        setTbImage(R.drawable.ic_account_circle_white_48dp, R.color.success);
        mTbImageIsMerchant = false;

        mTbTitle2.setVisibility(View.GONE);
        if(mWorkFragment.mCurrCustomer.getStatus()!=DbConstants.USER_STATUS_ACTIVE ) {
            mTbTitle2.setVisibility(View.VISIBLE);
            mTbTitle2.setText(DbConstants.userStatusDesc[mWorkFragment.mCurrCustomer.getStatus()]);
            if(mWorkFragment.mCurrCustomer.getStatus()!=DbConstants.USER_STATUS_LIMITED_CREDIT_ONLY) {
                setTbImage(R.drawable.ic_block_white_48dp, R.color.failure);
            }

        } else if(mWorkFragment.mCurrCustomer.getCardStatus() != DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED) {

            switch(mWorkFragment.mCurrCustomer.getCardStatus()) {
                case DbConstants.CUSTOMER_CARD_STATUS_ALLOTTED:
                    // do nothing
                    break;
                case DbConstants.CUSTOMER_CARD_STATUS_REMOVED:
                    mTbTitle2.setVisibility(View.VISIBLE);
                    mTbTitle2.setText(DbConstants.cardStatusDescriptions[mWorkFragment.mCurrCustomer.getCardStatus()]);
                    break;
                default:
                    //raise alarm
                    Map<String,String> params = new HashMap<>();
                    params.put("CustomerId",mWorkFragment.mCurrCustomer.getMobileNum());
                    params.put("CardId",mWorkFragment.mCurrCustomer.getCardId());
                    params.put("CardStatus",String.valueOf(mWorkFragment.mCurrCustomer.getCardStatus()));
                    AppAlarms.invalidCardState(mMerchant.getAuto_id(),DbConstants.USER_TYPE_MERCHANT,"updateTbForCustomer",params);
            }
        }

        mTbSubhead1Text1.setText(AppCommonUtil.getAmtStr(mWorkFragment.mCurrCashback.getCurrClBalance()));
        //mTbSubhead1Divider.setVisibility(View.VISIBLE);
        //mTbSubhead1Text2.setVisibility(View.VISIBLE);
        mTbSubhead1Text2.setText(AppCommonUtil.getAmtStr(mWorkFragment.mCurrCashback.getCurrCbBalance()));
    }

    public void updateTbForMerchant() {
        LogMy.d(TAG,"In updateTbForMerchant");
        if(mMerchantUser.getDisplayImage()!=null) {
            setTbImage(mMerchantUser.getDisplayImage());
            //mTbLayoutImage.setBackground(null);
            mTbImage.setBackground(null);
        }
        mTbImageIsMerchant = true;
        setTbTitle(mMerchant.getName());

        mTbTitle2.setVisibility(View.GONE);
        if(mMerchant.getAdmin_status()==DbConstants.USER_STATUS_UNDER_CLOSURE ) {
            mTbTitle2.setVisibility(View.VISIBLE);
            String msg = "Removal on "+AppCommonUtil.getMchntRemovalDate(mMerchant.getRemoveReqDate());
            mTbTitle2.setText(msg);

        } else if(mMerchant.getAdmin_status()!=DbConstants.USER_STATUS_ACTIVE) {
            // User should not be able to login, in any other status scenario
            // raise alarm
            Map<String,String> params = new HashMap<>();
            params.put("MerchantId",mMerchant.getAuto_id());
            params.put("Current Status",String.valueOf(mMerchant.getAdmin_status()));
            AppAlarms.invalidMerchantState(mMerchant.getAuto_id(),DbConstants.USER_TYPE_MERCHANT,"updateTbForMerchant",params);
        }
        mTbLayoutSubhead1.setVisibility(View.GONE);
    }

    private void initTbViews() {
        //mTbLayoutImage = (RelativeLayout) mToolbar.findViewById(R.id.layout_tb_img) ;
        //mTbHomeIcon = (ImageView) mToolbar.findViewById(R.id.tb_home_icon) ;
        mTbImage = (AppCompatImageView) mToolbar.findViewById(R.id.tb_image) ;
        mTbTitle = (EditText) mToolbar.findViewById(R.id.tb_title) ;
        mTbTitle2 = (EditText) mToolbar.findViewById(R.id.tb_title_2) ;
        mTbLayoutSubhead1 = (LinearLayout) mToolbar.findViewById(R.id.tb_layout_subhead1) ;
        mTbSubhead1Text1 = (EditText) mToolbar.findViewById(R.id.tb_curr_cashload) ;
        //mTbSubhead1Divider = mToolbar.findViewById(R.id.tb_view_1) ;
        mTbSubhead1Text2 = (EditText) mToolbar.findViewById(R.id.tb_curr_cashback) ;
    }

    private void askAndRegisterCustomer(boolean wrongOtpCase) {
        LogMy.d(TAG, "In askAndRegisterCustomer");
        // Show user registration confirmation dialogue
        // confirm for registration
        CustomerRegDialog.newInstance(mWorkFragment.mCustMobile, mWorkFragment.mCustCardId,
                mWorkFragment.mCustRegFirstName, mWorkFragment.mCustRegLastName, wrongOtpCase).
                show(mFragMgr, DIALOG_REG_CUSTOMER);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void searchCustByInternalId(String internalId) {
        int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(this);
        if ( resultCode != ErrorCodes.NO_ERROR) {
            // Show error notification dialog
            DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, AppCommonUtil.getErrorDesc(resultCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        } else {
            AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
            mWorkFragment.fetchCashback(internalId);
        }
    }

    @Override
    public void generateAllCustData() {
        if(refreshMerchantStats()) {
            AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
            // set this to null, to indicate that the customer data file is to be downloaded and processed again
            //mWorkFragment.mLastFetchCashbacks = null;

            // fetch merchant stats from backend
            // this builds fresh 'all customer details' file too
            mWorkFragment.fetchMerchantStats();

        } else {
            // check if file locally available (it should be) - from last fetch
            File file = new File(getFilesDir() + "/" + AppCommonUtil.getMerchantCustFileName(mMerchantUser.getMerchantId()));
            if(file.exists()) {
                startCustomerListFrag();
            } else {
                // Merchant stats refresh not required - but file not available locally
                // so download the same
                LogMy.d(TAG,"Merchant refresh not required, but file not available locally");
                // set this to null, to indicate that the customer data file is to be downloaded and processed again
                //mWorkFragment.mLastFetchCashbacks = null;
                onMerchantStatsResult(ErrorCodes.NO_ERROR);
            }
        }
    }

    // Callback from 'customer card change' dialog
    @Override
    public void onCustomerOpOk(String tag, String mobileNum, String qrCode, String extraParam, String imgFilename) {
        mWorkFragment.mCustomerOp = new MyCustomerOps();
        mWorkFragment.mCustomerOp.setMobile_num(mobileNum);
        mWorkFragment.mCustomerOp.setQr_card(qrCode);

        mWorkFragment.mCardImageFilename = imgFilename;

        boolean askPin = true;
        String txnTitle = null;
        String txnDetail = null;
        switch (tag) {
            case DIALOG_CUSTOMER_OP_NEW_CARD:
                mWorkFragment.mCustomerOp.setOp_code(DbConstants.OP_NEW_CARD);
                mWorkFragment.mCustomerOp.setExtra_op_params(extraParam);
                txnTitle = AppConstants.titleNewCardPin;
                txnDetail = AppConstants.msgNewCardPin;
                break;
            case DIALOG_CUSTOMER_OP_CHANGE_MOBILE:
                mWorkFragment.mCustomerOp.setOp_code(DbConstants.OP_CHANGE_MOBILE);
                mWorkFragment.mCustomerOp.setExtra_op_params(extraParam);
                txnTitle = AppConstants.titleChangeCustMobilePin;
                txnDetail = String.format(AppConstants.msgChangeCustMobilePin,mobileNum);
                break;
            case DIALOG_CUSTOMER_OP_RESET_PIN:
                mWorkFragment.mCustomerOp.setOp_code(DbConstants.OP_RESET_PIN);
                askPin = false;
                break;
        }

        if(askPin) {
            // ask for customer PIN
            OtpPinInputDialog dialog = OtpPinInputDialog.newInstance(txnTitle, txnDetail, "Enter PIN");
            dialog.show(mFragMgr, DIALOG_PIN_CUSTOMER_OP);
        } else {
            // dispatch customer op for execution
            executeCustomerOp();
        }
    }

    @Override
    public void onCustomerOpOtp(String otp) {
        LogMy.d(TAG, "In onCustomerOpOtp: " + otp);
        mWorkFragment.mCustomerOp.setOtp(otp);
        executeCustomerOp();
    }

    @Override
    public void onPinOtp(String pin, String tag) {
        if(tag.equals(DIALOG_PIN_CUSTOMER_OP)) {
            mWorkFragment.mCustomerOp.setPin(pin);
            executeCustomerOp();
        }
    }

    private void executeCustomerOp() {
        int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(this);
        if ( resultCode != ErrorCodes.NO_ERROR) {
            // Show error notification dialog
            DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, AppCommonUtil.getErrorDesc(resultCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        } else {
            AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
            mWorkFragment.executeCustomerOp();
        }
    }

    @Override
    public void onCustomerOpReset(String tag) {
        LogMy.d(TAG, "In onCustomerOpReset: ");
        mWorkFragment.mCustomerOp = null;

        // Create new dialog with same opcode
        switch (tag) {
            case DIALOG_CUSTOMER_OP_NEW_CARD:
                CustomerOpDialog.newInstance(DbConstants.OP_NEW_CARD,null).show(mFragMgr, DIALOG_CUSTOMER_OP_NEW_CARD);
                break;
            case DbConstants.OP_CHANGE_MOBILE:
                CustomerOpDialog.newInstance(DbConstants.OP_CHANGE_MOBILE,null).show(mFragMgr, DIALOG_CUSTOMER_OP_CHANGE_MOBILE);
                break;
            case DIALOG_CUSTOMER_OP_RESET_PIN:
                CustomerOpDialog.newInstance(DbConstants.OP_RESET_PIN,null).show(mFragMgr, DIALOG_CUSTOMER_OP_RESET_PIN);
                break;
        }
    }

    private void onCustomerOpResult(int errorCode) {
        LogMy.d(TAG, "In onCustOpResult: " + errorCode);
        AppCommonUtil.cancelProgressDialog(true);

        String toUploadImgFilename = null;
        String custOp = mWorkFragment.mCustomerOp.getOp_code();
        if(errorCode==ErrorCodes.NO_ERROR) {
            String successMsg = null;

            switch (custOp) {
                case DbConstants.OP_NEW_CARD:
                    successMsg = AppConstants.custOpNewCardSuccessMsg;
                    break;
                case DbConstants.OP_CHANGE_MOBILE:
                    successMsg = AppConstants.custOpChangeMobileSuccessMsg;
                    break;
                /*case DbConstants.OP_RESET_PIN:
                    successMsg = AppConstants.custOpResetPinSuccessMsg;
                    break;*/
            }

            DialogFragmentWrapper.createNotification(AppConstants.defaultSuccessTitle, successMsg, false, false)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);

            // customer operation success, reset to null
            toUploadImgFilename = mWorkFragment.mCustomerOp.getImageFilename();
            mWorkFragment.mCustomerOp = null;

        } else if(errorCode==ErrorCodes.OTP_GENERATED) {
            // OTP sent successfully to registered customer mobile, ask for the same
            mWorkFragment.mCustomerOp.setOp_status(MyCustomerOps.CUSTOMER_OP_STATUS_OTP_GENERATED);
            if(mWorkFragment.mCustomerOp==null) {
                // some issue - not supposed to be null - raise alarm
                AppAlarms.wtf(mMerchant.getAuto_id(),DbConstants.USER_TYPE_MERCHANT,"onCustomerOpResult",null);
            } else {
                CustomerOpDialog.newInstance(mWorkFragment.mCustomerOp.getOp_code(),mWorkFragment.mCustomerOp)
                        .show(mFragMgr, DIALOG_CUSTOMER_OP_OTP);
            }

        } else if(errorCode == ErrorCodes.OP_SCHEDULED &&
                custOp.equals(DbConstants.OP_RESET_PIN)) {
            // Show success notification dialog
            String msg = String.format(AppConstants.pinGenerateSuccessMsg, Integer.toString(MyGlobalSettings.getCustPasswdResetMins()));
            DialogFragmentWrapper.createNotification(AppConstants.defaultSuccessTitle, msg, false, false)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);

            // customer operation success, reset to null
            toUploadImgFilename = mWorkFragment.mCustomerOp.getImageFilename();
            mWorkFragment.mCustomerOp = null;

        } else if(errorCode == ErrorCodes.DUPLICATE_ENTRY &&
                custOp.equals(DbConstants.OP_RESET_PIN)) {
            deleteFile(mWorkFragment.mCardImageFilename);
            // Old request is already pending
            String msg = String.format(AppConstants.pinGenerateDuplicateRequestMsg, Integer.toString(MyGlobalSettings.getCustPasswdResetMins()));
            DialogFragmentWrapper.createNotification(AppConstants.pinResetFailureTitle, msg, false, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);

        } else {
            deleteFile(mWorkFragment.mCardImageFilename);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }

        // if required, start upload of txn image file in background thread
        // storing it in txnImageDir only
        if(toUploadImgFilename != null) {
            mWorkFragment.uploadImageFile(this, mWorkFragment.mCardImageFilename,
                    toUploadImgFilename,
                    CommonUtils.getTxnImgDir(mMerchant.getAuto_id()));
            mWorkFragment.mCardImageFilename = null;
        }
    }

    @Override
    public void getCustTxns(String id) {
        startReportsActivity(id);
    }

    private void startReportsActivity(String custId) {
        // check for reports blackout period
        /*
        Integer startHour = MyGlobalSettings.getMchntNoReportStartHrs();
        Integer endHour = MyGlobalSettings.getMchntNoReportEndHrs();

        if(startHour!=null && endHour!=null &&
                startHour.intValue()!=endHour.intValue()) {
            DateUtil blackoutStart = new DateUtil();
            blackoutStart.toMidnight();
            blackoutStart.addMinutes(startHour*60);

            DateUtil blackoutEnd = new DateUtil();
            blackoutEnd.toMidnight();
            blackoutEnd.addMinutes(endHour*60);

            DateUtil now = new DateUtil();
            if (now.isHourBetween(blackoutStart.getTime(),blackoutEnd.getTime()) ) {
                // reports not available
                // Show error notification dialog
                SimpleDateFormat sdf = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_TIME_12, CommonConstants.DATE_LOCALE);
                String msg = String.format(AppConstants.reportBlackoutMsg,sdf.format(blackoutStart.getTime()), sdf.format(blackoutEnd.getTime()));
                DialogFragmentWrapper.createNotification(AppConstants.reportBlackoutTitle, msg, false, false)
                        .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
                return;
            }
        }*/

        // start reports activity
        Intent intent = new Intent( this, TxnReportsActivity.class );
        intent.putExtra(TxnReportsActivity.EXTRA_CUSTOMER_ID, custId);
        startActivity(intent);
    }

    private SettingsFragment startSettingsFragment() {
        /*int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(this);
        if ( resultCode != ErrorCodes.NO_ERROR) {
            // Show error notification dialog
            DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, AppCommonUtil.getErrorDesc(resultCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        } else {*/
            // Store DB settings to app preferences
            if( restoreSettings() ) {
                //setDrawerState(false);
                // Display the fragment as the main content.
                if (mFragMgr.findFragmentByTag(SETTINGS_FRAGMENT) == null) {
                    SettingsFragment frag = new SettingsFragment();
                    mFragMgr.beginTransaction()
                            .replace(R.id.fragment_container_1, frag, SETTINGS_FRAGMENT)
                            .addToBackStack(SETTINGS_FRAGMENT)
                            .commit();
                    return frag;
                }
            } else {
                // Show error notification dialog
                DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), false, true)
                        .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
            }
        //}
        return null;
    }

    public boolean restoreSettings() {
        //MerchantSettings settings = mMerchant.getSettings();
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(this).edit();

        prefs.putString(SettingsFragment.KEY_CB_RATE, mMerchant.getCb_rate());
        prefs.putBoolean(SettingsFragment.KEY_ADD_CL_ENABLED, mMerchant.getCl_add_enable());
        prefs.putString(SettingsFragment.KEY_MOBILE_NUM, mMerchant.getMobile_num());
        prefs.putString(SettingsFragment.KEY_EMAIL, mMerchant.getEmail());

        return prefs.commit();
    }

    private int logoutMerchant() {
        int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(this);
        // if internet not available - dont try logout
        if ( resultCode == ErrorCodes.NO_ERROR) {
            // show progress dialog
            AppCommonUtil.showProgressDialog(this, AppConstants.progressLogout);
            mWorkFragment.logoutMerchant();
        }
        return resultCode;
    }

    private void onSettingsChange() {
        // Save merchant user to update changes in settings/profile
        // check internet and start customer reg thread
        int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(this);
        if ( resultCode != ErrorCodes.NO_ERROR) {
            // Show error notification dialog
            DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, AppCommonUtil.getErrorDesc(resultCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        } else {
            // show progress dialog
            AppCommonUtil.showProgressDialog(this, AppConstants.progressSettings);
            mWorkFragment.updateMerchantSettings();
        }
    }

    @Override
    public void changePassword(String oldPasswd, String newPassword) {
        int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(this);
        if ( resultCode != ErrorCodes.NO_ERROR) {
            // Show error notification dialog
            DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, AppCommonUtil.getErrorDesc(resultCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        } else {
            // show progress dialog
            AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
            mWorkFragment.changePassword(oldPasswd, newPassword);
        }
    }

    private void passwordChangeResponse(int errorCode) {
        LogMy.d(TAG, "In passwordChangeResponse: " + errorCode);
        AppCommonUtil.cancelProgressDialog(true);

        if(errorCode==ErrorCodes.NO_ERROR) {
            DialogFragmentWrapper.createNotification(AppConstants.pwdChangeSuccessTitle, AppConstants.pwdChangeSuccessMsg, false, false)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
            int error = logoutMerchant();
            if(error != ErrorCodes.NO_ERROR) {
                onLogoutResponse(error);
            }
        } else {
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    @Override
    public void deleteDevice() {
        AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
        mWorkFragment.deleteDevice();
    }

    @Override
    public void onBgProcessResponse(int errorCode, int operation) {
        LogMy.d(TAG,"In onBgProcessResponse: "+operation+", "+errorCode);
        switch(operation) {
            case MyRetainedFragment.REQUEST_FETCH_MERCHANT_OPS:
                AppCommonUtil.cancelProgressDialog(true);
                if(errorCode==ErrorCodes.NO_ERROR) {
                    startMerchantOpsFrag();
                } else {
                    DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                            .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
                }
                break;
            case MyRetainedFragment.REQUEST_ARCHIVE_TXNS:
                // do nothing
                break;
            case MyRetainedFragment.REQUEST_IMAGE_DOWNLOAD:
                onMerchantDpDownload(errorCode);
                break;
            case MyRetainedFragment.REQUEST_GET_CASHBACK:
                onCashbackResponse(errorCode);
                break;
            case MyRetainedFragment.REQUEST_REGISTER_CUSTOMER:
                onCustRegResponse(errorCode);
                break;
            case MyRetainedFragment.REQUEST_COMMIT_TRANS:
                onCommitTransResponse(errorCode);
                break;
            case MyRetainedFragment.REQUEST_UPDATE_MERCHANT_SETTINGS:
                onSettingsUpdateResponse(errorCode);
                break;
            case MyRetainedFragment.REQUEST_LOGOUT_MERCHANT:
                onLogoutResponse(errorCode);
                break;
            case MyRetainedFragment.REQUEST_ADD_CUSTOMER_OP:
                onCustomerOpResult(errorCode);
                break;
            case MyRetainedFragment.REQUEST_CHANGE_PASSWD:
                passwordChangeResponse(errorCode);
                break;
            case MyRetainedFragment.REQUEST_DELETE_TRUSTED_DEVICE:
                AppCommonUtil.cancelProgressDialog(true);
                if(errorCode == ErrorCodes.NO_ERROR) {
                    // detach and attach trusted device fragment - to refresh its view
                    Fragment currentFragment = getFragmentManager().findFragmentByTag(TRUSTED_DEVICES_FRAGMENT);
                    if(currentFragment != null &&
                            currentFragment.isVisible()) {
                        // remove 'trusted device' fragment
                        getFragmentManager().popBackStackImmediate();
                        // start the same again
                        startTrustedDevicesFragment();
                    } else {
                        LogMy.e(TAG, "Trusted device fragment not found.");
                    }
                } else {
                    DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                            .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
                }
                break;
            //case RetainedFragment.REQUEST_ADD_MERCHANT_OP:
            case MyRetainedFragment.REQUEST_CHANGE_MOBILE:
                onChangeMobileResponse(errorCode);
                break;
            case MyRetainedFragment.REQUEST_MERCHANT_STATS:
                if(errorCode == ErrorCodes.NO_ERROR) {
                    // store the fetched stats in shared preference in CSV format
                    setStoredMchntStats(MyMerchantStats.toCsvString(mWorkFragment.mMerchantStats));
                }
                onMerchantStatsResult(errorCode);
                break;
            case MyRetainedFragment.REQUEST_CUST_DATA_FILE_DOWNLOAD:
                AppCommonUtil.cancelProgressDialog(true);
                // start customer list fragment
                if(errorCode==ErrorCodes.NO_ERROR) {
                    startCustomerListFrag();
                } else {
                    // remove local stored stats - so as file is created again next time
                    setStoredMchntStats(null);
                    //raise alarm
                    Map<String,String> params = new HashMap<>();
                    params.put("opCode",String.valueOf(MyRetainedFragment.REQUEST_CUST_DATA_FILE_DOWNLOAD));
                    params.put("erroCode",String.valueOf(errorCode));
                    AppAlarms.fileDownloadFailed(mMerchant.getAuto_id(),DbConstants.USER_TYPE_MERCHANT,"onBgProcessResponse",params);
                    // show error
                    DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                            .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
                }
                break;
            case MyRetainedFragment.REQUEST_UPLOAD_IMG:
                if(errorCode==ErrorCodes.NO_ERROR) {
                    LogMy.d(TAG,"Uploaded image file successfully");
                } else {
                    LogMy.e(TAG,"Failed to upload image file");
                    //raise alarm
                    Map<String,String> params = new HashMap<>();
                    params.put("opCode",String.valueOf(MyRetainedFragment.REQUEST_UPLOAD_IMG));
                    params.put("erroCode",String.valueOf(errorCode));
                    AppAlarms.fileUploadFailed(mMerchant.getAuto_id(),DbConstants.USER_TYPE_MERCHANT,"onBgProcessResponse",params);
                }
                break;
        }
    }

    private void onMerchantStatsResult(int errorCode) {
        AppCommonUtil.cancelProgressDialog(true);

        if(errorCode==ErrorCodes.NO_ERROR) {
            if(mLastMenuItemId==R.id.menu_dashboard) {
                startDBoardSummaryFrag();

            } else if(mLastMenuItemId==R.id.menu_customers) {
                // customer data scenario - download the data file
                AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
                mWorkFragment.downloadCustDataFile(this,
                        CommonUtils.getMerchantCustFilePath(mMerchant.getAuto_id()));

                /*if(mWorkFragment.mLastFetchCashbacks == null) {
                    // refresh scenario - download the data file
                    AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
                    mWorkFragment.downloadCustDataFile(this,
                            AppCommonUtil.getMerchantCustFilePath(mMerchant.getAuto_id()));

                } else {
                    // old no-refresh scenario OR file downloaded
                    // show the data
                    startCustomerListFrag();
                }*/
            }
        } else {
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    private void onChangeMobileResponse(int errorCode) {
        LogMy.d(TAG, "In onChangeMobileResponse: " + errorCode);
        AppCommonUtil.cancelProgressDialog(true);

        if(errorCode==ErrorCodes.NO_ERROR) {
            DialogFragmentWrapper.createNotification(AppConstants.defaultSuccessTitle, AppConstants.mobileChangeSuccessMsg, false, false)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);

            // merchant operation success, reset to null
            changeMobileNumReset(false);

        } else if(errorCode==ErrorCodes.OTP_GENERATED) {
            // OTP sent successfully to new mobile, ask for the same
            // show the 'mobile change preference' again
            // create if fragment don't already exist
            SettingsFragment settingsFrag = (SettingsFragment) mFragMgr.findFragmentByTag(SETTINGS_FRAGMENT);
            if (settingsFrag==null) {
                settingsFrag = startSettingsFragment();
            }
            if( settingsFrag==null || !settingsFrag.showChangeMobilePreference() ) {
                // if failed to show prefernce for some reason - ask user to do so manually
                DialogFragmentWrapper.createNotification(AppConstants.generalInfoTitle,
                        AppConstants.msgChangeMobileOtpGenerated, false, false)
                        .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
            }
        } else {
            // reset in case of any error
            changeMobileNumReset(false);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    @Override
    public void changeMobileNumOk(String verifyParam, String newMobile) {
        mWorkFragment.mNewMobileNum = newMobile;
        mWorkFragment.mVerifyParamMobileChange = verifyParam;
        // dispatch customer op for execution
        changeMobileNum();
    }

    @Override
    public void changeMobileNumOtp(String otp) {
        LogMy.d(TAG, "In changeMobileNumOtp: " + otp);
        //mWorkFragment.mMerchantOp.setOtp(otp);
        mWorkFragment.mOtpMobileChange = otp;
        changeMobileNum();
    }

    @Override
    public void changeMobileNumReset(boolean showMobilePref) {
        LogMy.d(TAG, "In changeMobileNumReset: ");
        //mWorkFragment.mMerchantOp = null;
        mWorkFragment.mNewMobileNum = null;
        mWorkFragment.mVerifyParamMobileChange = null;
        mWorkFragment.mOtpMobileChange = null;

        // show the 'mobile change preference' again
        // create if fragment don't already exist
        if(showMobilePref) {
            SettingsFragment settingsFrag = (SettingsFragment) mFragMgr.findFragmentByTag(SETTINGS_FRAGMENT);
            if (settingsFrag==null) {
                settingsFrag = startSettingsFragment();
            }
            if( settingsFrag==null || !settingsFrag.showChangeMobilePreference() ) {
                //raise alarm
                AppAlarms.localOpFailed(mMerchant.getAuto_id(),DbConstants.USER_TYPE_MERCHANT,"changeMobileNumReset",null);
                // if failed to show preference for some reason - ask user to do so manually
                DialogFragmentWrapper.createNotification(AppConstants.generalInfoTitle,
                        "Please click on 'Change Mobile' again from the settings.", false, true)
                        .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
            }
        }
    }

    private void changeMobileNum() {
        int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(this);
        if ( resultCode != ErrorCodes.NO_ERROR) {
            // Show error notification dialog
            DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, AppCommonUtil.getErrorDesc(resultCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        } else {
            AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
            mWorkFragment.changeMobileNum();
        }
    }

    private void onLogoutResponse(int errorCode) {
        LogMy.d(TAG, "In onLogoutResponse: " + errorCode);

        AppCommonUtil.cancelProgressDialog(true);
        MerchantUser.reset();

        //Start Login Activity
        if(!mExitAfterLogout) {
            Intent intent = new Intent( this, LoginActivity.class );
            // clear cashback activity from backstack
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        finish();
    }

    private void onSettingsUpdateResponse(int errorCode) {
        LogMy.d(TAG, "In onSettingsUpdateResponse: " + errorCode);

        AppCommonUtil.cancelProgressDialog(true);
        mMerchant = mMerchantUser.getMerchant();

        if(errorCode!=ErrorCodes.NO_ERROR) {
            restoreSettings();
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
        // pop settings fragment
        // this was not intentially done in onBackPressed
        setDrawerState(true);
        Fragment settingsFrag = mFragMgr.findFragmentByTag(SETTINGS_FRAGMENT);
        if ( settingsFrag!=null &&
                settingsFrag.isVisible() ) {
            mFragMgr.popBackStackImmediate();
            //mSettingsFragment = null;
        }
    }

    public void onMerchantDpDownload(int errorCode) {
        LogMy.d(TAG, "In onMerchantDpDownload");
        Bitmap image = mWorkFragment.mLastFetchedImage;
        if(errorCode==ErrorCodes.NO_ERROR && image!=null) {
            // store in MerchantUser for later use
            mMerchantUser.setDisplayImage(image);

            FileOutputStream fos = null;
            try {
                fos = openFileOutput(mMerchant.getDisplayImage(), Context.MODE_PRIVATE);
                image.compress(AppCommonUtil.getImgCompressFormat(), 100, fos);
                fos.close();

                // Store image path
                /*String prefName = AppConstants.PREF_IMAGE_PATH_PREFIX +mMerchant.getAuto_id();
                PreferenceManager.getDefaultSharedPreferences(this)
                        .edit()
                        .putString(prefName, mMerchant.getDisplayImage())
                        .apply();*/

            } catch (Exception e) {
                // TODO
                LogMy.e(TAG,"Exception while storing merchant DP: "+mMerchant.getDisplayImage());
            }

            //Drawable drawable = new BitmapDrawable(getResources(), image);
            // store in SD card and path in preferences
            /*
            File photoFile = AppCommonUtil.createLocalImageFile(this, mMerchant.getDisplayImage());
            if (AppCommonUtil.createImageFromBitmap(image, photoFile)) {
                // Store image path
                String prefName = AppConstants.PREF_IMAGE_PATH_PREFIX +mMerchant.getAuto_id();
                PreferenceManager.getDefaultSharedPreferences(this)
                        .edit()
                        .putString(prefName, photoFile.getPath())
                        .apply();
            } else {
                LogMy.e(TAG, "Error while creating image file from bitmap");
            }*/
        } else {
            // TODO: set default image
        }
        // reset it
        mWorkFragment.mLastFetchedImage = null;
        updateTbForMerchant();
    }

    public void onCustRegResponse(int errorCode) {
        LogMy.d(TAG, "In onCustRegResponse: " + errorCode);

        AppCommonUtil.cancelProgressDialog(true);
        if(errorCode==ErrorCodes.NO_ERROR) {
            // user registered, please proceed
            DialogFragmentWrapper.createNotification(AppConstants.customerRegConfirmTitle, AppConstants.custRegSuccessMsg, false, false)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);

            if(mWorkFragment.mCurrCashback == null) {
                // Error scenario: Failed to create cashback object, as part of customer registration
                // Restart from the first screen - so as user press 'process' button again
                restartTxn();
            } else {
                // cashback successfully created as part of customer registration
                onCashbackResponse(ErrorCodes.NO_ERROR);
            }

        } else if(errorCode==ErrorCodes.OTP_GENERATED) {
            // OTP sent successfully to customer mobile, ask for the same
            if(mWorkFragment.mCustMobile==null || mWorkFragment.mCustCardId==null) {
                // some issue - not supposed to be null - raise alarm
                AppAlarms.wtf(mMerchant.getAuto_id(),DbConstants.USER_TYPE_MERCHANT,"onCustRegResponse",null);
            } else {
                // start the dialog again
                // but this time it will have pre-filled last entered mobile and cardId
                // based on which it will ask for OTP
                askAndRegisterCustomer(false);
            }

        } else if(errorCode==ErrorCodes.WRONG_OTP) {
            // OTP sent successfully to customer mobile, ask for the same
            if(mWorkFragment.mCustMobile==null || mWorkFragment.mCustCardId==null) {
                // some issue - not supposed to be null - raise alarm
                AppAlarms.wtf(mMerchant.getAuto_id(),DbConstants.USER_TYPE_MERCHANT,"onCustRegResponse",null);
            } else {
                // start the dialog again
                askAndRegisterCustomer(true);
            }

        } else {
            // Pop all fragments uptil mobile number one
            restartTxn();
            DialogFragmentWrapper.createNotification(AppConstants.regFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    public void onCashbackResponse(int errorCode) {
        LogMy.d(TAG, "In onCashbackResponse: " + errorCode);

        if(mLastMenuItemId == R.id.menu_customers) {
            AppCommonUtil.cancelProgressDialog(true);
            // response against search of particular customer details
            if(errorCode==ErrorCodes.NO_ERROR) {
                // show customer details dialog
                CustomerDetailsDialog dialog = CustomerDetailsDialog.newInstance(-1);
                dialog.show(mFragMgr, DIALOG_CUSTOMER_DETAILS);
            } else {
                DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                        .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
            }

            return;
        }

        // Update data in toolbar as per response
        if(errorCode== ErrorCodes.NO_SUCH_USER) {
            askAndRegisterCustomer(false);
        } else if(errorCode==ErrorCodes.NO_ERROR) {
            // update customer ids to actual fetched - just to be sure
            updateCustIds();
            updateTbForCustomer();
            if(mCashTxnStartPending) {
                startCashTransFragment();
            } else if(mLastMenuItemId==R.id.menu_reg_customer &&
                    mMobileNumFragment.isVisible()) {
                // Register from Menu item - start Billing fragment
                startBillingFragment();
            }
        } else {
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    private void updateCustIds() {
        mWorkFragment.mCustMobile = mWorkFragment.mCurrCustomer.getMobileNum();
    }

    @Override
    public void onTransactionSubmit(int cashPaid) {
        // start txn confirm fragment
        startTxnConfirmFrag(cashPaid);
    }

    @Override
    public void onTransactionConfirm() {
        if(CommonUtils.customerPinRequired(mMerchantUser.getMerchant(), mWorkFragment.mCurrTransaction.getTransaction())) {
            // ask for customer PIN
            TxnPinInputDialog dialog = TxnPinInputDialog.newInstance(
                    mWorkFragment.mCurrTransaction.getTransaction().getCl_credit(),
                    mWorkFragment.mCurrTransaction.getTransaction().getCl_debit(),
                    mWorkFragment.mCurrTransaction.getTransaction().getCb_debit(),
                    null);
            dialog.show(mFragMgr, DIALOG_PIN_CASH_TXN);
        } else {
            commitTxn(null);
        }
    }

    /*private boolean customerPinRequired() {
        int cl_credit_threshold = mMerchantUser.getClCreditLimitForPin();
        int cl_debit_threshold = mMerchantUser.getClDebitLimitForPin();
        int cb_debit_threshold = mMerchantUser.getCbDebitLimitForPin();

        int higher_debit_threshold = Math.max(cl_debit_threshold, cb_debit_threshold);

        int currClDebit = mWorkFragment.mCurrTransaction.getTransaction().getCl_debit();
        int currCbDebit = mWorkFragment.mCurrTransaction.getTransaction().getCb_debit();

        return ( (mWorkFragment.mCurrTransaction.getTransaction().getCl_credit() > cl_credit_threshold) ||
                (currClDebit > cl_debit_threshold) ||
                (currCbDebit > cb_debit_threshold) ||
                ((currClDebit+currCbDebit) > higher_debit_threshold) );
    }*/

    @Override
    public void onTxnPin(String pinOrOtp, String tag) {
        if(tag.equals(DIALOG_PIN_CASH_TXN)) {
            commitTxn(pinOrOtp);
        }
    }

    private void commitTxn(String pin) {
        // check internet and start customer reg thread
        int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(this);
        if ( resultCode != ErrorCodes.NO_ERROR) {
            // Show error notification dialog
            DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, AppCommonUtil.getErrorDesc(resultCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        } else {
            // show progress dialog
            AppCommonUtil.showProgressDialog(CashbackActivity.this, AppConstants.progressDefault);

            if(mWorkFragment.mCardImageFilename !=null) {
                // check if txn image is to be captured
                if(!captureTxnImage(pin)) {
                    // delete earlier stored captured image file
                    deleteFile(mWorkFragment.mCardImageFilename);
                    /*if(!txnImage.delete()) {
                        LogMy.w(TAG,"Failed to delete txn image file: "+txnImage.getAbsolutePath());
                    }*/
                    mWorkFragment.mCardImageFilename = null;
                }
            }

            mWorkFragment.mCurrTransaction.getTransaction().setImgFileName(mWorkFragment.mCardImageFilename);
            mWorkFragment.commitCashTransaction(pin);
        }
    }

    private boolean captureTxnImage(String pin) {
        switch(MyGlobalSettings.getCardImageCaptureMode()) {
            case MyGlobalSettings.TXN_IMAGE_CAPTURE_ALWAYS:
                return true;
            case MyGlobalSettings.TXN_IMAGE_CAPTURE_CARD_REQUIRED:
                return ( (mWorkFragment.mCurrTransaction.getTransaction().getCl_debit()>0 &&
                        MyGlobalSettings.getCardReqAccDebit()) ||
                        (mWorkFragment.mCurrTransaction.getTransaction().getCb_debit()>0 &&
                                MyGlobalSettings.getCardReqCbRedeem()) );
            case MyGlobalSettings.TXN_IMAGE_CAPTURE_NEVER:
                return false;
        }
        return true;
    }

    public void onCommitTransResponse(int errorCode) {
        LogMy.d(TAG, "In onCommitTransResponse: " + errorCode);

        AppCommonUtil.cancelProgressDialog(true);

        if(errorCode == ErrorCodes.NO_ERROR) {
            // Display success notification
            TxnSuccessDialog dialog = TxnSuccessDialog.newInstance(
                    mWorkFragment.mCurrCustomer.getMobileNum(),
                    mWorkFragment.mCurrCashback.getCurrClBalance(),
                    mWorkFragment.mCurrCashback.getCurrCbBalance(),
                    mWorkFragment.mCurrCashback.getOldClBalance(),
                    mWorkFragment.mCurrCashback.getOldCbBalance());
            dialog.show(mFragMgr, DIALOG_TXN_SUCCESS);

            // if required, start upload of txn image file in background thread
            if(mWorkFragment.mCardImageFilename != null) {
                mWorkFragment.uploadImageFile(this, mWorkFragment.mCardImageFilename,
                        mWorkFragment.mCurrTransaction.getTransaction().getImgFileName(),
                        CommonUtils.getTxnImgDir(mMerchant.getAuto_id()));
                mWorkFragment.mCardImageFilename = null;
            }
        } else {
            // delete file, if available
            if(mWorkFragment.mCardImageFilename != null) {
                deleteFile(mWorkFragment.mCardImageFilename);
                mWorkFragment.mCardImageFilename = null;
            }
            // Display failure notification
            DialogFragmentWrapper.createNotification(AppConstants.commitTransFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    /*public void uploadImage(String localStoredFileName, String remoteFileName, String remoteDir) {
        // get file object for the stored file
        File txnImage = new File(getFilesDir() + "/" + localStoredFileName);
        // check if image of card exists
        if(txnImage.exists()) {
            // rename the file to one provided by backend
            File to = new File(txnImage.getParentFile(), remoteFileName);
            if(txnImage.renameTo(to)) {
                // add upload request
                mWorkFragment.uploadImageFile(to, remoteDir);
            } else {
                LogMy.w(TAG, "Txn Image file rename failed: "+txnImage.getAbsolutePath());
                deleteFile(localStoredFileName);
                //raise alarm
                Map<String,String> params = new HashMap<>();
                params.put("FromFilePath",txnImage.getAbsolutePath());
                params.put("ToFilePath",to.getAbsolutePath());
                AppAlarms.localOpFailed(mMerchant.getAuto_id(),DbConstants.USER_TYPE_MERCHANT,"uploadImage",params);
            }

        } else {
            // for some reason file does not exist
            LogMy.w(TAG,"Image file does not exist: "+txnImage.getAbsolutePath());
            //raise alarm
            Map<String,String> params = new HashMap<>();
            params.put("FilePath",txnImage.getAbsolutePath());
            AppAlarms.localOpFailed(mMerchant.getAuto_id(),DbConstants.USER_TYPE_MERCHANT,"uploadImage",params);
        }
    }*/

    public void onDialogResult(String tag, int indexOrResultCode, ArrayList<Integer> selectedItemsIndexList) {
        LogMy.d(TAG, "In onDialogResult: " + tag);

        if (tag.equals(DIALOG_BACK_BUTTON)) {
            mExitAfterLogout = true;
            int error = logoutMerchant();
            if(error != ErrorCodes.NO_ERROR) {
                onLogoutResponse(error);
            }
        }/* else if(tag.equals(DIALOG_LOGOUT)) {
            mExitAfterLogout = false;
            logoutMerchant();
        }*/
    }

    @Override
    public void restartTxn() {
        getReadyForNewTransaction();
        // Pop all fragments uptil mobile number one
        mFragMgr.popBackStackImmediate(MOBILE_NUM_FRAGMENT, 0);
    }

    @Override
    public void onTxnSuccess() {
        restartTxn();
    }

    private void getReadyForNewTransaction() {
        LogMy.d(TAG, "In getReadyForNewTransaction");
        mWorkFragment.reset();
        updateTbForMerchant();
        setDrawerState(true);
    }

    @Override
    public void onCustomerRegOk(String mobileNum, String cardId, String otp, String firstName, String lastName) {

        int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(this);
        if ( resultCode != ErrorCodes.NO_ERROR) {
            // Show error notification dialog
            DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, AppCommonUtil.getErrorDesc(resultCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        } else {
            // show progress dialog
            AppCommonUtil.showProgressDialog(CashbackActivity.this, AppConstants.progressRegCustomer);
            // start in background thread
            mWorkFragment.registerCustomer(mobileNum, cardId, otp, firstName, lastName);
            // update values
            mWorkFragment.mCustMobile = mobileNum;
            mWorkFragment.mCustCardId = cardId;
            mWorkFragment.mCardPresented = true;

            mWorkFragment.mCustRegFirstName = firstName;
            mWorkFragment.mCustRegLastName = lastName;

            Crashlytics.setString(AppConstants.CLTS_INPUT_CUST_MOBILE, mobileNum);
            Crashlytics.setString(AppConstants.CLTS_INPUT_CUST_CARD, cardId);
        }
    }

    @Override
    public void onCustomerRegReset() {
        LogMy.d(TAG, "In onCustomerOpReset: ");
        // reset
        mWorkFragment.mCustMobile = null;
        mWorkFragment.mCustCardId = null;
        mWorkFragment.mCardPresented = false;
        mWorkFragment.mCustRegLastName = null;
        mWorkFragment.mCustRegFirstName = null;

        askAndRegisterCustomer(false);
    }


    @Override
    public void onMobileNumInput(String mobileNum) {

        // As 'process' button is clicked - so reset below indication variable
        mLastMenuItemId = -1;

        if(mobileNum==null) {
            LogMy.d(TAG,"In onMobileNumInput: null mobile number");
            // merchant decided to skip mobile number collection
            mWorkFragment.mCustMobile = null;
            startBillingFragment();
        } else {
            LogMy.d(TAG, "In onMobileNumInput: " + mobileNum);

            if(mobileNum.length() != CommonConstants.MOBILE_NUM_LENGTH) {
                // scan qr card
                // launch barcode activity.
                Intent intent = new Intent(this, BarcodeCaptureActivity.class);
                intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
                ///intent.putExtra(BarcodeCaptureActivity.UseFlash, SettingsFragment.useCameraFlash(this));
                intent.putExtra(BarcodeCaptureActivity.UseFlash, false);
                mWorkFragment.mCardImageFilename = CommonConstants.PREFIX_TXN_IMG_FILE_NAME+Long.toString(System.currentTimeMillis())+"."+CommonConstants.PHOTO_FILE_FORMAT;
                intent.putExtra(BarcodeCaptureActivity.ImageFileName,mWorkFragment.mCardImageFilename);

                startActivityForResult(intent, RC_BARCODE_CAPTURE);
            } else {
                int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(this);
                if ( resultCode != ErrorCodes.NO_ERROR) {
                    // Show error notification dialog
                    DialogFragmentWrapper.createNotification(AppConstants.noInternetTitle, AppCommonUtil.getErrorDesc(resultCode), false, true)
                            .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
                } else {
                    mWorkFragment.mCustMobile = mobileNum;
                    Crashlytics.setString(AppConstants.CLTS_INPUT_CUST_MOBILE, mobileNum);
                    mWorkFragment.fetchCashback(mobileNum);
                    startBillingFragment();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            String qrCode = null;
            if(data!=null) {
                qrCode = data.getStringExtra(BarcodeCaptureActivity.BarcodeObject);
            }
            if (resultCode == ErrorCodes.NO_ERROR &&
                    qrCode != null) {
                LogMy.d(TAG, "Read customer QR code: " + qrCode);
                if(ValidationHelper.validateMemberCard(qrCode) == ErrorCodes.NO_ERROR) {
                    mWorkFragment.mCustCardId = qrCode;
                    Crashlytics.setString(AppConstants.CLTS_INPUT_CUST_CARD, qrCode);
                    mWorkFragment.mCardPresented = true;

                    mWorkFragment.fetchCashback(qrCode);
                    startBillingFragment();
                } else {
                    Toast.makeText(this, "Not a valid customer card.", Toast.LENGTH_LONG).show();
                }
            } else {
                LogMy.d(TAG,"Failed to read barcode");
            }
        }
    }

    private void startTxnConfirmFrag(int cashPaid) {
        if (mFragMgr.findFragmentByTag(TXN_CONFIRM_FRAGMENT) == null) {
            //setDrawerState(false);

            Fragment fragment = TxnConfirmFragment.getInstance(cashPaid);
            FragmentTransaction transaction = mFragMgr.beginTransaction();

            // Add over the existing fragment
            transaction.replace(R.id.fragment_container_1, fragment, TXN_CONFIRM_FRAGMENT);
            transaction.addToBackStack(TXN_CONFIRM_FRAGMENT);

            // Commit the transaction
            transaction.commit();
        }
    }

    private void startBillingFragment() {
        Fragment fragment = mFragMgr.findFragmentByTag(BILLING_FRAGMENT);
        if (fragment == null) {
            //getReadyForNewTransaction();
            //setDrawerState(false);
            // Create new fragment and transaction
            Fragment billFragment = new BillingFragment();
            FragmentTransaction transaction = mFragMgr.beginTransaction();

            // Replace whatever is in the fragment_container view with this fragment,
            // and add the transaction to the back stack
            transaction.replace(R.id.fragment_container_1, billFragment, BILLING_FRAGMENT);
            transaction.addToBackStack(BILLING_FRAGMENT);

            // Commit the transaction
            transaction.commit();
        }
    }

    private void startOrderListFragment() {
        Fragment fragment = mFragMgr.findFragmentByTag(ORDER_LIST_FRAGMENT);
        if (fragment == null) {
            //setDrawerState(false);
            // Create new fragment and transaction
            Fragment listFragment = new OrderListViewFragment();
            FragmentTransaction transaction = mFragMgr.beginTransaction();

            // Add over the existing fragment
            transaction.replace(R.id.fragment_container_1, listFragment, ORDER_LIST_FRAGMENT);
            transaction.addToBackStack(ORDER_LIST_FRAGMENT);

            // Commit the transaction
            transaction.commit();
        }
    }

    private void startCashTransFragment() {
        if(mWorkFragment.mCurrCashback==null || mWorkFragment.mCurrCashback.getCurrCbBalance()==-1) {
            // means cashback object is null and not fetched yet from backend
            // we need to wait till it get fetched or error happens
            // set flag to start 'transaction fragment' in the response to fetch cashback request
            AppCommonUtil.showProgressDialog(this, AppConstants.progressDefault);
            mCashTxnStartPending = true;
        } else {
            if(mCashTxnStartPending) {
                AppCommonUtil.cancelProgressDialog(true);
                mCashTxnStartPending = false;
            }
            Fragment fragment = mFragMgr.findFragmentByTag(CASH_TRANS_FRAGMENT);
            if (fragment == null) {
                //setDrawerState(false);
                // Create new fragment and transaction
                Fragment transFragment = new CashTransactionFragment();
                FragmentTransaction transaction = mFragMgr.beginTransaction();

                // Add over the existing fragment
                transaction.replace(R.id.fragment_container_1, transFragment, CASH_TRANS_FRAGMENT);
                transaction.addToBackStack(CASH_TRANS_FRAGMENT);

                // Commit the transaction
                transaction.commit();
            }
        }
    }

    private void startMobileNumFragment() {
        mMobileNumFragment = (MobileNumberFragment) mFragMgr.findFragmentByTag(MOBILE_NUM_FRAGMENT);
        if (mMobileNumFragment == null) {
            //setDrawerState(false);
            mMobileNumFragment = new MobileNumberFragment();
            mFragMgr.beginTransaction()
                    .add(R.id.fragment_container_1, mMobileNumFragment, MOBILE_NUM_FRAGMENT)
                    .addToBackStack(MOBILE_NUM_FRAGMENT)
                    .commit();
        }
    }

    private void startCustomerListFrag() {
        if (mFragMgr.findFragmentByTag(CUSTOMER_LIST_FRAG) == null) {
            //setDrawerState(false);

            Fragment fragment = new CustomerListFragment();
            FragmentTransaction transaction = mFragMgr.beginTransaction();

            // Add over the existing fragment
            transaction.replace(R.id.fragment_container_1, fragment, CUSTOMER_LIST_FRAG);
            transaction.addToBackStack(CUSTOMER_LIST_FRAG);

            // Commit the transaction
            transaction.commit();
        }
    }

    private void startMerchantOpsFrag() {
        if (mFragMgr.findFragmentByTag(MERCHANT_OPS_LIST_FRAG) == null) {

            Fragment fragment = new MerchantOpListFrag();
            FragmentTransaction transaction = mFragMgr.beginTransaction();

            // Add over the existing fragment
            transaction.replace(R.id.fragment_container_1, fragment, MERCHANT_OPS_LIST_FRAG);
            transaction.addToBackStack(MERCHANT_OPS_LIST_FRAG);

            // Commit the transaction
            transaction.commit();
        }
    }

    @Override
    public void showDashboardDetails(int dbType) {
        startDashboardFragment(dbType);
    }

    private void startDashboardFragment(int dbType) {
        if (mFragMgr.findFragmentByTag(DASHBOARD_FRAGMENT) == null) {
            //setDrawerState(false);

            Fragment fragment = DashboardTxnFragment.getInstance(dbType);
            FragmentTransaction transaction = mFragMgr.beginTransaction();

            // Add over the existing fragment
            transaction.replace(R.id.fragment_container_1, fragment, DASHBOARD_FRAGMENT);
            transaction.addToBackStack(DASHBOARD_FRAGMENT);

            // Commit the transaction
            transaction.commit();
        }
    }

    private void startDBoardSummaryFrag() {
        if (mFragMgr.findFragmentByTag(DASHBOARD_SUMMARY_FRAG) == null) {
            //setDrawerState(false);

            Fragment fragment = new DashboardFragment();
            FragmentTransaction transaction = mFragMgr.beginTransaction();

            // Add over the existing fragment
            transaction.replace(R.id.fragment_container_1, fragment, DASHBOARD_SUMMARY_FRAG);
            transaction.addToBackStack(DASHBOARD_SUMMARY_FRAG);

            // Commit the transaction
            transaction.commit();
        }
    }

    private void startTrustedDevicesFragment() {
        if (mFragMgr.findFragmentByTag(TRUSTED_DEVICES_FRAGMENT) == null) {
            //setDrawerState(false);

            Fragment fragment = new TrustedDevicesFragment();
            FragmentTransaction transaction = mFragMgr.beginTransaction();

            // Add over the existing fragment
            transaction.replace(R.id.fragment_container_1, fragment, TRUSTED_DEVICES_FRAGMENT);
            transaction.addToBackStack(TRUSTED_DEVICES_FRAGMENT);

            // Commit the transaction
            transaction.commit();
        }
    }

    @Override
    public MyRetainedFragment getRetainedFragment() {
        return mWorkFragment;
    }

    @Override
    public void onTotalBill() {
        startCashTransFragment();
    }

    @Override
    public void onTotalBillFromOrderList() {
        startCashTransFragment();
    }

    @Override
    public void onViewOrderList() {
        startOrderListFragment();
    }

    @Override
    public void onBackPressed() {
        LogMy.d(TAG,"In onBackPressed: "+mFragMgr.getBackStackEntryCount());

        if (this.mDrawer.isDrawerOpen(GravityCompat.START)) {
            this.mDrawer.closeDrawer(GravityCompat.START);
            return;
        }

        // persist settings changes when back button is pressed
        // if 'settings changed' case, then dont pop fragment now
        // fragment should be popped after receiving response for update of merchant
        SettingsFragment settingsFrag = (SettingsFragment) mFragMgr.findFragmentByTag(SETTINGS_FRAGMENT);
        if ( settingsFrag!=null &&
                settingsFrag.isVisible() &&
                settingsFrag.isSettingsChanged() ) {
            onSettingsChange();
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
        LogMy.d(TAG,"In onPostCreate");
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();

        if(savedInstanceState==null) {
            // activity is re-created and not just re-started
            // Archive txns (all but today's) once a day
            DateUtil todayMidnight = new DateUtil();
            todayMidnight.toMidnight();

            if(mMerchant.getLast_txn_archive()==null ||
                    mMerchant.getLast_txn_archive().getTime() < todayMidnight.getTime().getTime()) {
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
        LogMy.d(TAG, "In onResume");
        super.onResume();
        if(AppCommonUtil.getProgressDialogMsg()!=null) {
            AppCommonUtil.showProgressDialog(this, AppCommonUtil.getProgressDialogMsg());
        }
        setDrawerState(true);
        AppCommonUtil.setUserType(DbConstants.USER_TYPE_MERCHANT);
    }

    @Override
    protected void onPause() {
        LogMy.d(TAG,"In onPause");
        super.onPause();
        AppCommonUtil.cancelProgressDialog(false);
        setDrawerState(false);
    }

    @Override
    protected void onDestroy() {
        LogMy.d(TAG,"In onDestroy");
        super.onDestroy();
        //MerchantUser.reset();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        LogMy.d(TAG,"In onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putBoolean("mCashTxnStartPending", mCashTxnStartPending);
        outState.putBoolean("mExitAfterLogout", mExitAfterLogout);
        outState.putBoolean("mTbImageIsMerchant", mTbImageIsMerchant);
        outState.putInt("mLastMenuItemId", mLastMenuItemId);
    }

    @Override
    public void onBgThreadCreated() {
        // nothing to do
    }
}

    /*
    private void onChangeMobileResponse(int errorCode) {
        LogMy.d(TAG, "In onChangeMobileResponse: " + errorCode);
        AndroidUtil.cancelProgressDialog();

        String merchantOpCode = mWorkFragment.mMerchantOp.getOp_code();
        if(errorCode==ErrorCodes.NO_ERROR) {
            String successMsg = null;

            switch (merchantOpCode) {
                case DbConstants.MERCHANT_OP_CHANGE_MOBILE:
                    successMsg = AppConstants.mobileChangeSuccessMsg;
                    break;
            }

            DialogFragmentWrapper.createNotification(AppConstants.defaultSuccessTitle, successMsg, false, false)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);

            // update local merchant instance
            mMerchantUser.setMobileNum(mWorkFragment.mMerchantOp.getExtra_op_params());

            // merchant operation success, reset to null
            mWorkFragment.mMerchantOp = null;

        } else if(errorCode==ErrorCodes.OTP_GENERATED) {
            // OTP sent successfully to new mobile, ask for the same
            mWorkFragment.mMerchantOp.setOp_status(DbConstants.MERCHANT_OP_STATUS_OTP_GENERATED);

            if(merchantOpCode.equals(DbConstants.MERCHANT_OP_CHANGE_MOBILE)) {
                // show the 'mobile change preference' again
                // create if fragment don't already exist
                SettingsFragment settingsFrag = (SettingsFragment) mFragMgr.findFragmentByTag(SETTINGS_FRAGMENT);
                if (settingsFrag==null) {
                    settingsFrag = startSettingsFragment();
                }
                if( settingsFrag==null || !settingsFrag.showChangeMobilePreference() ) {
                    // if failed to show prefernce for some reason - ask user to do so manually
                    DialogFragmentWrapper.createNotification(ErrorCodes.generalInfoTitle,
                            "OTP generated successfully. Request to try change mobile number again, after OTP is received on mobile.", false, false)
                            .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
                }
            }
        } else {
            // reset in case of any error
            mWorkFragment.mMerchantOp = null;
            DialogFragmentWrapper.createNotification(ErrorCodes.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                    .show(mFragMgr, DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    @Override
    public MerchantOps getMobileChangeMerchantOp() {
        if(mWorkFragment.mMerchantOp != null &&
                mWorkFragment.mMerchantOp.getOp_code().equals(DbConstants.MERCHANT_OP_CHANGE_MOBILE)) {
            return mWorkFragment.mMerchantOp;
        }
        return null;
    }*/

    /*
    private void updateTbForCustomer(String errorString) {
        mTbLayoutSubhead1.setVisibility(View.VISIBLE);

        if(errorString==null) {
            // no error case: all cashback values available
            mTbTitle.setText(AndroidUtil.getHalfVisibleId(mWorkFragment.mCustMobile));
            // display image
            mTbLayoutImage.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_border_coconut));
            setTbImage(R.drawable.ic_green_tick);

            if(mWorkFragment.mCurrCustomer.getStatus()!=DbConstants.USER_STATUS_ACTIVE ) {
                mTbTitle2.setVisibility(View.VISIBLE);
                //String custName = "~ "+mWorkFragment.mCurrCashback.getCashback().getCustomer().getName();
                mTbTitle2.setText(DbConstants.userStatusDesc[mWorkFragment.mCurrCustomer.getStatus()]);
            }
            mTbSubhead1Text1.setText(String.valueOf(mWorkFragment.mCurrCashback.getCurrClBalance()));
            mTbSubhead1Divider.setVisibility(View.VISIBLE);
            mTbSubhead1Text2.setVisibility(View.VISIBLE);
            mTbSubhead1Text2.setText(String.valueOf(mWorkFragment.mCurrCashback.getCurrCbBalance()));
        } else {
            // error case
            if(mWorkFragment.mCustMobile==null) {
                mTbTitle.setText(AndroidUtil.getHalfVisibleId(mWorkFragment.mCustCardId));
            } else {
                mTbTitle.setText(AndroidUtil.getHalfVisibleId(mWorkFragment.mCustMobile));
            }
            mTbLayoutImage.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_border_fujired));
            setTbImage(R.drawable.ic_red_cross);
            mTbSubhead1Text1.setText(errorString);
            mTbSubhead1Divider.setVisibility(View.GONE);
            mTbSubhead1Text2.setVisibility(View.GONE);
        }
    }*/

