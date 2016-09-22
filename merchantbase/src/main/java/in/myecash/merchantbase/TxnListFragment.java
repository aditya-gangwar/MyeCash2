package in.myecash.merchantbase;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import in.myecash.commonbase.constants.AppConstants;
import in.myecash.commonbase.constants.CommonConstants;
import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.models.Transaction;
import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.DialogFragmentWrapper;
import in.myecash.commonbase.utilities.LogMy;
import in.myecash.merchantbase.entities.MerchantUser;
import in.myecash.merchantbase.entities.MyTransaction;
import in.myecash.merchantbase.helper.MyRetainedFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by adgangwa on 07-04-2016.
 */
public class TxnListFragment extends Fragment {
    private static final String TAG = "TxnListFragment";

    private static final String CSV_REPORT_HEADER_1 = ",,MyeCash Merchant Statement,,,,,,,,";
    // <Store name>,,,,,,,,,,
    private static final String CSV_REPORT_HEADER_2 = "\"=\"\"%s\"\"\",,,,,,,,,,";
    // <address line 1>,,,,,,Merchant Id.,<id>,,,
    private static final String CSV_REPORT_HEADER_3 = "\"=\"\"%s\"\"\",,,,,,,Merchant Id.,%s,,";
    // <City>,,,,,,Period,From <start date> to <end date>,,,
    private static final String CSV_REPORT_HEADER_4 = "%s,,,,,,,Period,From %s to %s,,";
    // <State>,,,,,,Currency,INR,,,
    private static final String CSV_REPORT_HEADER_5 = "%s,,,,,,,Currency,INR,,";
    private static final String CSV_REPORT_HEADER_6 = ",,,,,,,,,,";
    private static final String CSV_REPORT_HEADER_7 = ",,,,,,,,,,";
    private static final String CSV_HEADER = "Sl. No.,Date,Time,Transaction Id,Customer Id,Bill Amount,Cash Account,Cr / Dr,Cashback Debit,Cashback Award,Cashback Rate, PIN used";
    // 5+10+10+10+10+10+5+5+5+5 = 75
    private static final int CSV_RECORD_MAX_CHARS = 100;
    //TODO: change this to 100 in production
    private static final int CSV_LINES_BUFFER = 5;

    private static final String ARG_START_TIME = "startTime";
    private static final String ARG_END_TIME = "endTime";

    private static final int REQ_NOTIFY_ERROR = 1;
    private static final int REQ_SORT_TXN_TYPES = 2;

    private static final String DIALOG_SORT_TXN_TYPES = "dialogSortTxn";
    private static final String DIALOG_TXN_DETAILS = "dialogTxnDetails";

    private SimpleDateFormat mSdfDateWithTime;
    private SimpleDateFormat mSdfOnlyDateCSV;
    private SimpleDateFormat mSdfOnlyTimeCSV;

    private RecyclerView mTxnRecyclerView;
    private MyRetainedFragment mRetainedFragment;
    private TxnListFragmentIf mCallback;
    private Date mStartTime;
    private Date mEndTime;
    // instance state - store and restore
    private int mSelectedSortType;

    public interface TxnListFragmentIf {
        void setToolbarTitle(String title);
        MyRetainedFragment getRetainedFragment();
    }

    public static TxnListFragment getInstance(Date startTime, Date endTime) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_START_TIME, startTime);
        args.putSerializable(ARG_END_TIME, endTime);

        TxnListFragment fragment = new TxnListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LogMy.d(TAG, "In onActivityCreated");

        try {
            mCallback = (TxnListFragmentIf) getActivity();

            mRetainedFragment = mCallback.getRetainedFragment();

            mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
            mSdfOnlyDateCSV = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_CSV, CommonConstants.DATE_LOCALE);
            mSdfOnlyTimeCSV = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_TIME_24_CSV, CommonConstants.DATE_LOCALE);
            //updateUI();

            // get arguments and store in instance
            mStartTime = (Date)getArguments().getSerializable(ARG_START_TIME);
            mEndTime = (Date)getArguments().getSerializable(ARG_END_TIME);
            if(savedInstanceState==null) {
                mSelectedSortType = SortTxnDialog.TXN_SORT_DATE_TIME;
            } else {
                mSelectedSortType = savedInstanceState.getInt("mSelectedSortType");
            }
            sortTxnList();

        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement TxnListFragmentIf");
        }

        setHasOptionsMenu(true);
    }

    private void sortTxnList() {
        switch (mSelectedSortType) {
            case SortTxnDialog.TXN_SORT_DATE_TIME:
                Collections.sort(mRetainedFragment.mLastFetchTransactions, new MyTransaction.TxnDateComparator());
                break;
            case SortTxnDialog.TXN_SORT_bILL_AMT:
                Collections.sort(mRetainedFragment.mLastFetchTransactions, new MyTransaction.TxnBillComparator());
                break;
            case SortTxnDialog.TXN_SORT_CB_AWARD:
                Collections.sort(mRetainedFragment.mLastFetchTransactions, new MyTransaction.TxnCbAwardComparator());
                break;
            case SortTxnDialog.TXN_SORT_CB_REDEEM:
                Collections.sort(mRetainedFragment.mLastFetchTransactions, new MyTransaction.TxnCbRedeemComparator());
                break;
            case SortTxnDialog.TXN_SORT_ACC_ADD:
                Collections.sort(mRetainedFragment.mLastFetchTransactions, new MyTransaction.TxnAccAddComparator());
                break;
            case SortTxnDialog.TXN_SORT_ACC_DEBIT:
                Collections.sort(mRetainedFragment.mLastFetchTransactions, new MyTransaction.TxnAccDebitComparator());
                break;
        }
        // Make it in decreasing order
        Collections.reverse(mRetainedFragment.mLastFetchTransactions);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_txn_list, container, false);

        mTxnRecyclerView = (RecyclerView) view.findViewById(R.id.txn_recycler_view);
        mTxnRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }

    public void showDetailedDialog(int pos) {
        TxnDetailsDialog dialog = TxnDetailsDialog.newInstance(pos);
        dialog.show(getFragmentManager(), DIALOG_TXN_DETAILS);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.txn_list_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //mActiveMenuItemId = item.getItemId();
        int i = item.getItemId();
        if (i == R.id.action_download) {
            downloadReport();
        } else if (i == R.id.action_email) {
            emailReport();
        } else if (i == R.id.action_sort) {
            SortTxnDialog dialog = SortTxnDialog.newInstance(mSelectedSortType);
            dialog.setTargetFragment(this, REQ_SORT_TXN_TYPES);
            dialog.show(getFragmentManager(), DIALOG_SORT_TXN_TYPES);
        }

        return super.onOptionsItemSelected(item);
    }

    private void downloadReport() {
        File file = createCsvReport();
        if(file!=null) {
            // register with download manager, so as can be seen by clicking 'Downloads' icon
            DownloadManager manager = (DownloadManager) getActivity().getSystemService(AppCompatActivity.DOWNLOAD_SERVICE);
            long fileid = manager.addCompletedDownload("MyeCash statement", "MyeCash transactions statement",
                    true, "text/plain", file.getAbsolutePath(), file.length(), true);
        }
    }

    private void emailReport() {
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
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "MyeCash Statement");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Please find attached the requested MyeCash transaction statement.\nThanks.\nRegards,\nMyeCash Team.");
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

    private File createCsvReport() {
        LogMy.d(TAG,"In createCsvReport");

        File file = null;
        try {
            long currentTimeInSecs = Math.abs(System.currentTimeMillis() / 1000);
            String fileName = AppConstants.FILE_PREFIX_TXN_LIST + String.valueOf(currentTimeInSecs) + CommonConstants.CSV_FILE_EXT;

            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            file = new File(dir, fileName);
            FileOutputStream stream = new FileOutputStream(file, false);

            // +10 to cover for headers
            StringBuilder sb = new StringBuilder(CSV_RECORD_MAX_CHARS*(CSV_LINES_BUFFER+10));
            String startDate = mSdfOnlyDateCSV.format(mStartTime);
            String endDate = mSdfOnlyDateCSV.format(mEndTime);

            // append all headers
            MerchantUser user = MerchantUser.getInstance();
            sb.append(CSV_REPORT_HEADER_1).append(CommonConstants.CSV_NEWLINE);
            sb.append(String.format(CSV_REPORT_HEADER_2,user.getMerchantName())).append(CommonConstants.CSV_NEWLINE);
            sb.append(String.format(CSV_REPORT_HEADER_3,user.getMerchant().getAddress().getLine_1(),user.getMerchantId()))
                    .append(CommonConstants.CSV_NEWLINE);
            sb.append(String.format(CSV_REPORT_HEADER_4,user.getMerchant().getAddress().getCity(),startDate,endDate))
                    .append(CommonConstants.CSV_NEWLINE);
            sb.append(String.format(CSV_REPORT_HEADER_5,user.getMerchant().getAddress().getState()))
                    .append(CommonConstants.CSV_NEWLINE);
            sb.append(CSV_REPORT_HEADER_6).append(CommonConstants.CSV_NEWLINE);
            sb.append(CSV_REPORT_HEADER_7).append(CommonConstants.CSV_NEWLINE);

            sb.append(CSV_HEADER).append(CommonConstants.CSV_NEWLINE);

            int cnt = mRetainedFragment.mLastFetchTransactions.size();
            for(int i=0; i<cnt; i++) {
                if(sb==null) {
                    // +1 for buffer
                    sb = new StringBuilder(CSV_RECORD_MAX_CHARS*(CSV_LINES_BUFFER+1));
                }

                // Append CSV record for this txn
                sb.append(i+1).append(CommonConstants.CSV_DELIMETER);

                Transaction txn = mRetainedFragment.mLastFetchTransactions.get(i);
                sb.append(mSdfOnlyDateCSV.format(txn.getCreate_time())).append(CommonConstants.CSV_DELIMETER);
                sb.append(mSdfOnlyTimeCSV.format(txn.getCreate_time())).append(CommonConstants.CSV_DELIMETER);
                sb.append(txn.getTrans_id()).append(CommonConstants.CSV_DELIMETER);
                sb.append(txn.getCustomer_id()).append(CommonConstants.CSV_DELIMETER);
                sb.append(txn.getTotal_billed()).append(CommonConstants.CSV_DELIMETER);

                if(txn.getCl_credit() > 0) {
                    sb.append(txn.getCl_credit()).append(CommonConstants.CSV_DELIMETER);
                    sb.append("CR").append(CommonConstants.CSV_DELIMETER);
                } else if(txn.getCl_debit() > 0) {
                    sb.append(txn.getCl_debit()).append(CommonConstants.CSV_DELIMETER);
                    sb.append("DR").append(CommonConstants.CSV_DELIMETER);
                } else {
                    sb.append("0").append(CommonConstants.CSV_DELIMETER);
                    sb.append(CommonConstants.CSV_DELIMETER);
                }

                if(txn.getCb_debit() > 0) {
                    sb.append(txn.getCb_debit()).append(CommonConstants.CSV_DELIMETER);
                } else {
                    sb.append("0").append(CommonConstants.CSV_DELIMETER);
                }

                if(txn.getCb_credit() > 0) {
                    sb.append(txn.getCb_credit()).append(CommonConstants.CSV_DELIMETER);
                } else {
                    sb.append("0").append(CommonConstants.CSV_DELIMETER);
                }

                sb.append(txn.getCb_percent()).append("%");
                sb.append(txn.getCpin());
                sb.append(CommonConstants.CSV_NEWLINE);

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

            DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, ErrorCodes.appErrorDesc.get(ErrorCodes.GENERAL_ERROR), true, true);
            notDialog.setTargetFragment(this,REQ_NOTIFY_ERROR);
            notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);

            return null;
        }

        return file;
    }

    private void updateUI() {
        mTxnRecyclerView.setAdapter(new TxnAdapter(mRetainedFragment.mLastFetchTransactions));
        mCallback.setToolbarTitle(mRetainedFragment.mLastFetchTransactions.size() + " Transactions");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogMy.d(TAG, "In onActivityResult :" + requestCode + ", " + resultCode);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if(requestCode== REQ_SORT_TXN_TYPES) {
            mSelectedSortType = data.getIntExtra(SortCustDialog.EXTRA_SELECTION, SortTxnDialog.TXN_SORT_DATE_TIME);
            sortTxnList();
            updateUI();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("mSelectedSortType", mSelectedSortType);
    }

    private class TxnHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private Transaction mTxn;

        public EditText mDatetime;
        public EditText mCustId;
        //public EditText mTxnId;

        public EditText mBillAmount;
        public EditText mCashbackAward;
        public View mAccountIcon;
        public EditText mAccountAmt;
        public View mCashbackIcon;
        public EditText mCashbackAmt;

        //public ImageView mSecureIcon;

        public TxnHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mDatetime = (EditText) itemView.findViewById(R.id.txn_time);
            mCustId = (EditText) itemView.findViewById(R.id.txn_customer_id);
            //mTxnId = (EditText) itemView.findViewById(R.id.txn_id);

            mBillAmount = (EditText) itemView.findViewById(R.id.txn_bill);
            mAccountIcon = itemView.findViewById(R.id.txn_account_icon);
            mAccountAmt = (EditText) itemView.findViewById(R.id.txn_account_amt);
            mCashbackIcon = itemView.findViewById(R.id.txn_cashback_icon);
            mCashbackAmt = (EditText) itemView.findViewById(R.id.txn_cashback_amt);

            mCashbackAward = (EditText) itemView.findViewById(R.id.txn_cashback_award);
            //mSecureIcon = (ImageView)itemView.findViewById(R.id.txn_secure_icon);

            mCustId.setOnClickListener(this);
            mDatetime.setOnClickListener(this);
            mBillAmount.setOnClickListener(this);
            mAccountAmt.setOnClickListener(this);
            mCashbackAmt.setOnClickListener(this);
            mCashbackAward.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            LogMy.d(TAG,"In onClick: "+getAdapterPosition());

            // getRootView was not working, so manually finding root view
            View rootView = null;
            if(v.getId()==mCustId.getId() || v.getId()==mDatetime.getId()) {
                rootView = (View) v.getParent().getParent();
            } else {
                rootView = (View) v.getParent().getParent().getParent();
            }
            rootView.performClick();
        }

        public void bindTxn(Transaction txn) {
            mTxn = txn;

            mDatetime.setText(mSdfDateWithTime.format(mTxn.getCreate_time()));
            mCustId.setText(mTxn.getCust_private_id());
            //mTxnId.setText(mTxn.getTrans_id());

            if(mTxn.getTotal_billed() > 0) {
                mBillAmount.setText(AppCommonUtil.getSignedAmtStr(mTxn.getTotal_billed(), true));
            } else {
                mBillAmount.setText("-");
            }

            if(mTxn.getCl_credit() > 0) {
                mAccountAmt.setText(AppCommonUtil.getSignedAmtStr(mTxn.getCl_credit(), true));
                mAccountAmt.setTextColor(ContextCompat.getColor(getActivity(), R.color.green_positive));
            } else if(mTxn.getCl_debit() > 0) {
                mAccountAmt.setText(AppCommonUtil.getSignedAmtStr(mTxn.getCl_debit(), false));
                mAccountAmt.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
            } else {
                mAccountIcon.setVisibility(View.GONE);
                mAccountAmt.setText("-");
            }

            if(mTxn.getCb_debit() > 0) {
                mCashbackAmt.setText(AppCommonUtil.getSignedAmtStr(mTxn.getCl_debit(), false));
                mCashbackAmt.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
            } else {
                mCashbackIcon.setVisibility(View.GONE);
                mCashbackAmt.setText("-");
            }

            if(mTxn.getCb_credit() > 0) {
                String cbData = AppCommonUtil.getAmtStr(mTxn.getCb_credit())+" @ "+mTxn.getCb_percent()+"%";
                mCashbackAward.setText(cbData);
            } else {
                mCashbackAward.setText("-");
            }

            /*
            if(mTxn.getCpin().equals(DbConstants.TXN_CUSTOMER_PIN_NOT_USED)) {
                mSecureIcon.setVisibility(View.GONE);
            }*/
        }
    }

    private class TxnAdapter extends RecyclerView.Adapter<TxnHolder> {
        private List<Transaction> mTxns;
        private View.OnClickListener mListener;

        public TxnAdapter(List<Transaction> txns) {
            mTxns = txns;
            mListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LogMy.d(TAG,"In onClickListener of txn list item");
                    int pos = mTxnRecyclerView.getChildAdapterPosition(v);
                    if (pos >= 0 && pos < getItemCount()) {
                        showDetailedDialog(pos);
                    } else {
                        LogMy.e(TAG,"Invalid position in onClickListener of customer list item: "+pos);
                    }
                }
            };
        }

        @Override
        public TxnHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.txn_itemview, parent, false);
            view.setOnClickListener(mListener);
            return new TxnHolder(view);
        }
        @Override
        public void onBindViewHolder(TxnHolder holder, int position) {
            Transaction txn = mTxns.get(position);
            holder.bindTxn(txn);
        }
        @Override
        public int getItemCount() {
             return mTxns.size();
        }
    }
}


    /*
    private boolean checkPermission() {
        // check for external storage permission
        int rc = ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return (rc == PackageManager.PERMISSION_GRANTED);
    }

    public void requestStoragePermission() {
        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(getActivity(), permissions, REQ_STORAGE_PERMISSION);
            return;
        }

        // show permission rationale
        DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification(AppConstants.generalInfoTitle,
                getString(R.string.permission_write_storage_rationale), true, false);
        notDialog.setTargetFragment(this, REQ_STORAGE_PERMISSION);
        notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_WRITE_STORAGE) {
            LogMy.w(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // permission granted - complete invoked action
            switch (mActiveMenuItemId) {
                case R.id.action_download:
                    downloadReport();
                    break;

                case R.id.action_email:
                    emailReport();
                    break;
            }
        } else {
            LogMy.i(TAG, "Permission not granted: results len = " + grantResults.length +
                    " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

            DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification(AppConstants.noPermissionTitle,
                    "Cannot download/email reports as write storage permission not provided.",
                    true, true);
            notDialog.setTargetFragment(this,REQ_NOTIFY_ERROR);
            notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("mActiveMenuItemId", mActiveMenuItemId);
    }
    */
