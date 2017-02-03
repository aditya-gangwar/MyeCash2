package in.myecash.appagent;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import in.myecash.appagent.entities.AgentUser;
import in.myecash.appagent.helper.MyRetainedFragment;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.appbase.utilities.LogMy;

/**
 * Created by adgangwa on 28-02-2016.
 */
public class ActionsFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "AgentApp-ActionsFragment";

    // Possible Merchant actions
    public static final String MERCHANT_REGISTER = "Register Merchant";
    public static final String MERCHANT_SEARCH = "Search Merchant";

    public static final int MAX_MERCHANT_BUTTONS = 2;
    // elements has to be <= MAX_MERCHANT_BUTTONS
    public static final String[] agentMerchantActions = {MERCHANT_SEARCH, MERCHANT_REGISTER};
    public static final String[] ccMerchantActions = {MERCHANT_SEARCH};


    // Possible Customer actions
    public static final String CUSTOMER_SEARCH = "Search Customer";

    public static final int MAX_CUSTOMER_BUTTONS = 2;
    // elements has to be <= MAX_CUSTOMER_BUTTONS
    public static final String[] agentCustomerActions = {};
    public static final String[] ccCustomerActions = {CUSTOMER_SEARCH};


    // Possible Member Card actions
    public static final String CARDS_SEARCH = "Search";
    // Bulk actions
    public static final String CARDS_UPLOAD = "Upload to Pool";
    public static final String CARDS_ALLOT_AGENT = "Allot to Agent";
    public static final String CARDS_ALLOT_MCHNT = "Allot to Merchant";
    public static final String CARDS_RETURN_MCHNT = "Return by Merchant";
    public static final String CARDS_RETURN_AGENT = "Return by Agent";
    // Map from Bulk action local code -> backend code
    public static final Map<String, String> cardsActionCodeMap;
    static {
        Map<String, String> aMap = new HashMap<>(10);

        aMap.put(CARDS_UPLOAD, CommonConstants.CARDS_UPLOAD_TO_POOL);
        aMap.put(CARDS_ALLOT_AGENT, CommonConstants.CARDS_ALLOT_TO_AGENT);
        aMap.put(CARDS_ALLOT_MCHNT, CommonConstants.CARDS_ALLOT_TO_MCHNT);
        aMap.put(CARDS_RETURN_MCHNT, CommonConstants.CARDS_RETURN_BY_MCHNT);
        aMap.put(CARDS_RETURN_AGENT, CommonConstants.CARDS_RETURN_BY_AGENT);

        cardsActionCodeMap = Collections.unmodifiableMap(aMap);
    }

    public static final int MAX_CARDS_BUTTONS = 4;
    // elements has to be <= MAX_CARDS_BUTTONS
    public static final String[] ccCardsActions = {CARDS_SEARCH};
    public static final String[] agentCardsActions = {CARDS_SEARCH, CARDS_ALLOT_MCHNT, CARDS_RETURN_MCHNT};
    public static final String[] cCntCardsActions = {CARDS_SEARCH, CARDS_UPLOAD, CARDS_ALLOT_AGENT, CARDS_RETURN_AGENT};


    // Possible other actions
    public static final String OTHER_GLOBAL_SETTINGS = "Global Settings";

    public static final int MAX_OTHER_BUTTONS = 2;
    // elements has to be <= MAX_OTHER_BUTTONS
    public static final String[] agentOtherActions = {OTHER_GLOBAL_SETTINGS};
    public static final String[] ccOtherActions = {OTHER_GLOBAL_SETTINGS};
    public static final String[] cCntOtherActions = {OTHER_GLOBAL_SETTINGS};


    private ActionsFragmentIf mCallback;

    // Container Activity must implement this interface
    public interface ActionsFragmentIf {
        void onActionBtnClick(String action);
        MyRetainedFragment getRetainedFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        LogMy.d(TAG, "In onActivityCreated");
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (ActionsFragmentIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement ActionsFragmentIf");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateView");
        View v = inflater.inflate(R.layout.fragment_actions, container, false);

        // access to UI elements
        bindUiResources(v);
        //setup buttons
        initButtons();

        return v;
    }

    @Override
    public void onClick(View v) {
        int vId = v.getId();
        LogMy.d(TAG, "In onClick: " + vId);

        switch(vId) {
            case R.id.btn_merchant_0:
            case R.id.btn_merchant_1:
            case R.id.btn_customer_0:
            case R.id.btn_customer_1:
            case R.id.btn_cards_0:
            case R.id.btn_cards_1:
            case R.id.btn_cards_2:
            case R.id.btn_cards_3:
            case R.id.btn_others_0:
            case R.id.btn_others_1:
                String btnLabel = ((AppCompatButton)v).getText().toString();
                mCallback.onActionBtnClick(btnLabel);
                break;
        }
    }

    private void initButtons() {
        String[] actions = null;

        // Init buttons for merchant actions
        if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_AGENT) {
            actions = agentMerchantActions;
        } else if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_CC) {
            // customer care
            actions = ccMerchantActions;
        }

        if(actions==null || actions.length<=0) {
            mLabelMerchant.setVisibility(View.GONE);
            mLayoutMchntBtns.setVisibility(View.GONE);
        } else {
            for (int i = 0; i < MAX_MERCHANT_BUTTONS; i++) {
                if (i < actions.length) {
                    mMerchantBtns[i].setVisibility(View.VISIBLE);
                    mMerchantBtns[i].setText(actions[i]);
                    mMerchantBtns[i].setOnClickListener(this);
                } else {
                    mMerchantBtns[i].setVisibility(View.INVISIBLE);
                    mMerchantBtns[i].setEnabled(false);
                }
            }
        }


        // Init buttons for customer actions
        if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_AGENT) {
            actions = agentCustomerActions;
        } else if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_CC) {
            // customer care
            actions = ccCustomerActions;
        }

        if(actions==null || actions.length<=0) {
            mLabelCustomer.setVisibility(View.GONE);
            mLayoutCustBtns.setVisibility(View.GONE);
        } else {
            for (int i = 0; i < MAX_CUSTOMER_BUTTONS; i++) {
                if (i < actions.length) {
                    mCustomerBtns[i].setVisibility(View.VISIBLE);
                    mCustomerBtns[i].setText(actions[i]);
                    mCustomerBtns[i].setOnClickListener(this);
                } else {
                    mCustomerBtns[i].setVisibility(View.INVISIBLE);
                    mCustomerBtns[i].setEnabled(false);
                }
            }
        }


        // Init buttons for Member Cards actions
        if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_CC) {
            actions = ccCardsActions;
        } else if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_AGENT) {
            actions = agentCardsActions;
        } else if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_CCNT) {
            actions = cCntCardsActions;
        }

        if(actions==null || actions.length<=0) {
            mLabelCards.setVisibility(View.GONE);
            mLayoutCardBtns1.setVisibility(View.GONE);
            mLayoutCardBtns2.setVisibility(View.GONE);
        } else {
            for (int i = 0; i < MAX_CARDS_BUTTONS; i++) {
                if (i < actions.length) {
                    mCardsBtns[i].setVisibility(View.VISIBLE);
                    mCardsBtns[i].setText(actions[i]);
                    mCardsBtns[i].setOnClickListener(this);
                } else {
                    mCardsBtns[i].setVisibility(View.INVISIBLE);
                    mCardsBtns[i].setEnabled(false);
                }
            }
            if(actions.length < 3) {
                mLayoutCardBtns2.setVisibility(View.GONE);
            }
        }


        // Init buttons for other actions
        if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_AGENT) {
            actions = agentOtherActions;
        } else if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_CC) {
            actions = ccOtherActions;
        } else if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_CCNT) {
            actions = cCntOtherActions;
        }

        if(actions==null || actions.length<=0) {
            mLabelOthers.setVisibility(View.GONE);
            mLayoutOthers.setVisibility(View.GONE);
        } else {
            for (int i = 0; i < MAX_OTHER_BUTTONS; i++) {
                if (i < actions.length) {
                    mOtherBtns[i].setVisibility(View.VISIBLE);
                    mOtherBtns[i].setText(actions[i]);
                    mOtherBtns[i].setOnClickListener(this);
                } else {
                    mOtherBtns[i].setVisibility(View.INVISIBLE);
                    mOtherBtns[i].setEnabled(false);
                }
            }
        }
    }

    private AppCompatButton mMerchantBtns[] = new AppCompatButton[MAX_MERCHANT_BUTTONS];
    private AppCompatButton mCustomerBtns[] = new AppCompatButton[MAX_CUSTOMER_BUTTONS];
    private AppCompatButton mCardsBtns[] = new AppCompatButton[MAX_CARDS_BUTTONS];
    private AppCompatButton mOtherBtns[] = new AppCompatButton[MAX_OTHER_BUTTONS];

    private View mLabelMerchant;
    private View mLabelCustomer;
    private View mLabelCards;
    private View mLabelOthers;

    private View mLayoutMchntBtns;
    private View mLayoutCustBtns;
    private View mLayoutCardBtns1;
    private View mLayoutCardBtns2;
    private View mLayoutOthers;

    private void bindUiResources(View v) {
        mMerchantBtns[0] = (AppCompatButton) v.findViewById(R.id.btn_merchant_0);
        mMerchantBtns[1] = (AppCompatButton) v.findViewById(R.id.btn_merchant_1);

        mCustomerBtns[0] = (AppCompatButton) v.findViewById(R.id.btn_customer_0);
        mCustomerBtns[1] = (AppCompatButton) v.findViewById(R.id.btn_customer_1);

        mCardsBtns[0] = (AppCompatButton) v.findViewById(R.id.btn_cards_0);
        mCardsBtns[1] = (AppCompatButton) v.findViewById(R.id.btn_cards_1);
        mCardsBtns[2] = (AppCompatButton) v.findViewById(R.id.btn_cards_2);
        mCardsBtns[3] = (AppCompatButton) v.findViewById(R.id.btn_cards_3);

        mOtherBtns[0] = (AppCompatButton) v.findViewById(R.id.btn_others_0);
        mOtherBtns[1] = (AppCompatButton) v.findViewById(R.id.btn_others_1);

        mLabelMerchant = v.findViewById(R.id.label_merchant);
        mLabelCustomer = v.findViewById(R.id.label_customer);
        mLabelCards = v.findViewById(R.id.label_cards);
        mLabelOthers = v.findViewById(R.id.label_others);

        mLayoutMchntBtns = v.findViewById(R.id.layout_mchnt_btns);
        mLayoutCustBtns = v.findViewById(R.id.layout_cust_btns);
        mLayoutCardBtns1 = v.findViewById(R.id.layout_cards_btns_1);
        mLayoutCardBtns2 = v.findViewById(R.id.layout_cards_btns_2);
        mLayoutOthers = v.findViewById(R.id.layout_others_btns);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCallback.getRetainedFragment().setResumeOk(true);
    }


}
