package com.art4l.dataconnector.module.vt100.domain.macro;

import com.art4l.dataconnector.module.vt100.domain.dto.screenxml.Field;
import com.art4l.dataconnector.module.vt100.domain.exception.ApplicationException;
import com.art4l.dataconnector.module.vt100.domain.exception.UserException;
import com.art4l.dataconnector.module.vt100.domain.vt100.Vt100Session;
import com.art4l.dataconnector.module.vt100.service.MacroService;
import com.jagacy.Key;
import com.jagacy.util.JagacyException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Predicate;

@Builder
@AllArgsConstructor
public class CancelMacroNode implements MacroNode {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private String description;

    @Builder.Default
    private boolean waitForCursor = true;
    @Builder.Default
    private boolean optional = false;
    private Predicate<Void> predicate;
    @Builder.Default
    private boolean skipPagesWithDefaultInput = true;

    @Override
    public void execute(MacroService macroService, Vt100Session session) throws ApplicationException, UserException, JagacyException {
        log.info("Executing CancelMarcoNode - " + description);

        //TODO : parametrizeer de cancel key
        String validCancelKey = Key.ESCAPE_NAME;

        // Wait until page is ready
        macroService.waitForPageReady(session);

        // Skip all pages with default input
        if(skipPagesWithDefaultInput){
            macroService.skipPagesWithDefaultInput(session);
        }
        // Find submit key
        Key submitKey = Key.ENTER;
        Optional<Field> optionalField = macroService.findActiveField(session);
        if(!optionalField.isPresent()){
            log.warn("Could not find active field, leaving submitKey as default");
        }
        else {
            if(optionalField.get().getNavigationKeys() != null && optionalField.get().getNavigationKeys().containsKey("submit")){
                submitKey = Key.find(optionalField.get().getNavigationKeys().get("submit"));
            }
            else {
                log.warn("No navigation key set for field " + optionalField.get().getName() + " using default key");
            }
        }
        String cancelKey = Key.ESCAPE_NAME;
        if(!optionalField.isPresent()){
            log.warn("Could not find active field, leaving cancel key as default");
        }
        else {
            if(optionalField.get().getNavigationKeys() != null && optionalField.get().getNavigationKeys().containsKey("cancel")){
                cancelKey = optionalField.get().getNavigationKeys().get("cancel");
            }
            else {
                log.warn("No navigation key set for field " + optionalField.get().getName() + " using default cancel key");
            }
        }

        // Execute if predicate is met
        if(predicate != null){
            if(predicate.test(null)){
                log.info("Predicate was valid, executing CancelMarcoNode - " + description);
                //macroService.submitInput(session, cancelKey, submitKey, waitForCursor);
                execute(macroService, session, cancelKey, submitKey, waitForCursor,validCancelKey);
            }
            else if(optional){
                log.info("CancelMarcoNode skipped, predicate was false and node is optional - " + description);
            }
            else {
                log.warn("Conditions CancelMarcoNode were not met and node was not optional - " + description);
            }
        }
        else {
            //macroService.submitInput(session, cancelKey, submitKey, waitForCursor);
            execute(macroService, session, cancelKey, submitKey, waitForCursor,validCancelKey);
        }
    }

    private void execute(MacroService macroService, Vt100Session session, String cancelKey, Key submitKey, boolean waitForCursor,String validCancelKey){
        if(cancelKey.equalsIgnoreCase(validCancelKey)){
            macroService.executeKeySequence(session, Collections.singletonList(Key.ESCAPE));
        }
        else {
            macroService.submitInput(session, cancelKey, submitKey, waitForCursor);
        }
    }
}