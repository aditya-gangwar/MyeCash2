package in.myecash.appbase;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import in.myecash.appbase.utilities.LogMy;

/**
 * Created by adgangwa on 22-04-2016.
 */
public class MyDatePickerDialog extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {
    private static final String TAG = "BaseApp-MyDatePickerDialog";

    private static final String ARG_DATE = "date";
    private static final String ARG_MIN_DATE = "min_date";
    private static final String ARG_MAX_DATE = "max_date";

    private MyDatePickerIf mCallback;

    public interface MyDatePickerIf {
        void onDateSelected(Date date, String title);
    }

    public static MyDatePickerDialog newInstance(long curValue, long minDate, long maxDate) {
        Bundle args = new Bundle();
        args.putLong(ARG_DATE, curValue);
        args.putLong(ARG_MIN_DATE, minDate);
        args.putLong(ARG_MAX_DATE, maxDate);
        MyDatePickerDialog fragment = new MyDatePickerDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (MyDatePickerIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement DatePickerIf");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        long curValue = getArguments().getLong(ARG_DATE);
        long minDate = getArguments().getLong(ARG_MIN_DATE);
        long maxDate = getArguments().getLong(ARG_MAX_DATE);

        LogMy.d(TAG,curValue+", "+minDate+", "+maxDate);

        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getDefault());
        c.setTimeInMillis(curValue);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        LogMy.d(TAG,year+", "+month+", "+day);

        // Create a new instance of MyDatePickerDialog and return it
        DatePickerDialog dateDialog = new DatePickerDialog(getActivity(), this, year, month, day);
        dateDialog.getDatePicker().setMinDate(minDate);
        dateDialog.getDatePicker().setMaxDate(maxDate);
        return dateDialog;
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        Date date = new GregorianCalendar(year, month, day).getTime();
        mCallback.onDateSelected(date, getTag());
    }
}