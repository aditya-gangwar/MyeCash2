package in.myecash.appbase.entities;

import com.backendless.exceptions.BackendlessException;

import in.myecash.common.constants.DbConstants;
import in.myecash.appbase.constants.ErrorCodes;
import in.myecash.common.database.Cashback;
import in.myecash.appbase.utilities.AppAlarms;
import in.myecash.appbase.utilities.LogMy;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by adgangwa on 07-05-2016.
 */
public class MyCashback {
    private static final String TAG = "MyCashback";

    // Cashback sort parameter types
    public static final int CB_CMP_TYPE_UPDATE_TIME = 0;
    public static final int CB_CMP_TYPE_BILL_AMT = 1;
    public static final int CB_CMP_TYPE_ACC_BALANCE = 2;
    public static final int CB_CMP_TYPE_ACC_ADD = 3;
    public static final int CB_CMP_TYPE_ACC_DEBIT = 4;
    public static final int CB_CMP_TYPE_CB_BALANCE = 5;
    public static final int CB_CMP_TYPE_CB_ADD = 6;
    public static final int CB_CMP_TYPE_CB_DEBIT = 7;
    // Cashback sort by merchant attributes
    public static final int CB_CMP_TYPE_MCHNT_NAME = 8;
    public static final int CB_CMP_TYPE_MCHNT_CITY = 9;

    /*
     * Index of various parameters in Cashback CSV records (stored in CustData CSV files)
     * Format:
     * <Total Account Credit>,<Total Account Debit>,
     * <Total Cashback Credit>,<Total Cashback Debit>,
     * <Total Billed>,<Total Cashback Billed>,
     * <create time>,<update time>
     * Records with double bracket '<<>>' are only sent to 'customer care' users
     */
    public static int CB_CSV_CUST_PVT_ID = 0;
    public static int CB_CSV_MCHNT_ID = 1;
    public static int CB_CSV_ACC_CR = 2;
    public static int CB_CSV_ACC_DB = 3;
    public static int CB_CSV_CR = 4;
    public static int CB_CSV_DB = 5;
    public static int CB_CSV_TOTAL_BILL = 6;
    public static int CB_CSV_BILL = 7;
    public static int CB_CSV_CREATE_TIME = 8;
    public static int CB_CSV_UPDATE_TIME = 9;
    public static int CB_CSV_OTHER_DETAILS = 10;
    public static int CB_CSV_TOTAL_FIELDS = 11;

    // Total size of above fields = 10*10
    public static final int CB_CSV_MAX_SIZE = 128;
    private static final String CB_CSV_DELIM = ",";

    private Cashback mOldCashback;
    private Cashback mCurrCashback;

    // customer/merchant associated with this cashback object
    // they are provided by the backend in the other_details field as CSV string
    private MyCustomer mCustomer;
    private MyMerchant mMerchant;

    /*
     * Init object values from given CSV string
     * containing both 'cashback' and 'customer/merchant' data in single record
     */
    public void init(String csvRecord, boolean callingUserIsMchnt) {
        if(csvRecord==null || csvRecord.isEmpty())
        {
            LogMy.e(TAG,"Cashback details not available.");
            throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "Cashback CSV record is null or empty");
        }
        LogMy.d(TAG,"In init: "+csvRecord);

        Cashback cb = new Cashback();
        String[] csvFields = csvRecord.split(CB_CSV_DELIM);

        cb.setCust_private_id(csvFields[CB_CSV_CUST_PVT_ID]);
        cb.setMerchant_id(csvFields[CB_CSV_MCHNT_ID]);
        cb.setCl_credit(Integer.parseInt(csvFields[CB_CSV_ACC_CR]));
        cb.setCl_debit(Integer.parseInt(csvFields[CB_CSV_ACC_DB]));
        cb.setCb_credit(Integer.parseInt(csvFields[CB_CSV_CR]));
        cb.setCb_debit(Integer.parseInt(csvFields[CB_CSV_DB]));
        cb.setTotal_billed(Integer.parseInt(csvFields[CB_CSV_TOTAL_BILL]));
        cb.setCb_billed(Integer.parseInt(csvFields[CB_CSV_BILL]));
        cb.setCreated(new Date(Long.parseLong(csvFields[CB_CSV_CREATE_TIME])));
        cb.setUpdated(new Date(Long.parseLong(csvFields[CB_CSV_UPDATE_TIME])));

        init(cb, callingUserIsMchnt);
    }

    public void init(Cashback cb, boolean callingUserIsMchnt) {
        mCurrCashback = cb;

        if(callingUserIsMchnt) {
            mCustomer = new MyCustomer();
            mCustomer.init(mCurrCashback.getOther_details());
        } else {
            mMerchant = new MyMerchant();
            mMerchant.init(mCurrCashback.getOther_details());
        }
    }

    public static String toCsvString(Cashback cb) {

        String[] csvFields = new String[CB_CSV_TOTAL_FIELDS];
        csvFields[CB_CSV_CUST_PVT_ID] = String.valueOf(cb.getCust_private_id()) ;
        csvFields[CB_CSV_MCHNT_ID] = String.valueOf(cb.getMerchant_id()) ;
        csvFields[CB_CSV_ACC_CR] = String.valueOf(cb.getCl_credit()) ;
        csvFields[CB_CSV_ACC_DB] = String.valueOf(cb.getCl_debit()) ;
        csvFields[CB_CSV_CR] = String.valueOf(cb.getCb_credit()) ;
        csvFields[CB_CSV_DB] = String.valueOf(cb.getCb_debit()) ;
        csvFields[CB_CSV_TOTAL_BILL] = String.valueOf(cb.getTotal_billed()) ;
        csvFields[CB_CSV_BILL] = String.valueOf(cb.getCb_billed()) ;
        csvFields[CB_CSV_CREATE_TIME] = String.valueOf(cb.getCreated().getTime()) ;
        csvFields[CB_CSV_UPDATE_TIME] = String.valueOf(cb.getUpdated().getTime()) ;
        csvFields[CB_CSV_OTHER_DETAILS] = cb.getOther_details() ;

        // join the fields in single CSV string
        StringBuilder sb = new StringBuilder(CB_CSV_MAX_SIZE + cb.getOther_details().length());
        for(int i=0; i<CB_CSV_TOTAL_FIELDS; i++) {
            sb.append(csvFields[i]).append(CB_CSV_DELIM);
        }
        return sb.toString();
    }

    // Current cashback operations
    public void setCashback(Cashback currCashback) {
        mOldCashback = mCurrCashback;
        mCurrCashback = currCashback;
    }

    /*
     * Getter methods
     */
    public MyCustomer getCustomer() {
        return mCustomer;
    }

    public MyMerchant getMerchant() {
        return mMerchant;
    }

    public String getMerchantId() {
        return mCurrCashback.getMerchant_id();
    }

    /*
     * Current cashback Getter methods
     */
    public Cashback getCurrCashback() {
        return mCurrCashback;
    }

    public int getCurrCbBalance() {
        int balance = mCurrCashback==null?-1:(mCurrCashback.getCb_credit() - mCurrCashback.getCb_debit());
        if(balance<0) {
            // raise fatal alarm
            Map<String,String> params = new HashMap<>();
            params.put("CustomerId",mCurrCashback.getCust_private_id());
            params.put("MerchantId",mCurrCashback.getMerchant_id());
            params.put("Balance",Integer.toString(balance));
            AppAlarms.invalidCbData(mCurrCashback.getMerchant_id(), DbConstants.USER_TYPE_MERCHANT,"getCurrCbBalance",params);
        }
        return balance;
    }
    public int getCurrClBalance() {
        int balance = mCurrCashback==null?-1:(mCurrCashback.getCl_credit() - mCurrCashback.getCl_debit());
        if(balance<0) {
            // raise fatal alarm
            Map<String,String> params = new HashMap<>();
            params.put("CustomerId",mCurrCashback.getCust_private_id());
            params.put("MerchantId",mCurrCashback.getMerchant_id());
            params.put("Balance",Integer.toString(balance));
            AppAlarms.invalidCbData(mCurrCashback.getMerchant_id(), DbConstants.USER_TYPE_MERCHANT,"getCurrClBalance",params);
        }
        return balance;
    }

    public int getCbCredit() {
        return mCurrCashback==null?-1:mCurrCashback.getCb_credit();
    }
    public int getCbRedeem() {
        return mCurrCashback==null?-1:mCurrCashback.getCb_debit();
    }
    public int getClCredit() {
        return mCurrCashback==null?-1:mCurrCashback.getCl_credit();
    }
    public int getClDebit() {
        return mCurrCashback==null?-1:mCurrCashback.getCl_debit();
    }
    public int getBillAmt() {
        return mCurrCashback==null?-1:mCurrCashback.getTotal_billed();
    }
    public int getCbBillAmt() {
        return mCurrCashback==null?-1:mCurrCashback.getCb_billed();
    }
    public Date getUpdateTime() {
        // updateTime will be null if no txn done after registration - use createTime in that case
        return mCurrCashback==null ?
                null :
                (mCurrCashback.getUpdated()==null ? getCreateTime():mCurrCashback.getUpdated());
    }
    public Date getCreateTime() {
        return mCurrCashback==null?null:mCurrCashback.getCreated();
    }

    /*
     * Old cashback Getter methods
     */
    public int getOldCbBalance() {
        return mOldCashback==null?0:(mOldCashback.getCb_credit() - mOldCashback.getCb_debit());
    }
    public int getOldClBalance() {
        return mOldCashback==null?0:(mOldCashback.getCl_credit() - mOldCashback.getCl_debit());
    }

    /*
     * comparator functions for sorting
     */
    public static class MyCashbackComparator implements Comparator<MyCashback> {

        int mCompareType;
        public MyCashbackComparator(int compareType) {
            mCompareType = compareType;
        }

        @Override
        public int compare(MyCashback lhs, MyCashback rhs) {
            // TODO: Handle null x or y values
            switch (mCompareType) {
                case CB_CMP_TYPE_UPDATE_TIME:
                    return compare(lhs.getUpdateTime().getTime(), rhs.getUpdateTime().getTime());
                case CB_CMP_TYPE_BILL_AMT:
                    return compare(lhs.getBillAmt(), rhs.getBillAmt());
                case CB_CMP_TYPE_ACC_BALANCE:
                    return compare(lhs.getCurrClBalance(), rhs.getCurrClBalance());
                case CB_CMP_TYPE_ACC_ADD:
                    return compare(lhs.getClCredit(), rhs.getClCredit());
                case CB_CMP_TYPE_ACC_DEBIT:
                    return compare(lhs.getClDebit(), rhs.getClDebit());
                case CB_CMP_TYPE_CB_BALANCE:
                    return compare(lhs.getCurrCbBalance(), rhs.getCurrCbBalance());
                case CB_CMP_TYPE_CB_ADD:
                    return compare(lhs.getCbCredit(), rhs.getCbCredit());
                case CB_CMP_TYPE_CB_DEBIT:
                    return compare(lhs.getCbRedeem(), rhs.getCbRedeem());
                case CB_CMP_TYPE_MCHNT_NAME:
                    return compare(lhs.getMerchant().getName(), rhs.getMerchant().getName());
                case CB_CMP_TYPE_MCHNT_CITY:
                    return compare(lhs.getMerchant().getCity(), rhs.getMerchant().getCity());
            }
            return 0;
        }
        private static int compare(long a, long b) {
            return a < b ? -1
                    : a > b ? 1
                    : 0;
        }
        private static int compare(int a, int b) {
            return a < b ? -1
                    : a > b ? 1
                    : 0;
        }
        private static int compare(String a, String b) {
            int res = String.CASE_INSENSITIVE_ORDER.compare(a, b);
            return (res != 0) ? res : a.compareTo(b);
        }
    }
}
