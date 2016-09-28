
  /*******************************************************************
  * CustomerServices.java
  * Generated by Backendless Corp.
  ********************************************************************/
		
package in.myecash.customerbase.backendAPI;

import com.backendless.Backendless;

import in.myecash.appbase.constants.BackendSettings;
import in.myecash.common.database.Cashback;
import in.myecash.common.database.Customers;

public class CustomerServices
{
    static final String SERVICE_NAME = "CustomerServices";
    static final String SERVICE_VERSION_NAME = "1.0.0";
    static final String APP_VERSION = "v1";

    private static CustomerServices ourInstance = new CustomerServices();

    private CustomerServices(  )
    {
    }

    public static CustomerServices getInstance()
    {
        return ourInstance;
    }

    public static void initApplication()
    {
        Backendless.setUrl( BackendSettings.BACKENDLESS_HOST );
        // if you invoke this sample inside of android application, you should use overloaded "initApp" with "context" argument
        Backendless.initApp( BackendSettings.APPLICATION_ID, BackendSettings.ANDROID_SECRET_KEY, CustomerServices.APP_VERSION );
    }

    public Customers changeMobile(String verifyParam, String newMobile, String otp)
    {
        Object[] args = new Object[]{verifyParam, newMobile, otp};
        return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "changeMobile", args, Customers.class );
    }

    public java.util.List<Cashback> getCashbacks(long updatedSince)
    {
        Object[] args = new Object[]{updatedSince};
        return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "getCashbacks", args, java.util.List.class );
    }

    public void changePin(String oldPin, String newPin, String cardNum)
    {
        Object[] args = new Object[]{oldPin, newPin, cardNum};
        Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "changePin", args );
    }

}