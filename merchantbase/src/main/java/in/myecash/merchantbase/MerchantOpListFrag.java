package in.myecash.merchantbase;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import in.myecash.commonbase.constants.AppConstants;
import in.myecash.commonbase.constants.CommonConstants;
import in.myecash.commonbase.constants.DbConstants;
import in.myecash.commonbase.constants.ErrorCodes;
import in.myecash.commonbase.models.MerchantOps;
import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.DialogFragmentWrapper;
import in.myecash.commonbase.utilities.LogMy;
import in.myecash.merchantbase.entities.MerchantUser;
import in.myecash.merchantbase.entities.MyCashback;
import in.myecash.merchantbase.entities.MyCustomer;
import in.myecash.merchantbase.helper.MyRetainedFragment;

/**
 * Created by adgangwa on 18-09-2016.
 */
public class MerchantOpListFrag extends Fragment {
    private static final String TAG = "MerchantOpListFrag";

    private static final int REQ_NOTIFY_ERROR = 1;

    private SimpleDateFormat mSdfDateWithTime;

    private RecyclerView mRecyclerView;
    private MyRetainedFragment mRetainedFragment;
    private MerchantOpListFragIf mCallback;

    public interface MerchantOpListFragIf {
        MyRetainedFragment getRetainedFragment();
        void setDrawerState(boolean isEnabled);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LogMy.d(TAG, "In onActivityCreated");

        try {
            mCallback = (MerchantOpListFragIf) getActivity();
            mRetainedFragment = mCallback.getRetainedFragment();

            mSdfDateWithTime = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement MerchantOpListFragIf");
        } catch(Exception e) {
            LogMy.e(TAG, "Exception is MerchantOpListFrag:onActivityCreated", e);
            // unexpected exception - show error
            DialogFragmentWrapper notDialog = DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, ErrorCodes.appErrorDesc.get(ErrorCodes.GENERAL_ERROR), true, true);
            notDialog.setTargetFragment(this,REQ_NOTIFY_ERROR);
            notDialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mchnt_op_list, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.mchntOp_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }

    private void updateUI() {
        if(mRetainedFragment.mLastFetchMchntOps!=null) {
            mRecyclerView.setAdapter(new CbAdapter(mRetainedFragment.mLastFetchMchntOps));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogMy.d(TAG, "In onActivityResult :" + requestCode + ", " + resultCode);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCallback.setDrawerState(false);
        updateUI();
    }

    private class CbHolder extends RecyclerView.ViewHolder {

        private MerchantOps mOp;

        public EditText mOpName;
        public EditText mOpTime;
        public EditText mOpInitBy;
        public EditText mOpStatus;
        public EditText mOpParams;
        public EditText mOpTicketId;
        public EditText mOpReason;
        public EditText mOpRemarks;

        public CbHolder(View itemView) {
            super(itemView);

            mOpName = (EditText) itemView.findViewById(R.id.input_opname);
            mOpTime = (EditText) itemView.findViewById(R.id.input_op_time);
            mOpInitBy = (EditText) itemView.findViewById(R.id.input_init_by);
            mOpStatus = (EditText) itemView.findViewById(R.id.input_status);
            mOpParams = (EditText) itemView.findViewById(R.id.input_params);
            mOpTicketId = (EditText) itemView.findViewById(R.id.input_ticketId);
            mOpReason = (EditText) itemView.findViewById(R.id.input_reason);
            mOpRemarks = (EditText) itemView.findViewById(R.id.input_remarks);
        }

        public void bindOp(MerchantOps op) {
            mOp = op;

            mOpName.setText(op.getOp_code());
            mOpTime.setText(mSdfDateWithTime.format(op.getCreated()));
            String initBy = op.getInitiatedBy();
            if(op.getInitiatedVia()!=null && !op.getInitiatedVia().isEmpty()) {
                initBy = initBy+" Via "+op.getInitiatedVia();
            }
            mOpInitBy.setText(initBy);

            // Optional parameters
            if(op.getExtra_op_params()!=null && !op.getExtra_op_params().isEmpty()) {
                mOpParams.setText(op.getExtra_op_params());
            } else {
                mOpParams.setVisibility(View.GONE);
            }

            // ticket num and remarks are to be shown to customer care user only
            if(!mCallback.getRetainedFragment().mMerchantUser.isPseudoLoggedIn()) {
                mOpTicketId.setVisibility(View.GONE);
                mOpRemarks.setVisibility(View.GONE);
                mOpReason.setVisibility(View.GONE);
                mOpStatus.setVisibility(View.GONE);
            } else {
                String statusStr = "Status: "+op.getOp_status();
                mOpStatus.setText(statusStr);

                if(op.getTicketNum()!=null && !op.getTicketNum().isEmpty()) {
                    String ticketStr = "Ticket ID: "+op.getTicketNum();
                    mOpTicketId.setText(ticketStr);
                } else {
                    mOpTicketId.setVisibility(View.GONE);
                }

                if(op.getRemarks()!=null && !op.getRemarks().isEmpty()) {
                    String str = "Remarks: "+op.getRemarks();
                    mOpRemarks.setText(str);
                } else {
                    mOpRemarks.setVisibility(View.GONE);
                }

                if(op.getReason()!=null && !op.getReason().isEmpty()) {
                    String str = "Reason: "+op.getReason();
                    mOpReason.setText(str);
                } else {
                    mOpReason.setVisibility(View.GONE);
                }
            }
        }
    }

    private class CbAdapter extends RecyclerView.Adapter<CbHolder> {
        private List<MerchantOps> mOps;
        private View.OnClickListener mListener;

        public CbAdapter(List<MerchantOps> ops) {
            mOps = ops;
        }

        @Override
        public CbHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.merchant_op_itemview, parent, false);
            view.setOnClickListener(mListener);
            return new CbHolder(view);
        }
        @Override
        public void onBindViewHolder(CbHolder holder, int position) {
            MerchantOps op = mOps.get(position);
            holder.bindOp(op);
        }
        @Override
        public int getItemCount() {
            return mOps.size();
        }
    }
}

