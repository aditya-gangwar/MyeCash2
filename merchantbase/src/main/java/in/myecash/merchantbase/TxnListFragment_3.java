package in.myecash.merchantbase;

/**
 * Created by adgangwa on 07-04-2016.
 */
/*
public class TxnListFragment_3 extends Fragment {
    private  static String ARG_CSV_RECORDS = "csvRecords";

    private RecyclerView mTxnRecyclerView;
    private TxnAdapter mAdapter;
    private MyRetainedFragment mRetainedFragment;
    // TODO: save this in onPause() to the bundle
    ArrayList<String> mCsvRecords;
    SimpleDateFormat mSdf;

    public static TxnListFragment_3 newInstance(ArrayList<String> csvRecords, MyRetainedFragment retainedFragment) {
        //Bundle args = new Bundle();
        //args.putStringArrayList(ARG_CSV_RECORDS, csvRecords);
        TxnListFragment_3 fragment = new TxnListFragment_3();
        fragment.mCsvRecords = csvRecords;
        fragment.mRetainedFragment = retainedFragment;
        fragment.mSdf = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
        //fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_txn_list, container, false);
        mTxnRecyclerView = (RecyclerView) view
                .findViewById(R.id.txn_recycler_view);
        mTxnRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        updateUI();

        return view;
    }

    private void updateUI() {
        if(mCsvRecords==null) {
            List<Transaction> txns = mRetainedFragment.mLastFetchTransactions;
            mAdapter = new TxnAdapter(txns);
        } else {
            mAdapter = new TxnAdapter(mCsvRecords);
        }
        mTxnRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private class TxnHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        //private static final String SYMBOL_RS = "\u20B9 ";
        //private static final String ITEM_SEPERATOR = " | ";

        private Transaction mTxn;
        private String mCsvRecord;

        public EditText mDatetime;
        public EditText mCustId;
        public EditText mTxnId;
        
        public EditText mBillAmount;
        public View mAccountIcon;
        public EditText mAccountAmt;
        //public View mAmtsDivider;
        public View mCashbackIcon;
        public EditText mCashbackAmt;

        public EditText mCashbackAward;


        public TxnHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mDatetime = (EditText) itemView.findViewById(R.id.txn_time);
            mCustId = (EditText) itemView.findViewById(R.id.txn_customer_id);
            mTxnId = (EditText) itemView.findViewById(R.id.txn_id);

            mBillAmount = (EditText) itemView.findViewById(R.id.txn_bill);
            mAccountIcon = itemView.findViewById(R.id.txn_account_icon);
            mAccountAmt = (EditText) itemView.findViewById(R.id.txn_account_amt);
            //mAmtsDivider = itemView.findViewById(R.id.txn_amts_divider);
            mCashbackIcon = itemView.findViewById(R.id.txn_cashback_icon);
            mCashbackAmt = (EditText) itemView.findViewById(R.id.txn_cashback_amt);

            mCashbackAward = (EditText) itemView.findViewById(R.id.txn_cashback_award);
        }

        @Override
        public void onClick(View v) {
            // TODO - show detailed transaction view
        }

        public void bindTxn(Transaction txn) {
            mTxn = txn;
            mCsvRecord = null;

            mDatetime.setText(mSdf.format(mTxn.getCreate_time()));
            mCustId.setText(mTxn.getCustomer_id());
            mTxnId.setText(mTxn.getTrans_id());

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
                String cbData = AppCommonUtil.getAmtStr(mTxn.getCb_credit())+"  @ "+mTxn.getCb_percent()+"%";
                mCashbackAward.setText(cbData);
            } else {
                mCashbackAward.setText("-");
            }
        }

        public void bindTxnCsv(String csvRecord) {
            mCsvRecord = csvRecord;
            mTxn = null;

            // trans_id(0), time, merchant_id, merchant_name, customer_id, cust_private_id,
            // total_billed(6), cb_billed, cl_debit, cl_credit, cb_debit, cb_credit, cb_percent\n
            csvRecord = csvRecord.replace(CommonConstants.CSV_NEWLINE,"");
            String[] csvFields = csvRecord.split(CommonConstants.CSV_DELIMETER);

            mDatetime.setText(csvFields[1]);
            mCustId.setText(csvFields[4]);
            mTxnId.setText(csvFields[0]);

            int billAmt = Integer.parseInt(csvFields[6]);
            if(billAmt > 0) {
                mBillAmount.setText(AppCommonUtil.getSignedAmtStr(csvFields[6], true));
            } else {
                mBillAmount.setText("-");
            }

            int cl_credit = Integer.parseInt(csvFields[9]);
            int cl_debit = Integer.parseInt(csvFields[8]);
            if(cl_credit > 0) {
                mAccountAmt.setText(AppCommonUtil.getSignedAmtStr(csvFields[9], true));
                mAccountAmt.setTextColor(ContextCompat.getColor(getActivity(), R.color.green_positive));
            } else if(cl_debit > 0) {
                mAccountAmt.setText(AppCommonUtil.getSignedAmtStr(csvFields[8], false));
                mAccountAmt.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
            } else {
                mAccountIcon.setVisibility(View.GONE);
                mAccountAmt.setText("-");
            }

            int cb_debit = Integer.parseInt(csvFields[10]);
            if(cb_debit > 0) {
                mCashbackAmt.setText(AppCommonUtil.getSignedAmtStr(csvFields[10], false));
                mCashbackAmt.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
            } else {
                mCashbackIcon.setVisibility(View.GONE);
                mCashbackAmt.setText("-");
            }

            int cb_credit = Integer.parseInt(csvFields[11]);
            if(cb_credit > 0) {
                String cbData = AppCommonUtil.getAmtStr(csvFields[11])+"  @ "+csvFields[12]+"%";
                mCashbackAward.setText(cbData);
            } else {
                mCashbackAward.setText("-");
            }
        }

    }

    private class TxnAdapter extends RecyclerView.Adapter<TxnHolder> {
        private List<Transaction> mTxns;
        private ArrayList<String> mCsvRecords;

        public TxnAdapter(List<Transaction> txns) {
            mTxns = txns;
            mCsvRecords = null;
        }
        public TxnAdapter(ArrayList<String> txns) {
            mCsvRecords = txns;
            mTxns = null;
        }

        @Override
        public TxnHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater
                    .inflate(R.layout.transaction_itemview, parent, false);
            return new TxnHolder(view);
        }
        @Override
        public void onBindViewHolder(TxnHolder holder, int position) {
            if(mTxns==null) {
                holder.bindTxnCsv(mCsvRecords.get(position));
            } else {
                Transaction txn = mTxns.get(position);
                holder.bindTxn(txn);
            }
        }
        @Override
        public int getItemCount() {
            if(mTxns==null) {
                return mCsvRecords.size();
            } else {
                return mTxns.size();
            }
        }
    }
}*/