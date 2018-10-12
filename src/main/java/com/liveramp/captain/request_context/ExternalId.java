package com.liveramp.captain.request_context;

/***
 *  The ExternalId exists to help clients associate their requests with each other, and outside objects.
 */
public class ExternalId {
  private String idType;
  private String id;

  /***
   * @param idType An identifier for the external id. (e.g. "data_sync_job")
   * @param id  An identifier for an instance from the external application. (e.g. id of DataSyncJob)
   */
  public ExternalId(String idType, String id) {
    this.idType = idType;
    this.id = id;
  }

  public String getIdType() {
    return idType;
  }

  public String getId() {
    return id;
  }

  public void setIdType(String idType) {
    this.idType = idType;
  }

  public void setId(String id) {
    this.id = id;
  }
}
