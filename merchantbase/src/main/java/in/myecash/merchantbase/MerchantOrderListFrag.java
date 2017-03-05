package in.myecash.merchantbase;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.List;

import in.myecash.appbase.BaseFragment;
import in.myecash.appbase.SingleWebViewActivity;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.OnSingleClickListener;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.MerchantOrders;
import in.myecash.merchantbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 01-03-2017.
 */

public class MerchantOrderListFrag extends BaseFragment {
    private static final String TAG = "MchntApp-MerchantOrderListFrag";

    private static final int REQ_NOTIFY_ERROR = 1;

    private SimpleDateFormat mSdfDateWithTime;

    private RecyclerView mRecyclerView;
    private View mEmptyListView;
    private MyRetainedFragment mRetainedFragment;
    private MerchantOrderListFragIf mCallback;

    public interface MerchantOrderListFragIf {
        MyRetainedFragment getRetainedFragment();
        void deleteMchntOrder(String orderId);
        void createMchntOrder();
        void setDrawerState(boolean isEnabled);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LogMy.d(TAG, "In onActivityCreated");

        try {
            mCallback = (MerchantOrderListFragIf) getActivity();
            mRetainedFragment = mCallback.getRetainedFragment();
            mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement MerchantOrderListFragIf");

        } catch (Exception e) {
            LogMy.e(TAG, "Exception in MerchantOrderListFrag: ", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            getActivity().onBackPressed();
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mchnt_order_list, container, false);

        mEmptyListView = view.findViewById(R.id.list_empty_view);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.mchntOrder_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.order_list_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            int i = item.getItemId();
            if (i == R.id.action_create) {
                // check if there's already any active order
                if(anyActiveOrder()) {
                    DialogFragmentWrapper dialog = DialogFragmentWrapper.createNotification(AppConstants.generalInfoTitle,
                            "Your last Order is not yet Complete.\n\nOnly Single Order is allowed at one time.", true, false);
                    dialog.setTargetFragment(this, REQ_NOTIFY_ERROR);
                    dialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
                } else {
                    mCallback.createMchntOrder();
                }
            }
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in Fragment: ", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean anyActiveOrder() {
        if(mRetainedFragment.mLastFetchMchntOrders==null) {
            return false;
        }
        for (MerchantOrders o :
                mRetainedFragment.mLastFetchMchntOrders) {
            DbConstants.MCHNT_ORDER_STATUS status = DbConstants.MCHNT_ORDER_STATUS.valueOf(o.getStatus());
            if (status==DbConstants.MCHNT_ORDER_STATUS.New ||
                    status==DbConstants.MCHNT_ORDER_STATUS.InProcess ||
                    status==DbConstants.MCHNT_ORDER_STATUS.Shipped) {
                return true;
            }
        }
        return false;
    }

    private void updateUI() {
        if(mRetainedFragment.mLastFetchMchntOrders==null ||
                mRetainedFragment.mLastFetchMchntOrders.isEmpty()) {
            mEmptyListView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            mEmptyListView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
            mRecyclerView.setAdapter(new MchntOrderAdapter(mRetainedFragment.mLastFetchMchntOrders));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogMy.d(TAG, "In onActivityResult :" + requestCode + ", " + resultCode);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
    }

    @Override
    public boolean handleTouchUp(View v) {
        // do nothing
        return false;
    }

    @Override
    public void handleBtnClick(View v) {
        // do nothing
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            mCallback.setDrawerState(false);
            updateUI();
            mCallback.getRetainedFragment().setResumeOk(true);
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in Fragment: ", e);
            mCallback.getRetainedFragment().setResumeOk(true);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            getActivity().onBackPressed();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private class MchntOrderHolder extends RecyclerView.ViewHolder {

        private MerchantOrders mOrder;

        public EditText mInputOrderId;
        public EditText mInputOrderTime;
        public EditText mInputItemDetails;
        public EditText mInputCostDetails;
        public EditText mInputStatus;
        public EditText mInputStatusReason;
        public EditText mInputDelivered;

        public AppCompatImageButton mDelete;
        public AppCompatButton mInvoice;

        private View.OnClickListener mListener;


        public MchntOrderHolder(View itemView) {
            super(itemView);
            mInputOrderId = (EditText) itemView.findViewById(R.id.input_orderId);
            mInputOrderTime = (EditText) itemView.findViewById(R.id.input_order_time);
            mInputItemDetails = (EditText) itemView.findViewById(R.id.input_item_details);
            mInputCostDetails = (EditText) itemView.findViewById(R.id.input_cost_details);
            mInputStatus = (EditText) itemView.findViewById(R.id.input_status);
            mInputStatusReason = (EditText) itemView.findViewById(R.id.input_status_reason);
            mInputDelivered = (EditText) itemView.findViewById(R.id.input_delivered_on);

            mDelete = (AppCompatImageButton) itemView.findViewById(R.id.img_delete);
            mInvoice = (AppCompatButton) itemView.findViewById(R.id.btn_invoice);

            mListener = new OnSingleClickListener() {
                @Override
                public void onSingleClick(View v) {
                    if(v.getId()==mInvoice.getId()) {
                        Intent intent = new Intent(getActivity(), SingleWebViewActivity.class );
                        intent.putExtra(SingleWebViewActivity.INTENT_EXTRA_URL, mOrder.getInvoiceUrl());
                        startActivity(intent);
                    } else if(v.getId()==mDelete.getId()) {
                        mCallback.deleteMchntOrder(mOrder.getOrderId());
                    }
                }
            };
            mDelete.setOnClickListener(mListener);
            mInvoice.setOnClickListener(mListener);
        }

        public void bindOrder(MerchantOrders order) {
            mOrder = order;

            String txt = "Order# "+order.getOrderId();
            mInputOrderId.setText(txt);
            mInputOrderTime.setText(mSdfDateWithTime.format(mOrder.getCreateTime()));

            txt = String.valueOf(order.getItemQty()) + " " + DbConstants.skuToItemName.get(order.getItemSku());
            mInputItemDetails.setText(txt);

            txt = "Total Cost: " + String.valueOf(order.getItemQty()) + " X " +
                    AppCommonUtil.getAmtStr(order.getItemPrice()) + " = " +
                    AppCommonUtil.getAmtStr(order.getTotalPrice());
            mInputCostDetails.setText(txt);

            DbConstants.MCHNT_ORDER_STATUS status = DbConstants.MCHNT_ORDER_STATUS.valueOf(order.getStatus());
            txt = "Status: "+DbConstants.MCHNT_ORDER_STATUS.toString(status);
            mInputStatus.setText(txt);

            if(status.equals(DbConstants.MCHNT_ORDER_STATUS.Rejected) ||
                    status.equals(DbConstants.MCHNT_ORDER_STATUS.PaymentFailed) ) {
                mInputStatusReason.setVisibility(View.VISIBLE);
                mInputStatusReason.setText(order.getRejectReason());
                mInputStatus.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
            } else {
                mInputStatusReason.setVisibility(View.GONE);
            }

            if(mOrder.getDeliveryTime()!=null) {
                mInputDelivered.setVisibility(View.VISIBLE);
                mInputDelivered.setText(mSdfDateWithTime.format(mOrder.getDeliveryTime()));
            } else {
                mInputDelivered.setVisibility(View.GONE);
            }

            if(status.equals(DbConstants.MCHNT_ORDER_STATUS.New)) {
                mDelete.setVisibility(View.VISIBLE);
            } else {
                mDelete.setVisibility(View.GONE);
            }

            if(mOrder.getInvoiceUrl()!=null && !mOrder.getInvoiceUrl().isEmpty()) {
                mInvoice.setVisibility(View.VISIBLE);
            } else {
                mInvoice.setVisibility(View.GONE);
            }
        }
    }

    private class MchntOrderAdapter extends RecyclerView.Adapter<MchntOrderHolder> {
        private List<MerchantOrders> mOrders;

        public MchntOrderAdapter(List<MerchantOrders> orders) {
            mOrders = orders;
        }

        @Override
        public MchntOrderHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.mchnt_order_itemview, parent, false);
            //LogMy.d(TAG,"Root view: "+view.getId());
            //view.setOnClickListener(mListener);
            return new MchntOrderHolder(view);
        }
        @Override
        public void onBindViewHolder(MchntOrderHolder holder, int position) {
            MerchantOrders order = mOrders.get(position);
            holder.bindOrder(order);
        }
        @Override
        public int getItemCount() {
            return mOrders.size();
        }
    }
}
