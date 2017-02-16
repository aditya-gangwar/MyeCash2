package in.myecash.customerbase;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import java.text.SimpleDateFormat;

import in.myecash.appbase.BaseDialog;
import in.myecash.appbase.entities.MyTransaction;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.database.Transaction;
import in.myecash.customerbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 15-09-2016.
 */
public class TxnDetailsDialog extends BaseDialog {
    private static final String TAG = "CustApp-TxnDetailsDialog";
    private static final String ARG_POSITION = "argPosition";

    private TxnDetailsDialogIf mCallback;
    private SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

    public interface TxnDetailsDialogIf {
        MyRetainedFragment getRetainedFragment();
        //void showMchntDetails(String mchntId);
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
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_txn_details_custapp, null);

        bindUiResources(v);

        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setView(v)
                .setPositiveButton(android.R.string.ok, this)
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

            mCardUsed.setText(txn.getUsedCardId());
            mPinUsed.setText(txn.getCpin());

            //mInputTotalBill.setText(AppCommonUtil.getAmtStr(txn.getTotal_billed()));
            //mInputCbBill.setText(AppCommonUtil.getAmtStr(txn.getCb_billed()));
            int noCbBill = txn.getTotal_billed() - txn.getCb_billed();
            if(noCbBill > 0) {
                String str = "* "+AppCommonUtil.getAmtStr(txn.getTotal_billed());
                mInputTotalBill.setText(str);

                mLayoutCbBill.setVisibility(View.VISIBLE);
                str = "(* " + AppCommonUtil.getAmtStr(noCbBill) + " Bill for No Cashback Items)";
                mInputCbBill.setText(str);
            } else {
                mInputTotalBill.setText(AppCommonUtil.getAmtStr(txn.getTotal_billed()));
                mLayoutCbBill.setVisibility(View.GONE);
            }

            //String cbData = AppCommonUtil.getAmtStr(txn.getCb_credit())+" @ "+txn.getCb_percent()+"%";
            //mInputCbAward.setText(cbData);
            int totalCb = txn.getCb_credit() + txn.getExtra_cb_credit();
            String detailStr = MyTransaction.getCbDetailStr(txn);

            if( txn.getCb_credit()>0 && txn.getExtra_cb_credit()>0) {
                // both CB applicable - this long details - show in seperate line
                mInputCbAward.setText(AppCommonUtil.getAmtStr(totalCb));
                mLayoutCbDetails.setVisibility(View.VISIBLE);
                mInputCbDetails.setText(detailStr);

            } else {
                // single CB type - thus show in same line
                mLayoutCbDetails.setVisibility(View.GONE);
                String str2 = AppCommonUtil.getAmtStr(totalCb) + " " + detailStr;
                mInputCbAward.setText(str2);
            }

            mInputCbRedeem.setText(AppCommonUtil.getAmtStr(txn.getCb_debit()));

            mInputAccAdd.setText(AppCommonUtil.getAmtStr(txn.getCl_credit()));
            mInputAccDebit.setText(AppCommonUtil.getAmtStr(txn.getCl_debit()));

            mInputMerchant.setText(txn.getMerchant_name());
            mInputMchntId.setText(txn.getMerchant_id());
            /*mInputMchntId.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        mCallback.showMchntDetails(txn.getMerchant_id());
                        getDialog().dismiss();
                        return true;
                    }
                    return false;
                }
            });
            mInputMchntId.setText(Html.fromHtml("<u>"+txn.getMerchant_id()+"</u>"));*/

            // Changes if cancelled txn
            mInputCbAward2.setVisibility(View.GONE);
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
                    if(txn.getExtra_cb_credit() > 0) {
                        mInputCbAward2.setVisibility(View.VISIBLE);
                        mInputCbAward2.setText(AppCommonUtil.getAmtStr(txn.getExtra_cb_credit()));
                    }
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
    public void handleBtnClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                //Do nothing here because we override this button in OnShowListener to change the close behaviour.
                //However, we still need this because on older versions of Android unless we
                //pass a handler the button doesn't get instantiated
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                dialog.dismiss();
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                break;
        }
    }

    @Override
    public boolean handleTouchUp(View v) {
        return false;
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
    private View mLayoutCbBill;
    private EditText mInputCbBill;

    private EditText mInputCbAward;
    private EditText mInputCbAward2;
    private View mLayoutCbDetails;
    private EditText mInputCbDetails;

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
        mLayoutCbBill = v.findViewById(R.id.layout_cb_bill);
        mInputCbBill = (EditText) v.findViewById(R.id.input_cb_bill);

        mInputAccAdd = (EditText) v.findViewById(R.id.input_acc_add);
        mInputAccDebit = (EditText) v.findViewById(R.id.input_acc_debit);

        mInputCbAward = (EditText) v.findViewById(R.id.input_cb_award);
        mInputCbAward2 = (EditText) v.findViewById(R.id.input_cb_award2);
        mLayoutCbDetails = v.findViewById(R.id.layout_cb_details);
        mInputCbDetails = (EditText) v.findViewById(R.id.input_cb_details);

        mInputCbRedeem = (EditText) v.findViewById(R.id.input_cb_redeem);

        mInputMerchant = (EditText) v.findViewById(R.id.input_merchant_name);
        mInputMchntId = (EditText) v.findViewById(R.id.input_merchant_id);;

    }
}

