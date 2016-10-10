package in.myecash.appbase;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import in.myecash.common.constants.ErrorCodes;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.ValidationHelper;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created by adgangwa on 30-04-2016.
 */
public class OtpPinInputDialog extends DialogFragment
        implements View.OnClickListener {

    private static final String TAG = "OtpInputDialog";
    private static final String ARG_TITLE = "title";
    private static final String ARG_INFO = "info";
    private static final String ARG_HINT = "hint";

    private static final Integer[] keys = {0,1,2,3,4,5,6,7,8,9};

    private OtpPinInputDialogIf mCallback;
    //private Boolean isPinCase;

    public interface OtpPinInputDialogIf {
        //public RetainedFragment getRetainedFragment();
        void onPinOtp(String pinOrOtp, String tag);
    }

    public static OtpPinInputDialog newInstance(String title, String info, String hint) {
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_INFO, info);
        args.putString(ARG_HINT, hint);

        OtpPinInputDialog fragment = new OtpPinInputDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (OtpPinInputDialogIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement OtpPinInputDialogIf");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_otp_pin_input, null);

        bindUiResources(v);

        // set values
        String title = getArguments().getString(ARG_TITLE);
        String info = getArguments().getString(ARG_INFO);
        String hint = getArguments().getString(ARG_HINT);

        mLabelTitle.setText(title);
        mLabelInfo.setText(info);
        mInputPinOtp.setHint(hint);

        initKeyboard();

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
                })
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(OtpPinInputDialog.this, (AlertDialog) dialog);
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
                    String pinOrOtp = mInputPinOtp.getText().toString();

                    int errorCode = ValidationHelper.validatePin(pinOrOtp);
                    if(errorCode == ErrorCodes.NO_ERROR) {
                        mCallback.onPinOtp(pinOrOtp, getTag());
                    } else {
                        // if PIN validation failed - then it may be Otp case
                        errorCode = ValidationHelper.validateOtp(pinOrOtp);
                        if(errorCode == ErrorCodes.NO_ERROR) {
                            mCallback.onPinOtp(pinOrOtp, getTag());
                        } else {
                            mInputPinOtp.setError(AppCommonUtil.getErrorDesc(errorCode));
                            wantToCloseDialog = false;
                        }
                    }

                    if (wantToCloseDialog)
                        d.dismiss();
                    //else dialog stays open. Make sure you have an obvious way to close the dialog especially if you set cancellable to false.
                }
            });
        }
    }

    /*
    @Override
    public void onClick(DialogInterface dialog, int which) {

        String pinOrOtp = mInputPinOtp.getText().toString();

        int errorCode = ValidationHelper.validatePinOtp(pinOrOtp);
        if( errorCode == ErrorCodes.NO_ERROR) {
            mCallback.onPinOtp(pinOrOtp, getTag());
            dialog.dismiss();
        } else {
            mInputPinOtp.setError(AppCommonUtil.getErrorDesc(errorCode));
        }
    }*/

    @Override
    public void onClick(View v) {
        int vId = v.getId();
        LogMy.d(TAG,"In onClick: "+vId);

        String curStr = mInputPinOtp.getText().toString();

        if (vId == R.id.input_kb_bs) {
            if (curStr.length() > 0) {
                mInputPinOtp.setText("");
                mInputPinOtp.append(curStr, 0, (curStr.length() - 1));
            }

        } else if (vId == R.id.input_kb_clear) {
            mInputPinOtp.setText("");

        } else if (vId == R.id.input_kb_0 || vId == R.id.input_kb_1 || vId == R.id.input_kb_2 || vId == R.id.input_kb_3 || vId == R.id.input_kb_4 || vId == R.id.input_kb_5 || vId == R.id.input_kb_6 || vId == R.id.input_kb_7 || vId == R.id.input_kb_8 || vId == R.id.input_kb_9) {
            AppCompatButton key = (AppCompatButton) v;
            mInputPinOtp.append(key.getText());

        }
    }

    private EditText mLabelTitle;
    private EditText mLabelInfo;
    private EditText mInputPinOtp;

    private AppCompatButton mKeys[];
    private AppCompatButton mKeyClear;
    private AppCompatImageButton mKeyBspace;

    private void bindUiResources(View v) {
        mLabelTitle = (EditText) v.findViewById(R.id.label_title);
        mLabelInfo = (EditText) v.findViewById(R.id.label_info);
        mInputPinOtp = (EditText) v.findViewById(R.id.input_pin_otp);

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
        mKeyClear = (AppCompatButton) v.findViewById(R.id.input_kb_clear);
    }

    private void initKeyboard() {

        // set text randomly for the keys
        Collections.shuffle(Arrays.asList(keys));

        for(int i=0; i<10; i++) {
            mKeys[i].setOnClickListener(this);
            mKeys[i].setText(String.valueOf(keys[i]));
        }

        mKeyBspace.setOnClickListener(this);
        mKeyClear.setOnClickListener(this);
    }
}

