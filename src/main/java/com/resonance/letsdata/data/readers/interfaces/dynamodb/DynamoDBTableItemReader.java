package com.resonance.letsdata.data.readers.interfaces.dynamodb;

import com.resonance.letsdata.data.readers.model.ParseDocumentResult;
import java.util.Map;

public interface DynamoDBTableItemReader {
    /**
     * The #LetsData DynamoDB Table Item Reader uses this interface's implementation (also called as user data handlers) to transform the records from DynamoDB Item to a #LetsData document. At a high level, the overall #LetsData DynamoDB Table Item Reader design is as follows:

     *  * #LetsData scans the DynamoDB table and passes the items to the user data handlers.
     *  * The user data handlers transform this record and returns a document.
     *  * #LetsData writes the document to the write / error destinations and checkpoints the DynamoDB table's location (lastEvaluatedKey)
     *  * For any errors in #LetsData DynamoDB Table Item Reader, or error docs being returned by the user data handler, #LetsData looks at the reader configuration and determines 1./ whether to fail the task with error 2./ or write an error doc and continue processing
     *  * If the decision is to continue processing, the reader polls for next record in the stream.

     +---------------------+                              +---------------------+                        +---------------------+
     | AWS DynamoDB Table  | ------ Read Items - -------> |    # Lets Data      |---- parseDocument ---> |  User Data Handler  |
     |       Scan          |                              |  DDB Table Reader   |<---- document -------- |                     |
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

     * The DynamoDB Table Item read connector configuration has details about the DynamoDB Table Item read and on dealing with failures.
     * @param tableName - The DynamoDB tableName
     * @param segmentNumber - The DynamoDB scan segmentNumber
     * @param keys - The primary key attribute(s) for the scanned DynamoDB item
     * @param item - The scanned item from the DynamoDB table
     * @return ParseDocumentResult which has the extracted document and the status (error, success or skip)
     */
    ParseDocumentResult parseDynamoDBItem(String tableName, int segmentNumber, Map<String, Object> keys, Map<String, Object> item);
}
