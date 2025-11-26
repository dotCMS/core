/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

/**
 * 
 */
package com.dotcms.enterprise.cluster.action.business;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.dotcms.enterprise.cluster.action.model.ServerActionBean;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;

/**
 * @author Oscar Arrieta
 *
 */
public class ServerActionFactoryImpl implements ServerActionFactory {

  public ServerActionFactoryImpl() {
    this(new ServerActionBeanCacheImpl());

  }

  public ServerActionFactoryImpl(ServerActionBeanCache cache) {
    this.cache = cache;

  }

  final ServerActionBeanCache cache;

  /**
   * @param map
   * @return ServerActionBean object with all data from DB row.
   */
  private ServerActionBean loadServerActionBean(Map<String, Object> map) {

    ServerActionBean returnedServerActionBean = null;

    try {
      if (map != null) {
        returnedServerActionBean = new ServerActionBean();
        returnedServerActionBean.setId((String) map.get("server_action_id"));
        returnedServerActionBean.setOriginatorId((String) map.get("originator_id"));
        returnedServerActionBean.setServerId((String) map.get("server_id"));

        // Could be null.
        if (map.containsKey("failed") && UtilMethods.isSet(map.get("failed"))) {
          if (DbConnectionFactory.isOracle()) {
            if (((BigDecimal) map.get("failed")).intValue() == 1) {
              returnedServerActionBean.setFailed(true);
            } else {
              returnedServerActionBean.setFailed(false);
            }
          } else {
            returnedServerActionBean.setFailed((boolean) map.get("failed"));
          }

        }

        // Could be null.
        if (map.containsKey("response") && UtilMethods.isSet((String) map.get("response"))) {
          String json = (String) map.get("response");
          returnedServerActionBean.setResponse(new JSONObject(json));
        }
        returnedServerActionBean.setServerActionId((String) map.get("action_id"));

        // Could be null.
        if (map.containsKey("completed") && UtilMethods.isSet(map.get("completed"))) {
          if (DbConnectionFactory.isOracle()) {
            if (((BigDecimal) map.get("completed")).intValue() == 1) {
              returnedServerActionBean.setCompleted(true);
            } else {
              returnedServerActionBean.setCompleted(false);
            }
          } else {
            returnedServerActionBean.setCompleted((boolean) map.get("completed"));
          }
        }
        returnedServerActionBean.setEnteredDate((Date) map.get("entered_date"));

        if (DbConnectionFactory.isOracle()) {
          returnedServerActionBean.setTimeOutSeconds(((BigDecimal) map.get("time_out_seconds")).longValue());
        } else {
          returnedServerActionBean.setTimeOutSeconds((Long) map.get("time_out_seconds"));
        }

      }

    } catch (Exception e) {
      Logger.error(ServerActionFactoryImpl.class, "Error creating ServerActionBean object: " + e.getMessage());
    }

    return returnedServerActionBean;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dotcms.enterprise.cluster.action.business.ServerActionFactory#findById(java.lang.String)
   */
  @Override
  public ServerActionBean findById(final String id) throws DotDataException {
    if (id == null)
      return null;
    List<ServerActionBean> listBeans = getAllServerActions();
    return listBeans.stream().filter(sb -> sb.getId().equals(id)).findFirst().orElse(null);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dotcms.enterprise.cluster.action.business.ServerActionFactory#existsID(java.lang.String)
   */
  @Override
  public boolean existsID(final String id) throws DotDataException {

    if (id == null)
      return false;
    List<ServerActionBean> listBeans = getAllServerActions();
    return listBeans.stream().anyMatch(sb -> sb.getId().equals(id));
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.dotcms.enterprise.cluster.action.business.ServerActionFactory#save(com.dotcms.enterprise.
   * cluster.action.model.ServerActionBean)
   */
  @Override
  public ServerActionBean save(final ServerActionBean serverActionBean) throws DotDataException {
    ServerActionBean returnedServerActionBean = null;

    if (serverActionBean.getId() == null || !existsID(serverActionBean.getId())) {
      serverActionBean.setId(UUID.randomUUID().toString());

      String MySQLInsert = "INSERT INTO cluster_server_action(" + "server_action_id," + "originator_id," + "server_id," + "failed,"
          + "response," + "action_id," + "completed," + "entered_date," + "time_out_seconds)" + "VALUES(?,?,?,?,?,?,?,?,?)";

      DotConnect dotConnect = new DotConnect();
      dotConnect.setSQL(MySQLInsert);
      dotConnect.addParam(serverActionBean.getId());
      dotConnect.addParam(serverActionBean.getOriginatorId());
      dotConnect.addParam(serverActionBean.getServerId());
      dotConnect.addParam(serverActionBean.isFailed());
      dotConnect.addParam(serverActionBean.getResponse() != null ? serverActionBean.getResponse().toString() : "");
      dotConnect.addParam(serverActionBean.getServerActionId());
      dotConnect.addParam(serverActionBean.isCompleted());
      dotConnect.addParam(serverActionBean.getEnteredDate());
      dotConnect.addParam(serverActionBean.getTimeOutSeconds());
      dotConnect.getResult();

      cache.clearServerActions();
      returnedServerActionBean = findById(serverActionBean.getId());
    } else {
      returnedServerActionBean = update(serverActionBean);
    }
    cache.clearServerActions();
    return returnedServerActionBean;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.dotcms.enterprise.cluster.action.business.ServerActionFactory#update(com.dotcms.enterprise.
   * cluster.action.model.ServerActionBean)
   */
  @Override
  public ServerActionBean update(final ServerActionBean serverActionBean) throws DotDataException {

    ServerActionBean returnedServerActionBean = null;

    if (serverActionBean.getId() != null && existsID(serverActionBean.getId())) {

      String MySQLUpdate =
          "UPDATE cluster_server_action " + "SET originator_id = ?," + " server_id = ?," + " failed = ?," + " response = ?,"
              + " action_id = ?," + " completed = ?," + " entered_date = ?," + " time_out_seconds = ? " + "WHERE server_action_id = ?";

      DotConnect dotConnect = new DotConnect();
      dotConnect.setSQL(MySQLUpdate);
      dotConnect.addParam(serverActionBean.getOriginatorId());
      dotConnect.addParam(serverActionBean.getServerId());
      dotConnect.addParam(serverActionBean.isFailed());
      dotConnect.addParam(serverActionBean.getResponse() != null ? serverActionBean.getResponse().toString() : "");
      dotConnect.addParam(serverActionBean.getServerActionId());
      dotConnect.addParam(serverActionBean.isCompleted());
      dotConnect.addParam(serverActionBean.getEnteredDate());
      dotConnect.addParam(serverActionBean.getTimeOutSeconds());
      dotConnect.addParam(serverActionBean.getId());

      dotConnect.getResult();

      cache.clearServerActions();
      returnedServerActionBean = findById(serverActionBean.getId());

    } else {
      returnedServerActionBean = save(serverActionBean);
    }

    return returnedServerActionBean;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.dotcms.enterprise.cluster.action.business.ServerActionFactory#delete(com.dotcms.enterprise.
   * cluster.action.model.ServerActionBean)
   */
  @Override
  public void delete(final ServerActionBean serverActionBean) throws DotDataException {

    final String MySQLDelete = "DELETE FROM cluster_server_action WHERE server_action_id = ?";
    DotConnect dotConnect = new DotConnect();
    dotConnect.setSQL(MySQLDelete);
    dotConnect.addParam(serverActionBean.getId());
    dotConnect.loadResult();
    cache.clearServerActions();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dotcms.enterprise.cluster.action.business.ServerActionFactory#getAllServerActions()
   */
  @Override
  public List<ServerActionBean> getAllServerActions() throws DotDataException {

    List<ServerActionBean> listBeans = cache.getServerActions();
    if (listBeans == null) {
      synchronized (ServerActionFactoryImpl.class) {
        listBeans = cache.getServerActions();

        if (listBeans == null) {

          List<ServerActionBean> newBeans= new ArrayList<>();

          final String MySQLSelect = "SELECT * FROM cluster_server_action";

          List<Map<String, Object>> results = new ArrayList<>();

          final DotConnect dotConnect = new DotConnect();
          dotConnect.setSQL(MySQLSelect);

          results = dotConnect.loadObjectResults();

          for (Map<String, Object> resut : results) {
            newBeans.add(loadServerActionBean(resut));
          }
          cache.putServerActions(newBeans);
          listBeans=newBeans;
        }
      }
    }
    return (listBeans==null) ? new ArrayList<>() : listBeans;
  }

}
