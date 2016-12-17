package in.myecash.appagent;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.myecash.appbase.utilities.ValidationHelper;
import in.myecash.common.MyCardForAction;
import in.myecash.appagent.helper.MyRetainedFragment;
import in.myecash.appbase.barcodeReader.BarcodeCaptureActivity;
import in.myecash.appbase.constants.AppConstants;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.ErrorCodes;

/**
 * Created by adgangwa on 14-12-2016.
 */
public class CardsActionListFrag extends Fragment implements View.OnClickListener {
    private static final String TAG = "CardsActionListFrag";

    private static final String ARG_ACTION = "argAction";
    private static final int MAX_CARD_ONE_GO = 10;
    public static final int RC_BARCODE_CAPTURE_CARD_DIALOG = 9005;

    private static final int REQ_CONFIRM_ACTION = 1;
    private static final int REQ_NOTIFY_ERROR = 2;

    private MyRetainedFragment mRetainedFragment;
    private CardsActionListFragIf mCallback;
    String mAction;
    // Layout views
    private View mLayoutAllottee;
    private EditText mInputAllottee;
    private RecyclerView mRecyclerView;
    private EditText mTitleView;
    private AppCompatButton mBtnScan;
    private AppCompatButton mBtnAction;

    // Map from Action code -> display title string
    /*public static final Map<String, String> cardsActionToStr;
    static {
        Map<String, String> aMap = new HashMap<>(10);

        aMap.put(CommonConstants.CARDS_UPLOAD_TO_POOL,"Upload to Pool");
        aMap.put(CommonConstants.CARDS_ALLOT_TO_AGENT,"Allot to Agent");
        aMap.put(CommonConstants.CARDS_ALLOT_TO_MCHNT,"Allot to Merchant");
        aMap.put(CommonConstants.CARDS_RETURN_BY_MCHNT,"Return by Merchant");
        aMap.put(CommonConstants.CARDS_RETURN_BY_AGENT,"Return by Agent");

        cardsActionToStr = Collections.unmodifiableMap(aMap);
    }*/


    public interface CardsActionListFragIf {
        MyRetainedFragment getRetainedFragment();
        //void checkCardsForAction(String action);
        void execActionForCards(String cards, String action, String allocateTo);
        void showCardDetails(String cardNum);
    }


    public static CardsActionListFrag getInstance(String action) {
        Bundle args = new Bundle();
        args.putString(ARG_ACTION, action);

        CardsActionListFrag fragment = new CardsActionListFrag();
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LogMy.d(TAG, "In onActivityCreated");

        try {
            mCallback = (CardsActionListFragIf) getActivity();
            mRetainedFragment = mCallback.getRetainedFragment();

            mAction = getArguments().getString(ARG_ACTION);
            mTitleView.setText(mAction);
            mBtnAction.setText(mAction);

            switch (mAction) {
                case ActionsFragment.CARDS_ALLOT_AGENT:
                    mInputAllottee.setHint("Agent ID");
                    break;
                case ActionsFragment.CARDS_ALLOT_MCHNT:
                    mInputAllottee.setHint("Merchant ID");
                    break;
                default:
                    mLayoutAllottee.setVisibility(View.GONE);
            }

        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement CardsActionListFragIf");
        } catch(Exception e) {
            LogMy.e(TAG, "Exception is CardsActionListFrag:onActivityCreated", e);
            // unexpected exception - show error
            DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true);
            notDialog.setTargetFragment(this,REQ_NOTIFY_ERROR);
            notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_cards_action, container, false);

        mLayoutAllottee = view.findViewById(R.id.layout_allottee);
        mInputAllottee = (EditText) view.findViewById(R.id.input_allottee);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.cards_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mTitleView = (EditText) view.findViewById(R.id.title_action);
        mBtnScan = (AppCompatButton) view.findViewById(R.id.btn_scan);
        mBtnAction = (AppCompatButton) view.findViewById(R.id.btn_action);

        mBtnScan.setOnClickListener(this);
        mBtnAction.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        if(view.getId()==mBtnScan.getId()) {
            if(mRetainedFragment.mLastCardsForAction.size() < MAX_CARD_ONE_GO) {
                startCodeScan();
            } else {
                String txt = "Max limit is "+MAX_CARD_ONE_GO;
                AppCommonUtil.toast(getActivity(), txt);
            }

        } else if(view.getId()==mBtnAction.getId()) {
            // Validate Allottee ID
            int error = ErrorCodes.NO_ERROR;
            switch (mAction) {
                case ActionsFragment.CARDS_ALLOT_AGENT:
                    error = ValidationHelper.validateAgentId(mInputAllottee.getText().toString());
                    break;
                case ActionsFragment.CARDS_ALLOT_MCHNT:
                    error = ValidationHelper.validateMerchantId(mInputAllottee.getText().toString());
                    break;
            }

            if(error!=ErrorCodes.NO_ERROR) {
                mInputAllottee.setError(AppCommonUtil.getErrorDesc(error));
            } else {
                if (mRetainedFragment.mLastCardsForAction.size() > 0) {
                    // check if any card in pending state
                    boolean pendingCard = false;
                    for (MyCardForAction card :
                            mRetainedFragment.mLastCardsForAction) {
                        if (card.getActionStatus().equals(MyCardForAction.ACTION_STATUS_PENDING)) {
                            pendingCard = true;
                            break;
                        }
                    }

                    if(pendingCard) {
                        // ask for confirmation
                        String title = mRetainedFragment.mLastCardsForAction.size() + " Cards";
                        String msg = "Are you sure to " + mAction + " ?";
                        DialogFragmentWrapper dialog = DialogFragmentWrapper.createConfirmationDialog(title, msg, true, false);
                        dialog.setTargetFragment(this, REQ_CONFIRM_ACTION);
                        dialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_CONFIRMATION);
                    } else {
                        AppCommonUtil.toast(getActivity(), "No Pending Cards Added");
                    }
                } else {
                    AppCommonUtil.toast(getActivity(), "No Pending Cards Added");
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogMy.d(TAG,"In onActivityResult"+requestCode+","+resultCode);

        if (requestCode == RC_BARCODE_CAPTURE_CARD_DIALOG) {
            if (resultCode == ErrorCodes.NO_ERROR) {
                String code = data.getStringExtra(BarcodeCaptureActivity.BarcodeObject);
                LogMy.d(TAG,"Read customer QR code: "+code);

                //TODO: validate code - for length etc
                // required in case of totally different, or multiple qr code scan

                if(mRetainedFragment.mLastCardsForAction.size() < MAX_CARD_ONE_GO) {
                    // check for duplicates
                    boolean duplicate = false;
                    for (MyCardForAction card :
                            mRetainedFragment.mLastCardsForAction) {
                        if (card.getScannedCode().equals(code)) {
                            duplicate = true;
                            break;
                        }
                    }

                    if(!duplicate) {
                        MyCardForAction card = new MyCardForAction();
                        card.setScannedCode(code);
                        card.setActionStatus(MyCardForAction.ACTION_STATUS_PENDING);
                        mRetainedFragment.mLastCardsForAction.add(card);
                        updateUI();
                    } else {
                        AppCommonUtil.toast(getActivity(), "Already Added");
                    }
                    if(mRetainedFragment.mLastCardsForAction.size() < MAX_CARD_ONE_GO) {
                        startCodeScan();
                    }
                } else {
                    String txt = "Max limit is "+MAX_CARD_ONE_GO;
                    AppCommonUtil.toast(getActivity(), txt);
                }
            } else {
                LogMy.e(TAG,"Failed to read barcode");
            }

        } else if(requestCode == REQ_CONFIRM_ACTION) {
            LogMy.d(TAG, "Received action confirmation.");
            StringBuilder sb = new StringBuilder();
            // Make CSV strings of all cards, for which action is still pending
            for (MyCardForAction card :
                    mRetainedFragment.mLastCardsForAction) {
                if (card.getActionStatus().equals(MyCardForAction.ACTION_STATUS_PENDING)) {
                    sb.append(card.getScannedCode()).append(CommonConstants.CSV_DELIMETER);
                }
            }
            sb.deleteCharAt(sb.length()-1); //remove last ,
            mCallback.execActionForCards(sb.toString(), mAction, mInputAllottee.getText().toString());
        }
    }

    private void startCodeScan() {
        Intent intent = new Intent(getActivity(), BarcodeCaptureActivity.class);
        intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
        intent.putExtra(BarcodeCaptureActivity.UseFlash, false);

        startActivityForResult(intent, RC_BARCODE_CAPTURE_CARD_DIALOG);
    }

    public void updateUI() {
        if(mRetainedFragment.mLastCardsForAction!=null) {
            CardsForActionAdapter adapter = (CardsForActionAdapter) mRecyclerView.getAdapter();
            if(adapter==null) {
                LogMy.d(TAG, "Adapter not set yet");
                mRecyclerView.setAdapter(new CardsForActionAdapter(mRetainedFragment.mLastCardsForAction));
            } else {
                sortList();
                mRecyclerView.invalidate();
                adapter.notifyDataSetChanged();
                //adapter.refresh(mRetainedFragment.mLastCardsForAction);
            }
        } else {
            LogMy.e(TAG,"In updateUI: mLastCardsForAction is null");
        }
    }

    private void removeCard(MyCardForAction card) {
        int position = mRetainedFragment.mLastCardsForAction.indexOf(card);
        mRetainedFragment.mLastCardsForAction.remove(position);
        updateUI();
    }

    private void sortList() {
        if(mRetainedFragment.mLastCardsForAction!=null) {
            Collections.sort(mRetainedFragment.mLastCardsForAction, new MyCardComparator());
        } else {
            LogMy.e(TAG,"In sortList: mLastCardsForAction is null");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        AppCommonUtil.cancelToast();
    }

    private class CardForActionHolder extends RecyclerView.ViewHolder
            implements View.OnTouchListener, View.OnClickListener {

        private MyCardForAction mCard;

        private EditText mSno;
        private EditText mCardId;
        private EditText mStatus;
        private AppCompatImageButton mRemove;

        public CardForActionHolder(View itemView) {
            super(itemView);

            mSno = (EditText) itemView.findViewById(R.id.item_sno);
            mCardId = (EditText) itemView.findViewById(R.id.input_cardNum);
            mStatus = (EditText) itemView.findViewById(R.id.card_status);
            mRemove = (AppCompatImageButton) itemView.findViewById(R.id.card_remove);

            mSno.setOnTouchListener(this);
            mCardId.setOnTouchListener(this);
            mStatus.setOnTouchListener(this);

            mRemove.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            //TODO:
            removeCard(mCard);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(event.getAction()==MotionEvent.ACTION_UP) {
                LogMy.d(TAG,"In onTouch: "+v.getId());

                // getRootView was not working, so manually finding root view
                // depending upon views on which listener is set
                View rootView = null;
                if(v.getId()==mSno.getId() ||
                        v.getId()==mCardId.getId() ||
                        v.getId()==mStatus.getId()) {
                    rootView = (View) v.getParent();
                    LogMy.d(TAG,"Clicked first-a level view "+rootView.getId());
                    rootView.performClick();
                }
            }
            return true;
        }

        public void bindCb(MyCardForAction cb, int position) {
            mCard = cb;
            String sno = String.format("%02d",(position+1))+"-";
            mSno.setText(sno);

            if(mCard.getCardNum()!=null && !mCard.getCardNum().isEmpty()) {
                mCardId.setText(mCard.getCardNum());
            } else {
                String txt = "Card "+(position+1);
                mCardId.setText(txt);
            }

            mStatus.setText(mCard.getActionStatus());
            if(!mCard.getActionStatus().equals(MyCardForAction.ACTION_STATUS_PENDING)) {
                mRemove.setAlpha(0.5f);
                mRemove.setEnabled(false);
                if(mCard.getActionStatus().equals(MyCardForAction.ACTION_STATUS_OK)) {
                    mStatus.setTextColor(ContextCompat.getColor(getActivity(), R.color.green_positive));
                } else {
                    mStatus.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
                }
            }
        }
    }

    private class CardsForActionAdapter extends RecyclerView.Adapter<CardForActionHolder> {
        private List<MyCardForAction> mCards;
        private View.OnClickListener mListener;

        public CardsForActionAdapter(List<MyCardForAction> cards) {
            mCards = cards;
            mListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LogMy.d(TAG,"In onClickListener of cards list item");
                    int pos = mRecyclerView.getChildAdapterPosition(v);
                    if (pos >= 0 && pos < getItemCount()) {
                        MyCardForAction card = mCards.get(pos);
                        if(card.getCardNum()!=null && !card.getCardNum().isEmpty()) {
                            mCallback.showCardDetails(card.getCardNum());
                        } else {
                            AppCommonUtil.toast(getActivity(), "Action not done for this card");
                        }
                    } else {
                        LogMy.e(TAG,"Invalid position in onClickListener of customer list item: "+pos);
                    }
                }
            };
        }

        public void refresh(List<MyCardForAction> cards) {
            mCards = cards;
            notifyDataSetChanged();
        }

        public void remove(MyCardForAction item) {
            int position = mCards.indexOf(item);
            mCards.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, mCards.size());
        }

        @Override
        public CardForActionHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.cards_action_itemview, parent, false);
            view.setOnClickListener(mListener);
            return new CardForActionHolder(view);
        }
        @Override
        public void onBindViewHolder(CardForActionHolder holder, int position) {
            MyCardForAction cb = mCards.get(position);
            holder.bindCb(cb, position);
        }
        @Override
        public int getItemCount() {
            return mCards.size();
        }
    }

    /*
     * comparator functions for sorting
     */
    public static class MyCardComparator implements Comparator<MyCardForAction> {
        @Override
        public int compare(MyCardForAction lhs, MyCardForAction rhs) {
            // TODO: Handle null x or y values
            return compare(lhs.getCardNum(), rhs.getCardNum());
        }
        private static int compare(String a, String b) {
            if(a==null) {
                return -1;
            } else if(b==null) {
                return 1;
            } else {
                int res = String.CASE_INSENSITIVE_ORDER.compare(a, b);
                return (res != 0) ? res : a.compareTo(b);
            }
        }
    }
}
