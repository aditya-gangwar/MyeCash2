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
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import in.myecash.appbase.BaseDialog;
import in.myecash.appbase.entities.MyTransaction;
import in.myecash.appbase.utilities.OnSingleClickListener;
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
public class TxnDetailsDialog extends BaseDialog {
    private static final String TAG = "MchntApp-TxnDetailsDialog";
    private static final String ARG_POSITION = "argPosition";

    private TxnDetailsDialogIf mCallback;
    private SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

    public interface TxnDetailsDialogIf {
        MyRetainedFragment getRetainedFragment();
        void showCustDetails(String internalId);
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

        Dialog dialog = new AlertDialog.Builder(getActivity(),R.style.MyAnimatedDialog)
                .setView(v)
                .setPositiveButton(android.R.string.ok, this)
                .setNeutralButton("Cancel Txn", this)
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

    @Override
    public void handleBtnClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                dialog.dismiss();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                dialog.dismiss();
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                //Do nothing here because we override this button later to change the close behaviour.
                //However, we still need this because on older versions of Android unless we
                //pass a handler the button doesn't get instantiated
                break;
        }
    }

    @Override
    public boolean handleTouchUp(View v) {
        return false;
    }

    private void initDialogView(final int position) {
        LogMy.d(TAG,"Entering initDialogView");
        final Transaction txn = mCallback.getRetainedFragment().mLastFetchTransactions.get(position);

        // hide fields for customer care logins only
        /*if(mCallback.getRetainedFragment().mMerchantUser.isPseudoLoggedIn()) {

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
                            AppCommonUtil.toast(getActivity(), "Card image was not required for this txn");
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
        }*/

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

            if(mCallback.getRetainedFragment().mMerchantUser.isPseudoLoggedIn()) {
                // For CC user - show full card#
                mCardUsed.setText(txn.getUsedCardId());
            } else {
                mCardUsed.setText(CommonUtils.getPartialVisibleStr(txn.getUsedCardId()));
            }
            mPinUsed.setText(txn.getCpin());

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

            LogMy.d(TAG,"Mid check 1");
            mInputCustomerId.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        mCallback.showCustDetails(txn.getCust_private_id());
                        getDialog().dismiss();
                        return true;
                    }
                    return false;
                }
            });
            LogMy.d(TAG,"Mid check 2");
            mInputCustomerId.setText(Html.fromHtml("<u>"+txn.getCust_private_id()+"</u>"));
            mInputMobileNum.setText(CommonUtils.getPartialVisibleStr(txn.getCustomer_id()));

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

            //String cbData = AppCommonUtil.getAmtStr(txn.getCb_credit())+" @ "+txn.getCb_percent()+"%";
            //mInputCbAward.setText(cbData);

            mInputCbRedeem.setText(AppCommonUtil.getAmtStr(txn.getCb_debit()));

            if(txn.getCl_credit()==0 && txn.getCl_debit()==0) {
                mLabelAcc.setVisibility(View.GONE);
                mLayoutAccAdd.setVisibility(View.GONE);
                mLayoutAccDebit.setVisibility(View.GONE);
            } else {
                mInputAccAdd.setText(AppCommonUtil.getAmtStr(txn.getCl_credit()));
                mInputAccDebit.setText(AppCommonUtil.getAmtStr(txn.getCl_debit()));
            }

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
        LogMy.d(TAG,"Exiting initDialogView");
    }

    @Override
    public void onStart()
    {
        LogMy.d(TAG,"Entering onStart");
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
                neutralButton.setOnClickListener(new OnSingleClickListener() {
                    @Override
                    public void onSingleClick(View v) {
                        mCallback.cancelTxn(position);
                        d.dismiss();
                    }
                });
            } else {
                neutralButton.setEnabled(false);
                neutralButton.setOnClickListener(null);
            }
        }
        LogMy.d(TAG,"Exiting onStart");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // reset it
        mCallback.getRetainedFragment().mLastFetchedImage = null;
    }

    private View mLayoutCancelled;
    private TextView mInputCancelTime;

    private TextView mInputTxnId;
    private TextView mInputTxnTime;

    private View mLayoutInvNum;
    private TextView mInvoiceNum;

    private TextView mInputTotalBill;
    private View mLayoutCbBill;
    private TextView mInputCbBill;

    private EditText mInputCustomerId;
    private TextView mInputMobileNum;
    private TextView mCardUsed;
    private TextView mPinUsed;

    private TextView mInputCbAward;
    private TextView mInputCbAward2;
    private View mLayoutCbDetails;
    private TextView mInputCbDetails;

    private TextView mInputCbRedeem;

    private TextView mInputAccAdd;
    private TextView mInputAccDebit;

    private View mLabelAcc;
    private View mLayoutAccDebit;
    private View mLayoutAccAdd;

    //private ImageView mTxnImage;

    private void bindUiResources(View v) {

        mLayoutCancelled = v.findViewById(R.id.layout_cancelled);
        mInputCancelTime = (TextView) v.findViewById(R.id.input_cancel_time);

        mInputTxnId = (TextView) v.findViewById(R.id.input_txn_id);
        mInputTxnTime = (TextView) v.findViewById(R.id.input_txn_time);

        mLayoutInvNum = v.findViewById(R.id.layout_invoice_num);
        mInvoiceNum = (TextView) v.findViewById(R.id.input_invoice_num);

        mInputTotalBill = (TextView) v.findViewById(R.id.input_total_bill);
        mLayoutCbBill = v.findViewById(R.id.layout_cb_bill);
        mInputCbBill = (TextView) v.findViewById(R.id.input_cb_bill);

        mInputCustomerId = (EditText) v.findViewById(R.id.input_customer_id);;
        mInputMobileNum = (TextView) v.findViewById(R.id.input_customer_mobile);
        mCardUsed = (TextView) v.findViewById(R.id.input_card_used);
        mPinUsed = (TextView) v.findViewById(R.id.input_pin_used);

        mLabelAcc = v.findViewById(R.id.label_acc);
        mLayoutAccDebit = v.findViewById(R.id.layout_acc_debit);
        mLayoutAccAdd = v.findViewById(R.id.layout_acc_add);

        mInputAccAdd = (TextView) v.findViewById(R.id.input_acc_add);
        mInputAccDebit = (TextView) v.findViewById(R.id.input_acc_debit);

        mInputCbAward = (TextView) v.findViewById(R.id.input_cb_award);
        mInputCbAward2 = (TextView) v.findViewById(R.id.input_cb_award2);
        mLayoutCbDetails = v.findViewById(R.id.layout_cb_details);
        mInputCbDetails = (TextView) v.findViewById(R.id.input_cb_details);

        mInputCbRedeem = (TextView) v.findViewById(R.id.input_cb_redeem);

        //mTxnImage = (ImageView) v.findViewById(R.id.txnImage);

    }
}

