package in.myecash.internal;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

import in.myecash.internal.helper.MyRetainedFragment;
import in.myecash.appbase.BaseDialog;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.common.database.CustomerCards;

/**
 * Created by adgangwa on 07-03-2017.
 */

public class CardListDialog extends BaseDialog {
    public static final String TAG = "AgentApp-CardListDialog";

    private CardListDialogIf mListener;
    ListView listView;

    public interface CardListDialogIf {
        MyRetainedFragment getRetainedFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mListener = (CardListDialogIf) getActivity();

            ArrayList<String> values = new ArrayList<>();
            for (CustomerCards card :
                    mListener.getRetainedFragment().mLastFetchedCards) {
                values.add(card.getCardNum());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, values);
            listView.setAdapter(adapter);

        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement CardListDialogIf");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateView");
        View v = inflater.inflate(R.layout.dialog_card_list, container, false);

        // Get ListView object from xml
        listView = (ListView) v.findViewById(R.id.cardList);

        return v;
    }


    @Override
    public boolean handleTouchUp(View v) {
        return false;
    }

    @Override
    public void handleBtnClick(DialogInterface dialog, int which) {

    }
}
