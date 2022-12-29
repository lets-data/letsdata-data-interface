package com.resonance.letsdata.data.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class ValidationUtils {
    private static final Logger logger = LoggerFactory.getLogger(ValidationUtils.class);

    public static void validateAssertCondition(boolean isTrue, String errorMessage, Object...params) {
        if (!isTrue) {
            logger.error("validateAssertCondition failed - {} - params: {}", errorMessage, params.length != 0 ? Arrays.toString(params) : "");
            throw new RuntimeException("validateAssertCondition failed - "+errorMessage);
        }
    }
}
