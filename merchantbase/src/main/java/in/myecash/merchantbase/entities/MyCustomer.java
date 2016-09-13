package in.myecash.merchantbase.entities;

import com.backendless.exceptions.BackendlessException;
import in.myecash.commonbase.constants.CommonConstants;
import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.utilities.LogMy;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by adgangwa on 02-06-2016.
 */
public class MyCustomer {
    private static final String TAG = "MyCustomer";
    private int mRegStatusCode;
    SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
    SimpleDateFormat mSdfOnlyDateDisplay = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_DISPLAY, CommonConstants.DATE_LOCALE);

    // Customer properties
    String mPrivateId;
    String mMobileNum;
    int mStatus;
    int mStatusReason;
    String mStatusUpdateTime;
    String mCardId;
    int mCardStatus;

    // optional properties
    String mName;
    Boolean mFirstLoginOk;
    String mCreateTime;
    String mRemarks;
    String mCardStatusUpdateTime;

    public int getRegStatusCode() {
        return mRegStatusCode;
    }

    public String getPrivateId() {
        return mPrivateId;
    }

    public String getMobileNum() {
        return mMobileNum;
    }

    public String getName() {
        return mName;
    }

    public Boolean isFirstLoginOk() {
        return mFirstLoginOk;
    }

    public String getCbCreateTime() {
        return mCreateTime;
    }

    public int getStatus() {
        return mStatus;
    }

    public int getStatusReason() {
        return mStatusReason;
    }

    public String getStatusUpdateTime() {
        return mStatusUpdateTime;
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

    public String getRemarks() {
        return mRemarks;
    }

    public void init(String customerDetailsInCsvFormat, String delim) {
        if(customerDetailsInCsvFormat==null || customerDetailsInCsvFormat.isEmpty())
        {
            LogMy.e(TAG,"Customer details not available.");
            throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "Cashback CSV record is null or empty");
        }
        // Customer detail in below CSV format
        // <private id>,<mobile_num>,<<name>>,<<first login ok>>,<<cust_create_time>>,
        // <acc_status>,<acc_status_reason>,<acc_status_update_time>,<<admin remarks>>
        // <card_id>,<card_status>,<card_status_update_time>
        // records with double bracket '<<>>' are only sent to 'customer care' users
        String[] csvFields = customerDetailsInCsvFormat.split(delim);
        mPrivateId = csvFields[CommonConstants.CUST_CSV_PRIVATE_ID];
        mMobileNum = csvFields[CommonConstants.CUST_CSV_MOBILE_NUM];
        mStatus = Integer.parseInt(csvFields[CommonConstants.CUST_CSV_ACC_STATUS]);
        mStatusReason = Integer.parseInt(csvFields[CommonConstants.CUST_CSV_STATUS_REASON]);
        mStatusUpdateTime = mSdfDateWithTime.format(new Date(Long.parseLong(csvFields[CommonConstants.CUST_CSV_STATUS_UPDATE_TIME])));
        mCardId = csvFields[CommonConstants.CUST_CSV_CARD_ID];
        mCardStatus = Integer.parseInt(csvFields[CommonConstants.CUST_CSV_CARD_STATUS]);

        // only customer care data - thus optional
        if(!csvFields[CommonConstants.CUST_CSV_NAME].isEmpty()) {
            mName = csvFields[CommonConstants.CUST_CSV_NAME];
        } else {
            mName = null;
        }
        if(!csvFields[CommonConstants.CUST_CSV_FIRST_LOGIN_OK].isEmpty()) {
            mFirstLoginOk = Boolean.parseBoolean(csvFields[CommonConstants.CUST_CSV_FIRST_LOGIN_OK]);
        } else {
            mFirstLoginOk = null;
        }
        if(!csvFields[CommonConstants.CUST_CSV_CUST_CREATE_TIME].isEmpty()) {
            mCreateTime = mSdfOnlyDateDisplay.format(new Date(Long.parseLong(csvFields[CommonConstants.CUST_CSV_CUST_CREATE_TIME])));
        } else {
            mCreateTime = null;
        }
        if(!csvFields[CommonConstants.CUST_CSV_ADMIN_REMARKS].isEmpty()) {
            mRemarks = csvFields[CommonConstants.CUST_CSV_ADMIN_REMARKS];
        } else {
            mRemarks = null;
        }
        if(!csvFields[CommonConstants.CUST_CSV_STATUS_UPDATE_TIME].isEmpty()) {
            mCardStatusUpdateTime = mSdfDateWithTime.format(new Date(Long.parseLong(csvFields[CommonConstants.CUST_CSV_STATUS_UPDATE_TIME])));
        } else {
            mCardStatusUpdateTime = null;
        }
    }

    /*
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(mMobileNum).append(",")
                .append(mStatus).append(",")
                .append(mStatusReason).append(",")
                .append(mStatusUpdateTime).append(",")
                .append(mCardId).append(",")
                .append(mCardStatus).append(",")
                .append(mCardStatusUpdateTime);
        return sb.toString();
    }*/
}
