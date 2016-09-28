package in.myecash.merchantbase.entities;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.exceptions.BackendlessException;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.QueryOptions;
import in.myecash.common.constants.CommonConstants;
import in.myecash.appbase.constants.ErrorCodes;
import in.myecash.common.database.Transaction;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;

/**
 * Created by adgangwa on 07-05-2016.
 */
public class MyTransaction {
    private static final String TAG = "MyTransaction";
    private static SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

    private Transaction mCurrTransaction;

    public MyTransaction(Transaction txn){
        mCurrTransaction = txn;
    }

    public static Transaction getTxnFromCsv(String[] csvFields) throws ParseException {
        Transaction txn = new Transaction();
        txn.setTrans_id(csvFields[CommonConstants.TXN_CSV_IDX_ID]);
        txn.setCreate_time(mSdfDateWithTime.parse(csvFields[CommonConstants.TXN_CSV_IDX_TIME]));
        txn.setMerchant_id(csvFields[CommonConstants.TXN_CSV_IDX_MERCHANT_ID]);
        txn.setMerchant_name(csvFields[CommonConstants.TXN_CSV_IDX_MERCHANT_NAME]);
        txn.setCustomer_id(csvFields[CommonConstants.TXN_CSV_IDX_CUSTOMER_ID]);
        txn.setCust_private_id(csvFields[CommonConstants.TXN_CSV_IDX_CUSTOMER_PVT_ID]);
        txn.setUsedCardId(csvFields[CommonConstants.TXN_CSV_IDX_USED_CARD_ID]);
        txn.setTotal_billed(Integer.parseInt(csvFields[CommonConstants.TXN_CSV_IDX_TOTAL_BILLED]));
        txn.setCb_billed(Integer.parseInt(csvFields[CommonConstants.TXN_CSV_IDX_CB_BILLED]));
        txn.setCl_debit(Integer.parseInt(csvFields[CommonConstants.TXN_CSV_IDX_ACC_DEBIT]));
        txn.setCl_credit(Integer.parseInt(csvFields[CommonConstants.TXN_CSV_IDX_ACC_CREDIT]));
        txn.setCb_debit(Integer.parseInt(csvFields[CommonConstants.TXN_CSV_IDX_CB_REDEEM]));
        txn.setCb_credit(Integer.parseInt(csvFields[CommonConstants.TXN_CSV_IDX_CB_AWARD]));
        txn.setCb_percent(csvFields[CommonConstants.TXN_CSV_IDX_CB_RATE]);
        txn.setCpin(csvFields[CommonConstants.TXN_CSV_IDX_CUST_PIN]);
        txn.setImgFileName(csvFields[CommonConstants.TXN_CSV_IDX_IMG_FILE]);
        return txn;
    }

    /*
     * comparator functions for sorting
     */
    public static class TxnDateComparator implements Comparator<Transaction> {
        @Override
        public int compare(Transaction lhs, Transaction rhs) {
            // TODO: Handle null x or y values
            return compare(lhs.getCreate_time().getTime(), rhs.getCreate_time().getTime());
        }
        private static int compare(long a, long b) {
            return a < b ? -1
                    : a > b ? 1
                    : 0;
        }
    }
    public static class TxnBillComparator implements Comparator<Transaction> {
        @Override
        public int compare(Transaction lhs, Transaction rhs) {
            // TODO: Handle null x or y values
            return compare(lhs.getTotal_billed(), rhs.getTotal_billed());
        }
        private static int compare(int a, int b) {
            return a < b ? -1
                    : a > b ? 1
                    : 0;
        }
    }
    public static class TxnCbAwardComparator implements Comparator<Transaction> {
        @Override
        public int compare(Transaction lhs, Transaction rhs) {
            // TODO: Handle null x or y values
            return compare(lhs.getCb_credit(), rhs.getCb_credit());
        }
        private static int compare(int a, int b) {
            return a < b ? -1
                    : a > b ? 1
                    : 0;
        }
    }
    public static class TxnCbRedeemComparator implements Comparator<Transaction> {
        @Override
        public int compare(Transaction lhs, Transaction rhs) {
            // TODO: Handle null x or y values
            return compare(lhs.getCb_debit(), rhs.getCb_debit());
        }
        private static int compare(int a, int b) {
            return a < b ? -1
                    : a > b ? 1
                    : 0;
        }
    }
    public static class TxnAccAddComparator implements Comparator<Transaction> {
        @Override
        public int compare(Transaction lhs, Transaction rhs) {
            // TODO: Handle null x or y values
            return compare(lhs.getCl_credit(), rhs.getCl_credit());
        }
        private static int compare(int a, int b) {
            return a < b ? -1
                    : a > b ? 1
                    : 0;
        }
    }
    public static class TxnAccDebitComparator implements Comparator<Transaction> {
        @Override
        public int compare(Transaction lhs, Transaction rhs) {
            // TODO: Handle null x or y values
            return compare(lhs.getCl_debit(), rhs.getCl_debit());
        }
        private static int compare(int a, int b) {
            return a < b ? -1
                    : a > b ? 1
                    : 0;
        }
    }


    public Transaction getTransaction() {
        return mCurrTransaction;
    }

    public int commit(String pin) {
        LogMy.d(TAG, "In commit");
        int errorCode = ErrorCodes.NO_ERROR;

        mCurrTransaction.setCpin(pin);
        try
        {
            mCurrTransaction = Backendless.Persistence.save( mCurrTransaction );
            //myCashback.setCashback(mCurrTransaction.getCashback());
        }
        catch( BackendlessException e )
        {
            LogMy.e(TAG, "Commit cash transaction failed: " + e.toString());
            return AppCommonUtil.getLocalErrorCode(e);
        }
        return errorCode;
    }

    public static List<Transaction> fetch(String whereClause) {
        LogMy.d(TAG, "In fetchTransactionsSync: "+whereClause);
        // init values
        List<Transaction> transactions = null;

        // fetch cashback object from DB
        try {
            BackendlessDataQuery dataQuery = new BackendlessDataQuery();
            QueryOptions queryOptions = new QueryOptions("created");
            dataQuery.setQueryOptions(queryOptions);
            dataQuery.setPageSize(CommonConstants.dbQueryMaxPageSize);
            dataQuery.setWhereClause(whereClause);

            LogMy.d(TAG, "Before remote call");
            BackendlessCollection<Transaction> collection = Backendless.Data.of(Transaction.class).find(dataQuery);

            int size = collection.getTotalObjects();
            LogMy.d(TAG, "Got transactions from DB: " + size+", "+collection.getData().size()+", Address:"+System.identityHashCode(collection));
            /*
            if (size == 0) {
                errorCode = ErrorCodes.NO_DATA_FOUND;
            } else {*/
            transactions = collection.getData();
                LogMy.d(TAG,"mLastFetchTransactions size: "+transactions.size());

                while(collection.getCurrentPage().size() > 0) {
                    collection = collection.nextPage();
                    LogMy.d(TAG,"nextPage size: "+collection.getData().size()+", "+collection.getTotalObjects()+", Address:"+System.identityHashCode(collection));
                    transactions.addAll(collection.getData());

                    LogMy.d(TAG, "mLastFetchTransactions size: " + transactions.size());
                }
                LogMy.d(TAG, "mLastFetchTransactions final size: " + transactions.size());
//            }
        } catch (BackendlessException e) {
            LogMy.e(TAG,"Failed to fetch transactions: "+e.toString());
            return null;
        }

        return transactions;
    }
}
