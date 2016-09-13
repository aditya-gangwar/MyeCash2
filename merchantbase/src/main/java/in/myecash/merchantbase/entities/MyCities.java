package in.myecash.merchantbase.entities;

/**
 * Created by adgangwa on 19-02-2016.
 */

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.QueryOptions;
import in.myecash.commonbase.constants.CommonConstants;
import in.myecash.commonbase.models.Cities;
import in.myecash.commonbase.utilities.LogMy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MyCities
{
    private static String TAG="MyCities";

    private static ArrayList<String> mCityValueSet;
    private static HashMap<String,Cities> mObjectMap;

    public static void init() {
        if(mCityValueSet == null) {
            mCityValueSet = new ArrayList<String>();
        }
        if(mObjectMap == null) {
            mObjectMap = new HashMap<String,Cities>();
        }

        // Fetch all categories from DB and build value set
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        QueryOptions queryOptions = new QueryOptions("city");
        dataQuery.setQueryOptions(queryOptions);
        dataQuery.setPageSize(CommonConstants.dbQueryMaxPageSize);

        Backendless.Data.of( Cities.class ).find(dataQuery, new AsyncCallback<BackendlessCollection<Cities>>() {
            @Override
            public void handleResponse(BackendlessCollection<Cities> items) {
                Iterator<Cities> iterator = items.getCurrentPage().iterator();
                while (iterator.hasNext()) {
                    Cities item = iterator.next();
                    mObjectMap.put(item.getCity(), item);
                    mCityValueSet.add(item.getCity());
                }
                if( items.getCurrentPage().size() > 0 )
                    items.nextPage( this );
            }

            @Override
            public void handleFault(BackendlessFault backendlessFault) {
                LogMy.e(TAG, "Failed to get Cities rows: " + backendlessFault.getMessage());
                mCityValueSet = null;
                mObjectMap = null;
            }
        });
    }

    public static Cities getCityWithName(String name) {
        return mObjectMap==null?null:mObjectMap.get(name);
    }

    public static CharSequence[] getCityValueSet() {
        return mCityValueSet==null?null:mCityValueSet.toArray(new CharSequence[mCityValueSet.size()]);
    }
}
