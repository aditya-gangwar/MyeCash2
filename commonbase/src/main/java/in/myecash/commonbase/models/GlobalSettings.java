package in.myecash.commonbase.models;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.persistence.BackendlessDataQuery;

public class GlobalSettings
{
  private Integer value_datatype;
  private Integer value_int;
  private String description;
  private java.util.Date updated;
  private String name;
  private java.util.Date value_date;
  private String display_name;
  private Boolean user_visible;
  private String value_string;
  private java.util.Date created;
  private String objectId;
  private String ownerId;
  public Integer getValue_datatype()
  {
    return value_datatype;
  }

  public void setValue_datatype( Integer value_datatype )
  {
    this.value_datatype = value_datatype;
  }

  public Integer getValue_int()
  {
    return value_int;
  }

  public void setValue_int( Integer value_int )
  {
    this.value_int = value_int;
  }

  public String getDescription()
  {
    return description;
  }

  public void setDescription( String description )
  {
    this.description = description;
  }

  public java.util.Date getUpdated()
  {
    return updated;
  }

  public String getName()
  {
    return name;
  }

  public void setName( String name )
  {
    this.name = name;
  }

  public java.util.Date getValue_date()
  {
    return value_date;
  }

  public void setValue_date( java.util.Date value_date )
  {
    this.value_date = value_date;
  }

  public String getDisplay_name()
  {
    return display_name;
  }

  public void setDisplay_name( String display_name )
  {
    this.display_name = display_name;
  }

  public Boolean getUser_visible()
  {
    return user_visible;
  }

  public void setUser_visible( Boolean user_visible )
  {
    this.user_visible = user_visible;
  }

  public String getValue_string()
  {
    return value_string;
  }

  public void setValue_string( String value_string )
  {
    this.value_string = value_string;
  }

  public java.util.Date getCreated()
  {
    return created;
  }

  public String getObjectId()
  {
    return objectId;
  }

  public String getOwnerId()
  {
    return ownerId;
  }

                                                    
  public GlobalSettings save()
  {
    return Backendless.Data.of( GlobalSettings.class ).save( this );
  }

  public Future<GlobalSettings> saveAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<GlobalSettings> future = new Future<GlobalSettings>();
      Backendless.Data.of( GlobalSettings.class ).save( this, future );

      return future;
    }
  }

  public void saveAsync( AsyncCallback<GlobalSettings> callback )
  {
    Backendless.Data.of( GlobalSettings.class ).save( this, callback );
  }

  public Long remove()
  {
    return Backendless.Data.of( GlobalSettings.class ).remove( this );
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
      Backendless.Data.of( GlobalSettings.class ).remove( this, future );

      return future;
    }
  }

  public void removeAsync( AsyncCallback<Long> callback )
  {
    Backendless.Data.of( GlobalSettings.class ).remove( this, callback );
  }

  public static GlobalSettings findById( String id )
  {
    return Backendless.Data.of( GlobalSettings.class ).findById( id );
  }

  public static Future<GlobalSettings> findByIdAsync( String id )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<GlobalSettings> future = new Future<GlobalSettings>();
      Backendless.Data.of( GlobalSettings.class ).findById( id, future );

      return future;
    }
  }

  public static void findByIdAsync( String id, AsyncCallback<GlobalSettings> callback )
  {
    Backendless.Data.of( GlobalSettings.class ).findById( id, callback );
  }

  public static GlobalSettings findFirst()
  {
    return Backendless.Data.of( GlobalSettings.class ).findFirst();
  }

  public static Future<GlobalSettings> findFirstAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<GlobalSettings> future = new Future<GlobalSettings>();
      Backendless.Data.of( GlobalSettings.class ).findFirst( future );

      return future;
    }
  }

  public static void findFirstAsync( AsyncCallback<GlobalSettings> callback )
  {
    Backendless.Data.of( GlobalSettings.class ).findFirst( callback );
  }

  public static GlobalSettings findLast()
  {
    return Backendless.Data.of( GlobalSettings.class ).findLast();
  }

  public static Future<GlobalSettings> findLastAsync()
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<GlobalSettings> future = new Future<GlobalSettings>();
      Backendless.Data.of( GlobalSettings.class ).findLast( future );

      return future;
    }
  }

  public static void findLastAsync( AsyncCallback<GlobalSettings> callback )
  {
    Backendless.Data.of( GlobalSettings.class ).findLast( callback );
  }

  public static BackendlessCollection<GlobalSettings> find( BackendlessDataQuery query )
  {
    return Backendless.Data.of( GlobalSettings.class ).find( query );
  }

  public static Future<BackendlessCollection<GlobalSettings>> findAsync( BackendlessDataQuery query )
  {
    if( Backendless.isAndroid() )
    {
      throw new UnsupportedOperationException( "Using this method is restricted in Android" );
    }
    else
    {
      Future<BackendlessCollection<GlobalSettings>> future = new Future<BackendlessCollection<GlobalSettings>>();
      Backendless.Data.of( GlobalSettings.class ).find( query, future );

      return future;
    }
  }

  public static void findAsync( BackendlessDataQuery query, AsyncCallback<BackendlessCollection<GlobalSettings>> callback )
  {
    Backendless.Data.of( GlobalSettings.class ).find( query, callback );
  }
}