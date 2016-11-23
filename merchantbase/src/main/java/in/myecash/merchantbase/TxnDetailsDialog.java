package in.myecash.merchantbase;

import android.app.Activity;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.text.SimpleDateFormat;
import java.util.Date;

import in.myecash.appbase.utilities.TxnReportsHelper;
import in.myecash.common.CommonUtils;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.database.Transaction;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.merchantbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 15-09-2016.
 */
public class TxnDetailsDialog extends DialogFragment {
    private static final String TAG = "TxnDetailsDialog";
    private static final String ARG_POSITION = "argPosition";

    private TxnDetailsDialogIf mCallback;
    private SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

    public interface TxnDetailsDialogIf {
        MyRetainedFragment getRetainedFragment();
        void showTxnImg(int currTxnPos);
        void cancelTxn(int txnPos);
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
                .setNeutralButton("Cancel Txn", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Do nothing here because we override this button later to change the close behaviour.
                        //However, we still need this because on older versions of Android unless we
                        //pass a handler the button doesn't get instantiated
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

        // hide fields for customer care logins only
        if(mCallback.getRetainedFragment().mMerchantUser.isPseudoLoggedIn()) {

            // check if file locally available - will be after the call to showTxnImg()
            // if not, set the listener
            Bitmap image = mCallback.getRetainedFragment().mLastFetchedImage;
            if(image != null) {
                int radiusInDp = (int) getResources().getDimension(R.dimen.txn_img_image_width);
                int radiusInPixels = AppCommonUtil.dpToPx(radiusInDp);
                Bitmap scaledImg = Bitmap.createScaledBitmap(image,radiusInPixels,radiusInPixels,true);

                mTxnImage.setVisibility(View.VISIBLE);
                mTxnImage.setImageBitmap(scaledImg);

            } else {
                mTxnImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(txn.getImgFileName()==null || txn.getImgFileName().isEmpty()) {
                            AppCommonUtil.toast(getActivity(), "Card image not required for this txn");
                        } else {
                            // start file download
                            // pass index of current shown txn - so as this dialog can be started again to show the same txn
                            mCallback.showTxnImg(position);
                            getDialog().dismiss();
                        }
                    }
                });
            }
        } else {
            mTxnImage.setVisibility(View.GONE);
        }

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
            /*if(txn.getComments()==null || txn.getComments().isEmpty()) {
                mLayoutComments.setVisibility(View.GONE);
            } else {
                mLayoutComments.setVisibility(View.VISIBLE);
                mComments.setText(txn.getComments());
            }*/

            mInputTotalBill.setText(AppCommonUtil.getAmtStr(txn.getTotal_billed()));
            mInputCbBill.setText(AppCommonUtil.getAmtStr(txn.getCb_billed()));

            mInputCustomerId.setText(txn.getCust_private_id());
            mInputMobileNum.setText(CommonUtils.getPartialVisibleStr(txn.getCustomer_id()));
            mCardUsed.setText(txn.getUsedCardId());
            mPinUsed.setText(txn.getCpin());

            String cbData = AppCommonUtil.getAmtStr(txn.getCb_credit())+" @ "+txn.getCb_percent()+"%";
            mInputCbAward.setText(cbData);
            mInputCbRedeem.setText(AppCommonUtil.getAmtStr(txn.getCb_debit()));

            mInputAccAdd.setText(AppCommonUtil.getAmtStr(txn.getCl_credit()));
            mInputAccDebit.setText(AppCommonUtil.getAmtStr(txn.getCl_debit()));

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
    public void onStart()
    {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point

        final int position = getArguments().getInt(ARG_POSITION, -1);
        final Transaction txn = mCallback.getRetainedFragment().mLastFetchTransactions.get(position);

        final AlertDialog d = (AlertDialog)getDialog();
        if(d != null)
        {
            Button neutralButton = d.getButton(Dialog.BUTTON_NEUTRAL);

            Date dbTime = TxnReportsHelper.getTxnInDbStartTime();
            LogMy.d( TAG, "dbTime: "+ String.valueOf(dbTime.getTime()) );

            if(txn.getCreate_time().getTime() > dbTime.getTime() &&
                    txn.getCancelTime()==null) {
                neutralButton.setEnabled(true);
                neutralButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCallback.cancelTxn(position);
                        d.dismiss();
                    }
                });
            } else {
                neutralButton.setEnabled(false);
                neutralButton.setOnClickListener(null);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // reset it
        mCallback.getRetainedFragment().mLastFetchedImage = null;
    }

    private View mLayoutCancelled;
    private EditText mInputCancelTime;

    private EditText mInputTxnId;
    private EditText mInputTxnTime;

    private View mLayoutInvNum;
    private EditText mInvoiceNum;
    //private View mLayoutComments;
    //private EditText mComments;

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

        mLayoutCancelled = v.findViewById(R.id.layout_cancelled);
        mInputCancelTime = (EditText) v.findViewById(R.id.input_cancel_time);

        mInputTxnId = (EditText) v.findViewById(R.id.input_txn_id);
        mInputTxnTime = (EditText) v.findViewById(R.id.input_txn_time);

        mLayoutInvNum = v.findViewById(R.id.layout_invoice_num);
        mInvoiceNum = (EditText) v.findViewById(R.id.input_invoice_num);
        //mLayoutComments = v.findViewById(R.id.layout_comments);
        //mComments = (EditText) v.findViewById(R.id.input_comments);

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

