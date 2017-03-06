package in.myecash.common;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Created by adgangwa on 14-12-2016.
 */
public class MyCardForAction implements Serializable {

    // Action status codes
    public static final String ACTION_STATUS_PENDING = "Pending";
    public static final String ACTION_STATUS_OK = "Done";
    public static final String ACTION_STATUS_NSC = "No Such Card";
    public static final String ACTION_STATUS_WRONG_STATUS = "Wrong Status";
    public static final String ACTION_STATUS_NSA = "No Such Allottee";
    public static final String ACTION_STATUS_WRONG_ALLOT = "Wrong Allottee";
    public static final String ACTION_STATUS_WRONG_ALLOT_STATUS = "Allottee Status";
    public static final String ACTION_STATUS_ERROR = "Unknown Error";
    public static final String ACTION_STATUS_WRONG_ORDER = "Wrong Order ID";
    public static final String ACTION_STATUS_ORDER_FULL = "Order Full";
    public static final String ACTION_STATUS_ORDER_EMPTY = "No Alloted Card";

    private String scannedCode;
    private String cardNum;
    //public String mCheckStatus;
    private String actionStatus;
    private int position;

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    /*public MyCardForAction(String scannedCode) {
        mScannedCode = scannedCode;
        mActionStatus = ACTION_STATUS_PENDING;
    }*/

    public String getScannedCode() {
        return scannedCode;
    }

    public void setScannedCode(String mScannedCode) {
        this.scannedCode = mScannedCode;
    }

    public String getCardNum() {
        return cardNum;
    }

    public void setCardNum(String mCardNum) {
        this.cardNum = mCardNum;
    }

    public String getActionStatus() {
        return actionStatus;
    }

    public void setActionStatus(String mActionStatus) {
        this.actionStatus = mActionStatus;
    }

}
