package in.myecash.merchantbase;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.LogMy;
import in.myecash.commonbase.utilities.ValidationHelper;

/**
 * Created by adgangwa on 14-09-2016.
 */
public class SortTxnDialog extends DialogFragment implements DialogInterface.OnClickListener {
    public static final String TAG = "SortTxnDialog";
    public static final String ARG_SELECTED = "argSelected";

    // Txn sort parameter types
    public static final int TXN_SORT_DATE_TIME = 0;
    public static final int TXN_SORT_bILL_AMT = 1;
    public static final int TXN_SORT_CB_AWARD = 2;
    public static final int TXN_SORT_CB_REDEEM = 3;
    public static final int TXN_SORT_ACC_ADD = 4;
    public static final int TXN_SORT_ACC_DEBIT = 5;

    private SortTxnDialogIf mListener;

    public interface SortTxnDialogIf {
        void onTxnSortType(int sortType);
    }

    public static SortTxnDialog newInstance(int selectedSortType) {
        LogMy.d(TAG, "Creating new SortTxnDialog instance: "+selectedSortType);
        Bundle args = new Bundle();
        args.putInt(ARG_SELECTED, selectedSortType);

        SortTxnDialog fragment = new SortTxnDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mListener = (SortTxnDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement SortTxnDialogIf");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateDialog");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_sort_txn, null);
        initUiResources(v);
        // set selection
        int selected = getArguments().getInt(ARG_SELECTED);
        switch (selected) {
            case TXN_SORT_DATE_TIME:
                mDateTime.setSelected(true);
                break;
            case TXN_SORT_bILL_AMT:
                mBillAmt.setSelected(true);
                break;
            case TXN_SORT_CB_AWARD:
                mAwardCb.setSelected(true);
                break;
            case TXN_SORT_CB_REDEEM:
                mRedeemCb.setSelected(true);
                break;
            case TXN_SORT_ACC_ADD:
                mAddAcc.setSelected(true);
                break;
            case TXN_SORT_ACC_DEBIT:
                mDebitAcc.setSelected(true);
                break;
        }

        // return new dialog
        final AlertDialog alertDialog =  new AlertDialog.Builder(getActivity()).setView(v)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        alertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(SortTxnDialog.this, (AlertDialog) dialog);

                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        int selectedId = mSortTxnRadioGroup.getCheckedRadioButtonId();
                        if (selectedId == R.id.dateTime) {
                            mListener.onTxnSortType(TXN_SORT_DATE_TIME);

                        } else if (selectedId == R.id.billAmt) {
                            mListener.onTxnSortType(TXN_SORT_bILL_AMT);

                        } else if (selectedId == R.id.awardCb) {
                            mListener.onTxnSortType(TXN_SORT_CB_AWARD);

                        } else if (selectedId == R.id.redeemCb) {
                            mListener.onTxnSortType(TXN_SORT_CB_REDEEM);

                        } else if (selectedId == R.id.addAcc) {
                            mListener.onTxnSortType(TXN_SORT_ACC_ADD);

                        } else if (selectedId == R.id.debitAcc) {
                            mListener.onTxnSortType(TXN_SORT_ACC_DEBIT);

                        }
                        getDialog().dismiss();
                    }
                });
            }
        });

        alertDialog.setCanceledOnTouchOutside(false);
        return alertDialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        //Do nothing here because we override this button in OnShowListener to change the close behaviour.
        //However, we still need this because on older versions of Android unless we
        //pass a handler the button doesn't get instantiated
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
    }

    private RadioGroup mSortTxnRadioGroup;
    private RadioButton mDateTime;
    private RadioButton mBillAmt;
    private RadioButton mAwardCb;
    private RadioButton mRedeemCb;
    private RadioButton mAddAcc;
    private RadioButton mDebitAcc;

    private void initUiResources(View v) {
        mSortTxnRadioGroup = (RadioGroup) v.findViewById(R.id.txnSortRadioGroup);
        mDateTime = (RadioButton) v.findViewById(R.id.dateTime);
        mBillAmt = (RadioButton) v.findViewById(R.id.billAmt);
        mAwardCb = (RadioButton) v.findViewById(R.id.awardCb);
        mRedeemCb = (RadioButton) v.findViewById(R.id.redeemCb);
        mAddAcc = (RadioButton) v.findViewById(R.id.addAcc);
        mDebitAcc = (RadioButton) v.findViewById(R.id.debitAcc);
    }
}


