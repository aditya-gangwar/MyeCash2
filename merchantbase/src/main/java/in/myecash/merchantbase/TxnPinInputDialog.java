package in.myecash.merchantbase;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import in.myecash.appbase.BaseDialog;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.OnSingleClickListener;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.ValidationHelper;

/**
 * Created by adgangwa on 30-04-2016.
 */
public class TxnPinInputDialog extends BaseDialog
        implements View.OnClickListener {

    private static final String TAG = "MchntApp-TxnPinInputDialog";
    private static final String ARG_CASH_CREDIT = "cashCredit";
    private static final String ARG_CASH_DEBIT = "cashDebit";
    private static final String ARG_CASHBACK_DEBIT = "cashbackDebit";
    private static final String ARG_CANCEL_TXNID = "cancelTxnId";

    //private static Integer[] keys = {0,1,2,3,4,5,6,7,8,9};

    private TxnPinInputDialogIf mCallback;
    private String mPin;

    public interface TxnPinInputDialogIf {
        void onTxnPin(String pin, String tag);
    }


    public static TxnPinInputDialog newInstance(int cashCredit, int cashDebit, int cashbackDebit, String cancelTxnId) {
        Bundle args = new Bundle();
        args.putInt(ARG_CASH_CREDIT, cashCredit);
        args.putInt(ARG_CASH_DEBIT, cashDebit);
        args.putInt(ARG_CASHBACK_DEBIT, cashbackDebit);
        if(cancelTxnId!=null && !cancelTxnId.isEmpty()) {
            args.putString(ARG_CANCEL_TXNID, cancelTxnId);
        }

        TxnPinInputDialog fragment = new TxnPinInputDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (TxnPinInputDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement TxnPinInputDialogIf");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_txn_pin_input, null);

        bindUiResources(v);
        if(savedInstanceState!=null) {
            LogMy.d(TAG,"Restoring");
            mPin = savedInstanceState.getString("mPin");
        } else {
            mPin = "";
        }

        // set values
        String cancelTxnId = getArguments().getString(ARG_CANCEL_TXNID);
        if(cancelTxnId!=null) {
            mLayoutAmts.setVisibility(View.GONE);
            String msg = "Cancel Transaction "+cancelTxnId+" ?";
            mLabelInfo.setText(msg);

        } else {
            int cashCredit = getArguments().getInt(ARG_CASH_CREDIT);
            int cashDebit = getArguments().getInt(ARG_CASH_DEBIT);
            int cashbackDebit = getArguments().getInt(ARG_CASHBACK_DEBIT);

            if (cashCredit > 0) {
                mInputCashAmount.setText(AppCommonUtil.getSignedAmtStr(cashCredit, true));
                mInputCashAmount.setTextColor(ContextCompat.getColor(getActivity(), R.color.green_positive));
            } else if (cashDebit > 0) {
                mInputCashAmount.setText(AppCommonUtil.getSignedAmtStr(cashDebit, false));
                mInputCashAmount.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
            } else {
                mLayoutCashAmount.setVisibility(View.GONE);
            }

            if (cashbackDebit > 0) {
                mInputCashbackAmount.setText(AppCommonUtil.getSignedAmtStr(cashbackDebit, false));
                mInputCashAmount.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
            } else {
                mLayoutCashbackAmount.setVisibility(View.GONE);
            }
        }

        initKeyboard();

        /*Dialog dialog =  new AlertDialog.Builder(getActivity(), R.style.WrapEverythingDialog)
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
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(TxnPinInputDialog.this, (AlertDialog) dialog);
            }
        });*/

        Dialog dialog =  new AlertDialog.Builder(getActivity(), R.style.WrapEverythingDialog).setView(v).create();
        //dialog.setTitle("Enter PIN");
        if(AppConstants.BLOCK_SCREEN_CAPTURE) {
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(TxnPinInputDialog.this, (AlertDialog) dialog);
            }
        });
        return dialog;
    }

    @Override
    public void handleBtnClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                //Do nothing
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
                    Boolean wantToCloseDialog = true;

                    LogMy.d(TAG, "Clicked Ok");
                    String pinOrOtp = mInputSecretPin.getText().toString();

                    int errorCode = ValidationHelper.validatePin(pinOrOtp);
                    if (errorCode == ErrorCodes.NO_ERROR) {
                        mCallback.onTxnPin(pinOrOtp, getTag());
                    } else {
                        mInputSecretPin.setError(AppCommonUtil.getErrorDesc(errorCode));
                        wantToCloseDialog = false;
                    }

                    if (wantToCloseDialog)
                        d.dismiss();
                    //else dialog stays open. Make sure you have an obvious way to close the dialog especially if you set cancellable to false.
                }
            });
        }
    }*/

    @Override
    public void onClick(View v) {
        int vId = v.getId();

        String curStr = mInputSecretPin.getText().toString();

        if (vId == R.id.input_kb_bs) {
            LogMy.d(TAG,"Clicked BS");
            if (curStr.length() > 0) {
                mInputSecretPin.setText("");
                mInputSecretPin.append(curStr, 0, (curStr.length() - 1));
                mPin = mPin.substring(0,(mPin.length()-1));
            }

        } /*else if (vId == R.id.input_kb_ok) {
            LogMy.d(TAG,"Clicked OK");
            //mInputSecretPin.setText("");
            Boolean wantToCloseDialog = true;
            //String pin = mInputSecretPin.getText().toString();

            int errorCode = ValidationHelper.validatePin(mPin);
            if (errorCode == ErrorCodes.NO_ERROR) {
                mCallback.onTxnPin(mPin, getTag());
            } else {
                mInputSecretPin.setError(AppCommonUtil.getErrorDesc(errorCode));
                wantToCloseDialog = false;
            }

            if (wantToCloseDialog)
                getDialog().dismiss();

        } */else if (vId == R.id.input_kb_0 || vId == R.id.input_kb_1 || vId == R.id.input_kb_2 || vId == R.id.input_kb_3 || vId == R.id.input_kb_4 || vId == R.id.input_kb_5 || vId == R.id.input_kb_6 || vId == R.id.input_kb_7 || vId == R.id.input_kb_8 || vId == R.id.input_kb_9) {
            LogMy.d(TAG,"Clicked Num key");
            AppCompatButton key = (AppCompatButton) v;
            if(mPin.length() >= CommonConstants.PIN_LEN) {
                AppCommonUtil.toast(getActivity(), CommonConstants.PIN_LEN+" digit PIN allowed");
            } else {
                mPin = mPin + key.getText();
                //mInputSecretPin.append(key.getText());
                mInputSecretPin.append("*");
            }
        }
    }

    private View mLayoutAmts;
    private EditText mInputCashAmount;
    private EditText mInputCashbackAmount;
    private LinearLayout mLayoutCashAmount;
    private LinearLayout mLayoutCashbackAmount;
    private EditText mLabelInfo;
    private EditText mInputSecretPin;

    private AppCompatButton mKeys[];
    private AppCompatButton mKeyOk;
    private AppCompatImageButton mKeyBspace;

    private void bindUiResources(View v) {
        mLayoutAmts = v.findViewById(R.id.layout_amounts);
        mInputCashAmount = (EditText) v.findViewById(R.id.input_cash_amount);
        mInputCashbackAmount = (EditText) v.findViewById(R.id.input_cashback_amount);
        mLayoutCashAmount = (LinearLayout) v.findViewById(R.id.layout_cash_amount);
        mLayoutCashbackAmount = (LinearLayout) v.findViewById(R.id.layout_cashback_amount);

        mLabelInfo = (EditText) v.findViewById(R.id.label_information);
        mInputSecretPin = (EditText) v.findViewById(R.id.input_secret_pin);

        mKeys = new AppCompatButton[10];
        mKeys[0] = (AppCompatButton) v.findViewById(R.id.input_kb_0);
        mKeys[1] = (AppCompatButton) v.findViewById(R.id.input_kb_1);
        mKeys[2] = (AppCompatButton) v.findViewById(R.id.input_kb_2);
        mKeys[3] = (AppCompatButton) v.findViewById(R.id.input_kb_3);
        mKeys[4] = (AppCompatButton) v.findViewById(R.id.input_kb_4);
        mKeys[5] = (AppCompatButton) v.findViewById(R.id.input_kb_5);
        mKeys[6] = (AppCompatButton) v.findViewById(R.id.input_kb_6);
        mKeys[7] = (AppCompatButton) v.findViewById(R.id.input_kb_7);
        mKeys[8] = (AppCompatButton) v.findViewById(R.id.input_kb_8);
        mKeys[9] = (AppCompatButton) v.findViewById(R.id.input_kb_9);
        mKeyBspace = (AppCompatImageButton) v.findViewById(R.id.input_kb_bs);
        mKeyOk = (AppCompatButton) v.findViewById(R.id.input_kb_ok);
    }

    private void initKeyboard() {
        // set text randomly for the keys
        //Collections.shuffle(Arrays.asList(keys));

        for(int i=0; i<10; i++) {
            mKeys[i].setOnClickListener(this);
            //mKeys[i].setText(String.valueOf(keys[i]));
        }

        mKeyBspace.setOnClickListener(this);
        mKeyOk.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                // Seperate click handler - to avoid double click of OK button
                LogMy.d(TAG,"Clicked OK");
                //mInputSecretPin.setText("");
                Boolean wantToCloseDialog = true;
                //String pin = mInputSecretPin.getText().toString();

                int errorCode = ValidationHelper.validatePin(mPin);
                if (errorCode == ErrorCodes.NO_ERROR) {
                    mCallback.onTxnPin(mPin, getTag());
                } else {
                    mInputSecretPin.setError(AppCommonUtil.getErrorDesc(errorCode));
                    wantToCloseDialog = false;
                }

                if (wantToCloseDialog)
                    getDialog().dismiss();
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("mPin", mPin);
    }

}

