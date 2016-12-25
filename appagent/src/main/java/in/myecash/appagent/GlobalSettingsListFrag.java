package in.myecash.appagent;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.List;

import in.myecash.common.constants.CommonConstants;
import in.myecash.common.MyGlobalSettings;
import in.myecash.appbase.utilities.LogMy;

/**
 * Created by adgangwa on 22-09-2016.
 */
public class GlobalSettingsListFrag extends Fragment {
    private static final String TAG = "AgentApp-GlobalSettingsListFrag";

    private SimpleDateFormat mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);
    private RecyclerView mRecyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gsettings_list, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.list_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }

    private void updateUI() {

        if(MyGlobalSettings.userVisibleSettings!=null) {
            mRecyclerView.setAdapter(new ListAdapter(MyGlobalSettings.userVisibleSettings));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogMy.d(TAG, "In onActivityResult :" + requestCode + ", " + resultCode);
    }

    @Override
    public void onResume() {
        super.onResume();
        //mCallback.setDrawerState(false);
        updateUI();
    }

    private class MyItemHolder extends RecyclerView.ViewHolder {

        private MyGlobalSettings.gSettings mSetting;

        public EditText mSettingName;
        public EditText mSettingValue;
        public EditText mSettingDesc;
        public EditText mSettingTime;

        public MyItemHolder(View itemView) {
            super(itemView);

            mSettingName = (EditText) itemView.findViewById(R.id.input_setting_name);
            mSettingValue = (EditText) itemView.findViewById(R.id.input_setting_value);
            mSettingDesc = (EditText) itemView.findViewById(R.id.input_setting_desc);
            mSettingTime = (EditText) itemView.findViewById(R.id.input_setting_updated);
        }

        public void bindOp(MyGlobalSettings.gSettings setting) {
            mSetting = setting;

            mSettingName.setText(mSetting.name);
            mSettingValue.setText(mSetting.value);
            mSettingDesc.setText(mSetting.description);
            String updated = "Updated: "+mSdfDateWithTime.format(mSetting.updated);
            mSettingTime.setText(updated);
        }
    }

    private class ListAdapter extends RecyclerView.Adapter<MyItemHolder> {
        private List<MyGlobalSettings.gSettings> mSettings;

        public ListAdapter(List<MyGlobalSettings.gSettings> settings) {
            mSettings = settings;
        }

        @Override
        public MyItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.global_setting_itemview, parent, false);
            return new MyItemHolder(view);
        }
        @Override
        public void onBindViewHolder(MyItemHolder holder, int position) {
            MyGlobalSettings.gSettings op = mSettings.get(position);
            holder.bindOp(op);
        }
        @Override
        public int getItemCount() {
            return mSettings.size();
        }
    }
}

    /*
    private MyRetainedFragment mRetainedFragment;
    private GlobalSettingsListFragIf mCallback;
    public interface GlobalSettingsListFragIf {
        MyRetainedFragment getRetainedFragment();
        void setDrawerState(boolean isEnabled);
    }*/

    /*@Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LogMy.d(TAG, "In onActivityCreated");

        try {
            mCallback = (GlobalSettingsListFragIf) getActivity();
            mRetainedFragment = mCallback.getRetainedFragment();

            mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement GlobalSettingsListFragIf");
        } catch(Exception e) {
            LogMy.e(TAG, "Exception is GlobalSettingsListFrag:onActivityCreated", e);
            // unexpected exception - show error
            DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true);
            notDialog.setTargetFragment(this,REQ_NOTIFY_ERROR);
            notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }*/



