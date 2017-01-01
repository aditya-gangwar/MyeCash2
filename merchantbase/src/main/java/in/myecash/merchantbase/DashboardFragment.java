package in.myecash.merchantbase;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import in.myecash.common.constants.CommonConstants;
import in.myecash.common.MyGlobalSettings;
import in.myecash.common.database.MerchantStats;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.merchantbase.helper.MyRetainedFragment;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by adgangwa on 05-07-2016.
 */
public class DashboardFragment extends Fragment
        implements View.OnClickListener, View.OnTouchListener {
    private static final String TAG = "MchntApp-DashboardSummary";

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
        MyRetainedFragment getRetainedFragment();
        void setDrawerState(boolean isEnabled);
        void showDashboardDetails(int which);
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

    }

    @Override
    public void onResume() {
        LogMy.d(TAG, "In onResume");
        super.onResume();
        mCallback.setDrawerState(false);
        mCallback.getRetainedFragment().setResumeOk(true);
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

        labelCustCnt.setOnTouchListener(this);
        labelBillAmt.setOnTouchListener(this);
        labelAccount.setOnTouchListener(this);
        labelCashback.setOnTouchListener(this);

        total_customers.setOnTouchListener(this);
        total_bill_amt.setOnTouchListener(this);
        total_account_cash.setOnTouchListener(this);
        total_cashback.setOnTouchListener(this);

        //downloadDataFile.setOnClickListener(this);
        //emailDataFile.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(!mCallback.getRetainedFragment().getResumeOk())
            return;

        LogMy.d(TAG,"In onClick: "+v.getId());
        showDetailedView(v);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(!mCallback.getRetainedFragment().getResumeOk())
            return true;

        if(event.getAction() == MotionEvent.ACTION_UP) {
            LogMy.d(TAG,"In onTouch: "+v.getId());
            showDetailedView(v);
        }
        return true;
    }

    private void showDetailedView(View v) {
        int id = v.getId();

        if (id == R.id.layout_cust_cnt) {
            mCallback.showDashboardDetails(DB_TYPE_CUSTOMER);

        } else if (id == R.id.layout_bill_amt) {
            mCallback.showDashboardDetails(DB_TYPE_BILL_AMT);

        } else if (id == R.id.layout_account_cash) {
            mCallback.showDashboardDetails(DB_TYPE_ACCOUNT);

        } else if (id == R.id.layout_cashback) {
            mCallback.showDashboardDetails(DB_TYPE_CASHBACK);

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

        // update time
        SimpleDateFormat sdf = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
        Date updateTime = mMerchantStats.getUpdated();
        if(updateTime==null) {
            updateTime = mMerchantStats.getCreated();
        }

        mUpdated.setText(sdf.format(updateTime));
        String txt = "Data is updated only once every "+ MyGlobalSettings.getMchntDashBNoRefreshHrs()+" hours.";
        mUpdatedDetail.setText(txt);
    }

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

