package in.myecash.customerbase;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
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

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import in.myecash.appbase.SortTxnDialog;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.entities.MyTransaction;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.common.MyGlobalSettings;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.Customers;
import in.myecash.common.database.Transaction;
import in.myecash.customerbase.entities.CustomerUser;
import in.myecash.customerbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 07-04-2016.
 */
public class TxnListFragment extends Fragment {
    private static final String TAG = "TxnListFragment";

    private static final String CSV_REPORT_HEADER_1 = ",,MyeCash Customer Statement,,,,,,,,,,,,,";
    // Merchant Id.,<id>,,,,,,,,,
    private static final String CSV_REPORT_HEADER_2 = "\"=\"\"Customer Id.\"\"\",%s,,,,,,,,,,,,,";
    // Card Num.,<card id>,,,,,,,,,
    private static final String CSV_REPORT_HEADER_3 = "\"=\"\"Membership Card.\"\"\",%s,,,,,,,,,,,,,";
    // ,,,,,,Period,From <start date> to <end date>,,,
    private static final String CSV_REPORT_HEADER_4 = ",,,,,,,Period,From %s to %s,,";
    // ,,,,,,Currency,INR,,,
    private static final String CSV_REPORT_HEADER_5 = ",,,,,,,Currency,INR,,";
    private static final String CSV_REPORT_HEADER_6 = ",,,,,,,,,,";
    private static final String CSV_REPORT_HEADER_7 = ",,,,,,,,,,";
    private static final String CSV_HEADER = "Sl. No.,Date,Time,Transaction Id,Merchant Id,Merchant Name,Bill Amount,Account Debit,Account Credit,Cashback Redeem,Cashback Award,Cashback Rate, Card Used, PIN used";
    // 5+10+10+10+10+10+10+5+5+5+5 = 85
    private static final int CSV_RECORD_MAX_CHARS = 128;
    //TODO: change this to 100 in production
    private static final int CSV_LINES_BUFFER = 5;

    private static final String ARG_START_TIME = "startTime";
    private static final String ARG_END_TIME = "endTime";
    private static final String ARG_FILTER_MCHNT = "filterMchnt";

    private static final int REQ_NOTIFY_ERROR = 1;
    private static final int REQ_SORT_TXN_TYPES = 2;

    private static final String DIALOG_SORT_TXN_TYPES = "dialogSortTxn";
    private static final String DIALOG_TXN_DETAILS = "dialogTxnDetails";

    private SimpleDateFormat mSdfDateWithTime;
    private SimpleDateFormat mSdfOnlyDateCSV;
    private SimpleDateFormat mSdfOnlyTimeCSV;
    private SimpleDateFormat mSdfOnlyDate;

    private EditText mFilterMchnt;
    private EditText mFilterDuration;
    private EditText mHeaderBill;
    private EditText mHeaderAmts;
    private EditText mHeaderTime;
    private EditText mHeaderMchnt;
    private RecyclerView mTxnRecyclerView;
    private EditText mInfoOldTxns;

    private MyRetainedFragment mRetainedFragment;
    private TxnListFragmentIf mCallback;
    private Date mStartTime;
    private Date mEndTime;
    private Boolean mForSingleMchnt;
    // instance state - store and restore
    private int mSelectedSortType;

    public interface TxnListFragmentIf {
        void setToolbarTitle(String title);
        MyRetainedFragment getRetainedFragment();
    }

    public static TxnListFragment getInstance(Date startTime, Date endTime, String mchntName) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_START_TIME, startTime);
        args.putSerializable(ARG_END_TIME, endTime);
        if(mchntName!=null) {
            args.putString(ARG_FILTER_MCHNT, mchntName);
        }

        TxnListFragment fragment = new TxnListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_txn_list, container, false);

        mFilterMchnt = (EditText) view.findViewById(R.id.txnlist_filter_mchnt);
        mFilterDuration = (EditText) view.findViewById(R.id.txnlist_filter_duration);

        mHeaderTime = (EditText) view.findViewById(R.id.txnlist_header_time);
        mHeaderAmts = (EditText) view.findViewById(R.id.txnlist_header_amts);
        mHeaderBill = (EditText) view.findViewById(R.id.txnlist_header_bill);
        mHeaderMchnt = (EditText) view.findViewById(R.id.txnlist_header_mchnt);

        mTxnRecyclerView = (RecyclerView) view.findViewById(R.id.txn_recycler_view);
        mTxnRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mInfoOldTxns = (EditText) view.findViewById(R.id.info_old_txns);

        return view;
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
            mSdfOnlyDate = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_DISPLAY, CommonConstants.DATE_LOCALE);
            //updateUI();

            // get arguments and store in instance
            mStartTime = (Date)getArguments().getSerializable(ARG_START_TIME);
            mEndTime = (Date)getArguments().getSerializable(ARG_END_TIME);

            // show filters
            String filterMchnt = getArguments().getString(ARG_FILTER_MCHNT);
            if(filterMchnt!=null) {
                // Txns are for particular merchant only
                mFilterMchnt.setVisibility(View.VISIBLE);
                mFilterMchnt.setText(filterMchnt);
                mForSingleMchnt = true;
                mInfoOldTxns.setVisibility(View.GONE);
                mHeaderMchnt.setVisibility(View.GONE);

                String durationFilter = "From: "+mSdfOnlyDate.format(mStartTime)+
                        ",  To: "+mSdfOnlyDate.format(mEndTime);
                mFilterDuration.setText(durationFilter);

            } else {
                mFilterMchnt.setVisibility(View.GONE);
                mForSingleMchnt = false;
                mInfoOldTxns.setVisibility(View.VISIBLE);
                mHeaderMchnt.setVisibility(View.VISIBLE);

                String durationFilter = "Duration: Last "+ MyGlobalSettings.getTxnsIntableKeepDays()+" days";
                mFilterDuration.setText(durationFilter);
            }


            int sortType = SortTxnDialog.TXN_SORT_DATE_TIME;
            if(savedInstanceState!=null) {
                sortType = savedInstanceState.getInt("mSelectedSortType");
            }
            sortTxnList(sortType);

        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement TxnListFragmentIf");
        }

        setHasOptionsMenu(true);
    }

    private void sortTxnList(int sortType) {
        switch (sortType) {
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

        // Remove arrow as per old sort type
        switch (mSelectedSortType) {
            case SortTxnDialog.TXN_SORT_DATE_TIME:
                mHeaderTime.setText("Date Time");
                break;
            case SortTxnDialog.TXN_SORT_CB_AWARD:
            case SortTxnDialog.TXN_SORT_bILL_AMT:
                mHeaderBill.setText("Total Bill  |  Cashback @ x%");
                break;
            case SortTxnDialog.TXN_SORT_CB_REDEEM:
            case SortTxnDialog.TXN_SORT_ACC_ADD:
            case SortTxnDialog.TXN_SORT_ACC_DEBIT:
                mHeaderAmts.setText("Account |  Cashback Redeem");
                break;
        }

        // Add arrow in header as per new sort type
        String text = null;
        switch (sortType) {
            case SortTxnDialog.TXN_SORT_DATE_TIME:
                text = AppConstants.SYMBOL_DOWN_ARROW + "Date Time";
                mHeaderTime.setText(text);
                break;
            case SortTxnDialog.TXN_SORT_bILL_AMT:
                text = AppConstants.SYMBOL_DOWN_ARROW + "Total Bill  |  Cashback @ x%";
                mHeaderBill.setText(text);
                break;
            case SortTxnDialog.TXN_SORT_CB_AWARD:
                text = "Total Bill  | "+AppConstants.SYMBOL_DOWN_ARROW+"Cashback @ x%";
                mHeaderBill.setText(text);
                break;
            case SortTxnDialog.TXN_SORT_CB_REDEEM:
                text = "Account  | "+AppConstants.SYMBOL_DOWN_ARROW+"Cashback Redeem";
                mHeaderAmts.setText(text);
                break;
            case SortTxnDialog.TXN_SORT_ACC_ADD:
            case SortTxnDialog.TXN_SORT_ACC_DEBIT:
                text = AppConstants.SYMBOL_DOWN_ARROW+"Account  |  Cashback Redeem";
                mHeaderAmts.setText(text);
                break;
        }

        // store existing sortType
        mSelectedSortType = sortType;
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

            SimpleDateFormat ddMM = new SimpleDateFormat(CommonConstants.DATE_FORMAT_DDMM, CommonConstants.DATE_LOCALE);
            String startDate = ddMM.format(mStartTime);
            String endDate = ddMM.format(mEndTime);

            String fileName = "MyeCash_Statement_"+startDate+"_"+endDate+CommonConstants.CSV_FILE_EXT;
            long fileid = manager.addCompletedDownload(fileName, "MyeCash transactions statement",
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
            /*String emailId = MerchantUser.getInstance().getMerchant().getEmail();
            if(emailId!=null && emailId.length()>0) {
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {emailId}); // recipients
            }*/
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "MyeCash Customer Statement");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Dear Sir - \n\nPlease find attached the requested MyeCash transaction statement.\n\nThanks.\n\nRegards,\nMyeCash Team.");
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
            Customers user = CustomerUser.getInstance().getCustomer();
            sb.append(CSV_REPORT_HEADER_1).append(CommonConstants.CSV_NEWLINE);
            sb.append(String.format(CSV_REPORT_HEADER_2,user.getMobile_num())).append(CommonConstants.CSV_NEWLINE);
            sb.append(String.format(CSV_REPORT_HEADER_3,user.getMembership_card().getCard_id())).append(CommonConstants.CSV_NEWLINE);
            sb.append(String.format(CSV_REPORT_HEADER_4,startDate,endDate)).append(CommonConstants.CSV_NEWLINE);
            sb.append(CSV_REPORT_HEADER_5).append(CommonConstants.CSV_NEWLINE);
            sb.append(CSV_REPORT_HEADER_6).append(CommonConstants.CSV_NEWLINE);
            sb.append(CSV_REPORT_HEADER_7).append(CommonConstants.CSV_NEWLINE);

            sb.append(CSV_HEADER).append(CommonConstants.CSV_NEWLINE);

            int billTotal = 0;
            int accDebitTotal = 0;
            int accCreditTotal = 0;
            int cbRedeemTotal = 0;
            int cbAwardTotal = 0;

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
                sb.append(txn.getMerchant_id()).append(CommonConstants.CSV_DELIMETER);
                sb.append(txn.getMerchant_name()).append(CommonConstants.CSV_DELIMETER);
                if(txn.getTotal_billed() > 0) {
                    sb.append(txn.getTotal_billed()).append(CommonConstants.CSV_DELIMETER);
                    billTotal = billTotal + txn.getTotal_billed();
                } else {
                    sb.append("0").append(CommonConstants.CSV_DELIMETER);
                }

                /*if(txn.getCl_credit() > 0) {
                    sb.append(txn.getCl_credit()).append(CommonConstants.CSV_DELIMETER);
                    sb.append("CR").append(CommonConstants.CSV_DELIMETER);
                } else if(txn.getCl_debit() > 0) {
                    sb.append(txn.getCl_debit()).append(CommonConstants.CSV_DELIMETER);
                    sb.append("DR").append(CommonConstants.CSV_DELIMETER);
                } else {
                    sb.append("0").append(CommonConstants.CSV_DELIMETER);
                    sb.append(CommonConstants.CSV_DELIMETER);
                }*/

                if(txn.getCl_debit() > 0) {
                    sb.append(txn.getCl_debit()).append(CommonConstants.CSV_DELIMETER);
                    accDebitTotal = accDebitTotal +txn.getCl_debit();
                } else {
                    sb.append("0").append(CommonConstants.CSV_DELIMETER);
                }
                if(txn.getCl_credit() > 0) {
                    sb.append(txn.getCl_credit()).append(CommonConstants.CSV_DELIMETER);
                    accCreditTotal = accCreditTotal +txn.getCl_credit();
                } else {
                    sb.append("0").append(CommonConstants.CSV_DELIMETER);
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

                sb.append(txn.getCb_percent()).append("%").append(CommonConstants.CSV_DELIMETER);
                if(txn.getUsedCardId()==null) {
                    sb.append("").append(CommonConstants.CSV_DELIMETER);
                } else {
                    sb.append(txn.getUsedCardId()).append(CommonConstants.CSV_DELIMETER);
                }
                sb.append(txn.getCpin());
                sb.append(CommonConstants.CSV_NEWLINE);

                // Write every 100 records in one go to the file
                if(i%CSV_LINES_BUFFER == 0) {
                    stream.write(sb.toString().getBytes());
                    LogMy.d(TAG,"Written "+String.valueOf(i+1)+"records to "+file.getAbsolutePath());
                    sb = null;
                }
            }

            // write totals line
            if(sb==null) {
                sb = new StringBuilder(CSV_RECORD_MAX_CHARS);
            }
            sb.append("Total").append(CommonConstants.CSV_DELIMETER);
            sb.append(CommonConstants.CSV_DELIMETER).append(CommonConstants.CSV_DELIMETER).append(CommonConstants.CSV_DELIMETER).append(CommonConstants.CSV_DELIMETER).append(CommonConstants.CSV_DELIMETER);
            sb.append(billTotal).append(CommonConstants.CSV_DELIMETER);
            sb.append(accDebitTotal).append(CommonConstants.CSV_DELIMETER);
            sb.append(accCreditTotal).append(CommonConstants.CSV_DELIMETER);
            sb.append(cbRedeemTotal).append(CommonConstants.CSV_DELIMETER);
            sb.append(cbAwardTotal).append(CommonConstants.CSV_DELIMETER);
            sb.append(CommonConstants.CSV_DELIMETER).append(CommonConstants.CSV_DELIMETER);
            sb.append(CommonConstants.CSV_NEWLINE);

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

            DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true);
            notDialog.setTargetFragment(this,REQ_NOTIFY_ERROR);
            notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);

            return null;
        }

        return file;
    }

    private void updateUI() {
        mTxnRecyclerView.setAdapter(new TxnAdapter(mRetainedFragment.mLastFetchTransactions, mForSingleMchnt));
        mCallback.setToolbarTitle(mRetainedFragment.mLastFetchTransactions.size() + " Transactions");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogMy.d(TAG, "In onActivityResult :" + requestCode + ", " + resultCode);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if(requestCode== REQ_SORT_TXN_TYPES) {
            int sortType = data.getIntExtra(SortTxnDialog.EXTRA_SELECTION, SortTxnDialog.TXN_SORT_DATE_TIME);
            sortTxnList(sortType);
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
        //public EditText mCustId;
        //public EditText mTxnId;

        public EditText mBillAmount;
        public EditText mCashbackAward;
        public View mAccountIcon;
        public EditText mAccountAmt;
        public View mCashbackIcon;
        public EditText mCashbackAmt;

        public EditText mMchntName;
        public View mLayoutCancel;
        public EditText mCancelTime;

        //public ImageView mSecureIcon;

        public TxnHolder(View itemView) {
            super(itemView);
            //itemView.setOnClickListener(this);
            mDatetime = (EditText) itemView.findViewById(R.id.txn_time);
            mMchntName = (EditText) itemView.findViewById(R.id.txn_mchnt_name);
            //mTxnId = (EditText) itemView.findViewById(R.id.txn_id);

            mBillAmount = (EditText) itemView.findViewById(R.id.txn_bill);
            mAccountIcon = itemView.findViewById(R.id.txn_account_icon);
            mAccountAmt = (EditText) itemView.findViewById(R.id.txn_account_amt);
            mCashbackIcon = itemView.findViewById(R.id.txn_cashback_icon);
            mCashbackAmt = (EditText) itemView.findViewById(R.id.txn_cashback_amt);

            mCashbackAward = (EditText) itemView.findViewById(R.id.txn_cashback_award);
            //mSecureIcon = (ImageView)itemView.findViewById(R.id.txn_secure_icon);

            mLayoutCancel = itemView.findViewById(R.id.layout_cancelled);
            mCancelTime = (EditText) itemView.findViewById(R.id.input_cancel_time);

            mMchntName.setOnClickListener(this);
            mDatetime.setOnClickListener(this);
            mBillAmount.setOnClickListener(this);
            mAccountAmt.setOnClickListener(this);
            mCashbackAmt.setOnClickListener(this);
            mCashbackAward.setOnClickListener(this);
            mCancelTime.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            LogMy.d(TAG,"In onClick: "+v.getId());

            // getRootView was not working, so manually finding root view
            View rootView = null;
            if(v.getId()==mMchntName.getId() || v.getId()==mDatetime.getId()) {
                rootView = (View) v.getParent().getParent();
                LogMy.d(TAG,"Clicked first level view "+rootView.getId());
            } else {
                rootView = (View) v.getParent().getParent().getParent();
                LogMy.d(TAG,"Clicked second level view "+rootView.getId());
            }
            rootView.performClick();
        }

        public void bindTxn(Transaction txn, boolean forSingleMchnt) {
            mTxn = txn;

            mDatetime.setText(mSdfDateWithTime.format(mTxn.getCreate_time()));

            if(forSingleMchnt) {
                mMchntName.setVisibility(View.GONE);
            } else {
                mMchntName.setVisibility(View.VISIBLE);
                mMchntName.setText(mTxn.getMerchant_name());
            }
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

            // changes if txn was cancelled
            if(mTxn.getCancelTime()==null) {
                mLayoutCancel.setVisibility(View.GONE);
            } else {
                mLayoutCancel.setVisibility(View.VISIBLE);
                mCancelTime.setText(mSdfDateWithTime.format(txn.getCancelTime()));

                if(txn.getTotal_billed()>0) {
                    mBillAmount.setPaintFlags(mBillAmount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                }

                if(txn.getCb_credit() > 0) {
                    mCashbackAward.setPaintFlags(mCashbackAward.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                }
                if(txn.getCb_debit()>0) {
                    mCashbackAmt.setPaintFlags(mCashbackAmt.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                }

                if(txn.getCl_debit()>0) {
                    mAccountAmt.setPaintFlags(mAccountAmt.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                }
            }

            /*
            if(mTxn.getCpin().equals(DbConstants.TXN_CUSTOMER_PIN_NOT_USED)) {
                mSecureIcon.setVisibility(View.GONE);
            }*/
        }
    }

    private class TxnAdapter extends RecyclerView.Adapter<TxnHolder> {
        private List<Transaction> mTxns;
        private boolean mForSingleMchnt;

        private View.OnClickListener mListener;

        public TxnAdapter(List<Transaction> txns, boolean forSingleMchnt) {
            mTxns = txns;
            mForSingleMchnt = forSingleMchnt;
            mListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LogMy.d(TAG,"In onClickListener of txn list item");
                    int pos = mTxnRecyclerView.getChildAdapterPosition(v);
                    if (pos >= 0 && pos < getItemCount()) {
                        showDetailedDialog(pos);
                    } else {
                        LogMy.e(TAG,"Invalid position in onClickListener of txn list item: "+pos);
                    }
                }
            };
        }

        @Override
        public TxnHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.txn_itemview, parent, false);
            LogMy.d(TAG,"Root view: "+view.getId());
            view.setOnClickListener(mListener);
            return new TxnHolder(view);
        }
        @Override
        public void onBindViewHolder(TxnHolder holder, int position) {
            Transaction txn = mTxns.get(position);
            holder.bindTxn(txn, mForSingleMchnt);
        }
        @Override
        public int getItemCount() {
             return mTxns.size();
        }
    }
}
