package in.myecash.appagent;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import in.myecash.appagent.entities.AgentUser;
import in.myecash.commonbase.constants.DbConstants;
import in.myecash.commonbase.utilities.LogMy;

/**
 * Created by adgangwa on 28-02-2016.
 */
public class ActionsFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "ActionsFragment";

    // Possible merchant actions
    public static final String MERCHANT_REGISTER = "Register Merchant";
    public static final String MERCHANT_SEARCH = "Search Merchant";

    public static final int MAX_MERCHANT_BUTTONS = 2;
    // elements has to be <= MAX_MERCHANT_BUTTONS
    public static final String[] agentMerchantActions = {MERCHANT_SEARCH, MERCHANT_REGISTER};
    public static final String[] ccMerchantActions = {MERCHANT_SEARCH};

    private ActionsFragmentIf mCallback;

    // Container Activity must implement this interface
    public interface ActionsFragmentIf {
        public void onMerchantBtnClick(String action);
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
                String btnLabel = ((AppCompatButton)v).getText().toString();
                mCallback.onMerchantBtnClick(btnLabel);
                break;
        }
    }

    private void initButtons() {
        String[] actions = null;

        if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_AGENT) {
            actions = agentMerchantActions;
        } else if(AgentUser.getInstance().getUserType() == DbConstants.USER_TYPE_CC) {
            // customer care
            actions = ccMerchantActions;
        }

        // Init buttons for merchant actions
        for(int i=0; i<MAX_MERCHANT_BUTTONS; i++) {
            if(i<actions.length) {
                mMerchantBtns[i].setVisibility(View.VISIBLE);
                mMerchantBtns[i].setText(actions[i]);
                mMerchantBtns[i].setOnClickListener(this);
            } else {
                mMerchantBtns[i].setVisibility(View.GONE);
            }
        }

    }

    private AppCompatButton mMerchantBtns[] = new AppCompatButton[MAX_MERCHANT_BUTTONS];

    private void bindUiResources(View v) {
        mMerchantBtns[0] = (AppCompatButton) v.findViewById(R.id.btn_merchant_0);
        mMerchantBtns[1] = (AppCompatButton) v.findViewById(R.id.btn_merchant_1);
    }
}
