package in.myecash.common.database;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.geo.GeoPoint;
import com.backendless.persistence.BackendlessDataQuery;

public class MerchantOrders
{
    private String orderId;
    private String ownerId;
    private java.util.Date statusChangeTime;
    private String invoiceId;
    private String plannedPayMode;
    private java.util.Date deliveryTime;
    private String paymentRef;
    private String agentId;
    private java.util.Date createTime;
    private String merchantId;
    private String agentName;
    private java.util.Date created;
    private String actualPayMode;
    private String saleOrderId;
    private String status;
    private String statusChangeUser;
    private Integer itemQty;
    private String objectId;
    private java.util.Date updated;
    private String itemSku;
    private String rejectReason;
    private Integer totalPrice;

    public Integer getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Integer totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getOrderId()
    {
        return orderId;
    }

    public void setOrderId( String orderId )
    {
        this.orderId = orderId;
    }

    public String getOwnerId()
    {
        return ownerId;
    }

    public java.util.Date getStatusChangeTime()
    {
        return statusChangeTime;
    }

    public void setStatusChangeTime( java.util.Date statusChangeTime )
    {
        this.statusChangeTime = statusChangeTime;
    }

    public String getInvoiceId()
    {
        return invoiceId;
    }

    public void setInvoiceId( String invoiceId )
    {
        this.invoiceId = invoiceId;
    }

    public String getPlannedPayMode()
    {
        return plannedPayMode;
    }

    public void setPlannedPayMode( String plannedPayMode )
    {
        this.plannedPayMode = plannedPayMode;
    }

    public java.util.Date getDeliveryTime()
    {
        return deliveryTime;
    }

    public void setDeliveryTime( java.util.Date deliveryTime )
    {
        this.deliveryTime = deliveryTime;
    }

    public String getPaymentRef()
    {
        return paymentRef;
    }

    public void setPaymentRef( String paymentRef )
    {
        this.paymentRef = paymentRef;
    }

    public String getAgentId()
    {
        return agentId;
    }

    public void setAgentId( String agentId )
    {
        this.agentId = agentId;
    }

    public java.util.Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime( java.util.Date createTime )
    {
        this.createTime = createTime;
    }

    public String getMerchantId()
    {
        return merchantId;
    }

    public void setMerchantId( String merchantId )
    {
        this.merchantId = merchantId;
    }

    public String getAgentName()
    {
        return agentName;
    }

    public void setAgentName( String agentName )
    {
        this.agentName = agentName;
    }

    public java.util.Date getCreated()
    {
        return created;
    }

    public String getActualPayMode()
    {
        return actualPayMode;
    }

    public void setActualPayMode( String actualPayMode )
    {
        this.actualPayMode = actualPayMode;
    }

    public String getSaleOrderId()
    {
        return saleOrderId;
    }

    public void setSaleOrderId( String saleOrderId )
    {
        this.saleOrderId = saleOrderId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus( String status )
    {
        this.status = status;
    }

    public String getStatusChangeUser()
    {
        return statusChangeUser;
    }

    public void setStatusChangeUser( String statusChangeUser )
    {
        this.statusChangeUser = statusChangeUser;
    }

    public Integer getItemQty()
    {
        return itemQty;
    }

    public void setItemQty( Integer itemQty )
    {
        this.itemQty = itemQty;
    }

    public String getObjectId()
    {
        return objectId;
    }

    public java.util.Date getUpdated()
    {
        return updated;
    }

    public String getItemSku()
    {
        return itemSku;
    }

    public void setItemSku( String itemSku )
    {
        this.itemSku = itemSku;
    }


    public MerchantOrders save()
    {
        return Backendless.Data.of( MerchantOrders.class ).save( this );
    }

    public Future<MerchantOrders> saveAsync()
    {
        if( Backendless.isAndroid() )
        {
            throw new UnsupportedOperationException( "Using this method is restricted in Android" );
        }
        else
        {
            Future<MerchantOrders> future = new Future<MerchantOrders>();
            Backendless.Data.of( MerchantOrders.class ).save( this, future );

            return future;
        }
    }

    public void saveAsync( AsyncCallback<MerchantOrders> callback )
    {
        Backendless.Data.of( MerchantOrders.class ).save( this, callback );
    }

    public Long remove()
    {
        return Backendless.Data.of( MerchantOrders.class ).remove( this );
    }

    public Future<Long> removeAsync()
    {
        if( Backendless.isAndroid() )
        {
            throw new UnsupportedOperationException( "Using this method is restricted in Android" );
        }
        else
        {
            Future<Long> future = new Future<Long>();
            Backendless.Data.of( MerchantOrders.class ).remove( this, future );

            return future;
        }
    }

    public void removeAsync( AsyncCallback<Long> callback )
    {
        Backendless.Data.of( MerchantOrders.class ).remove( this, callback );
    }

    public static MerchantOrders findById( String id )
    {
        return Backendless.Data.of( MerchantOrders.class ).findById( id );
    }

    public static Future<MerchantOrders> findByIdAsync( String id )
    {
        if( Backendless.isAndroid() )
        {
            throw new UnsupportedOperationException( "Using this method is restricted in Android" );
        }
        else
        {
            Future<MerchantOrders> future = new Future<MerchantOrders>();
            Backendless.Data.of( MerchantOrders.class ).findById( id, future );

            return future;
        }
    }

    public static void findByIdAsync( String id, AsyncCallback<MerchantOrders> callback )
    {
        Backendless.Data.of( MerchantOrders.class ).findById( id, callback );
    }

    public static MerchantOrders findFirst()
    {
        return Backendless.Data.of( MerchantOrders.class ).findFirst();
    }

    public static Future<MerchantOrders> findFirstAsync()
    {
        if( Backendless.isAndroid() )
        {
            throw new UnsupportedOperationException( "Using this method is restricted in Android" );
        }
        else
        {
            Future<MerchantOrders> future = new Future<MerchantOrders>();
            Backendless.Data.of( MerchantOrders.class ).findFirst( future );

            return future;
        }
    }

    public static void findFirstAsync( AsyncCallback<MerchantOrders> callback )
    {
        Backendless.Data.of( MerchantOrders.class ).findFirst( callback );
    }

    public static MerchantOrders findLast()
    {
        return Backendless.Data.of( MerchantOrders.class ).findLast();
    }

    public static Future<MerchantOrders> findLastAsync()
    {
        if( Backendless.isAndroid() )
        {
            throw new UnsupportedOperationException( "Using this method is restricted in Android" );
        }
        else
        {
            Future<MerchantOrders> future = new Future<MerchantOrders>();
            Backendless.Data.of( MerchantOrders.class ).findLast( future );

            return future;
        }
    }

    public static void findLastAsync( AsyncCallback<MerchantOrders> callback )
    {
        Backendless.Data.of( MerchantOrders.class ).findLast( callback );
    }

    public static BackendlessCollection<MerchantOrders> find( BackendlessDataQuery query )
    {
        return Backendless.Data.of( MerchantOrders.class ).find( query );
    }

    public static Future<BackendlessCollection<MerchantOrders>> findAsync( BackendlessDataQuery query )
    {
        if( Backendless.isAndroid() )
        {
            throw new UnsupportedOperationException( "Using this method is restricted in Android" );
        }
        else
        {
            Future<BackendlessCollection<MerchantOrders>> future = new Future<BackendlessCollection<MerchantOrders>>();
            Backendless.Data.of( MerchantOrders.class ).find( query, future );

            return future;
        }
    }

    public static void findAsync( BackendlessDataQuery query, AsyncCallback<BackendlessCollection<MerchantOrders>> callback )
    {
        Backendless.Data.of( MerchantOrders.class ).find( query, callback );
    }
}