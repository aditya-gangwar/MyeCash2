package in.myecash.customerbase;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import java.text.SimpleDateFormat;

import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.common.CommonUtils;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.database.Transaction;
import in.myecash.customerbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 15-09-2016.
 */
public class TxnDetailsDialog extends DialogFragment {
    private static final String TAG = "CustApp-TxnDetailsDialog";
    private static final String ARG_POSITION = "argPosition";

    private TxnDetailsDialogIf mCallback;
    private SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

    public interface TxnDetailsDialogIf {
        MyRetainedFragment getRetainedFragment();
        //void showTxnImg(int currTxnPos);
    }

    public static TxnDetailsDialog newInstance(int position) {
        LogMy.d(TAG, "Creating new TxnDetailsDialog instance: "+position);
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);

        TxnDetailsDialog fragment = new TxnDetailsDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (TxnDetailsDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement TxnDetailsDialogIf");
        }

        int position = getArguments().getInt(ARG_POSITION, -1);
        initDialogView(position);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_txn_details, null);

        bindUiResources(v);

        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setView(v)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
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
                AppCommonUtil.setDialogTextSize(TxnDetailsDialog.this, (AlertDialog) dialog);
            }
        });

        return dialog;
    }

    private void initDialogView(final int position) {
        final Transaction txn = mCallback.getRetainedFragment().mLastFetchTransactions.get(position);

        if(txn != null) {
            mLayoutCancelled.setVisibility(View.GONE);

            mInputTxnId.setText(txn.getTrans_id());
            mInputTxnTime.setText(mSdfDateWithTime.format(txn.getCreate_time()));

            if(txn.getInvoiceNum()==null || txn.getInvoiceNum().isEmpty()) {
                mLayoutInvNum.setVisibility(View.GONE);
            } else {
                mLayoutInvNum.setVisibility(View.VISIBLE);
                mInvoiceNum.setText(txn.getInvoiceNum());
            }

            mCardUsed.setText(CommonUtils.getPartialVisibleStr(txn.getUsedCardId()));
            mPinUsed.setText(txn.getCpin());

            mInputTotalBill.setText(AppCommonUtil.getAmtStr(txn.getTotal_billed()));
            mInputCbBill.setText(AppCommonUtil.getAmtStr(txn.getCb_billed()));

            String cbData = AppCommonUtil.getAmtStr(txn.getCb_credit())+" @ "+txn.getCb_percent()+"%";
            mInputCbAward.setText(cbData);
            mInputCbRedeem.setText(AppCommonUtil.getAmtStr(txn.getCb_debit()));

            mInputAccAdd.setText(AppCommonUtil.getAmtStr(txn.getCl_credit()));
            mInputAccDebit.setText(AppCommonUtil.getAmtStr(txn.getCl_debit()));

            mInputMerchant.setText(txn.getMerchant_name());
            mInputMchntId.setText(txn.getMerchant_id());

            // Changes if cancelled txn
            if(txn.getCancelTime()!=null) {
                mLayoutCancelled.setVisibility(View.VISIBLE);
                mInputCancelTime.setText(mSdfDateWithTime.format(txn.getCancelTime()));

                if(txn.getTotal_billed()>0) {
                    mInputTotalBill.setPaintFlags(mInputTotalBill.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                }
                if(txn.getCb_billed() > 0) {
                    mInputCbBill.setPaintFlags(mInputCbBill.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                }

                if(txn.getCb_credit() > 0) {
                    mInputCbAward.setPaintFlags(mInputCbAward.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                }
                if(txn.getCb_debit()>0) {
                    mInputCbRedeem.setPaintFlags(mInputCbRedeem.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                }

                if(txn.getCl_debit()>0) {
                    mInputAccDebit.setPaintFlags(mInputAccDebit.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                }
            }

        } else {
            LogMy.wtf(TAG, "Txn object is null !!");
            getDialog().dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // reset it
        //mCallback.getRetainedFragment().mLastFetchedImage = null;
    }

    private View mLayoutCancelled;
    private EditText mInputCancelTime;

    private EditText mInputTxnId;
    private EditText mInputTxnTime;

    private View mLayoutInvNum;
    private EditText mInvoiceNum;

    private EditText mCardUsed;
    private EditText mPinUsed;

    private EditText mInputTotalBill;
    private EditText mInputCbBill;

    private EditText mInputCbAward;
    private EditText mInputCbRedeem;

    private EditText mInputAccAdd;
    private EditText mInputAccDebit;

    private EditText mInputMerchant;
    private EditText mInputMchntId;


    private void bindUiResources(View v) {

        mLayoutCancelled = v.findViewById(R.id.layout_cancelled);
        mInputCancelTime = (EditText) v.findViewById(R.id.input_cancel_time);

        mInputTxnId = (EditText) v.findViewById(R.id.input_txn_id);
        mInputTxnTime = (EditText) v.findViewById(R.id.input_txn_time);

        mLayoutInvNum = v.findViewById(R.id.layout_invoice_num);
        mInvoiceNum = (EditText) v.findViewById(R.id.input_invoice_num);

        mCardUsed = (EditText) v.findViewById(R.id.input_card_used);
        mPinUsed = (EditText) v.findViewById(R.id.input_pin_used);

        mInputTotalBill = (EditText) v.findViewById(R.id.input_total_bill);
        mInputCbBill = (EditText) v.findViewById(R.id.input_cb_bill);

        mInputAccAdd = (EditText) v.findViewById(R.id.input_acc_add);
        mInputAccDebit = (EditText) v.findViewById(R.id.input_acc_debit);

        mInputCbAward = (EditText) v.findViewById(R.id.input_cb_award);
        mInputCbRedeem = (EditText) v.findViewById(R.id.input_cb_redeem);

        mInputMerchant = (EditText) v.findViewById(R.id.input_merchant_name);
        mInputMchntId = (EditText) v.findViewById(R.id.input_merchant_id);;

    }
}

