package com.resonance.letsdata.data.readers.interfaces.sqs;

import com.resonance.letsdata.data.readers.model.ParseDocumentResult;

import java.util.Map;

public interface QueueMessageReader {
    ParseDocumentResult parseMessage(String messageId, String messageGroupId, String messageDeduplicationId, Map<String, String> messageAttributes, String messageBody);
}
