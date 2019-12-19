package com.art4l.dataconnector.module.vt100.service.impl.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import com.art4l.dataconnector.module.vt100.domain.dto.screenxml.Document;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScreenConfigUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenConfigUtil.class);

    private static final Object MUTEX_CHECK = new Object();

    private static Document screenDefinitions;

    // thread safe instance creation, use synchronized if necessary, avoid overhead if possible
    public static Document getScreenDefinitions() {
        Document result = screenDefinitions;
        if (result == null) {
            synchronized (MUTEX_CHECK) {
                result = screenDefinitions;
                if (result == null)
                    screenDefinitions = result = loadScreenDefinitions();
            }
        }
        return result;
    }

    private static Document loadScreenDefinitions() {
        // Read screens from xml
        final ObjectMapper objectMapper = new XmlMapper();
        Document document = null;
        try {
            document = objectMapper.readValue(
                    StringUtils.toEncodedString(
                            Files.readAllBytes(new File("src/main/resources/scn.xml").toPath()),
                            StandardCharsets.UTF_8
                    ),
                    Document.class);
        } catch (IOException e) {
            LOGGER.error("failed to load screendefinitions {}", e.getMessage());
        }
        return document;
    }
}
