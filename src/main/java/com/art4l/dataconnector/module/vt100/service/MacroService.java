package com.art4l.dataconnector.module.vt100.service;

import com.art4l.dataconnector.module.vt100.domain.dto.screenxml.Field;
import com.art4l.dataconnector.module.vt100.domain.dto.screenxml.OutputParameter;
import com.art4l.dataconnector.module.vt100.domain.dto.screenxml.Screen;
import com.art4l.dataconnector.module.vt100.domain.exception.ApplicationException;
import com.art4l.dataconnector.module.vt100.domain.vt100.HistoryEntry;
import com.art4l.dataconnector.module.vt100.domain.vt100.Vt100Session;
import com.jagacy.Key;
import com.jagacy.Location;
import com.jagacy.util.JagacyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Service
public class MacroService {
    private static int TIMEOUT = 10000;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final ScreenService screenService;

    @Autowired
    public MacroService(ScreenService screenService) {
        this.screenService = screenService;
    }

    public void waitForPageReady(final Vt100Session session) throws JagacyException, ApplicationException {
        waitForPageReady(session, false);
    }

    public void waitForPageReady(final Vt100Session session, boolean ignoreSamePage) throws JagacyException, ApplicationException {
        int timeout = getTimeout(session);
        long startTime = System.currentTimeMillis();

        while(System.currentTimeMillis() - startTime < timeout){ // stop searching for a screen after xx seconds, page should be ready by then
            try {
                boolean keyboardUnlocked = session.waitForUnlock(timeout);
                checkArgument(keyboardUnlocked, "failed to get unlocked keyboard");

                Optional<Screen> optionalScreen = screenService.findCurrentScreen(session);
                
                
                if(optionalScreen.isPresent()){
                    log.info("Current screen in MacroService: "+optionalScreen.get().getName());
                    // Get fields
                    Field[] fields = optionalScreen.get().getFields();

                    if(fields == null || fields.length == 0){
                        log.info("Detected screen without fields, assuming page is ready right away");
                        return;
                    }

                    // Check if one of the fields is active
                    Optional<Field> optionalField = findActiveFieldOrEmpty(session, optionalScreen.get());
                    if(optionalField.isPresent()){
                        // check if this is not the previous page that is still processing
                        log.info("Active field: " + optionalField.get().getName());
                        if(!session.getHistory().isEmpty() && !ignoreSamePage){
                            
                            HistoryEntry lastEntry = session.getHistory().get(session.getHistory().size()-1);
                            if(optionalScreen.get().getName().equalsIgnoreCase(lastEntry.getDetectedScreen().getName())
                                    && optionalField.get().getName().equalsIgnoreCase(lastEntry.getDetectedField().getName())){
                                log.info("Detected screen and field are the same as last submitted screen and field, waiting for another change ...");
                                if(System.currentTimeMillis() - lastEntry.getTimestamp() > timeout){
                                    log.info("Page remained the same for longer than xx seconds, assuming page submit failed, assuming current page is ready.");
                                    return;
                                }
                            }
                            else {

                                return;
                            }
                        }
                        else {
                            return; // page is visible and there is an active field, thus page must be ready
                        }
                    }
                }
                log.info("No current screen ready");
                session.waitForChange(timeout);

            } catch (JagacyException e) {
                log.info("JagacyException occured: {}", e.getMessage());
                throw e;
            }
        }

        throw new ApplicationException("Could not recognize page");
    }
    public Optional<Field> findActiveField(final Vt100Session session) throws JagacyException, ApplicationException{
    	return findActiveField(session,false);
    	
    }
    

    public Optional<Field> findActiveField(final Vt100Session session, boolean ignoreSamePage) throws JagacyException, ApplicationException{
        waitForPageReady(session,ignoreSamePage);
        Optional<Screen> optionalScreen = screenService.findCurrentScreen(session);
        if(optionalScreen.isPresent()){
            return findActiveField(session, optionalScreen.get());
        }
        return Optional.empty();
    }

    public boolean isActiveScreen(Vt100Session vt100Session, String screenName){
        Optional<Screen> optionalScreen = screenService.findCurrentScreen(vt100Session);
        if(!optionalScreen.isPresent()){
            return false;
        }
        return optionalScreen.get().getName().equalsIgnoreCase(screenName);
    }

    public boolean isActiveField(Vt100Session vt100Session, String fieldName){
        Optional<Screen> optionalScreen = screenService.findCurrentScreen(vt100Session);
        if(!optionalScreen.isPresent()){
            return false;
        }

        Optional<Field> optionalField = findActiveField(vt100Session, optionalScreen.get());
        if(!optionalField.isPresent()){
            return false;
        }
        return optionalField.get().getName().equalsIgnoreCase(fieldName);
    }

    public void skipPagesWithDefaultInput(Vt100Session vt100Session) throws JagacyException, ApplicationException {
        while(skipPageWithDefaultInput(vt100Session));
    }

    public boolean clearField(final Vt100Session session){
        try {
            // wait until keyboard unlocks
            final boolean keyboardUnlocked = session.waitForUnlock(getTimeout(session));
            checkArgument(keyboardUnlocked, "failed to get unlocked keyboard");
            // get the current cursor location
            final Location currentCursorLocation = session.readCursorLocation();
            boolean cursorAtValidLocation = currentCursorLocation.isValid();

            // enter the input
            String clear = "                    ";
            session.writeString(clear);

            for(int i=0; i<clear.length(); i++){
                // wait until keyboard unlocks
                final boolean keyboardUnlockedAgain = session.waitForUnlock(getTimeout(session));
                checkArgument(keyboardUnlockedAgain, "failed to get unlocked keyboard after input");
                session.writeKey(Key.BACKSPACE);
            }

            return true;
        } catch (Exception e){
            log.error("Error submitting input: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean submitInput(final Vt100Session session, final String input, final Key submitKey, final boolean waitForCursor) {
        try {
            // wait until keyboard unlocks
            final boolean keyboardUnlocked = session.waitForUnlock(getTimeout(session));
            checkArgument(keyboardUnlocked, "failed to get unlocked keyboard");
            // get the current cursor location
            final Location currentCursorLocation = session.readCursorLocation();
            boolean cursorAtValidLocation = currentCursorLocation.isValid();

            // Record history before writing to session
            recordHistory(session);

            // enter the input
            session.writeString(input);
            // don't wait for the cursor for a password field
            if (waitForCursor) {
                // check if the cursor arrived at the end of the input
                final boolean cursorArrivedAtInputEnd = session.waitForCursor(currentCursorLocation.getRow(), currentCursorLocation.getColumn() + input.length(), getTimeout(session));
                checkArgument(cursorArrivedAtInputEnd, "cursor did not arrive at end of input length");
            }
            // wait until keyboard unlocks
            final boolean keyboardUnlockedAgain = session.waitForUnlock(getTimeout(session));
            checkArgument(keyboardUnlockedAgain, "failed to get unlocked keyboard after input");
            // Record history again
            //recordHistory(session);

            if(submitKey != null){
                session.writeKey(submitKey);
            }
            if(submitKey != null && !submitKey.equals(Key.TAB)){ // don't wait on change with empty submit key or tab
                // wait for change
                session.waitForChange(getTimeout(session));
            }

            return true;
        } catch (Exception e){
            log.error("Error submitting input: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean executeKeySequence(final Vt100Session session, List<Key> keys) {
        try {
            boolean keyboardUnlocked = session.waitForUnlock(getTimeout(session));

            // Record history before writing to session
            recordHistory(session);

            for (Key key : keys) {
                // wait until keyboard unlocks
                keyboardUnlocked = session.waitForUnlock(getTimeout(session));
                checkArgument(keyboardUnlocked, "failed to get unlocked keyboard");

                // enter the input
                session.writeKey(key);
            }

            // return success
            return true;

        } catch(Exception e) {
            log.error("Error executing key sequence: {}", e.getMessage(), e);
            return false;
        }
    }

    public Optional<Map<String, String>> readOutputParameters(final Vt100Session session) {
        try {
            waitForPageReady(session);
            Optional<Screen> optionalScreen = screenService.findCurrentScreen(session);
            if (!optionalScreen.isPresent()) {
                log.error("Could not recognize current screen");
                return Optional.empty();
            }

            Screen screen = optionalScreen.get();
            if (screen.getOutputParameters() == null) {
                log.warn("Trying to read screen without output parameters");
                return Optional.empty();
            }

            String screenContent = Arrays.stream(session.readScreen()).collect(Collectors.joining("\n"));
            Map<String, String> params = new HashMap<>();
            for (OutputParameter outputParameter : screen.getOutputParameters()) {
                // apply regex to screenContent to get data
                Pattern pattern = Pattern.compile(outputParameter.getRegex());
                Matcher matcher = pattern.matcher(screenContent);

                if(matcher.find(1)){
                    String value = matcher.group(1).trim();
                    params.put(outputParameter.getKey(), value);
                }
                else {
                    log.warn("Could not find matching data for field " + outputParameter.getKey());
                }

            }

            return Optional.of(params);
        } catch (Exception e){
            log.error("Failed to read outputParameters from screen");
        }

        return Optional.empty();
    }


    public void recordHistory(Vt100Session session){
        long startTime = System.currentTimeMillis();
        Optional<Screen> optionalScreen = screenService.findCurrentScreen(session);
        while(!optionalScreen.isPresent() && System.currentTimeMillis() - startTime < getTimeout(session)){
            log.info("Screen not found while trying to record history, waiting for change");
            try {
                session.waitForChange(5000);
                optionalScreen = screenService.findCurrentScreen(session);
            } catch (JagacyException e) {
                e.printStackTrace();
            }
        }


        Optional<Field> optionalField = findActiveField(session, optionalScreen.get());
//        if(!optionalField.isPresent()){
//            log.warn("Field not found while trying to record history!");
//        }
        try {
            HistoryEntry historyEntry = HistoryEntry.builder()
                    .detectedScreen(optionalScreen.get())
                    .detectedField(optionalField.orElse(null))
                    .screen(session.readScreen())
                    .timestamp(System.currentTimeMillis())
                    .build();

            session.getHistory().add(historyEntry);
        } catch (Exception e){
            log.error("Error while trying to record history! {}", e.getMessage(), e);
        }

    }

    // PRIVATE FUNCTIONS

    private Optional<Field> findFieldOnLine(String line, Screen screen){
        if(screen.getFields() == null){
            return Optional.empty();
        }
        return Arrays.stream(screen.getFields())
                .filter(
                        field -> {
                            boolean res = line.toLowerCase().contains(field.getLabel().getValue().toLowerCase()); // check if field label value is in front of cursor
                            //log.info("Comparing field " + field.getLabel().getValue() + " with line " + line + " Match? " + res);
                            return res;
                        }
                ).findFirst();
    }

    private Optional<Field> findActiveField(final Vt100Session session, Screen screen){
        try {
            if(screen.getFields() == null || screen.getFields().length == 0){
                log.info("Screen doesn't have any fields, returning empty");
                return Optional.empty();
            }
            Optional<Field> optionalField = findActiveFieldOrEmpty(session, screen);
            if(optionalField.isPresent()){
                return optionalField;
            }
            else {
                // check if field is a prefix of the current cursor position
                Location cursorLocation = session.readCursorLocation();
                String[] lines = session.readScreen();

                for(int row = cursorLocation.getRow(); row < lines.length; row++){
                    String line = lines[row];
                    Optional<Field> optional = findFieldOnLine(line, screen);
                    if(optional.isPresent()){
                        return optional;
                    }
                }

                for(int row = cursorLocation.getRow(); row >= 0; row--){
                    String line = lines[row];
                    Optional<Field> optional = findFieldOnLine(line, screen);
                    if(optional.isPresent()){
                        return optional;
                    }
                }
            }
            log.warn("Couldn't find any field on this page, guessing by returning the first field in the xml");
            return Optional.of(screen.getFields()[0]);
        } catch (JagacyException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private Optional<Field> findActiveFieldOrEmpty(final Vt100Session session, Screen screen){
        try {
            // check if field is a prefix of the current cursor position
            Location cursorLocation = session.readCursorLocation();
            String[] lines = session.readScreen();

            String line = lines[cursorLocation.getRow()];
//            log.info("Cursor line: " + line);

            if(cursorLocation.isValid()){
                Optional<Field> optional = findFieldOnLine(line, screen);

                int offset = 0;
                //check the previous line, entry field is sometimes xLines above cursor (as defined in Offset)
                if (!optional.isPresent()) {
                    for (offset = 0; offset > -5; offset--) {
                        if (!optional.isPresent() && cursorLocation.getRow() + offset >= 0) {
                            line = lines[cursorLocation.getRow() + offset];
//                            log.info("Couldn't find field on current line, trying the previous line: " + line);
                            optional = findFieldOnLine(line, screen);
                            if (optional.isPresent()) break;
                        }

                    }
                }

                // if we couldn't find it on the current row, check the next row (cursor sometimes has an offset of one row)

                if(!optional.isPresent() && lines.length > cursorLocation.getRow() + 1){
                    line = lines[cursorLocation.getRow() + 1];
//                    log.info("Couldn't find field on current line, trying the next line: " + line);
                    optional = findFieldOnLine(line, screen);
                    offset = 1;
                }

                //check if offset is correct
                if (optional.isPresent()&& (optional.get().getLabel().getCursoroffset() == null)){
                    return optional;
                } else if (optional.isPresent()&& (optional.get().getLabel().getCursoroffset() == offset)) {
                    return optional;
                }

                return Optional.empty();
            }
        } catch (JagacyException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private boolean skipPageWithDefaultInput(Vt100Session vt100Session) throws JagacyException, ApplicationException{
        // wait for page to be ready
        waitForPageReady(vt100Session);

        Optional<Screen> optionalScreen = screenService.findCurrentScreen(vt100Session);
        if(!optionalScreen.isPresent()){
            log.warn("No screen found, this was unexpected");
            return false;
        }

        Screen screen = optionalScreen.get();

        Optional<Field> optionalField = findActiveField(vt100Session, screen);
        if(!optionalField.isPresent()){
            log.warn("No active field found, this was unexpected");
            return false;
        }

        Field field = optionalField.get();
        if(field.getInput() != null && field.getInput().getValue() != null){
            log.info("Found field with default input, applying input");
            Key navigationKey = Key.ENTER;
            if(field.getNavigationKeys() != null && field.getNavigationKeys().containsKey("submit")){
                navigationKey = Key.find(field.getNavigationKeys().get("submit"));
            }
            submitInput(vt100Session, field.getInput().getValue(), navigationKey, false);
            waitForPageReady(vt100Session);
            return true;
        }
        return false;
    }

    private int getTimeout(Vt100Session session){
        if(session.getHistory() != null && !session.getHistory().isEmpty()){
            Screen screen = session.getHistory().get(session.getHistory().size()-1).getDetectedScreen();
            if(screen.getTimeout() != null){
                return screen.getTimeout();
            }
        }
        return TIMEOUT;
    }


}
