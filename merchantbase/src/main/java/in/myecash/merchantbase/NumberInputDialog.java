package in.myecash.merchantbase;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;


/**
 * Created by adgangwa on 28-03-2016.
 */
public class NumberInputDialog extends DialogFragment implements View.OnClickListener {
    public static final String TAG = "InputNumberDialog";

    public static final String EXTRA_INPUT_HUMBER = "cashPaid";
    private static final String ARG_LABEL = "label";
    private static final String ARG_IS_AMOUNT = "isAmount";
    private static final String ARG_CUR_VALUE = "curValue";
    private static final String ARG_MAX_VALUE = "maxValue";

    private int maxValue;

    public static NumberInputDialog newInstance(String label, String curValue, Boolean isAmount, int maxValue) {
        LogMy.d(TAG, "Creating new input number dialog");
        Bundle args = new Bundle();
        args.putString(ARG_LABEL, label);
        args.putBoolean(ARG_IS_AMOUNT, isAmount);
        args.putString(ARG_CUR_VALUE, curValue);
        args.putInt(ARG_MAX_VALUE, maxValue);

        NumberInputDialog fragment = new NumberInputDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateDialog");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_num_input, null);
        initUiResources(v);
        setListeners();

        // Initialize view with values
        String label = getArguments().getString(ARG_LABEL);
        Boolean isAmount = getArguments().getBoolean(ARG_IS_AMOUNT);
        String curValue = getArguments().getString(ARG_CUR_VALUE);
        maxValue = getArguments().getInt(ARG_MAX_VALUE);

        mLabelInputNum.setText(label);
        if(isAmount) {
            mRsInputNum.setVisibility(View.VISIBLE);
        } else {
            mRsInputNum.setVisibility(View.GONE);
        }
        //mInputNum.setHint(curValue);
        if(curValue == null || curValue.isEmpty()) {
            mInputOldValue.setVisibility(View.GONE);
        } else {
            mInputOldValue.setVisibility(View.VISIBLE);
            mInputOldValue.setText(curValue);
            mInputOldValue.setPaintFlags(mInputOldValue.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        mInputNum.requestFocus();

        // return new dialog
        Dialog dialog =  new AlertDialog.Builder(getActivity(), R.style.WrapEverythingDialog)
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
                }).create();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(NumberInputDialog.this, (AlertDialog) dialog);
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
                    Boolean wantToCloseDialog = true;

                    LogMy.d(TAG, "Clicked Ok");
                    String curStr = mInputNum.getText().toString();
                    if (curStr.isEmpty()) {
                        sendResult(Activity.RESULT_OK, mInputOldValue.getText().toString());
                    } else {
                        int curValue = Integer.parseInt(curStr);
                        if(maxValue > 0 && curValue > maxValue) {
                            wantToCloseDialog = false;
                            mInputNum.setError("Cannot be more than: "+ AppCommonUtil.getAmtStr(maxValue));
                        } else {
                            sendResult(Activity.RESULT_OK, curStr);
                        }
                    }

                    if (wantToCloseDialog)
                        d.dismiss();
                    //else dialog stays open. Make sure you have an obvious way to close the dialog especially if you set cancellable to false.
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        int vId = v.getId();
        LogMy.d(TAG,"In onClick: "+vId);

        String curStr = mInputNum.getText().toString();

        if (vId == R.id.input_kb_bs) {
            if (curStr.length() > 0) {
                mInputNum.setText("");
                mInputNum.append(curStr, 0, (curStr.length() - 1));
            }

        } else if (vId == R.id.input_kb_clear) {
            mInputNum.setText("");

        } else if (vId == R.id.input_kb_0 || vId == R.id.input_kb_1 || vId == R.id.input_kb_2 || vId == R.id.input_kb_3 || vId == R.id.input_kb_4 || vId == R.id.input_kb_5 || vId == R.id.input_kb_6 || vId == R.id.input_kb_7 || vId == R.id.input_kb_8 || vId == R.id.input_kb_9) {
            AppCompatButton key = (AppCompatButton) v;
            // ignore 0 as first entered digit
            if (!(v.getId() == R.id.input_kb_0 && curStr.isEmpty())) {
                mInputNum.append(key.getText());
            }


            /*
            case R.id.rs_input_number:
            case R.id.input_number:
                LogMy.d(TAG,"Clicked input number");
                if(curStr.isEmpty()) {
                    sendResult(Activity.RESULT_OK, mInputNum.getHint().toString());
                } else {
                    sendResult(Activity.RESULT_OK, curStr);
                }
                getDialog().dismiss();
                break;*/
        }
    }

    private void sendResult(int resultCode, String number) {
        LogMy.d(TAG,"In sendResult");
        if (getTargetFragment() == null) {
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(EXTRA_INPUT_HUMBER, number);
        getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, intent);
    }

    private void setListeners() {
        LogMy.d(TAG, "In setListeners");
        //mInputNum.setOnClickListener(this);
        //mRsInputNum.setOnClickListener(this);

        initKeyboard();

        mInputNum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppCommonUtil.hideKeyboard(getDialog());
            }
        });

        mInputNum.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                AppCommonUtil.hideKeyboard(getDialog());
            }
        });
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

    private AppCompatButton mKey1;
    private AppCompatButton mKey2;
    private AppCompatButton mKey3;
    private AppCompatButton mKey4;
    private AppCompatButton mKey5;
    private AppCompatButton mKey6;
    private AppCompatButton mKey7;
    private AppCompatButton mKey8;
    private AppCompatButton mKey9;
    private AppCompatButton mKey0;
    private AppCompatButton mKeyClear;
    private AppCompatImageButton mKeyBspace;

    private EditText mLabelInputNum;
    private EditText mRsInputNum;
    private EditText mInputNum;
    private EditText mInputOldValue;

    //private LinearLayout mRootLayout;

    private void initUiResources(View v) {
        mLabelInputNum = (EditText) v.findViewById(R.id.label_input_number);
        mRsInputNum = (EditText) v.findViewById(R.id.rs_input_number);
        mInputNum = (EditText) v.findViewById(R.id.input_number);
        mInputOldValue = (EditText) v.findViewById(R.id.input_old_value);

        mKey1 = (AppCompatButton) v.findViewById(R.id.input_kb_1);
        mKey2 = (AppCompatButton) v.findViewById(R.id.input_kb_2);
        mKey3 = (AppCompatButton) v.findViewById(R.id.input_kb_3);
        mKey4 = (AppCompatButton) v.findViewById(R.id.input_kb_4);
        mKey5 = (AppCompatButton) v.findViewById(R.id.input_kb_5);
        mKey6 = (AppCompatButton) v.findViewById(R.id.input_kb_6);
        mKey7 = (AppCompatButton) v.findViewById(R.id.input_kb_7);
        mKey8 = (AppCompatButton) v.findViewById(R.id.input_kb_8);
        mKey9 = (AppCompatButton) v.findViewById(R.id.input_kb_9);
        mKey0 = (AppCompatButton) v.findViewById(R.id.input_kb_0);
        mKeyBspace = (AppCompatImageButton) v.findViewById(R.id.input_kb_bs);
        mKeyClear = (AppCompatButton) v.findViewById(R.id.input_kb_clear);

        //mRootLayout = (LinearLayout)v.findViewById(R.id.layout_root_dialog);
    }

    /*
    @Override
    public void show(FragmentManager manager, String tag) {
        super.show(manager, tag);
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public int show(FragmentTransaction transaction, String tag) {
        int retValue = super.show(transaction, tag);
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        return retValue;
    }*/

 /*
    @Override
    public void onStart() {
        // This MUST be called first! Otherwise the view tweaking will not be present in the displayed Dialog (most likely overriden)
        super.onStart();
        //forceWrapContent(mKey1);
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }


    protected void forceWrapContent(View v) {
        // Start with the provided view
        View current = v;

        // Travel up the tree until fail, modifying the LayoutParams
        do {
            // Get the parent
            ViewParent parent = current.getParent();

            // Check if the parent exists
            if (parent != null) {
                // Get the view
                try {
                    current = (View) parent;
                } catch (ClassCastException e) {
                    // This will happen when at the top view, it cannot be cast to a View
                    break;
                }

                // Modify the layout
                current.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
            }
        } while (current.getParent() != null);

        // Request a layout to be re-done
        current.requestLayout();
    }*/
}
