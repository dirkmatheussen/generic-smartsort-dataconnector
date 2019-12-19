package com.art4l.dataconnector.module.vt100.domain.macro;

import com.art4l.dataconnector.module.vt100.domain.exception.ApplicationException;
import com.art4l.dataconnector.module.vt100.domain.exception.UserException;
import com.art4l.dataconnector.module.vt100.domain.vt100.Vt100Session;
import com.art4l.dataconnector.module.vt100.service.MacroService;
import com.jagacy.util.JagacyException;

public interface MacroNode {
    void execute(MacroService macroService, Vt100Session session) throws ApplicationException, UserException, JagacyException;

}
