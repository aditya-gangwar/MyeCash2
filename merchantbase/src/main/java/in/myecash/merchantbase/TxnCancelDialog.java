package in.myecash.merchantbase;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import in.myecash.appbase.barcodeReader.BarcodeCaptureActivity;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.ValidationHelper;
import in.myecash.common.CommonUtils;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.Transaction;

/**
 * Created by adgangwa on 30-10-2016.
 */
public class TxnCancelDialog extends DialogFragment
        implements View.OnTouchListener {

    private static final String TAG = "MchntApp-TxnCancelDialog";
    private static final String ARG_TXN = "txn";
    public static final int RC_BARCODE_CAPTURE_CARD_DIALOG = 9003;

    private TxnCancelDialogIf mCallback;
    // we may loos this during screen rotation etc
    // but ignoring it for now
    private String mImgFilename;
    private String mTxnId;

    public interface TxnCancelDialogIf {
        void onCancelTxnConfirm(String txnId, String cardId, String imgFileName);
    }
    
    public static TxnCancelDialog newInstance(Transaction txn) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_TXN, txn);
        TxnCancelDialog fragment = new TxnCancelDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (TxnCancelDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement TxnCancelDialogIf");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_txn_cancel, null);

        bindUiResources(v);
        displayTransactionValues();
        mInputQrCard.setOnTouchListener(this);

        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setView(v)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //Do nothing here because we override this button later to change the close behaviour.
                                //However, we still need this because on older versions of Android unless we
                                //pass a handler the button doesn't get instantiated
                            }
                        })
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
                AppCommonUtil.setDialogTextSize(TxnCancelDialog.this, (AlertDialog) dialog);
            }
        });

        return dialog;
    }

    @Override
    public void onStart()
    {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point
        final AlertDialog d = (AlertDialog)getDialog();
        if(d != null)
        {
            Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Boolean wantToCloseDialog = false;

                    int errorCode = ValidationHelper.validateMemberCard(mInputQrCard.getText().toString());
                    if(errorCode != ErrorCodes.NO_ERROR) {
                        mInputQrCard.setError(AppCommonUtil.getErrorDesc(errorCode));
                    }

                    if(errorCode==ErrorCodes.NO_ERROR) {
                        mCallback.onCancelTxnConfirm(
                                mInputTxnId.getText().toString(),
                                mInputQrCard.getText().toString(),
                                mImgFilename);
                        wantToCloseDialog = true;
                    }

                    if (wantToCloseDialog)
                        d.dismiss();
                    //else dialog stays open. Make sure you have an obvious way to close the dialog especially if you set cancellable to false.
                }
            });
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP) {
            int vId = v.getId();
            LogMy.d(TAG, "In onTouch: " + vId);

            if (vId == R.id.input_qr_card) {// launch barcode activity.
                Intent intent = new Intent(getActivity(), BarcodeCaptureActivity.class);
                intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
                intent.putExtra(BarcodeCaptureActivity.UseFlash, false);
                mImgFilename = getTempImgFilename(mTxnId);
                intent.putExtra(BarcodeCaptureActivity.ImageFileName, mImgFilename);
                startActivityForResult(intent, RC_BARCODE_CAPTURE_CARD_DIALOG);

            }
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogMy.d(TAG,"In onActivityResult"+requestCode+","+resultCode);
        if (requestCode == RC_BARCODE_CAPTURE_CARD_DIALOG) {
            if (resultCode == ErrorCodes.NO_ERROR) {
                String qrCode = data.getStringExtra(BarcodeCaptureActivity.BarcodeObject);
                LogMy.d(TAG,"Read customer QR code: "+qrCode);
                setQrCode(qrCode);
            } else {
                LogMy.e(TAG,"Failed to read barcode");
            }
        }
    }

    private void setQrCode(String qrCode) {
        if(ValidationHelper.validateMemberCard(qrCode) == ErrorCodes.NO_ERROR) {
            mInputQrCard.setText(qrCode);
            mInputQrCard.setError(null);
        } else {
            AppCommonUtil.toast(getActivity(), "Invalid Membership card");
        }
    }

    private void displayTransactionValues() {

        Transaction txn = (Transaction) getArguments().getSerializable(ARG_TXN);
        if(txn==null) {
            //TODO: raise alarm
            return;
        }

        mTxnId = txn.getTrans_id();
        mInputTxnId.setText(txn.getTrans_id());
        mInputCustId.setText(CommonUtils.getPartialVisibleStr(txn.getCustomer_id()));

        if(txn.getCl_credit()<=0 && txn.getCl_debit()<=0) {
            mLayoutAccount.setVisibility(View.GONE);
        } else {
            mLayoutAccount.setVisibility(View.VISIBLE);

            if(txn.getCl_debit()>0) {
                mInputAccDebit.setText(AppCommonUtil.getSignedAmtStr(txn.getCl_debit(), false));
            } else {
                mLayoutAccDebit.setVisibility(View.GONE);
            }
            if(txn.getCl_credit()>0) {
                mInputAccAdd.setText(AppCommonUtil.getSignedAmtStr(txn.getCl_credit(), true));
            } else {
                mLayoutAccAdd.setVisibility(View.GONE);
            }
        }

        if(txn.getCb_credit()<=0 && txn.getCb_debit()<=0) {
            mLayoutCb.setVisibility(View.GONE);
        } else {
            mLayoutCb.setVisibility(View.VISIBLE);

            if(txn.getCb_debit()>0) {
                mInputCbDebit.setText(AppCommonUtil.getSignedAmtStr(txn.getCb_debit(), false));
            } else {
                mLayoutCbDebit.setVisibility(View.GONE);
            }
            if(txn.getCb_credit()>0) {
                mInputCbAdd.setText(AppCommonUtil.getSignedAmtStr(txn.getCb_credit(), true));
            } else {
                mLayoutCbAdd.setVisibility(View.GONE);
            }
        }

    }

    private String getTempImgFilename(String txnId) {
        return CommonConstants.PREFIX_TXN_CANCEL_IMG_FILE_NAME+Long.toString(System.currentTimeMillis())+"."+CommonConstants.PHOTO_FILE_FORMAT;
    }


    private EditText mInputTxnId;
    private EditText mInputCustId;

    private View mLayoutAccount;
    private View mLayoutAccDebit;
    private EditText mInputAccDebit;
    private View mLayoutAccAdd;
    private EditText mInputAccAdd;

    private View mLayoutCb;
    private View mLayoutCbDebit;
    private EditText mInputCbDebit;
    private View mLayoutCbAdd;
    private EditText mInputCbAdd;

    private EditText mInputQrCard;

    private void bindUiResources(View v) {
        mInputTxnId = (EditText) v.findViewById(R.id.input_txn_id);
        mInputCustId = (EditText) v.findViewById(R.id.input_custId);

        mLayoutAccount = v.findViewById(R.id.layout_account);
        mLayoutAccDebit = v.findViewById(R.id.layout_account_debit);
        mInputAccDebit = (EditText) v.findViewById(R.id.input_acc_debit);
        mLayoutAccAdd = v.findViewById(R.id.layout_account_add);
        mInputAccAdd = (EditText) v.findViewById(R.id.input_acc_add);

        mLayoutCb = v.findViewById(R.id.layout_cb);
        mLayoutCbDebit = v.findViewById(R.id.layout_cb_debit);
        mInputCbDebit = (EditText) v.findViewById(R.id.input_cb_debit);
        mLayoutCbAdd = v.findViewById(R.id.layout_cb_add);
        mInputCbAdd = (EditText) v.findViewById(R.id.input_cb_add);

        mInputQrCard = (EditText) v.findViewById(R.id.input_qr_card);
    }
}

