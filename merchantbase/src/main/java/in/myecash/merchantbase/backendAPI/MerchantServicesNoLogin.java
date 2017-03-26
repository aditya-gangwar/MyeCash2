
  /*******************************************************************
  * MerchantServicesNoLogin.java
  * Generated by Backendless Corp.
  ********************************************************************/
		
package in.myecash.merchantbase.backendAPI;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;

import in.myecash.appbase.constants.AppConstants;


  public class MerchantServicesNoLogin
{
    static final String SERVICE_NAME = "MerchantServicesNoLogin";
    static final String SERVICE_VERSION_NAME = "1.0.0";
    static final String APP_VERSION = "v1";

    private static MerchantServicesNoLogin ourInstance = new MerchantServicesNoLogin();

    private MerchantServicesNoLogin(  )
    {
    }

    public static MerchantServicesNoLogin getInstance()
    {
        return ourInstance;
    }

    public static void initApplication()
    {
        Backendless.setUrl( AppConstants.BACKENDLESS_HOST );
        // if you invoke this sample inside of android application, you should use overloaded "initApp" with "context" argument
        Backendless.initApp( AppConstants.BACKENDLESS_APP_ID, AppConstants.ANDROID_SECRET_KEY, MerchantServicesNoLogin.APP_VERSION );
    }




    public void resetMerchantPwd(String userId, String deviceId, String dob)
    {
        Object[] args = new Object[]{userId, deviceId, dob};
        Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "resetMerchantPwd", args );
    }
    
    /*public void setDeviceForLogin(String loginId, String deviceInfo, String rcvdOtp)
    {
        Object[] args = new Object[]{loginId, deviceInfo, rcvdOtp};
        Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "setDeviceForLogin", args );
    }*/
    
    public void sendMerchantId(String mobileNum, String deviceId)
    {
        Object[] args = new Object[]{mobileNum, deviceId};
        Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "sendMerchantId", args );
    }
    
}
