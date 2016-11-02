package in.myecash.merchantbase;

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

import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.merchantbase.helper.CashPaid;

/**
 * Created by adgangwa on 13-03-2016.
 */
/*public class CashPaidDialog extends DialogFragment implements CashPaid.CashPaidIf {
    public static final String TAG = "CashPayFragment";

    public static final String EXTRA_CASH_PAID = "cashPaid";
    private static final String ARG_AMOUNT = "amount";
    private static final String ARG_OLD_VALUE = "oldValue";

    CashPaid mCashPaidHelper;
    int mCashToPay;

    public static CashPaidDialog newInstance(int amount, String oldValue) {
        LogMy.d(TAG, "Creating new cash paid dialog");
        Bundle args = new Bundle();
        args.putInt(ARG_AMOUNT, amount);
        args.putString(ARG_OLD_VALUE, oldValue);
        CashPaidDialog fragment = new CashPaidDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateDialog");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_cash_paid, null);

        mCashToPay = getArguments().getInt(ARG_AMOUNT);
        String oldValue = getArguments().getString(ARG_OLD_VALUE);
        mCashPaidHelper = new CashPaid(mCashToPay, oldValue, this, getActivity());
        mCashPaidHelper.initView(v);

        AppCommonUtil.hideKeyboard(getActivity());

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(v)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
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
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.hideKeyboard((Dialog) dialog);
                AppCommonUtil.setDialogTextSize(CashPaidDialog.this, (AlertDialog) dialog);
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

                    int amt = mCashPaidHelper.getCashPaidAmt();
                    if(amt >= mCashToPay || amt==0) {
                        sendResult(Activity.RESULT_OK, amt);
                        wantToCloseDialog = true;
                    } else {
                        mCashPaidHelper.setError("Minimum cash required: "+ AppCommonUtil.getAmtStr(mCashToPay));
                    }

                    if (wantToCloseDialog)
                        d.dismiss();
                    //else dialog stays open. Make sure you have an obvious way to close the dialog especially if you set cancellable to false.
                }
            });
        }
    }

    @Override
    public void onAmountEnter(int value) {
        // do nothing
    }

    @Override
    public void onAmountEnterFinal() {
        LogMy.d(TAG,"In onAmountEnterFinal");
        AlertDialog dialog = (AlertDialog) getDialog();
        Button positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
        positiveButton.performClick();
    }

    private void sendResult(int resultCode, int cashPaid) {
        LogMy.d(TAG,"In sendResult");
        if (getTargetFragment() == null) {
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CASH_PAID, cashPaid);
        getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, intent);
    }
}


    /*
    @Override
    public void onClick(View v) {
        int vId = v.getId();
        LogMy.d(TAG,"In onClick: "+vId);

        String curStr = mInputCPCustom.getText().toString();

        switch(vId) {
            case R.id.input_kb_bs:
                if(curStr.length()>0) {
                    mInputCPCustom.setText("");
                    mInputCPCustom.append(curStr,0,(curStr.length()-1));
                }
                break;

            case R.id.input_kb_clear:
                mInputCPCustom.setText("");
                break;

            case R.id.input_kb_0:
            case R.id.input_kb_1:
            case R.id.input_kb_2:
            case R.id.input_kb_3:
            case R.id.input_kb_4:
            case R.id.input_kb_5:
            case R.id.input_kb_6:
            case R.id.input_kb_7:
            case R.id.input_kb_8:
            case R.id.input_kb_9:
                EditText key = (EditText)v;
                // ignore 0 as first entered digit
                if( !(v.getId()==R.id.input_kb_0 && curStr.isEmpty())) {
                    mInputCPCustom.append(key.getText());
                }
                break;

            case R.id.choice_cash_pay_custom:
            case R.id.rs_cash_pay_custom:
                LogMy.d(TAG,"Clicked custom input box");
                if(!curStr.isEmpty()) {
                    int amount = Integer.parseInt(curStr);
                    sendResult(Activity.RESULT_OK, amount);
                    getDialog().dismiss();
                }
                break;

            default:
                int index=0;
                while(index < UI_SLOT_COUNT) {
                    if( vId==mInputCashPay[index].getId() || vId==mRsCashPay[index].getId()) {
                        LogMy.d(TAG,"Clicked input box: "+index);
                        String amt = mInputCashPay[index].getText().toString();
                        if(!amt.isEmpty()) {
                            int amount = Integer.parseInt(amt);
                            sendResult(Activity.RESULT_OK, amount);
                            getDialog().dismiss();
                        }
                    }
                    index++;
                }
                break;
        }
    }

    private void buildValueSet(int amount) {
        LogMy.d(TAG,"In buildValueSet");
        int rem = 0, tempNewAmt = 0;
        //mValueSet.clear();

        if(amount==0) {
            // either 'only cashload' case or 'bill amount <= redeem cash available' case
            mValueSet.add(50); mValueSet.add(100); mValueSet.add(500);
        } else {
            for (int n : currency) {
                // calculate rounded off amount
                rem = amount % n;
                tempNewAmt = (rem == 0) ? amount : (amount + (n - rem));
                // store 'rounded off amount' only if not already equal to earlier calculated amount
                if (tempNewAmt != amount) {
                    mValueSet.add(tempNewAmt);
                }
            }
        }
        // add 'amount' in case free slot available
        if(mValueSet.size() < UI_SLOT_COUNT) {
            // this will also ensure that there's always 1 element atleast
            mValueSet.add(amount);
        }
        LogMy.d(TAG,"Exiting buildValueSet: "+mValueSet.size());
    }

    private void setValuesInUi() {
        LogMy.d(TAG,"In setValuesInUi");
        // Iterating over the elements in the set
        int index = 0;
        Iterator it = mValueSet.iterator();
        while (it.hasNext()) {
            Integer element = (Integer) it.next();
            LogMy.d(TAG,"First loop: "+index+":"+element.toString());
            // mValueSet is sorted set - use only first 3
            if(index < UI_SLOT_COUNT) {
                mInputCashPay[index].setText(element.toString());
                index++;
            }
        }
        // if still there's free slots, remove them from UI
        while(index < UI_SLOT_COUNT) {
            LogMy.d(TAG,"Second loop: "+index);
            mRsCashPay[index].setVisibility(View.GONE);
            mInputCashPay[index].setVisibility(View.GONE);
            index++;
        }
    }

    private void setListeners() {
        LogMy.d(TAG, "In setListeners");
        mInputCashPay[0].setOnClickListener(this);
        mInputCashPay[1].setOnClickListener(this);
        mInputCashPay[2].setOnClickListener(this);
        mInputCPCustom.setOnClickListener(this);

        mRsCashPay[0].setOnClickListener(this);
        mRsCashPay[1].setOnClickListener(this);
        mRsCashPay[2].setOnClickListener(this);
        mRsCPCustom.setOnClickListener(this);

        initKeyboard();
    }

    private void initKeyboard() {
        mKey1.setOnClickListener(this);
        mKey2.setOnClickListener(this);
        mKey3.setOnClickListener(this);
        mKey4.setOnClickListener(this);
        mKey5.setOnClickListener(this);
        mKey6.setOnClickListener(this);
        mKey7.setOnClickListener(this);
        mKey8.setOnClickListener(this);
        mKey9.setOnClickListener(this);
        mKey0.setOnClickListener(this);
        mKeyBspace.setOnClickListener(this);
        mKeyClear.setOnClickListener(this);
    }

    private EditText mKey1;
    private EditText mKey2;
    private EditText mKey3;
    private EditText mKey4;
    private EditText mKey5;
    private EditText mKey6;
    private EditText mKey7;
    private EditText mKey8;
    private EditText mKey9;
    private EditText mKey0;
    private EditText mKeyClear;
    private ImageView mKeyBspace;

    private EditText[] mInputCashPay;
    private EditText[] mRsCashPay;
    private EditText mInputCPCustom;
    private EditText mRsCPCustom;

    private void initUiResources(View v) {
        mInputCashPay[0] = (EditText) v.findViewById(R.id.choice_cash_pay_1);
        mInputCashPay[1] = (EditText) v.findViewById(R.id.choice_cash_pay_2);
        mInputCashPay[2] = (EditText) v.findViewById(R.id.choice_cash_pay_3);
        mInputCPCustom = (EditText) v.findViewById(R.id.choice_cash_pay_custom);

        mRsCashPay[0] = (EditText) v.findViewById(R.id.rs_cash_pay_1);
        mRsCashPay[1] = (EditText) v.findViewById(R.id.rs_cash_pay_2);
        mRsCashPay[2] = (EditText) v.findViewById(R.id.rs_cash_pay_3);
        mRsCPCustom = (EditText) v.findViewById(R.id.rs_cash_pay_custom);

        mKey1 = (EditText) v.findViewById(R.id.input_kb_1);
        mKey2 = (EditText) v.findViewById(R.id.input_kb_2);
        mKey3 = (EditText) v.findViewById(R.id.input_kb_3);
        mKey4 = (EditText) v.findViewById(R.id.input_kb_4);
        mKey5 = (EditText) v.findViewById(R.id.input_kb_5);
        mKey6 = (EditText) v.findViewById(R.id.input_kb_6);
        mKey7 = (EditText) v.findViewById(R.id.input_kb_7);
        mKey8 = (EditText) v.findViewById(R.id.input_kb_8);
        mKey9 = (EditText) v.findViewById(R.id.input_kb_9);
        mKey0 = (EditText) v.findViewById(R.id.input_kb_0);
        mKeyBspace = (ImageView) v.findViewById(R.id.input_kb_bs);
        mKeyClear = (EditText) v.findViewById(R.id.input_kb_clear);
    }
    */

