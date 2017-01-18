package in.myecash.merchantbase;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import in.myecash.appbase.constants.AppConstants;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.constants.ErrorCodes;
import in.myecash.common.database.MerchantDevice;
import in.myecash.appbase.utilities.AppCommonUtil;
import in.myecash.appbase.utilities.DialogFragmentWrapper;
import in.myecash.appbase.utilities.LogMy;
import in.myecash.merchantbase.entities.MerchantUser;
import in.myecash.merchantbase.helper.MyRetainedFragment;

import java.util.List;

/**
 * Created by adgangwa on 29-05-2016.
 */
public class TrustedDevicesFragment extends Fragment
        implements View.OnClickListener {
    private static final String TAG = "MchntApp-TrustedDevicesFragment";
    private static final int REQ_CONFIRM_DEVICE_DELETE = 1;
    private static final int NOTIFY_DELETE_NOT_ALLOWED = 2;

    public interface TrustedDevicesFragmentIf {
        MyRetainedFragment getRetainedFragment();
        void deleteDevice();
        void setDrawerState(boolean isEnabled);
    }

    private TrustedDevicesFragmentIf mCallback;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LogMy.d(TAG, "In onActivityCreated");

        try {
            mCallback = (TrustedDevicesFragmentIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement TrustedDevicesFragmentIf");
        }
        mCallback.setDrawerState(false);
    }

    @Override
    public void onResume() {
        LogMy.d(TAG, "In onResume");
        super.onResume();
        mCallback.setDrawerState(false);
        mCallback.getRetainedFragment().setResumeOk(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateView");
        View v = inflater.inflate(R.layout.fragment_trusted_devices, container, false);

        try {
            // access to UI elements
            bindUiResources(v);

            // update values against available devices
            List<MerchantDevice> devices = MerchantUser.getInstance().getTrustedDeviceList();
            //SimpleDateFormat sdf = new SimpleDateFormat(CommonConstants.DATE_FORMAT_WITH_TIME, CommonConstants.DATE_LOCALE);

            int cnt = (devices.size() < CommonConstants.MAX_DEVICES_PER_MERCHANT) ? devices.size() : CommonConstants.MAX_DEVICES_PER_MERCHANT;
            for (int i = 0; i < cnt; i++) {
                device_layouts[i].setAlpha(1.0f);

                String comp = (devices.get(i).getManufacturer() == null) ? "Device" : devices.get(i).getManufacturer();
                String model = (devices.get(i).getModel() == null) ? String.valueOf(i + 1) : devices.get(i).getModel();
                String deviceName = comp + " " + model;
                device_names[i].setText(deviceName);
                device_deletes[i].setClickable(true);
                device_deletes[i].setOnClickListener(this);
            }
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in TrustedDevicesFragment: ", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            getActivity().onBackPressed();
        }
        return v;
    }

    RelativeLayout device_layouts[] = new RelativeLayout[CommonConstants.MAX_DEVICES_PER_MERCHANT];
    EditText device_names[] = new EditText[CommonConstants.MAX_DEVICES_PER_MERCHANT];
    //EditText device_login_times[] = new EditText[CommonConstants.MAX_DEVICES_PER_MERCHANT];
    ImageButton device_deletes[] = new ImageButton[CommonConstants.MAX_DEVICES_PER_MERCHANT];

    protected void bindUiResources(View view) {
        device_layouts[0] = (RelativeLayout) view.findViewById(R.id.device1_layout);
        device_names[0] = (EditText) view.findViewById(R.id.device1_name);
        device_deletes[0] = (ImageButton) view.findViewById(R.id.device1_delete);
        //device_login_times[0] = (EditText) view.findViewById(R.id.device1_value_login);

        device_layouts[1] = (RelativeLayout) view.findViewById(R.id.device2_layout);
        device_names[1] = (EditText) view.findViewById(R.id.device2_name);
        device_deletes[1] = (ImageButton) view.findViewById(R.id.device2_delete);
        //device_login_times[1] = (EditText) view.findViewById(R.id.device2_value_login);

        device_layouts[2] = (RelativeLayout) view.findViewById(R.id.device2_layout);
        device_names[2] = (EditText) view.findViewById(R.id.device3_name);
        device_deletes[2] = (ImageButton) view.findViewById(R.id.device3_delete);
        //device_login_times[2] = (EditText) view.findViewById(R.id.device3_value_login);
    }

    @Override
    public void onClick(View v) {
        if(!mCallback.getRetainedFragment().getResumeOk())
            return;

        int id = v.getId();

        try {
            List<MerchantDevice> devices = MerchantUser.getInstance().getTrustedDeviceList();

            int index = 0;
            if (id == R.id.device1_delete) {
                index = 0;

            } else if (id == R.id.device2_delete) {
                index = 1;

            } else if (id == R.id.device3_delete) {
                index = 2;

            }
            // delete of device from which it is logged in is not allowed
            /*if (devices.get(index).getDevice_id().equals(AppCommonUtil.getDeviceId(getActivity()))) {
                DialogFragmentWrapper dialog = DialogFragmentWrapper.createNotification(AppConstants.deviceDeleteTitle,
                        "Cannot remove device from which you are logged in.", true, true);
                dialog.setTargetFragment(this, NOTIFY_DELETE_NOT_ALLOWED);
                dialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);

            } else */if (devices.size() == 1) {
                DialogFragmentWrapper dialog = DialogFragmentWrapper.createNotification(AppConstants.deviceDeleteTitle,
                        "Cannot remove last trusted device.", true, true);
                dialog.setTargetFragment(this, NOTIFY_DELETE_NOT_ALLOWED);
                dialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
            } else {
                // ask for confirmation
                String deviceName = devices.get(index).getManufacturer() + " " + devices.get(index).getModel();
                mCallback.getRetainedFragment().toDeleteTrustedDeviceIndex = index;

                String msg = String.format(AppConstants.deviceDeleteMsg, deviceName);
                DialogFragmentWrapper dialog = DialogFragmentWrapper.createConfirmationDialog(AppConstants.deviceDeleteTitle, msg, true, false);
                dialog.setTargetFragment(this, REQ_CONFIRM_DEVICE_DELETE);
                dialog.show(getFragmentManager(), DialogFragmentWrapper.DIALOG_CONFIRMATION);
            }
        } catch (Exception e) {
            LogMy.e(TAG, "Exception in TrustedDevicesFragment: ", e);
            DialogFragmentWrapper.createNotification(AppConstants.generalFailureTitle, AppCommonUtil.getErrorDesc(ErrorCodes.GENERAL_ERROR), true, true)
                    .show(getFragmentManager(), DialogFragmentWrapper.DIALOG_NOTIFICATION);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if(requestCode == REQ_CONFIRM_DEVICE_DELETE) {
            LogMy.d(TAG, "Received delete device confirmation.");
            mCallback.deleteDevice();
        } else if (requestCode == NOTIFY_DELETE_NOT_ALLOWED) {
            // do nothing
        }
    }

}

