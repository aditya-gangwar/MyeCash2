package in.myecash.merchantbase;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import in.myecash.appbase.constants.AppConstants;
import in.myecash.common.CommonUtils;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.ValidationHelper;
import in.myecash.merchantbase.entities.MerchantUser;
import in.myecash.merchantbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 30-03-2016.
 */
public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {

    private static final String TAG = "MchntApp-SettingsFragment";

    private static final int REQ_NOTIFICATION = 0;

    public static final String KEY_CB_RATE = "settings_cb_rate";
    public static final String KEY_ADD_CL_ENABLED = "settings_cl_add_enabled";
    public static final String KEY_PP_CB_RATE = "settings_ppcb_rate";
    public static final String KEY_PP_MIN_AMT = "settings_ppcb_amt";

    public static final String KEY_MOBILE_NUM = "settings_change_mobile";
    public static final String KEY_EMAIL = "settings_email_id";
    public static final String KEY_CONTACT_PHONE = "settings_contact_num";

    public static final String KEY_LINKED_INV = "settings_linked_invoice";
    public static final String KEY_LINKED_INV_OPTIONAL = "settings_invoice_optional";
    public static final String KEY_LINKED_INV_ONLY_NMBRS = "settings_invoice_numbers_only";
    public static final String KEY_CAMERA_FLASH = "settings_camera_flash";

    private MerchantUser mMerchantUser;

    // store-restore as part of instance state
    private boolean mSettingsChanged;

    // Container Activity must implement this interface
    public interface SettingsFragmentIf {
        MyRetainedFragment getRetainedFragment();
        void setDrawerState(boolean isEnabled);
    }
    private SettingsFragmentIf mCallback;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LogMy.d(TAG, "In onActivityCreated");

        try {
            mCallback = (SettingsFragmentIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement SettingsFragmentIf");
        }

        //TODO: use '?android:attr/windowBackground' instead of white
        getView().setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.white));
        getView().setClickable(true);

        setAllSummaries();
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
        pref = getPreferenceScreen().findPreference(KEY_LINKED_INV_OPTIONAL);
        pref.setOnPreferenceChangeListener(this);
        pref = getPreferenceScreen().findPreference(KEY_LINKED_INV_ONLY_NMBRS);
        pref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        LogMy.d(TAG, "In onSharedPreferenceChanged");
        int errorCode = ErrorCodes.NO_ERROR;
        String key = preference.getKey();
        String newValue = null;

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
            //}
        } else if (key.equals(KEY_PP_CB_RATE)) {
            newValue = (String)o;
            errorCode = ValidationHelper.validateCbRate(newValue);
            if (errorCode == ErrorCodes.NO_ERROR) {
                mMerchantUser.setNewPpCbRate(newValue);
                mSettingsChanged = true;
                setPpCbRateSummary(newValue, false);
            }
        } else if (key.equals(KEY_PP_MIN_AMT)) {
            newValue = (String)o;
            //errorCode = ValidationHelper.validateCbRate(newValue);
            //if (errorCode == ErrorCodes.NO_ERROR) {
                mMerchantUser.setNewPpMinAmt(Integer.valueOf(newValue));
                mSettingsChanged = true;
                setPpMinAmtSummary(newValue, false);
            //}
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
        } else if (key.equals(KEY_LINKED_INV_OPTIONAL)) {
            boolean linkedInvOptional = (boolean)o;
            //if (linkedInvOptional != mMerchantUser.getMerchant().isInvoiceNumOptional()) {
                mMerchantUser.setNewInvNumOptional(linkedInvOptional);
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
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), true, true)
                .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            return false;
        }

        return true;
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        LogMy.d(TAG, "In onSharedPreferenceChanged");
        /*
        String newValue;
        try {
            int errorCode = AppCommonUtil.isNetworkAvailableAndConnected(getActivity());
            if (errorCode == ErrorCodes.NO_ERROR) {
                if (key.equals(KEY_CB_RATE)) {
                    newValue = sharedPreferences.getString(KEY_CB_RATE, null);
                    errorCode = ValidationHelper.validateCbRate(newValue);
                    if (errorCode == ErrorCodes.NO_ERROR) {
                        mMerchantUser.setNewCbRate(newValue);
                        mSettingsChanged = true;
                        setCbRateSummary(newValue, false);
                    }
                } else if (key.equals(KEY_ADD_CL_ENABLED)) {
                    boolean isAddClEnabled = sharedPreferences.getBoolean(KEY_ADD_CL_ENABLED, mMerchantUser.getMerchant().getCl_add_enable());
                    if (isAddClEnabled != mMerchantUser.getMerchant().getCl_add_enable()) {
                        mMerchantUser.setNewIsAddClEnabled(isAddClEnabled);
                        mSettingsChanged = true;
                        setAddCashSummary(isAddClEnabled, false);
                    }
                } else if (key.equals(KEY_EMAIL)) {
                    newValue = sharedPreferences.getString(KEY_EMAIL, null);
                    errorCode = ValidationHelper.validateEmail(newValue);
                    if (errorCode == ErrorCodes.NO_ERROR) {
                        mMerchantUser.setNewEmail(newValue);
                        mSettingsChanged = true;
                        setEmailSummary(newValue);
                    } else {
                        AppCommonUtil.toast(getActivity(), AppCommonUtil.getErrorDesc(errorCode));
                    }
                } else if (key.equals(KEY_CONTACT_PHONE)) {
                    newValue = sharedPreferences.getString(KEY_CONTACT_PHONE, null);
                    errorCode = ValidationHelper.validateMobileNo(newValue);
                    if (errorCode == ErrorCodes.NO_ERROR) {
                        mMerchantUser.setNewContactPhome(newValue);
                        mSettingsChanged = true;
                        setContactPhoneSummary(newValue);
                    } else {
                        AppCommonUtil.toast(getActivity(), AppCommonUtil.getErrorDesc(errorCode));
                    }
                } else if (key.equals(KEY_LINKED_INV)) {
                    boolean askLinkedInvNum = sharedPreferences.getBoolean(KEY_LINKED_INV, mMerchantUser.getMerchant().isInvoiceNumAsk());
                    if (askLinkedInvNum != mMerchantUser.getMerchant().isInvoiceNumAsk()) {
                        mMerchantUser.setNewInvNumAsk(askLinkedInvNum);
                        mSettingsChanged = true;
                    }
                } else if (key.equals(KEY_LINKED_INV_OPTIONAL)) {
                    boolean linkedInvOptional = sharedPreferences.getBoolean(KEY_LINKED_INV_OPTIONAL, mMerchantUser.getMerchant().isInvoiceNumOptional());
                    if (linkedInvOptional != mMerchantUser.getMerchant().isInvoiceNumOptional()) {
                        mMerchantUser.setNewInvNumOptional(linkedInvOptional);
                        mSettingsChanged = true;
                    }
                } else if (key.equals(KEY_LINKED_INV_ONLY_NMBRS)) {
                    boolean linkedInvOnlyNmbrs = sharedPreferences.getBoolean(KEY_LINKED_INV_ONLY_NMBRS, mMerchantUser.getMerchant().isInvoiceNumOnlyNumbers());
                    if (linkedInvOnlyNmbrs != mMerchantUser.getMerchant().isInvoiceNumOnlyNumbers()) {
                        mMerchantUser.setNewInvNumOnlyNumbers(linkedInvOnlyNmbrs);
                        mSettingsChanged = true;
                    }
                }
            }

            if (errorCode != ErrorCodes.NO_ERROR &&
                    !key.equals(KEY_CAMERA_FLASH)) {
                DialogFragmentWrapper dialog = DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true);
                dialog.setTargetFragment(this, REQ_NOTIFICATION);
                dialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            }
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in Fragment: ", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }*/
    }

    private void setAllSummaries() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        boolean disablePref = false;
        if(mMerchantUser.getMerchant().getAdmin_status()== DbConstants.USER_STATUS_UNDER_CLOSURE) {
            disablePref = true;
        }

        setCbRateSummary(prefs.getString(KEY_CB_RATE,null), disablePref);
        setAddCashSummary(prefs.getBoolean(KEY_ADD_CL_ENABLED, false), disablePref);
        setPpCbRateSummary(prefs.getString(KEY_PP_CB_RATE,null), disablePref);
        setPpMinAmtSummary(prefs.getString(KEY_PP_MIN_AMT,null), disablePref);

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
            pref.setSummary("Not allowed to change, as Cashback Credit not allowed in 'expiry' notice period");
            pref.setEnabled(false);
        } else {
            String summary = String.format("%s%% Cashback on Bill Amount.\n0 means Disabled.", value);
            pref.setSummary(summary);
        }
    }

    private void setAddCashSummary(boolean value, boolean disable) {
        Preference pref = findPreference(KEY_ADD_CL_ENABLED);
        if(disable) {
            pref.setSummary("Not allowed to change, as Cash Account Credit not allowed in 'expiry' notice period");
            pref.setEnabled(false);
        } else {
            String summary = String.format("%s add cash to customer account.", value?"Disable":"Enable");
            pref.setSummary(summary);
        }
    }

    private void setPpCbRateSummary(String value, boolean disable) {
        if(null==value) {
            return;
        }
        Preference pref = findPreference(KEY_PP_CB_RATE);
        if(disable) {
            pref.setSummary("Not allowed to change, as Prepaid Credit not allowed in 'Account Expiry' notice period");
            pref.setEnabled(false);
        } else {
            String summary = String.format("%s%% extra Cashback on Prepaid Amount.\n0 means Disabled.", value);
            pref.setSummary(summary);
        }
    }

    private void setPpMinAmtSummary(String value, boolean disable) {
        if(null==value) {
            return;
        }
        Preference pref = findPreference(KEY_PP_MIN_AMT);
        if(disable) {
            pref.setSummary("Not allowed to change, as Prepaid Credit not allowed in 'Account Expiry' notice period");
            pref.setEnabled(false);
        } else {
            String summary = String.format("%s is Minimum Prepaid Deposit for Extra Cashback.", value);
            pref.setSummary(summary);
        }
    }

    private void setMobileNumSummary(String value) {
        if(null==value) {
            return;
        }
        String summary = String.format("Change '%s' as registered mobile number.", CommonUtils.getPartialVisibleStr(value));
        Preference pref = findPreference(KEY_MOBILE_NUM);
        pref.setSummary(summary);
    }

    private void setEmailSummary(String value) {
        if(null==value) {
            return;
        }
        String summary = String.format("Change '%s' as contact email id.", value);
        Preference pref = findPreference(KEY_EMAIL);
        pref.setSummary(summary);
    }

    private void setContactPhoneSummary(String value) {
        if(null==value) {
            return;
        }
        String summary = String.format("Change '%s' as contact phone number.", value);
        Preference pref = findPreference(KEY_CONTACT_PHONE);
        pref.setSummary(summary);
    }

    @Override
    public void onResume() {
        LogMy.d(TAG, "In onResume");
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        mCallback.setDrawerState(false);
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
