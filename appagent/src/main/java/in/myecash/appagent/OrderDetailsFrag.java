package in.myecash.appagent;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.backendless.exceptions.BackendlessException;

import java.text.SimpleDateFormat;

import in.myecash.appagent.entities.AgentUser;
import in.myecash.appagent.helper.MyRetainedFragment;
import in.myecash.appbase.BaseFragment;
import in.myecash.appbase.SingleWebViewActivity;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.OnSingleClickListener;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.MerchantOrders;

/**
 * Created by adgangwa on 05-03-2017.
 */

public class OrderDetailsFrag extends BaseFragment {
    private static final String TAG = "AgentApp-OrderDetailsFrag";

    private final SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

    public interface OrderDetailsFragIf {
        MyRetainedFragment getRetainedFragment();
        void allocateCards();
        void fetchAllottedCards(String orderId);
        //void changeOrderStatus(String orderId);
    }

    private OrderDetailsFragIf mCallback;
    MerchantOrders mOrder;

    /*public static OrderDetailsFrag getInstance(String orderId) {
        Bundle args = new Bundle();
        args.putString(ARG_ORDERID, orderId);
        OrderDetailsFrag fragment = new OrderDetailsFrag();
        fragment.setArguments(args);
        return fragment;
    }*/

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (OrderDetailsFragIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement OrderDetailsFragIf");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateView");
        View v = inflater.inflate(R.layout.frag_order_details, container, false);
        // access to UI elements
        bindUiResources(v);
        return v;
    }

    private void initDialogView() {
        /*String orderId = getArguments().getString(ARG_ORDERID, null);
        // find order object
        mOrder = null;
        for (MerchantOrders od :
                mCallback.getRetainedFragment().mLastFetchMchntOrders) {
            if(od.getOrderId().equals(orderId)) {
                mOrder = od;
            }
        }
        if (mOrder==null) {
            throw new BackendlessException(String.valueOf(ErrorCodes.GENERAL_ERROR), "No Order with ID: "+orderId);
        }*/

        mOrderId.setText(mOrder.getOrderId());
        mCreated.setText(mSdfDateWithTime.format(mOrder.getCreateTime()));
        if(mOrder.getDeliveryTime()!=null) {
            mDelivered.setText(mSdfDateWithTime.format(mOrder.getDeliveryTime()));
        }

        mItemName.setText(DbConstants.skuToItemName.get(mOrder.getItemSku()));
        mItemQty.setText(String.valueOf(mOrder.getItemQty()));
        mItemRate.setText(String.valueOf(mOrder.getItemPrice()));
        mTotalPrice.setText(String.valueOf(mOrder.getTotalPrice()));

        DbConstants.MCHNT_ORDER_STATUS status = DbConstants.MCHNT_ORDER_STATUS.valueOf(mOrder.getStatus());
        mInputStatus.setText(mOrder.getStatus());
        mChangedBy.setText(mOrder.getStatusChangeUser());
        mInputStatusDate.setText(mSdfDateWithTime.format(mOrder.getStatusChangeTime()));
        if(status==DbConstants.MCHNT_ORDER_STATUS.Rejected ||
                status==DbConstants.MCHNT_ORDER_STATUS.PaymentFailed ) {
            mInputReason.setVisibility(View.VISIBLE);
            mInputReason.setText(mOrder.getRejectReason());
            mInputStatus.setTextColor(ContextCompat.getColor(getActivity(), in.myecash.merchantbase.R.color.red_negative));
        } else {
            mInputReason.setVisibility(View.GONE);
        }

        if(mOrder.getActualPayMode()!=null)
            mPayMode.setText(mOrder.getActualPayMode());
        if(mOrder.getPaymentRef()!=null)
            mPayRefId.setText(mOrder.getPaymentRef());
        if(mOrder.getInvoiceId()!=null)
            mInvoiceId.setText(mOrder.getInvoiceId());

        mMchntId.setText(mOrder.getMerchantId());
        if(mOrder.getAgentName()!=null || mOrder.getAgentId()!=null) {
            String txt = mOrder.getAgentName() + "(" + mOrder.getAgentId() + ")";
            mAgentNameId.setText(txt);
        }
        mIsFrstOrder.setText(mOrder.getIsFirstOrder().toString());
        mAllottedCards.setText(mOrder.getAllotedCardCnt().toString());
        if(mOrder.getAllotedCardCnt()>0) {
            mAllottedCards.setTextColor(ContextCompat.getColor(getActivity(), R.color.link_blue));
            mAllottedCards.setPaintFlags(mAllottedCards.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);
            mAllottedCards.setOnClickListener(new OnSingleClickListener() {
                @Override
                public void onSingleClick(View v) {
                    // show card list dialog
                    mCallback.fetchAllottedCards(mOrder.getOrderId());
                }
            });
        }

        // Set up buttons
        initButtons();
    }

    private void initButtons() {
        DbConstants.MCHNT_ORDER_STATUS status = DbConstants.MCHNT_ORDER_STATUS.valueOf(mOrder.getStatus());

        if(mOrder.getInvoiceUrl()==null || mOrder.getInvoiceUrl().isEmpty()) {
            mViewInvoice.setEnabled(false);
            mViewInvoice.setOnClickListener(null);
            mViewInvoice.setAlpha(0.4f);
        } else {
            mViewInvoice.setOnClickListener(this);
        }

        if(status==DbConstants.MCHNT_ORDER_STATUS.New &&
                mOrder.getAllotedCardCnt()<mOrder.getItemQty()) {
            mBtnAllocateCards.setOnClickListener(this);
        } else {
            mBtnAllocateCards.setEnabled(false);
            mBtnAllocateCards.setOnClickListener(null);
            mBtnAllocateCards.setAlpha(0.4f);
        }

        if(status==DbConstants.MCHNT_ORDER_STATUS.New ||
                status==DbConstants.MCHNT_ORDER_STATUS.InProcess ||
                status==DbConstants.MCHNT_ORDER_STATUS.Shipped ||
                status==DbConstants.MCHNT_ORDER_STATUS.PaymentVerifyPending) {
            mChangeStatus.setOnClickListener(this);
        } else {
            mChangeStatus.setEnabled(false);
            mChangeStatus.setOnClickListener(null);
            mChangeStatus.setAlpha(0.4f);
        }

        if(status==DbConstants.MCHNT_ORDER_STATUS.Shipped) {
            mCancel.setOnClickListener(this);
        } else {
            mCancel.setEnabled(false);
            mCancel.setOnClickListener(null);
            mCancel.setAlpha(0.4f);
        }

        // For agent - only 'Allocate Cards' is valid
        // that also only for first order during registration
        if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_AGENT) {
            mBtnLayout2.setVisibility(View.GONE);

            if(!mOrder.getIsFirstOrder() && mChangeStatus.isEnabled()) {
                mChangeStatus.setEnabled(false);
                mChangeStatus.setOnClickListener(null);
                mChangeStatus.setAlpha(0.4f);
            }
        }
    }

    @Override
    public boolean handleTouchUp(View v) {
        // do nothing
        return false;
    }

    @Override
    public void handleBtnClick(View v) {
        switch (v.getId()) {
            case R.id.btn_invoice:
                // show invoice
                //mCallback.viewInvoice(mOrder.getOrderId());
                Intent intent = new Intent(getActivity(), SingleWebViewActivity.class );
                intent.putExtra(SingleWebViewActivity.INTENT_EXTRA_URL, mOrder.getInvoiceUrl());
                startActivity(intent);
                break;

            case R.id.btn_allocateCards:
                mCallback.allocateCards();
                break;
        }
    }


    private EditText mOrderId;
    private EditText mCreated;
    private EditText mDelivered;

    private EditText mItemName;
    private EditText mItemQty;
    private EditText mItemRate;
    private EditText mTotalPrice;

    private EditText mInputStatus;
    private EditText mChangedBy;
    private EditText mInputStatusDate;
    private EditText mInputReason;

    private EditText mPayMode;
    private EditText mPayRefId;
    private EditText mInvoiceId;

    private EditText mMchntId;
    private EditText mAgentNameId;
    private EditText mIsFrstOrder;
    private EditText mAllottedCards;

    private AppCompatButton mViewInvoice;
    private AppCompatButton mBtnAllocateCards;
    private AppCompatButton mChangeStatus;
    private AppCompatButton mCancel;
    private View mBtnLayout2;

    private void bindUiResources(View v) {
        mOrderId = (EditText) v.findViewById(R.id.input_orderId);
        mCreated = (EditText) v.findViewById(R.id.input_created);
        mDelivered = (EditText) v.findViewById(R.id.input_delivered);

        mItemName = (EditText) v.findViewById(R.id.input_item_name);
        mItemQty = (EditText) v.findViewById(R.id.input_itemQty);
        mItemRate = (EditText) v.findViewById(R.id.input_itemRate);
        mTotalPrice = (EditText) v.findViewById(R.id.input_totalPrice);

        mInputStatus = (EditText) v.findViewById(R.id.input_status);
        mChangedBy = (EditText) v.findViewById(R.id.input_status_changedBy);
        mInputStatusDate = (EditText) v.findViewById(R.id.input_status_date);
        mInputReason = (EditText) v.findViewById(R.id.input_status_reason);

        mPayMode = (EditText) v.findViewById(R.id.input_payMode);
        mPayRefId = (EditText) v.findViewById(R.id.input_payRefId);
        mInvoiceId = (EditText) v.findViewById(R.id.input_invoiceId);

        mMchntId = (EditText) v.findViewById(R.id.input_merchantId);
        mAgentNameId = (EditText) v.findViewById(R.id.input_agent);
        mIsFrstOrder = (EditText) v.findViewById(R.id.input_firstOrder);
        mAllottedCards = (EditText) v.findViewById(R.id.input_allotCards);

        mViewInvoice = (AppCompatButton) v.findViewById(R.id.btn_invoice);
        mBtnAllocateCards = (AppCompatButton) v.findViewById(R.id.btn_allocateCards);
        mChangeStatus = (AppCompatButton) v.findViewById(R.id.btn_status);
        mCancel = (AppCompatButton) v.findViewById(R.id.btn_cancel);
        mBtnLayout2 = v.findViewById(R.id.btn_layout2);
    }

    @Override
    public void onResume() {
        super.onResume();
        mOrder = mCallback.getRetainedFragment().mCurrOrder;
        initDialogView();
        mCallback.getRetainedFragment().setResumeOk(true);
    }

}

