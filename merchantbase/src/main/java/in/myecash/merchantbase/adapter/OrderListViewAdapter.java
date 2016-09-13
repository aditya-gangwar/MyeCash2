package in.myecash.merchantbase.adapter;

import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import in.myecash.commonbase.constants.AppConstants;
import in.myecash.commonbase.utilities.LogMy;
import in.myecash.merchantbase.R;
import in.myecash.merchantbase.entities.OrderItem;

import java.util.List;

/**
 * Created by adgangwa on 04-03-2016.
 */
public class OrderListViewAdapter extends ArrayAdapter<OrderItem> {
    private static final String TAG = "OrderListViewAdapter";

    private Activity mActivity;
    private OrderListViewIf mCallback;

    public interface OrderListViewIf {
        void deleteItem(int position);
        /*
        void onEditUnitPrice(OrderItem item, ViewHolder views);
        void onEditQuantity(OrderItem item, ViewHolder views);
        void onToggleExclusion(OrderItem item, ViewHolder views);*/
        void onEditUnitPrice(int position);
        void onEditQuantity(int position);
        void onToggleExclusion(int position);
    }

    public OrderListViewAdapter(Activity activity, List<OrderItem> items, OrderListViewIf callback) {
        super(activity, R.layout.order_itemview, items);
        mActivity = activity;
        mCallback = callback;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if(convertView == null) {
            // inflate the GridView item layout
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.order_itemview, parent, false);
            // initialize the view holder
            viewHolder = new ViewHolder();
            viewHolder.itemSNo = (EditText) convertView.findViewById(R.id.item_s_no);
            viewHolder.qty = (EditText) convertView.findViewById(R.id.input_quantity);
            viewHolder.unitPrice = (EditText) convertView.findViewById(R.id.input_unit_price);
            viewHolder.totalPrice = (EditText) convertView.findViewById(R.id.input_item_price);
            viewHolder.deleteAction = (AppCompatImageButton) convertView.findViewById(R.id.img_delete);
            convertView.setTag(viewHolder);
        } else {
            // recycle the already inflated view
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // update the item view
        OrderItem item = getItem(position);

        viewHolder.setItemSNo(position + 1);
        viewHolder.setUnitPrice(item);
        viewHolder.setQty(item);
        viewHolder.setTotalPrice(item);

        // Add listeners
        // it is important to create new listener here, as they involve using 'item' and 'viewholder'
        viewHolder.deleteAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogMy.d(TAG, "In onClick for delete");
                mCallback.deleteItem(position);
            }
        });
        viewHolder.unitPrice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogMy.d(TAG, "In onClick for unit price");
                //mCallback.onEditUnitPrice(item, viewHolder);
                mCallback.onEditUnitPrice(position);
            }
        });
        viewHolder.qty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogMy.d(TAG, "In onClick for quantity");
                //mCallback.onEditQuantity(item, viewHolder);
                mCallback.onEditQuantity(position);
            }
        });
        viewHolder.totalPrice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogMy.d(TAG, "In onClick for totalPrice");
                //mCallback.onToggleExclusion(item, viewHolder);
                mCallback.onToggleExclusion(position);
            }
        });

        return convertView;
    }

    public class ViewHolder {
        private EditText itemSNo;
        private EditText qty;
        private EditText unitPrice;
        private EditText totalPrice;
        private AppCompatImageButton deleteAction;

        private int oldColor;

        public void setItemSNo(int value) {
            itemSNo.setText(String.format("%02d", value));
        }
        public void setUnitPrice(OrderItem item) {
            unitPrice.setText(AppConstants.RS_SYMBOL);
            unitPrice.append(item.getUnitPriceStr());
        }
        public void setQty(OrderItem item) {
            qty.setText(String.format("%02d", item.getQuantity()));
        }
        public void setTotalPrice(OrderItem item) {
            totalPrice.setText(AppConstants.RS_SYMBOL);
            totalPrice.append(item.getPriceStr());
            if(item.isCashbackExcluded()) {
                markItemExclusion(item);
            }
        }

        private void markItemExclusion(OrderItem item) {
            //item.setCashbackExcluded(true);
            // set text color to highlight exclusion
            oldColor = totalPrice.getCurrentTextColor();
            totalPrice.setTextColor(ContextCompat.getColor(mActivity, R.color.cb_exclusion));
        }
        /*
        public void toggleItemExclusion(OrderItem item) {
            if(item.isCashbackExcluded()) {
                item.setCashbackExcluded(false);
                totalPrice.setTextColor(oldColor);
            } else {
                setItemExclusion(item);
            }
        }*/
    }
}


        /*
        viewHolder.qty.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                LogMy.d(TAG,"In onTextChanged: "+s+", "+start+", "+before+", "+count);
                // do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                String newQty = s.toString();
                LogMy.d(TAG, "In afterTextChanged: " + newQty);
                if(!newQty.isEmpty()) {
                    if (!item.mQuantity.equals(newQty)) {
                        item.mQuantity = newQty;
                        int newPrice = Integer.parseInt(newQty) * Integer.parseInt(item.mUnitPrice);
                        processPriceChange(newPrice, item, viewHolder);
                    }
                    //AndroidUtil.hideKeyboard(mActivity);
                    mTempQty = null;
                    viewHolder.qty.setHint("");
                    viewHolder.qty.setBackground(null);
                }
            }
        });*/

        /*
        viewHolder.qty.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                LogMy.d(TAG,"In onTouch: "+event.toString());
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (mTempQty == null) {
                        mTempQty = viewHolder.qty.getText().toString();
                        viewHolder.qty.setHint(mTempQty);
                        viewHolder.qty.setText("");
                        viewHolder.qty.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.frame));
                        //viewHolder.qty.requestFocus();
                        // show keyboard
                        //v.performClick();
                        v.dispatchTouchEvent(event);
                    }
//                    return true;
                }
                return false;
            }
        });*/

        /*
        viewHolder.qty.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                LogMy.d(TAG, "In onEditorAction: " + actionId + ", " + (event == null ? "null" : event.toString()));
                if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_NEXT || actionId==EditorInfo.IME_ACTION_DONE) {
                    String newQty = v.getText().toString();
                    LogMy.d(TAG, "Enter key pressed: "+newQty);
                    if(newQty.isEmpty()) {
                        //restore old value
                        viewHolder.qty.setText(item.mQuantity);
                    }else if (!item.mQuantity.equals(newQty)) {
                        item.mQuantity = newQty;
                        int newPrice = Integer.parseInt(newQty) * Integer.parseInt(item.mUnitPrice);
                        processPriceChange(newPrice, item, viewHolder);
                    }
                    mTempQty = null;
                    viewHolder.qty.setHint("");
                    viewHolder.qty.setBackground(null);
                    AndroidUtil.hideKeyboard(mActivity);
                    viewHolder.qty.clearFocus();
                    //mBtnLabelBillAmt.requestFocus();
                    //View current = mActivity.getCurrentFocus();
                    //if (current != null) current.clearFocus();
                }
                return true;
            }
        });*/

        /*
        viewHolder.qty.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                LogMy.d(TAG, "In onFocusChange: " + hasFocus);
                if (hasFocus) {
                    if (mTempQty == null) {
                        LogMy.d(TAG, "In onFocusChange: change for editing: "+viewHolder.qty.getId()+", "+v.getId());
                        // first time with hasFocus==true, or
                        // again after the new value is used
                        mTempQty = viewHolder.qty.getText().toString();
                        viewHolder.qty.setText("");
                        viewHolder.qty.setHint(mTempQty);
                        viewHolder.qty.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.frame));

                        InputMethodManager mgr = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        if( !mgr.showSoftInput(viewHolder.qty, InputMethodManager.SHOW_IMPLICIT) ) {
                            LogMy.d(TAG, "Show keyboard failure");
                        }
                        //mgr.restartInput(viewHolder.qty);
                        //viewHolder.qty.requestFocus();
                        //v.performClick();
                        //viewHolder.qty.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                        //viewHolder.qty.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
                    }
                }
            }
        });*/

        /*
        viewHolder.qty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogMy.d(TAG, "In onClick");
                String newQty = ((EditText) v).getText().toString();
                if (!item.mQuantity.equals(newQty)) {
                    item.mQuantity = newQty;
                    int newPrice = Integer.parseInt(newQty) * Integer.parseInt(item.mUnitPrice);
                    processPriceChange(newPrice, item, viewHolder);
                }
            }
        }); */

        /*
        viewHolder.unitPrice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newUnitPrice = ((EditText) v).getText().toString();
                // update in order to add rupee symbol
                viewHolder.unitPrice.setText(newUnitPrice);

                if (!item.mUnitPrice.equals(newUnitPrice)) {
                    item.mUnitPrice = newUnitPrice;
                    int newPrice = Integer.parseInt(newUnitPrice) * Integer.parseInt(item.mQuantity);
                    processPriceChange(newPrice, item, viewHolder);
                }
            }
        });

        viewHolder.totalPrice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(item.mCashbackExcluded) {
                    removeItemAmtExclusion(item, viewHolder);
                } else {
                    setItemAmtExclusion(item, viewHolder);
                }
            }
        });*/
