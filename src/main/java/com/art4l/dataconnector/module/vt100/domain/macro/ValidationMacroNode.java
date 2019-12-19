package com.art4l.dataconnector.module.vt100.domain.macro;

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
public class ValidationMacroNode implements MacroNode {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private String description;
    private Predicate<Void> predicate;
    @Builder.Default
    private boolean userException = false;
    @Builder.Default
    private String errorMessage = "Predicate was not met";
    @Builder.Default
    private boolean skipPagesWithDefaultInput = true;
    @Builder.Default
    private boolean ignoreSamePage = false;



    @Override
    public void execute(MacroService macroService, Vt100Session session) throws ApplicationException, UserException, JagacyException {
        log.info("Executing ValidationMacroNode - " + description+ " ignore Same Page: " + ignoreSamePage +" skip pages: "+ skipPagesWithDefaultInput);

        // Wait until page is ready
        macroService.waitForPageReady(session,ignoreSamePage);

        // Skip all pages with default input
        if(skipPagesWithDefaultInput){
            macroService.skipPagesWithDefaultInput(session);
        }

        // Check if predicate is met
        if(predicate != null){
            if(!predicate.test(null)){
                log.error("Conditions ValidationMacroNode were not met - " + description);
                if(userException){
                    throw new UserException(errorMessage);
                }
                else {
                    throw new ApplicationException(errorMessage);
                }
            }
        }
        else {
            log.warn("ValidationMacroNode without Predicate - " + description);
        }
    }
}
