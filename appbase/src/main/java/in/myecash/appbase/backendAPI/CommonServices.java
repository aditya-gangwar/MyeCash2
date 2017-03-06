
  /*******************************************************************
  * CommonServices.java
  * Generated by Backendless Corp.
  ********************************************************************/
		
package in.myecash.appbase.backendAPI;

  import com.backendless.Backendless;

  import in.myecash.common.constants.CommonConstants;
  import in.myecash.common.database.CustomerCards;
  import in.myecash.common.database.Customers;
  import in.myecash.common.database.MerchantOrders;
  import in.myecash.common.database.Merchants;


  public class CommonServices
  {
      static final String SERVICE_NAME = "CommonServices";
      static final String SERVICE_VERSION_NAME = "1.0.0";
      static final String APP_VERSION = "v1";

      private static CommonServices ourInstance = new CommonServices();

      private CommonServices(  )
      {
      }

      public static CommonServices getInstance()
      {
          return ourInstance;
      }

      public static void initApplication()
      {
          Backendless.setUrl( CommonConstants.BACKENDLESS_HOST );
          // if you invoke this sample inside of android application, you should use overloaded "initApp" with "context" argument
          Backendless.initApp( CommonConstants.APPLICATION_ID, CommonConstants.ANDROID_SECRET_KEY, CommonServices.APP_VERSION );
      }

      public void isSessionValid()
      {
          Object[] args = new Object[]{};
          Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "isSessionValid", args );
      }

      public void changePassword(String userId, String oldPasswd, String newPasswd)
      {
          Object[] args = new Object[]{userId, oldPasswd, newPasswd};
          Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "changePassword", args );
      }

      public Merchants getMerchant(String merchantId)
      {
          Object[] args = new Object[]{merchantId};
          return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "getMerchant", args, Merchants.class );
      }

      public Customers getCustomer(String custId)
      {
          Object[] args = new Object[]{custId};
          return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "getCustomer", args, Customers.class );
      }

      public String execCustomerOp(java.lang.String opCode, java.lang.String customerId, java.lang.String argCardId, java.lang.String otp, java.lang.String pin, java.lang.String opParam)
      {
          Object[] args = new Object[]{opCode, customerId, argCardId, otp, pin, opParam};
          return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "execCustomerOp", args, String.class );
      }

      public java.util.List<MerchantOrders> getMchntOrders(String merchantId, String orderId, String statusCsvStr)
      {
          Object[] args = new Object[]{merchantId,orderId,statusCsvStr};
          return Backendless.CustomService.invoke( SERVICE_NAME, SERVICE_VERSION_NAME, "getMchntOrders", args, java.util.List.class );
      }
  }
