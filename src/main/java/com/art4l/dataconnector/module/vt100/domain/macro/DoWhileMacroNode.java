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
public class DoWhileMacroNode implements MacroNode {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private String description;
    private Predicate<Void> predicate;
    private MacroPipeline child;
    @Builder.Default
    private boolean skipPagesWithDefaultInput = true;

    @Override
    public void execute(MacroService macroService, Vt100Session session) throws ApplicationException, UserException, JagacyException {
        log.info("Executing DoWhileMacroNode - " + description);

        // wait until page is ready
        macroService.waitForPageReady(session);

        // skip pages with default input
        if(skipPagesWithDefaultInput){
            macroService.skipPagesWithDefaultInput(session);
        }

        // Keep executing as long as predicate is valid
        while(predicate.test(null)){
            // Wait until page is ready
            macroService.waitForPageReady(session);

            // Skip all pages with default input
            if(skipPagesWithDefaultInput){
                macroService.skipPagesWithDefaultInput(session);
            }
            child.execute();
        }
    }
}
