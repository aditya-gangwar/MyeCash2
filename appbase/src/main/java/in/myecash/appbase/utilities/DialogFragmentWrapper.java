package in.myecash.appbase.utilities;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.WindowManager;

import in.myecash.appbase.R;
import in.myecash.common.constants.ErrorCodes;

import java.util.ArrayList;

/**
 * Created by adgangwa on 13-02-2016.
 */
public class DialogFragmentWrapper extends DialogFragment {
    private static final String TAG = "BaseApp-DialogFragmentWrapper";

    private static final String ARG_TITLE = "title";
    private static final String ARG_TYPE = "type";
    private static final String ARG_ITEMS = "items";
    private static final String ARG_MSG = "message";
    private static final String ARG_FRAG_CALL = "call_fragment";
    private static final String ARG_SELECTED = "selected_items";
    private static final String ARG_INFORM_NEGATIVE = "inform_negative";
    private static final String ARG_ERROR_NOTIFICATION = "error_notify";

    public static final String DIALOG_TYPE_SINGLE_CHOICE = "DialogSingleChoice";
    public static final String DIALOG_TYPE_MULTIPLE_CHOICE = "DialogMultipleChoice";
    public static final String DIALOG_CONFIRMATION = "DialogConfirmation";
    public static final String DIALOG_NOTIFICATION = "DialogNotification";

    public static final String EXTRA_SELECTION = "extraSelected";
    public static final String EXTRA_SELECTION_INDEX = "extraSelectedIndex";

    private DialogFragmentWrapperIf mListener;
    //private ArrayList<Integer> mSelectedItemsIndexList;

    public interface DialogFragmentWrapperIf {
        // Second parameter: 'index' for Single Choice dialogs
        // else it gives result code
        void onDialogResult(String tag, int indexOrResultCode, ArrayList<Integer> selectedItemsIndexList);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mListener = (DialogFragmentWrapperIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement DialogFragmentWrapperIf");
        }
    }

    public static DialogFragmentWrapper createSingleChoiceDialog(String title, CharSequence[] items, int checkedItem, boolean callFromFragment) {
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, DIALOG_TYPE_SINGLE_CHOICE);
        args.putCharSequenceArray(ARG_ITEMS, items);
        args.putString(ARG_TITLE, title);
        args.putInt(ARG_SELECTED,checkedItem);
        args.putBoolean(ARG_FRAG_CALL, callFromFragment);
        DialogFragmentWrapper fragment = new DialogFragmentWrapper();
        fragment.setArguments(args);
        return fragment;
    }

    /*
    public static DialogFragmentWrapper createMultipleChoiceDialog(String title, CharSequence[] items, boolean[] isSelectedArray) {
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, DIALOG_TYPE_MULTIPLE_CHOICE);
        args.putString(ARG_TITLE, title);
        args.putCharSequenceArray(ARG_ITEMS, items);
        args.putBooleanArray(ARG_SELECTED, isSelectedArray);
        DialogFragmentWrapper fragment = new DialogFragmentWrapper();
        fragment.setArguments(args);
        fragment.mSelectedItemsIndexList = new ArrayList<Integer>();
        for(int i=0; i<isSelectedArray.length; i++) {
            if(isSelectedArray[i]) {
                fragment.mSelectedItemsIndexList.add(i);
                LogMy.d(TAG, "mSelectedItemsIndexList:" + i);
            }
        }
        LogMy.d(TAG,fragment.mSelectedItemsIndexList.toString());

        return fragment;
    }*/

    public static DialogFragmentWrapper createConfirmationDialog(String title, String message, boolean callFromFragment, boolean informNegativeAlso) {
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, DIALOG_CONFIRMATION);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MSG, message);
        args.putBoolean(ARG_FRAG_CALL, callFromFragment);
        args.putBoolean(ARG_INFORM_NEGATIVE, informNegativeAlso);
        DialogFragmentWrapper fragment = new DialogFragmentWrapper();
        fragment.setArguments(args);
        return fragment;
    }

    public static DialogFragmentWrapper createNotification(String title, String message, boolean callFromFragment, boolean isError) {
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, DIALOG_NOTIFICATION);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MSG, message);
        args.putBoolean(ARG_FRAG_CALL, callFromFragment);
        args.putBoolean(ARG_ERROR_NOTIFICATION, isError);
        DialogFragmentWrapper fragment = new DialogFragmentWrapper();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String type = getArguments().getString(ARG_TYPE);
        String title = getArguments().getString(ARG_TITLE);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);

        if(type.equals(DIALOG_TYPE_SINGLE_CHOICE)) {
            final boolean frag_call = getArguments().getBoolean(ARG_FRAG_CALL);
            final CharSequence[] items = getArguments().getCharSequenceArray(ARG_ITEMS);
            int selected = getArguments().getInt(ARG_SELECTED);
            builder.setSingleChoiceItems(items, selected,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(frag_call) {
                            Intent intent = new Intent();
                            intent.putExtra(EXTRA_SELECTION,items[which]);
                            intent.putExtra(EXTRA_SELECTION_INDEX,which);
                            getTargetFragment().onActivityResult(getTargetRequestCode(), ErrorCodes.NO_ERROR, intent);
                        } else if (mListener != null) {
                            mListener.onDialogResult(getTag(), which, null);
                        }
                        dialog.dismiss();
                    }
                }
            );
        } else if (type.equals(DIALOG_CONFIRMATION)) {
            String msg = getArguments().getString(ARG_MSG);
            final boolean frag_call = getArguments().getBoolean(ARG_FRAG_CALL);
            final boolean inform_negative = getArguments().getBoolean(ARG_INFORM_NEGATIVE);
            //builder.setIcon(R.drawable.ic_help_white_24dp);
            builder.setIcon(AppCommonUtil.getTintedDrawable(getActivity(), R.drawable.ic_help_white_24dp, R.color.accent));
            builder.setMessage(msg);

            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(frag_call) {
                        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
                    } else if (mListener != null) {
                        mListener.onDialogResult(getTag(), Activity.RESULT_OK, null);
                    }
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (inform_negative) {
                        if (frag_call) {
                            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, null);
                        } else if (mListener != null) {
                            mListener.onDialogResult(getTag(), Activity.RESULT_CANCELED, null);
                        }
                    }
                    dialog.dismiss();
                }
            });
        } else if (type.equals(DIALOG_NOTIFICATION)) {
            String msg = getArguments().getString(ARG_MSG);
            final boolean frag_call = getArguments().getBoolean(ARG_FRAG_CALL);
            boolean isError = getArguments().getBoolean(ARG_ERROR_NOTIFICATION);
            if(isError) {
                builder.setIcon(AppCommonUtil.getTintedDrawable(getActivity(), R.drawable.ic_error_white_24dp, R.color.failure));
            } else {
                builder.setIcon(AppCommonUtil.getTintedDrawable(getActivity(), R.drawable.ic_check_circle_white_24dp, R.color.success));
            }
            //builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage(msg);

            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (frag_call && getTargetFragment()!=null) {
                        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
                    } else if (mListener != null) {
                        mListener.onDialogResult(getTag(), Activity.RESULT_OK, null);
                    }
                    dialog.dismiss();
                }
            });
        }
        /*
        else if (type.equals(DIALOG_TYPE_MULTIPLE_CHOICE)) {
            CharSequence[] items = getArguments().getCharSequenceArray(ARG_ITEMS);
            boolean[] isSelectedArray = getArguments().getBooleanArray(ARG_SELECTED);
            builder.setMultiChoiceItems(items, isSelectedArray, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    if (isChecked) {
                        mSelectedItemsIndexList.add(which);
                    } else if (mSelectedItemsIndexList.contains(which)) {
                        mSelectedItemsIndexList.remove((Integer)which);
                    }
                    LogMy.d(TAG,mSelectedItemsIndexList.toString());
                }
            });
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mListener.onDialogResult(getTag(), Activity.RESULT_OK, mSelectedItemsIndexList);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

        }*/

        Dialog dialog =  builder.create();
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AppCommonUtil.setDialogTextSize(DialogFragmentWrapper.this, (AlertDialog) dialog);
            }
        });

        return dialog;
    }
}
