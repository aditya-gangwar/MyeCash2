package in.myecash.internal;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import in.myecash.appbase.BaseDialog;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.OnSingleClickListener;
import in.myecash.appbase.utilities.ValidationHelper;
import in.myecash.common.constants.ErrorCodes;

/**
 * Created by adgangwa on 15-12-2016.
 */
public class CustLimitedDialog extends BaseDialog
        implements AdapterView.OnItemSelectedListener {
    public static final String TAG = "AgentApp-CustLimitedDialog";

    private String reasonStr;
    private CustLimitedDialogIf mListener;

    public interface CustLimitedDialogIf {
        void disableCustomer(String ticketId, String reason, String remarks);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mListener = (CustLimitedDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement CustLimitedDialogIf");
        }

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.cust_limited_reasons_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mReason.setAdapter(adapter);

        mReason.setOnItemSelectedListener(this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateDialog");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_cust_limited_mode, null);
        initUiResources(v);

        // return new dialog
        final AlertDialog alertDialog =  new AlertDialog.Builder(getActivity()).setView(v)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, this)
                .create();
        alertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(CustLimitedDialog.this, (AlertDialog) dialog);

                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new OnSingleClickListener() {
                    @Override
                    public void onSingleClick(View v) {
                        AppCommonUtil.hideKeyboard(getDialog());

                        boolean allOk = true;
                        String ticketId = mTicketNum.getText().toString();
                        int error = ValidationHelper.validateTicketNum(ticketId);
                        if(error != ErrorCodes.NO_ERROR) {
                            mTicketNum.setError(AppCommonUtil.getErrorDesc(error));
                            allOk = false;
                        }

                        String remarks = mRemarks.getText().toString();
                        // 'None' and 'Other' strings should be exactly same
                        // as defined in used array in strings.xml
                        if(reasonStr==null || reasonStr.equals("None")) {
                            AppCommonUtil.toast(getActivity(), "Reason value not set");
                            allOk = false;
                        } else if( reasonStr.equals("Other") && remarks.isEmpty() ) {
                            AppCommonUtil.toast(getActivity(), "Other reason value not provided");
                            allOk = false;
                        }

                        if(allOk) {
                            mListener.disableCustomer(ticketId, reasonStr, remarks);
                            getDialog().dismiss();
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
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        reasonStr = (String)parent.getItemAtPosition(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        reasonStr = null;
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
                AppCommonUtil.hideKeyboard(getDialog());
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

    private EditText mTicketNum;
    private Spinner mReason;
    private EditText mRemarks;

    private void initUiResources(View v) {
        mTicketNum = (EditText) v.findViewById(R.id.input_ticketId);
        mReason = (Spinner) v.findViewById(R.id.input_reason);
        mRemarks = (EditText) v.findViewById(R.id.input_remarks);
    }

}

