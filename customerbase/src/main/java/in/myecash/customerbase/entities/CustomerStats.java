package in.myecash.customerbase.entities;

import java.util.Date;

import in.myecash.commonbase.entities.MyCashback;
import in.myecash.commonbase.models.Cashback;

/**
 * Created by adgangwa on 28-09-2016.
 */
public class CustomerStats {
    private Integer mchnt_cnt;

    private Integer bill_amt_total;
    private Integer bill_amt_no_cb;

    private Integer cb_credit;
    private Integer cb_debit;

    private Integer acc_credit;
    private Integer acc_debit;

    // Update stats
    public void addToStats(MyCashback cb) {
        mchnt_cnt++;

        bill_amt_total = bill_amt_total+cb.getBillAmt();
        bill_amt_no_cb = bill_amt_no_cb+cb.getCbBillAmt();

        cb_credit = cb_credit+cb.getCbCredit();
        cb_debit = cb_debit+cb.getCbRedeem();

        acc_credit = acc_credit+cb.getClCredit();
        acc_debit = acc_debit+cb.getClDebit();
    }

    public int getClBalance() {
        return acc_credit - acc_debit;
    }

    public int getCbBalance() {
        return cb_credit - cb_debit;
    }

    // Getter methods
    public Integer getMchnt_cnt() {
        return mchnt_cnt;
    }

    public Integer getBill_amt_total() {
        return bill_amt_total;
    }

    public Integer getBill_amt_no_cb() {
        return bill_amt_no_cb;
    }

    public Integer getCb_credit() {
        return cb_credit;
    }

    public Integer getCb_debit() {
        return cb_debit;
    }

    public Integer getAcc_credit() {
        return acc_credit;
    }

    public Integer getAcc_debit() {
        return acc_debit;
    }
}
