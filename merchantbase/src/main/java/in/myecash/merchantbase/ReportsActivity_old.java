package in.myecash.merchantbase;

/**
 * Created by adgangwa on 04-04-2016.
 */
/*
public class ReportsActivity extends AppCompatActivity implements
        View.OnClickListener, DialogFragmentWrapper.DialogFragmentWrapperIf,
        MyRetainedFragment.RetainedFragmentIf, DatePickerDialog.DatePickerIf,
        TxnSummaryFragment.TxnSummaryFragmentIf {
    private static final String TAG = "ReportsActivity";

    private static final String RETAINED_FRAGMENT = "workCashback";

    private static final String DIALOG_TXN_TYPE = "DialogTxnType";
    private static final String DIALOG_AMT_COMPARE = "DialogAmtCompare";
    private static final String DIALOG_DATE_FROM = "DialogDateFrom";
    private static final String DIALOG_DATE_TO = "DialogDateTo";

    private static final String TXN_TYPE_SEPARATOR = "&";
    private static final String TXN_LIST_FRAGMENT = "TxnListFragment";
    private static final String TXN_SUMMARY_FRAGMENT = "TxnSummaryFragment";

    // manually calculated as 100 - weight of remaining visible widgets
    // to be changed if weight of any widget is changed in XML
    private static final float END_SPACE_WEIGHT_MORE_FILTERS = 7.5f;
    private static final float END_SPACE_WEIGHT_LESS_FILTERS = 42.5f;

    // The column names corresponding to txn_types_array in strings.xml, in exact same order
    private static final String[] DB_TXN_TYPE_COLUMNS= {"total_billed","cl_credit","cl_debit","cb_credit","cb_debit"};

    //private String mRsSymbolStr;
    FragmentManager mFragMgr;
    MyRetainedFragment mWorkFragment;

    private ActionBar mActionBar;
    private Toolbar mToolbar;
    private MerchantUser mMerchant;

    private String[] mTxnTypes;
    // All required date formatters
    private SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
    private SimpleDateFormat mSdfOnlyDateBackend = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_BACKEND, CommonConstants.DATE_LOCALE);
    private SimpleDateFormat mSdfOnlyDateFilename = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_FILENAME, CommonConstants.DATE_LOCALE);
    private SimpleDateFormat mSdfOnlyDateDisplay = new SimpleDateFormat(CommonConstants.DATE_FORMAT_ONLY_DATE_DISPLAY, CommonConstants.DATE_LOCALE);


    //  TODO: restore below data attributes
    private boolean mReportToday;
    private Date mFromDate;
    private Date mToDate;
    private boolean[] mSelectedTxnArray;
    private int mAmtCompareSelectedIndx;
    private String mCustomerId;

    private ArrayList<String> mAllFiles = new ArrayList<>();
    private ArrayList<String> mMissingFiles = new ArrayList<>();
    private ArrayList<String> mFilteredCsvRecords;

    // Summary fields as array
    public static int INDEX_TXN_COUNT = 0;
    public static int INDEX_BILL_AMOUNT = 1;
    public static int INDEX_ADD_ACCOUNT = 2;
    public static int INDEX_DEBIT_ACCOUNT = 3;
    public static int INDEX_CASHBACK = 4;
    public static int INDEX_DEBIT_CASHBACK = 5;
    public static int INDEX_SUMMARY_MAX_VALUE = 6;
    private int mSummary[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        // gets handlers to screen resources
        bindUiResources();

        mMerchant = MerchantUser.getInstance();
        //mRsSymbolStr = getString(R.string.Rs)+" ";
        mFragMgr = getFragmentManager();

        // Setup a toolbar to replace the action bar.
        initToolbar();

        initDateInputs();
        // should be after DateInit only
        //initReportTypeBtns();
        mReportToday = true;
        initMoreFilters();
        initChoiceTxnType();
        initChoiceAmtCompare();
        initGetReportBtn();

        // Initialize retained fragment before other fragments
        // Check to see if we have retained the worker fragment.
        mWorkFragment = (MyRetainedFragment)mFragMgr.findFragmentByTag(RETAINED_FRAGMENT);
        // If not retained (or first time running), we need to create it.
        if (mWorkFragment == null) {
            LogMy.d(TAG, "Creating retained fragment instance");
            mWorkFragment = new MyRetainedFragment();
            // Tell it who it is working with.
            //mWorkFragment.setTargetFragment(this, 0);
            mFragMgr.beginTransaction().add(mWorkFragment, RETAINED_FRAGMENT).commit();
        }

    }

    @Override
    public void onClick(View v) {
        int vId = v.getId();
        LogMy.d(TAG, "In onClick: " + vId);

        switch(vId) {
            case R.id.input_date_from:
                if(!mReportToday) {
                    // Find the minimum date for DatePicker
                    int oldDays = (Integer)MyGlobalSettings.mSettings.get(DbConstants.SETTINGS_REPORTS_HISTORY_DAYS);
                    DateUtil minFrom = new DateUtil();
                    minFrom.removeDays(30);

                    DateUtil maxFrom = new DateUtil();
                    maxFrom.removeDays(1);

                    DialogFragment fromDialog = DatePickerDialog.newInstance(mFromDate, minFrom.getTime(), maxFrom.getTime());
                    fromDialog.show(getFragmentManager(), DIALOG_DATE_FROM);
                }
                break;
            case R.id.input_date_to:
                if(!mReportToday) {
                    if(mFromDate==null) {
                        Toast.makeText(this, "Set From date first", Toast.LENGTH_LONG).show();
                    } else {
                        DateUtil maxTo = new DateUtil();
                        maxTo.removeDays(1);

                        DialogFragment toDialog = DatePickerDialog.newInstance(mToDate, mFromDate, maxTo.getTime());
                        toDialog.show(getFragmentManager(), DIALOG_DATE_TO);
                    }
                }
                break;

            case R.id.input_more_filter:
                changeMoreFiltersVisibility(true);
                break;
            case R.id.input_less_filter:
                //resetMoreFilters();
                changeMoreFiltersVisibility(false);
                break;

            default:
                break;
        }
    }

    private void initGetReportBtn() {
        mBtnGetReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCustomerId = mInputCustId.getText().toString();
                if (mCustomerId.length() > 0) {
                    int errorCode = ValidationHelper.validateMobileNo(mCustomerId);
                    if (errorCode != ErrorCodes.NO_ERROR) {
                        mInputCustId.setError(AppCommonUtil.getErrorDesc(errorCode));
                        return;
                    }
                }
                // start thread to fetch data from DB
                // show progress dialog
                AppCommonUtil.showProgressDialog(ReportsActivity.this, AppConstants.progressReports);
                // init summary array
                mSummary = new int[INDEX_SUMMARY_MAX_VALUE];

                // data for today from backend
                // older data from the local db
                if (mReportToday) {
                    mFilteredCsvRecords = null;
                    // fetch data from backend
                    mWorkFragment.fetchTransactions(buildWhereClause());
                } else {
                    if (mFilteredCsvRecords == null) {
                        mFilteredCsvRecords = new ArrayList<>();
                    } else {
                        mFilteredCsvRecords.clear();
                    }
                    // fetch data from local db
                    // check files required for given dates
                    getTxnCsvFilePaths();
                    if (mMissingFiles.isEmpty()) {
                        showTxnFromFiles();
                    } else {
                        // fetch missing files
                        mWorkFragment.fetchTxnFiles(ReportsActivity.this, mMissingFiles);
                    }
                }
            }
        });
    }

    // returns array of txn CSV file paths, not available in local DB
    private void getTxnCsvFilePaths() {
        // loop through the dates for which report is required
        // check if file against the date is available locally
        // if not, fetch the same from backend
        long diff = Math.abs(mToDate.getTime() - mFromDate.getTime());
        int diffDays = (int) (diff / (24 * 60 * 60 * 1000));
        // Add 1 as both days are inclusive
        diffDays = diffDays + 1;

        DateUtil txnDay = new DateUtil(mFromDate);
        String merchantId = MerchantUser.getInstance().getMerchantId();
        mMissingFiles.clear();

        for(int i=0; i<diffDays; i++) {
            String filename = getTxnCsvFilename(txnDay.getTime(),merchantId);
            mAllFiles.add(filename);

            File file = getFileStreamPath(filename);
            if(file == null || !file.exists()) {
                // file does not exist
                LogMy.d(TAG,"Missing file: "+filename);
                String filepath = getMerchantTxnDir(merchantId) + filename;
                mMissingFiles.add(filepath);
            }
            txnDay.addDays(1);
        }
    }

    private String getMerchantTxnDir(String merchantId) {
        // directory: merchants/txn_files/<first 2 chars of merchant id>/<next 2 chars of merchant id>/<merchant id>/
        return CommonConstants.MERCHANT_TXN_ROOT_DIR +
                merchantId.substring(0,2) + CommonConstants.FILE_PATH_SEPERATOR +
                merchantId.substring(2,4) + CommonConstants.FILE_PATH_SEPERATOR +
                merchantId + CommonConstants.FILE_PATH_SEPERATOR;
    }

    private String getTxnCsvFilename(Date date, String merchantId) {
        // File name: txns_<merchant_id>_<ddMMMyy>.csv
        String fileName = CommonConstants.MERCHANT_TXN_FILE_PREFIX + merchantId + "_" + mSdfOnlyDateFilename.format(date) + CommonConstants.CSV_FILE_EXT;
        LogMy.d(TAG, "CSV filename: " + fileName);
        return fileName;
    }

    private String buildWhereClause() {
        StringBuilder whereClause = new StringBuilder();

        // customer and merchant id
        whereClause.append("merchant_id = '").append(mMerchant.getUser_id()).append("'");

        //SimpleDateFormat sdf = new SimpleDateFormat(AppConstants.DATE_FORMAT_ONLY_DATE_BACKEND, AppConstants.DATE_LOCALE);
        String fromDateStr = mSdfOnlyDateBackend.format(mFromDate);
        whereClause.append(" AND create_time >= '").append(fromDateStr).append("'");

        // increment to date by 1 day
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mToDate);
        calendar.add(Calendar.DATE, 1);
        String toDateStr = mSdfOnlyDateBackend.format(calendar.getTime());
        // we used '<' and not '<='
        whereClause.append(" AND create_time < '").append(toDateStr).append("'");

        if(mCustomerId.length() > 0) {
            whereClause.append(" AND customer_id = '").append(mCustomerId).append("'");
        }

        // If more filters are visible, use them*/
        /*
        if(mBtnLessFilters.getVisibility()==View.VISIBLE) {
            String amtStr = mInputAmount.getText().toString();
            int amt = 0;
            if(amtStr.length()>0) {
                amt = Integer.parseInt(amtStr);
            }

            // if amt=0 and all txn types selected - no need to add any filter
            if ( !isAllTxnTypesSelected() && amt>0 ) {
                String comparator = mOneChoiceAmtCompare.getText().toString();
                // loop and add filter for selected txn types
                for (int i=0; i<mSelectedTxnArray.length; i++) {
                    if(mSelectedTxnArray[i]) {
                        whereClause.append(" AND ").append(DB_TXN_TYPE_COLUMNS[i]).append(comparator).append(amt);
                    }
                }
            }
        }*//*
        return whereClause.toString();
    }

    private void initMoreFilters() {
        // For now disable 'more' filters
        // not completely removing from layout and code, as may be required later
        // if enabled later, corresponding handling CSV file processing will need to be added

        changeMoreFiltersVisibility(false);
        //mBtnMoreFilters.setOnClickListener(this);
        //mBtnLessFilters.setOnClickListener(this);
        mBtnMoreFilters.setVisibility(View.GONE);
        mBtnLessFilters.setVisibility(View.GONE);
        mBtnMoreFilters.setOnClickListener(null);
        mBtnLessFilters.setOnClickListener(null);
    }*/

    /*private void resetMoreFilters() {
        for (int i=0; i<mSelectedTxnArray.length; i++) {
            mSelectedTxnArray[i]=true;
        }
        mChoiceTxnTypes.setText("All");

        mAmtCompareSelectedIndx = 0;
        String[] array = getResources().getStringArray(R.array.amt_compare_array_short);
        mOneChoiceAmtCompare.setText(array[mAmtCompareSelectedIndx]);

        mInputAmount.setText("");
    }*//*

    private void initChoiceTxnType() {
        mTxnTypes = getResources().getStringArray(R.array.txn_types_array);

        if(mSelectedTxnArray==null) {
            mSelectedTxnArray = new boolean[mTxnTypes.length];
        }
        // init all items as selected
        for (int i=0; i<mSelectedTxnArray.length; i++) {
            mSelectedTxnArray[i]=true;
        }

        mChoiceTxnTypes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //AndroidUtil.hideKeyboard(ReportsActivity.this);
                DialogFragmentWrapper dialog = DialogFragmentWrapper.createMultipleChoiceDialog("Transaction types:", mTxnTypes, mSelectedTxnArray);
                dialog.show(getFragmentManager(), DIALOG_TXN_TYPE);
            }
        });
    }

    private void initChoiceAmtCompare() {
        final String[] amtCompareArray = getResources().getStringArray(R.array.amt_compare_array);
        mAmtCompareSelectedIndx = 0; // this should be greater then sign
        mOneChoiceAmtCompare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragmentWrapper dialog = DialogFragmentWrapper.createSingleChoiceDialog("Amount is:", amtCompareArray, mAmtCompareSelectedIndx, false);
                dialog.show(getFragmentManager(), DIALOG_AMT_COMPARE);
            }
        });
    }

    @Override
    public void onDialogResult(String tag, int position, ArrayList<Integer> selectedItemsIndexList) {
        if (tag.equals(DIALOG_TXN_TYPE)) {
            // reset all selections
            for (int i=0; i<mSelectedTxnArray.length; i++) {
                mSelectedTxnArray[i]=false;
            }

            // update mSelectedTxnArray as per new selection
            for (Integer temp : selectedItemsIndexList) {
                mSelectedTxnArray[temp] = true;
            }

            // update text shown
            if(selectedItemsIndexList.size() == 0) {
                mChoiceTxnTypes.setError("Select transaction types");
            } else if(isAllTxnTypesSelected()) {
                mChoiceTxnTypes.setText("All");
            } else {
                StringBuilder sb = new StringBuilder();
                for (Integer temp : selectedItemsIndexList) {
                    sb.append(mTxnTypes[temp]);
                    sb.append(TXN_TYPE_SEPARATOR);
                }
                // remove last TXN_TYPE_SEPARATOR
                int end = sb.length() - TXN_TYPE_SEPARATOR.length();
                mChoiceTxnTypes.setText(sb.substring(0,end));
            }
        } else if (tag.equals(DIALOG_AMT_COMPARE)) {
            mAmtCompareSelectedIndx = position;
            String[] array = getResources().getStringArray(R.array.amt_compare_array_short);
            mOneChoiceAmtCompare.setText(array[mAmtCompareSelectedIndx]);
        }
    }

    private boolean isAllTxnTypesSelected() {
        for (int i=0; i<mSelectedTxnArray.length; i++) {
            if( !mSelectedTxnArray[i] ) {
                return false;
            }
        }
        return true;
    }

    // Not removed: but not used currently, as more filters are disabled and not used for now
    private void changeMoreFiltersVisibility(boolean showMoreFilters) {
        int moreBtnVisibility =  showMoreFilters?View.GONE:View.VISIBLE;
        int otherVisibility =  showMoreFilters?View.VISIBLE:View.GONE;

        mSpaceMoreFilters.setVisibility(moreBtnVisibility);
        mBtnMoreFilters.setVisibility(moreBtnVisibility);

        mSpaceTxnTypes.setVisibility(otherVisibility);
        mLayoutTxnTypes.setVisibility(otherVisibility);
        mSpaceAmount.setVisibility(otherVisibility);
        mLayoutAmount.setVisibility(otherVisibility);
        mSpaceLessFilters.setVisibility(otherVisibility);
        mBtnLessFilters.setVisibility(otherVisibility);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mSpaceEnd.getLayoutParams();
        params.weight = showMoreFilters?END_SPACE_WEIGHT_MORE_FILTERS:END_SPACE_WEIGHT_LESS_FILTERS;
        mSpaceEnd.setLayoutParams(params);
    }

    private void initDateInputs() {
        //SimpleDateFormat format = new SimpleDateFormat(AppConstants.DATE_FORMAT_ONLY_DATE_DISPLAY, AppConstants.DATE_LOCALE);
        setFromDateState(false);
        setToDateState(false);

        mInputDateFrom.setOnClickListener(this);
        mInputDateTo.setOnClickListener(this);
    }


    @Override
    public void onDateSelected(Date argDate, String tag) {
        LogMy.d(TAG, "Selected date: " + argDate.toString());
        //SimpleDateFormat format = new SimpleDateFormat(AppConstants.DATE_FORMAT_ONLY_DATE_DISPLAY, AppConstants.DATE_LOCALE);
        if(tag.equals(DIALOG_DATE_FROM)) {
            mFromDate = argDate;
            mInputDateFrom.setText(mSdfOnlyDateDisplay.format(mFromDate));
        } else {
            mToDate = argDate;
            mInputDateTo.setText(mSdfOnlyDateDisplay.format(mToDate));
        }
    }


    private void setToDateState(boolean enable) {
        mInputDateTo.setEnabled(enable);
        //setDrawableTint(R.drawable.ic_date_range_black_24dp_copy, enable);
        float alpha = enable?1.0f:0.5f;
        mInputDateTo.setAlpha(alpha);
        // if disable case, set value to today
        if(!enable) {
            mToDate = new Date();
        } else {
            DateUtil date = new DateUtil();
            date.removeDays(1);
            mToDate = date.getTime();
        }
        mInputDateTo.setText(mSdfOnlyDateDisplay.format(mToDate));
    }
    private void setFromDateState(boolean enable) {
        mInputDateFrom.setEnabled(enable);
        //setDrawableTint(R.drawable.ic_date_range_black_24dp_copy, enable);
        float alpha = enable?1.0f:0.5f;
        mInputDateFrom.setAlpha(alpha);
        // if disable case, set value to today
        if(!enable) {
            mFromDate = new Date();
        } else {
            DateUtil date = new DateUtil();
            date.removeDays(1);
            mFromDate = date.getTime();
        }
        mInputDateFrom.setText(mSdfOnlyDateDisplay.format(mFromDate));
    }

    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        switch(view.getId()) {
            case R.id.radio_today:
                if (checked) {
                    if(!mReportToday) {
                        mReportToday = true;
                        setFromDateState(false);
                        setToDateState(false);
                    }
                }
                break;
            case R.id.radio_old:
                if (checked) {
                    if(mReportToday) {
                        mReportToday = false;
                        setFromDateState(true);
                        setToDateState(true);
                    }
                }
                break;
        }
    }

    private void initToolbar() {
        LogMy.d(TAG, "In initToolbar");
        mToolbar = (Toolbar) findViewById(R.id.toolbar_report);

        setSupportActionBar(mToolbar);
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
        getSupportActionBar().setTitle("History");
    }

    @Override
    public void onBgProcessResponse(int errorCode, int operation) {
        if(operation== MyRetainedFragment.REQUEST_FETCH_TXNS) {
            AppCommonUtil.cancelProgressDialog();
            if(errorCode==ErrorCodes.NO_ERROR) {
                // traverse txns fetched from DB and build summary
                addToSummary(mWorkFragment.mLastFetchTransactions);
                startTxnSummaryFragment();
                //startTxnListFragment();
            } else {
                DialogFragmentWrapper.createNotification(ErrorCodes.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                        .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            }
        } else if(operation== MyRetainedFragment.REQUEST_FETCH_TXN_FILES) {
            if(errorCode==ErrorCodes.NO_ERROR) {
                // all files should now be available locally
                showTxnFromFiles();
            } else if(errorCode==ErrorCodes.FILE_NOT_FOUND) {
                // one or more file not found
                // try to fetch un-archived txns from transaction table, if available
                mWorkFragment.fetchTransactions(buildWhereClause());
            } else {
                AppCommonUtil.cancelProgressDialog();
                DialogFragmentWrapper.createNotification(ErrorCodes.generalFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                        .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            }
        }
    }

    private void showTxnFromFiles() {
        boolean status = processFiles();
        // cancel progress dialog, only after processing all files
        AppCommonUtil.cancelProgressDialog();
        if(status) {
            LogMy.d(TAG,"CSV records to show: "+mFilteredCsvRecords.size());
            // mFilteredCsvRecords should have all csv records to display now
            //startTxnListFragment();
            startTxnSummaryFragment();
        }
    }

    private boolean processFiles() {
        boolean isCustomerFilter = false;
        if(mCustomerId != null && mCustomerId.length() > 0 )
        {
            isCustomerFilter = true;
        }

        for(int i=0; i<mAllFiles.size(); i++) {
            try {
                InputStream inputStream = openFileInput(mAllFiles.get(i));

                if ( inputStream != null ) {
                    int lineCnt = 0;

                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String receiveString = "";
                    while ( (receiveString = bufferedReader.readLine()) != null ) {
                        lineCnt++;
                        switch(lineCnt) {
                            case 1:
                                // this is header - do nothing
                                break;
                            default:
                                if(!receiveString.equals(CommonConstants.CSV_NEWLINE)) {
                                    processTxnCsvRecord(receiveString, isCustomerFilter);
                                }
                                break;
                        }
                    }
                    inputStream.close();
                    LogMy.d(TAG,mAllFiles.get(i)+": "+lineCnt);
                } else {
                    LogMy.e(TAG, "openFileInput returned null.");
                    return false;
                }
            }
            catch (FileNotFoundException e) {
                LogMy.e(TAG, "File not found: " + e.toString());
                return false;
            } catch (IOException e) {
                LogMy.e(TAG, "Can not read file: " + e.toString());
                return false;
            }
        }
        return true;
    }

    private void processTxnCsvRecord(String csvString, boolean isCustomerFilter) {
        // trans_id,time,merchant_id,merchant_name,customer_id,cust_private_id,
        // total_billed,cb_billed,cl_debit,cl_credit,cb_debit,cb_credit,cb_percent\n
        String[] csvFields = csvString.split(CommonConstants.CSV_DELIMETER);

        // Filter 1 : Customer Id
        if(!isCustomerFilter) {
            mFilteredCsvRecords.add(csvString);
            addToSummary(csvFields[6], csvFields[9], csvFields[8], csvFields[11], csvFields[10]);
        } else if(mCustomerId.equals(csvFields[4])) {
            mFilteredCsvRecords.add(csvString);
            addToSummary(csvFields[6], csvFields[9], csvFields[8], csvFields[11], csvFields[10]);
        }
    }

    private void addToSummary(List<Transaction> txns) {
        for (Transaction txn : txns) {
            mSummary[INDEX_TXN_COUNT]++;
            mSummary[INDEX_BILL_AMOUNT] = mSummary[INDEX_BILL_AMOUNT] + txn.getTotal_billed();
            mSummary[INDEX_ADD_ACCOUNT] = mSummary[INDEX_ADD_ACCOUNT] + txn.getCl_credit();
            mSummary[INDEX_DEBIT_ACCOUNT] = mSummary[INDEX_DEBIT_ACCOUNT] + txn.getCl_debit();
            mSummary[INDEX_CASHBACK] = mSummary[INDEX_CASHBACK] + txn.getCb_credit();
            mSummary[INDEX_DEBIT_CASHBACK] = mSummary[INDEX_DEBIT_CASHBACK] + txn.getCb_debit();
        }
    }

    private void addToSummary(String billAmt, String addAcc, String debitAcc, String cashback, String debitCashback) {
        mSummary[INDEX_TXN_COUNT]++;
        mSummary[INDEX_BILL_AMOUNT] = mSummary[INDEX_BILL_AMOUNT] + Integer.parseInt(billAmt);
        mSummary[INDEX_ADD_ACCOUNT] = mSummary[INDEX_ADD_ACCOUNT] + Integer.parseInt(addAcc);
        mSummary[INDEX_DEBIT_ACCOUNT] = mSummary[INDEX_DEBIT_ACCOUNT] + Integer.parseInt(debitAcc);
        mSummary[INDEX_CASHBACK] = mSummary[INDEX_CASHBACK] + Integer.parseInt(cashback);
        mSummary[INDEX_DEBIT_CASHBACK] = mSummary[INDEX_DEBIT_CASHBACK] + Integer.parseInt(debitCashback);
    }

    private void startTxnSummaryFragment() {
        Fragment fragment = mFragMgr.findFragmentByTag(TXN_SUMMARY_FRAGMENT);
        if (fragment == null) {
            LogMy.d(TAG,"Creating new txn summary fragment");

            // Create new fragment and transaction
            fragment = TxnSummaryFragment.newInstance(mSummary);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();

            // Add over the existing fragment
            mMainLayout.setVisibility(View.GONE);
            mFragmentContainer.setVisibility(View.VISIBLE);
            transaction.replace(R.id.fragment_container_report, fragment, TXN_SUMMARY_FRAGMENT);
            transaction.addToBackStack(TXN_SUMMARY_FRAGMENT);

            // Commit the transaction
            transaction.commit();

            getSupportActionBar().setTitle("Summary");
        }
    }

    @Override
    public void showTxnDetails() {
        startTxnListFragment();
    }

    private void startTxnListFragment() {
        Fragment fragment = mFragMgr.findFragmentByTag(TXN_LIST_FRAGMENT);
        if (fragment == null) {
            LogMy.d(TAG,"Creating new txn list fragment");

            // Create new fragment and transaction
            fragment = TxnListFragment.newInstance(mFilteredCsvRecords, mWorkFragment);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();

            // Add over the existing fragment
            mMainLayout.setVisibility(View.GONE);
            mFragmentContainer.setVisibility(View.VISIBLE);
            transaction.replace(R.id.fragment_container_report, fragment, TXN_LIST_FRAGMENT);
            transaction.addToBackStack(TXN_LIST_FRAGMENT);

            // Commit the transaction
            transaction.commit();

            getSupportActionBar().setTitle("Transactions");
        }
    }

    @Override
    public void setToolbarTitle(String title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void onBackPressed() {
        int count = getFragmentManager().getBackStackEntryCount();
        LogMy.d(TAG, "In onBackPressed: " + count);

        if (count == 0) {
            super.onBackPressed();
        } else {
            getFragmentManager().popBackStackImmediate();

            if(mFragmentContainer.getVisibility()==View.VISIBLE) {
                mMainLayout.setVisibility(View.VISIBLE);
                mFragmentContainer.setVisibility(View.GONE);
                getSupportActionBar().setTitle("History");
            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // this takes the user 'back', as if they pressed the left-facing triangle icon on the main android toolbar.
                // if this doesn't work as desired, another possibility is to call `finish()` here.
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
       */
/*
    private EditText mInputDateFrom;
    private EditText mInputDateTo;
    private EditText mInputCustId;
    private EditText mBtnMoreFilters;
    private LinearLayout mLayoutTxnTypes;
    private EditText mChoiceTxnTypes;
    private LinearLayout mLayoutAmount;
    private EditText mOneChoiceAmtCompare;
    private EditText mInputAmount;
    private EditText mBtnLessFilters;
    private AppCompatButton mBtnGetReport;

    private View mSpaceMoreFilters;
    private View mSpaceTxnTypes;
    private View mSpaceAmount;
    private View mSpaceLessFilters;
    private View mSpaceEnd;

    private LinearLayout mMainLayout;
    private FrameLayout mFragmentContainer;

    private void bindUiResources() {
        mInputDateFrom = (EditText) findViewById(R.id.input_date_from);
        mInputDateTo = (EditText) findViewById(R.id.input_date_to);
        mInputCustId = (EditText) findViewById(R.id.input_customer_id);
        mBtnMoreFilters = (EditText) findViewById(R.id.input_more_filter);
        mLayoutTxnTypes = (LinearLayout) findViewById(R.id.layout_report_transactions);
        mChoiceTxnTypes = (EditText) findViewById(R.id.input_transaction_type);
        mLayoutAmount = (LinearLayout) findViewById(R.id.layout_report_amount);
        mOneChoiceAmtCompare = (EditText) findViewById(R.id.input_amount_compare);
        mInputAmount = (EditText) findViewById(R.id.input_amount_value);
        mBtnLessFilters = (EditText) findViewById(R.id.input_less_filter);
        mBtnGetReport = (AppCompatButton) findViewById(R.id.btn_get_report);

        mSpaceMoreFilters = findViewById(R.id.space_more_filters);
        mSpaceTxnTypes = findViewById(R.id.space_transactions);
        mSpaceAmount = findViewById(R.id.space_report_amount);
        mSpaceLessFilters = findViewById(R.id.space_less_filters);
        mSpaceEnd = findViewById(R.id.space_end);

        mMainLayout = (LinearLayout) findViewById(R.id.layout_report_main);
        mFragmentContainer = (FrameLayout) findViewById(R.id.fragment_container_report);
    }

}
*/

    /*
    private void initReportTypeBtns() {
        setReportOnscreenState(true);
        mBtnReportOnscreen.setOnClickListener(this);

        setReportDownloadState(false);
        mBtnReportDownload.setOnClickListener(this);
    }
    private void setReportOnscreenState(boolean enable) {
        mIsReportOnscreen = enable;
        float alpha = enable?1.0f:0.5f;
        //mBtnReportOnscreen.setEnabled(enable);
        setDrawableTint(R.drawable.ic_view_list_black_24dp, enable);
        mBtnReportOnscreen.setAlpha(alpha);
    }
    private void setReportDownloadState(boolean enable) {
        mIsReportDownload = enable;
        float alpha = enable?1.0f:0.5f;
        //mBtnReportDownload.setEnabled(enable);
        setDrawableTint(R.drawable.ic_file_download_black_24dp, enable);
        mBtnReportDownload.setAlpha(alpha);
        // to date status is tied to reportType status
        //setToDateState(enable);
    }*/

    /*
    private void setDrawableTint(int drawableId, boolean isEnabled) {
        // Give 'disabled color' to drawable
        TypedArray a = obtainStyledAttributes(R.styleable.Label);

        int tintColor = -1;
        if(isEnabled) {
            tintColor = a.getColor(R.styleable.Label_android_textColorPrimary, -1);
        } else {
            tintColor = a.getColor(R.styleable.Label_android_textColorHint, -1);
        }
        a.recycle();

        LogMy.d(TAG, "tintColor = " + tintColor);
        if(tintColor != -1) {
            Drawable drawable = ContextCompat.getDrawable(this, drawableId);
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable.mutate(), tintColor);
        }
    }*/

