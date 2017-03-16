package in.myecash.merchantbase;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import in.myecash.appbase.BaseDialog;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.OnSingleClickListener;
import in.myecash.common.MyGlobalSettings;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;

/**
 * Created by adgangwa on 03-03-2017.
 */

public class CreateMchntOrderDialog extends BaseDialog {
    public static final String TAG = "MchntApp-CreateMchntOrderDialog";

    private CreateMchntOrderDialogIf mListener;

    public interface CreateMchntOrderDialogIf {
        void onCreateMchntOrder(String sku, int qty, int totalPrice);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mListener = (CreateMchntOrderDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement CreateMchntOrderDialogIf");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateDialog");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_mchnt_order, null);
        initUiResources(v);

        String txt = "* Minimum Quatity is "+MyGlobalSettings.getCustCardMinQty()+" Cards";
        mLabelMinQty.setText(txt);

        mInputItemName.setText(DbConstants.skuToItemName.get(DbConstants.SKU_CUSTOMER_CARDS));
        mInputUnitPrice.setText(AppCommonUtil.getAmtStr(MyGlobalSettings.getCustCardPrice()));

        mInputQty.addTextChangedListener(textWatcher);

        // return new dialog
        final AlertDialog alertDialog =  new AlertDialog.Builder(getActivity()).setView(v)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, this)
                .create();
        if(AppConstants.BLOCK_SCREEN_CAPTURE) {
            alertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(CreateMchntOrderDialog.this, (AlertDialog) dialog);

                final Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new OnSingleClickListener() {
                    @Override
                    public void onSingleClick(View v) {
                        if(validate()) {
                            mListener.onCreateMchntOrder( DbConstants.SKU_CUSTOMER_CARDS,
                                    Integer.valueOf(mInputQty.getText().toString()),
                                    AppCommonUtil.getValueAmtStr(mInputTotalBill.getText().toString()) );
                            getDialog().dismiss();
                        }
                    }
                });
            }
        });

        alertDialog.setCanceledOnTouchOutside(false);
        return alertDialog;
    }

    private boolean validate() {
        if(mInputQty.getText().toString().isEmpty()) {
            mInputQty.setError("Quantity is Zero");
            return false;
        }

        int qty = 0;
        try {
            qty = Integer.valueOf(mInputQty.getText().toString());
        } catch (NumberFormatException e) {
            mInputQty.setError("Invalid Format");
            return false;
        }

        if (qty == 0) {
            mInputQty.setError("Quantity is Zero");
            return false;
        }
        if (qty < MyGlobalSettings.getCustCardMinQty()) {
            String txt = "Minimum Quantity is " + MyGlobalSettings.getCustCardMinQty();
            AppCommonUtil.toast(getActivity(), txt);
            return false;
        }
        if (qty > MyGlobalSettings.getCustCardMaxQty()) {
            String txt = "Max Allowed Quantity is " + MyGlobalSettings.getCustCardMaxQty();
            AppCommonUtil.toast(getActivity(), txt);
            return false;
        }
        return true;
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
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        //mListener.onPasswdResetData(null);
    }

    private EditText mInputItemName;
    private EditText mInputQty;
    private EditText mInputUnitPrice;
    private EditText mInputTotalBill;
    private EditText mLabelMinQty;

    private void initUiResources(View v) {
        mInputItemName = (EditText) v.findViewById(R.id.input_itemName);
        mInputQty = (EditText) v.findViewById(R.id.input_qty);
        mInputUnitPrice = (EditText) v.findViewById(R.id.input_price);
        mInputTotalBill = (EditText) v.findViewById(R.id.input_bill);
        mLabelMinQty = (EditText) v.findViewById(R.id.label_min_qty);
    }


    private final TextWatcher textWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        public void afterTextChanged(Editable s) {
            if (s.length() > 0) {
                try {
                    int totalPrice = MyGlobalSettings.getCustCardPrice() * Integer.valueOf(s.toString());
                    mInputTotalBill.setText(AppCommonUtil.getAmtStr(totalPrice));
                } catch (NumberFormatException ne) {
                    AppCommonUtil.toast(getActivity(), "Invalid Quantity");
                } catch (Exception e) {
                    //ignore exception
                }
            }
        }
    };
}

