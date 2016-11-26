package in.myecash.common;

import com.backendless.exceptions.BackendlessException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.Cashback;
import in.myecash.common.database.Transaction;

/**
 * Created by adgangwa on 30-09-2016.
 */
public class CsvConverter {

    private static SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

    /*
     * Index of various parameters in Txn CSV records
     * Per day file containing these records is created by backend,
     * but used by app to show txns to user.
     * Format:
     * trans_id,time,merchant_id,merchant_name,customer_id,cust_private_id,used_card_id
     * total_billed,cb_billed,cl_debit,cl_credit,cb_debit,cb_credit,cb_percent,cpin\n
     */
    private static int TXN_CSV_IDX_ID = 0;
    private static int TXN_CSV_IDX_TIME = 1;
    private static int TXN_CSV_IDX_MERCHANT_ID = 2;
    private static int TXN_CSV_IDX_MERCHANT_NAME = 3;
    private static int TXN_CSV_IDX_CUSTOMER_ID = 4;
    private static int TXN_CSV_IDX_CUSTOMER_PVT_ID = 5;
    private static int TXN_CSV_IDX_USED_CARD_ID = 6;
    private static int TXN_CSV_IDX_TOTAL_BILLED = 7;
    private static int TXN_CSV_IDX_CB_BILLED = 8;
    private static int TXN_CSV_IDX_ACC_DEBIT = 9;
    private static int TXN_CSV_IDX_ACC_CREDIT = 10;
    private static int TXN_CSV_IDX_CB_REDEEM = 11;
    private static int TXN_CSV_IDX_CB_AWARD = 12;
    private static int TXN_CSV_IDX_CB_RATE = 13;
    private static int TXN_CSV_IDX_CUST_PIN = 14;
    private static int TXN_CSV_IDX_IMG_FILE = 15;
    private static int TXN_CSV_IDX_INV_NUM = 16;
    private static int TXN_CSV_IDX_COMMENTS = 17;
    private static int TXN_CSV_TOTAL_FIELDS = 18;

    // Total size of above fields = 15*10 + 50;
    public static final int TXN_CSV_MAX_SIZE = 256;
    private static final String TXN_CSV_DELIM = ",";

    public static String csvStrFromTxn(Transaction txn) {
        String[] csvFields = new String[TXN_CSV_TOTAL_FIELDS];

        csvFields[TXN_CSV_IDX_ID] = txn.getTrans_id();
        csvFields[TXN_CSV_IDX_TIME] = mSdfDateWithTime.format(txn.getCreate_time());
        csvFields[TXN_CSV_IDX_MERCHANT_ID] = txn.getMerchant_id();
        csvFields[TXN_CSV_IDX_MERCHANT_NAME] = txn.getMerchant_name();
        csvFields[TXN_CSV_IDX_CUSTOMER_ID] = txn.getCustomer_id();
        csvFields[TXN_CSV_IDX_CUSTOMER_PVT_ID] = txn.getCust_private_id();
        csvFields[TXN_CSV_IDX_USED_CARD_ID] = CommonUtils.getPartialVisibleStr(txn.getUsedCardId());
        csvFields[TXN_CSV_IDX_TOTAL_BILLED] = String.valueOf(txn.getTotal_billed());
        csvFields[TXN_CSV_IDX_CB_BILLED] = String.valueOf(txn.getCb_billed());
        csvFields[TXN_CSV_IDX_ACC_DEBIT] = String.valueOf(txn.getCl_debit());
        csvFields[TXN_CSV_IDX_ACC_CREDIT] = String.valueOf(txn.getCl_credit());
        csvFields[TXN_CSV_IDX_CB_REDEEM] = String.valueOf(txn.getCb_debit());
        csvFields[TXN_CSV_IDX_CB_AWARD] = String.valueOf(txn.getCb_credit());
        csvFields[TXN_CSV_IDX_CB_RATE] = txn.getCb_percent();
        csvFields[TXN_CSV_IDX_CUST_PIN] = txn.getCpin();
        String imgFilename = txn.getImgFileName();
        csvFields[TXN_CSV_IDX_IMG_FILE] = (imgFilename==null)?"":imgFilename;
        csvFields[TXN_CSV_IDX_INV_NUM] = (txn.getInvoiceNum()==null)?"":txn.getInvoiceNum();
        csvFields[TXN_CSV_IDX_COMMENTS] = (txn.getCanImgFileName()==null)?"":txn.getCanImgFileName();

        // join the fields in single CSV string
        StringBuilder sb = new StringBuilder(TXN_CSV_MAX_SIZE+csvFields[TXN_CSV_IDX_COMMENTS].length());
        for(int i=0; i<TXN_CSV_TOTAL_FIELDS; i++) {
            sb.append(csvFields[i]).append(TXN_CSV_DELIM);
        }
        return sb.toString();
    }

    public static Transaction txnFromCsvStr(String csvString) throws ParseException {
        String[] csvFields = csvString.split(TXN_CSV_DELIM, -1);

        Transaction txn = new Transaction();
        txn.setTrans_id(csvFields[TXN_CSV_IDX_ID]);
        txn.setCreate_time(mSdfDateWithTime.parse(csvFields[TXN_CSV_IDX_TIME]));
        txn.setMerchant_id(csvFields[TXN_CSV_IDX_MERCHANT_ID]);
        txn.setMerchant_name(csvFields[TXN_CSV_IDX_MERCHANT_NAME]);
        txn.setCustomer_id(csvFields[TXN_CSV_IDX_CUSTOMER_ID]);
        txn.setCust_private_id(csvFields[TXN_CSV_IDX_CUSTOMER_PVT_ID]);
        txn.setUsedCardId(csvFields[TXN_CSV_IDX_USED_CARD_ID]);
        txn.setTotal_billed(Integer.parseInt(csvFields[TXN_CSV_IDX_TOTAL_BILLED]));
        txn.setCb_billed(Integer.parseInt(csvFields[TXN_CSV_IDX_CB_BILLED]));
        txn.setCl_debit(Integer.parseInt(csvFields[TXN_CSV_IDX_ACC_DEBIT]));
        txn.setCl_credit(Integer.parseInt(csvFields[TXN_CSV_IDX_ACC_CREDIT]));
        txn.setCb_debit(Integer.parseInt(csvFields[TXN_CSV_IDX_CB_REDEEM]));
        txn.setCb_credit(Integer.parseInt(csvFields[TXN_CSV_IDX_CB_AWARD]));
        txn.setCb_percent(csvFields[TXN_CSV_IDX_CB_RATE]);
        txn.setCpin(csvFields[TXN_CSV_IDX_CUST_PIN]);
        txn.setImgFileName(csvFields[TXN_CSV_IDX_IMG_FILE]);
        txn.setInvoiceNum(csvFields[TXN_CSV_IDX_INV_NUM]);
        txn.setInvoiceNum(csvFields[TXN_CSV_IDX_COMMENTS]);
        return txn;
    }

    /*
     * Index of various parameters in Cashback CSV records (stored in CustData CSV files)
     * Format:
     * <Total Account Credit>,<Total Account Debit>,
     * <Total Cashback Credit>,<Total Cashback Debit>,
     * <Total Billed>,<Total Cashback Billed>,
     * <create time>,<update time>
     * Records with double bracket '<<>>' are only sent to 'customer care' users
     */
    private static int CB_CSV_CUST_PVT_ID = 0;
    private static int CB_CSV_MCHNT_ID = 1;
    private static int CB_CSV_ACC_CR = 2;
    private static int CB_CSV_ACC_DB = 3;
    private static int CB_CSV_CR = 4;
    private static int CB_CSV_DB = 5;
    private static int CB_CSV_TOTAL_BILL = 6;
    private static int CB_CSV_BILL = 7;
    private static int CB_CSV_CREATE_TIME = 8;
    private static int CB_CSV_UPDATE_TIME = 9;
    private static int CB_CSV_OTHER_DETAILS = 10;
    private static int CB_CSV_TOTAL_FIELDS = 11;

    // Total size of above fields = 10*10
    public static final int CB_CSV_MAX_SIZE = 128;
    private static final String CB_CSV_DELIM = ",";

    public static Cashback cbFromCsvStr(String csvRecord) {
        if(csvRecord==null || csvRecord.isEmpty())
        {
            throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "Cashback CSV record is null or empty");
        }

        Cashback cb = new Cashback();
        String[] csvFields = csvRecord.split(CB_CSV_DELIM, -1);

        cb.setCust_private_id(csvFields[CB_CSV_CUST_PVT_ID]);
        cb.setMerchant_id(csvFields[CB_CSV_MCHNT_ID]);
        cb.setCl_credit(Integer.parseInt(csvFields[CB_CSV_ACC_CR]));
        cb.setCl_debit(Integer.parseInt(csvFields[CB_CSV_ACC_DB]));
        cb.setCb_credit(Integer.parseInt(csvFields[CB_CSV_CR]));
        cb.setCb_debit(Integer.parseInt(csvFields[CB_CSV_DB]));
        cb.setTotal_billed(Integer.parseInt(csvFields[CB_CSV_TOTAL_BILL]));
        cb.setCb_billed(Integer.parseInt(csvFields[CB_CSV_BILL]));
        cb.setCreated(new Date(Long.parseLong(csvFields[CB_CSV_CREATE_TIME])));
        if(!csvFields[CB_CSV_UPDATE_TIME].isEmpty()) {
            cb.setUpdated(new Date(Long.parseLong(csvFields[CB_CSV_UPDATE_TIME])));
        }
        cb.setOther_details(csvFields[CB_CSV_OTHER_DETAILS]);

        return cb;
    }

    public static String csvStrFromCb(Cashback cb) {

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
        if(cb.getUpdated()!=null) {
            csvFields[CB_CSV_UPDATE_TIME] = String.valueOf(cb.getUpdated().getTime()) ;
        } else {
            csvFields[CB_CSV_UPDATE_TIME] = "";
        }
        csvFields[CB_CSV_OTHER_DETAILS] = cb.getOther_details() ;

        // join the fields in single CSV string
        StringBuilder sb = new StringBuilder(CB_CSV_MAX_SIZE +
                (csvFields[CB_CSV_OTHER_DETAILS]==null ? 0 : csvFields[CB_CSV_OTHER_DETAILS].length()) );

        for(int i=0; i<CB_CSV_TOTAL_FIELDS; i++) {
            sb.append(csvFields[i]).append(CB_CSV_DELIM);
        }
        return sb.toString();
    }



}
