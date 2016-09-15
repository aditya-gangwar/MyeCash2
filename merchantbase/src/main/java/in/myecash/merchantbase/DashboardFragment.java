package in.myecash.merchantbase;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import in.myecash.commonbase.constants.CommonConstants;
import in.myecash.commonbase.constants.DbConstants;
import in.myecash.commonbase.entities.MyGlobalSettings;
import in.myecash.commonbase.models.MerchantStats;
import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.LogMy;
import in.myecash.merchantbase.helper.MyRetainedFragment;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by adgangwa on 05-07-2016.
 */
public class DashboardFragment extends Fragment
        implements View.OnClickListener {
    private static final String TAG = "DashboardSummary";

    public static final int DB_TYPE_CUSTOMER = 1;
    public static final int DB_TYPE_CASHBACK = 2;
    public static final int DB_TYPE_ACCOUNT = 3;
    public static final int DB_TYPE_BILL_AMT = 4;

    private static final int REQ_NOTIFY_ERROR = 1;
    private static final int REQ_STORAGE_PERMISSION = 2;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_WRITE_STORAGE = 10;

    private MerchantStats mMerchantStats;
    // instance state - store and restore
    //private int mActiveRequestId;

    public interface DashboardSummaryFragmentIf {
        public MyRetainedFragment getRetainedFragment();
        public void setDrawerState(boolean isEnabled);
        public void showHistoryTxns(int which);
    }
    private DashboardSummaryFragmentIf mCallback;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LogMy.d(TAG, "In onActivityCreated");

        try {
            mCallback = (DashboardSummaryFragmentIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement DashboardSummaryFragmentIf");
        }

        mMerchantStats = mCallback.getRetainedFragment().mMerchantStats;

        // Setting values here and not in onCreateView - as mMerchantStats is not available in it
        updateData();
        // update time
        SimpleDateFormat sdf = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_TIME_12, CommonConstants.DATE_LOCALE);
        Date updateTime = mMerchantStats.getUpdated();
        if(updateTime==null) {
            updateTime = mMerchantStats.getCreated();
        }
        mUpdated.setText(sdf.format(updateTime));

        /*
        if(savedInstanceState!=null) {
            mActiveRequestId = savedInstanceState.getInt("mActiveRequestId");
        }*/
    }

    @Override
    public void onResume() {
        LogMy.d(TAG, "In onResume");
        super.onResume();
        mCallback.setDrawerState(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateView");
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // access to UI elements
        bindUiResources(v);
        setListeners();

        return v;
    }

    private void setListeners() {
        layoutCustCnt.setOnClickListener(this);
        layoutBillAmt.setOnClickListener(this);
        layoutAccount.setOnClickListener(this);
        layoutCashback.setOnClickListener(this);

        labelCustCnt.setOnClickListener(this);
        labelBillAmt.setOnClickListener(this);
        labelAccount.setOnClickListener(this);
        labelCashback.setOnClickListener(this);

        total_customers.setOnClickListener(this);
        total_bill_amt.setOnClickListener(this);
        total_account_cash.setOnClickListener(this);
        total_cashback.setOnClickListener(this);

        //downloadDataFile.setOnClickListener(this);
        //emailDataFile.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.layout_cust_cnt) {
            mCallback.showHistoryTxns(DB_TYPE_CUSTOMER);

        } else if (id == R.id.layout_bill_amt) {
            mCallback.showHistoryTxns(DB_TYPE_BILL_AMT);

        } else if (id == R.id.layout_account_cash) {
            mCallback.showHistoryTxns(DB_TYPE_ACCOUNT);

        } else if (id == R.id.layout_cashback) {
            mCallback.showHistoryTxns(DB_TYPE_CASHBACK);

            /*
            case R.id.btn_email_report:
            case R.id.btn_cust_report_dwnload:
                mActiveRequestId = id;
                if( checkPermission() ) {
                    mCallback.downloadCustDataFile();
                } else {
                    requestStoragePermission();
                }
                break;
            */
        } else {
            View parent = (View) v.getParent();
            parent.performClick();
        }
    }

    private void updateData() {
        int cust_cnt = mMerchantStats.getCust_cnt_cash()+mMerchantStats.getCust_cnt_cb()+mMerchantStats.getCust_cnt_cb_and_cash()+mMerchantStats.getCust_cnt_no_balance();
        total_customers.setText(String.valueOf(cust_cnt));

        total_bill_amt.setText(AppCommonUtil.getAmtStr(mMerchantStats.getBill_amt_total()));
        total_account_cash.setText(AppCommonUtil.getAmtStr(mMerchantStats.getCash_credit()));
        total_cashback.setText(AppCommonUtil.getAmtStr(mMerchantStats.getCb_credit()));

        String txt = "Data is updated only once every "+ MyGlobalSettings.getMchntDashBNoRefreshHrs()+" hours.";
        mUpdatedDetail.setText(txt);
    }

    /*
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("mActiveRequestId", mActiveRequestId);
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
            // permission granted - download the file
            mCallback.downloadFile(AppCommonUtil.getMerchantCustFilePath(MerchantUser.getInstance().getMerchantId()));
        } else {
            LogMy.i(TAG, "Permission not granted: results len = " + grantResults.length +
                    " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

            DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification(AppConstants.noPermissionTitle,
                    "Cannot download/email reports as write storage permission not provided.",
                    false, true);
            notDialog.setTargetFragment(this,REQ_NOTIFY_ERROR);
            notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }
    */

    View layoutCustCnt;
    View layoutBillAmt;
    View layoutAccount;
    View layoutCashback;

    View labelCustCnt;
    View labelBillAmt;
    View labelAccount;
    View labelCashback;

    EditText total_customers;
    EditText total_bill_amt;
    EditText total_account_cash;
    EditText total_cashback;

    EditText mUpdated;
    EditText mUpdatedDetail;
    //AppCompatButton downloadDataFile;
    //AppCompatButton emailDataFile;

    protected void bindUiResources(View view) {

        layoutCustCnt = view.findViewById(R.id.layout_cust_cnt);
        layoutBillAmt = view.findViewById(R.id.layout_bill_amt);
        layoutAccount = view.findViewById(R.id.layout_account_cash);
        layoutCashback = view.findViewById(R.id.layout_cashback);

        labelCustCnt = view.findViewById(R.id.label_cust_cnt);
        labelBillAmt = view.findViewById(R.id.label_bill_amt);
        labelAccount = view.findViewById(R.id.label_account_cash);
        labelCashback = view.findViewById(R.id.label_cashback);

        total_customers = (EditText) view.findViewById(R.id.input_cust_cnt);
        total_bill_amt = (EditText) view.findViewById(R.id.input_bill_amt);
        total_account_cash = (EditText) view.findViewById(R.id.input_account_cash);
        total_cashback = (EditText) view.findViewById(R.id.input_cashback);

        mUpdated = (EditText) view.findViewById(R.id.input_updated_time);
        mUpdatedDetail = (EditText) view.findViewById(R.id.updated_time_details);
        //downloadDataFile = (AppCompatButton) view.findViewById(R.id.btn_cust_report_dwnload);
        //emailDataFile = (AppCompatButton) view.findViewById(R.id.btn_email_report);

    }
}

