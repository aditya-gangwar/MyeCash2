package in.myecash.commonbase.entities;

import com.backendless.exceptions.BackendlessException;

import java.util.Date;

import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.utilities.LogMy;

/**
 * Created by adgangwa on 25-09-2016.
 */
public class MyMerchant {
    private static final String TAG = "MyMerchant";

    private final int MCHNT_CSV_NAME = 0;
    private final int MCHNT_CSV_ID = 1;
    private final int MCHNT_CSV_MOBILE = 2;
    private final int MCHNT_CSV_CB_RATE = 3;
    private final int MCHNT_CSV_BUSS_CATEGORY = 4;
    private final int MCHNT_CSV_ADDR_LINE1 = 5;
    private final int MCHNT_CSV_ADDR_CITY = 6;
    private final int MCHNT_CSV_ADDR_STATE = 7;
    private final int MCHNT_CSV_STATUS = 8;
    private final int MCHNT_CSV_STATUS_TIME = 9;
    private final int MCHNT_CSV_FIELD_CNT = 10;

    // Total size of above fields = 50+50+10*7
    public final int MCHNT_CSV_MAX_SIZE = 200;

    private final String MCHNT_CSV_DELIM = ":";

    // Merchant properties
    String mName;
    String mId;
    String mMobileNum;
    String mCbRate;
    String mBusinessCategory;
    // address data
    String mAddressLine1;
    String mCity;
    String mState;
    // status data
    String mStatus;
    Date mStatusUpdateTime;

    // Init from CSV string
    public void init(String csvStr) {
        if(csvStr==null || csvStr.isEmpty())
        {
            LogMy.e(TAG,"Merchant details not available.");
            throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "Merchant CSV record is null or empty");
        }
        LogMy.d(TAG,"In init: "+csvStr);

        String[] csvFields = csvStr.split(MCHNT_CSV_DELIM);

        mName = csvFields[MCHNT_CSV_NAME];
        mId = csvFields[MCHNT_CSV_ID];
        mMobileNum = csvFields[MCHNT_CSV_MOBILE];
        mCbRate = csvFields[MCHNT_CSV_CB_RATE];
        mBusinessCategory = csvFields[MCHNT_CSV_BUSS_CATEGORY];
        mAddressLine1 = csvFields[MCHNT_CSV_ADDR_LINE1];
        mCity = csvFields[MCHNT_CSV_ADDR_CITY];
        mState = csvFields[MCHNT_CSV_ADDR_STATE];
        mStatus = csvFields[MCHNT_CSV_STATUS];
        mStatusUpdateTime = new Date(Long.parseLong(csvFields[MCHNT_CSV_STATUS_TIME]));
    }

    // Convert to CSV string
    public String toCsvString() {
        String[] csvFields = new String[MCHNT_CSV_FIELD_CNT];
        csvFields[MCHNT_CSV_NAME] = mName;
        csvFields[MCHNT_CSV_ID] = mId;
        csvFields[MCHNT_CSV_MOBILE] = mMobileNum;
        csvFields[MCHNT_CSV_CB_RATE] = mCbRate;
        csvFields[MCHNT_CSV_BUSS_CATEGORY] = mBusinessCategory;
        csvFields[MCHNT_CSV_ADDR_LINE1] = mAddressLine1;
        csvFields[MCHNT_CSV_ADDR_CITY] = mCity;
        csvFields[MCHNT_CSV_ADDR_STATE] = mState;
        csvFields[MCHNT_CSV_STATUS] = mStatus;
        csvFields[MCHNT_CSV_STATUS_TIME] = String.valueOf(mStatusUpdateTime.getTime());

        // join the fields in single CSV string
        StringBuilder sb = new StringBuilder(MCHNT_CSV_MAX_SIZE);
        for(int i=0; i<MCHNT_CSV_FIELD_CNT; i++) {
            sb.append(csvFields[i]).append(MCHNT_CSV_DELIM);
        }

        return sb.toString();
    }

    /*
     * Getter methods
     */
    public String getName() {
        return mName;
    }

    public String getId() {
        return mId;
    }

    public String getMobileNum() {
        return mMobileNum;
    }

    public String getCbRate() {
        return mCbRate;
    }

    public String getBusinessCategory() {
        return mBusinessCategory;
    }

    public String getAddressLine1() {
        return mAddressLine1;
    }

    public String getCity() {
        return mCity;
    }

    public String getState() {
        return mState;
    }

    public String getStatus() {
        return mStatus;
    }

    public Date getStatusUpdateTime() {
        return mStatusUpdateTime;
    }
}

