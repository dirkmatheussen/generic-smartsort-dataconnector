package com.art4l.dataconnector;

import com.art4l.dataconnector.container.DataConnectorApplication;
import com.art4l.dataconnector.module.db.domain.*;
import com.art4l.dataconnector.module.vt100.service.util.ExecutorWithTimeout;
import com.art4l.martur.db.MarturDbConnector;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = DataConnectorApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MarturDbConnectorTest {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public MarturDbConnector dbConnector;


    @Test
    public void shouldArComponents() throws Exception {

        ExecutorWithTimeout.execute(new ExecutorWithTimeout.Execution() {
            @Override
            public Object execute() throws Exception {

                //start process for a user
               dbConnector.dbLogin("procesInstanceId");
               //check scanned layout
               Optional<KittingLayout> kittingLayout = dbConnector.getKittingArea("procesInstanceId","Layout 1");

               //get order by scanned name or by sequence
               Optional<Order> order = dbConnector.getOrder("procesInstanceId","LABEL1");
//             Product product = dbConnector.getProduct("procesInstanceId",order.getProductId() );
               Optional<Product> product = dbConnector.getProduct("procesInstanceId",order.get().getProductId() );
               List<Component> components = dbConnector.getComponents("procesInstanceId",product.get(),kittingLayout.get().getId());
               //update the product record with all full components
               product.get().setComponents(components);
               return true;


            }
        }, false, false, 120000);

//        vt100Connector.exitFlow("procesInstanceId");
    }


    @Test
    public void shouldLogger() throws Exception {

        ExecutorWithTimeout.execute(new ExecutorWithTimeout.Execution() {
            @Override
            public Object execute() throws Exception {

                //start process for a user
                dbConnector.dbLogin("procesInstanceId");

                //create new logData entries
                ArrayList<LogData> logDataArrayList = new ArrayList<>();
                LogData logData = new LogData();
                DateTime dateTime = DateTime.now();
                String timestamp = dateTime.toString();

                logData.setTimestamp(timestamp);
                logData.setBusinessTask("businesstask1");
                logData.setPayload("payload");
                logData.setUser("userOne");

                logDataArrayList.add(logData);


                logData = new LogData();
                dateTime = DateTime.now();
                timestamp = dateTime.toString();

                logData.setTimestamp(timestamp);
                logData.setBusinessTask("businesstask2");
                logData.setPayload("payload2");
                logData.setUser("userTwo");
                logDataArrayList.add(logData);

                logData = new LogData();
                dateTime = DateTime.now();
                timestamp = dateTime.toString();

                logData.setTimestamp(timestamp);
                logData.setBusinessTask("businesstask3");
                logData.setPayload("payload3");
                logData.setUser("userThree");
                logData.setLocation("location3");
                logDataArrayList.add(logData);


                //check: I love you
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            log.info("Write to log starts");
                            dbConnector.writeLogs("procesInstanceId", logDataArrayList);
                            log.info("Write to log ended");

                        } catch (Exception e){
                            //swallow the exception
                        }
                    }
                }).start();

                Thread.sleep(5000);

                return true;


            }
        }, false, false, 120000);

//        vt100Connector.exitFlow("procesInstanceId");
    }



}
