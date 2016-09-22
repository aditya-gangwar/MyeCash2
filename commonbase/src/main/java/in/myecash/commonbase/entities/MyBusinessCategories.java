package in.myecash.commonbase.entities;

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
import in.myecash.commonbase.models.BusinessCategories;
import in.myecash.commonbase.utilities.LogMy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MyBusinessCategories
{
    private static String TAG="MyBusinessCategories";

    private static ArrayList<String> mCategoryValueSet;
    private static HashMap<String,BusinessCategories> mObjectMap;

    public static void init() {
        if(mCategoryValueSet == null) {
            mCategoryValueSet = new ArrayList<String>();
        }
        if(mObjectMap == null) {
            mObjectMap = new HashMap<String,BusinessCategories>();
        }

        // Fetch all categories from DB and build value set
        BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        QueryOptions queryOptions = new QueryOptions("category_name");
        dataQuery.setQueryOptions(queryOptions);
        dataQuery.setPageSize(CommonConstants.dbQueryMaxPageSize);

        Backendless.Data.of( BusinessCategories.class ).find( dataQuery, new AsyncCallback<BackendlessCollection<BusinessCategories>>()
        {
            @Override
            public void handleResponse( BackendlessCollection<BusinessCategories> items )
            {
                Iterator<BusinessCategories> iterator = items.getCurrentPage().iterator();
                while( iterator.hasNext() )
                {
                    BusinessCategories category = iterator.next();
                    mObjectMap.put(category.getCategory_name(), category);
                    mCategoryValueSet.add(category.getCategory_name());
                }

                if( items.getCurrentPage().size() > 0 )
                    items.nextPage( this );
            }

            @Override
            public void handleFault( BackendlessFault backendlessFault )
            {
                LogMy.e(TAG, "Failed to get BusinessCategory rows: " + backendlessFault.getMessage());
                mCategoryValueSet = null;
                mObjectMap = null;
            }
        } );
    }

    public static BusinessCategories getCategoryWithName(String name) {
        return mObjectMap==null?null:mObjectMap.get(name);
    }

    public static CharSequence[] getCategoryValueSet() {
        return mCategoryValueSet==null?null:mCategoryValueSet.toArray(new CharSequence[mCategoryValueSet.size()]);
    }
}
