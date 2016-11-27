package in.myecash.merchantbase;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.appbase.utilities.LogMy;

/**
 * Created by adgangwa on 28-02-2016.
 */
public class MobileNumberFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "MobileNumberFragment";

    private static final String MOBILE_NUM_EMPTY_CHAR = ".";

    private MobileFragmentIf mCallback;
    private StringBuffer mStrDots = new StringBuffer(CommonConstants.MOBILE_NUM_LENGTH);

    // Container Activity must implement this interface
    public interface MobileFragmentIf {
        public void onMobileNumInput(String mobileNum);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        LogMy.d(TAG, "In onActivityCreated");
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (MobileFragmentIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement MobileFragmentIf");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateView");
        View v = inflater.inflate(R.layout.fragment_mobile_number, container, false);

        // access to UI elements
        bindUiResources(v);
        // setup keyboard listeners
        initKeyboard();
        //setup buttons
        initButtons();

        return v;
    }

    @Override
    public void onClick(View v) {
        // process the keys
        String curStr = mInputCustMobile.getText().toString();
        // remove dots
        curStr = curStr.replace(MOBILE_NUM_EMPTY_CHAR,"");
        String newStr="";

        if(v.getId() == R.id.input_kb_bs) {
            if(curStr.length()>0) {
                //mInputCustMobile.setText("");
                //mInputCustMobile.append(curStr,0,(curStr.length()-1));
                newStr = curStr.substring(0,(curStr.length()-1));
            }
        } else if(v.getId() == R.id.input_kb_clear) {
            //mInputCustMobile.setText("");
            newStr="";
        } else {
            Button key = (Button)v;
            // ignore 0 as first entered digit
            if( !(v.getId()== R.id.input_kb_0 && curStr.isEmpty())) {
                //mInputCustMobile.append(key.getText());
                newStr = curStr+key.getText();
            }
        }

        // Add dots for remaining length
        //String strWithoutDots = mInputCustMobile.getText().toString();
        if(newStr.length() > 0) {
            mStrDots.delete(0, mStrDots.length());
            for(int i=newStr.length(); i<CommonConstants.MOBILE_NUM_LENGTH; i++) {
                mStrDots.append(MOBILE_NUM_EMPTY_CHAR);
            }
            String finalStr = newStr+mStrDots.toString();
            //String finalStr = newStr;
            mInputCustMobile.setText(finalStr);
        } else {
            mInputCustMobile.setText(newStr);
        }
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

    private void initButtons() {
        mBtnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mobileNum = mInputCustMobile.getText().toString();
                LogMy.d(TAG, "Clicked process: "+mobileNum);

                // If less then MOBILE_NUM_PROCESS_MIN_LENGTH, attempt QR code scan
                if(mobileNum.length() > AppConstants.MOBILE_NUM_PROCESS_MIN_LENGTH &&
                        mobileNum.length() != CommonConstants.MOBILE_NUM_LENGTH ) {
                    mInputCustMobile.setError(AppCommonUtil.getErrorDesc(ErrorCodes.INVALID_LENGTH));
                    return;
                }
                mCallback.onMobileNumInput(mobileNum);
            }
        });

        mInputCustMobile.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    LogMy.d(TAG, "Clicked skip");
                    mCallback.onMobileNumInput(null);
                }
                return true;
            }
        });
    }

    /*
    public void reset() {
        mInputCustMobile.setText("");
    }*/

    @Override
    public void onResume() {
        LogMy.d(TAG, "In onResume");
        super.onResume();
        mInputCustMobile.setText("");
    }

    private EditText mInputCustMobile;
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
    private AppCompatButton mBtnProcess;

    private void bindUiResources(View v) {
        mInputCustMobile = (EditText) v.findViewById(R.id.input_cust_mobile);
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

        mBtnProcess = (AppCompatButton) v.findViewById(R.id.btn_process);
    }
}
