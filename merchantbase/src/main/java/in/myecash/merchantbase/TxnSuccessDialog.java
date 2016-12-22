package in.myecash.merchantbase;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;

/**
 * Created by adgangwa on 24-04-2016.
 */
public class TxnSuccessDialog extends DialogFragment
        implements DialogInterface.OnClickListener {

    private static final String TAG = "MchntApp-TxnSuccessDialog";
    private static final String RS_SYMBOL = "\u20B9 ";

    private static final String ARG_MOBILE_NUM = "mobile_num";
    private static final String ARG_TXN_ID = "txnId";
    private static final String ARG_CL_BALANCE = "cl_balance";
    private static final String ARG_CB_BALANCE = "cb_balance";
    private static final String ARG_CL_BALANCE_OLD = "cl_balance_old";
    private static final String ARG_CB_BALANCE_OLD = "cb_balance_old";

    private TxnSuccessDialogIf mListener;

    public interface TxnSuccessDialogIf {
        void onTxnSuccess();
    }

    public static TxnSuccessDialog newInstance(String txnId, int clBalance, int cbBalance, int clBalanceOld, int cbBalanceOld) {
        Bundle args = new Bundle();
        args.putString(ARG_TXN_ID, txnId);
        args.putInt(ARG_CL_BALANCE, clBalance);
        args.putInt(ARG_CB_BALANCE, cbBalance);
        args.putInt(ARG_CL_BALANCE_OLD, clBalanceOld);
        args.putInt(ARG_CB_BALANCE_OLD, cbBalanceOld);

        TxnSuccessDialog fragment = new TxnSuccessDialog();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mListener = (TxnSuccessDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement TxnSuccessDialogIf");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //String mobileNum = getArguments().getString(ARG_MOBILE_NUM, null);
        String txnId = getArguments().getString(ARG_TXN_ID, null);
        int clbalance = getArguments().getInt(ARG_CL_BALANCE);
        int cbBalance = getArguments().getInt(ARG_CB_BALANCE);
        int clbalanceOld = getArguments().getInt(ARG_CL_BALANCE_OLD);
        int cbBalanceOld = getArguments().getInt(ARG_CB_BALANCE_OLD);

        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_txn_success, null);

        bindUiResources(v);
        // display values
        //mInputCustomer.setText(mobileNum);
        if(txnId!=null) {
            String txt = "ID: " + txnId;
            mInputTxnId.setText(txt);
        }
        mInputCashBalance.setText(AppCommonUtil.getAmtStr(clbalance));
        mInputCbBalance.setText(AppCommonUtil.getAmtStr(cbBalance));

        mInputCashBalanceOld.setText(AppCommonUtil.getAmtStr(clbalanceOld));
        mInputCbBalanceOld.setText(AppCommonUtil.getAmtStr(cbBalanceOld));

        //displayTransactionValues();

        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setView(v)
                .setPositiveButton(android.R.string.ok, this)
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(TxnSuccessDialog.this, (AlertDialog) dialog);
            }
        });

        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        //sendResult();
        dialog.dismiss();
    }

    private void sendResult() {
        LogMy.d(TAG, "In sendResult");
        if (mListener != null) {
            mListener.onTxnSuccess();
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        sendResult();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        sendResult();
    }

    //private EditText mInputCustomer;
    private EditText mInputTxnId;
    private EditText mInputCashBalance;
    private EditText mInputCbBalance;
    private EditText mInputCashBalanceOld;
    private EditText mInputCbBalanceOld;

    private void bindUiResources(View v) {
        //mInputCustomer = (EditText) v.findViewById(R.id.input_customer_id);

        mInputTxnId = (EditText) v.findViewById(R.id.input_txn_id);
        mInputCashBalance = (EditText) v.findViewById(R.id.input_account_balance);
        mInputCbBalance = (EditText) v.findViewById(R.id.input_cb_balance);

        mInputCashBalanceOld = (EditText) v.findViewById(R.id.input_account_balance_old);
        mInputCbBalanceOld = (EditText) v.findViewById(R.id.input_cb_balance_old);
    }
}