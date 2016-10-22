package in.myecash.merchantbase.entities;

import java.util.Date;

import in.myecash.common.constants.CommonConstants;
import in.myecash.common.database.MerchantStats;

/**
 * Created by adgangwa on 22-09-2016.
 */
public class MyMerchantStats {

    /*
     * Format:
     * <update time>,<create time>,<merchant id>,
     * <bill amt total>,<cb bill amt total>,
     * <cust cnt cb>,<cust cnt cash>,<cust cnt cb and cash>,<cust cnt no balance>,
     * <cash credit>,<cash debit>,
     * <cb credit>,<cb debit>
     */
    public static int MRCHNT_STATS_CSV_UPDATED = 0;
    public static int MRCHNT_STATS_CSV_CREATED = 1;
    public static int MRCHNT_STATS_CSV_ID = 2;
    public static int MRCHNT_STATS_CSV_BILL = 3;
    public static int MRCHNT_STATS_CSV_BILL_NOCB = 4;
    public static int MRCHNT_STATS_CSV_CNT_CB = 5;
    public static int MRCHNT_STATS_CSV_CNT_ACC = 6;
    public static int MRCHNT_STATS_CSV_CNT_BOTH = 7;
    public static int MRCHNT_STATS_CSV_CNT_NOBAL = 8;
    public static int MRCHNT_STATS_CSV_ACC_CR = 9;
    public static int MRCHNT_STATS_CSV_ACC_DB = 10;
    public static int MRCHNT_STATS_CSV_CB_CR = 11;
    public static int MRCHNT_STATS_CSV_CB_DB = 12;
    public static int MRCHNT_STATS_CSV_TOTAL_FIELDS = 13;

    public static String toCsvString(MerchantStats stats) {

        String[] csvFields = new String[MRCHNT_STATS_CSV_TOTAL_FIELDS];
        if(stats.getUpdated()!=null) {
            csvFields[MRCHNT_STATS_CSV_UPDATED] = String.valueOf(stats.getUpdated().getTime());
        } else {
            csvFields[MRCHNT_STATS_CSV_UPDATED] = "";
        }
        csvFields[MRCHNT_STATS_CSV_CREATED] = String.valueOf(stats.getCreated().getTime());
        csvFields[MRCHNT_STATS_CSV_ID] = stats.getMerchant_id() ;
        csvFields[MRCHNT_STATS_CSV_BILL] = stats.getBill_amt_total().toString() ;
        csvFields[MRCHNT_STATS_CSV_BILL_NOCB] = stats.getBill_amt_no_cb().toString() ;
        csvFields[MRCHNT_STATS_CSV_CNT_CB] = stats.getCust_cnt_cb().toString() ;
        csvFields[MRCHNT_STATS_CSV_CNT_ACC] = stats.getCust_cnt_cash().toString() ;
        csvFields[MRCHNT_STATS_CSV_CNT_BOTH] = stats.getCust_cnt_cb_and_cash().toString() ;
        csvFields[MRCHNT_STATS_CSV_CNT_NOBAL] = stats.getCust_cnt_no_balance().toString() ;
        csvFields[MRCHNT_STATS_CSV_ACC_CR] = stats.getCash_credit().toString() ;
        csvFields[MRCHNT_STATS_CSV_ACC_DB] = stats.getCash_debit().toString() ;
        csvFields[MRCHNT_STATS_CSV_CB_CR] = stats.getCb_credit().toString() ;
        csvFields[MRCHNT_STATS_CSV_CB_DB] = stats.getCb_debit().toString() ;

        // combine to single string
        StringBuffer buff = new StringBuffer(MRCHNT_STATS_CSV_TOTAL_FIELDS*10);
        for (String field:csvFields) {
            buff.append(field).append(CommonConstants.CSV_DELIMETER);
        }
        return buff.toString();
    }

    public static MerchantStats fromCsvString(String csvStr) {

        MerchantStats stats = new MerchantStats();
        String[] csvFields = csvStr.split(CommonConstants.CSV_DELIMETER, -1);

        if(csvFields[MRCHNT_STATS_CSV_UPDATED]!=null && !csvFields[MRCHNT_STATS_CSV_UPDATED].isEmpty()) {
            stats.setUpdated(new Date(Long.parseLong(csvFields[MRCHNT_STATS_CSV_UPDATED])));
        }
        stats.setCreated(new Date(Long.parseLong(csvFields[MRCHNT_STATS_CSV_CREATED])));
        stats.setMerchant_id(csvFields[MRCHNT_STATS_CSV_ID]);
        stats.setBill_amt_total(Integer.parseInt(csvFields[MRCHNT_STATS_CSV_BILL]));
        stats.setBill_amt_no_cb(Integer.parseInt(csvFields[MRCHNT_STATS_CSV_BILL_NOCB]));
        stats.setCust_cnt_cb(Integer.parseInt(csvFields[MRCHNT_STATS_CSV_CNT_CB]));
        stats.setCust_cnt_cash(Integer.parseInt(csvFields[MRCHNT_STATS_CSV_CNT_ACC]));
        stats.setCust_cnt_cb_and_cash(Integer.parseInt(csvFields[MRCHNT_STATS_CSV_CNT_BOTH]));
        stats.setCust_cnt_no_balance(Integer.parseInt(csvFields[MRCHNT_STATS_CSV_CNT_NOBAL]));
        stats.setCash_credit(Integer.parseInt(csvFields[MRCHNT_STATS_CSV_ACC_CR]));
        stats.setCash_debit(Integer.parseInt(csvFields[MRCHNT_STATS_CSV_ACC_DB]));
        stats.setCb_credit(Integer.parseInt(csvFields[MRCHNT_STATS_CSV_CB_CR]));
        stats.setCb_debit(Integer.parseInt(csvFields[MRCHNT_STATS_CSV_CB_DB]));

        return stats;
    }
}
