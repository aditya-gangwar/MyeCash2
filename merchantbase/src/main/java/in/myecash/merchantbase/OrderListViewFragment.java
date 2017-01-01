package in.myecash.merchantbase;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;

import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.merchantbase.adapter.OrderListViewAdapter;
import in.myecash.merchantbase.entities.OrderItem;
import in.myecash.merchantbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 04-03-2016.
 */
public class OrderListViewFragment extends ListFragment implements
        View.OnClickListener, OrderListViewAdapter.OrderListViewIf {

    private static final String TAG = "MchntApp-OrderListViewFragment";

    private static final int REQ_CONFIRM_ITEM_DEL = 1;
    private static final int REQ_NEW_UNIT_PRICE = 2;
    private static final int REQ_NEW_QTY = 3;

    private static final String DIALOG_NUM_INPUT = "NumberInput";

    private OrderListViewFragmentIf mCallback;
    private MyRetainedFragment mRetainedFragment;
    private ArrayAdapter<OrderItem> mAdapter;

    // store to instance state
    private int mChangePosition;
    //private OrderListViewAdapter.ViewHolder mCurViewholder;
    //private OrderItem mCurItem;

    // Container Activity must implement this interface
    public interface OrderListViewFragmentIf {
        MyRetainedFragment getRetainedFragment();
        void onTotalBillFromOrderList();
        void setDrawerState(boolean isEnabled);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateView");
        View view = inflater.inflate(R.layout.fragment_order_itemlist, container, false);

        bindUiResources(view);
        mTotalBtn.setOnClickListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        LogMy.d(TAG, "In onActivityCreated");
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (OrderListViewFragmentIf) getActivity();
            mRetainedFragment = mCallback.getRetainedFragment();
            mAdapter = new OrderListViewAdapter(getActivity(), mRetainedFragment.mOrderItems, this);
            setListAdapter(mAdapter);
            setTotalAmt();

            if(savedInstanceState!=null) {
                mChangePosition = savedInstanceState.getInt("mChangePosition");
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement OrderListViewFragmentIf");
        } catch(Exception e) {
            LogMy.e(TAG, "Exception is OrderListViewFragmentIf", e);
            // unexpected exception - show error
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            getActivity().onBackPressed();
        }

    }

    private void setTotalAmt() {
        String str = "Total      "+ AppConstants.SYMBOL_RS + String.valueOf(mRetainedFragment.mBillTotal);
        mTotalBtn.setText(str);
    }

    @Override
    public void onResume() {
        LogMy.d(TAG, "In onResume");
        super.onResume();
        mCallback.setDrawerState(false);
        mCallback.getRetainedFragment().setResumeOk(true);
    }

    @Override
    public void deleteItem(int position) {
        LogMy.d(TAG, "In deleteItem: " + position);
        mChangePosition = position;
        // Show confirmation dialog
        DialogFragmentWrapper dialog = DialogFragmentWrapper.createConfirmationDialog(AppConstants.itemDeleteConfirmTitle, AppConstants.itemDeleteConfirmMsg, true, false);
        dialog.setTargetFragment(this, REQ_CONFIRM_ITEM_DEL);
        dialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_CONFIRMATION);
    }

    @Override
    public void onEditUnitPrice(int position) {
        LogMy.d(TAG, "In onEditUnitPrice");
        mChangePosition = position;

        FragmentManager manager = getFragmentManager();
        // cash to be paid
        NumberInputDialog dialog = NumberInputDialog.newInstance("Unit Price:",
                mRetainedFragment.mOrderItems.get(position).getUnitPriceStr(), true, 0);
        dialog.setTargetFragment(this, REQ_NEW_UNIT_PRICE);
        dialog.show(manager, DIALOG_NUM_INPUT);
    }

    @Override
    public void onEditQuantity(int position) {
        LogMy.d(TAG, "In onEditQuantity");
        mChangePosition = position;

        FragmentManager manager = getFragmentManager();
        // cash to be paid
        NumberInputDialog dialog = NumberInputDialog.newInstance("Quantity:",
                mRetainedFragment.mOrderItems.get(position).getQuantityStr(), false, 0);
        dialog.setTargetFragment(this, REQ_NEW_QTY);
        dialog.show(manager, DIALOG_NUM_INPUT);
    }

    @Override
    public void onToggleExclusion(int position) {
        LogMy.d(TAG, "In onEditQuantity");

        OrderItem item = mRetainedFragment.mOrderItems.get(position);
        item.setCashbackExcluded(!item.isCashbackExcluded());

        // change amount as per new state
        if (item.isCashbackExcluded()) {
            mRetainedFragment.mCbExcludedTotal = mRetainedFragment.mCbExcludedTotal + item.getPrice();
        } else {
            mRetainedFragment.mCbExcludedTotal = mRetainedFragment.mCbExcludedTotal - item.getPrice();
        }

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        try {
            if (requestCode == REQ_CONFIRM_ITEM_DEL) {
                LogMy.d(TAG, "Received delete item confirmation.");
                // delete item in the list
                OrderItem item = mRetainedFragment.mOrderItems.remove(mChangePosition);
                // update total bill amount
                mRetainedFragment.mBillTotal = mRetainedFragment.mBillTotal - item.getPrice();
                if (item.isCashbackExcluded()) {
                    mRetainedFragment.mCbExcludedTotal = mRetainedFragment.mCbExcludedTotal - item.getPrice();
                }

            } else if (requestCode == REQ_NEW_UNIT_PRICE) {
                LogMy.d(TAG, "Received new unit price.");
                String newUnitPrice = (String) data.getSerializableExtra(NumberInputDialog.EXTRA_INPUT_HUMBER);

                OrderItem item = mRetainedFragment.mOrderItems.get(mChangePosition);
                if (!item.getUnitPriceStr().equals(newUnitPrice)) {
                    int oldPrice = item.getPrice();
                    item.setUnitPriceStr(newUnitPrice);
                    updateBillTotal(oldPrice, item);
                }

            } else if (requestCode == REQ_NEW_QTY) {
                LogMy.d(TAG, "Received new quantity.");
                String newQty = (String) data.getSerializableExtra(NumberInputDialog.EXTRA_INPUT_HUMBER);

                OrderItem item = mRetainedFragment.mOrderItems.get(mChangePosition);
                if (!item.getQuantityStr().equals(newQty)) {
                    int oldPrice = item.getPrice();
                    item.setQuantityStr(newQty);
                    updateBillTotal(oldPrice, item);
                }
            }

            mAdapter.notifyDataSetChanged();
            setTotalAmt();
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in OrderListViewFragment: ", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    // Update Bill total for change in Item price
    private void updateBillTotal(int oldItemPrice, OrderItem updatedItem) {
        LogMy.d(TAG,"In processPriceChange: "+oldItemPrice);

        mRetainedFragment.mBillTotal = mRetainedFragment.mBillTotal - oldItemPrice + updatedItem.getPrice();
        if (updatedItem.isCashbackExcluded()) {
            mRetainedFragment.mCbExcludedTotal = mRetainedFragment.mCbExcludedTotal - oldItemPrice + updatedItem.getPrice();
        }
    }

    @Override
    public void onClick(View v) {
        if(!mCallback.getRetainedFragment().getResumeOk())
            return;

        try {
            int id = v.getId();
            if (id == R.id.btn_bill_total) {
                mCallback.onTotalBillFromOrderList();
            }
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in Fragment: ", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        LogMy.d(TAG, "In onActivityCreated");
        super.onViewCreated(view, savedInstanceState);
        // remove the dividers from the ListView of the ListFragment
        getListView().setDivider(null);
    }

    private Button mTotalBtn;
    private void bindUiResources(View v) {
        mTotalBtn = (Button)v.findViewById(R.id.btn_bill_total);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("mChangePosition", mChangePosition);
    }
}
