package com.art4l.dataconnector.module.vt100.domain.macro;

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

import java.util.List;
import java.util.function.Predicate;

@Builder
@AllArgsConstructor
public class KeySequenceMacroNode implements MacroNode {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private String description;
    private Predicate<Void> predicate;
    private List<Key> keys;
    @Builder.Default
    private boolean skipPagesWithDefaultInput = true;

    @Override
    public void execute(MacroService macroService, Vt100Session session) throws ApplicationException, UserException, JagacyException {
        log.info("Executing KeySequenceMacroNode - " + description);

        // Wait until page is ready
        macroService.waitForPageReady(session);

        // Skip all pages with default input
        if(skipPagesWithDefaultInput){
            macroService.skipPagesWithDefaultInput(session);
        }
        // Check if predicate is met
        if(predicate != null){
            if(!predicate.test(null)){
                log.info("Conditions KeySequenceMacroNode were not met, skipping node - " + description);
            }
            else {
                log.info("Predicate is true, executing KeySequenceMacroNode - " + description);
                macroService.executeKeySequence(session, keys);
            }
        }
        else {
            log.warn("KeySequenceMacroNode without predicate executed - " + description);
            macroService.executeKeySequence(session, keys);
        }

        macroService.waitForPageReady(session);
    }
}
