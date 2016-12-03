package in.myecash.appbase.constants;

/**
 * This class contains constants relevant only the user app code and not to backend code.
 */
public class AppConstants {

    // Crashlytics custom keys
    public static final String CLTS_INPUT_CUST_MOBILE = "inputCustMobile";
    public static final String CLTS_INPUT_CUST_CARD = "inputCustCard";

    // Prefixes for downloadable files
    public static final String FILE_PREFIX_TXN_LIST = "MyeCash_txns_";
    public static final String FILE_PREFIX_CUSTOMER_LIST = "MyeCash_Customers_";

    // Titles/Msgs/hints shown on dialogues
    public static final String generalInfoTitle = "Information";
    public static final String msgInsecureDevice = "Your device is not secure. Please install and run application on other device.";
    public static final String titleAddTrustedDeviceOtp = "Add as Trusted Device";
    public static final String msgAddTrustedDeviceOtp = "First login from this device. Please enter OTP as received on registered number.";
    public static final String msgChangeMobileOtpGenerated = "OTP generated. Please enter the same on 'Change Mobile' screen through Settings.";

    public static final String hintEnterOtp = "Enter OTP";
    public static final String titleNewCardPin = "New MyeCash Card";
    public static final String msgNewCardPin = "Approve registration of new Card.\nKeep your PIN secret !!";
    public static final String titleChangeCustMobilePin = "Change Mobile";
    public static final String msgChangeCustMobilePin = "Approve change of registered Mobile to %s.\nKeep your PIN secret !!";

    // Msgs shown on toasts
    public static final String toastInProgress = "Operation already in progress. Please wait.";
    public static final String toastNoBalance = "Not enough balance";









    public static final String generalFailureTitle = "Error";
    public static final String regFailureTitle = "Registration Failed";
    public static final String noInternetTitle = "No Internet Connection";
    public static final String loginFailureTitle = "Login Failed";
    public static final String noPermissionTitle = "No Permissions";
    public static final String noDataFailureTitle = "No Data";

    public static int MOBILE_NUM_PROCESS_MIN_LENGTH = 8;
    public static final String SYMBOL_RS = "\u20B9 ";
    public static final String SYMBOL_RS_0 = "\u20B9 0";
    public static final String SYMBOL_DOWN_ARROW = "\u25BC";

    // Txn summary constants
    public static int INDEX_TXN_COUNT = 0;
    public static int INDEX_BILL_AMOUNT = 1;
    public static int INDEX_ADD_ACCOUNT = 2;
    public static int INDEX_DEBIT_ACCOUNT = 3;
    public static int INDEX_CASHBACK = 4;
    public static int INDEX_DEBIT_CASHBACK = 5;
    public static int INDEX_SUMMARY_MAX_VALUE = 6;

    // shared preference keys
    public static final String PREF_LOGIN_ID = "successLoginId";
    public static final String PREF_IMAGE_PATH_PREFIX = "merchantDp";
    public static final String PREF_MCHNT_STATS_PREFIX = "merchantStats";

    // Messages shown on popup dialogues
    public static final String defaultSuccessTitle = "Success";

    public static final String regConfirmTitle = "Merchant Registration";
    public static final String regConfirmMsg = "Are you sure all information given is correct ?";

    public static final String mchntRegSuccessMsg = "Registered Merchant Id is %s. Please ask Merchant to use 'Forget Password' link to get password and proceed further.";

    public static final String custOpNewCardSuccessMsg = "New Customer card registered successfully";
    //public static final String custOpResetPinSuccessMsg = "New PIN is sent to the registered customer mobile. Please try again, if not received in 5-10 mins.";
    public static final String custOpChangeMobileSuccessMsg = "Customer registered number updated successfully.";

    public static final String customerRegConfirmTitle = "Customer Registration Success";
    public static final String custRegSuccessMsg = "Customer PIN is sent to the provided mobile number. Please proceed with transaction.";

    public static final String deviceDeleteTitle = "Remove Trusted Device";
    public static final String deviceDeleteMsg = "Remove '%s' from Trusted Device list ? You will need to login again after device delete.";

    public static final String exitGenTitle = "Exit";
    public static final String exitRegActivityMsg = "You will loose all changes done.";
    public static final String exitAppMsg = "Are you sure to exit ?";

    public static final String logoutTitle = "Logout ?";
    public static final String logoutMsg = "Are you sure to logout ?";

    public static final String commitCashTransTitle = "Process transaction ?";
    public static final String commitTransSuccessTitle = "Transaction Success";
    public static final String commitTransFailureTitle = "Transaction Failure";

    public static final String itemDeleteConfirmTitle = "Delete Item ?";
    public static final String itemDeleteConfirmMsg = "Are you sure to delete ?";

    public static final String reportBlackoutTitle = "Reports not available";
    public static final String reportBlackoutMsg = "Reports are not available between %s and %s. Please try later.";

    public static final String pwdGenerateSuccessTitle = "Password reset success";
    public static final String genericPwdGenerateSuccessMsg = "Password generated and sent to your registered mobile number. If not received in few minutes, please try again.";

    public static final String pwdGenerateDuplicateRequestMsg = "Old Password reset request already pending. Please note that Password is sent to registered mobile, only %s minutes after request is submitted.";
    public static final String pwdGenerateSuccessMsg = "For security reasons, Password will be sent to your registered mobile number only after %s minutes. If not received by then, please try again.";
    //public static final String firstPwdGenerateSuccessMsg = "First password generated and sent to your registered mobile number. If not received in few minutes, please try again.";

    public static final String pwdChangeSuccessTitle = "Password changed";
    public static final String pwdChangeSuccessMsg = "Logging out as password changed. Please login with new password.";

    public static final String pinChangeSuccessMsg = "PIN changed successfully";
    public static final String pinChangeFailureTitle = "PIN Change Failed";

    public static final String pinResetFailureTitle = "PIN Change Failed";
    public static final String pinGenerateSuccessMsg = "For security reasons, PIN will be sent to your registered mobile number only after %s minutes. If not received by then, please try again.";
    public static final String pinGenerateDuplicateRequestMsg = "Old PIN reset request already pending. Please note that PIN is sent to registered mobile, only %s minutes after request is submitted.";

    public static final String forgotIdSuccessMsg = "Your user id is sent to your registered mobile number. If not received in next few minutes, please try again.";
    public static final String mobileChangeSuccessMsg = "New mobile number registered successfully.";
    public static final String merchantDisableSuccessMsg = "Merchant disabled successfully.";
    public static final String customerDisableSuccessMsg = "Customer disabled successfully.";

    public static final String custMobileChangeSuccessMsg = "Mobile Number changed successfully. Please login again with new mobile number.";
    public static final String enableAccSuccessMsg = "Account is Active now. Please login to proceed.";

    // Progress dialog messages
    public static final String progressDefault = "Processing ...";
    public static final String progressRegisterMerchant = "Registering merchant ...";
    public static final String progressLogin = "Logging in ...";
    public static final String progressRegCustomer = "Registering customer ...";
    public static final String progressSettings = "Saving settings ...";
    public static final String progressLogout = "Logging out ...";
    public static final String progressReports = "Generating report ...";
}
