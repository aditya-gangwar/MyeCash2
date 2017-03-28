package in.myecash.appbase;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.OnSingleClickListener;

/**
 * Created by adgangwa on 14-09-2016.
 */
public class SortTxnDialog extends BaseDialog {
    public static final String TAG = "BaseApp-SortTxnDialog";

    public static final String ARG_SELECTED = "argSelected";
    public static final String ARG_SHOW_ACC = "argShowAcc";
    public static final String EXTRA_SELECTION = "extraSelected";

    // Txn sort parameter types
    public static final int TXN_SORT_DATE_TIME = 0;
    public static final int TXN_SORT_bILL_AMT = 1;
    public static final int TXN_SORT_CB_AWARD = 2;
    public static final int TXN_SORT_CB_REDEEM = 3;
    public static final int TXN_SORT_ACC_ADD = 4;
    public static final int TXN_SORT_ACC_DEBIT = 5;

    /*
    private SortTxnDialogIf mListener;

    public interface SortTxnDialogIf {
        void onTxnSortType(int sortType);
    }*/

    public static SortTxnDialog newInstance(int selectedSortType, boolean showAcc) {
        LogMy.d(TAG, "Creating new SortTxnDialog instance: "+selectedSortType);
        Bundle args = new Bundle();
        args.putInt(ARG_SELECTED, selectedSortType);
        args.putBoolean(ARG_SHOW_ACC, showAcc);

        SortTxnDialog fragment = new SortTxnDialog();
        fragment.setArguments(args);
        return fragment;
    }

    /*
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mListener = (SortTxnDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement SortTxnDialogIf");
        }
    }*/

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateDialog");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_sort_txn, null);
        initUiResources(v);
        // set selection
        int selected = getArguments().getInt(ARG_SELECTED);
        switch (selected) {
            case TXN_SORT_DATE_TIME:
                mSortTxnRadioGroup.check(mDateTime.getId());
                break;
            case TXN_SORT_bILL_AMT:
                mSortTxnRadioGroup.check(mBillAmt.getId());
                break;
            case TXN_SORT_CB_AWARD:
                mSortTxnRadioGroup.check(mAwardCb.getId());
                break;
            case TXN_SORT_CB_REDEEM:
                mSortTxnRadioGroup.check(mRedeemCb.getId());
                break;
            case TXN_SORT_ACC_ADD:
                mSortTxnRadioGroup.check(mAddAcc.getId());
                break;
            case TXN_SORT_ACC_DEBIT:
                mSortTxnRadioGroup.check(mDebitAcc.getId());
                break;
        }

        boolean showAcc = getArguments().getBoolean(ARG_SHOW_ACC, true);
        if(!showAcc) {
            mLabelAcc.setVisibility(View.GONE);
            mAddAcc.setVisibility(View.GONE);
            mDebitAcc.setVisibility(View.GONE);
        }

        // return new dialog
        final AlertDialog alertDialog =  new AlertDialog.Builder(getActivity()).setView(v)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, this)
                .create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(SortTxnDialog.this, (AlertDialog) dialog);

                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new OnSingleClickListener() {
                    @Override
                    public void onSingleClick(View v) {

                        int selectedId = mSortTxnRadioGroup.getCheckedRadioButtonId();
                        int selectedSortType = TXN_SORT_DATE_TIME;
                        
                        if (selectedId == R.id.dateTime) {
                            selectedSortType = TXN_SORT_DATE_TIME;

                        } else if (selectedId == R.id.billAmt) {
                            selectedSortType = TXN_SORT_bILL_AMT;

                        } else if (selectedId == R.id.awardCb) {
                            selectedSortType = TXN_SORT_CB_AWARD;

                        } else if (selectedId == R.id.redeemCb) {
                            selectedSortType = TXN_SORT_CB_REDEEM;

                        } else if (selectedId == R.id.addAcc) {
                            selectedSortType = TXN_SORT_ACC_ADD;

                        } else if (selectedId == R.id.debitAcc) {
                            selectedSortType = TXN_SORT_ACC_DEBIT;

                        }

                        Intent intent = new Intent();
                        intent.putExtra(EXTRA_SELECTION,selectedSortType);
                        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                        
                        getDialog().dismiss();
                    }
                });
            }
        });

        alertDialog.setCanceledOnTouchOutside(false);
        return alertDialog;
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

    /*@Override
    public void onClick(DialogInterface dialog, int which) {
        //Do nothing here because we override this button in OnShowListener to change the close behaviour.
        //However, we still need this because on older versions of Android unless we
        //pass a handler the button doesn't get instantiated
    }*/

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
    }

    private RadioGroup mSortTxnRadioGroup;
    private RadioButton mDateTime;
    private RadioButton mBillAmt;
    private RadioButton mAwardCb;
    private RadioButton mRedeemCb;
    private View mLabelAcc;
    private RadioButton mAddAcc;
    private RadioButton mDebitAcc;

    private void initUiResources(View v) {
        mSortTxnRadioGroup = (RadioGroup) v.findViewById(R.id.txnSortRadioGroup);
        mDateTime = (RadioButton) v.findViewById(R.id.dateTime);
        mBillAmt = (RadioButton) v.findViewById(R.id.billAmt);
        mAwardCb = (RadioButton) v.findViewById(R.id.awardCb);
        mRedeemCb = (RadioButton) v.findViewById(R.id.redeemCb);
        mLabelAcc = v.findViewById(R.id.labelAcc);
        mAddAcc = (RadioButton) v.findViewById(R.id.addAcc);
        mDebitAcc = (RadioButton) v.findViewById(R.id.debitAcc);
    }
}


