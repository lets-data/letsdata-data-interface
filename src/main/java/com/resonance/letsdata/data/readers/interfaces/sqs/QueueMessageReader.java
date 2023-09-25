package com.resonance.letsdata.data.readers.interfaces.sqs;

import com.resonance.letsdata.data.readers.model.ParseDocumentResult;

import java.util.Map;

public interface QueueMessageReader {
    /**
     The #Let's Data SQS Queue Reader uses this interface's implementation (also called as user data handlers) to transform the messages from SQS queue message to a #Let's Data document. At a high level, the overall # Let's Data SQS reader design is as follows:

     * #Let's Data reads the messages from the SQS queue and passes the message contents to the user data handlers.
     * The user data handlers transform this message and returns a document.
     * #Let's data writes the document to the write / error destinations and then deletes the message from the SQS queue.
     * For any errors in # Let's Data SQS Reader, or error docs being returned by the user data handler, #Let's Data looks at the reader configuration and determines 1./ whether to fail the task with error 2./ or write an error doc and continue processing
     * If the decision is to continue processing, the reader deletes the message from the queue and polls for next message.

     +---------------------+                              +---------------------+                        +---------------------+
     |                     | ------ Read Message -------> |    # Let's Data     |---- parseDocument ---> |  User Data Handler  |
     |                     |                              |     SQS Reader      |<---- document -------- |                     |
     |                     |                              |                     |                        +---------------------+
     |   AWS SQS Queue     |                              |   Is Error Doc?     |
     |                     |                              |        |            |                        +---------------------+
     |                     |                              |        +---- yes ->-|---- write document --->|  Write Destination  |
     |                     |                              |        |            |                        +---------------------+
     +---------------------+                              |        |            |                        +---------------------+
              ^                                           |        +---- no -->-|---- write error ------>|  Error Destination  |
              |                                           |        |            |                        +---------------------+
              |                                           |   Should Delete?    |
              |                                           |        |            |
              +---<------- Delete Message ---------<------|<- yes -+            |
                                                          |        |            |
                                                          |        |            |
                                                          |  no <--+            |
                                                          |  |                  |
                                                          |  V                  |
                                                          |  Throw on Error     |
                                                          +---------------------+

     The SQS read connector configuration has details about the SQS receive message batch size and on dealing with failures.

     * @param messageId The SQS message messageId
     * @param messageGroupId The SQS message messageGroupId
     * @param messageDeduplicationId The SQS message messageDeduplicationId
     * @param messageAttributes The SQS message messageAttributes
     * @param messageBody The SQS message messageBody
     * @return ParseDocumentResult which has the extracted document and the status (error, success or skip)
     */
    ParseDocumentResult parseMessage(String messageId, String messageGroupId, String messageDeduplicationId, Map<String, String> messageAttributes, String messageBody);
}
