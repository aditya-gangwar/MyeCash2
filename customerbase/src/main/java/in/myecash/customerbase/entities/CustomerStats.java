package in.myecash.customerbase.entities;

import in.myecash.appbase.entities.MyCashback;

/**
 * Created by adgangwa on 28-09-2016.
 */
public class CustomerStats {
    private int mchnt_cnt;

    private int bill_amt_total;
    private int bill_amt_no_cb;

    private int cb_credit;
    private int cb_debit;

    private int acc_credit;
    private int acc_debit;

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
    public int getMchnt_cnt() {
        return mchnt_cnt;
    }

    public int getBill_amt_total() {
        return bill_amt_total;
    }

    public int getBill_amt_no_cb() {
        return bill_amt_no_cb;
    }

    public int getCb_credit() {
        return cb_credit;
    }

    public int getCb_debit() {
        return cb_debit;
    }

    public int getAcc_credit() {
        return acc_credit;
    }

    public int getAcc_debit() {
        return acc_debit;
    }
}
