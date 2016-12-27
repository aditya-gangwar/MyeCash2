package in.myecash.merchantbase;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TableRow;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.MerchantStats;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.merchantbase.helper.MyRetainedFragment;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by adgangwa on 09-06-2016.
 */
public class DashboardTxnFragment extends Fragment {
    private static final String TAG = "MchntApp-DashboardTxnFragment";

    //private static final String DIALOG_DASHBOARD_TYPE = "DialogDashboardType";
    //private static final int REQUEST_DASHBOARD_TYPE = 1;
    //private static final int DASHBOARD_TYPE_INIT_INDEX = 0;

    private static final String ARG_DBOARD_TYPE = "dBoardType";
    private static final int COLUMN_COUNT = 3;

    private MerchantStats mMerchantStats;
    private final DecimalFormat df = new DecimalFormat("##.#");

    public interface DashboardFragmentIf {
        public MyRetainedFragment getRetainedFragment();
        public void setDrawerState(boolean isEnabled);
    }
    private DashboardFragmentIf mCallback;

    public static DashboardTxnFragment getInstance(int dbType) {
        Bundle args = new Bundle();
        args.putInt(ARG_DBOARD_TYPE, dbType);

        DashboardTxnFragment fragment = new DashboardTxnFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LogMy.d(TAG, "In onActivityCreated");

        try {
            mCallback = (DashboardFragmentIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement DashboardFragmentIf");
        }

        mMerchantStats = mCallback.getRetainedFragment().mMerchantStats;

        // Setting values here and not in onCreateView - as mMerchantStats is not available in it
        // first time show data for mDashboardSelectedIndx = 0
        try {
            int type = getArguments().getInt(ARG_DBOARD_TYPE);
            updateData(type);
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in Fragment: ", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            getActivity().onBackPressed();
        }
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
        View v = inflater.inflate(R.layout.fragment_dashboard_details, container, false);

        // access to UI elements
        bindUiResources(v);
        //initChoiceChartType();
        return v;
    }

    private void updateData(int index) {
        switch(index) {
            case DashboardFragment.DB_TYPE_CUSTOMER:
                createCustomerCntChart();
                updateTableForCustomerCnt();
                break;
            case DashboardFragment.DB_TYPE_CASHBACK:
                createCashbackChart();
                updateTableForCashback();
                break;
            case DashboardFragment.DB_TYPE_ACCOUNT:
                createCashAccountChart();
                updateTableForCashAccount();
                break;
            case DashboardFragment.DB_TYPE_BILL_AMT:
                createBillAmountChart();
                updateTableForBillAmount();
        }
        // common chart settings
        mPieChart.setUsePercentValues(true);
        // update pie chart
        mPieChart.invalidate();
    }

    private void createCustomerCntChart() {
        // creating values - Y axis
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<Entry> entries = new ArrayList<>();
        int index = 0;

        if(mMerchantStats.getCust_cnt_cb() > 0) {
            labels.add("Only Cashback");
            entries.add(new Entry(mMerchantStats.getCust_cnt_cb(), index));
            index++;
        }
        if(mMerchantStats.getCust_cnt_cash() > 0) {
            labels.add("Only Account Balance");
            entries.add(new Entry(mMerchantStats.getCust_cnt_cash(), index));
            index++;
        }
        if(mMerchantStats.getCust_cnt_cb_and_cash() > 0) {
            labels.add("Account + Cashback");
            entries.add(new Entry(mMerchantStats.getCust_cnt_cb_and_cash(), index));
            index++;
        }
        if(mMerchantStats.getCust_cnt_no_balance() > 0) {
            labels.add("0 Balance");
            entries.add(new Entry(mMerchantStats.getCust_cnt_no_balance(), index));
        }

        PieDataSet dataset = new PieDataSet(entries, "Customers");
        dataset.setColors(ColorTemplate.MATERIAL_COLORS);

        PieData data = new PieData(labels, dataset); // initialize Piedata
        data.setValueFormatter(new PercentFormatter());
        mPieChart.setData(data); //set data into chart

        mPieChart.setDescription("Customer Data");
    }

    private void updateTableForCustomerCnt() {
        // update title row
        row_title_values[0].setText("Customers with ...");
        row_title_values[1].setText("Count");
        row_title_values[2].setText("Share");

        int total_cust_cnt = mMerchantStats.getCust_cnt_cb() +
                mMerchantStats.getCust_cnt_cash() +
                mMerchantStats.getCust_cnt_cb_and_cash() +
                mMerchantStats.getCust_cnt_no_balance();

        float percent = 0;
        String percentStr = null;
//        if(mMerchantStats.getCust_cnt_cb() > 0) {
            row_1_values[0].setText("Only Cashback");
            row_1_values[1].setText(String.valueOf(mMerchantStats.getCust_cnt_cb()));
            percent = (mMerchantStats.getCust_cnt_cb()*100.0f)/total_cust_cnt;
            percentStr = df.format(percent)+" %";
            row_1_values[2].setText(percentStr);
/*
            rows_table[0].setVisibility(View.VISIBLE);
        } else {
            rows_table[0].setVisibility(View.GONE);
        }*/

//        if(mMerchantStats.getCust_cnt_cash() > 0) {
            row_2_values[0].setText("Only Account Balance");
            row_2_values[1].setText(String.valueOf(mMerchantStats.getCust_cnt_cash()));
            percent = (mMerchantStats.getCust_cnt_cash()*100.0f)/total_cust_cnt;
            percentStr = df.format(percent)+" %";
            row_2_values[2].setText(percentStr);
/*
            rows_table[1].setVisibility(View.VISIBLE);
        } else {
            rows_table[1].setVisibility(View.GONE);
        }*/

//        if(mMerchantStats.getCust_cnt_cb_and_cash() > 0) {
            row_3_values[0].setText("Account + Cashback");
            row_3_values[1].setText(String.valueOf(mMerchantStats.getCust_cnt_cb_and_cash()));
            percent = (mMerchantStats.getCust_cnt_cb_and_cash()*100.0f)/total_cust_cnt;
            percentStr = df.format(percent)+" %";
            row_3_values[2].setText(percentStr);
/*
            rows_table[2].setVisibility(View.VISIBLE);
        } else {
            rows_table[2].setVisibility(View.GONE);
        }*/

//        if(mMerchantStats.getCust_cnt_no_balance() > 0) {
            row_4_values[0].setText("0 Balance");
            row_4_values[1].setText(String.valueOf(mMerchantStats.getCust_cnt_no_balance()));
            percent = (mMerchantStats.getCust_cnt_no_balance()*100.0f)/total_cust_cnt;
            percentStr = df.format(percent)+" %";
            row_4_values[2].setText(percentStr);
/*
            rows_table[3].setVisibility(View.VISIBLE);
        } else {
            rows_table[3].setVisibility(View.GONE);
        }*/

        total_value.setText(String.valueOf(total_cust_cnt));
    }

    private void createCashbackChart() {
        // creating values - Y axis
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<Entry> entries = new ArrayList<>();

        labels.add("Debited");
        entries.add(new Entry(mMerchantStats.getCb_debit(), 0));

        labels.add("Pending");
        int balance = mMerchantStats.getCb_credit()- mMerchantStats.getCb_debit();
        entries.add(new Entry(balance, 1));

        PieDataSet dataset = new PieDataSet(entries, "Cashback");
        dataset.setColors(ColorTemplate.MATERIAL_COLORS);

        PieData data = new PieData(labels, dataset); // initialize Piedata
        data.setValueFormatter(new PercentFormatter());
        mPieChart.setData(data); //set data into chart

        mPieChart.setDescription("Cashback Data");
    }

    private void updateTableForCashback() {
        // update title row
        row_title_values[0].setText("Cashback");
        row_title_values[1].setText("Amount");
        row_title_values[2].setText("Share");

        int balance = mMerchantStats.getCb_credit()- mMerchantStats.getCb_debit();
        //DecimalFormat df = new DecimalFormat("##.##");

        row_1_values[0].setText("Redeemed");
        String amount = AppConstants.SYMBOL_RS +String.valueOf(mMerchantStats.getCb_debit());
        row_1_values[1].setText(amount);
        Float percent = (mMerchantStats.getCb_debit()*100.0f)/mMerchantStats.getCb_credit();
        String percentStr = (percent.isNaN() ? "0" : df.format(percent))+" %";
        row_1_values[2].setText(percentStr);
        rows_table[0].setVisibility(View.VISIBLE);

        row_2_values[0].setText("Pending");
        amount = AppConstants.SYMBOL_RS +String.valueOf(balance);
        row_2_values[1].setText(amount);
        percent = (balance*100.0f)/mMerchantStats.getCb_credit();
        percentStr = (percent.isNaN() ? "0" : df.format(percent))+" %";
        row_2_values[2].setText(percentStr);
        rows_table[1].setVisibility(View.VISIBLE);

        rows_table[2].setVisibility(View.GONE);
        rows_table[3].setVisibility(View.GONE);

        amount = AppConstants.SYMBOL_RS + String.valueOf(mMerchantStats.getCb_credit());
        total_value.setText(amount);
    }

    private void createCashAccountChart() {
        // creating values - Y axis
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<Entry> entries = new ArrayList<>();

        labels.add("Debited");
        entries.add(new Entry(mMerchantStats.getCash_debit(), 0));

        labels.add("Available");
        int balance = mMerchantStats.getCash_credit()- mMerchantStats.getCash_debit();
        entries.add(new Entry(balance, 1));

        PieDataSet dataset = new PieDataSet(entries, "Account Cash");
        dataset.setColors(ColorTemplate.MATERIAL_COLORS);

        PieData data = new PieData(labels, dataset); // initialize Piedata
        data.setValueFormatter(new PercentFormatter());
        mPieChart.setData(data); //set data into chart

        mPieChart.setDescription("Account Cash");
    }

    private void updateTableForCashAccount() {
        // update title row
        row_title_values[0].setText("Account Cash");
        row_title_values[1].setText("Amount");
        row_title_values[2].setText("Share");

        int balance = mMerchantStats.getCash_credit()- mMerchantStats.getCash_debit();
        //DecimalFormat df = new DecimalFormat("##.##");

        row_1_values[0].setText("Debited");
        String amount = AppConstants.SYMBOL_RS +String.valueOf(mMerchantStats.getCash_debit());
        row_1_values[1].setText(amount);
        Float percent = (mMerchantStats.getCash_debit()*100.0f)/mMerchantStats.getCash_credit();
        String percentStr = (percent.isNaN() ? "0" : df.format(percent))+" %";
        row_1_values[2].setText(percentStr);
        rows_table[0].setVisibility(View.VISIBLE);

        row_2_values[0].setText("Available");
        amount = AppConstants.SYMBOL_RS +String.valueOf(balance);
        row_2_values[1].setText(amount);
        percent = (balance*100.0f)/mMerchantStats.getCash_credit();
        percentStr = (percent.isNaN() ? "0" : df.format(percent))+" %";
        row_2_values[2].setText(percentStr);
        rows_table[1].setVisibility(View.VISIBLE);

        amount = AppConstants.SYMBOL_RS + String.valueOf(mMerchantStats.getCash_credit());
        total_value.setText(amount);

        rows_table[2].setVisibility(View.GONE);
        rows_table[3].setVisibility(View.GONE);
    }

    private void createBillAmountChart() {
        // creating values - Y axis
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int index = 0;

        int direct = mMerchantStats.getBill_amt_total()- mMerchantStats.getCb_debit() - mMerchantStats.getCash_debit();
        if(direct>0) {
            labels.add("Direct");
            entries.add(new Entry(direct, index));
            index++;
        }

        if(mMerchantStats.getCb_debit() > 0) {
            labels.add("From Cashback");
            entries.add(new Entry(mMerchantStats.getCb_debit(), index));
            index++;
        }

        if(mMerchantStats.getCash_debit()>0) {
            labels.add("From Account");
            entries.add(new Entry(mMerchantStats.getCash_debit(), index));
            index++;
        }

        PieDataSet dataset = new PieDataSet(entries, "Bill Payment");
        dataset.setColors(ColorTemplate.MATERIAL_COLORS);

        // creating labels
        PieData data = new PieData(labels, dataset); // initialize Piedata
        data.setValueFormatter(new PercentFormatter());
        mPieChart.setData(data); //set data into chart

        mPieChart.setDescription("Bill Payment");
    }

    private void updateTableForBillAmount() {
        // update title row
        row_title_values[0].setText("Bill Payment");
        row_title_values[1].setText("Amount");
        row_title_values[2].setText("Share");

        int direct = mMerchantStats.getBill_amt_total()- mMerchantStats.getCb_debit() - mMerchantStats.getCash_debit();
        //DecimalFormat df = new DecimalFormat("##.##");

        row_1_values[0].setText("Direct");
        String amount = AppConstants.SYMBOL_RS +String.valueOf(direct);
        row_1_values[1].setText(amount);
        Float percent = (direct*100.0f)/mMerchantStats.getBill_amt_total();
        String percentStr = (percent.isNaN() ? "0" : df.format(percent))+" %";
        row_1_values[2].setText(percentStr);
        rows_table[0].setVisibility(View.VISIBLE);

        row_2_values[0].setText("With Cashback");
        amount = AppConstants.SYMBOL_RS +String.valueOf(mMerchantStats.getCb_debit());
        row_2_values[1].setText(amount);
        percent = (mMerchantStats.getCb_debit()*100.0f)/mMerchantStats.getBill_amt_total();
        percentStr = (percent.isNaN() ? "0" : df.format(percent))+" %";
        row_2_values[2].setText(percentStr);
        rows_table[1].setVisibility(View.VISIBLE);

        row_3_values[0].setText("From Account");
        amount = AppConstants.SYMBOL_RS +String.valueOf(mMerchantStats.getCash_debit());
        row_3_values[1].setText(amount);
        percent = (mMerchantStats.getCash_debit()*100.0f)/mMerchantStats.getBill_amt_total();
        percentStr = (percent.isNaN() ? "0" : df.format(percent))+" %";
        row_3_values[2].setText(percentStr);
        rows_table[2].setVisibility(View.VISIBLE);

        amount = AppConstants.SYMBOL_RS + String.valueOf(mMerchantStats.getBill_amt_total());
        total_value.setText(amount);

        rows_table[3].setVisibility(View.GONE);
    }


    //EditText mDashboardType;
    PieChart mPieChart;
    //EditText mUpdated;

    TableRow rows_table[] = new TableRow[4];
    EditText row_title_values[] = new EditText[COLUMN_COUNT];
    EditText row_1_values[] = new EditText[COLUMN_COUNT];
    EditText row_2_values[] = new EditText[COLUMN_COUNT];
    EditText row_3_values[] = new EditText[COLUMN_COUNT];
    EditText row_4_values[] = new EditText[COLUMN_COUNT];
    EditText total_value;

    protected void bindUiResources(View view) {
        //mDashboardType = (EditText) view.findViewById(R.id.choice_dashboard_type);
        //mUpdated = (EditText) view.findViewById(R.id.input_updated_time);
        mPieChart = (PieChart) view.findViewById(R.id.chart_1);

        row_title_values[0] = (EditText) view.findViewById(R.id.table_row_title_col_1);
        row_title_values[1] = (EditText) view.findViewById(R.id.table_row_title_col_2);
        row_title_values[2] = (EditText) view.findViewById(R.id.table_row_title_col_3);

        rows_table[0] = (TableRow) view.findViewById(R.id.table_row_1);
        row_1_values[0] = (EditText) view.findViewById(R.id.table_row_1_col_1);
        row_1_values[1] = (EditText) view.findViewById(R.id.table_row_1_col_2);
        row_1_values[2] = (EditText) view.findViewById(R.id.table_row_1_col_3);

        rows_table[1] = (TableRow) view.findViewById(R.id.table_row_2);
        row_2_values[0] = (EditText) view.findViewById(R.id.table_row_2_col_1);
        row_2_values[1] = (EditText) view.findViewById(R.id.table_row_2_col_2);
        row_2_values[2] = (EditText) view.findViewById(R.id.table_row_2_col_3);

        rows_table[2] = (TableRow) view.findViewById(R.id.table_row_3);
        row_3_values[0] = (EditText) view.findViewById(R.id.table_row_3_col_1);
        row_3_values[1] = (EditText) view.findViewById(R.id.table_row_3_col_2);
        row_3_values[2] = (EditText) view.findViewById(R.id.table_row_3_col_3);

        rows_table[3] = (TableRow) view.findViewById(R.id.table_row_4);
        row_4_values[0] = (EditText) view.findViewById(R.id.table_row_4_col_1);
        row_4_values[1] = (EditText) view.findViewById(R.id.table_row_4_col_2);
        row_4_values[2] = (EditText) view.findViewById(R.id.table_row_4_col_3);

        total_value = (EditText) view.findViewById(R.id.table_row_total_col_2);

    }
}


