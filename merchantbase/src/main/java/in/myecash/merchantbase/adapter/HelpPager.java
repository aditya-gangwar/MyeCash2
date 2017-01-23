package in.myecash.merchantbase.adapter;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.helpshift.support.Support;

/**
 * Created by adgangwa on 05-01-2017.
 */
public class HelpPager extends FragmentStatePagerAdapter {

    //integer to count number of tabs
    int tabCount;
    Activity activity;

    //Constructor to the class
    public HelpPager(FragmentManager fm, int tabCount, Activity activity) {
        super(fm);
        //Initializing tab count
        this.activity = activity;
        this.tabCount= tabCount;
    }

    //Overriding method getItem
    @Override
    public Fragment getItem(int position) {
        //Returning the current tabs
        switch (position) {
            case 0:
                return Support.getFAQsFragment(this.activity);
            case 1:
                return Support.getConversationFragment(this.activity);
            default:
                return null;
        }
    }

    //Overriden method getCount to get the number of tabs
    @Override
    public int getCount() {
        return tabCount;
    }
}
