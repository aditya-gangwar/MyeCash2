package in.myecash.appagent;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;

import in.myecash.appagent.helper.MyRetainedFragment;
import in.myecash.appbase.BaseFragment;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.ItemClickSupport;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.appbase.utilities.OnSingleClickListener;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.MerchantOrders;

/**
 * Created by adgangwa on 01-03-2017.
 */

public class MchntOrderListFragInternal extends BaseFragment {
    private static final String TAG = "MchntApp-MerchantOrderListFrag";

    private static final int REQ_NOTIFY_ERROR = 1;

    private SimpleDateFormat mSdfDateWithTime;

    private RecyclerView mRecyclerView;
    private MyRetainedFragment mRetainedFragment;
    private MerchantOrderListFragIf mCallback;

    public interface MerchantOrderListFragIf {
        MyRetainedFragment getRetainedFragment();
        void showOrderDetails(int pos);
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_mchnt_order_list, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.mchntOrder_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }

    private void updateUI() {
        if(mRetainedFragment.mLastFetchMchntOrders!=null &&
                !mRetainedFragment.mLastFetchMchntOrders.isEmpty()) {
            mRecyclerView.setAdapter(new MchntOrderAdapter(mRetainedFragment.mLastFetchMchntOrders));

            /*ItemClickSupport.addTo(mRecyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
                @Override
                public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                    // show order details
                    LogMy.d(TAG,"Clicked list item: "+position);
                    mCallback.showOrderDetails(position);
                }
            });*/
        } else {
            LogMy.d(TAG,"Mchnt Order List is empty");
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

    private class MchntOrderHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private MerchantOrders mOrder;

        public TextView mInputOrderId;
        public TextView mInputOrderTime;
        public TextView mInputStatus;
        public TextView mMerchantId;

        public View mLayout;

        public MchntOrderHolder(View itemView) {
            super(itemView);
            mInputOrderId = (TextView) itemView.findViewById(R.id.input_orderId);
            mInputOrderTime = (TextView) itemView.findViewById(R.id.input_order_time);
            mInputStatus = (TextView) itemView.findViewById(R.id.input_status);
            mMerchantId = (TextView) itemView.findViewById(R.id.input_mchntId);


            mLayout = itemView.findViewById(R.id.layout_card);
            /*mLayout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    ((View)v.getParent()).performClick();
                    return false;
                }
            });*/
            mLayout.setOnClickListener(this);

            //mInputOrderId.setOnClickListener(null);
            //mInputOrderTime.setOnClickListener(null);
            //mInputOrderTime.setOnTouchListener(null);
            //mInputStatus.setOnClickListener(null);
            //mMerchantId.setOnClickListener(null);
        }

        public void bindOrder(MerchantOrders order) {
            mOrder = order;

            String txt = "Order#   "+order.getOrderId();
            mInputOrderId.setText(txt);
            mInputOrderTime.setText(mSdfDateWithTime.format(mOrder.getCreateTime()));

            DbConstants.MCHNT_ORDER_STATUS status = DbConstants.MCHNT_ORDER_STATUS.valueOf(order.getStatus());
            txt = "Status: "+DbConstants.MCHNT_ORDER_STATUS.toString(status);
            mInputStatus.setText(txt);

            txt = "Merchant ID: "+order.getMerchantId();
            mMerchantId.setText(txt);
        }

        @Override
        public void onClick(View view) {
            LogMy.d(TAG, "onClick: " + getAdapterPosition());
            view.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.disabled));
            mCallback.showOrderDetails(getAdapterPosition());
        }

        /*@Override
        public boolean onTouch(View v, MotionEvent event) {
            if(event.getAction()==MotionEvent.ACTION_UP) {
                v.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.disabled));
                mCallback.showOrderDetails(getAdapterPosition());
            }
            return false;
        }*/
    }

    private class MchntOrderAdapter extends RecyclerView.Adapter<MchntOrderHolder> {
        private List<MerchantOrders> mOrders;

        public MchntOrderAdapter(List<MerchantOrders> orders) {
            mOrders = orders;
        }

        @Override
        public MchntOrderHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.mchnt_order_itemview_internal, parent, false);
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
