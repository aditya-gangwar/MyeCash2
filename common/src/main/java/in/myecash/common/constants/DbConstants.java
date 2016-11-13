package in.myecash.common.constants;

/**
 * Created by adgangwa on 07-05-2016.
 */
public class DbConstants {

    // Users table - 'user_type' column values
    public static final int USER_TYPE_MERCHANT = 0;
    public static final int USER_TYPE_CUSTOMER = 1;
    public static final int USER_TYPE_AGENT = 2;
    public static final int USER_TYPE_CC = 3;
    public static final int USER_TYPE_CNT = 4;
    public static final int USER_TYPE_ADMIN = 5;
    // user type code to text description
    public static String userTypeDesc[] = {
            "Merchant",
            "Customer",
            "Agent",
            "CustomerCare",
            "Controller",
            "Admin"
    };

    // 'admin_status' column values - user tables
    public static final int USER_STATUS_ACTIVE = 1;
    // Disabled means permanent - until further action by the admin to explicitly enable the account
    public static final int USER_STATUS_DISABLED = 2;
    // Locked means temporary - and will be automatically unlocked after defined duration
    public static final int USER_STATUS_LOCKED = 3;
    // Error during registration - to be manually deleted
    public static final int USER_STATUS_REG_ERROR = 4;
    public static final int USER_STATUS_READY_TO_ACTIVE = 5;
    public static final int USER_STATUS_UNDER_CLOSURE = 6;
    // mainly for customer users: indicate mobile number is changed recently
    // and access to account will be restricted. Only 'Credit' txns will be allowed
    public static final int USER_STATUS_MOB_CHANGE_RECENT = 7;
    // status code to text description
    public static String userStatusDesc[] = {
            "",
            "Active",
            "Disabled",
            "Locked",
            "Not Registered",
            "Ready to Enable",
            "Under Closure Notice",
            "Limited: Credit Only"
    };

    // CustomerCards table - 'status' column values
    public static final int CUSTOMER_CARD_STATUS_NEW = 0;
    public static final int CUSTOMER_CARD_STATUS_WITH_MERCHANT = 1;
    public static final int CUSTOMER_CARD_STATUS_ALLOTTED = 2;
    //public static final int CUSTOMER_CARD_STATUS_BLOCKED = 3;
    public static final int CUSTOMER_CARD_STATUS_REMOVED = 3;
    // Map int status values to corresponding descriptions
    public static String cardStatusDescriptions[] = {
            "Invalid",
            "Invalid",
            "Active",
            "Removed"
    };

    // 'opcode' values used in different tables
    public static final String OP_REG_CUSTOMER = "Register Customer";

    public static final String OP_LOGIN = "Login";
    public static final String OP_TXN_COMMIT = "Transaction Submit";
    public static final String OP_CHANGE_MOBILE = "Mobile Change";

    public static final String OP_FORGOT_USERID = "Forgot User ID";
    public static final String OP_RESET_PASSWD = "Password Reset";
    public static final String OP_CHANGE_PASSWD = "Password Change";

    public static final String OP_RESET_ACC_FOR_LOGIN = "Reset Account for Login";
    public static final String OP_DISABLE_ACC = "Account Disable";
    public static final String OP_REMOVE_ACC = "Remove Account";

    public static final String OP_NEW_CARD = "New Membership Card";
    public static final String OP_RESET_PIN = "Reset PIN";
    public static final String OP_CHANGE_PIN = "Change PIN";

    // Transactions table values
    public static final String TXN_CUSTOMER_PIN_USED = "Yes";
    public static final String TXN_CUSTOMER_PIN_NOT_USED = "No";


}
