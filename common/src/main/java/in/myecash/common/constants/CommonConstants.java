package in.myecash.common.constants;

import java.util.Date;
import java.util.Locale;

/**
 * This class contains constants relevant to both server code and app code.
 * This class should be exactly same everywhere
 */
public class CommonConstants {

    /*
     * Backend server settings
     */
    public static final String APPLICATION_ID = "927292A7-D4D3-7742-FFED-86CED1441100";
    public static final String ANDROID_SECRET_KEY = "14765681-0A7C-3F4E-FF29-41A948E33500";
    public static final String VERSION = "v1";

    public static final String BACKENDLESS_HOST = "http://52.66.52.215:8080/api";
    public static String BACKEND_FILE_BASE_URL = BACKENDLESS_HOST+"/"+APPLICATION_ID+"/"+VERSION+"/files/";
    public static final String BULK_API_URL  = BACKENDLESS_HOST+"/"+VERSION+"/data/bulk/";

    /*
     * Due to some issue in backendless - the errorCode is not correctly transmitted to app
     * from the Backend API - on raising an exception.
     * So, the errorCode is passed in 'errorMsg' field of the Backendless exception.
     * This prefix is added to the message to signal the same.
     */
    public static final String PREFIX_ERROR_CODE_AS_MSG = "ZZ/";

    /*
     * To use int as boolean
     */
    public static final int BOOLEAN_VALUE_FALSE = 0;
    public static final int BOOLEAN_VALUE_TRUE = 2;
    public static final int BOOLEAN_VALUE_INVALID = -1;

    /*
     * Date formats
     */
    public static final Locale DATE_LOCALE = Locale.ENGLISH;
    // used where only date (without time) is to be shown
    public static final String DATE_FORMAT_ONLY_DATE_DISPLAY = "dd MMM, yy";
    // used to specify 'date with no time' to the backend, like in where clause
    public static final String DATE_FORMAT_ONLY_DATE_BACKEND = "dd-MMM-yyyy";
    // used to specify 'date with no time' in the CSV report generated
    public static final String DATE_FORMAT_ONLY_DATE_CSV = "dd/MM/yyyy";
    // used in reports etc where both date and time is to be shown
    public static final String DATE_FORMAT_WITH_TIME = "dd/MM/yyyy HH:mm";
    // date format to be used in filename
    public static final String DATE_FORMAT_ONLY_DATE_FILENAME = "ddMMMyy";
    // format to show only time in 12 hr format
    public static final String DATE_FORMAT_ONLY_TIME_12 = "hh:mm a";
    // format to show only time in CSV file
    public static final String DATE_FORMAT_ONLY_TIME_24_CSV = "HH:mm:ss";

    public static final int MILLISECS_IN_HOUR = 3600000;
    public static final int MILLISECS_IN_MINUTE = 60000;
    /*
     * Size, Length and Limits
     */
    public static final int CUSTOMER_INTERNAL_ID_LEN = 6;
    public static final int INTERNAL_USER_ID_LEN = 7;
    public static final int MERCHANT_ID_LEN = 8;
    public static final int MOBILE_NUM_LENGTH = 10;
    public static final int CUSTOMER_CARDID_LEN = 11;
    // DOB in format 'DDMMYYYY'
    public static final int DOB_LEN = 8;
    public static final int TRANSACTION_ID_LEN = 13;
    public static final int PIN_LEN = 5;
    public static final int OTP_LEN = 5;

    public static final int MAX_DEVICES_PER_MERCHANT = 3;
    public static final int dbQueryMaxPageSize = 100;

    /*
     * Backend path constants
     */
    public static final String FILE_PATH_SEPERATOR = "/";
    public static final String MERCHANT_ROOT_DIR = "merchants"+ CommonConstants.FILE_PATH_SEPERATOR;
    public static final String MERCHANT_DISPLAY_IMAGES_DIR = MERCHANT_ROOT_DIR+"displayImages"+ CommonConstants.FILE_PATH_SEPERATOR;
    public static final String MERCHANT_LOGGING_ROOT_DIR = "logging"+ CommonConstants.FILE_PATH_SEPERATOR;
    public static final String MERCHANT_TXN_ROOT_DIR = MERCHANT_ROOT_DIR+"txnCsvFiles"+ CommonConstants.FILE_PATH_SEPERATOR;
    public static final String MERCHANT_CUST_DATA_ROOT_DIR = MERCHANT_ROOT_DIR+"customerData"+ CommonConstants.FILE_PATH_SEPERATOR;
    public static final String MERCHANT_TXN_IMAGE_ROOT_DIR = MERCHANT_ROOT_DIR+"txnImages"+ CommonConstants.FILE_PATH_SEPERATOR;

    public static final String MERCHANT_TXN_FILE_PREFIX = "txns_";
    public static final String MERCHANT_CUST_DATA_FILE_PREFIX = "customers_";
    public static final String PREFIX_TXN_IMG_FILE_NAME = "txnImg_";
    public static final String CASHBACK_DATA_FILE_PREFIX = "cashback_";

    public static final String CSV_DELIMETER = ",";
    public static final String CSV_FILE_EXT = ".csv";
    public static final String CSV_NEWLINE = "\n";

    /*
     * Other common constants
     */
    public static final String TRANSACTION_ID_PREFIX = "TX";
    // number to character mapping
    public static final String numToChar[] = {"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
}
