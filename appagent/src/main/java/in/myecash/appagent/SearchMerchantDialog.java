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

import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.LogMy;
import in.myecash.commonbase.utilities.ValidationHelper;

/**
 * Created by adgangwa on 11-07-2016.
 */
public class SearchMerchantDialog extends DialogFragment implements DialogInterface.OnClickListener {
    public static final String TAG = "SearchMerchantDialog";

    private SearchMerchantDialogIf mListener;

    public interface SearchMerchantDialogIf {
        void onInputData(String value, boolean searchById);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mListener = (SearchMerchantDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement SearchMerchantDialogIf");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateDialog");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_search_merchant, null);
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
                AppCommonUtil.setDialogTextSize(SearchMerchantDialog.this, (AlertDialog) dialog);

                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AppCommonUtil.hideKeyboard(getDialog());
                        String id = mInputId.getText().toString();
                        String mobileNum = mInputMobileNum.getText().toString();

                        int error = ErrorCodes.NO_ERROR;

                        if(!id.isEmpty()) {
                            error = ValidationHelper.validateMerchantId(id);
                            if(error == ErrorCodes.NO_ERROR) {
                                mListener.onInputData(id,true);
                                getDialog().dismiss();
                            } else {
                                mInputId.setError(ErrorCodes.appErrorDesc.get(error));
                            }

                        } else if(!mobileNum.isEmpty()) {
                            error = ValidationHelper.validateMobileNo(mobileNum);
                            if(error == ErrorCodes.NO_ERROR) {
                                mListener.onInputData(id,false);
                                getDialog().dismiss();
                            } else {
                                mInputMobileNum.setError(ErrorCodes.appErrorDesc.get(error));
                            }
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

    private EditText mInputId;
    private EditText mInputMobileNum;
    private void initUiResources(View v) {
        mInputMobileNum = (EditText) v.findViewById(R.id.input_mobile_num);
        mInputId = (EditText) v.findViewById(R.id.input_id);
    }
}

