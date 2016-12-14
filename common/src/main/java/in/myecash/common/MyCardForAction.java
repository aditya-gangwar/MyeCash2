package in.myecash.common;

import java.util.Comparator;

/**
 * Created by adgangwa on 14-12-2016.
 */
public class MyCardForAction {

    // Action status codes
    public static String ACTION_STATUS_PENDING = "Pending";
    public static String ACTION_STATUS_OK = "Done";
    public static String ACTION_STATUS_NSC = "No Such Card";
    public static String ACTION_STATUS_WRONG_STATUS = "Wrong Status";
    public static String ACTION_STATUS_WRONG_OWNER = "Wrong Owner";
    public static String ACTION_STATUS_WRONG_ALLOT = "Wrong Allottee";
    public static String ACTION_STATUS_WRONG_ALLOT_STATUS = "Allottee Status";
    public static String ACTION_STATUS_ERROR = "Unknown Error";

    private String mScannedCode;
    private String mCardNum;
    //public String mCheckStatus;
    private String mActionStatus;

    private MyCardForAction() {}

    public MyCardForAction(String scannedCode) {
        mScannedCode = scannedCode;
        mActionStatus = ACTION_STATUS_PENDING;
    }

    public String getScannedCode() {
        return mScannedCode;
    }

    public void setScannedCode(String mScannedCode) {
        this.mScannedCode = mScannedCode;
    }

    public String getCardNum() {
        return mCardNum;
    }

    public void setCardNum(String mCardNum) {
        this.mCardNum = mCardNum;
    }

    public String getActionStatus() {
        return mActionStatus;
    }

    public void setActionStatus(String mActionStatus) {
        this.mActionStatus = mActionStatus;
    }

    /*
         * comparator functions for sorting
         */
    public static class MyCardComparator implements Comparator<MyCardForAction> {
        @Override
        public int compare(MyCardForAction lhs, MyCardForAction rhs) {
            // TODO: Handle null x or y values
            return compare(lhs.mCardNum, rhs.mCardNum);
        }
        private static int compare(String a, String b) {
            if(a==null) {
                return -1;
            } else if(b==null) {
                return 1;
            } else {
                int res = String.CASE_INSENSITIVE_ORDER.compare(a, b);
                return (res != 0) ? res : a.compareTo(b);
            }
        }
    }

}
