package com.resonance.letsdata.data.readers.interfaces.kinesis;

import com.resonance.letsdata.data.readers.model.ParseDocumentResult;

import java.nio.ByteBuffer;
import java.util.Date;

public interface KinesisRecordReader {
    /**
     The #LetsData Kinesis Record Reader uses this interface's implementation (also called as user data handlers) to transform the records from Kinesis stream to a #Let's Data document. At a high level, the overall #LetsData Kinesis reader design is as follows:

     * #LetsData reads the records from the stream from the specified location (sequenceNumber) and passes the record contents to the user data handlers.
     * The user data handlers transform this record and returns a document.
     * #LetsData writes the document to the write / error destinations and checkpoints the location (sequenceNumber) in Kinesis stream.
     * For any errors in #LetsData Kinesis Reader, or error docs being returned by the user data handler, #LetsData looks at the reader configuration and determines 1./ whether to fail the task with error 2./ or write an error doc and continue processing
     * If the decision is to continue processing, the reader polls for next record in the stream.

     +---------------------+                              +---------------------+                        +---------------------+
     | AWS Kinesis Stream  | ------ Read Message -------> |    # Lets Data      |---- parseDocument ---> |  User Data Handler  |
     |       Shard         |                              |   Kinesis Reader    |<---- document -------- |                     |
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

     The Kinesis read connector configuration has details about the Kinesis read and on dealing with failures.

     * @param streamArn The kinesis stream ARN
     * @param shardId The kinesis stream shardId
     * @param partitionKey The record's partitionKey
     * @param sequenceNumber The record's sequenceNumber
     * @param approximateArrivalTimestamp The record's approximateArrivalTimestamp
     * @return ParseDocumentResult which has the extracted document and the status (error, success or skip)
     */
    ParseDocumentResult parseMessage(String streamArn, String shardId, String partitionKey, String sequenceNumber, Date approximateArrivalTimestamp, ByteBuffer data);
}
