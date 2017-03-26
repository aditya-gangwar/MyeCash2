package in.myecash.merchantbase;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.ValidationHelper;
import in.myecash.common.CommonUtils;
import in.myecash.common.MyGlobalSettings;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.merchantbase.entities.MerchantUser;
import in.myecash.merchantbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 26-03-2017.
 */

public class SettingsFragment2 extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {

    private static final String TAG = "MchntApp-SettingsFragment";

    private static final int REQ_NOTIFICATION = 0;

    public static final String KEY_GRP_CB = "prefGrpCb";
    public static final String KEY_GRP_ACC = "prefGrpAcc";
    public static final String KEY_GRP_PROFILE = "prefGrpProfile";
    //public static final String KEY_GRP_INVOICE = "prefGrpInvoice";
    public static final String KEY_GRP_OTHERS = "prefGrpOthers";

    //public static final String KEY_CAT_ACC = "prefCatAcc";
    //public static final String KEY_CAT_CB = "prefCatCb";

    public static final String KEY_CB_RATE = "settings_cb_rate";
    public static final String KEY_ADD_CL_ENABLED = "settings_cl_add_enabled";
    public static final String KEY_PP_CB_RATE = "settings_ppcb_rate";
    public static final String KEY_PP_MIN_AMT = "settings_ppcb_amt";

    public static final String KEY_MOBILE_NUM = "settings_change_mobile";
    public static final String KEY_EMAIL = "settings_email_id";
    public static final String KEY_CONTACT_PHONE = "settings_contact_num";

    public static final String KEY_LINKED_INV = "settings_linked_invoice";
    public static final String KEY_LINKED_INV_MANDATORY = "settings_invoice_mandatory";
    public static final String KEY_LINKED_INV_ONLY_NMBRS = "settings_invoice_numbers_only";
    public static final String KEY_CAMERA_FLASH = "settings_camera_flash";

    private MerchantUser mMerchantUser;

    // store-restore as part of instance state
    private boolean mSettingsChanged;

    // Container Activity must implement this interface
    public interface SettingsFragment2If {
        MyRetainedFragment getRetainedFragment();
        void setDrawerState(boolean isEnabled);
    }
    private SettingsFragment2If mCallback;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LogMy.d(TAG, "In onActivityCreated");

        try {
            mCallback = (SettingsFragment2If) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement SettingsFragment2If");
        }

        //TODO: use '?android:attr/windowBackground' instead of white
        getView().setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.white));
        getView().setClickable(true);

        //setAllSummaries();

        // Set Icons
        PreferenceScreen pref = (PreferenceScreen) getPreferenceScreen().findPreference(KEY_GRP_PROFILE);
        Drawable icon = AppCommonUtil.getTintedDrawable(getActivity(), R.drawable.ic_perm_identity_white_24dp, R.color.primary);
        pref.setIcon(icon);

        pref = (PreferenceScreen) getPreferenceScreen().findPreference(KEY_GRP_OTHERS);
        icon = AppCommonUtil.getTintedDrawable(getActivity(), R.drawable.ic_more_horiz_white_24dp, R.color.primary);
        pref.setIcon(icon);

        pref = (PreferenceScreen) getPreferenceScreen().findPreference(KEY_GRP_CB);
        icon = AppCommonUtil.getTintedDrawable(getActivity(), R.drawable.ic_favorite_white_24dp, R.color.primary);
        pref.setIcon(icon);

        pref = (PreferenceScreen) getPreferenceScreen().findPreference(KEY_GRP_ACC);
        icon = AppCommonUtil.getTintedDrawable(getActivity(), R.drawable.ic_account_balance_wallet_white_24dp, R.color.primary);
        pref.setIcon(icon);
        //pref.setSelectable(false);
        pref.setEnabled(false);
        pref.setShouldDisableView(true);
        pref.setSummary("Not available in FREE plan. Contact Customer Care to Upgrade");
        /*pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AppCommonUtil.toast(getActivity(),"Not available in FREE plan");
                return true;
            }
        });*/

        /*PreferenceCategory pref1 = (PreferenceCategory) getPreferenceScreen().findPreference(KEY_CAT_ACC);
        icon = AppCommonUtil.getTintedDrawable(getActivity(), R.drawable.ic_account_balance_wallet_white_18dp, R.color.primary);
        pref1.setIcon(icon);

        pref1 = (PreferenceCategory) getPreferenceScreen().findPreference(KEY_CAT_CB);
        icon = AppCommonUtil.getTintedDrawable(getActivity(), R.drawable.ic_favorite_white_18dp, R.color.primary);
        pref1.setIcon(icon);*/
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMerchantUser  = MerchantUser.getInstance();
        mSettingsChanged = (savedInstanceState!=null) && savedInstanceState.getBoolean("mSettingsChanged");

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        Preference pref = getPreferenceScreen().findPreference(KEY_CB_RATE);
        pref.setOnPreferenceChangeListener(this);
        pref = getPreferenceScreen().findPreference(KEY_ADD_CL_ENABLED);
        pref.setOnPreferenceChangeListener(this);
        pref = getPreferenceScreen().findPreference(KEY_PP_CB_RATE);
        pref.setOnPreferenceChangeListener(this);
        pref = getPreferenceScreen().findPreference(KEY_PP_MIN_AMT);
        pref.setOnPreferenceChangeListener(this);
        pref = getPreferenceScreen().findPreference(KEY_EMAIL);
        pref.setOnPreferenceChangeListener(this);
        pref = getPreferenceScreen().findPreference(KEY_CONTACT_PHONE);
        pref.setOnPreferenceChangeListener(this);
        pref = getPreferenceScreen().findPreference(KEY_LINKED_INV);
        pref.setOnPreferenceChangeListener(this);
        pref = getPreferenceScreen().findPreference(KEY_LINKED_INV_MANDATORY);
        pref.setOnPreferenceChangeListener(this);
        pref = getPreferenceScreen().findPreference(KEY_LINKED_INV_ONLY_NMBRS);
        pref.setOnPreferenceChangeListener(this);
    }

    /*
     * Hack to show toolbar - taken from below:
     * http://stackoverflow.com/questions/26509180/no-actionbar-in-preferenceactivity-after-upgrade-to-support-library-v21/27455363#27455363
     * https://github.com/davcpas1234/MaterialSettings/tree/master/app/src/main
     */
    @SuppressWarnings("deprecation")
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        // If the user has clicked on a preference screen, set up the screen
        if (preference instanceof PreferenceScreen) {
            setUpNestedScreen((PreferenceScreen) preference);
        }

        return false;
    }

    public void setUpNestedScreen(PreferenceScreen preferenceScreen) {
        final Dialog dialog = preferenceScreen.getDialog();

        AppBarLayout appBar;

        View listRoot = dialog.findViewById(android.R.id.list);
        ViewGroup mRootView = (ViewGroup) dialog.findViewById(android.R.id.content);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LinearLayout root = (LinearLayout) dialog.findViewById(android.R.id.list).getParent().getParent();
            appBar = (AppBarLayout) LayoutInflater.from(getActivity()).inflate(R.layout.settings_toolbar, root, false);
            root.addView(appBar, 0);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            LinearLayout root = (LinearLayout) dialog.findViewById(android.R.id.list).getParent();
            appBar = (AppBarLayout) LayoutInflater.from(getActivity()).inflate(R.layout.settings_toolbar, root, false);
            root.addView(appBar, 0);
        } else {
            ListView content = (ListView) mRootView.getChildAt(0);
            mRootView.removeAllViews();

            LinearLayout LL = new LinearLayout(getActivity());
            LL.setOrientation(LinearLayout.VERTICAL);

            ViewGroup.LayoutParams LLParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            LL.setLayoutParams(LLParams);

            appBar = (AppBarLayout) LayoutInflater.from(getActivity()).inflate(R.layout.settings_toolbar, mRootView, false);

            LL.addView(appBar);
            LL.addView(content);

            mRootView.addView(LL);
        }

        Toolbar Tbar = (Toolbar) appBar.getChildAt(0);

        Tbar.setTitle(preferenceScreen.getTitle());

        Tbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        LogMy.d(TAG, "In onSharedPreferenceChanged");
        int errorCode = ErrorCodes.NO_ERROR;
        String key = preference.getKey();
        String newValue = null;
        String errDesc = null;

        if (key.equals(KEY_CB_RATE)) {
            newValue = (String)o;
            errorCode = ValidationHelper.validateCbRate(newValue);
            if (errorCode == ErrorCodes.NO_ERROR) {
                mMerchantUser.setNewCbRate(newValue);
                mSettingsChanged = true;
                setCbRateSummary(newValue, false);
            }
        } else if (key.equals(KEY_ADD_CL_ENABLED)) {
            //boolean isAddClEnabled = sharedPreferences.getBoolean(KEY_ADD_CL_ENABLED, mMerchantUser.getMerchant().getCl_add_enable());
            boolean isAddClEnabled = (boolean)o;
            //if (isAddClEnabled != mMerchantUser.getMerchant().getCl_add_enable()) {
            mMerchantUser.setNewIsAddClEnabled(isAddClEnabled);
            mSettingsChanged = true;
            setAddCashSummary(isAddClEnabled, false);

            // as they depend on it
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            setPpCbRateSummary(prefs.getString(KEY_PP_CB_RATE,null), !isAddClEnabled, isAddClEnabled);
            setPpMinAmtSummary(prefs.getString(KEY_PP_MIN_AMT,null), !isAddClEnabled, isAddClEnabled);
            //}
        } else if (key.equals(KEY_PP_CB_RATE)) {
            newValue = (String)o;
            errorCode = ValidationHelper.validateCbRate(newValue);
            if (errorCode == ErrorCodes.NO_ERROR) {
                mMerchantUser.setNewPpCbRate(newValue);
                mSettingsChanged = true;
                setPpCbRateSummary(newValue, false, null);
            }
        } else if (key.equals(KEY_PP_MIN_AMT)) {
            newValue = (String)o;
            if(newValue==null || newValue.isEmpty()) {
                errorCode = ErrorCodes.EMPTY_VALUE;
            } else {
                try {
                    int newVal = Integer.valueOf(newValue);
                    if (newVal > MyGlobalSettings.getCashAccLimit()) {
                        errDesc = "Value more than Cash Account Limit of " +
                                AppCommonUtil.getValueAmtStr(MyGlobalSettings.getCashAccLimit().toString());
                        errorCode = ErrorCodes.INVALID_VALUE;
                    } else {
                        mMerchantUser.setNewPpMinAmt(newVal);
                        mSettingsChanged = true;
                        setPpMinAmtSummary(newValue, false, null);
                    }
                } catch (Exception e) {
                    errorCode = ErrorCodes.INVALID_FORMAT;
                }
            }
        } else if (key.equals(KEY_EMAIL)) {
            //newValue = sharedPreferences.getString(KEY_EMAIL, null);
            newValue = (String)o;
            errorCode = ValidationHelper.validateEmail(newValue);
            if (errorCode == ErrorCodes.NO_ERROR) {
                mMerchantUser.setNewEmail(newValue);
                mSettingsChanged = true;
                setEmailSummary(newValue);
            }
        } else if (key.equals(KEY_CONTACT_PHONE)) {
            //newValue = sharedPreferences.getString(KEY_CONTACT_PHONE, null);
            newValue = (String)o;
            errorCode = ValidationHelper.validateMobileNo(newValue);
            if (errorCode == ErrorCodes.NO_ERROR) {
                mMerchantUser.setNewContactPhome(newValue);
                mSettingsChanged = true;
                setContactPhoneSummary(newValue);
            }
        } else if (key.equals(KEY_LINKED_INV)) {
            boolean askLinkedInvNum = (boolean)o;
            //if (askLinkedInvNum != mMerchantUser.getMerchant().isInvoiceNumAsk()) {
            mMerchantUser.setNewInvNumAsk(askLinkedInvNum);
            mSettingsChanged = true;
            //}
        } else if (key.equals(KEY_LINKED_INV_MANDATORY)) {
            boolean linkedInvOptional = (boolean)o;
            //if (linkedInvOptional != mMerchantUser.getMerchant().isInvoiceNumOptional()) {
            mMerchantUser.setNewInvNumOptional(!linkedInvOptional);
            mSettingsChanged = true;
            //}
        } else if (key.equals(KEY_LINKED_INV_ONLY_NMBRS)) {
            boolean linkedInvOnlyNmbrs = (boolean)o;
            //if (linkedInvOnlyNmbrs != mMerchantUser.getMerchant().isInvoiceNumOnlyNumbers()) {
            mMerchantUser.setNewInvNumOnlyNumbers(linkedInvOnlyNmbrs);
            mSettingsChanged = true;
            //}
        }

        if (errorCode != ErrorCodes.NO_ERROR &&
                !key.equals(KEY_CAMERA_FLASH)) {
            if(errDesc==null) {
                errDesc = AppCommonUtil.getErrorDesc(errorCode);
            }
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, errDesc, true, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            return false;
        }

        return true;
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        LogMy.d(TAG, "In onSharedPreferenceChanged");
    }

    private void setAllSummaries() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        boolean disablePref = false;
        if(mMerchantUser.getMerchant().getAdmin_status()== DbConstants.USER_STATUS_UNDER_CLOSURE) {
            disablePref = true;
        }

        setCbRateSummary(prefs.getString(KEY_CB_RATE,null), disablePref);
        setAddCashSummary(prefs.getBoolean(KEY_ADD_CL_ENABLED, false), disablePref);

        if(!disablePref &&
                !prefs.getBoolean(KEY_ADD_CL_ENABLED, mMerchantUser.getMerchant().getCl_add_enable())) {
            disablePref = true;
        }
        setPpCbRateSummary(prefs.getString(KEY_PP_CB_RATE,null), disablePref, null);
        setPpMinAmtSummary(prefs.getString(KEY_PP_MIN_AMT,null), disablePref, null);

        setMobileNumSummary(prefs.getString(KEY_MOBILE_NUM, null));
        setContactPhoneSummary(prefs.getString(KEY_CONTACT_PHONE, null));
        setEmailSummary(prefs.getString(KEY_EMAIL, null));
    }

    private void setCbRateSummary(String value, boolean disable) {
        if(null==value) {
            return;
        }
        Preference pref = findPreference(KEY_CB_RATE);
        if(disable) {
            pref.setSummary("Won't apply as Merchant under 'Expiry' Notice period.");
            pref.setEnabled(false);
        } else {
            String summary = String.format(getActivity().getString(R.string.addCBSummary), value);
            pref.setSummary(summary);
            pref.setEnabled(true);
        }
    }

    private void setAddCashSummary(boolean value, boolean disable) {
        Preference pref = findPreference(KEY_ADD_CL_ENABLED);
        if(disable) {
            pref.setSummary("Won't apply as Merchant under 'Expiry' Notice period.");
            pref.setEnabled(false);
        } else {
            String summary = String.format(getActivity().getString(R.string.addCashSummary), value?"Disable":"Enable");
            pref.setSummary(summary);
            pref.setEnabled(true);
        }
    }

    private void setPpCbRateSummary(String value, boolean disable, Boolean addCashEnabled) {
        if(null==value) {
            return;
        }
        Preference pref = findPreference(KEY_PP_CB_RATE);
        if(disable) {
            if(addCashEnabled==null) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                addCashEnabled = prefs.getBoolean(KEY_ADD_CL_ENABLED,
                        mMerchantUser.getMerchant().getCl_add_enable());
            }

            if(addCashEnabled) {
                pref.setSummary("Won't apply as Merchant under 'Expiry' Notice period.");
            } else {
                pref.setSummary("Won't apply as 'Add Cash to Account' is Disabled.");
            }
            pref.setEnabled(false);
            pref.setSelectable(false);
        } else {
            String summary = String.format(getActivity().getString(R.string.addExtraCBSummary), value);
            pref.setSummary(summary);
            pref.setEnabled(true);
            pref.setSelectable(true);
        }
    }

    private void setPpMinAmtSummary(String value, boolean disable, Boolean addCashEnabled) {
        if(null==value) {
            return;
        }
        Preference pref = findPreference(KEY_PP_MIN_AMT);
        if(disable) {
            if(addCashEnabled==null) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                addCashEnabled = prefs.getBoolean(KEY_ADD_CL_ENABLED,
                        mMerchantUser.getMerchant().getCl_add_enable());
            }

            if(addCashEnabled) {
                pref.setSummary("Won't apply as Merchant under 'Expiry' Notice period.");
            } else {
                pref.setSummary("Won't apply as 'Add Cash to Account' is Disabled.");
            }
            pref.setEnabled(false);
            pref.setSelectable(false);
        } else {
            String summary = String.format(getActivity().getString(R.string.minCashExtraCBSummary), value);
            pref.setSummary(summary);
            pref.setEnabled(true);
            pref.setSelectable(true);
        }
    }

    private void setMobileNumSummary(String value) {
        if(null==value) {
            return;
        }
        String summary = String.format(getActivity().getString(R.string.regdMobileSummary), CommonUtils.getPartialVisibleStr(value));
        Preference pref = findPreference(KEY_MOBILE_NUM);
        pref.setSummary(summary);
    }

    private void setEmailSummary(String value) {
        if(null==value) {
            return;
        }
        String summary = String.format(getActivity().getString(R.string.emailSummary), value);
        Preference pref = findPreference(KEY_EMAIL);
        pref.setSummary(summary);
    }

    private void setContactPhoneSummary(String value) {
        if(null==value) {
            return;
        }
        String summary = String.format(getActivity().getString(R.string.contactPhoneSummary), value);
        Preference pref = findPreference(KEY_CONTACT_PHONE);
        pref.setSummary(summary);
    }

    @Override
    public void onResume() {
        LogMy.d(TAG, "In onResume");
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        mCallback.setDrawerState(false);
        setAllSummaries();
        mCallback.getRetainedFragment().setResumeOk(true);
    }

    @Override
    public void onPause() {
        LogMy.d(TAG, "In onPause");
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    public boolean isSettingsChanged() {
        return mSettingsChanged;
    }

    public boolean showChangeMobilePreference() {
        MobileChangePreference mobilePref = (MobileChangePreference) findPreference("settings_change_mobile");
        if(mobilePref != null) {
            mobilePref.showDialog(null);
            return true;
        } else {
            LogMy.e(TAG, "Not able to find mobile change preference");
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("mSettingsChanged", mSettingsChanged);
    }

}
