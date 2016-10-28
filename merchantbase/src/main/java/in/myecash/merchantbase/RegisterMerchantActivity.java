package in.myecash.merchantbase;

/*
public class RegisterMerchantActivity extends AppCompatActivity
        implements MyRetainedFragment.RetainedFragmentIf,
        DialogFragmentWrapper.DialogFragmentWrapperIf,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private static final String TAG = "RegMerchantActivity";

    private static final int REQUEST_LOAD_IMAGE = 0;

    // Tags of dialogues created by this activity
    private static final String DIALOG_CATEGORY = "DialogCategory";
    private static final String DIALOG_CITY = "DialogCity";
    private static final String DIALOG_NO_INTERNET = "DialogNoInternet";
    private static final String DIALOG_REG_MERCHANT = "DialogRegMerchant";
    private static final String DIALOG_REG_SUCCESS = "DialogRegSuccess";
    private static final String DIALOG_REG_FAILED = "DialogRegFailure";
    private static final String DIALOG_BACK_BUTTON = "dialogBackButton";

    private static final String RETAINED_FRAGMENT_TAG = "workLogin";

    private Merchants mMerchant;
    private MerchantUser mMerchantUser;
    private Address mAddress;

    private File        mPhotoFile;
    private boolean     mImageUploaded;
    private boolean     mTermsAgreed;
    MyRetainedFragment mWorkFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_merchant);

        // gets handlers to screen resources
        bindUiResources();

        // check internet connectivity
        int resultCode = AppCommonUtil.isNetworkAvailableAndConnected(RegisterMerchantActivity.this);
        if ( resultCode != ErrorCodes.NO_ERROR) {
            // Show error notification dialog
            DialogFragmentWrapper.createNotification(ErrorCodes.noInternetTitle, AppCommonUtil.getErrorDesc(resultCode), false, true)
                    .show(getFragmentManager(), DIALOG_NO_INTERNET);
            return;
        }

        // fetch 'business category' and 'cities' value sets asynchronously
        MyCities.init();
        MyBusinessCategories.init();

        // init data members
        mMerchantUser = MerchantUser.getInstance();
        mMerchant = mMerchantUser.getMerchant();
        mAddress = new Address();

        // setup choice handlers
        initChoiceCategories();
        initChoiceCities();

        // activate Links from register screen to other activities
        makeLoginLink();
        makeTermsAndConditionsLink();

        // Initialize retained fragment - used for registration background thread
        // Check to see if we have retained the worker fragment.
        FragmentManager fm = getFragmentManager();
        mWorkFragment = (MyRetainedFragment)fm.findFragmentByTag(RETAINED_FRAGMENT_TAG);
        // If not retained (or first time running), we need to create it.
        if (mWorkFragment == null) {
            mWorkFragment = new MyRetainedFragment();
            fm.beginTransaction().add(mWorkFragment, RETAINED_FRAGMENT_TAG).commit();
        }

        // Image upload handling
        mImageUploaded=false;
        final Intent pickImageIntent = prepareImageUpload();
        mImageUploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(pickImageIntent, REQUEST_LOAD_IMAGE);
            }
        });

        // Location related
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                // The next two lines tell the new client that “this” current class will handle connection stuff
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                //fourth line adds the LocationServices API endpoint from GooglePlayServices
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds

        // check for location permission
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if(rc==PackageManager.PERMISSION_GRANTED) {
            mGoogleApiClient.connect();
        } else {
            // request permission
            requestLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION, RC_HANDLE_LOCATION_FINE);
        }

        // Register button listener
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get all input values
                getUiResourceValues();
                // register merchant
                registerMerchant();
            }
        });
    }

    @Override
    public void onDialogResult(String tag, int indexOrResultCode, ArrayList<Integer> selectedItemsIndexList) {
        switch(tag) {
            case DIALOG_NO_INTERNET:
                finish();
                break;
            case DIALOG_REG_SUCCESS:
                MerchantUser.reset();
                finish();
                break;
            case DIALOG_CATEGORY:
                String category = String.valueOf(MyBusinessCategories.getCategoryValueSet()[indexOrResultCode]);
                mMerchant.setBuss_category(MyBusinessCategories.getCategoryWithName(category));
                mCategoryTextRes.setText(category);
                mCategoryTextRes.setError(null);
                break;
            case DIALOG_CITY:
                String city = String.valueOf(MyCities.getCityValueSet()[indexOrResultCode]);
                Cities cityObj = MyCities.getCityWithName(city);
                mAddress.setCity(cityObj);
                mCityTextRes.setText(city);
                mCityTextRes.setError(null);
                mStateTextRes.setText(cityObj.getState());
                mStateTextRes.setError(null);
                break;
            case DIALOG_BACK_BUTTON:
                finish();
                break;
        }
    }

    private void registerMerchant() {
        if (validate()) {
            // set location
            mAddress.setLatitude(currentLatitude);
            mAddress.setLongitude(currentLongitude);

            mMerchant.setAddress(mAddress);
            // confirm for registration
            DialogFragmentWrapper.createConfirmationDialog(AppConstants.regConfirmTitle, AppConstants.regConfirmMsg, false, false)
                    .show(getFragmentManager(), DIALOG_REG_MERCHANT);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOAD_IMAGE) {
            if (resultCode == RESULT_OK) {
                updateDisplayImage(mPhotoFile);
            } else {
                LogMy.d(TAG, "Image crop return failure, Result code : "+resultCode);
            }
        }
    }

    private void updateDisplayImage(File image) {
        try {
            FileInputStream in =  new FileInputStream(image);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            Bitmap userImage = BitmapFactory.decodeStream(in, null, options);
            mImageUploadBtn.setImageBitmap(userImage);
            mImageUploaded = true;
        } catch (Exception e) {
            LogMy.d(TAG, "Failed to update display image: " + e.toString());
        }
    }

    private void makeTermsAndConditionsLink()
    {
        SpannableString termsPrompt = new SpannableString( getString( R.string.agree_terms_label ) );

        ClickableSpan clickableSpan = new ClickableSpan()
        {
            @Override
            public void onClick( View widget )
            {
            // Start terms and conditions activity
            Intent tcIntent = new Intent(RegisterMerchantActivity.this, TermsConditionsActivity.class );
            startActivity(tcIntent);
            }
        };

        String linkText = getString(R.string.agree_terms_link);
        int linkStartIndex = termsPrompt.toString().indexOf( linkText );
        int linkEndIndex = linkStartIndex + linkText.length();
        termsPrompt.setSpan(clickableSpan, linkStartIndex, linkEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        mTermsLink.setText(termsPrompt);
        mTermsLink.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void makeLoginLink()
    {
        SpannableString loginPrompt = new SpannableString( getString( R.string.login_label ) );

        ClickableSpan clickableSpan = new ClickableSpan()
        {
            @Override
            public void onClick( View widget )
            {
                RegisterMerchantActivity.this.finish();
            }
        };

        String linkText = getString(R.string.login_link);
        int linkStartIndex = loginPrompt.toString().indexOf(linkText);
        int linkEndIndex = linkStartIndex + linkText.length();
        loginPrompt.setSpan(clickableSpan, linkStartIndex, linkEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        mLoginLink.setText(loginPrompt);
        mLoginLink.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void onBgProcessResponse(int errorCode, int operation) {
        LogMy.d(TAG, "In onBgProcessResponse");

        if(operation== MyRetainedFragment.REQUEST_REGISTER_MERCHANT) {
            AppCommonUtil.cancelProgressDialog(true);
            if(errorCode==ErrorCodes.NO_ERROR) {
                mMerchant = mMerchantUser.getMerchant();
                mRegisterButton.setEnabled(true);

                // Return response to parent activity (login) and finish this activity
                setResult(RESULT_OK, null);

                String detail = AppConstants.mchntRegSuccessMsg;
                if(mMerchant.getDisplayImage()==null) {
                    // image upload failed
                    String detail_new = "Display image upload failed. Please try it later from 'Settings'.";
                    detail = detail.concat("\n");
                    detail = detail.concat("\n");
                    detail = detail.concat(detail_new);
                }

                // Show dialog
                DialogFragmentWrapper.createNotification(AppConstants.regSuccessTitle, detail, false, false)
                        .show(getFragmentManager(), DIALOG_REG_SUCCESS);

            } else {
                mRegisterButton.setEnabled(true);
                // Show error notification dialog
                DialogFragmentWrapper.createNotification(ErrorCodes.regFailureTitle, AppCommonUtil.getErrorDesc(errorCode), false, true)
                        .show(getFragmentManager(), DIALOG_REG_FAILED);
            }
        }
    }

    private boolean validate() {
        boolean valid = true;
        int errorCode = ErrorCodes.NO_ERROR;

        StringBuilder sb = new StringBuilder();
        sb.append("Correct following: ");

        // validate all fields and mark ones with error
        // return false if any invalid
        if(!mImageUploaded) {
            valid = false;
            errorCode=ErrorCodes.GENERAL_ERROR;
            sb.append("Display image; ");
        }

        errorCode = ValidationHelper.validateBrandName(mBrandNameTextRes.getText().toString());
        if( errorCode != ErrorCodes.NO_ERROR ) {
            valid = false;
            mBrandNameTextRes.setError(AppCommonUtil.getErrorDesc(errorCode));
            sb.append("Brand name; ");
        }

        if(mMerchant.getBuss_category() == null) {
            mCategoryTextRes.setError("Select business category");
            valid = false;
            sb.append("Business category; ");
        } else {
            mCategoryTextRes.setError(null);
        }

        errorCode = ValidationHelper.validateMobileNo(mMobileNoTextRes.getText().toString());
        if( errorCode != ErrorCodes.NO_ERROR ) {
            valid = false;
            mMobileNoTextRes.setError(AppCommonUtil.getErrorDesc(errorCode));
            sb.append("Mobile number; ");
        }

        errorCode = ValidationHelper.validateEmail(mEmailTextRes.getText().toString());
        if( errorCode != ErrorCodes.NO_ERROR ) {
            valid = false;
            mEmailTextRes.setError(AppCommonUtil.getErrorDesc(errorCode));
            sb.append("Email; ");
        }

        if(mAddress.getCity() == null) {
            mCityTextRes.setError("Select city");
            valid = false;
            sb.append("Address city; ");
        } else {
            mCityTextRes.setError(null);
        }

        errorCode = ValidationHelper.validateAddress(mAddressTextRes1.getText().toString());
        if( errorCode != ErrorCodes.NO_ERROR ) {
            valid = false;
            mAddressTextRes1.setError(AppCommonUtil.getErrorDesc(errorCode));
            sb.append("Address; ");
        }

        if(!mTermsAgreed) {
            valid = false;
            mTermsLink.setError("Agree to terms and conditions");
        } else {
            mTermsLink.setError(null);
        }

        if(currentLatitude==0 || currentLongitude==0) {
            valid = false;
            sb.append("Location");
        }

        if(!valid) {
            Toast.makeText(getBaseContext(), sb.toString(), Toast.LENGTH_LONG).show();
        }
        return valid;
    }

    private void initChoiceCategories() {
        mCategoryTextRes.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                FragmentManager fragManager = getFragmentManager();
                if ((event.getAction() == MotionEvent.ACTION_UP) && (fragManager.findFragmentByTag(DIALOG_CATEGORY) == null)) {
                    //LogMy.d(TAG, "In onTouch");
                    AppCommonUtil.hideKeyboard(RegisterMerchantActivity.this);
                    DialogFragmentWrapper dialog = DialogFragmentWrapper.createSingleChoiceDialog(getString(R.string.category_hint), MyBusinessCategories.getCategoryValueSet(),-1, false);
                    dialog.show(fragManager, DIALOG_CATEGORY);
                    return true;
                }
                return false;
            }
        });
        mCategoryTextRes.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                FragmentManager fragManager = getFragmentManager();
                if (hasFocus && (fragManager.findFragmentByTag(DIALOG_CATEGORY) == null)) {
                    //LogMy.d(TAG, "In onFocusChange");
                    AppCommonUtil.hideKeyboard(RegisterMerchantActivity.this);
                    DialogFragmentWrapper dialog = DialogFragmentWrapper.createSingleChoiceDialog(getString(R.string.category_hint), MyBusinessCategories.getCategoryValueSet(),-1, false);
                    dialog.show(fragManager, DIALOG_CATEGORY);
                }
            }
        });
    }

    private void initChoiceCities() {
        mCityTextRes.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                FragmentManager fragManager = getFragmentManager();
                if ((event.getAction() == MotionEvent.ACTION_UP) && (fragManager.findFragmentByTag(DIALOG_CITY) == null)) {
                    //LogMy.d(TAG, "In onTouch");
                    AppCommonUtil.hideKeyboard(RegisterMerchantActivity.this);
                    DialogFragmentWrapper dialog = DialogFragmentWrapper.createSingleChoiceDialog(getString(R.string.city_hint), MyCities.getCityValueSet(),-1, false);
                    dialog.show(fragManager, DIALOG_CITY);
                    return true;
                }
                return false;
            }
        });
        mCityTextRes.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                FragmentManager fragManager = getFragmentManager();
                if (hasFocus && (fragManager.findFragmentByTag(DIALOG_CITY) == null)) {
                    //LogMy.d(TAG, "In onFocusChange");
                    AppCommonUtil.hideKeyboard(RegisterMerchantActivity.this);
                    DialogFragmentWrapper dialog = DialogFragmentWrapper.createSingleChoiceDialog(getString(R.string.city_hint), MyCities.getCityValueSet(),-1, false);
                    dialog.show(fragManager, DIALOG_CITY);
                }
            }
        });
    }

    private Intent prepareImageUpload() {
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageIntent.setType("image/*");
        pickImageIntent.putExtra("crop", "true");

        Float imgWidth = getResources().getDimension(R.dimen.register_image_width);
        Float imgHeight = getResources().getDimension(R.dimen.register_image_height);

        int cropWidth = 4*imgWidth.intValue();
        int cropHeight = 4*imgHeight.intValue();

        // Crop size as twice to that of display size
        pickImageIntent.putExtra("outputX", cropWidth);
        pickImageIntent.putExtra("outputY", cropHeight);
        pickImageIntent.putExtra("aspectX", 1);
        pickImageIntent.putExtra("aspectY", 1);
        pickImageIntent.putExtra("scale", true);
        pickImageIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());

        mPhotoFile = AppCommonUtil.createLocalImageFile(this);
        PackageManager packageManager = this.getPackageManager();

        boolean canTakePhoto = (mPhotoFile != null) && pickImageIntent.resolveActivity(packageManager) != null;
        mImageUploadBtn.setEnabled(canTakePhoto);
        if (canTakePhoto) {
            Uri uri = Uri.fromFile(mPhotoFile);
            LogMy.d(TAG,"URI: "+uri);
            pickImageIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        }

        return pickImageIntent;
    }

    @Override
    public void onBackPressed() {
        DialogFragmentWrapper.createConfirmationDialog(AppConstants.exitGenTitle, AppConstants.exitRegActivityMsg, false, false).show(getFragmentManager(), DIALOG_BACK_BUTTON);
    }

    // ui resources
    private ImageView   mImageUploadBtn;
    private EditText    mBrandNameTextRes;
    private EditText    mCategoryTextRes;
    private EditText    mMobileNoTextRes;
    private EditText    mEmailTextRes;
    private EditText    mCityTextRes;
    private EditText    mStateTextRes;
    private EditText    mAddressTextRes1;
    private CheckBox    mTermsCheckBox;
    private Button      mRegisterButton;
    private TextView    mLoginLink;
    private TextView    mTermsLink;

    private void bindUiResources() {
        mImageUploadBtn     = (ImageView) findViewById(R.id.btn_upload_image);
        mBrandNameTextRes   = (EditText) findViewById(R.id.input_merchant_name);
        mCategoryTextRes    = (EditText) findViewById(R.id.edittext_category);
        mMobileNoTextRes    = (EditText) findViewById(R.id.input_merchant_mobile);
        mEmailTextRes       = (EditText) findViewById(R.id.input_merchant_email);
        mCityTextRes        = (EditText) findViewById(R.id.edittext_city);
        mStateTextRes       = (EditText) findViewById(R.id.edittext_state);
        mAddressTextRes1     = (EditText) findViewById(R.id.input_address_1);
        mTermsCheckBox      = (CheckBox) findViewById(R.id.checkBox_terms);
        mRegisterButton     = (Button) findViewById(R.id.btn_register);
        mLoginLink          = (TextView) findViewById(R.id.link_login);
        mTermsLink          = (TextView) findViewById(R.id.link_terms);
    }

    private void getUiResourceValues() {
        mMerchant.setName(mBrandNameTextRes.getText().toString());
        mMerchant.setMobile_num(mMobileNoTextRes.getText().toString());
        mMerchant.setEmail(mEmailTextRes.getText().toString());
        mAddress.setLine_1(mAddressTextRes1.getText().toString());
        mTermsAgreed = mTermsCheckBox.isChecked();
    }

    // Location related callbacks

    @Override
    protected void onResume() {
        super.onResume();
        //Now lets connect to the API
        //mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(this.getClass().getSimpleName(), "onPause()");

        //Disconnect from API onPause()
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    //Define a request code to send to Google Play services
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_LOCATION_FINE = 3;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private double currentLatitude;
    private double currentLongitude;

    @Override
    public void onConnected(Bundle bundle) {
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        if(rc==PackageManager.PERMISSION_GRANTED) {
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (location == null) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            } else {
                //If everything went fine lets get latitude and longitude
                currentLatitude = location.getLatitude();
                currentLongitude = location.getLongitude();

                LogMy.d(TAG, currentLatitude + " WORKS " + currentLongitude);
            }
            return;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Do nothing
    }

    @Override
    public void onLocationChanged(Location location) {
        // Do nothing
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                LogMy.e(TAG, "Exception in onConnectionFailed: " + e.toString());
                e.printStackTrace();
            }
        } else {
            LogMy.e(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    private void requestLocationPermission(String permission, final int handle) {
        LogMy.w(TAG, "Location permission is not granted. Requesting permission");

        final String[] permissions = new String[]{permission};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            ActivityCompat.requestPermissions(this, permissions, handle);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions, handle);
            }
        };

        final ScrollView scrollview_register = (ScrollView) findViewById(R.id.scrollview_register);
        Snackbar.make(scrollview_register, R.string.permission_location_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_LOCATION_FINE) {
            LogMy.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            LogMy.d(TAG, "Location permission granted - initialize the camera source");
            mGoogleApiClient.connect();
            return;
        }

        LogMy.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Location permission !!")
                .setMessage(R.string.no_location_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }


}*/
