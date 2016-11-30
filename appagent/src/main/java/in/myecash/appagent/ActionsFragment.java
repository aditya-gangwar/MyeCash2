package in.myecash.appagent;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import in.myecash.appagent.entities.AgentUser;
import in.myecash.common.constants.DbConstants;
import in.myecash.appbase.utilities.LogMy;

/**
 * Created by adgangwa on 28-02-2016.
 */
public class ActionsFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "ActionsFragment";

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

    // Possible other actions
    public static final String OTHER_GLOBAL_SETTINGS = "Global Settings";

    public static final int MAX_OTHER_BUTTONS = 2;
    // elements has to be <= MAX_OTHER_BUTTONS
    public static final String[] agentOtherActions = {OTHER_GLOBAL_SETTINGS};
    public static final String[] ccOtherActions = {OTHER_GLOBAL_SETTINGS};


    private ActionsFragmentIf mCallback;

    // Container Activity must implement this interface
    public interface ActionsFragmentIf {
        public void onActionBtnClick(String action);
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

        for(int i=0; i<MAX_MERCHANT_BUTTONS; i++) {
            if(i<actions.length) {
                mMerchantBtns[i].setVisibility(View.VISIBLE);
                mMerchantBtns[i].setText(actions[i]);
                mMerchantBtns[i].setOnClickListener(this);
            } else {
                mMerchantBtns[i].setVisibility(View.INVISIBLE);
                mMerchantBtns[i].setEnabled(false);
            }
        }

        // Init buttons for customer actions
        if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_AGENT) {
            actions = agentCustomerActions;
        } else if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_CC) {
            // customer care
            actions = ccCustomerActions;
        }

        boolean noBtnsVisible = true;
        for(int i=0; i<MAX_CUSTOMER_BUTTONS; i++) {
            if(i<actions.length) {
                mCustomerBtns[i].setVisibility(View.VISIBLE);
                mCustomerBtns[i].setText(actions[i]);
                mCustomerBtns[i].setOnClickListener(this);
                noBtnsVisible = false;
            } else {
                mCustomerBtns[i].setVisibility(View.INVISIBLE);
                mCustomerBtns[i].setEnabled(false);
            }
        }
        // only in case of customer - as for agent customer has no valid actions
        if(noBtnsVisible) {
            mLabelCustBtns.setVisibility(View.GONE);
            mLayoutCustBtns.setVisibility(View.GONE);
        }

        // Init buttons for other actions
        if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_AGENT) {
            actions = agentOtherActions;
        } else if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_CC) {
            // customer care
            actions = ccOtherActions;
        }

        for(int i=0; i<MAX_OTHER_BUTTONS; i++) {
            if(i<actions.length) {
                mOtherBtns[i].setVisibility(View.VISIBLE);
                mOtherBtns[i].setText(actions[i]);
                mOtherBtns[i].setOnClickListener(this);
            } else {
                mOtherBtns[i].setVisibility(View.INVISIBLE);
                mOtherBtns[i].setEnabled(false);
            }
        }

    }

    private AppCompatButton mMerchantBtns[] = new AppCompatButton[MAX_MERCHANT_BUTTONS];
    private AppCompatButton mCustomerBtns[] = new AppCompatButton[MAX_CUSTOMER_BUTTONS];
    private AppCompatButton mOtherBtns[] = new AppCompatButton[MAX_OTHER_BUTTONS];

    private View mLabelCustBtns;
    private View mLayoutCustBtns;

    private void bindUiResources(View v) {
        mMerchantBtns[0] = (AppCompatButton) v.findViewById(R.id.btn_merchant_0);
        mMerchantBtns[1] = (AppCompatButton) v.findViewById(R.id.btn_merchant_1);

        mCustomerBtns[0] = (AppCompatButton) v.findViewById(R.id.btn_customer_0);
        mCustomerBtns[1] = (AppCompatButton) v.findViewById(R.id.btn_customer_1);

        mOtherBtns[0] = (AppCompatButton) v.findViewById(R.id.btn_others_0);
        mOtherBtns[1] = (AppCompatButton) v.findViewById(R.id.btn_others_1);

        mLabelCustBtns = v.findViewById(R.id.label_cust_btns);
        mLayoutCustBtns = v.findViewById(R.id.layout_cust_btns);
    }
}
