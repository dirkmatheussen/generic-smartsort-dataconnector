package com.art4l.dataconnector.module.vt100.domain.macro;

/**
 * Macro to put a screen in sleep mode for x time, continous only when time is expired
 *
 */


import com.art4l.dataconnector.module.vt100.domain.exception.ApplicationException;
import com.art4l.dataconnector.module.vt100.domain.exception.UserException;
import com.art4l.dataconnector.module.vt100.domain.vt100.Vt100Session;
import com.art4l.dataconnector.module.vt100.service.MacroService;
import com.jagacy.util.JagacyException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

@Builder
@AllArgsConstructor
public class SleepMacroNode implements MacroNode {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private String description;
    private Predicate<Void> predicate;
    @Builder.Default
    private boolean skipPagesWithDefaultInput = true;
    @Builder.Default
    private boolean optional = false;
    @Builder.Default
    private boolean ignoreSamePage = false;
    @Builder.Default
    private long timeout = 2000;

    @Override
    public void execute(MacroService macroService, Vt100Session session) throws ApplicationException, UserException, JagacyException {
        log.info("Executing SleepMacroNode - " + description);

        // Wait until page is ready
        macroService.waitForPageReady(session, ignoreSamePage);

        // Skip all pages with default input
        if(skipPagesWithDefaultInput){
            macroService.skipPagesWithDefaultInput(session);
        }

        // Execute if predicate is met
        if(predicate != null){
            if(predicate.test(null)){
                log.info("Predicate was valid, executing SleepMacroNode - " + description);
                execute();
            }
            else if(optional){
                log.info("SleepMacroNode skipped, predicate was false and node is optional - " + description);
            }
            else {
                log.warn("Conditions SleepMacroNode were not met and node was not optional - " + description);
                throw new ApplicationException("Preconditions SleepMacroNode not met - " + description);
            }
        }
        else {
            execute();
        }
    }

    private void execute(){
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            // ignore
        }
    }
}