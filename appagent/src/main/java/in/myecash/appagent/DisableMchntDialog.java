package in.myecash.appagent;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
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

import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.LogMy;
import in.myecash.commonbase.utilities.ValidationHelper;

/**
 * Created by adgangwa on 20-09-2016.
 */
public class DisableMchntDialog extends DialogFragment
        implements DialogInterface.OnClickListener, AdapterView.OnItemSelectedListener {
    public static final String TAG = "DisableMchntDialog";

    public static final String EXTRA_TICKET_ID = "ticketId";
    public static final String EXTRA_REASON = "reason";
    public static final String EXTRA_REMARKS = "remarks";


    private String reasonStr;
    private DisableMchntDialogIf mListener;

    public interface DisableMchntDialogIf {
        void disableMerchant(String ticketId, String reason, String remarks);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mListener = (DisableMchntDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement DisableMchntDialogIf");
        }

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.acc_status_reasons_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mReason.setAdapter(adapter);

        mReason.setOnItemSelectedListener(this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateDialog");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_disable_acc, null);
        initUiResources(v);

        // return new dialog
        final AlertDialog alertDialog =  new AlertDialog.Builder(getActivity()).setView(v)
                .setPositiveButton(in.myecash.merchantbase.R.string.ok, this)
                .setNegativeButton(in.myecash.merchantbase.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppCommonUtil.hideKeyboard(getDialog());
                        //mListener.onPasswdResetData(null);
                        dialog.dismiss();
                    }
                })
                .create();
        alertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(DisableMchntDialog.this, (AlertDialog) dialog);

                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AppCommonUtil.hideKeyboard(getDialog());

                        boolean allOk = true;
                        String ticketId = mTicketNum.getText().toString();
                        int error = ValidationHelper.validateTicketNum(ticketId);
                        if(error != ErrorCodes.NO_ERROR) {
                            mTicketNum.setError(ErrorCodes.appErrorDesc.get(error));
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
                            mListener.disableMerchant(ticketId, reasonStr, remarks);
                            //sendResult(Activity.RESULT_OK, ticketId, reasonStr, remarks);
                            getDialog().dismiss();
                        }
                    }
                });
            }
        });

        alertDialog.setCanceledOnTouchOutside(false);
        return alertDialog;
    }

    /*
    private void sendResult(int resultCode, String ticketId, String reason, String remarks) {
        LogMy.d(TAG,"In sendResult");
        if (getTargetFragment() == null) {
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(EXTRA_TICKET_ID, ticketId);
        intent.putExtra(EXTRA_REASON, reason);
        intent.putExtra(EXTRA_REMARKS, remarks);
        getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, intent);
    }*/

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
        //mListener.onPasswdResetData(null);
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

