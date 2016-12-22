package in.myecash.merchantbase;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;

/**
 * Created by adgangwa on 08-06-2016.
 */
public class TxnSummaryFragment extends Fragment {
    private static final String TAG = "MchntApp-TxnSummaryFragment";

    private static final String ARG_SUMMARY = "summary";

    public interface TxnSummaryFragmentIf {
        void setToolbarTitle(String title);
        void showTxnDetails();
    }

    private TxnSummaryFragmentIf mCallback;

    public static TxnSummaryFragment newInstance(int[] summary) {
        Bundle args = new Bundle();
        args.putIntArray(ARG_SUMMARY, summary);
        TxnSummaryFragment fragment = new TxnSummaryFragment();
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LogMy.d(TAG, "In onActivityCreated");

        try {
            mCallback = (TxnSummaryFragmentIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement TxnSummaryFragmentIf");
        }
        mCallback.setToolbarTitle("Summary");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateView");
        View v = inflater.inflate(R.layout.fragment_txn_report_summary, container, false);

        // access to UI elements
        bindUiResources(v);

        // update values
        int[] summary = getArguments().getIntArray(ARG_SUMMARY);

        input_values[AppConstants.INDEX_TXN_COUNT].setText(String.valueOf(summary[AppConstants.INDEX_TXN_COUNT]));
        input_values[AppConstants.INDEX_BILL_AMOUNT].setText(AppCommonUtil.getSignedAmtStr(summary[AppConstants.INDEX_BILL_AMOUNT], true));
        input_values[AppConstants.INDEX_ADD_ACCOUNT].setText(AppCommonUtil.getSignedAmtStr(summary[AppConstants.INDEX_ADD_ACCOUNT], true));
        input_values[AppConstants.INDEX_DEBIT_ACCOUNT].setText(AppCommonUtil.getSignedAmtStr(summary[AppConstants.INDEX_DEBIT_ACCOUNT], false));
        input_values[AppConstants.INDEX_CASHBACK].setText(AppCommonUtil.getSignedAmtStr(summary[AppConstants.INDEX_CASHBACK], true));
        input_values[AppConstants.INDEX_DEBIT_CASHBACK].setText(AppCommonUtil.getSignedAmtStr(summary[AppConstants.INDEX_DEBIT_CASHBACK], true));

        detailsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.showTxnDetails();
            }
        });

        return v;
    }

    EditText input_values[] = new EditText[AppConstants.INDEX_SUMMARY_MAX_VALUE];
    AppCompatButton detailsButton;

    protected void bindUiResources(View view) {
        input_values[AppConstants.INDEX_TXN_COUNT] = (EditText) view.findViewById(R.id.input_trans_count);
        input_values[AppConstants.INDEX_BILL_AMOUNT] = (EditText) view.findViewById(R.id.input_trans_bill_amt);
        input_values[AppConstants.INDEX_ADD_ACCOUNT] = (EditText) view.findViewById(R.id.input_trans_add_account);
        input_values[AppConstants.INDEX_DEBIT_ACCOUNT] = (EditText) view.findViewById(R.id.input_trans_debit_account);
        input_values[AppConstants.INDEX_CASHBACK] = (EditText) view.findViewById(R.id.input_trans_add_cb);
        input_values[AppConstants.INDEX_DEBIT_CASHBACK] = (EditText) view.findViewById(R.id.input_trans_redeem_cb);

        detailsButton = (AppCompatButton) view.findViewById(R.id.details_btn);
    }
}

