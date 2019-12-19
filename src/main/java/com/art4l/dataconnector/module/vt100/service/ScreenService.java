package com.art4l.dataconnector.module.vt100.service;

import com.art4l.dataconnector.container.configuration.ScreenConfig;
import com.art4l.dataconnector.module.vt100.domain.dto.screenxml.Document;
import com.art4l.dataconnector.module.vt100.domain.dto.screenxml.Field;
import com.art4l.dataconnector.module.vt100.domain.dto.screenxml.Screen;
import com.art4l.dataconnector.module.vt100.domain.dto.screenxml.ScreenIdentifier;
import com.art4l.dataconnector.module.vt100.domain.vt100.Vt100Session;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.jagacy.util.JagacyException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// why is this in a separate service? it's so intertwined with behaviour from the macroservice they might as well be merged?
@Service
public class ScreenService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final int TIMEOUT = 20000;

    private static final int SCREEN_DETECTION_MARGIN_ROWS = 2;
    private static final int SCREEN_DETECTION_MARGIN_COLUMNS = 2;

    private static final int FIELD_DETECTION_START_MARGIN_DEFAULT = 2;
    private static final int FIELD_DETECTION_END_MARGIN_DEFAULT = 2;

    private final Map<String, Document> screenDefinitions;

    private final ScreenConfig screenConfig;

    @Autowired
    public ScreenService(ScreenConfig screenConfig) throws IOException {
        this.screenConfig = screenConfig;

        // Read screens from xml files
        ObjectMapper objectMapper = new XmlMapper();

        File folder = new File(screenConfig.getFolder());
        screenDefinitions = Arrays.stream(folder.listFiles()).collect(
                Collectors.toMap(
                        file -> file.getName().substring(0, file.getName().lastIndexOf(".")),
                        file -> {
                            try {
                                return objectMapper.readValue(
                                        StringUtils.toEncodedString(
                                                Files.readAllBytes(
                                                        file.toPath()
                                                ),
                                                StandardCharsets.UTF_8
                                        ),
                                        Document.class);
                            } catch (IOException e) {
                                e.printStackTrace();
                                return null;
                            }
                        }

                ));

        log.info("Loaded " + screenDefinitions.size() + " screenDefinitions");

        /*this.screenDefinitions = objectMapper.readValue(
                StringUtils.toEncodedString(
                        Files.readAllBytes(
                                new File("src/main/resources/screens/scn.xml").toPath()
                        ),
                        StandardCharsets.UTF_8
                ),
                Document.class);*/
    }

    public Optional<Screen> findScreen(final Vt100Session session) {
        try {
            Optional<Screen> screen;
            // start with exact location match
            screen = findCurrentScreen(session);
            if (screen.isPresent()) {
                return screen;
            }
            // if that fails, try with location margin
            // todo: this seems to be faster than the exact match, if this is a bottleneck at some point, this could be put first
            screen = findCurrentScreenWithRange(session);
            /*if (screen.isPresent()) {
                return screen;
            }
            // if that fails, try by fields
            screen = findCurrentScreenByFields(session);
            if (screen.isPresent()) {
                return screen;
            }
            // if that fails, try by field rows
            screen = findCurrentScreenByFieldRows(session);
            // if that fails, we failed, but just return the optional*/
            return screen;
        } catch (Exception e) {
            log.warn("finding screen failed: " + e.getMessage(), e.getStackTrace());
            return Optional.empty();
        }
    }

    // Searches for a screen or returns empty if no screen is found
    public Optional<Screen> findCurrentScreen(Vt100Session vt100Session) {
        // Search for matching screen
        return Arrays.stream(screenDefinitions.get(vt100Session.getLocationName()).getScreens())
                .filter(s -> sessionMatchesScreenIdentifiers(vt100Session, s.getScreenIdentifiers())).findFirst();
    }

    private boolean sessionMatchesScreenIdentifiers(Vt100Session session, ScreenIdentifier[] identifiers) {
        return Arrays.stream(identifiers).allMatch(i -> {
            try {
                if(i.getRow() != null && i.getColumn() != null){
                    // identifier with fixed position
                    return session.readPosition(i.getRow(), i.getColumn(), i.getValue().length()).equalsIgnoreCase(i.getValue());
                }
                else {
                    // identifier with any position (no row and column set)
                    return Arrays.stream(session.readScreen()).anyMatch(s -> s.toLowerCase().contains(i.getValue().toLowerCase()));
                }
            } catch (JagacyException e) {
                return false;
            }
        });
    }

    /**
     * Get the current screen by iterating over screen identifiers within a location range
     * This is useful in case a screen moved up or down due to an error
     *
     * @param session the vt100 session
     * @return the identified screen
     */
    public Optional<Screen> findCurrentScreenWithRange(final Vt100Session session) {
        // search for matching screen
        return Arrays.stream(screenDefinitions.get(session.getLocationName()).getScreens())
                .filter(screen -> sessionMatchesScreenIdentifiersWithRange(session, screen.getScreenIdentifiers())).findFirst();
    }

    /**
     * This looks for a given screen identifier in a more "dynamic" way,
     * identifiers will be looked for in a range around their described location if they can't be found
     * This is useful in case a screen moved up or down due to an error
     *
     * @param session the vt100 session
     * @param identifiers the screen's identifier list to look for
     * @return whether or not the screen was identified
     */
    private boolean sessionMatchesScreenIdentifiersWithRange(final Vt100Session session, final ScreenIdentifier[] identifiers) {
        return Arrays.stream(identifiers).allMatch(identifier -> screenHasIdentifier(session, identifier));
    }

    /**
     * This looks for a given screen identifier in a more "dynamic" way,
     * the function will look above and below the described identifier location if it can't be found on the exact location
     * This is useful in case a screen moved up or down due to an error
     *
     * @param session the vt100 session
     * @param identifier the identifier to look for
     * @return whether or not the identifier was found
     */
    private boolean screenHasIdentifier(final Vt100Session session, final ScreenIdentifier identifier) {
        try {
            // check if we can find the identifier at the exact given location
            final boolean exactLocationMatch = session.readPosition(identifier.getRow(), identifier.getColumn(), identifier.getValue().length()).equalsIgnoreCase(identifier.getValue());
            // instantly return if a direct location match is found
            if (exactLocationMatch) return true;
            // if we can't find the screen identifier exactly, look above and below the exact line in an attempt to detect a moved screen identifier (due to errors etc)
            // set up the margin limits, start row should not go below 0, end row should not go beyond possible length (optional)
            final int limitedStartMargin = Math.max(0, identifier.getColumn() - identifier.getValue().length() - SCREEN_DETECTION_MARGIN_ROWS);
            final int limitedEndMargin = Math.min(identifier.getValue().length() + identifier.getColumn(), identifier.getValue().length() + SCREEN_DETECTION_MARGIN_ROWS);
            // read two lines above the exact location and check if it contains the identifier, instantly return success if we do
            for (int i = SCREEN_DETECTION_MARGIN_COLUMNS; i > 0; i--) {
                final String readLine = session.readPosition(identifier.getRow() - i, limitedStartMargin, limitedEndMargin);
                final boolean readLineContainsValue = readLine.contains(identifier.getValue());
                if (readLineContainsValue) return true;
            }
            // read two lines below the exact location and check if it contains the identifier, instantly return success if we do
            for (int i = 1; i < SCREEN_DETECTION_MARGIN_COLUMNS + 1; i++) {
                final String readLine = session.readPosition(identifier.getRow() + i, limitedStartMargin, limitedEndMargin);
                final boolean readLineContainsValue = readLine.contains(identifier.getValue());
                if (readLineContainsValue) return true;
            }
            // failed to find identifier
            return false;
        } catch (Exception e) {
            log.error("failed to find screen identifier. Error message: {}", e.getMessage(), e.getStackTrace());
            return false;
        }
    }

    /**
     * This looks for a given screen by searching for the screen's fields
     *
     * @param session the vt100 session
     * @return the identified screen
     */
    public Optional<Screen> findCurrentScreenByFields(final Vt100Session session) {
        // search for matching screen
        return Arrays.stream(screenDefinitions.get(session.getLocationName()).getScreens())
                .filter(screen -> sessionMatchesScreenFields(session, screen.getFields())).findFirst();
    }


    private boolean sessionMatchesScreenFields(final Vt100Session session, final Field[] fields) {
        return fields == null || Arrays.stream(fields).allMatch(field -> screenHasField(session, field));
    }

    /**
     * This looks for a given screen by searching for the screen's fields
     *
     * @param session the vt100 session
     * @return the identified screen
     */
    public Optional<Screen> findCurrentScreenByFieldRows(final Vt100Session session) {
        // search for matching screen
        return Arrays.stream(screenDefinitions.get(session.getLocationName()).getScreens())
                .filter(screen -> sessionMatchesScreenFieldRows(session, screen.getFields())).findFirst();
    }

    /**
     * This looks for a given screen by searching for the screen's fields
     *
     * @param session the vt100 session
     * @param fields the screen's field list to look for
     * @return whether or not the screen was identified
     */
    private boolean sessionMatchesScreenFieldRows(final Vt100Session session, final Field[] fields) {
        return Arrays.stream(fields).allMatch(field -> screenHasFieldRow(session, field));
    }

    /**
     * This looks for a given screen by searching for the screen's fields
     *
     * @param session the vt100 session
     * @param field the field to look for
     * @return whether or not the identifier was found
     */
    private boolean screenHasField(final Vt100Session session, final Field field) {
        try {
            // check if we can find the field at the exact given location of its input
            final boolean exactLocationMatch = locationHasField(session, field.getLabel().getValue(), TIMEOUT, field.getInput().getRow(), field.getInput().getColumn());
            // instantly return if we find an exact match
            if (exactLocationMatch) return true;
            // read two lines above
            for (int i = 1; i <= SCREEN_DETECTION_MARGIN_COLUMNS; i++) {
                final boolean foundPrefix = locationHasField(session, field.getLabel().getValue(), TIMEOUT, field.getInput().getRow() - i, field.getInput().getColumn());
                if (foundPrefix) return true;
            }
            // read two lines below
            for (int i = 1; i < SCREEN_DETECTION_MARGIN_COLUMNS + 1; i++) {
                final boolean foundPrefix = locationHasField(session, field.getLabel().getValue(), TIMEOUT, field.getInput().getRow() + i, field.getInput().getColumn());
                if (foundPrefix) return true;
            }
            // failed to find the field
            return false;
        } catch (Exception e) {
            log.error("failed to find screen identifier {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // todo: move this method to the macroservice, outsource this behaviour to the cursor method because it's bascially the same
    /**
     * Parses an area with default margins prefixing the given position to detect a field (or string)
     *
     * @param session               the vt100 session
     * @param field                 the field that will be looked for
     * @param timeoutInMilliseconds time until execution timeout
     * @return location prefix contained field or not
     */
    public boolean locationHasField(final Vt100Session session, final String field, final int timeoutInMilliseconds, final int row, final int column) {
        return locationHasField(session, field, FIELD_DETECTION_START_MARGIN_DEFAULT, FIELD_DETECTION_END_MARGIN_DEFAULT, timeoutInMilliseconds, row, column);
    }

    // todo: move this method to the macroservice, outsource this behaviour to the cursor method because it's bascially the same
    /**
     * Parses an area with given margins prefixing the given position to detect a field (or string)
     *
     * @param session               the vt100 session
     * @param field                 the field that will be looked for
     * @param startMargin           the margin that will be used preceding the field length
     * @param endMargin             the margin that will be used following the field length
     * @param timeoutInMilliseconds time until execution timeout
     * @return location prefix contained field or not
     */
    public boolean locationHasField(final Vt100Session session, final String field, final int startMargin, final int endMargin, final int timeoutInMilliseconds, final int row, final int column) {
        try {
            // wait until the current field is done buffering and the keyboard is unlocked
            session.waitForUnlock(timeoutInMilliseconds); // todo: this might not be necessary here

            // read the line before the position with margin (from start to way to far beyond)
            final String positionPrefix = session.readPosition(row, 0, column + field.length() + endMargin);

            // debug
            log.info("read positionPrefix");
            log.info("{}", positionPrefix);
            log.info("positionPrefix equal to field? {}", positionPrefix.equals(field));
            log.info("positionPrefix contains field? {}", positionPrefix.contains(field));

            // check if the line prefix is equal to, or contains, the desired field
            final boolean positionPrefixEqualToField = positionPrefix.equals(field);
            final boolean positionPrefixContainsField = positionPrefix.contains(field);

            // show a warning if prefix is not exact, in case a field happens to contain a word that also matches but isn't supposed to match
            if (!positionPrefixEqualToField && positionPrefixContainsField) {
                log.warn("position prefix was not equal to field, but contained field. read prefix: {}", positionPrefix);
            }

            return positionPrefixEqualToField || positionPrefixContainsField;
        } catch (Exception e) {
            log.error("failed to find field {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private boolean screenHasFieldRow(final Vt100Session session, final Field field) {
        try {
            // get the row of the field, return instantly if the value is found there
            final String exactRow = session.readRow(field.getInput().getRow());
            if (exactRow.contains(field.getLabel().getValue())) return true;
            // if the value is not found there, look up and down around the exact row
            for (int i = 1; i <= SCREEN_DETECTION_MARGIN_COLUMNS; i++) {
                // get the row and check it for a value match, return instantly if we find it
                final String row = session.readRow(field.getInput().getRow() - i);
                if (row.contains(field.getLabel().getValue())) return true;
            }
            for (int i = 1; i < SCREEN_DETECTION_MARGIN_COLUMNS + 1; i++) {
                // get the row and check it for a value match, return instantly if we find it
                final String row = session.readRow(field.getInput().getRow() + i);
                if (row.contains(field.getLabel().getValue())) return true;
            }
            // failed to find the field
            return false;
        } catch (Exception e) {
            log.error("Failed to find screen identifier, error: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

}
