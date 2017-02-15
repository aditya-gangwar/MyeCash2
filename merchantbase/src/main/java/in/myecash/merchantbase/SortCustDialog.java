package in.myecash.merchantbase;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import in.myecash.appbase.BaseDialog;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.entities.MyCashback;
import in.myecash.appbase.utilities.OnSingleClickListener;

/**
 * Created by adgangwa on 15-09-2016.
 */
public class SortCustDialog extends BaseDialog {
    public static final String TAG = "MchntApp-SortCustDialog";

    public static final String ARG_SELECTED = "argSelected";
    public static final String EXTRA_SELECTION = "extraSelected";

    //private SortCustDialogIf mListener;

    /*
    public interface SortCustDialogIf {
        void onCustSortType(int sortType);
    }*/

    public static SortCustDialog newInstance(int selectedSortType) {
        LogMy.d(TAG, "Creating new SortCustDialog instance: "+selectedSortType);
        Bundle args = new Bundle();
        args.putInt(ARG_SELECTED, selectedSortType);

        SortCustDialog fragment = new SortCustDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        /*
        try {
            mListener = (SortCustDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement SortCustDialogIf");
        }*/

        // set selection
        int selected = getArguments().getInt(ARG_SELECTED);
        LogMy.d(TAG,"Setting selection to "+selected);
        switch (selected) {
            case MyCashback.CB_CMP_TYPE_UPDATE_TIME:
                mSortCustRadioGroup.check(mUpdateTime.getId());
                break;
            case MyCashback.CB_CMP_TYPE_BILL_AMT:
                mSortCustRadioGroup.check(mBillAmt.getId());
                break;
            case MyCashback.CB_CMP_TYPE_ACC_BALANCE:
                mSortCustRadioGroup.check(mBalanceAcc.getId());
                break;
            case MyCashback.CB_CMP_TYPE_ACC_ADD:
                mSortCustRadioGroup.check(mAddAcc.getId());
                break;
            case MyCashback.CB_CMP_TYPE_ACC_DEBIT:
                mSortCustRadioGroup.check(mDebitAcc.getId());
                break;
            case MyCashback.CB_CMP_TYPE_CB_BALANCE:
                mSortCustRadioGroup.check(mBalanceCb.getId());
                break;
            case MyCashback.CB_CMP_TYPE_CB_ADD:
                mSortCustRadioGroup.check(mAwardCb.getId());
                break;
            case MyCashback.CB_CMP_TYPE_CB_DEBIT:
                mSortCustRadioGroup.check(mRedeemCb.getId());
                break;
        }

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateDialog");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_sort_cust, null);
        initUiResources(v);


        // return new dialog
        final AlertDialog alertDialog =  new AlertDialog.Builder(getActivity()).setView(v)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, this)
                .create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(SortCustDialog.this, (AlertDialog) dialog);

                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new OnSingleClickListener() {
                    @Override
                    public void onSingleClick(View v) {

                        int selectedId = mSortCustRadioGroup.getCheckedRadioButtonId();
                        int selectedSortType = MyCashback.CB_CMP_TYPE_UPDATE_TIME;

                        if (selectedId == R.id.lastTxnTime) {
                            selectedSortType = MyCashback.CB_CMP_TYPE_UPDATE_TIME;

                        } else if (selectedId == R.id.billAmt) {
                            selectedSortType = MyCashback.CB_CMP_TYPE_BILL_AMT;

                        } else if (selectedId == R.id.balanceCb) {
                            selectedSortType = MyCashback.CB_CMP_TYPE_CB_BALANCE;

                        } else if (selectedId == R.id.awardCb) {
                            selectedSortType = MyCashback.CB_CMP_TYPE_CB_ADD;

                        } else if (selectedId == R.id.redeemCb) {
                            selectedSortType = MyCashback.CB_CMP_TYPE_CB_DEBIT;

                        } else if (selectedId == R.id.balanceAcc) {
                            selectedSortType = MyCashback.CB_CMP_TYPE_ACC_BALANCE;

                        } else if (selectedId == R.id.addAcc) {
                            selectedSortType = MyCashback.CB_CMP_TYPE_ACC_ADD;

                        } else if (selectedId == R.id.debitAcc) {
                            selectedSortType = MyCashback.CB_CMP_TYPE_ACC_DEBIT;
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

    private RadioGroup mSortCustRadioGroup;
    private RadioButton mUpdateTime;
    private RadioButton mBillAmt;

    private RadioButton mBalanceCb;
    private RadioButton mAwardCb;
    private RadioButton mRedeemCb;

    private RadioButton mBalanceAcc;
    private RadioButton mAddAcc;
    private RadioButton mDebitAcc;

    private void initUiResources(View v) {
        mSortCustRadioGroup = (RadioGroup) v.findViewById(R.id.custSortRadioGroup);
        mUpdateTime = (RadioButton) v.findViewById(R.id.lastTxnTime);
        mBillAmt = (RadioButton) v.findViewById(R.id.billAmt);

        mBalanceCb = (RadioButton) v.findViewById(R.id.balanceCb);
        mAwardCb = (RadioButton) v.findViewById(R.id.awardCb);
        mRedeemCb = (RadioButton) v.findViewById(R.id.redeemCb);

        mBalanceAcc = (RadioButton) v.findViewById(R.id.balanceAcc);
        mAddAcc = (RadioButton) v.findViewById(R.id.addAcc);
        mDebitAcc = (RadioButton) v.findViewById(R.id.debitAcc);
    }
}
