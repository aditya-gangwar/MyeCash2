package in.myecash.merchantbase;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import java.text.SimpleDateFormat;

import in.myecash.commonbase.constants.CommonConstants;
import in.myecash.commonbase.constants.DbConstants;
import in.myecash.commonbase.entities.MyGlobalSettings;
import in.myecash.commonbase.models.Transaction;
import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.LogMy;
import in.myecash.merchantbase.entities.MyCashback;
import in.myecash.merchantbase.entities.MyCustomer;
import in.myecash.merchantbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 15-09-2016.
 */
public class TxnDetailsDialog extends DialogFragment {
    private static final String TAG = "TxnDetailsDialog";
    private static final String ARG_POSITION = "cbPosition";

    private TxnDetailsDialogIf mCallback;
    private SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

    public interface TxnDetailsDialogIf {
        MyRetainedFragment getRetainedFragment();
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
        Transaction txn = mCallback.getRetainedFragment().mLastFetchTransactions.get(position);
        initDialogView(txn);
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

    private void initDialogView(Transaction txn) {

        // hide fields for customer care logins only
        mTxnImage.setVisibility(View.GONE);

        if(txn != null) {
            mInputTxnId.setText(txn.getTrans_id());
            mInputTxnTime.setText(mSdfDateWithTime.format(txn.getCreate_time()));

            mInputTotalBill.setText(AppCommonUtil.getAmtStr(txn.getTotal_billed()));
            mInputCbBill.setText(AppCommonUtil.getAmtStr(txn.getCb_billed()));

            mInputCustomerId.setText(txn.getCust_private_id());
            mInputMobileNum.setText(AppCommonUtil.getPartialVisibleStr(txn.getCustomer_id()));
            mCardUsed.setText(txn.getUsedCardId());
            mPinUsed.setText(txn.getCpin());

            String cbData = AppCommonUtil.getAmtStr(txn.getCb_credit())+" @ "+txn.getCb_percent()+"%";
            mInputCbAward.setText(cbData);
            mInputCbRedeem.setText(AppCommonUtil.getAmtStr(txn.getCb_debit()));

            mInputAccAdd.setText(AppCommonUtil.getAmtStr(txn.getCl_credit()));
            mInputAccDebit.setText(AppCommonUtil.getAmtStr(txn.getCl_debit()));

        } else {
            LogMy.wtf(TAG, "Txn object is null !!");
            getDialog().dismiss();
        }
    }

    private EditText mInputTxnId;
    private EditText mInputTxnTime;
    private EditText mInputTotalBill;
    private EditText mInputCbBill;

    private EditText mInputCustomerId;
    private EditText mInputMobileNum;
    private EditText mCardUsed;
    private EditText mPinUsed;

    private EditText mInputCbAward;
    private EditText mInputCbRedeem;

    private EditText mInputAccAdd;
    private EditText mInputAccDebit;

    private ImageView mTxnImage;

    private void bindUiResources(View v) {

        mInputTxnId = (EditText) v.findViewById(R.id.input_txn_id);
        mInputTxnTime = (EditText) v.findViewById(R.id.input_txn_time);

        mInputTotalBill = (EditText) v.findViewById(R.id.input_total_bill);
        mInputCbBill = (EditText) v.findViewById(R.id.input_cb_bill);

        mInputCustomerId = (EditText) v.findViewById(R.id.input_customer_id);;
        mInputMobileNum = (EditText) v.findViewById(R.id.input_customer_mobile);
        mCardUsed = (EditText) v.findViewById(R.id.input_card_used);
        mPinUsed = (EditText) v.findViewById(R.id.input_pin_used);

        mInputAccAdd = (EditText) v.findViewById(R.id.input_acc_add);
        mInputAccDebit = (EditText) v.findViewById(R.id.input_acc_debit);

        mInputCbAward = (EditText) v.findViewById(R.id.input_cb_award);
        mInputCbRedeem = (EditText) v.findViewById(R.id.input_cb_redeem);

        mTxnImage = (ImageView) v.findViewById(R.id.txnImage);

    }
}

