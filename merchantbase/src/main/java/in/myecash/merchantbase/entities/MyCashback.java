package in.myecash.merchantbase.entities;

import com.backendless.exceptions.BackendlessException;
import in.myecash.commonbase.constants.CommonConstants;
import in.myecash.commonbase.constants.DbConstants;
import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.models.Cashback;
import in.myecash.commonbase.models.Transaction;
import in.myecash.commonbase.utilities.AppAlarms;
import in.myecash.commonbase.utilities.LogMy;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by adgangwa on 07-05-2016.
 */
public class MyCashback {
    private static final String TAG = "MyCashback";

    // Txn sort parameter types
    public static final int CB_CMP_TYPE_UPDATE_TIME = 0;
    public static final int CB_CMP_TYPE_BILL_AMT = 1;
    public static final int CB_CMP_TYPE_ACC_BALANCE = 2;
    public static final int CB_CMP_TYPE_ACC_ADD = 3;
    public static final int CB_CMP_TYPE_ACC_DEBIT = 4;
    public static final int CB_CMP_TYPE_CB_BALANCE = 5;
    public static final int CB_CMP_TYPE_CB_ADD = 6;
    public static final int CB_CMP_TYPE_CB_DEBIT = 7;

    private Cashback mOldCashback;
    private Cashback mCurrCashback;
    // customer associated with this cashback object
    private MyCustomer mCustomer;

    // Current cashback operations
    public void setCashback(Cashback currCashback) {
        mOldCashback = mCurrCashback;
        mCurrCashback = currCashback;
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
    public String getCustomerDetails() {
        return mCurrCashback.getCustomer_details();
    }*/

    // Old cashback operations
    public int getOldCbBalance() {
        return mOldCashback==null?0:(mOldCashback.getCb_credit() - mOldCashback.getCb_debit());
    }
    public int getOldClBalance() {
        return mOldCashback==null?0:(mOldCashback.getCl_credit() - mOldCashback.getCl_debit());
    }

    public MyCustomer getCustomer() {
        return mCustomer;
    }

    public void init(Cashback cb) {
        mCurrCashback = cb;
        // init customer
        mCustomer = new MyCustomer();
        mCustomer.init(mCurrCashback.getCustomer_details(), CommonConstants.CSV_SUB_DELIMETER);
    }

    // init with CSV record containing both 'cashback' and 'customer' data in single record
    public void init(String csvRecord) {
        if(csvRecord==null || csvRecord.isEmpty())
        {
            LogMy.e(TAG,"Cashback details not available.");
            throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "Cashback CSV record is null or empty");
        }
        LogMy.d(TAG,"In init: "+csvRecord);

        Cashback cb = new Cashback();
        String[] csvFields = csvRecord.split(CommonConstants.CSV_DELIMETER);
        cb.setCust_private_id(csvFields[CommonConstants.CB_CSV_CUST_PVT_ID]);
        cb.setMerchant_id(csvFields[CommonConstants.CB_CSV_MCHNT_ID]);
        cb.setCl_credit(Integer.parseInt(csvFields[CommonConstants.CB_CSV_ACC_CR]));
        cb.setCl_debit(Integer.parseInt(csvFields[CommonConstants.CB_CSV_ACC_DB]));
        cb.setCb_credit(Integer.parseInt(csvFields[CommonConstants.CB_CSV_CR]));
        cb.setCb_debit(Integer.parseInt(csvFields[CommonConstants.CB_CSV_DB]));
        cb.setTotal_billed(Integer.parseInt(csvFields[CommonConstants.CB_CSV_TOTAL_BILL]));
        cb.setCb_billed(Integer.parseInt(csvFields[CommonConstants.CB_CSV_BILL]));
        cb.setCreated(new Date(Long.parseLong(csvFields[CommonConstants.CB_CSV_CREATE_TIME])));
        cb.setUpdated(new Date(Long.parseLong(csvFields[CommonConstants.CB_CSV_UPDATE_TIME])));
        cb.setCustomer_details(csvFields[CommonConstants.CB_CSV_CUST_DETAILS]);

        init(cb);
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
    }
}
