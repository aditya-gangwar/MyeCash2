package in.myecash.appagent;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.ArrayList;

import in.myecash.appagent.helper.MyBackgroundProcessor;
import in.myecash.appagent.helper.MyRetainedFragment;
import in.myecash.appbase.BaseFragment;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.entities.MyBusinessCategories;
import in.myecash.appbase.entities.MyCities;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.ValidationHelper;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.Cities;
import in.myecash.common.database.MerchantOrders;
import in.myecash.merchantbase.CustomerOpDialog;

/**
 * Created by adgangwa on 08-03-2017.
 */

public class OrderStatusChangeFrag extends BaseFragment {
    public static final String TAG = "AgentApp-OrderStatusChangeFrag";

    private static final String TXT_INV_NUM = "Invoice#";
    private static final String TXT_INV_URL = "Invoice URL";
    private static final String TXT_AGENT_ID = "Agent ID";
    private static final String TXT_AGENT_NAME = "Agent Name";
    private static final String TXT_PAY_MODE = "Payment Mode";
    private static final String TXT_PAY_REF_ID = "Pay Reference ID";

    private static final String DIALOG_PAYMODE = "DialogPaymode";
    private static final int REQUEST_PAYMODE = 1;

    private OrderStatusChangeFragIf mListener;
    MerchantOrders mOrder;
    DbConstants.MCHNT_ORDER_STATUS newStatus;
    MerchantOrders updatedOrder;
    View.OnClickListener mClickListener;

    public interface OrderStatusChangeFragIf {
        MyRetainedFragment getRetainedFragment();
        void changeOrderStatus(MerchantOrders updatedOrder);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mListener = (OrderStatusChangeFragIf) getActivity();
            mOrder = mListener.getRetainedFragment().mCurrOrder;
            initListeners();
            initView();

        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement OrderStatusChangeFragIf");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateView");

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.frag_order_status, null);
        initUiResources(v);
        return v;
    }

    private void initView() {
        mInputOrderId.setText(mOrder.getOrderId());
        mInputMchntId.setText(mOrder.getMerchantId());
        mCurrStatus.setText(mOrder.getStatus());

        mRadio1.setVisibility(View.INVISIBLE);
        mRadio2.setVisibility(View.INVISIBLE);
        mRadio3.setVisibility(View.INVISIBLE);
        mRadio4.setVisibility(View.INVISIBLE);

        DbConstants.MCHNT_ORDER_STATUS status = DbConstants.MCHNT_ORDER_STATUS.valueOf(mOrder.getStatus());
        switch (status) {
            case New:
                AppCommonUtil.toast(getActivity(), "Order in Invalid state");
                break;
            case InProcess:
                mRadio1.setText(DbConstants.MCHNT_ORDER_STATUS.toString(DbConstants.MCHNT_ORDER_STATUS.Shipped));
                mRadio1.setVisibility(View.VISIBLE);
                break;

            case Shipped:
                mRadio1.setText(DbConstants.MCHNT_ORDER_STATUS.toString(DbConstants.MCHNT_ORDER_STATUS.Completed));
                mRadio1.setVisibility(View.VISIBLE);
                mRadio2.setText(DbConstants.MCHNT_ORDER_STATUS.toString(DbConstants.MCHNT_ORDER_STATUS.Rejected));
                mRadio2.setVisibility(View.VISIBLE);
                mRadio3.setText(DbConstants.MCHNT_ORDER_STATUS.toString(DbConstants.MCHNT_ORDER_STATUS.PaymentVerifyPending));
                mRadio3.setVisibility(View.VISIBLE);
                break;
            case PaymentVerifyPending:
                mRadio1.setText(DbConstants.MCHNT_ORDER_STATUS.toString(DbConstants.MCHNT_ORDER_STATUS.Completed));
                mRadio1.setVisibility(View.VISIBLE);
                mRadio2.setText(DbConstants.MCHNT_ORDER_STATUS.toString(DbConstants.MCHNT_ORDER_STATUS.PaymentFailed));
                mRadio2.setVisibility(View.VISIBLE);
                break;
            case Completed:
            case PaymentFailed:
            case Rejected:
                break;
            default:
                break;
        }

        // Show only on selecting new status
        mLabelParams.setVisibility(View.GONE);
        mLayoutPayMode.setVisibility(View.GONE);
        mLayoutValue1.setVisibility(View.GONE);
        mLayoutValue2.setVisibility(View.GONE);
        mLayoutValue3.setVisibility(View.GONE);
        mLayoutValue4.setVisibility(View.GONE);
    }

    private void initListeners() {
        mInputPayMode.setOnTouchListener(this);
        mProcess.setOnClickListener(this);

        mRadio1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mRadio1.setChecked(!mRadio1.isChecked());
                mRadio2.setChecked(false);
                mRadio3.setChecked(false);
                mRadio4.setChecked(false);
                showParams(v);
            }
        });
        mRadio2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRadio1.setChecked(false);
                //mRadio2.setChecked(!mRadio1.isChecked());
                mRadio3.setChecked(false);
                mRadio4.setChecked(false);
                showParams(v);
            }
        });
        mRadio3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRadio1.setChecked(false);
                mRadio2.setChecked(false);
                //mRadio3.setChecked(!mRadio1.isChecked());
                mRadio4.setChecked(false);
                showParams(v);
            }
        });
        mRadio4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRadio1.setChecked(false);
                mRadio2.setChecked(false);
                mRadio3.setChecked(false);
                //mRadio4.setChecked(!mRadio1.isChecked());
                showParams(v);
            }
        });
    }

    private void showParams(View v) {
        RadioButton rb = (RadioButton) mRadioGroup.findViewById(v.getId());
        newStatus = DbConstants.MCHNT_ORDER_STATUS.fromString(rb.getText().toString());

                /*
                in_process -> shipped
                    - Invoice# and URL
                    - Agent ID and Name

                shipped -> completed
                shipped -> pay_verify_pending
                    - delivery Time
                    - delivery agent (if changed)
                    - payment mode and refId

                shipped -> rejected
                    - comments

                pay_verify_pending -> completed
                    - nothing

                pay_verify_pending -> payment_failed
                    - comments
                */
        switch (newStatus) {
            case Shipped:
                mLabelValue1.setText(TXT_INV_NUM);
                mLabelValue2.setText(TXT_INV_URL);
                mLabelValue3.setText(TXT_AGENT_ID);
                //mLabelValue4.setText(TXT_AGENT_NAME);

                mLabelParams.setVisibility(View.VISIBLE);
                mLayoutPayMode.setVisibility(View.GONE);
                mLayoutValue1.setVisibility(View.VISIBLE);
                mLayoutValue2.setVisibility(View.VISIBLE);
                mLayoutValue3.setVisibility(View.VISIBLE);
                mLayoutValue4.setVisibility(View.GONE);
                break;

            case Completed:
                if(mOrder.getStatus().equals(DbConstants.MCHNT_ORDER_STATUS.Shipped.name())) {
                    mLabelValue1.setText(TXT_PAY_REF_ID);
                    mLabelValue2.setText(TXT_AGENT_ID);

                    mLabelParams.setVisibility(View.VISIBLE);
                    mLayoutPayMode.setVisibility(View.VISIBLE);
                    mLayoutValue1.setVisibility(View.VISIBLE);
                    mLayoutValue2.setVisibility(View.VISIBLE);
                    mLayoutValue3.setVisibility(View.GONE);
                    mLayoutValue4.setVisibility(View.GONE);
                } else {
                    mLabelParams.setVisibility(View.GONE);
                    mLayoutPayMode.setVisibility(View.GONE);
                    mLayoutValue1.setVisibility(View.GONE);
                    mLayoutValue2.setVisibility(View.GONE);
                    mLayoutValue3.setVisibility(View.GONE);
                    mLayoutValue4.setVisibility(View.GONE);
                }
                break;

            case PaymentVerifyPending:
                mLabelValue1.setText(TXT_PAY_REF_ID);
                mLabelValue2.setText(TXT_AGENT_ID);

                mLabelParams.setVisibility(View.VISIBLE);
                mLayoutPayMode.setVisibility(View.VISIBLE);
                mLayoutValue1.setVisibility(View.VISIBLE);
                mLayoutValue2.setVisibility(View.VISIBLE);
                mLayoutValue3.setVisibility(View.GONE);
                mLayoutValue4.setVisibility(View.GONE);
                break;

            case Rejected:
            case PaymentFailed:
                mLabelParams.setVisibility(View.GONE);
                mLayoutPayMode.setVisibility(View.GONE);
                mLayoutValue1.setVisibility(View.GONE);
                mLayoutValue2.setVisibility(View.GONE);
                mLayoutValue3.setVisibility(View.GONE);
                mLayoutValue4.setVisibility(View.GONE);
                break;
        }
    }


    private int validate() {
        if(newStatus==null) {
            AppCommonUtil.toast(getActivity(),"Select New Status");
            return ErrorCodes.WRONG_INPUT_DATA;
        }

        int errCode = ErrorCodes.NO_ERROR;
        switch (newStatus) {
            case Shipped:
                if(mInputValue1.getText().toString().isEmpty()) {
                    mInputValue1.setError("Enter Value");
                    errCode = ErrorCodes.EMPTY_VALUE;
                } else if(mInputValue2.getText().toString().isEmpty()) {
                    mInputValue2.setError("Enter Value");
                    errCode = ErrorCodes.EMPTY_VALUE;
                    updatedOrder.setInvoiceUrl(mInputValue2.getText().toString());
                } else {
                    errCode = ValidationHelper.validateAgentId(mInputValue3.getText().toString());
                    if (errCode != ErrorCodes.NO_ERROR) {
                        mInputValue3.setError(AppCommonUtil.getErrorDesc(errCode));
                    }
                }
                break;

            case Completed:
                if(mOrder.getStatus().equals(DbConstants.MCHNT_ORDER_STATUS.Shipped.name())) {
                    if(mInputPayMode.getText().toString().isEmpty()) {
                        mInputPayMode.setError("Enter Value");
                        errCode = ErrorCodes.EMPTY_VALUE;
                    } else if(mInputValue1.getText().toString().isEmpty()) {
                        mInputValue1.setError("Enter Value");
                        errCode = ErrorCodes.EMPTY_VALUE;
                    } else {
                        errCode = ValidationHelper.validateAgentId(mInputValue2.getText().toString());
                        if (errCode != ErrorCodes.NO_ERROR) {
                            mInputValue2.setError(AppCommonUtil.getErrorDesc(errCode));
                        }
                    }
                }
                break;

            case PaymentVerifyPending:
                if(mInputPayMode.getText().toString().isEmpty()) {
                    mInputPayMode.setError("Enter Value");
                    errCode = ErrorCodes.EMPTY_VALUE;
                } else if(mInputValue1.getText().toString().isEmpty()) {
                    mInputValue1.setError("Enter Value");
                    errCode = ErrorCodes.EMPTY_VALUE;
                } else {
                    errCode = ValidationHelper.validateAgentId(mInputValue2.getText().toString());
                    if (errCode != ErrorCodes.NO_ERROR) {
                        mInputValue2.setError(AppCommonUtil.getErrorDesc(errCode));
                    }
                }
                break;

            case Rejected:
            case PaymentFailed:
                if(mComments.getText().toString().isEmpty()) {
                    mComments.setError("Enter Value");
                    errCode = ErrorCodes.EMPTY_VALUE;
                }
                break;
        }

        return errCode;
    }

    @Override
    public void handleBtnClick(View v) {
        if(v.getId()==mProcess.getId()) {
            int errCode = validate();
            if(errCode==ErrorCodes.NO_ERROR) {
                if(updatedOrder==null) {
                    updatedOrder = new MerchantOrders();
                }
                updatedOrder.setOrderId(mOrder.getOrderId());
                updatedOrder.setMerchantId(mOrder.getMerchantId());

                // create updated order object
                updatedOrder.setStatus(newStatus.name());
                switch (newStatus) {
                    case Shipped:
                        updatedOrder.setInvoiceId(mInputValue1.getText().toString());
                        updatedOrder.setInvoiceUrl(mInputValue2.getText().toString());
                        updatedOrder.setAgentId(mInputValue3.getText().toString());
                        break;

                    case Completed:
                        if(mOrder.getStatus().equals(DbConstants.MCHNT_ORDER_STATUS.Shipped.name())) {
                            updatedOrder.setActualPayMode(mInputPayMode.getText().toString());
                            updatedOrder.setPaymentRef(mInputValue1.getText().toString());
                            updatedOrder.setAgentId(mInputValue2.getText().toString());
                        }
                        break;

                    case PaymentVerifyPending:
                        updatedOrder.setActualPayMode(mInputPayMode.getText().toString());
                        updatedOrder.setPaymentRef(mInputValue1.getText().toString());
                        updatedOrder.setAgentId(mInputValue2.getText().toString());
                        break;

                    case Rejected:
                    case PaymentFailed:
                        updatedOrder.setComments(mComments.getText().toString());
                        break;
                }

                updatedOrder.setComments(mComments.getText().toString());
                mListener.changeOrderStatus(updatedOrder);
            } else {
                AppCommonUtil.toast(getActivity(), AppCommonUtil.getErrorDesc(errCode));
            }
        }
    }

    @Override
    public boolean handleTouchUp(View v) {
        if(v.getId()==mInputPayMode.getId()) {
            AppCommonUtil.hideKeyboard(getActivity());
            DialogFragmentWrapper dialog = DialogFragmentWrapper.createSingleChoiceDialog("Payment Mode",
                    DbConstants.MO_PAY_MODE.getValueSet(), -1, true);
            dialog.setTargetFragment(OrderStatusChangeFrag.this,REQUEST_PAYMODE);
            dialog.show(getFragmentManager(), DIALOG_PAYMODE);
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_PAYMODE:
                if(resultCode==ErrorCodes.NO_ERROR) {
                    String payMode = data.getStringExtra(DialogFragmentWrapper.EXTRA_SELECTION);
                    mInputPayMode.setText(payMode);
                    mInputPayMode.setError(null);
                }
                break;
        }
    }

    private EditText mInputOrderId;
    private EditText mInputMchntId;
    private EditText mCurrStatus;

    private RadioGroup mRadioGroup;
    private RadioButton mRadio1;
    private RadioButton mRadio2;
    private RadioButton mRadio3;
    private RadioButton mRadio4;

    private View mLabelParams;

    private View mLayoutPayMode;
    private View mLayoutValue1;
    private View mLayoutValue2;
    private View mLayoutValue3;
    private View mLayoutValue4;

    private EditText mLabelValue1;
    private EditText mLabelValue2;
    private EditText mLabelValue3;
    private EditText mLabelValue4;

    private EditText mInputPayMode;
    private EditText mInputValue1;
    private EditText mInputValue2;
    private EditText mInputValue3;
    private EditText mInputValue4;

    private EditText mComments;
    private AppCompatButton mProcess;

    private void initUiResources(View v) {
        mInputOrderId = (EditText) v.findViewById(R.id.input_orderId);
        mInputMchntId = (EditText) v.findViewById(R.id.input_merchantId);
        mCurrStatus = (EditText) v.findViewById(R.id.input_status);

        mRadioGroup = (RadioGroup) v.findViewById(R.id.myradioGroup);
        mRadio1 = (RadioButton) v.findViewById(R.id.radioButton1);
        mRadio2 = (RadioButton) v.findViewById(R.id.radioButton2);
        mRadio3 = (RadioButton) v.findViewById(R.id.radioButton3);
        mRadio4 = (RadioButton) v.findViewById(R.id.radioButton4);

        mLabelParams = v.findViewById(R.id.label_params);

        mLayoutPayMode = v.findViewById(R.id.layout_payMode);
        mLayoutValue1 = v.findViewById(R.id.layout_value1);
        mLayoutValue2 = v.findViewById(R.id.layout_value2);
        mLayoutValue3 = v.findViewById(R.id.layout_value3);
        mLayoutValue4 = v.findViewById(R.id.layout_value4);

        mLabelValue1 = (EditText) v.findViewById(R.id.label_value1);
        mLabelValue2 = (EditText) v.findViewById(R.id.label_value2);
        mLabelValue3 = (EditText) v.findViewById(R.id.label_value3);
        mLabelValue4 = (EditText) v.findViewById(R.id.label_value4);

        mInputPayMode = (EditText) v.findViewById(R.id.input_payMode);
        mInputValue1 = (EditText) v.findViewById(R.id.input_value1);
        mInputValue2 = (EditText) v.findViewById(R.id.input_value2);
        mInputValue3 = (EditText) v.findViewById(R.id.input_value3);
        mInputValue4 = (EditText) v.findViewById(R.id.input_value4);

        mComments = (EditText) v.findViewById(R.id.input_comments);
        mProcess = (AppCompatButton) v.findViewById(R.id.btn_process);
    }

    @Override
    public void onResume() {
        super.onResume();
        mListener.getRetainedFragment().setResumeOk(true);
    }
}



