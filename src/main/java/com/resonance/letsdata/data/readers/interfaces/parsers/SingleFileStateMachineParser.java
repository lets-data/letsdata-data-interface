package com.resonance.letsdata.data.readers.interfaces.parsers;

import com.resonance.letsdata.data.documents.interfaces.DocumentInterface;
import com.resonance.letsdata.data.readers.model.ParseDocumentResult;
import com.resonance.letsdata.data.readers.model.RecordParseHint;

/**
 * The parser interface for Single File State Machine Reader usecase. This is used when data document to be extracted is completely contained in a single file but is created from multiple data record in the file.
 * This interface is where you tell us how to parse the individual records from the file. Since this is single file state machine parser, the records in the file follow a finite state machine.
 * This class maintains the overall state machine for the file parser. It will create the extracted document from different file records that are being read from the files.
 *
 * Simple example is a data file where a record's metadata and data are written sequentially as two separate records.
 *
 *                     Example: Single File State Machine Reader Files and Record Layout:
 *                     +-----------------------+---------------------------+---------------------------+
 *                     | s3file_1.gz           | s3file_2.gz               | s3file_3.gz               |
 *                     +-----------------------+---------------------------+---------------------------+
 *                     | &lt;metadata_record_1&gt;   | &lt;metadata_record_n&gt;       | &lt;metadata_record_m&gt;       |
 *                     | &lt;data_record_1&gt;       | &lt;data_record_n&gt;           | &lt;data_record_m&gt;           |
 *                     | &lt;metadata_record_2&gt;   | &lt;metadata_record_n+1&gt;     | &lt;metadata_record_m+1&gt;     |
 *                     | &lt;data_record_2&gt;       | &lt;data_record_n+1&gt;         | &lt;data_record_m+1&gt;         |
 *                     | ...                   | ...                       | ...                       |
 *                     +-----------------------+---------------------------+---------------------------+
 *
 *                     Example:  Single File State Machine Parser's Finite State Machine
 *                     +----------+                   +------+
 *                     | Metadata | -------->-------- | Data | -->----+
 *                     +----------+                   +------+        |
 *                     ^                                              V
 *                     |                                              |
 *                     +--------<----------------------<--------------+
 */
public interface SingleFileStateMachineParser {
    /**
     * This method returns the filetype (logical name) for the file being parsed by the Single File State Machine parser.
     * For example, if the metadata records are in file s3file_1.gz and we've named this filetype in
     * the dataset creation as DATAFILE, then this function will return a DATAFILE as the file type
     *
     * Here is an example implementation:
     *
     *     public String getS3FileType() {
     *         return "DATAFILE";
     *     }
     *
     * @return - the fileType of the file
     */
    String getS3FileType();

    /**
     * Given the filetype (DATAFILE) and filename (s3file_1.gz) from the manifest file, return the resolved filename if necessary.
     * In most cases, the resolved filename would be the same as the filename, but in some cases, you might need to prepend paths if the data in the s3 bucket is not in the root directory.
     * For example, dataset_name/data_date/s3file_1.gz (See the docs for MultipleFileStateMachineReader.getResolvedS3Filename method for additional details around the file name resolution)
     *
     * Here is an example implementation using the example in the class doc:
     *
     *      public String getResolvedS3FileName(String s3FileType, String fileName) {
     *          ValidationUtils.validateAssertCondition(getS3FileType().equals(fileType.toUpperCase()), "fileType is unexpected");
     *          ValidationUtils.validateAssertCondition(fileName != null, "filename should not be null");
     *          // no resolution being done, expect incoming file names to be resolved but custom logic can be put here
     *          return resolvedFileNames;
     *      }
     *
     * @param s3FileType - the filetype - example DATAFILE
     * @param fileName - the filename - example metadata_file_1.gz
     * @return - the resolved fileName - example dataset_name/data_date/metadata_file_1.gz
     */
    String getResolvedS3FileName(String s3FileType, String fileName);

    /**
     * Given the fileType, next expected record type and the last processed doc, this function returns the start delimiter for the next record
     * To extract the records from the file, the parser needs to know the expected record type for the state machine and the record type start and end delimiters - it will search the file sequentially till it finds this delimiter and then from that point on, it will search for the end record delimiter.
     *
     * For example, for the following {metadata, data} records in the data file, we define the start patterns:
     *
     *              metadata record start pattern: \r\nRecord-Type: metadata
     *              metadata record end pattern: \r\nRecord-Type: data
     *
     *              data record start pattern: \r\nRecord-Type: data
     *              data record end pattern: \r\nRecord-Type: metadata
     *
     *              Logfile:
     *              --------
     *
     *              Record-Type: metadata
     *              Content-Language: en-us
     *              Content-Length: 1024
     *              Content-Encoding: gzip
     *
     *
     *              Record-Type: data
     *              Content: eJxtUTFuwzAM3POKg8ci7QP6gK5dOmZRZbomKpOuxMAJiv69lBInRlEDAiweeXc8AdfvAW8KOlkO0WAjIVPU3BcMWadWGDjRvv3NIRfKECLHTfEpumxmUCxkQ0+JJzZvfAQbFk4JhUKO443O719HEuOQ0hlWO7xzYKm8I5cNR5C+jsnqJxhmZTGo7P/SD5qbBPnM1dKN6AmvEmkr83/fnTTq7N5GLYT3s1Hb+JYCeo3HyVfAcJRorFLhmCkYuec1UVoFdrgH/uI26RSmuea6eh40JV1YPpDUD4sLsjSoFi6PsLguuX9q9UvcczC3LQgF3fehs3LoupZa3W4L/hyke979Ar2qsBI=
     *
     *
     *               @Override
     *               public RecordParseHint getNextRecordStartPattern(String s3FileType, String nextExpectedRecordType, DocumentInterface lastProcessedDoc) {
     *                  ValidationUtils.validateAssertCondition(s3FileType != null && s3FileType.equalsIgnoreCase(ParserFileType), "METADATALOGFILE getNextRecordStartPattern file type is unexpected");
     *                  switch(nextExpectedRecordType)
     *                  {
     *                      case "HEADER": {
     *                          // the header record starts with a new line followed by the Record-Type: header line
     *                          return new RecordParseHint(RecordHintType.PATTERN, "\r\nRecord-Type: metadata", -1);
     *                      }
     *                      case "DATA": {
     *                          // the data record starts with a new line followed by the Record-Type: data line
     *                          return new RecordParseHint(RecordHintType.PATTERN, "\r\nRecord-Type: data", -1);
     *                      }
     *                      default: {
     *                          throw new RuntimeException("unexpected record type");
     *                      }
     *                  }
     *              }
     *
     * @param s3FileType - the filetype
     * @param nextExpectedRecordType - the nextExpectedRecordType in the state machine
     * @param lastProcessedDoc - the lastProcessedDoc
     * @return - the record start pattern as a RecordParseHint object
     */
    RecordParseHint getNextRecordStartPattern(String s3FileType, String nextExpectedRecordType, DocumentInterface lastProcessedDoc);

    /**
     /*
     * Given the fileType, next expected record type and the last processed doc, this function returns the end delimiter for the next record
     * To extract the records from the file, the parser needs to know the expected record type for the state machine and the record type start and end delimiters - it will search the file sequentially till it finds this delimiter and then from that point on, it will search for the end record delimiter.
     *
     * For example, for the following {metadata, data} records in the data file, we define the start patterns:
     *
     *     metadata record start pattern: \r\nRecord-Type: metadata
     *     metadata record end pattern: \r\nRecord-Type: data
     *
     *     data record start pattern: \r\nRecord-Type: data
     *     data record end pattern: \r\nRecord-Type: metadata
     *
     *     Logfile:
     *     --------
     *
     *     Record-Type: metadata
     *     Content-Language: en-us
     *     Content-Length: 1024
     *     Content-Encoding: gzip
     *
     *
     *     Record-Type: data
     *     Content: eJxtUTFuwzAM3POKg8ci7QP6gK5dOmZRZbomKpOuxMAJiv69lBInRlEDAiweeXc8AdfvAW8KOlkO0WAjIVPU3BcMWadWGDjRvv3NIRfKECLHTfEpumxmUCxkQ0+JJzZvfAQbFk4JhUKO443O719HEuOQ0hlWO7xzYKm8I5cNR5C+jsnqJxhmZTGo7P/SD5qbBPnM1dKN6AmvEmkr83/fnTTq7N5GLYT3s1Hb+JYCeo3HyVfAcJRorFLhmCkYuec1UVoFdrgH/uI26RSmuea6eh40JV1YPpDUD4sLsjSoFi6PsLguuX9q9UvcczC3LQgF3fehs3LoupZa3W4L/hyke979Ar2qsBI=

     *
     *     @Override
     *     public RecordParseHint getNextRecordEndPattern(String s3FileType, String nextExpectedRecordType, DocumentInterface lastProcessedDoc) {
     *         ValidationUtils.validateAssertCondition(s3FileType != null && s3FileType.equalsIgnoreCase(ParserFileType), "DATAFILE getNextRecordEndPattern file type is unexpected");
     *         switch(nextExpectedRecordType)
     *         {
     *             case "HEADER": {
     *                 // the data record start is the end of the header record
     *                 return new RecordParseHint(RecordHintType.PATTERN, "\r\nRecord-Type: data", -1);
     *             }
     *             case "DATA": {
     *                 // the header record start is the end of the data record
     *                 return new RecordParseHint(RecordHintType.PATTERN, "\r\nRecord-Type: metadata", -1);
     *             }
     *             default: {
     *                 throw new RuntimeException("unexpected record type");
     *             }
     *         }
     *     }
     *
     * @param s3FileType - the filetype
     * @param nextExpectedRecordType - the nextExpectedRecordType in the state machine
     * @param lastProcessedDoc - the lastProcessedDoc
     * @return - the record end pattern as a RecordParseHint object
     */
    RecordParseHint getNextRecordEndPattern(String s3FileType, String nextExpectedRecordType, DocumentInterface lastProcessedDoc);

    /**
     * Given the fileType and the last processed record type, find the next expected record type from the state machine
     * This method encodes the file's state machine. From the METADATALOGFILE example above, an example implementation could be as follows:
     *                @Override
     *                public String getNextExpectedRecordType(String s3FileType, String lastProcessedRecordType) {
     *                    ValidationUtils.validateAssertCondition(s3FileType != null && s3FileType.equalsIgnoreCase(ParserFileType), "DATAFILE getNextRecordStartPattern file type is unexpected");
     *                    if (lastProcessedRecordType == null) {
     *                        return "METADATA";
     *                    } else {
     *                        if (lastProcessedRecordType.equals("METADATA")) {
     *                            return "DATA";
     *                        } else if (lastProcessedRecordType.equals("DATA")) {
     *                            return "METADATA";
     *                        } else {
     *                            throw new RuntimeException("Unexpected lastProcessedRecordType "+lastProcessedRecordType);
     *                        }
     *                    }
     *                }
     * @param s3FileType - the filetype
     * @param lastProcessedRecordType - the lastProcessedRecordType in the state machine
     * @return - the nextExpectedRecordType in the state machine
     */
    String getNextExpectedRecordType(String s3FileType, String lastProcessedRecordType);

    /**
     * This function is called with each record's contents in a byteArr
     * The startIndex and endIndex into the byteArr as the start and end of the log line record.
     * The implementer is expected to construct the output record from these bytes.
     *
     * Here is an example implementation which parses error records from the DATALOGFILE example:
     *        public ParseDocumentResult parseDocument(String s3FileType, String s3Filename, long offsetBytes, String lastProcessedRecordType, DocumentInterface lastProcessedDoc, byte[] byteArr, int startIndex, int endIndex) {
     *            ValidationUtils.validateAssertCondition(s3FileType != null && s3FileType.equalsIgnoreCase(getS3FileType()), "parseDocument file type is unexpected");
     *            ValidationUtils.validateAssertCondition(byteArr != null && startIndex >= 0 && byteArr.length > endIndex && endIndex > startIndex, "parseDocument byte array offsets are invalid");
     *            String nextRecordType = getNextExpectedRecordType(s3FileType, lastProcessedRecordType);
     *            ValidationUtils.validateAssertCondition(nextRecordType  != null && (nextRecordType.equalsIgnoreCase("DATA") || nextRecordType.equalsIgnoreCase("METADATA")), "nextRecordType should not be null or invalid");
     *
     *            AbstractDataRecord record = null;
     *            switch (nextRecordType.toUpperCase()) {
     *                case "METADATA": {
     *                    try {
     *                        record = DataRecordFactory.constructMetadataRecord(byteArr, startIndex, endIndex);
     *                    } catch (Exception ex) {
     *                        logger.error("Exception in parseDocument for METADATA record - recordType: "+nextRecordType+", ex: "+exception);
     *
     *                        String nextRecordTypeForExtractedRecord = getNextExpectedRecordType(s3FileType, nextRecordType);
     *                        Map&lt;String, String&gt; startOffset = new HashMap&lt;&gt;();
     *                        startOffset.put(s3FileType, Long.toString(offsetBytes));
     *                        Map&lt;String, String&gt; endOffset = new HashMap&lt;&gt;();
     *                        endOffset.put(s3FileType, Long.toString(offsetBytes+(endIndex-startIndex)));
     *                        String errorMessage = "Exception in parseDocument for METADATA record - recordType: "+nextRecordType+", ex: "+exception;
     *                        String documentId = null;
     *                        String recordType = null;
     *                        Map&lt;String, Object&gt; documentMetadata = null;
     *                        String serialize = null;
     *                        String partitionKey = s3Filename;
     *                        DocumentInterface errorDoc = new ErrorDoc(startOffset, endOffset, errorMessage, documentId, recordType, documentMetadata, serialize, partitionKey);
     *                        return new ParseDocumentResult(nextRecordTypeForExtractedRecord, errorDoc, ParseDocumentResultStatus.ERROR);
     *                    }
     *                    break;
     *                }
     *               case "DATA": {
     *                    try {
     *                        record = DataRecordFactory.constructDataRecord(byteArr, startIndex, endIndex);
     *                    } catch (Exception ex) {
     *                        logger.error("Exception in parseDocument for DATA record - recordType: "+nextRecordType+", ex: "+exception);
     *
     *                        String nextRecordTypeForExtractedRecord = getNextExpectedRecordType(s3FileType, nextRecordType);
     *                        Map&lt;String, String&gt; startOffset = new HashMap&lt;&gt;();
     *                        startOffset.put(s3FileType, Long.toString(offsetBytes));
     *                        Map&lt;String, String&gt; endOffset = new HashMap&lt;&gt;();
     *                        endOffset.put(s3FileType, Long.toString(offsetBytes+(endIndex-startIndex)));
     *                        String errorMessage = "Exception in parseDocument for DATA record - recordType: "+nextRecordType+", ex: "+exception;
     *                        String documentId = null;
     *                        String recordType = null;
     *                        Map&lt;String, Object&gt; documentMetadata = null;
     *                        String serialize = null;
     *                        String partitionKey = s3Filename;
     *                        DocumentInterface errorDoc = new ErrorDoc(startOffset, endOffset, errorMessage, documentId, recordType, documentMetadata, serialize, partitionKey);
     *                        return new ParseDocumentResult(nextRecordTypeForExtractedRecord, errorDoc, ParseDocumentResultStatus.ERROR);
     *                    }
     *                    break;
     *                }
     *                default: {
     *                    logger.error("Unexpected record type in datafile - recordType: "+nextRecordType);
     *                    throw new RuntimeException("Unexpected record type in datafile - recordType: "+nextRecordType);
     *                }
     *            }
     *            String nextRecordTypeForExtractedRecord = getNextExpectedRecordType(s3FileType, nextRecordType);
     *            return new ParseDocumentResult(nextRecordTypeForExtractedRecord, record, ParseDocumentResultStatus.SUCCESS);
     *        }
     *
     * @param s3FileType - the filetype
     * @param s3Filename - the filename
     * @param offsetBytes - the offset bytes into the file
     * @param lastProcessedRecordType - the last processed record type
     * @param lastProcessedDoc - the last processed doc
     * @param byteArr - the byteArr that has the contents of the record
     * @param startIndex - the start index of the record in the byteArr
     * @param endIndex - the end index of the record in the byteArr
     * @return - ParseDocumentResult which has the extracted record and the status (error, success or skip)
     */
    ParseDocumentResult parseDocument(String s3FileType, String s3Filename, long offsetBytes, String lastProcessedRecordType, DocumentInterface lastProcessedDoc, byte[] byteArr, int startIndex, int endIndex);
}
