package in.myecash.merchantbase;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import in.myecash.appbase.BaseFragment;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.common.CommonUtils;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.MyGlobalSettings;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.merchantbase.entities.MerchantUser;
import in.myecash.appbase.entities.MyCashback;
import in.myecash.common.MyCustomer;
import in.myecash.merchantbase.helper.MyRetainedFragment;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by adgangwa on 09-09-2016.
 */
public class CustomerListFragment extends BaseFragment {
    private static final String TAG = "MchntApp-CustomerListFragment";
    private static final String DIALOG_CUSTOMER_DETAILS = "dialogCustomerDetails";
    private static final String DIALOG_SORT_CUST_TYPES = "dialogSortCust";

    private static final String CSV_HEADER = "Sl.No.,Customer ID,Mobile No.,Card ID,Status,Account Balance,Account Add,Account Debit,Cashback Balance,Cashback Award,Cashback Redeem,Total Bill,Cashback Bill,Last Txn here,First Txn here";
    // 5+10+10+10+10+5+5+5+5+5+5+5+5+10+10 = 105
    private static final int CSV_RECORD_MAX_CHARS = 128;
    private static final int CSV_LINES_BUFFER = 100;

    private static final int REQ_NOTIFY_ERROR = 1;
    private static final int REQ_SORT_CUST_TYPES = 2;

    private SimpleDateFormat mSdfDateWithTime;
    private SimpleDateFormat mSdfOnlyDateFilename;

    private MyRetainedFragment mRetainedFragment;
    private CustomerListFragmentIf mCallback;

    public interface CustomerListFragmentIf {
        MyRetainedFragment getRetainedFragment();
        void setDrawerState(boolean isEnabled);
    }

    // instance state - store and restore
    private int mSelectedSortType;
    private String mUpdatedTime;

    private RecyclerView mCustRecyclerView;
    private EditText mLabelTxnTime;
    private EditText mLabelBill;
    private EditText mLabelAcc;
    private EditText mLabelCb;
    private EditText mUpdated;
    private EditText mUpdatedDetail;
    

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LogMy.d(TAG, "In onActivityCreated");

        try {
            mCallback = (CustomerListFragmentIf) getActivity();
            mRetainedFragment = mCallback.getRetainedFragment();

            mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
            mSdfOnlyDateFilename = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_FILENAME, CommonConstants.DATE_LOCALE);

            if(mRetainedFragment.mLastFetchCashbacks==null) {
                mRetainedFragment.mLastFetchCashbacks = new ArrayList<>();
            } else {
                mRetainedFragment.mLastFetchCashbacks.clear();
            }

            try {
                // process the file
                processCustFile();
            } catch(Exception e) {
                // if any issue processing the file - delete the same
                String fileName = AppCommonUtil.getMerchantCustFileName(mRetainedFragment.mMerchantUser.getMerchantId());
                LogMy.e(TAG, "Failed to process the file: "+fileName);
                getActivity().deleteFile(fileName);
                throw e;
            }

            int sortType = MyCashback.CB_CMP_TYPE_UPDATE_TIME;
            if(savedInstanceState!=null) {
                sortType = savedInstanceState.getInt("mSelectedSortType");
            }
            sortCustList(sortType);

            // update time
            mUpdated.setText(mUpdatedTime);
            int hours = CommonUtils.roundUpTo(MyGlobalSettings.getMchntDashBNoRefreshMins(), 60);
            String txt = "Data is updated only once every "+hours+" hours.";
            mUpdatedDetail.setText(txt);

        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement CustomerListFragmentIf");

        } catch(Exception e) {
            LogMy.e(TAG, "Exception is CustomerListFragment:onActivityCreated", e);
            // unexpected exception - show error
            DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true);
            notDialog.setTargetFragment(this,REQ_NOTIFY_ERROR);
            notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            getActivity().onBackPressed();
        }

        setHasOptionsMenu(true);
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

    private void sortCustList(int sortType) {
        Collections.sort(mRetainedFragment.mLastFetchCashbacks, new MyCashback.MyCashbackComparator(sortType));
        // Make it in decreasing order
        Collections.reverse(mRetainedFragment.mLastFetchCashbacks);

        // Remove arrow as per old sort type
        switch (mSelectedSortType) {
            case MyCashback.CB_CMP_TYPE_UPDATE_TIME:
                mLabelTxnTime.setText("Last Txn Time");
                mLabelTxnTime.setTypeface(null, Typeface.NORMAL);
                break;
            case MyCashback.CB_CMP_TYPE_BILL_AMT:
                mLabelBill.setText("Total Bill");
                mLabelBill.setTypeface(null, Typeface.NORMAL);
                break;
            case MyCashback.CB_CMP_TYPE_ACC_BALANCE:
            case MyCashback.CB_CMP_TYPE_ACC_ADD:
            case MyCashback.CB_CMP_TYPE_ACC_DEBIT:
                mLabelAcc.setText("Account:  Add - Used = Balance");
                mLabelAcc.setTypeface(null, Typeface.NORMAL);
                break;
            case MyCashback.CB_CMP_TYPE_CB_BALANCE:
            case MyCashback.CB_CMP_TYPE_CB_ADD:
            case MyCashback.CB_CMP_TYPE_CB_DEBIT:
                mLabelCb.setText("Cashback:  Add - Used = Balance");
                mLabelCb.setTypeface(null, Typeface.NORMAL);
                break;
        }

        // Add arrow in header as per new sort type
        String text = null;
        switch (sortType) {
            case MyCashback.CB_CMP_TYPE_UPDATE_TIME:
                text = AppConstants.SYMBOL_DOWN_ARROW + mLabelTxnTime.getText().toString();
                mLabelTxnTime.setText(text);
                mLabelTxnTime.setTypeface(null, Typeface.BOLD);
                break;
            case MyCashback.CB_CMP_TYPE_BILL_AMT:
                text = AppConstants.SYMBOL_DOWN_ARROW + mLabelBill.getText().toString();
                mLabelBill.setText(text);
                mLabelBill.setTypeface(null, Typeface.BOLD);
                break;
            case MyCashback.CB_CMP_TYPE_ACC_BALANCE:
                text = "Account:  Add - Used = "+AppConstants.SYMBOL_DOWN_ARROW+"Balance";
                mLabelAcc.setText(text);
                mLabelAcc.setTypeface(null, Typeface.BOLD);
                break;
            case MyCashback.CB_CMP_TYPE_ACC_ADD:
                text = "Account: "+AppConstants.SYMBOL_DOWN_ARROW+"Add - Used = Balance";
                mLabelAcc.setText(text);
                mLabelAcc.setTypeface(null, Typeface.BOLD);
                break;
            case MyCashback.CB_CMP_TYPE_ACC_DEBIT:
                text = "Account:  Add - "+AppConstants.SYMBOL_DOWN_ARROW+"Used = Balance";
                mLabelAcc.setText(text);
                mLabelAcc.setTypeface(null, Typeface.BOLD);
                break;
            case MyCashback.CB_CMP_TYPE_CB_BALANCE:
                text = "Cashback:  Add - Used = "+AppConstants.SYMBOL_DOWN_ARROW+"Balance";
                mLabelCb.setText(text);
                mLabelCb.setTypeface(null, Typeface.BOLD);
                break;
            case MyCashback.CB_CMP_TYPE_CB_ADD:
                text = "Cashback: "+AppConstants.SYMBOL_DOWN_ARROW+"Add - Used = Balance";
                mLabelCb.setText(text);
                mLabelCb.setTypeface(null, Typeface.BOLD);
                break;
            case MyCashback.CB_CMP_TYPE_CB_DEBIT:
                text = "Cashback:  Add - "+AppConstants.SYMBOL_DOWN_ARROW+"Used = Balance";
                mLabelCb.setText(text);
                mLabelCb.setTypeface(null, Typeface.BOLD);
                break;
        }

        // store existing sortType
        mSelectedSortType = sortType;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_list, container, false);

        mCustRecyclerView = (RecyclerView) view.findViewById(R.id.cust_recycler_view);
        mCustRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mLabelTxnTime = (EditText) view.findViewById(R.id.list_header_txnTime);
        mLabelBill = (EditText) view.findViewById(R.id.list_header_bill);
        mLabelAcc = (EditText) view.findViewById(R.id.list_header_acc);
        mLabelCb = (EditText) view.findViewById(R.id.list_header_cb);

        mUpdated = (EditText) view.findViewById(R.id.input_updated_time);
        mUpdatedDetail = (EditText) view.findViewById(R.id.updated_time_details);

        return view;
    }

    private void processCustFile() throws Exception {
        String fileName = AppCommonUtil.getMerchantCustFileName(mRetainedFragment.mMerchantUser.getMerchantId());

        byte[] bytes = AppCommonUtil.fileAsByteArray(getActivity(), fileName);
        //LogMy.d(TAG,"Encoded "+fileName+": "+new String(bytes));
        byte[] decodedBytes = Base64.decode(bytes, Base64.DEFAULT);
        //LogMy.d(TAG,"Decoded "+fileName+": "+new String(decodedBytes));

        InputStream is = new ByteArrayInputStream(decodedBytes);
        BufferedReader bfReader = new BufferedReader(new InputStreamReader(is));

        int lineCnt = 0;
        String receiveString = "";
        while ( (receiveString = bfReader.readLine()) != null ) {
            //LogMy.d(TAG,"Read line: "+receiveString);
            if(lineCnt==0) {
                // first line is header giving file creation time epoch
                String[] csvFields = receiveString.split(CommonConstants.CSV_DELIMETER, -1);
                mUpdatedTime = mSdfDateWithTime.format(new Date(Long.parseLong(csvFields[0])));
            } else {
                // ignore empty lines
                if(receiveString.trim().isEmpty()) {
                    LogMy.d(TAG, "Read empty line");
                } else {
                    processCbCsvRecord(receiveString);
                }
            }

            lineCnt++;
        }
        is.close();
        LogMy.d(TAG,"Processed "+lineCnt+" lines from "+fileName);
    }

    private void processCbCsvRecord(String csvString) {
        MyCashback cb = new MyCashback();
        cb.init(csvString, true);
        mRetainedFragment.mLastFetchCashbacks.add(cb);
        LogMy.d(TAG,"Added new item in mLastFetchCashbacks: "+mRetainedFragment.mLastFetchCashbacks.size());
    }

    private void updateUI() {
        if(mRetainedFragment.mLastFetchCashbacks!=null) {
            mCustRecyclerView.setAdapter(new CbAdapter(mRetainedFragment.mLastFetchCashbacks));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogMy.d(TAG, "In onActivityResult :" + requestCode + ", " + resultCode);
        try {
            if (resultCode != Activity.RESULT_OK) {
                return;
            }
            if (requestCode == REQ_SORT_CUST_TYPES) {
                int sortType = data.getIntExtra(SortCustDialog.EXTRA_SELECTION, MyCashback.CB_CMP_TYPE_UPDATE_TIME);
                sortCustList(sortType);
                updateUI();
            }
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in CustomerListFragment:onActivityResult", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    @Override
    public void onResume() {
        LogMy.d(TAG,"In Resume");
        super.onResume();
        mCallback.setDrawerState(false);
        updateUI();
        mCallback.getRetainedFragment().setResumeOk(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("mSelectedSortType", mSelectedSortType);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.customer_list_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(mCallback.getRetainedFragment().getResumeOk()) {
            try {
                int i = item.getItemId();
                if (i == R.id.action_download) {
                    downloadReport();

                } else if (i == R.id.action_email) {
                    emailReport();

                } else if (i == R.id.action_sort) {
                    SortCustDialog dialog = SortCustDialog.newInstance(mSelectedSortType);
                    dialog.setTargetFragment(this, REQ_SORT_CUST_TYPES);
                    dialog.show(getFragmentManager(), DIALOG_SORT_CUST_TYPES);
                }

            } catch (Exception e) {
                LogMy.e(TAG, "Exception is CustomerListFragment:onOptionsItemSelected", e);
                // unexpected exception - show error
                DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true);
                notDialog.setTargetFragment(this, REQ_NOTIFY_ERROR);
                notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void downloadReport() throws IOException {
        File file = createCsvReport();
        if(file!=null) {
            // register with download manager, so as can be seen by clicking 'Downloads' icon
            DownloadManager manager = (DownloadManager) getActivity().getSystemService(AppCompatActivity.DOWNLOAD_SERVICE);
            long fileid = manager.addCompletedDownload("MyeCash Customer Data", "MyeCash Customer data file",
                    true, "text/plain", file.getAbsolutePath(), file.length(), true);
        }
    }

    private void emailReport() throws IOException {
        File csvFile = createCsvReport();
        if(csvFile != null) {
            // create intent for email
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            // The intent does not have a URI, so declare the "text/plain" MIME type
            emailIntent.setType("text/csv");
            String emailId = MerchantUser.getInstance().getMerchant().getEmail();
            if(emailId!=null && emailId.length()>0) {
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {emailId}); // recipients
            }
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "MyeCash Customer Data");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Please find attached the requested MyeCash customer data file.\nThanks.\nRegards,\nMyeCash Team.");
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(csvFile));

            // check if there's activity available for the intent
            PackageManager packageManager = getActivity().getPackageManager();
            List activities = packageManager.queryIntentActivities(emailIntent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            boolean isIntentSafe = activities.size() > 0;

            if(isIntentSafe) {
                startActivity(emailIntent);
            } else {
                DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle,
                        "No Email App available to send the email. Please install any and try again.", true, true);
                notDialog.setTargetFragment(this,REQ_NOTIFY_ERROR);
                notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            }
        }
    }

    private File createCsvReport() throws IOException {
        LogMy.d(TAG,"In createCsvReport");

        File file = null;
        try {
            String fileName = AppConstants.FILE_PREFIX_CUSTOMER_LIST +
                    mSdfOnlyDateFilename.format(new Date()) +
                    CommonConstants.CSV_FILE_EXT;

            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            file = new File(dir, fileName);
            FileOutputStream stream = new FileOutputStream(file, false);

            // +10 to cover for headers
            StringBuilder sb = new StringBuilder(CSV_RECORD_MAX_CHARS*(CSV_LINES_BUFFER+10));
            sb.append(CSV_HEADER).append(CommonConstants.NEWLINE_SEP);

            int cnt = mRetainedFragment.mLastFetchCashbacks.size();
            for(int i=0; i<cnt; i++) {
                if(sb==null) {
                    // +1 for buffer
                    sb = new StringBuilder(CSV_RECORD_MAX_CHARS*(CSV_LINES_BUFFER+1));
                }

                MyCashback cb = mRetainedFragment.mLastFetchCashbacks.get(i);
                MyCustomer cust = cb.getCustomer();
                // Append CSV record for this txn
                sb.append(i+1).append(CommonConstants.CSV_DELIMETER);
                sb.append(cust.getPrivateId()).append(CommonConstants.CSV_DELIMETER);
                sb.append(cust.getMobileNum()).append(CommonConstants.CSV_DELIMETER);
                sb.append(CommonUtils.getPartialVisibleStr(cust.getCardId())).append(CommonConstants.CSV_DELIMETER);
                sb.append(DbConstants.userStatusDesc[cust.getStatus()]).append(CommonConstants.CSV_DELIMETER);
                sb.append(cb.getCurrClBalance()).append(CommonConstants.CSV_DELIMETER);
                sb.append(cb.getClCredit()).append(CommonConstants.CSV_DELIMETER);
                sb.append(cb.getClDebit()).append(CommonConstants.CSV_DELIMETER);
                sb.append(cb.getCurrCbBalance()).append(CommonConstants.CSV_DELIMETER);
                sb.append(cb.getCbCredit()).append(CommonConstants.CSV_DELIMETER);
                sb.append(cb.getCbRedeem()).append(CommonConstants.CSV_DELIMETER);
                sb.append(cb.getBillAmt()).append(CommonConstants.CSV_DELIMETER);
                sb.append(mSdfDateWithTime.format(cb.getLastTxnTime())).append(CommonConstants.CSV_DELIMETER);
                sb.append(mSdfDateWithTime.format(cb.getCreateTime()));
                sb.append(CommonConstants.NEWLINE_SEP);

                // Write every 100 records in one go to the file
                if(i%CSV_LINES_BUFFER == 0) {
                    stream.write(sb.toString().getBytes());
                    LogMy.d(TAG,"Written "+String.valueOf(i+1)+"records to "+file.getAbsolutePath());
                    sb = null;
                }
            }

            // write remaining records
            if(sb!=null) {
                stream.write(sb.toString().getBytes());
                LogMy.d(TAG,"Written pending records to "+file.getAbsolutePath());
                sb = null;
            }

            stream.close();

        } catch(Exception e) {
            LogMy.e(TAG,"exception in createCsvReport: "+e.toString());
            if(file!=null) {
                // delete it
                file.delete();
            }
            throw e;
        }

        return file;
    }

    private class CbHolder extends RecyclerView.ViewHolder {

        private MyCashback mCb;

        public TextView mCustId;
        public TextView mLastTxnTime;

        //public View mLayoutBill;
        public TextView mBillAmt;

        //public View mLayoutAcc;
        public TextView mAccAdd;
        public TextView mAccDebit;
        public TextView mAccBalance;

        //public View mLayoutCb;
        public TextView mCbAdd;
        public TextView mCbDebit;
        public TextView mCbBalance;

        public View mLayout;

        public CbHolder(View itemView) {
            super(itemView);

            mCustId = (TextView) itemView.findViewById(R.id.input_cust_id);
            mLastTxnTime = (TextView) itemView.findViewById(R.id.input_last_txn);

            //mLayoutBill = itemView.findViewById(R.id.layout_bill);
            mBillAmt = (TextView) itemView.findViewById(R.id.cust_bill_amt);

            //mLayoutAcc = itemView.findViewById(R.id.layout_account);
            mAccAdd = (TextView) itemView.findViewById(R.id.cust_acc_credit);
            mAccDebit = (TextView) itemView.findViewById(R.id.cust_acc_debit);
            mAccBalance = (TextView) itemView.findViewById(R.id.cust_acc_balance);

            //mLayoutCb = itemView.findViewById(R.id.layout_cashback);
            mCbAdd = (TextView) itemView.findViewById(R.id.cust_cb_credit);
            mCbDebit = (TextView) itemView.findViewById(R.id.cust_cb_debit);
            mCbBalance = (TextView) itemView.findViewById(R.id.cust_cb_balance);

            /*mCustId.setOnTouchListener(this);
            mLastTxnTime.setOnTouchListener(this);
            mBillAmt.setOnTouchListener(this);;
            mAccAdd.setOnTouchListener(this);;
            mAccDebit.setOnTouchListener(this);;
            mAccBalance.setOnTouchListener(this);;
            mCbAdd.setOnTouchListener(this);;
            mCbDebit.setOnTouchListener(this);;
            mCbBalance.setOnTouchListener(this);;*/

            //mLayoutBill.setOnClickListener(this);
            //mLayoutAcc.setOnClickListener(this);
            //mLayoutCb.setOnClickListener(this);

            //itemView.setOnClickListener(this);
            //mLayout = itemView.findViewById(R.id.layout_card);
            //mLayout.setOnClickListener(this);
        }

        /*@Override
        public boolean onTouch(View v, MotionEvent event) {
            if(!mCallback.getRetainedFragment().getResumeOk())
                return false;

            if(event.getAction()==MotionEvent.ACTION_UP) {
                LogMy.d(TAG,"In onTouch: "+v.getId());

            // getRootView was not working, so manually finding root view
            // depending upon views on which listener is set
            //View rootView = (View) v.getParent().getParent();
            View rootView = null;
            if(v.getId()==mCustId.getId() || v.getId()==mLastTxnTime.getId()) {
                rootView = (View) v.getParent().getParent();
                LogMy.d(TAG,"Clicked first level view "+rootView.getId());
            } else {
                rootView = (View) v.getParent().getParent().getParent();
                LogMy.d(TAG,"Clicked second level view "+rootView.getId());
            }

            rootView.performClick();
            }
            return true;
        }*/

        /*@Override
        public void onClick(View v) {
            LogMy.d(TAG,"In onClickListener of customer list item");
            //v.setSelected(true);
            //new Handler().postDelayed(() -> v.setSelected(false), 100);
            //v.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.list_highlight));
            int pos = getAdapterPosition();
            CustomerDetailsDialog dialog = CustomerDetailsDialog.newInstance(pos, true);
            dialog.show(getFragmentManager(), DIALOG_CUSTOMER_DETAILS);
        }*/

        public void bindCb(MyCashback cb) {
            LogMy.d(TAG,"In bindCb");
            mCb = cb;
            MyCustomer customer = mCb.getCustomer();

            mCustId.setText(customer.getPrivateId());
            mLastTxnTime.setText(mSdfDateWithTime.format(cb.getLastTxnTime()));
            mBillAmt.setText(AppCommonUtil.getAmtStr(cb.getBillAmt()));

            mAccAdd.setText(AppCommonUtil.getAmtStr(cb.getClCredit()));
            mAccDebit.setText(AppCommonUtil.getAmtStr(cb.getClDebit()));
            mAccBalance.setText(AppCommonUtil.getAmtStr(mCb.getCurrClBalance()));

            mCbAdd.setText(AppCommonUtil.getAmtStr(cb.getCbCredit()));
            mCbDebit.setText(AppCommonUtil.getAmtStr(cb.getCbRedeem()));
            mCbBalance.setText(AppCommonUtil.getAmtStr(mCb.getCurrCbBalance()));
        }
    }

    private class CbAdapter extends RecyclerView.Adapter<CbHolder>{
        private List<MyCashback> mCbs;
        private int selected_position = -1;
        private View.OnClickListener mListener;

        public CbAdapter(List<MyCashback> cbs) {
            mCbs = cbs;
            mListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LogMy.d(TAG,"In onClickListener of customer list item");
                    int pos = mCustRecyclerView.getChildAdapterPosition(v);

                    // Updating old as well as new positions
                    notifyItemChanged(selected_position);
                    selected_position = pos;
                    notifyItemChanged(selected_position);

                    if (pos >= 0 && pos < getItemCount()) {
                        CustomerDetailsDialog dialog = CustomerDetailsDialog.newInstance(pos, true);
                        dialog.show(getFragmentManager(), DIALOG_CUSTOMER_DETAILS);
                    } else {
                        LogMy.e(TAG,"Invalid position in onClickListener of customer list item: "+pos);
                    }
                }
            };
        }

        @Override
        public CbHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LogMy.d(TAG,"In CbHolder");
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.customer_itemview, parent, false);
            //view.setOnClickListener(mListener);
            return new CbHolder(view);
        }
        @Override
        public void onBindViewHolder(CbHolder holder, int position) {
            LogMy.d(TAG,"In onBindViewHolder");
            MyCashback cb = mCbs.get(position);

            if(selected_position == position){
                // Here I am just highlighting the background
                holder.itemView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.list_highlight2));
            }else{
                holder.itemView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.white));
            }

            holder.itemView.setOnClickListener(mListener);

            holder.bindCb(cb);
        }
        @Override
        public int getItemCount() {
            return mCbs.size();
        }
    }
}
