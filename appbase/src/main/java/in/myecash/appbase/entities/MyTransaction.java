package in.myecash.appbase.entities;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.exceptions.BackendlessException;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.QueryOptions;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.Transaction;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by adgangwa on 07-05-2016.
 */
public class MyTransaction {
    private static final String TAG = "BaseApp-MyTransaction";

    private Transaction mCurrTransaction;

    public MyTransaction(Transaction txn){
        mCurrTransaction = txn;
    }

    public Transaction getTransaction() {
        return mCurrTransaction;
    }

    public void setCurrTransaction(Transaction mCurrTransaction) {
        this.mCurrTransaction = mCurrTransaction;
    }

    public static List<Transaction> fetch(String whereClause, String tableName) {
        LogMy.d(TAG, "In fetchTransactionsSync: "+whereClause);
        // init values
        List<Transaction> transactions = null;

        // fetch cashback object from DB
        try {
            Backendless.Data.mapTableToClass(tableName, Transaction.class);

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

    public static Collection<Transaction> removeDuplicateTxns(List<Transaction> txnList) {
        Map<String, Transaction> map = new HashMap<>();
        for (Transaction txn : txnList) {
            String key = txn.getTrans_id();
            if (!map.containsKey(key)) {
                map.put(key, txn);
            }
        }
        return map.values();
    }

    public void commit(String pin) {
        LogMy.d(TAG, "In commit");

        mCurrTransaction.setCpin(pin);
        mCurrTransaction = Backendless.Persistence.save( mCurrTransaction );
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
}
