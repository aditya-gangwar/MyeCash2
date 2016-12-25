package in.myecash.common;

import com.backendless.exceptions.BackendlessException;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.database.CustomerCards;
import in.myecash.common.database.Customers;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by adgangwa on 02-06-2016.
 */
public class MyCustomer {
    private static final String TAG = "MyCustomer";

    SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
    //SimpleDateFormat mSdfOnlyDateDisplay = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_DISPLAY, CommonConstants.DATE_LOCALE);

    /*
     * Index of various parameters in CustomerDetails CSV records (rcvd as part of cashback object)
     * Format:
     * <private id>,<mobile_num>,<<name>>,<<first login ok>>,<<cust create time>>
     * <acc_status>,<acc_status_reason>,<acc_status_update_time>,<<admin remarks>>
     *     -- membership card data follows --
     * <card_id>,<card_status>,<card_status_update_time>
     * Records with double bracket '<<>>' are only sent to 'customer care' users
     */
    public static int CUST_CSV_PRIVATE_ID = 0;
    public static int CUST_CSV_MOBILE_NUM = 1;
    //public static int CUST_CSV_NAME = 2;
    //public static int CUST_CSV_FIRST_LOGIN_OK = 3;
    //public static int CUST_CSV_CUST_CREATE_TIME = 4;
    public static int CUST_CSV_ACC_STATUS = 2;
    public static int CUST_CSV_STATUS_REASON = 3;
    public static int CUST_CSV_STATUS_UPDATE_TIME = 4;
    //public static int CUST_CSV_ADMIN_REMARKS = 8;
    public static int CUST_CSV_CARD_ID = 5;
    public static int CUST_CSV_CARD_STATUS = 6;
    public static int CUST_CSV_CARD_STATUS_UPDATE_TIME = 7;
    public static int CUST_CSV_TOTAL_FIELDS = 8;

    // Total size of above fields = 50+10*9
    public static final int CUST_CSV_MAX_SIZE = 200;
    private static final String CUST_CSV_DELIM = ":";

    // Customer properties
    private String mPrivateId;
    private String mMobileNum;
    private int mStatus;
    private String mStatusUpdateTime;
    private Date mStatusUpdateDate;
    private String mCardId;
    private int mCardStatus;

    // optional properties
    //String mName;
    //private Boolean mFirstLoginOk;
    //private String mCreateTime;
    private String mStatusReason;
    //String mRemarks;
    private String mCardStatusUpdateTime;

    // Init from CSV string
    public void init(String customerDetailsInCsvFormat) {
        if(customerDetailsInCsvFormat== null || customerDetailsInCsvFormat.isEmpty()) {
            return;
        }

        String[] csvFields = customerDetailsInCsvFormat.split(CUST_CSV_DELIM, -1);
        mPrivateId = csvFields[CUST_CSV_PRIVATE_ID];
        mMobileNum = csvFields[CUST_CSV_MOBILE_NUM];
        mStatus = Integer.parseInt(csvFields[CUST_CSV_ACC_STATUS]);
        mStatusUpdateDate = new Date(Long.parseLong(csvFields[CUST_CSV_STATUS_UPDATE_TIME]));
        mStatusUpdateTime = mSdfDateWithTime.format(mStatusUpdateDate);
        mCardId = csvFields[CUST_CSV_CARD_ID];
        mCardStatus = Integer.parseInt(csvFields[CUST_CSV_CARD_STATUS]);

        // only customer care data - thus optional
        /*if(!csvFields[CUST_CSV_NAME].isEmpty()) {
            mName = csvFields[CUST_CSV_NAME];
        } else {
            mName = null;
        }
        if(!csvFields[CUST_CSV_FIRST_LOGIN_OK].isEmpty()) {
            mFirstLoginOk = Boolean.parseBoolean(csvFields[CUST_CSV_FIRST_LOGIN_OK]);
        } else {
            mFirstLoginOk = null;
        }
        if(!csvFields[CUST_CSV_CUST_CREATE_TIME].isEmpty()) {
            mCreateTime = mSdfOnlyDateDisplay.format(new Date(Long.parseLong(csvFields[CUST_CSV_CUST_CREATE_TIME])));
        } else {
            mCreateTime = null;
        }*/
        if(!csvFields[CUST_CSV_STATUS_REASON].isEmpty()) {
            mStatusReason = csvFields[CUST_CSV_STATUS_REASON];
        } else {
            mStatusReason = null;
        }
        if(!csvFields[CUST_CSV_STATUS_UPDATE_TIME].isEmpty()) {
            mCardStatusUpdateTime = mSdfDateWithTime.format(new Date(Long.parseLong(csvFields[CUST_CSV_STATUS_UPDATE_TIME])));
        } else {
            mCardStatusUpdateTime = null;
        }
    }

    // Convert to CSV string
    public static String toCsvString(Customers customer) {

        CustomerCards card = customer.getMembership_card();
        String[] csvFields = new String[CUST_CSV_TOTAL_FIELDS];

        csvFields[CUST_CSV_PRIVATE_ID] = customer.getPrivate_id() ;
        csvFields[CUST_CSV_MOBILE_NUM] = CommonUtils.getPartialVisibleStr(customer.getMobile_num());
        csvFields[CUST_CSV_ACC_STATUS] = String.valueOf(customer.getAdmin_status()) ;
        csvFields[CUST_CSV_STATUS_REASON] = customer.getStatus_reason();
        csvFields[CUST_CSV_STATUS_UPDATE_TIME] = String.valueOf(customer.getStatus_update_time().getTime()) ;
        csvFields[CUST_CSV_CARD_ID] = CommonUtils.getPartialVisibleStr(card.getCardNum());
        csvFields[CUST_CSV_CARD_STATUS] = String.valueOf(card.getStatus()) ;
        csvFields[CUST_CSV_CARD_STATUS_UPDATE_TIME] = String.valueOf(card.getStatus_update_time().getTime()) ;

        /*if(addCustCareData) {
            //csvFields[CUST_CSV_NAME] = customer.getName() ;
            csvFields[CUST_CSV_FIRST_LOGIN_OK] = String.valueOf(customer.getFirst_login_ok()) ;
            csvFields[CUST_CSV_CUST_CREATE_TIME] = String.valueOf(customer.getCreated().getTime()) ;
            //csvFields[CUST_CSV_ADMIN_REMARKS] = customer.getAdmin_remarks() ;
        } else {
            //csvFields[CUST_CSV_NAME] = "";
            csvFields[CUST_CSV_FIRST_LOGIN_OK] = "";
            csvFields[CUST_CSV_CUST_CREATE_TIME] = "";
            //csvFields[CUST_CSV_ADMIN_REMARKS] = "";
        }*/

        // join the fields in single CSV string
        StringBuilder sb = new StringBuilder(CUST_CSV_MAX_SIZE);
        for(int i=0; i<CUST_CSV_TOTAL_FIELDS; i++) {
            sb.append(csvFields[i]).append(CUST_CSV_DELIM);
        }

        return sb.toString();
    }

    /*
     * Getter methods
     */
    public String getPrivateId() {
        return mPrivateId;
    }

    public String getMobileNum() {
        return mMobileNum;
    }

    /*public String getName() {
        return mName;
    }

    public Boolean isFirstLoginOk() {
        return mFirstLoginOk;
    }

    public String getCustCreateTime() {
        return mCreateTime;
    }*/

    public int getStatus() {
        return mStatus;
    }

    public String getStatusReason() {
        return mStatusReason;
    }

    public String getStatusUpdateTime() {
        return mStatusUpdateTime;
    }

    public Date getStatusUpdateDate() {
        return mStatusUpdateDate;
    }

    public String getCardId() {
        return mCardId;
    }

    public int getCardStatus() {
        return mCardStatus;
    }

    public String getCardStatusUpdateTime() {
        return mCardStatusUpdateTime;
    }

    /*public String getRemarks() {
        return mRemarks;
    }*/

}
