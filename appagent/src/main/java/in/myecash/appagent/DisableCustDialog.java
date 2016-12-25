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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.ValidationHelper;
import in.myecash.common.MyGlobalSettings;
import in.myecash.common.constants.ErrorCodes;

/**
 * Created by adgangwa on 29-11-2016.
 */
public class DisableCustDialog extends DialogFragment
        implements DialogInterface.OnClickListener, AdapterView.OnItemSelectedListener {
    public static final String TAG = "AgentApp-DisableCustDialog";
    private static final String ARG_ACTION = "argAction";

    boolean isLtdMode;
    private String reasonStr;
    private DisableCustDialogIf mListener;

    public interface DisableCustDialogIf {
        void disableCustomer(boolean isLtdMode, String ticketId, String reason, String remarks);
    }

    public static DisableCustDialog getInstance(boolean isLtdMode) {
        Bundle args = new Bundle();
        args.putBoolean(ARG_ACTION, isLtdMode);

        DisableCustDialog dialog = new DisableCustDialog();
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mListener = (DisableCustDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement DisableCustDialogIf");
        }

        isLtdMode = getArguments().getBoolean(ARG_ACTION);
        if(isLtdMode) {
            mTitle.setText("Customer: Limited Mode");
            String msg1 = "Customer account will be 'enabled' automatically after "+ MyGlobalSettings.getCustAccLimitModeHrs()+" hours. Only Credit transactions are allowed in 'Limited Mode'.";
            mInfo1.setText(msg1);
            mInfo2.setText("Are you sure to put account in LIMITED Mode ?");
        }

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.cust_disable_reasons_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mReason.setAdapter(adapter);

        mReason.setOnItemSelectedListener(this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateDialog");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_disable_cust, null);
        initUiResources(v);

        // return new dialog
        final AlertDialog alertDialog =  new AlertDialog.Builder(getActivity()).setView(v)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppCommonUtil.hideKeyboard(getDialog());
                        dialog.dismiss();
                    }
                })
                .create();
        alertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(DisableCustDialog.this, (AlertDialog) dialog);

                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
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
                            mListener.disableCustomer(isLtdMode, ticketId, reasonStr, remarks);
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
    public void onClick(DialogInterface dialog, int which) {
        //Do nothing here because we override this button in OnShowListener to change the close behaviour.
        //However, we still need this because on older versions of Android unless we
        //pass a handler the button doesn't get instantiated
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
    }

    private EditText mTitle;
    private EditText mInfo1;
    private EditText mInfo2;

    private EditText mTicketNum;
    private Spinner mReason;
    private EditText mRemarks;

    private void initUiResources(View v) {
        mTitle = (EditText) v.findViewById(R.id.label_cash_pay_title);
        mInfo1 = (EditText) v.findViewById(R.id.label_information_1);
        mInfo2 = (EditText) v.findViewById(R.id.label_information_2);

        mTicketNum = (EditText) v.findViewById(R.id.input_ticketId);
        mReason = (Spinner) v.findViewById(R.id.input_reason);
        mRemarks = (EditText) v.findViewById(R.id.input_remarks);
    }

}


