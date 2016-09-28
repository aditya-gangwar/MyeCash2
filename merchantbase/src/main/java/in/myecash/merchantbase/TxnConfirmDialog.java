package in.myecash.merchantbase;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import in.myecash.common.database.Transaction;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;

/**
 * Created by adgangwa on 14-04-2016.
 */
public class TxnConfirmDialog extends DialogFragment
        implements DialogInterface.OnClickListener {

    private static final String TAG = "TxnConfirmDialog";
    private static final String ARG_TXN = "txn";
    private static final String ARG_CASH_PAID = "cashPaid";

    public static TxnConfirmDialog newInstance(Transaction txn, int cashPaid) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_TXN, txn);
        args.putInt(ARG_CASH_PAID, cashPaid);
        TxnConfirmDialog fragment = new TxnConfirmDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_txn_confirm_2, null);

        bindUiResources(v);

        displayTransactionValues();

        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setView(v)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();

        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(TxnConfirmDialog.this, (AlertDialog) dialog);
            }
        });

        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        sendResult(Activity.RESULT_OK);
        dialog.dismiss();
    }

    private void sendResult(int resultCode) {
        LogMy.d(TAG, "In sendResult");
        if (getTargetFragment() == null) {
            return;
        }
        getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, null);
    }

    private void displayTransactionValues() {

        Transaction curTransaction = (Transaction) getArguments().getSerializable(ARG_TXN);
        int cashPaid = getArguments().getInt(ARG_CASH_PAID);

        //int total = 0;
        //mInputCustomer.setText(curTransaction.getCustomer_id());

        int toPay = 0;
        int returnCash = 0;
        boolean anyDebit = false;

        // Bill amount
        int value = curTransaction.getTotal_billed();
        if(value <= 0) {
            mLayoutBillAmt.setVisibility(View.GONE);
            // no point of any debit when no bill amount
            mLayoutDebitCl.setVisibility(View.GONE);
            mLayoutDebitCb.setVisibility(View.GONE);
        } else {
            mInputBillAmt.setText(AppCommonUtil.getSignedAmtStr(value, true));
            toPay = toPay + value;

            value = curTransaction.getCl_debit();
            if(value <= 0) {
                mLayoutDebitCl.setVisibility(View.GONE);
            } else {
                mInputDebitCl.setText(AppCommonUtil.getSignedAmtStr(value, false));
                toPay = toPay - value;
                anyDebit = true;
            }

            value = curTransaction.getCb_debit();
            if(value <= 0) {
                mLayoutDebitCb.setVisibility(View.GONE);
            } else {
                mInputDebitCb.setText(AppCommonUtil.getSignedAmtStr(value, false));
                toPay = toPay - value;
                anyDebit = true;
            }
        }

        if(anyDebit) {
            mInputToPay.setText(AppCommonUtil.getSignedAmtStr(toPay, true));
        } else {
            mLayoutToPay.setVisibility(View.GONE);
            mDividerToPay.setVisibility(View.GONE);
        }

        //always show cash paid - as -ve though
        mInputCashPaid.setText(AppCommonUtil.getSignedAmtStr(cashPaid, false));

        value = curTransaction.getCl_credit();
        if(value <= 0) {
            mLayoutAddCl.setVisibility(View.GONE);
            mLayoutBalance.setVisibility(View.GONE);
            mDividerBalance.setVisibility(View.GONE);

            returnCash = cashPaid - toPay;
        } else {
            int balance = cashPaid - toPay;
            if(balance>0) {
                // check for 'bill amount' again - this to avoid un-necessary showing Blanace row for 'put Add CL' txns
                if(curTransaction.getTotal_billed() > 0) {
                    mLayoutBalance.setVisibility(View.VISIBLE);
                    mDividerBalance.setVisibility(View.VISIBLE);
                    mInputBalance.setText(AppCommonUtil.getSignedAmtStr(balance, false));
                } else {
                    mLayoutBalance.setVisibility(View.GONE);
                    mDividerBalance.setVisibility(View.GONE);
                }
                mInputAddCl.setText(AppCommonUtil.getSignedAmtStr(value, true));
                returnCash = balance - value;
            } else {
                LogMy.wtf(TAG, "Doubtful state: "+value+","+cashPaid+","+toPay);
            }
        }

        if(returnCash > 0) {
            mLabelReturnCash.setText("RETURN");
            mInputReturnCash.setText(AppCommonUtil.getSignedAmtStr(returnCash, false));
            mInputReturnCash.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
            mLayoutReturnCash.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.bg_light_pink));
        } else {
            mLabelReturnCash.setText("COLLECT");
            mInputReturnCash.setText(AppCommonUtil.getSignedAmtStr(Math.abs(returnCash), true));
            mLayoutReturnCash.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.bg_light_green));
        }

        // add cashback
        value = curTransaction.getCb_credit();
        if(value == 0) {
            mLayoutAddCb.setVisibility(View.GONE);
        } else {
            mInputAddCb.setText(AppCommonUtil.getSignedAmtStr(value, true));
        }
    }

    //private EditText mInputCustomer;

    private LinearLayout mLayoutBillAmt;
    private EditText mInputBillAmt;
    //private Space mSpaceBillAmt;

    private LinearLayout mLayoutDebitCl;
    private EditText mInputDebitCl;

    private LinearLayout mLayoutDebitCb;
    private EditText mInputDebitCb;

    private View mDividerToPay;
    private LinearLayout mLayoutToPay;
    private EditText mInputToPay;

    private LinearLayout mLayoutCashpaid;
    private EditText mInputCashPaid;

    private View mDividerBalance;
    private LinearLayout mLayoutBalance;
    private EditText mInputBalance;

    private LinearLayout mLayoutAddCl;
    private EditText mInputAddCl;

    private LinearLayout mLayoutReturnCash;
    private EditText mLabelReturnCash;
    private EditText mInputReturnCash;

    private LinearLayout mLayoutAddCb;
    private EditText mInputAddCb;

    private void bindUiResources(View v) {
        //mInputCustomer = (EditText) v.findViewById(R.id.input_customer);

        mLayoutBillAmt = (LinearLayout) v.findViewById(R.id.layout_bill_amt);
        mInputBillAmt = (EditText) v.findViewById(R.id.input_bill_amt);
        //mSpaceBillAmt = (Space) v.findViewById(R.id.space_bill_amt);

        mLayoutDebitCl = (LinearLayout) v.findViewById(R.id.layout_debit_account);
        mInputDebitCl = (EditText) v.findViewById(R.id.input_debit_account);

        mLayoutDebitCb = (LinearLayout) v.findViewById(R.id.layout_redeem_cb);
        mInputDebitCb = (EditText) v.findViewById(R.id.input_redeem_cb);

        mDividerToPay = v.findViewById(R.id.divider_to_pay);
        mLayoutToPay = (LinearLayout) v.findViewById(R.id.layout_to_pay);
        mInputToPay = (EditText) v.findViewById(R.id.input_to_pay);

        mLayoutCashpaid = (LinearLayout) v.findViewById(R.id.layout_cash_paid);
        mInputCashPaid = (EditText) v.findViewById(R.id.input_cash_paid);

        mDividerBalance = v.findViewById(R.id.divider_balance);
        mLayoutBalance = (LinearLayout) v.findViewById(R.id.layout_balance);
        mInputBalance = (EditText) v.findViewById(R.id.input_balance);

        mLayoutAddCl = (LinearLayout) v.findViewById(R.id.layout_add_account);
        mInputAddCl = (EditText) v.findViewById(R.id.input_add_account);

        mLayoutReturnCash = (LinearLayout) v.findViewById(R.id.layout_return_cash);
        mLabelReturnCash = (EditText) v.findViewById(R.id.label_cash_to_pay);
        mInputReturnCash = (EditText) v.findViewById(R.id.input_return_cash);

        mLayoutAddCb = (LinearLayout) v.findViewById(R.id.layout_add_cb);
        mInputAddCb = (EditText) v.findViewById(R.id.input_add_cb);
    }
}
