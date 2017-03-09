package in.myecash.appagent;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

import in.myecash.appagent.helper.MyRetainedFragment;
import in.myecash.appbase.BaseFragment;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.ValidationHelper;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;

/**
 * Created by adgangwa on 05-03-2017.
 */

public class SearchMchntOrderFrag extends BaseFragment {
    public static final String TAG = "AgentApp-SearchMchntOrderFrag";

    private SearchMchntOrderFragIf mListener;
    CompoundButton.OnCheckedChangeListener cboxListener;
    MyRetainedFragment mRetainedFrag;

    public interface SearchMchntOrderFragIf {
        MyRetainedFragment getRetainedFragment();
        void searchMchntOrder();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mListener = (SearchMchntOrderFragIf) getActivity();
            mRetainedFrag = mListener.getRetainedFragment();
            setListeners();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement SearchMchntOrderFragIf");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateView");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.frag_search_mchnt_order, null);
        initUiResources(v);
        return v;
    }

    private void setListeners() {

        cboxListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MyRetainedFragment retainedFrag = mListener.getRetainedFragment();

                DbConstants.MCHNT_ORDER_STATUS effectedStatus;
                switch (buttonView.getId()) {
                    case R.id.cb_new:
                        effectedStatus = DbConstants.MCHNT_ORDER_STATUS.New;
                        break;
                    case R.id.cb_inProcess:
                        effectedStatus = DbConstants.MCHNT_ORDER_STATUS.InProcess;
                        break;
                    case R.id.cb_shipped:
                        effectedStatus = DbConstants.MCHNT_ORDER_STATUS.Shipped;
                        break;
                    case R.id.cb_complete:
                        effectedStatus = DbConstants.MCHNT_ORDER_STATUS.Completed;
                        break;
                    case R.id.cb_reject:
                        effectedStatus = DbConstants.MCHNT_ORDER_STATUS.Rejected;
                        break;
                    case R.id.cb_pvp:
                        effectedStatus = DbConstants.MCHNT_ORDER_STATUS.PaymentVerifyPending;
                        break;
                    case R.id.cb_payFailed:
                        effectedStatus = DbConstants.MCHNT_ORDER_STATUS.PaymentFailed;
                        break;
                    default:
                        effectedStatus = null;
                }

                if(effectedStatus != null) {
                    if(isChecked) {
                        retainedFrag.mSelectedStatus.add(effectedStatus);
                    } else {
                        retainedFrag.mSelectedStatus.remove(effectedStatus);
                    }
                }
            }
        };
        mCbNew.setOnCheckedChangeListener(cboxListener);
        mCbInProcess.setOnCheckedChangeListener(cboxListener);
        mCbShipped.setOnCheckedChangeListener(cboxListener);
        mCbCompleted.setOnCheckedChangeListener(cboxListener);
        mCbRejected.setOnCheckedChangeListener(cboxListener);
        mCbPVP.setOnCheckedChangeListener(cboxListener);
        mCbPayFailed.setOnCheckedChangeListener(cboxListener);

        mSearch.setOnClickListener(this);
    }

    @Override
    public void handleBtnClick(View v) {
        if(v.getId()==mSearch.getId()) {

            int errCode = ErrorCodes.NO_ERROR;
            String mchntId = mInputMchntId.getText().toString();
            String orderId = mInputOrderId.getText().toString();

            if(!mchntId.isEmpty()) {
                errCode = ValidationHelper.validateMerchantId(mchntId);
                if (errCode != ErrorCodes.NO_ERROR) {
                    mInputMchntId.setError(AppCommonUtil.getErrorDesc(errCode));
                } else {
                    mRetainedFrag.mMchntIdForOrder = mchntId;
                }
            }
            if(!orderId.isEmpty()) {
                errCode = ValidationHelper.validateMchntOrderId(orderId);
                if (errCode != ErrorCodes.NO_ERROR) {
                    mInputOrderId.setError(AppCommonUtil.getErrorDesc(errCode));
                } else {
                    mRetainedFrag.mMchntOrderId = orderId;
                }
            }

            if(errCode==ErrorCodes.NO_ERROR) {
                mListener.searchMchntOrder();
            }
        }
    }

    private EditText mInputOrderId;
    private EditText mInputMchntId;
    private AppCompatButton mSearch;

    private AppCompatCheckBox mCbNew;
    private AppCompatCheckBox mCbInProcess;
    private AppCompatCheckBox mCbShipped;
    private AppCompatCheckBox mCbCompleted;
    private AppCompatCheckBox mCbRejected;
    private AppCompatCheckBox mCbPVP;
    private AppCompatCheckBox mCbPayFailed;


    private void initUiResources(View v) {
        mInputOrderId = (EditText) v.findViewById(R.id.input_orderid);
        mInputMchntId = (EditText) v.findViewById(R.id.input_merchantid);
        mSearch = (AppCompatButton) v.findViewById(R.id.btn_search);

        mCbNew = (AppCompatCheckBox)v.findViewById(R.id.cb_new);
        mCbInProcess = (AppCompatCheckBox)v.findViewById(R.id.cb_inProcess);
        mCbShipped = (AppCompatCheckBox)v.findViewById(R.id.cb_shipped);
        mCbCompleted = (AppCompatCheckBox)v.findViewById(R.id.cb_complete);
        mCbRejected = (AppCompatCheckBox)v.findViewById(R.id.cb_reject);
        mCbPVP = (AppCompatCheckBox)v.findViewById(R.id.cb_pvp);
        mCbPayFailed = (AppCompatCheckBox)v.findViewById(R.id.cb_payFailed);
    }

    @Override
    public void onResume() {
        super.onResume();
        mListener.getRetainedFragment().setResumeOk(true);
    }

    @Override
    public boolean handleTouchUp(View v) {
        // do nothing
        return false;
    }
}


