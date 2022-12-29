package com.resonance.letsdata.data.readers.model;

import com.resonance.letsdata.data.util.ValidationUtils;

public enum SingleFileReaderState {
    CREATED,
    FILE_DOWNLOADED,
    PROCESSING,
    CLOSED,
    COMPLETED;

    public static void assertValidTransition(SingleFileReaderState from, SingleFileReaderState to) {
        if (from == to) {
            return;
        }

        switch (from) {
            case CREATED: {
                ValidationUtils.validateAssertCondition(to == FILE_DOWNLOADED || to == PROCESSING || to == CLOSED, "created -> (FILE_DOWNLOADED, PROCESSING, CLOSED) transition expected", to);
                return;
            }
            case FILE_DOWNLOADED: {
                ValidationUtils.validateAssertCondition(to == PROCESSING, "file downloaded -> processing transition expected", to);
                return;
            }
            case PROCESSING: {
                ValidationUtils.validateAssertCondition(to == COMPLETED || to == CLOSED , "processing -> (COMPLETED, CLOSED) transition expected", to);
                return;
            }
            case COMPLETED: {
                ValidationUtils.validateAssertCondition(to == CLOSED , "COMPLETED -> (CLOSED) transition expected", to);
                return;
            }
            case CLOSED: {
                throw new RuntimeException("Closed is a Terminal State - no transition expected");
            }
            default:{
                throw new RuntimeException("Unexpected From State");
            }
        }
    }
}
