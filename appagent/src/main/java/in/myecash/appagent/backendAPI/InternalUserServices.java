
  /*******************************************************************
  * AgentServices.java
  * Generated by Backendless Corp.
  ********************************************************************/
		
package in.myecash.appagent.backendAPI;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;


import in.myecash.appbase.constants.AppConstants;
import in.myecash.common.MyCardForAction;
import in.myecash.common.constants.CommonConstants;
import in.myecash.common.database.CustomerCards;
import in.myecash.common.database.MerchantOrders;
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
        Backendless.setUrl( AppConstants.BACKENDLESS_HOST );
        // if you invoke this sample inside of android application, you should use overloaded "initApp" with "context" argument
        Backendless.initApp( AppConstants.APPLICATION_ID, AppConstants.ANDROID_SECRET_KEY, InternalUserServices.APP_VERSION );
    }

    public String registerMerchant(Merchants merchant)
    {
        Object[] args = new Object[]{merchant};
        return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "registerMerchant", args, String.class );
    }

    public void disableMerchant(java.lang.String merchantId, java.lang.String ticketNum, java.lang.String reason, java.lang.String remarks)
    {
        Object[] args = new Object[]{merchantId, ticketNum, reason, remarks};
        Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "disableMerchant", args );
    }

    public void disableCustomer(boolean ltdModeCase, java.lang.String customerId, java.lang.String ticketNum, java.lang.String reason, java.lang.String remarks)
    {
        Object[] args = new Object[]{ltdModeCase, customerId, ticketNum, reason, remarks};
        Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "disableCustomer", args );
    }

    public java.util.List<MyCardForAction> execActionForCards(java.lang.String codes, java.lang.String action, java.lang.String allotToUserId, java.lang.String orderId, boolean getCardNumsOnly)
    {
        Object[] args = new Object[]{codes, action, allotToUserId, orderId, getCardNumsOnly};
        return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "execActionForCards", args, java.util.List.class );
    }

    public void disableCustCard(java.lang.String privateId, java.lang.String cardNum, java.lang.String ticketNum, java.lang.String reason, java.lang.String remarks)
    {
        Object[] args = new Object[]{privateId, cardNum, ticketNum, reason, remarks};
        Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "disableCustCard", args );
    }

    public CustomerCards getMemberCard(String cardId)
    {
        Object[] args = new Object[]{cardId};
        return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "getMemberCard", args, CustomerCards.class );
    }

    public java.util.List<CustomerCards> getAllottedCards(java.lang.String orderId)
    {
        Object[] args = new Object[]{orderId};
        return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "getAllottedCards", args, java.util.List.class );
    }

    public MerchantOrders changeOrderStatus(MerchantOrders updatedOrder)
    {
        Object[] args = new Object[]{updatedOrder};
        return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "changeOrderStatus", args, MerchantOrders.class );
    }

    public void clearDummyMchntData()
    {
        Object[] args = new Object[]{};
        Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "clearDummyMchntData", args );
    }


}
