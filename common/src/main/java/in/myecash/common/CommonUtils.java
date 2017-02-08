package in.myecash.common;

import in.myecash.common.constants.CommonConstants;
import in.myecash.common.database.MerchantStats;
import in.myecash.common.database.Merchants;
import in.myecash.common.database.Transaction;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by adgangwa on 08-10-2016.
 */
public class CommonUtils {

    private static final SimpleDateFormat mSdfOnlyDateFilename = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_FILENAME, CommonConstants.DATE_LOCALE);

    public static boolean customerPinRequired(Merchants merchant, Transaction txn) {
        if(txn.getCancelTime()==null) {
            int cl_credit_threshold = (merchant.getCl_credit_limit_for_pin() < 0) ? MyGlobalSettings.getAccAddPinLimit() : merchant.getCl_credit_limit_for_pin();
            int cl_debit_threshold = (merchant.getCl_debit_limit_for_pin() < 0) ? MyGlobalSettings.getAccDebitPinLimit() : merchant.getCl_debit_limit_for_pin();
            int cb_debit_threshold = (merchant.getCb_debit_limit_for_pin() < 0) ? MyGlobalSettings.getCbDebitPinLimit() : merchant.getCb_debit_limit_for_pin();

            /*int cl_credit_threshold = MyGlobalSettings.getAccAddPinLimit();
            int cl_debit_threshold = MyGlobalSettings.getAccDebitPinLimit();
            int cb_debit_threshold = MyGlobalSettings.getCbDebitPinLimit();*/

            int higher_debit_threshold = Math.max(cl_debit_threshold, cb_debit_threshold);

            return (txn.getCl_credit() > cl_credit_threshold ||
                    txn.getCl_debit() > cl_debit_threshold ||
                    txn.getCb_debit() > cb_debit_threshold ||
                    (txn.getCl_debit() + txn.getCb_debit()) > higher_debit_threshold);
        }
        return true;
    }

    public static String getPartialVisibleStr(String userId) {
        // build half visible userid : XXXXX91535
        StringBuilder halfVisibleUserid = new StringBuilder();
        int hiddenlen = userId.length() - (userId.length() / 3);
        for(int i=0; i<hiddenlen; i++) {
            halfVisibleUserid.append("x");
        }
        halfVisibleUserid.append(userId.substring(hiddenlen));
        return halfVisibleUserid.toString();
    }

    /*
     * Methods to get file paths (in backend) for various types of user files
     */
    public static String getMerchantTxnDir(String merchantId) {
        // merchant directory: merchants/<first 3 chars of merchant id>/<next 2 chars of merchant id>/<merchant id>/
        return CommonConstants.MERCHANT_TXN_ROOT_DIR +
                merchantId.substring(0,3) + CommonConstants.FILE_PATH_SEPERATOR +
                merchantId.substring(0,5) + CommonConstants.FILE_PATH_SEPERATOR +
                merchantId;
    }

    public static String getMerchantCustFilePath(String merchantId) {
        // File name: customers_<merchant_id>.csv
        return CommonConstants.MERCHANT_CUST_DATA_ROOT_DIR +
                CommonConstants.MERCHANT_CUST_DATA_FILE_PREFIX+merchantId+CommonConstants.CSV_FILE_EXT;
    }

    public static String getTxnCsvFilename(Date date, String merchantId) {
        // File name: txns_<merchant_id>_<ddMMMyy>.csv
        return CommonConstants.MERCHANT_TXN_FILE_PREFIX + merchantId + "_" + mSdfOnlyDateFilename.format(date) + CommonConstants.CSV_FILE_EXT;
    }

    /*public static String getTxnImgDir(String merchantId) {
        // merchant directory: merchants/<first 3 chars of merchant id>/<next 2 chars of merchant id>/<merchant id>/
        return CommonConstants.MERCHANT_TXN_IMAGE_ROOT_DIR +
                merchantId.substring(0,3) + CommonConstants.FILE_PATH_SEPERATOR +
                merchantId.substring(0,5) + CommonConstants.FILE_PATH_SEPERATOR +
                merchantId;
    }*/

    public static String getTxnImgDir(Date date) {
        return CommonConstants.MERCHANT_TXN_IMAGE_ROOT_DIR + mSdfOnlyDateFilename.format(date);
    }

    public static boolean mchntStatsRefreshReq(MerchantStats stats) {
        boolean retValue = true;
        long now = (new Date()).getTime();
        long updateTime = (stats.getUpdated()==null) ?
                stats.getCreated().getTime() :
                stats.getUpdated().getTime();

        long timeDiff = now - updateTime;
        long noRefreshDuration = MyGlobalSettings.getMchntDashBNoRefreshMins()*CommonConstants.MILLISECS_IN_MINUTE;

        if( timeDiff <= noRefreshDuration ) {
            retValue = false;
        }
        return retValue;
    }

    public static int roundUpTo(int i, int v){
        return Math.round(i/v) * v;
    }
}
