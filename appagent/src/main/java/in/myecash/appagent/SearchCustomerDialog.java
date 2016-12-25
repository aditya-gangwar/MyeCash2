package in.myecash.appagent;

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

import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.ValidationHelper;
import in.myecash.common.constants.ErrorCodes;

/**
 * Created by adgangwa on 29-11-2016.
 */
public class SearchCustomerDialog extends DialogFragment implements DialogInterface.OnClickListener {
    public static final String TAG = "AgentApp-SearchCustomerDialog";

    private SearchCustomerDialogIf mListener;

    public interface SearchCustomerDialogIf {
        void onCustInputData(String value, boolean searchById);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mListener = (SearchCustomerDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement SearchCustomerDialogIf");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateDialog");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_search_customer, null);
        initUiResources(v);

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
                AppCommonUtil.setDialogTextSize(SearchCustomerDialog.this, (AlertDialog) dialog);

                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AppCommonUtil.hideKeyboard(getDialog());
                        String id = mInputId.getText().toString();
                        String mobileNum = mInputMobileNum.getText().toString();

                        int error = ErrorCodes.NO_ERROR;

                        if(!id.isEmpty()) {
                            error = ValidationHelper.validateCustInternalId(id);
                            if(error == ErrorCodes.NO_ERROR) {
                                mListener.onCustInputData(id,true);
                                getDialog().dismiss();
                            } else {
                                mInputId.setError(AppCommonUtil.getErrorDesc(error));
                            }

                        } else if(!mobileNum.isEmpty()) {
                            error = ValidationHelper.validateMobileNo(mobileNum);
                            if(error == ErrorCodes.NO_ERROR) {
                                mListener.onCustInputData(mobileNum,false);
                                getDialog().dismiss();
                            } else {
                                mInputMobileNum.setError(AppCommonUtil.getErrorDesc(error));
                            }
                        }
                    }
                });
            }
        });

        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
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

    private EditText mInputId;
    private EditText mInputMobileNum;
    private void initUiResources(View v) {
        mInputMobileNum = (EditText) v.findViewById(R.id.input_mobile_num);
        mInputId = (EditText) v.findViewById(R.id.input_id);
    }
}

