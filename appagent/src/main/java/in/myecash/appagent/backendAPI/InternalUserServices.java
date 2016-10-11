
  /*******************************************************************
  * AgentServices.java
  * Generated by Backendless Corp.
  ********************************************************************/
		
package in.myecash.appagent.backendAPI;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;

import in.myecash.appbase.constants.BackendSettings;
import in.myecash.common.database.Merchants;

public class InternalUserServices
{
    static final String SERVICE_NAME = "InternalUserServices";
    static final String SERVICE_VERSION_NAME = "1.0.0";
    static final String APP_VERSION = "v1";

    private static InternalUserServices ourInstance = new InternalUserServices();

    private InternalUserServices(  )
    {
    }

    public static InternalUserServices getInstance()
    {
        return ourInstance;
    }

    public static void initApplication()
    {
        Backendless.setUrl( BackendSettings.BACKENDLESS_HOST );
        // if you invoke this sample inside of android application, you should use overloaded "initApp" with "context" argument
        Backendless.initApp( BackendSettings.APPLICATION_ID, BackendSettings.ANDROID_SECRET_KEY, InternalUserServices.APP_VERSION );
    }

    public String registerMerchant(Merchants merchant)
    {
        Object[] args = new Object[]{merchant};
        return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "registerMerchant", args, String.class );
    }

    public void registerMerchantAsync(Merchants merchant, AsyncCallback<Object> callback)
    {
        Object[] args = new Object[]{merchant};
        Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "registerMerchant", args, Object.class, callback);
    }

    public void disableMerchant(java.lang.String merchantId, java.lang.String ticketNum, java.lang.String reason, java.lang.String remarks)
    {
        Object[] args = new Object[]{merchantId, ticketNum, reason, remarks};
        Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "disableMerchant", args );
    }

    public void disableMerchantAsync(java.lang.String merchantId, java.lang.String ticketNum, java.lang.String reason, java.lang.String remarks, AsyncCallback<Object> callback)
    {
        Object[] args = new Object[]{merchantId, ticketNum, reason, remarks};
        Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "disableMerchant", args, Object.class, callback);
    }

}
