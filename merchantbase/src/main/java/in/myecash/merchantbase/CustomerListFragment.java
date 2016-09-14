package in.myecash.merchantbase;

import android.app.DownloadManager;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
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
import in.myecash.commonbase.constants.DbConstants;
import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.DialogFragmentWrapper;
import in.myecash.commonbase.utilities.LogMy;
import in.myecash.merchantbase.entities.MerchantUser;
import in.myecash.merchantbase.entities.MyCashback;
import in.myecash.merchantbase.entities.MyCustomer;
import in.myecash.merchantbase.helper.MyRetainedFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by adgangwa on 09-09-2016.
 */
public class CustomerListFragment  extends Fragment {
    private static final String TAG = "CustomerListFragment";
    private static final String DIALOG_CUSTOMER_DETAILS = "dialogCustomerDetails";

    private static final String CSV_HEADER = "Sl.No.,Internal Id,Mobile No.,Card ID,Status,Account Balance,Account Add,Account Debit,Cashback Balance,Cashback Award,Cashback Redeem,Total Bill,Cashback Bill,Last Txn here,First Txn here";
    // 5+10+10+10+10+5+5+5+5+5+5+5+5+10+10 = 105
    private static final int CSV_RECORD_MAX_CHARS = 128;
    //TODO: change this to 100 in production
    private static final int CSV_LINES_BUFFER = 5;

    private static final int REQ_NOTIFY_ERROR = 1;

    private SimpleDateFormat mSdfDateWithTime;
    private SimpleDateFormat mSdfOnlyDateFilename;

    private RecyclerView mCustRecyclerView;
    private MyRetainedFragment mRetainedFragment;
    private CustomerListFragmentIf mCallback;

    public interface CustomerListFragmentIf {
        MyRetainedFragment getRetainedFragment();
        public void setDrawerState(boolean isEnabled);
    }

    /*
    public static CustomerListFragment getInstance(Date startTime, Date endTime) {
        Bundle args = new Bundle();

        CustomerListFragment fragment = new CustomerListFragment();
        fragment.setArguments(args);
        return fragment;
    }*/

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
                // new file download scenario - process the file
                processFile();
            }

        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement CustomerListFragmentIf");
        } catch(Exception e) {
            LogMy.e(TAG, "Exception is CustomerListFragment:onActivityCreated", e);
            // unexpected exception - show error
            DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, ErrorCodes.appErrorDesc.get(ErrorCodes.GENERAL_ERROR), true, true);
            notDialog.setTargetFragment(this,REQ_NOTIFY_ERROR);
            notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_list, container, false);
        mCustRecyclerView = (RecyclerView) view.findViewById(R.id.cust_recycler_view);
        mCustRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }

    private void processFile() throws Exception {
        String fileName = AppCommonUtil.getMerchantCustFileName(mRetainedFragment.mMerchantUser.getMerchantId());

        InputStream inputStream = getActivity().openFileInput(fileName);
        if ( inputStream != null ) {
            int lineCnt = 0;

            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString = "";
            while ( (receiveString = bufferedReader.readLine()) != null ) {
                lineCnt++;
                // ignore empty lines
                if(!receiveString.equals(CommonConstants.CSV_NEWLINE)) {
                    processCbCsvRecord(receiveString);
                }
            }
            inputStream.close();
            LogMy.d(TAG,"Processed "+lineCnt+" lines from "+fileName);
        } else {
            String error = "openFileInput returned null for Cashback CSV file: "+fileName;
            LogMy.e(TAG, error);
            throw new FileNotFoundException(error);
        }
    }

    private void processCbCsvRecord(String csvString) {
        MyCashback cb = new MyCashback();
        cb.init(csvString);
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
        // do nothing
    }

    @Override
    public void onResume() {
        super.onResume();
        mCallback.setDrawerState(false);
        updateUI();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.customer_list_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            int i = item.getItemId();
            if (i == R.id.action_download) {
                downloadReport();

            } else if (i == R.id.action_email) {
                emailReport();

            }
        } catch(Exception e) {
            LogMy.e(TAG, "Exception is CustomerListFragment:onOptionsItemSelected", e);
            // unexpected exception - show error
            DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, ErrorCodes.appErrorDesc.get(ErrorCodes.GENERAL_ERROR), true, true);
            notDialog.setTargetFragment(this,REQ_NOTIFY_ERROR);
            notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
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
            sb.append(CSV_HEADER).append(CommonConstants.CSV_NEWLINE);

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
                sb.append(cust.getCardId()).append(CommonConstants.CSV_DELIMETER);
                sb.append(DbConstants.userStatusDesc[cust.getStatus()]).append(CommonConstants.CSV_DELIMETER);
                sb.append(cb.getCurrClBalance()).append(CommonConstants.CSV_DELIMETER);
                sb.append(cb.getClCredit()).append(CommonConstants.CSV_DELIMETER);
                sb.append(cb.getClDebit()).append(CommonConstants.CSV_DELIMETER);
                sb.append(cb.getCurrCbBalance()).append(CommonConstants.CSV_DELIMETER);
                sb.append(cb.getCbCredit()).append(CommonConstants.CSV_DELIMETER);
                sb.append(cb.getCbRedeem()).append(CommonConstants.CSV_DELIMETER);
                sb.append(cb.getBillAmt()).append(CommonConstants.CSV_DELIMETER);
                sb.append(mSdfDateWithTime.format(cb.getUpdateTime())).append(CommonConstants.CSV_DELIMETER);
                sb.append(mSdfDateWithTime.format(cb.getCreateTime()));
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
            throw e;
        }

        return file;
    }

    private class CbHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private MyCashback mCb;

        public EditText mCustId;
        public EditText mCustMobile;
        public EditText mCustStatus;
        public EditText mAccountAmt;
        public EditText mCashbackAmt;

        public CbHolder(View itemView) {
            super(itemView);
            //itemView.setOnClickListener(this);
            mCustId = (EditText) itemView.findViewById(R.id.input_cust_id);
            mCustMobile = (EditText) itemView.findViewById(R.id.input_cust_mobile);
            mCustStatus = (EditText) itemView.findViewById(R.id.input_cust_status);
            mAccountAmt = (EditText) itemView.findViewById(R.id.cust_account_amt);
            mCashbackAmt = (EditText) itemView.findViewById(R.id.cust_cashback_amt);

            mCustId.setOnClickListener(this);
            mCustMobile.setOnClickListener(this);
            mCustStatus.setOnClickListener(this);
            mAccountAmt.setOnClickListener(this);
            mCashbackAmt.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            LogMy.d(TAG,"In onClick: "+getAdapterPosition());

            // getRootView was not working, so manually finding root view
            View rootView = null;
            if(v.getId()==mCustId.getId()) {
                rootView = (View) v.getParent().getParent();
            } else {
                rootView = (View) v.getParent().getParent().getParent();
            }
            rootView.performClick();
        }

        public void bindCb(MyCashback cb) {
            mCb = cb;
            MyCustomer customer = mCb.getCustomer();

            mCustId.setText(customer.getPrivateId());
            mCustMobile.setText(AppCommonUtil.getPartialVisibleStr(customer.getMobileNum()));
            mCustStatus.setText(DbConstants.userStatusDesc[customer.getStatus()]);

            mAccountAmt.setText(AppCommonUtil.getAmtStr(mCb.getCurrClBalance()));
            mCashbackAmt.setText(AppCommonUtil.getAmtStr(mCb.getCurrCbBalance()));
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
                    int pos = mCustRecyclerView.getChildAdapterPosition(v);
                    if (pos >= 0 && pos < getItemCount()) {
                        CustomerDetailsDialog dialog = CustomerDetailsDialog.newInstance(pos);
                        dialog.show(getFragmentManager(), DIALOG_CUSTOMER_DETAILS);
                    } else {
                        LogMy.e(TAG,"Invalid position in onClickListener of customer list item: "+pos);
                    }
                }
            };
        }

        @Override
        public CbHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.customer_itemview, parent, false);
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
