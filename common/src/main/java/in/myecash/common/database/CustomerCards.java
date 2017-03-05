package in.myecash.common.database;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.persistence.BackendlessDataQuery;

public class CustomerCards
{
  private Integer status;
  private java.util.Date created;
  private java.util.Date updated;
  private String ccntId;
  private String agentId;
  private String mchntId;
  private String custId;
  private java.util.Date status_update_time;
  private String status_reason;
  private String objectId;
  private String card_id;
  private String cardNum;
  private String barcode;

  public String getBarCode() {
    return barcode;
  }

  public void setBarCode(String barcode) {
    this.barcode = barcode;
  }

  public String getCardNum() {
    return cardNum;
  }

  public void setCardNum(String cardNum) {
    this.cardNum = cardNum;
  }

  public Integer getStatus()
  {
    return status;
  }

  public void setStatus( Integer status )
  {
    this.status = status;
  }

  public java.util.Date getCreated()
  {
    return created;
  }

  public java.util.Date getUpdated()
  {
    return updated;
  }

  public java.util.Date getStatus_update_time()
  {
    return status_update_time;
  }

  public void setStatus_update_time( java.util.Date status_update_time )
  {
    this.status_update_time = status_update_time;
  }

  public String getStatus_reason()
  {
    return status_reason;
  }

  public void setStatus_reason( String status_reason )
  {
    this.status_reason = status_reason;
  }

  public String getObjectId()
  {
    return objectId;
  }

  public String getCard_id()
  {
    return card_id;
  }

  public void setCard_id( String card_id )
  {
    this.card_id = card_id;
  }

  public String getCcntId() {
    return ccntId;
  }

  public void setCcntId(String ccntId) {
    this.ccntId = ccntId;
  }

  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  public String getMchntId() {
    return mchntId;
  }

  public void setMchntId(String mchntId) {
    this.mchntId = mchntId;
  }

  public String getCustId() {
    return custId;
  }

  public void setCustId(String custId) {
    this.custId = custId;
  }

  public CustomerCards save()
  {
    return Backendless.Data.of( CustomerCards.class ).save( this );
  }

  public Future<CustomerCards> saveAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<CustomerCards> future = new Future<CustomerCards>();
      Backendless.Data.of( CustomerCards.class ).save( this, future );

      return future;
    }
  }

  public void saveAsync( AsyncCallback<CustomerCards> callback )
  {
    Backendless.Data.of( CustomerCards.class ).save( this, callback );
  }

  public Long remove()
  {
    return Backendless.Data.of( CustomerCards.class ).remove( this );
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
      Backendless.Data.of( CustomerCards.class ).remove( this, future );

      return future;
    }
  }

  public void removeAsync( AsyncCallback<Long> callback )
  {
    Backendless.Data.of( CustomerCards.class ).remove( this, callback );
  }

  public static CustomerCards findById( String id )
  {
    return Backendless.Data.of( CustomerCards.class ).findById( id );
  }

  public static Future<CustomerCards> findByIdAsync( String id )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<CustomerCards> future = new Future<CustomerCards>();
      Backendless.Data.of( CustomerCards.class ).findById( id, future );

      return future;
    }
  }

  public static void findByIdAsync( String id, AsyncCallback<CustomerCards> callback )
  {
    Backendless.Data.of( CustomerCards.class ).findById( id, callback );
  }

  public static CustomerCards findFirst()
  {
    return Backendless.Data.of( CustomerCards.class ).findFirst();
  }

  public static Future<CustomerCards> findFirstAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<CustomerCards> future = new Future<CustomerCards>();
      Backendless.Data.of( CustomerCards.class ).findFirst( future );

      return future;
    }
  }

  public static void findFirstAsync( AsyncCallback<CustomerCards> callback )
  {
    Backendless.Data.of( CustomerCards.class ).findFirst( callback );
  }

  public static CustomerCards findLast()
  {
    return Backendless.Data.of( CustomerCards.class ).findLast();
  }

  public static Future<CustomerCards> findLastAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<CustomerCards> future = new Future<CustomerCards>();
      Backendless.Data.of( CustomerCards.class ).findLast( future );

      return future;
    }
  }

  public static void findLastAsync( AsyncCallback<CustomerCards> callback )
  {
    Backendless.Data.of( CustomerCards.class ).findLast( callback );
  }

  public static BackendlessCollection<CustomerCards> find( BackendlessDataQuery query )
  {
    return Backendless.Data.of( CustomerCards.class ).find( query );
  }

  public static Future<BackendlessCollection<CustomerCards>> findAsync( BackendlessDataQuery query )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BackendlessCollection<CustomerCards>> future = new Future<BackendlessCollection<CustomerCards>>();
      Backendless.Data.of( CustomerCards.class ).find( query, future );

      return future;
    }
  }

  public static void findAsync( BackendlessDataQuery query, AsyncCallback<BackendlessCollection<CustomerCards>> callback )
  {
    Backendless.Data.of( CustomerCards.class ).find( query, callback );
  }
}