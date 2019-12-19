package com.art4l.dataconnector.module.vt100.domain.vt100;

import com.jagacy.SessionVt;
import com.jagacy.util.JagacyException;
import com.jagacy.util.JagacyProperties;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class Vt100Session extends SessionVt {
    private String processInstanceId;
    private String locationName;
    private List<HistoryEntry> history;

    /**
     * Start a ScreenScraper session. This will open a VT100 or Telnet session
     * The credentials are stored in the application.yml file
     * @param name
     * @param host
     * @param locationName
     * @param processInstanceId
     * @throws JagacyException
     */

    public Vt100Session(String name, String host, String locationName, String processInstanceId) throws JagacyException {
        super(name, host);
        JagacyProperties jagacyProperties = this.getProperties();
        jagacyProperties.set("window", "false");
        this.locationName = locationName;
        this.processInstanceId = processInstanceId;
        this.history = new ArrayList<>();
    }

    /**
     * Start a ScreeScraper session.
     * This will open a VT100 or Telnet session
     * The credentials and the properties are stored in the application.yml file
     *
     * @param name
     * @param host
     * @param locationName
     * @param processInstanceId
     * @param properties
     * @throws JagacyException
     */
    public Vt100Session(String name, String host, String locationName, String processInstanceId, Map<String, String> properties) throws JagacyException {
        this(name, host, locationName, processInstanceId);
        JagacyProperties jagacyProperties = this.getProperties();
        for(Map.Entry<String, String> entry: properties.entrySet()){
            jagacyProperties.set(entry.getKey(), entry.getValue());
        }
    }

    @Override
    protected boolean logon() throws JagacyException {
        waitForCursor(createLocation(7, 7), 5000);
        return true;
    }

    @Override
    protected void logoff() throws JagacyException {
        super.logoff();
    }
}