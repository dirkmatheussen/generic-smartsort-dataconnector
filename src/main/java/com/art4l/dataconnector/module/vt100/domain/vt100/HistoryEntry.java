package com.art4l.dataconnector.module.vt100.domain.vt100;

import com.art4l.dataconnector.module.vt100.domain.dto.screenxml.Field;
import com.art4l.dataconnector.module.vt100.domain.dto.screenxml.Screen;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HistoryEntry {
    private String[] screen;
    private Screen detectedScreen;
    private Field detectedField;
    private Long timestamp;
}
