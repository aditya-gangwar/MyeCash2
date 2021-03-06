
  /*******************************************************************
  * MerchantServices.java
  * Generated by Backendless Corp.
  ********************************************************************/
		
package in.myecash.merchantbase.backendAPI;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;


import in.myecash.appbase.constants.AppConstants;
import in.myecash.common.database.Cashback;
import in.myecash.common.database.MerchantOps;
import in.myecash.common.database.MerchantOrders;
import in.myecash.common.database.MerchantStats;
import in.myecash.common.database.Merchants;
import in.myecash.common.database.Transaction;

  public class MerchantServices
{
    static final String SERVICE_NAME = "MerchantServices";
    static final String SERVICE_VERSION_NAME = "1.0.0";
    static final String APP_VERSION = "v1";

    private static MerchantServices ourInstance = new MerchantServices();

    private MerchantServices(  )
    {
    }

    public static MerchantServices getInstance()
    {
        return ourInstance;
    }

    public static void initApplication()
    {
        Backendless.setUrl( AppConstants.BACKENDLESS_HOST );
        // if you invoke this sample inside of android application, you should use overloaded "initApp" with "context" argument
        Backendless.initApp( AppConstants.BACKENDLESS_APP_ID, AppConstants.ANDROID_SECRET_KEY, MerchantServices.APP_VERSION );
    }

    public Merchants changeMobile(java.lang.String verifyparam, java.lang.String newMobile, java.lang.String otp)
    {
        Object[] args = new Object[]{verifyparam, newMobile, otp};
        return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "changeMobile", args, Merchants.class );
    }

    public Merchants updateSettings(java.lang.String cbRate, boolean addClEnabled, java.lang.String email, java.lang.String contactPhone,
                                    boolean askLinkedInvNum, boolean linkedInvNumOptional, boolean invNumOnlyNmbrs,
                                    String ppCbRate, int ppMinAmt)
    {
        Object[] args = new Object[]{cbRate, addClEnabled, email, contactPhone, askLinkedInvNum, linkedInvNumOptional, invNumOnlyNmbrs, ppCbRate, ppMinAmt};
        return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "updateSettings", args, Merchants.class );
    }

    public void deleteTrustedDevice(java.lang.String deviceId, java.lang.String curDeviceId)
    {
        Object[] args = new Object[]{deviceId, curDeviceId};
        Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "deleteTrustedDevice", args);
    }

    public MerchantStats getMerchantStats(java.lang.String mchntId)
    {
        Object[] args = new Object[]{mchntId};
        return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "getMerchantStats", args, MerchantStats.class );
    }

    public java.util.List<MerchantOps> getMerchantOps(java.lang.String merchantId)
    {
        Object[] args = new Object[]{merchantId};
        return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "getMerchantOps", args, java.util.List.class );
    }

    public void archiveTxns()
    {
        Object[] args = new Object[]{};
        Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "archiveTxns", args );
    }

    public Cashback registerCustomer(java.lang.String customerMobile, java.lang.String dob, int sex, java.lang.String cardId, java.lang.String otp, java.lang.String firstName, java.lang.String lastName)
    {
        Object[] args = new Object[]{customerMobile, dob, sex, cardId, otp, firstName, lastName};
        return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "registerCustomer", args, Cashback.class );
    }

    public Cashback getCashback(java.lang.String merchantId, java.lang.String merchantCbTable, java.lang.String customerId, boolean debugLogs)
    {
        Object[] args = new Object[]{merchantId, merchantCbTable, customerId, debugLogs};
        return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "getCashback", args, Cashback.class );
    }

    public Transaction cancelTxn(java.lang.String txnId, java.lang.String cardId, java.lang.String pin, boolean isOtp)
    {
        Object[] args = new Object[]{txnId, cardId, pin, isOtp};
        return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "cancelTxn", args, Transaction.class);
    }

    public Transaction commitTxn(String csvTxnData, String pin, boolean isOtp)
    {
        Object[] args = new Object[]{csvTxnData,pin,isOtp};
        return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "commitTxn", args, Transaction.class );
    }

    public void generateTxnOtp(java.lang.String custMobileOrId)
    {
        Object[] args = new Object[]{custMobileOrId};
        Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "generateTxnOtp", args );
    }

    public void deleteMchntOrder(java.lang.String orderId)
    {
        Object[] args = new Object[]{orderId};
        Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "deleteMchntOrder", args );
    }

    public MerchantOrders createMchntOrder(java.lang.String itemSku, int itemQty, int totalPrice)
    {
        Object[] args = new Object[]{itemSku, itemQty, totalPrice};
        return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "createMchntOrder", args, in.myecash.common.database.MerchantOrders.class );
    }
}