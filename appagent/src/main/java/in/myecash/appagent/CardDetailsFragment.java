package in.myecash.appagent;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.text.SimpleDateFormat;

import in.myecash.appagent.entities.AgentUser;
import in.myecash.appagent.helper.MyRetainedFragment;
import in.myecash.appbase.BaseFragment;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.common.CommonUtils;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.DbConstants;
import in.myecash.common.database.CustomerCards;
import in.myecash.common.database.Customers;

/**
 * Created by adgangwa on 13-12-2016.
 */
public class CardDetailsFragment extends BaseFragment {
    private static final String TAG = "AgentApp-CardDetailsFragment";

    private final SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

    public interface CardDetailsFragmentIf {
        MyRetainedFragment getRetainedFragment();
    }
    private CardDetailsFragmentIf mCallback;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (CardDetailsFragmentIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement CardDetailsFragmentIf");
        }
        initDialogView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateView");
        View v = inflater.inflate(R.layout.frag_card_details, container, false);

        // access to UI elements
        bindUiResources(v);
        return v;
    }

    private void initDialogView() {
        CustomerCards card = mCallback.getRetainedFragment().mCurrMemberCard;

        mInputQrCard.setText(card.getCardNum());
        mInputCardStatus.setText(DbConstants.cardStatusDescInternal[card.getStatus()]);
        if(card.getStatus() != DbConstants.CUSTOMER_CARD_STATUS_ACTIVE) {
            mInputCardStatus.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_negative));
        }
        mCardStatusDate.setText(mSdfDateWithTime.format(card.getStatus_update_time()));
        mCardStatusReason.setText( (card.getStatus_reason()==null)?"":card.getStatus_reason() );

        mCcntId.setText( (card.getCcntId()==null)?"":card.getCcntId() );
        mAgentId.setText( (card.getAgentId()==null)?"":card.getAgentId() );
        mMchntId.setText( (card.getMchntId()==null)?"":card.getMchntId() );
        mCustId.setText( (card.getCustId()==null)?"":card.getCustId() );
        
    }

    private EditText mInputQrCard;
    private EditText mInputCardStatus;
    private EditText mCardStatusDate;
    private EditText mCardStatusReason;

    private EditText mCcntId;
    private EditText mAgentId;
    private EditText mMchntId;
    private EditText mCustId;

    private void bindUiResources(View v) {
        mInputQrCard = (EditText) v.findViewById(R.id.input_card_id);
        mInputCardStatus = (EditText) v.findViewById(R.id.input_card_status);
        mCardStatusDate = (EditText) v.findViewById(R.id.input_card_status_date);
        mCardStatusReason = (EditText) v.findViewById(R.id.input_card_status_reason);

        mCcntId = (EditText) v.findViewById(R.id.input_ccnt_id);
        mAgentId = (EditText) v.findViewById(R.id.input_agent_id);
        mMchntId = (EditText) v.findViewById(R.id.input_mchnt_id);
        mCustId = (EditText) v.findViewById(R.id.input_cust_id);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCallback.getRetainedFragment().setResumeOk(true);
    }

    @Override
    public boolean handleTouchUp(View v) {
        // do nothing
        return false;
    }

    @Override
    public void handleBtnClick(View v) {
        // do nothing
    }


}
