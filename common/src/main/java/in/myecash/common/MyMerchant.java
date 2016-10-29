package in.myecash.common;

import com.backendless.exceptions.BackendlessException;

import java.util.Date;

import in.myecash.common.database.Merchants;

/**
 * Created by adgangwa on 25-09-2016.
 */
public class MyMerchant {
    private static final String TAG = "MyMerchant";

    private static final int MCHNT_CSV_NAME = 0;
    private static final int MCHNT_CSV_ID = 1;
    private static final int MCHNT_CSV_MOBILE = 2;
    private static final int MCHNT_CSV_CB_RATE = 3;
    private static final int MCHNT_CSV_BUSS_CATEGORY = 4;
    private static final int MCHNT_CSV_ADDR_LINE1 = 5;
    private static final int MCHNT_CSV_ADDR_CITY = 6;
    private static final int MCHNT_CSV_ADDR_STATE = 7;
    private static final int MCHNT_CSV_STATUS = 8;
    private static final int MCHNT_CSV_STATUS_TIME = 9;
    private static final int MCHNT_CSV_REMOVE_REQ_DATE = 10;
    private static final int MCHNT_CSV_DP_FILENAME = 11;
    private static final int MCHNT_CSV_FIELD_CNT = 12;

    // Total size of above fields = 50+50+10*7
    private static final int MCHNT_CSV_MAX_SIZE = 200;
    private static final String MCHNT_CSV_DELIM = ":";

    // Merchant properties
    private String mName;
    private String mId;
    private String mMobileNum;
    private String mCbRate;
    private String mBusinessCategory;
    // address data
    private String mAddressLine1;
    private String mCity;
    private String mState;
    // status data
    private int mStatus;
    private Date mStatusUpdateTime;
    private Date mRemoveReqDate;
    private String mDpFilename;

    // Init from CSV string
    public void init(String csvStr) {
        String[] csvFields = csvStr.split(MCHNT_CSV_DELIM, -1);

        mName = csvFields[MCHNT_CSV_NAME];
        mId = csvFields[MCHNT_CSV_ID];
        mMobileNum = csvFields[MCHNT_CSV_MOBILE];
        mCbRate = csvFields[MCHNT_CSV_CB_RATE];
        mBusinessCategory = csvFields[MCHNT_CSV_BUSS_CATEGORY];
        mAddressLine1 = csvFields[MCHNT_CSV_ADDR_LINE1];
        mCity = csvFields[MCHNT_CSV_ADDR_CITY];
        mState = csvFields[MCHNT_CSV_ADDR_STATE];
        mStatus = Integer.parseInt(csvFields[MCHNT_CSV_STATUS]);
        mStatusUpdateTime = new Date(Long.parseLong(csvFields[MCHNT_CSV_STATUS_TIME]));
        if(!csvFields[MCHNT_CSV_REMOVE_REQ_DATE].isEmpty()) {
            mRemoveReqDate = new Date(Long.parseLong(csvFields[MCHNT_CSV_REMOVE_REQ_DATE]));
        }
        mDpFilename = csvFields[MCHNT_CSV_DP_FILENAME];
    }

    // Convert to CSV string
    public static String toCsvString(Merchants merchant) {
        String[] csvFields = new String[MCHNT_CSV_FIELD_CNT];
        csvFields[MCHNT_CSV_NAME] = merchant.getName();
        csvFields[MCHNT_CSV_ID] = merchant.getAuto_id();
        csvFields[MCHNT_CSV_MOBILE] = merchant.getMobile_num();
        csvFields[MCHNT_CSV_CB_RATE] = merchant.getCb_rate();
        csvFields[MCHNT_CSV_BUSS_CATEGORY] = merchant.getBuss_category();
        csvFields[MCHNT_CSV_ADDR_LINE1] = merchant.getAddress().getLine_1();
        csvFields[MCHNT_CSV_ADDR_CITY] = merchant.getAddress().getCity();
        csvFields[MCHNT_CSV_ADDR_STATE] = merchant.getAddress().getState();
        csvFields[MCHNT_CSV_STATUS] = String.valueOf(merchant.getAdmin_status());
        csvFields[MCHNT_CSV_STATUS_TIME] = Long.toString(merchant.getStatus_update_time().getTime());
        if(merchant.getRemoveReqDate()==null) {
            csvFields[MCHNT_CSV_REMOVE_REQ_DATE] = "";
        } else {
            csvFields[MCHNT_CSV_REMOVE_REQ_DATE] = Long.toString(merchant.getRemoveReqDate().getTime());
        }
        csvFields[MCHNT_CSV_DP_FILENAME] = merchant.getDisplayImage();

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

    public int getStatus() {
        return mStatus;
    }

    public Date getStatusUpdateTime() {
        return mStatusUpdateTime;
    }

    public Date getRemoveReqDate() {
        return mRemoveReqDate;
    }

    public String getDpFilename() {
        return mDpFilename;
    }
}

