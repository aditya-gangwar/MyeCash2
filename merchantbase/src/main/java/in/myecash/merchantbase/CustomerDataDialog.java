package in.myecash.merchantbase;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.LogMy;
import in.myecash.commonbase.utilities.ValidationHelper;

/**
 * Created by adgangwa on 09-09-2016.
 */
public class CustomerDataDialog extends DialogFragment implements DialogInterface.OnClickListener {
    public static final String TAG = "CustomerDataDialog";

    private CustomerDataDialogIf mListener;

    public interface CustomerDataDialogIf {
        void searchCustByInternalId(String internalId);
        void generateAllCustData();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mListener = (CustomerDataDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement CustomerDataDialogIf");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateDialog");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_customer_data, null);
        initUiResources(v);

        // return new dialog
        final AlertDialog alertDialog =  new AlertDialog.Builder(getActivity()).setView(v)
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
                AppCommonUtil.setDialogTextSize(CustomerDataDialog.this, (AlertDialog) dialog);

                //Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                mGetCustData.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AppCommonUtil.hideKeyboard(getDialog());
                        String custId = mInputCustId.getText().toString();
                        int error = ValidationHelper.validateCustInternalId(custId);
                        if(error == ErrorCodes.NO_ERROR) {
                            mListener.searchCustByInternalId(custId);
                            getDialog().dismiss();
                        } else if(error == ErrorCodes.EMPTY_VALUE) {
                            mListener.generateAllCustData();
                            getDialog().dismiss();
                        } else {
                            mInputCustId.setError(ErrorCodes.appErrorDesc.get(error));
                        }
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

    private EditText mInputCustId;
    private AppCompatButton mGetCustData;

    private void initUiResources(View v) {
        mInputCustId = (EditText) v.findViewById(R.id.input_cust_id);
        mGetCustData = (AppCompatButton) v.findViewById(R.id.btn_cust_data);
    }
}

