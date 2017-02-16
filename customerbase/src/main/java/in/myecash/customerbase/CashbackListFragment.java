package in.myecash.customerbase;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import in.myecash.appbase.BaseFragment;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.common.MyGlobalSettings;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.appbase.entities.MyCashback;
import in.myecash.common.MyMerchant;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.customerbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 09-09-2016.
 */
public class CashbackListFragment extends BaseFragment {
    private static final String TAG = "CustApp-CashbackListFragment";

    private static final String DIALOG_MERCHANT_DETAILS = "dialogMerchantDetails";
    private static final String DIALOG_SORT_CUST_TYPES = "dialogSortCust";

    private static final int REQ_NOTIFY_ERROR = 1;
    private static final int REQ_SORT_CUST_TYPES = 2;

    private SimpleDateFormat mSdfDateWithTime;
    private MyRetainedFragment mRetainedFragment;
    private CashbackListFragmentIf mCallback;

    public interface CashbackListFragmentIf {
        MyRetainedFragment getRetainedFragment();
        boolean refreshMchntList();
        //void setDrawerState(boolean isEnabled);
    }

    private RecyclerView mRecyclerView;
    private EditText mUpdated;
    //private EditText mUpdatedDetail;
    private List<MyCashback> mMyCbs;

    // instance state - store and restore
    private int mSelectedSortType;


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LogMy.d(TAG, "In onActivityCreated");

        try {
            mCallback = (CashbackListFragmentIf) getActivity();
            mRetainedFragment = mCallback.getRetainedFragment();

            mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

            // As the data is in Map - which cant be sorted
            // so create a local list from Map in retained fragment
            // note - this is not copy, as both list and Map will point to same MyCashback objects
            mMyCbs = new ArrayList<>(mRetainedFragment.mCashbacks.values());

            int sortType = MyCashback.CB_CMP_TYPE_UPDATE_TIME;
            if(savedInstanceState!=null) {
                sortType = savedInstanceState.getInt("mSelectedSortType");
            }
            sortList(sortType);

            // update time
            //mUpdated.setText(mSdfDateWithTime.format(mRetainedFragment.mCbsUpdateTime));

            setHasOptionsMenu(true);

        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement CashbackListFragmentIf");
        } catch(Exception e) {
            LogMy.e(TAG, "Exception is CashbackListFragment:onActivityCreated", e);
            // unexpected exception - show error
            DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true);
            notDialog.setTargetFragment(this,REQ_NOTIFY_ERROR);
            notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            getActivity().onBackPressed();
        }
    }

    private void sortList(int sortType) {
        if(mMyCbs!=null) {
            Collections.sort(mMyCbs, new MyCashback.MyCashbackComparator(sortType));

            if (sortType != MyCashback.CB_CMP_TYPE_MCHNT_NAME &&
                    sortType != MyCashback.CB_CMP_TYPE_MCHNT_CITY) {
                // Make it in decreasing order - if not string comparison
                Collections.reverse(mMyCbs);
            }
            // store existing sortType
            mSelectedSortType = sortType;
        } else {
            LogMy.e(TAG,"In sortList: mCashbacks is null");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_merchant_list, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.cust_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mUpdated = (EditText) view.findViewById(R.id.input_updated_time);
        //mUpdatedDetail = (EditText) view.findViewById(R.id.updated_time_details);

        return view;
    }

    private void updateUI() {
        if(mMyCbs!=null) {
            CbAdapter adapter = (CbAdapter) mRecyclerView.getAdapter();
            if(adapter==null) {
                LogMy.d(TAG, "Adaptor not set yet");
                mRecyclerView.setAdapter(new CbAdapter(mMyCbs));
            } else {
                adapter.refresh(mMyCbs);
            }
            // update time
            mUpdated.setText(mSdfDateWithTime.format(mRetainedFragment.mCbsUpdateTime));

            /*mRecyclerView.removeAllViews();
            mRecyclerView.setAdapter(new CbAdapter(mMyCbs));
            mRecyclerView.invalidate();
            mRecyclerView.getAdapter().notifyDataSetChanged();*/
        } else {
            LogMy.e(TAG,"In updateUI: mCashbacks is null");
        }
    }

    public void refreshData() {
        if(mRetainedFragment.mCashbacks!=null) {
            mMyCbs = new ArrayList<>(mRetainedFragment.mCashbacks.values());
            for(MyCashback cb:mMyCbs) {
                LogMy.d(TAG,"In refreshData: "+cb.getMerchantId()+", "+cb.getClCredit()+", "+cb.getCbCredit());
            }
            sortList(mSelectedSortType);
            updateUI();
        } else {
            LogMy.e(TAG,"In refreshData: mCashbacks is null");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogMy.d(TAG, "In onActivityResult :" + requestCode + ", " + resultCode);
        if(resultCode != Activity.RESULT_OK) {
            return;
        }
        try {
            if (requestCode == REQ_SORT_CUST_TYPES) {
                int sortType = data.getIntExtra(SortMchntDialog.EXTRA_SELECTION, MyCashback.CB_CMP_TYPE_UPDATE_TIME);
                sortList(sortType);
                updateUI();
            }
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in Fragment: ", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //mCallback.setDrawerState(false);
        try {
            updateUI();
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in Fragment: ", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            getActivity().onBackPressed();
        }
        mCallback.getRetainedFragment().setResumeOk(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("mSelectedSortType", mSelectedSortType);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.merchant_list_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(!mCallback.getRetainedFragment().getResumeOk())
            return true;

        try {
            int i = item.getItemId();
            if (i == R.id.action_sort) {
                sortMerchantList();

            } else if(i == R.id.action_refresh) {
                if(!mCallback.refreshMchntList()) {
                    String msg = null;
                    if(MyGlobalSettings.getCustNoRefreshMins()==(24*60)) {
                        // 24 is treated as special case as 'once in a day'
                        msg = "Refresh is allowed once a day.";
                    } else {
                        msg = "Refresh allowed once every "+String.valueOf(MyGlobalSettings.getCustNoRefreshMins())+" minutes";
                    }

                    msg = msg+"\n\n"+"* Last Updated: "+mSdfDateWithTime.format(mRetainedFragment.mCbsUpdateTime);

                    DialogFragmentWrapper dialog = DialogFragmentWrapper.createNotification(AppConstants.generalInfoTitle, msg, true, false);
                    dialog.setTargetFragment(this, REQ_NOTIFY_ERROR);
                    dialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_CONFIRMATION);
                }
            }

        } catch(Exception e) {
            LogMy.e(TAG, "Exception is CashbackListFragment:onOptionsItemSelected", e);
            // unexpected exception - show error
            DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true);
            notDialog.setTargetFragment(this,REQ_NOTIFY_ERROR);
            notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }

        return super.onOptionsItemSelected(item);
    }

    private void sortMerchantList() {
        try {
            SortMchntDialog dialog = SortMchntDialog.newInstance(mSelectedSortType);
            dialog.setTargetFragment(this, REQ_SORT_CUST_TYPES);
            dialog.show(getFragmentManager(), DIALOG_SORT_CUST_TYPES);

        } catch(Exception e) {
            LogMy.e(TAG, "Exception in sortMerchantList", e);
            // unexpected exception - show error
            DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true);
            notDialog.setTargetFragment(this,REQ_NOTIFY_ERROR);
            notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
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

    private class CbHolder extends RecyclerView.ViewHolder
            implements View.OnTouchListener {

        private MyCashback mCb;

        private View mCardView;
        private View mLayoutMchntItem;
        private ImageView mMerchantDp;
        private EditText mMerchantName;
        private View mMchntStatusAlert;
        private EditText mCategoryNdCity;
        private EditText mLastTxnTime;
        private EditText mAccBalance;
        private EditText mCbBalance;

        public CbHolder(View itemView) {
            super(itemView);

            mCardView = itemView.findViewById(R.id.card_view);
            mLayoutMchntItem = itemView.findViewById(R.id.layout_mchnt_item);
            mMerchantDp = (ImageView) itemView.findViewById(R.id.img_merchant);
            mMerchantName = (EditText) itemView.findViewById(R.id.input_mchnt_name);
            mMchntStatusAlert = itemView.findViewById(R.id.icon_mchnt_status_alert);
            mCategoryNdCity = (EditText) itemView.findViewById(R.id.mchnt_category_city);
            mLastTxnTime = (EditText) itemView.findViewById(R.id.input_last_txn);
            mAccBalance = (EditText) itemView.findViewById(R.id.input_acc_bal);
            mCbBalance = (EditText) itemView.findViewById(R.id.input_cb_bal);

            mCardView.setOnTouchListener(this);
            mLayoutMchntItem.setOnTouchListener(this);
            mMerchantDp.setOnTouchListener(this);
            mMerchantName.setOnTouchListener(this);
            mCategoryNdCity.setOnTouchListener(this);
            mLastTxnTime.setOnTouchListener(this);
            mAccBalance.setOnTouchListener(this);;
            mCbBalance.setOnTouchListener(this);;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            try {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if(!mCallback.getRetainedFragment().getResumeOk())
                        return true;

                    LogMy.d(TAG, "In onTouch: " + v.getId());

                    // getRootView was not working, so manually finding root view
                    // depending upon views on which listener is set
                    View rootView = null;
                    if (v.getId() == mCardView.getId()) {
                        rootView = (View) v.getParent();
                        LogMy.d(TAG, "Clicked first level view " + rootView.getId());

                    } else if (v.getId() == mLayoutMchntItem.getId()) {
                        rootView = (View) v.getParent().getParent();
                        LogMy.d(TAG, "Clicked first-a level view " + rootView.getId());

                    } else if (v.getId() == mMerchantDp.getId()) {
                        rootView = (View) v.getParent().getParent().getParent();
                        LogMy.d(TAG, "Clicked 2nd level view " + rootView.getId());

                    } else if (v.getId() == mCategoryNdCity.getId() || v.getId() == mLastTxnTime.getId()) {
                        rootView = (View) v.getParent().getParent().getParent().getParent().getParent();
                        LogMy.d(TAG, "Clicked 3rd level view " + rootView.getId());

                    } else {
                        rootView = (View) v.getParent().getParent().getParent().getParent().getParent().getParent();
                        LogMy.d(TAG, "Clicked 4th level view " + rootView.getId());
                    }

                    rootView.performClick();
                }
            } catch (Exception e) {
                LogMy.e(TAG, "Exception in Fragment: ", e);
                DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                        .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            }
            return true;
        }

        public void bindCb(MyCashback cb) {
            mCb = cb;
            MyMerchant merchant = mCb.getMerchant();

            Bitmap dp = getMchntDp(merchant.getDpFilename());
            if(dp!=null) {
                mCb.setDpMerchant(dp);
                mMerchantDp.setImageBitmap(dp);
            }

            mMerchantName.setText(merchant.getName());
            if(merchant.getStatus()== DbConstants.USER_STATUS_UNDER_CLOSURE) {
                mMchntStatusAlert.setVisibility(View.VISIBLE);
            } else {
                mMchntStatusAlert.setVisibility(View.GONE);
            }
            String txt = merchant.getBusinessCategory()+", "+merchant.getCity();
            mCategoryNdCity.setText(txt);
            String str = "Last: "+mSdfDateWithTime.format(cb.getLastTxnTime());
            mLastTxnTime.setText(str);
            mAccBalance.setText(AppCommonUtil.getAmtStr(mCb.getCurrClBalance()));
            mCbBalance.setText(AppCommonUtil.getAmtStr(mCb.getCurrCbBalance()));
        }

        private Bitmap getMchntDp(String filename) {
            if(filename!=null) {
                File file = getActivity().getFileStreamPath(filename);
                if(file!=null) {
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
                    if(bitmap==null) {
                        LogMy.e(TAG,"Not able to decode mchnt dp file: "+file.getName());
                    } else {
                        LogMy.d(TAG,"Decoded file as bitmap: "+file.getPath());
                        // convert to round image
                        int radiusInDp = (int) getResources().getDimension(R.dimen.dp_item_image_width);
                        int radiusInPixels = AppCommonUtil.dpToPx(radiusInDp);

                        Bitmap scaledImg = Bitmap.createScaledBitmap(bitmap,radiusInPixels,radiusInPixels,true);
                        Bitmap roundImage = AppCommonUtil.getCircleBitmap(scaledImg);
                        return roundImage;
                    }
                } else {
                    LogMy.e(TAG,"Mchnt Dp file not available locally: "+filename);
                }
            }
            return null;
        }


    }

    private class CbAdapter extends RecyclerView.Adapter<CbHolder> {
        private List<MyCashback> mCbs;
        private View.OnClickListener mListener;

        public CbAdapter(List<MyCashback> cbs) {
            mCbs = cbs;
            mListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LogMy.d(TAG,"In onClickListener of customer list item");
                    int pos = mRecyclerView.getChildAdapterPosition(v);
                    if (pos >= 0 && pos < getItemCount()) {
                        MchntDetailsDialogCustApp dialog = MchntDetailsDialogCustApp.newInstance(mCbs.get(pos).getMerchantId());
                        dialog.show(getFragmentManager(), DIALOG_MERCHANT_DETAILS);
                    } else {
                        LogMy.e(TAG,"Invalid position in onClickListener of customer list item: "+pos);
                    }
                }
            };
        }

        public void refresh(List<MyCashback> cbs) {
            mCbs = cbs;
            notifyDataSetChanged();
        }

        @Override
        public CbHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.merchant_itemview, parent, false);
            view.setOnClickListener(mListener);
            return new CbHolder(view);
        }
        @Override
        public void onBindViewHolder(CbHolder holder, int position) {
            MyCashback cb = mCbs.get(position);
            holder.bindCb(cb);
        }
        @Override
        public int getItemCount() {
            return mCbs.size();
        }
    }
}
