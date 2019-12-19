package com.art4l.dataconnector.module.vt100.domain.macro;

import com.art4l.dataconnector.module.vt100.domain.exception.ApplicationException;
import com.art4l.dataconnector.module.vt100.domain.exception.UserException;
import com.art4l.dataconnector.module.vt100.domain.vt100.Vt100Session;
import com.art4l.dataconnector.module.vt100.domain.dto.screenxml.Field;
import com.art4l.dataconnector.module.vt100.service.MacroService;
import com.jagacy.Key;
import com.jagacy.util.JagacyException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Predicate;

@Builder
@AllArgsConstructor
public class InputMacroNode implements MacroNode {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private String description;
    private String input;
    @Builder.Default
    private boolean waitForCursor = true;
    @Builder.Default
    private boolean optional = false;
    private Predicate<Void> predicate;
    @Builder.Default
    private boolean skipPagesWithDefaultInput = true;
    @Builder.Default
    private boolean ignoreSamePage = false;
    @Builder.Default
    private boolean addPadding = false;

    @Override
    public void execute(MacroService macroService, Vt100Session session) throws ApplicationException, UserException, JagacyException {
        log.info("Executing InputMacroNode - " + description);

        // Wait until page is ready
        macroService.waitForPageReady(session, ignoreSamePage);

        // Skip all pages with default input
        if(skipPagesWithDefaultInput){
            macroService.skipPagesWithDefaultInput(session);
        }

        // Find submit key
        Key submitKey = Key.ENTER;
        Optional<Field> optionalField = macroService.findActiveField(session,ignoreSamePage);
        if(!optionalField.isPresent()){
            log.warn("Could not find active field, leaving submitKey as default");
        }
        else {
            if(optionalField.get().getNavigationKeys() != null && optionalField.get().getNavigationKeys().containsKey("submit")){
                if(optionalField.get().getNavigationKeys().get("submit").isEmpty()){
                    submitKey = null;
                }
                else {
                    submitKey = Key.find(optionalField.get().getNavigationKeys().get("submit"));
                }
            }
            else {
                log.warn("No navigation key set for field " + optionalField.get().getName() + " using default key");
            }
        }

        // Execute if predicate is met
        if(predicate != null){
            if(predicate.test(null)){
                log.info("Predicate was valid, executing InputMacroNode - " + description);
                execute(macroService, session, input, submitKey, waitForCursor, addPadding);
            }
            else if(optional){
                log.info("InputMacroNode skipped, predicate was false and node is optional - " + description);
            }
            else {
                log.warn("Conditions InputMacroNode were not met and node was not optional - " + description);
                throw new ApplicationException("Preconditions InputMacroNode not met - " + description);
            }
        }
        else {
            execute(macroService, session, input, submitKey, waitForCursor, addPadding);
        }
    }

    private void execute(MacroService macroService, Vt100Session session, String input, Key submitKey, boolean waitForCursor, boolean addPadding){
        if(addPadding){
            input += "                    ";
            waitForCursor = false;
        }
        macroService.submitInput(session, input, submitKey, waitForCursor);
    }
}