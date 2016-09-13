package in.myecash.merchantbase;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by adgangwa on 22-04-2016.
 */
public class DatePickerDialog extends DialogFragment
        implements android.app.DatePickerDialog.OnDateSetListener {

    private static final String TAG = "DatePickerFragment";
    private static final String ARG_DATE = "date";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MIN_DATE = "min_date";
    private static final String ARG_MAX_DATE = "max_date";

    private DatePickerIf mCallback;

    public interface DatePickerIf {
        void onDateSelected(Date date, String title);
    }

    public static DatePickerDialog newInstance(Date date, Date minDate, Date maxDate) {
        //LogMy.d(TAG, "Date: " + date.toString() + "minDate:  + minDate.toString() + "maxDate: " + maxDate.toString());
        Bundle args = new Bundle();
        args.putSerializable(ARG_DATE, date);
        args.putSerializable(ARG_MIN_DATE, minDate);
        args.putSerializable(ARG_MAX_DATE, maxDate);
        DatePickerDialog fragment = new DatePickerDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mCallback = (DatePickerIf) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement DatePickerIf");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        Date date = (Date) getArguments().getSerializable(ARG_DATE);
        Date minDate = (Date) getArguments().getSerializable(ARG_MIN_DATE);
        Date maxDate = (Date) getArguments().getSerializable(ARG_MAX_DATE);

        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        android.app.DatePickerDialog dateDialog = new android.app.DatePickerDialog(getActivity(), this, year, month, day);
        dateDialog.getDatePicker().setMinDate(minDate.getTime());
        dateDialog.getDatePicker().setMaxDate(maxDate.getTime());
        return dateDialog;
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        Date date = new GregorianCalendar(year, month, day).getTime();
        mCallback.onDateSelected(date, getTag());
    }
}