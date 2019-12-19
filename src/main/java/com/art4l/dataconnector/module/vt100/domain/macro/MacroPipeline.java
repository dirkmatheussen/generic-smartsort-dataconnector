package com.art4l.dataconnector.module.vt100.domain.macro;

import com.art4l.dataconnector.module.vt100.domain.exception.ApplicationException;
import com.art4l.dataconnector.module.vt100.domain.exception.UserException;
import com.art4l.dataconnector.module.vt100.domain.vt100.Vt100Session;
import com.art4l.dataconnector.module.vt100.service.MacroService;
import com.jagacy.util.JagacyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MacroPipeline {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private Vt100Session vt100Session;
    private List<MacroNode> nodes;
    private MacroService macroService;
    private boolean skipPagesWithDefaultInput;
    private boolean ignoreSamePage;

    public MacroPipeline(MacroService macroService, Vt100Session vt100Session){
        this(macroService, vt100Session, true, false);
    }

    public MacroPipeline(MacroService macroService, Vt100Session vt100Session, boolean skipPagesWithDefaultInput){
        this(macroService, vt100Session, skipPagesWithDefaultInput, false);

    }

    public MacroPipeline(MacroService macroService, Vt100Session vt100Session, boolean skipPagesWithDefaultInput, boolean ignoreSamePage){
        this.macroService = macroService;
        this.vt100Session = vt100Session;
        this.nodes = new ArrayList<>();
        this.skipPagesWithDefaultInput = skipPagesWithDefaultInput;
        this.ignoreSamePage = ignoreSamePage;
    }

    public MacroPipeline next(MacroNode macroNode){
        this.nodes.add(macroNode);
        return this;
    }

    public void execute() throws ApplicationException, UserException, JagacyException {
        try {
            for(MacroNode node: nodes){
                node.execute(macroService, vt100Session);
            }

            // Wait until page is ready
            macroService.waitForPageReady(vt100Session, ignoreSamePage);

            // Skip default pages
            if(skipPagesWithDefaultInput){
                macroService.skipPagesWithDefaultInput(vt100Session);
            }

        } catch (UserException e) {
            log.info("User exception thrown: " + e.getMessage());
            throw e;
        } catch (JagacyException e){
            log.error("JagacyException: " + e.getMessage());
            throw e;
        } catch (Exception e){
            log.error("Application Exception: " + e.getMessage());
            throw new ApplicationException(e.getMessage());
        }
    }

}