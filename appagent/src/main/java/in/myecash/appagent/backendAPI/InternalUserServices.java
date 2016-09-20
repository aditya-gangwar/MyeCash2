
  /*******************************************************************
  * AgentServices.java
  * Generated by Backendless Corp.
  ********************************************************************/
		
package in.myecash.appagent.backendAPI;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;

import in.myecash.commonbase.models.Merchants;

  public class InternalUserServices
{
    static final String BACKENDLESS_HOST = "https://api.backendless.com";
    static final String SERVICE_NAME = "AgentServices";
    static final String SERVICE_VERSION_NAME = "1.0.0";
    static final String APP_VERSION = "v1";
    static final String APP_ID = "09667F8B-98A7-E6B9-FFEB-B2B6EE831A00";
    static final String SECRET_KEY = "BB557D9A-4C9C-84FE-FFA1-B15A476D7400";

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
        Backendless.setUrl( InternalUserServices.BACKENDLESS_HOST );
        // if you invoke this sample inside of android application, you should use overloaded "initApp" with "context" argument
        Backendless.initApp( InternalUserServices.APP_ID, InternalUserServices.SECRET_KEY, InternalUserServices.APP_VERSION );
    }



    public void registerMerchant(Merchants merchant)
    {
        Object[] args = new Object[]{merchant};
        Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "registerMerchant", args );
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
