package com.art4l.martur.db;

import com.art4l.dataconnector.container.configuration.DbConfig;
import com.art4l.dataconnector.container.configuration.DbSettings;
import com.art4l.dataconnector.module.db.DbSession;
import com.art4l.dataconnector.module.db.domain.*;
import com.art4l.dataconnector.module.vt100.domain.exception.ApplicationException;
import com.art4l.dataconnector.module.vt100.domain.exception.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import org.joda.time.DateTime;


import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

@Service
public class MarturDbConnector {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final String LOCATION = "martur_ro";

    private final DbConfig dbConfig;
    private Map<String, NamedParameterJdbcTemplate> sessions;

    @Autowired
    public MarturDbConnector( DbConfig dbConfig) throws IOException {
        this.sessions = new HashMap<>();
        this.dbConfig = dbConfig;
    }

    @PreDestroy
    public void cleanup(){
        log.info("Cleaning Db sessions Martur");
        List<String> keys = new ArrayList<>();
        keys.addAll(sessions.keySet());
        keys.parallelStream()
                .forEach(key -> {
                    log.info("Cleanup up session " + key);
                    try {
                        exitFlow(key);
                    } catch (Exception e){
                        log.info("Failed to exit flow for session " + key);
                    }
                });
        sessions.clear();

    }

    public void dbLogin(String processInstanceId) throws ApplicationException, UserException {
        try {

            // open a session
            DbSettings settings = new DbSettings();
            settings.setUrl(dbConfig.getSettings().get(LOCATION).getUrl());
            settings.setUsername(dbConfig.getSettings().get(LOCATION).getUsername());
            settings.setPassword(dbConfig.getSettings().get(LOCATION).getPassword());



            DataSource dataSource = DbSession.MarturDataSource(settings);
            // store session for later use
            NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
            sessions.put(processInstanceId, jdbcTemplate);


        } catch (Exception e){
            log.error("Login failed: " + e.getMessage());
            e.printStackTrace();
            throw new ApplicationException("Login failed");
        }
    }

    public void exitFlow(String processInstanceId) throws ApplicationException, UserException {
        try {

            NamedParameterJdbcTemplate jdbcTemplate= sessions.get(processInstanceId);
            sessions.remove(processInstanceId);
        } catch (Exception e){
            log.error("Login failed: " + e.getMessage());
            e.printStackTrace();
            throw new ApplicationException("Login failed");
        }
    }

    public Optional<KittingLayout> getKittingArea(String processInstanceId, String kittingArea) throws ApplicationException, UserException {
        try {

            NamedParameterJdbcTemplate jdbcTemplate= sessions.get(processInstanceId);

            if(jdbcTemplate == null){
                log.error("Could not find active DataSource for processInstanceId " + processInstanceId);
                throw new ApplicationException("Could not find active session for processInstanceId " + processInstanceId);
            }

            SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("layoutName", kittingArea);


            List<KittingLayout> kittingLayouts = jdbcTemplate.query("SELECT * FROM Martur.KITTINGLAYOUT WHERE LAYOUTNAME = :layoutName", namedParameters,new KittingLayoutRowMapper());
            if (kittingLayouts.size() != 1) return Optional.empty();
            return Optional.of(kittingLayouts.get(0));

        } catch (Exception e){
            log.error("Login failed: " + e.getMessage());
            e.printStackTrace();
            throw new ApplicationException("Login failed");
        }
    }
    /*
    Get the order based on scanned Orderlabel
     */

    public Optional<Order> getOrder(String processInstanceId, String scannedOrder) throws ApplicationException,UserException{
        try {

            NamedParameterJdbcTemplate jdbcTemplate= sessions.get(processInstanceId);

            if(jdbcTemplate == null){
                log.error("Could not find active DataSource for processInstanceId " + processInstanceId);
                throw new ApplicationException("Could not find active session for processInstanceId " + processInstanceId);
            }

            SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("orderLabel", scannedOrder);

            List<Order> orders = jdbcTemplate.query("SELECT * FROM Martur.ORDER WHERE ORDERLABEL = :orderLabel", namedParameters,new OrderRowMapper());
            if (orders.size() != 1) return Optional.empty();

            return Optional.of(orders.get(0));

        } catch (Exception e){
            log.error("getOrder Failed: " + e.getMessage());
            e.printStackTrace();
            throw new ApplicationException("getOrder failed");
        }

    }

    /*
    Get the order based on next sequenceNumber
    If no more orders, return null ;
     */

    public Optional<Order> getOrder(String processInstanceId, int seqNumber) throws ApplicationException,UserException{
        try {

            NamedParameterJdbcTemplate jdbcTemplate= sessions.get(processInstanceId);

            if(jdbcTemplate == null){
                log.error("Could not find active DataSource for processInstanceId " + processInstanceId);
                throw new ApplicationException("Could not find active session for processInstanceId " + processInstanceId);
            }

            SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("seqNumber", seqNumber);

            List<Order> orders = jdbcTemplate.query("SELECT * FROM Martur.ORDER WHERE SEQNUMBER = :seqNumber", namedParameters,new OrderRowMapper());
            if (orders.size() != 1) return Optional.empty();
            return Optional.of(orders.get(0));

        } catch (Exception e){
            log.error("getOrder failed: " + e.getMessage());
            e.printStackTrace();
            throw new ApplicationException("getOrder failed");
        }

    }

    /**
     * Get the product data (with components of a given product
     *
     * @param processInstanceId
     * @param productId
     * @return
     * @throws ApplicationException
     */

    public Optional<Product> getProduct(String processInstanceId, int productId) throws ApplicationException{
        try {

            NamedParameterJdbcTemplate jdbcTemplate= sessions.get(processInstanceId);

            if(jdbcTemplate == null){
                log.error("Could not find active DataSource for processInstanceId " + processInstanceId);
                throw new ApplicationException("Could not find active session for processInstanceId " + processInstanceId);
            }

            SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("productId", productId);

            List<Product> products = jdbcTemplate.query("SELECT * FROM Martur.CARKITPRODUCT WHERE ID = :productId", namedParameters,new ProductRowMapper());
            if (products.size() != 1) return Optional.empty();

            return Optional.of(products.get(0));

        } catch (Exception e){
            log.error("getComponents failed: " + e.getMessage());
            e.printStackTrace();
            throw new ApplicationException("getComponents failed");
        }


    }

    /**
     * Get all the components with AR code for the given product and the kitting area
     *
     * @param processInstanceId
     * @param product
     * @param kittingId
     * @return
     * @throws ApplicationException
     * @throws UserException
     */


    public List<Component> getComponents(String processInstanceId, Product product, int kittingId) throws ApplicationException, UserException {
        try {

            final List<Component> components = product.getComponents();
            NamedParameterJdbcTemplate jdbcTemplate= sessions.get(processInstanceId);

            if(jdbcTemplate == null){
                log.error("Could not find active DataSource for processInstanceId " + processInstanceId);
                throw new ApplicationException("Could not find active session for processInstanceId " + processInstanceId);
            }

            for (Component component:components){
                if (component.getId() > 0){
                    //get the component name
                    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
                    mapSqlParameterSource.addValue("componentId",component.getId());
                    List<Component> namedcomponents = jdbcTemplate.query("SELECT * FROM Martur.CARKITCOMPONENT WHERE ID = :componentId", mapSqlParameterSource,new ComponentRowMapper());
                    if (namedcomponents.size() == 1) {
                        component.setComponentName(namedcomponents.get(0).getComponentName());
                        component.setBarcodeId(namedcomponents.get(0).getBarcodeId());
                    }

                    //get the arposition
                    mapSqlParameterSource.addValue("layoutId",kittingId);
                    List<ComponentPosition> componentPositions = jdbcTemplate.query("SELECT * FROM Martur.COMPONENTPOSITION WHERE " +
                            "COMPONENTID = :componentId && " +
                            "LAYOUTID = :layoutId", mapSqlParameterSource,new ComponentPositionRowMapper());
                    if (componentPositions.size() == 1) {
                        component.setArPosition(componentPositions.get(0).getArPosition());
                    }
                }

            }

            //now copy over only the valid components (with AR position > 0)
            final List<Component> validComponents = new ArrayList<>();
            for (Component component:components){
                if (component.getArPosition()>0) validComponents.add(component);
            }

            return validComponents;

        } catch (Exception e){
            log.error("getComponents failed: " + e.getMessage());
            e.printStackTrace();
            throw new ApplicationException("getComponents failed");
        }
    }

    public void writeLogs(String processInstanceId,ArrayList<LogData> logDataArrayList) throws ApplicationException, UserException{

        NamedParameterJdbcTemplate jdbcTemplate= sessions.get(processInstanceId);

        if(jdbcTemplate == null){
            log.error("Could not find active DataSource for processInstanceId " + processInstanceId);
            throw new ApplicationException("Could not find active session for processInstanceId " + processInstanceId);
        }

        String sql  = "INSERT INTO LOGDATA (BUSINESSTASK,EVENTACTION,LOCATION,USER,PAYLOAD,VALUE1,VALUE2,TIMESTAMP) " +
                "VALUES(:businesstask,:eventaction,:location,:username,:payload,:value1,:value2,:timestampname)";

        List<Map<String,Object>> batchValues = new ArrayList<>(logDataArrayList.size());
        for (LogData logData: logDataArrayList){
            DateTime dateTime = new DateTime(logData.getTimestamp());

            batchValues.add(new MapSqlParameterSource("businesstask",logData.getBusinessTask())
                    .addValue("eventaction",logData.getEventAction())
                    .addValue("location",logData.getLocation())
                    .addValue("username",logData.getUser())
                    .addValue("payload",logData.getPayload())
                    .addValue("value1",logData.getValue1())
                    .addValue("value2",logData.getValue2())
                    .addValue("timestampname",new Timestamp(dateTime.getMillis()))
                    .getValues());
        }

        int[] updateCounts = jdbcTemplate.batchUpdate(sql,batchValues.toArray(new Map[logDataArrayList.size()]));

    }

}
