package com.resonance.letsdata.data.readers.interfaces.dynamodbstreams;

import com.resonance.letsdata.data.readers.model.ParseDocumentResult;

import java.util.Date;
import java.util.Map;

public interface DynamoDBStreamsRecordReader {
    /**
     * The #LetsData DynamoDB Streams Record Reader uses this interface's implementation (also called as user data handlers) to transform the records from DynamoDB stream to a #LetsData document. At a high level, the overall #LetsData DynamoDB Stream reader design is as follows:

     *  * #LetsData reads the records from the DynamoDB stream from the specified location (sequenceNumber) and passes the record contents to the user data handlers.
     *  * The user data handlers transform this record and returns a document.
     *  * #LetsData writes the document to the write / error destinations and checkpoints the location (sequenceNumber) in DynamoDB stream.
     *  * For any errors in #LetsData DynamoDB Streams Reader, or error docs being returned by the user data handler, #LetsData looks at the reader configuration and determines 1./ whether to fail the task with error 2./ or write an error doc and continue processing
     *  * If the decision is to continue processing, the reader polls for next record in the stream.

     +---------------------+                              +---------------------+                        +---------------------+
     | AWS DynamoDB Stream | ------ Read Message -------> |    # Lets Data      |---- parseDocument ---> |  User Data Handler  |
     |       Shard         |                              |  DDB Stream Reader  |<---- document -------- |                     |
     +---------------------+                              |                     |                        +---------------------+
                                                          |   Is Error Doc?     |
                                                          |        |            |                        +---------------------+
                                                          |        +---- yes ->-|---- write document --->|  Write Destination  |
                                                          |        |            |                        +---------------------+
                                                          |        |            |                        +---------------------+
                                                          |        +---- no -->-|---- write error ------>|  Error Destination  |
                                                          |        |            |                        +---------------------+
                                                          | Should Checkpoint?  |
                                                          |        |            |
               ---<------- Checkpoint Task --------<------|<- yes -+            |
                                                          |        |            |
                                                          |        |            |
                                                          | Throw on Error?     |
                                                          |<- yes -+            |
                                                          |        |            |
                                                          |        V            |
                                                          |  Throw on Error     |
                                                          +---------------------+

     * The DynamoDB Streams read connector configuration has details about the DynamoDB Streams read and on dealing with failures.
     *
     * For detailed explanation of the interface parameters, see AWS docs as well:
     *  * https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_streams_Record.html
     *  * https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_streams_StreamRecord.html
     *
     * @param streamArn - The DynamoDB Stream ARN
     * @param shardId - The DynamoDB Shard Id
     * @param eventId - A globally unique identifier for the event that was recorded in this stream record.
     * @param eventName - The type of data modification that was performed on the DynamoDB table. INSERT | MODIFY | REMOVE
     * @param identityPrincipalId - The userIdentity's principalId
     * @param identityType - The userIdentity's principalType
     * @param sequenceNumber - The sequence number of the stream record
     * @param sizeBytes - The size of the stream record, in bytes
     * @param streamViewType - The stream view type - NEW_IMAGE | OLD_IMAGE | NEW_AND_OLD_IMAGES | KEYS_ONLY
     * @param approximateCreationDateTime - The approximate date and time when the stream record was created, in UNIX epoch time format and rounded down to the closest second
     * @param keys - The primary key attribute(s) for the DynamoDB item that was modified
     * @param oldImage - The item in the DynamoDB table as it appeared before it was modified
     * @param newImage - The item in the DynamoDB table as it appeared after it was modified
     * @return ParseDocumentResult which has the extracted document and the status (error, success or skip)
     */
    ParseDocumentResult parseRecord(String streamArn, String shardId, String eventId, String eventName, String identityPrincipalId, String identityType, String sequenceNumber, Long sizeBytes, String streamViewType, Date approximateCreationDateTime, Map<String, Object> keys, Map<String, Object> oldImage, Map<String, Object> newImage);
}
