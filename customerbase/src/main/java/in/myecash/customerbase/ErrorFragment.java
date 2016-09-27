package in.myecash.customerbase;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import in.myecash.commonbase.utilities.AppCommonUtil;
import in.myecash.commonbase.utilities.LogMy;

/**
 * Created by adgangwa on 28-09-2016.
 */
public class ErrorFragment extends Fragment {
    private static final String TAG = "ErrorFragment";

    private static final String ARG_IS_INFO = "isInfo";
    private static final String ARG_INFO_TXT1 = "info1";
    private static final String ARG_INFO_TXT2 = "info2";

    public static ErrorFragment newInstance(boolean isInfo, String infoText1, String infoText2) {
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_INFO, isInfo);
        args.putString(ARG_INFO_TXT1, infoText1);
        args.putString(ARG_INFO_TXT2, infoText2);
        ErrorFragment fragment = new ErrorFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogMy.d(TAG, "In onCreateView");
        View v = inflater.inflate(R.layout.fragment_error, container, false);

        // access to UI elements
        bindUiResources(v);

        boolean isInfo = getArguments().getBoolean(ARG_IS_INFO);
        if(isInfo) {
            // Change icon and text
            mMainImage.setImageDrawable(AppCommonUtil.getTintedDrawable(getActivity(), R.drawable.ic_info_outline_white_48dp, R.color.success));

            String info1 = getArguments().getString(ARG_INFO_TXT1);
            String info2 = getArguments().getString(ARG_INFO_TXT2);

            if(info1!=null) {
                mLabelInfo1.setText(info1);
            } else {
                mLabelInfo1.setVisibility(View.GONE);
            }
            if(info2!=null) {
                mLabelInfo2.setText(info2);
            } else {
                mLabelInfo2.setVisibility(View.GONE);
            }
        }
        return v;
    }

    private ImageView mMainImage;
    private EditText mLabelInfo1;
    private EditText mLabelInfo2;

    private void bindUiResources(View v) {
        mMainImage = (ImageView) v.findViewById(R.id.image_main);
        mLabelInfo1 = (EditText) v.findViewById(R.id.label_info1);
        mLabelInfo2 = (EditText) v.findViewById(R.id.label_info2);
    }
}

