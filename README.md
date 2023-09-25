# letsdata-data-interface
The #Let'sData infrastructure simplifies the data processing for the customers. Our promise to the customers is that "Focus on the data - we'll manage the infrastructure".

The #Let'sData infrastructure implements a control plane for the data tasks, reads and writes to different destinations, scales the processing infrastructure as needed, builds in monitoring and instrumentation. 

However, it does not know what the data is, how it is stored in the data files and how to read the data and create transformed documents. This is where the #Let'sData infrastructure requires the customer code.
The customer needs to implement user data handlers that tell us what makes a data record in the files - we'll then send the records to these data handlers where the user can transform the records into documents that can be sent to the write destination. This requires implementation of the user data handler interfaces. 

This package defines these user data handler interfaces. 

(You can also view the # Let's Data docs to see how to implement different usecases, examples and more: https://www.letsdata.io/docs)

## Documents
#Let's Data defines the following data model class / interfaces for the transformed document implementations that are required in data processing. These are defined in the namespace `com.resonance.letsdata.data.documents.interfaces` and defined as follows:

### `DocumentInterface`
The `com.resonance.letsdata.data.documents.interfaces.DocumentInterface` is the base interface for any document that can be returned by the user handlers. All other document interfaces and documents either extend or implement this interface.
### `SingleDocInterface`
The `com.resonance.letsdata.data.documents.interfaces.SingleDocInterface` extends the "DocumentInterface" is the base interface for any documents that are transformed from single records and are returned by the user handlers. The java doc on the interface below explain these in detail.
### `CompositeDocInterface` 
The `com.resonance.letsdata.data.documents.interfaces.CompositeDocInterface` extends the "DocumentInterface" is the base interface for any documents that are composited from multiple single docs and are returned by the user handlers. The java doc on the interface below explain these in detail.
### `ErrorDocInterface`
The `com.resonance.letsdata.data.documents.interfaces.ErrorDocInterface` extends the "DocumentInterface" is the base interface for any error documents that are returned by the user handlers. A default implementation for the interface is provided at `com.resonance.letsdata.data.documents.implementation.ErrorDoc` which is used by default. Customers can return errors from handlers using this default implementation or write their own Error docs and return these during processing.
### `SkipDocInterface`
The `com.resonance.letsdata.data.documents.interfaces.SkipDocInterface` extends the "DocumentInterface" is the base interface for any skip documents that are returned by the user handlers. A skip document is returned when the processor determines that the record from the file is not of interest to the current processor and should be skipped from being written to the write destination. A default implementation for the interface is provided at `com.resonance.letsdata.data.documents.implementation.SkipDoc` which is used by default. Customers can return skip records from handlers using this default implementation or write their own Skip docs and return these during processing.
## Parser / Reader Interfaces
### `Parsers - SingleFileParser`
The parser interface for "Single File" reader usecase. This is where you tell us how to parse the individual records from the file. Since this is single file reader, there is no state machine maintained.
### `Parsers - SingleFileStateMachineParser`
The parser interface for "Single File State Machine" reader usecase. This is where you tell us how to parse the different records from a file. This class maintains the overall state machine for the file parser. It will create the extracted document from different file records that are being read from the files.

### `Readers - SingleFileStateMachineReader`
The SingleFileStateMachineReader implements the logic to combine the individual records parsed by the SingleFileStateMachine parser and output them to a composite doc. For example, if we have a DATAFILE which contains 2 types of records {metadata record, data record} and the output doc is constructed by the combining these two docs, then the SingleFileStateMachineReader combines each {metadata, data} record pair into an output doc.

### `MultipleFileStateMachineReader`
The reader interface for "Multiple File State Machine" reader. This is where you tell us how to make sense of the individual records that are parsed from multiple files. This class would maintain the overall state machine across the files. It will create the extracted document from different file records that are being read from the files.

## Usecases - Implementation Requirements 
The #Let'sData implementation defines 2 parser interfaces and 2 reader interface for the different supported use cases (Single File Reader, Single File State Machine Reader, Multiple File State Machine Reader). In the simplest case, the Single File Reader, you'll need to implement only 1 parser interface. In the most complicated case, the Multiple File State Machine Reader, you'll need to implement parser interfaces (1 for each file type) and a Reader interface that can combine the parsed records from multiple files into a single, composite output document. The details about each interface are defined later, but before we look at the individual interfaces, lets look at these different usecases and the interfaces that need to be implemented for each usecase.

### Single File Reader
You'll need to implement the `com.resonance.letsdata.data.readers.interfaces.parsers.SingleFileParser`

The Single File Reader usecase, as explained earlier, is when all the files are of a single type and the records in the file do not follow a state machine. Simple example is a log file where each line is a data record and the extracted document is transformed from each data record (line in the file).
```
    Example: Single File Reader Files and Record Layout:

    +----------------------+------------------=---+---------------------+
    | logfile_1.gz         | logfile_2.gz         | logfile_3.gz        |
    +---=------------------+----------------------+---------------------+
    | <logline_1>          | <logline_1>          | <logline_1>         |
    | <logline_2>          | <logline_2>          | <logline_2>         |
    | <logline_3>          | <logline_3>          | <logline_3>         |
    | <logline_4>          | <logline_4>          | <logline_4>         |
    | ...                  | ...                  | ...                 |
    +----------------------+----------------------+---------------------+
```
In this simple example, you'll only need to implement the 'SingleFileParser' interface. Here is a quick look at the interface methods, the implementation has detailed comments on what each method does and how to implement it:

* getS3FileType(): The logical name of the filetype defined in manifest
* getResolvedS3FileName():The filename resolved from the manifest name
* getRecordStartPattern():The start pattern / delimiter of the record
* getRecordEndPattern():The end pattern / delimiter of the record
* parseDocument():The logic to skip, error or return the parsed document

Here is an example implementation:
```
    Example SingleFileParser Interface Implementation:

    public class LogFileParserImpl implements SingleFileReaderParserInterface {
        /**
        * The filetype of the file - for the examples we've used, we define the filetype (logical name) as "LOGFILE"
        * @return - The file type
        */
        public String getS3FileType() {
            return "LOGFILE"
        }

        /**
        * Given the filetype (LOGLINE) and filename (logfile_1.gz/logfile_2.gz/logfile_3.gz) from the manifest file, return the resolved filename if necessary.
        * In most cases, the resolved filename would be the same as the filename, but in some cases, you might need to prepend paths if the data in the s3 bucket is not in the root directory.
        * For example, dataset_name/log_date/logfile_1.gz (See the docs for MultipleFileStateMachineReader.getResolvedS3Filename method for additional details around the file name resolution)
        * @param s3FileType - the file type
        * @param fileName - the file name
        * @return - the resolved file name
        */
        public String getResolvedS3FileName(String s3FileType, String fileName) {
            ValidationUtils.validateAssertCondition(getS3FileType().equals(fileType.toUpperCase()), "fileType is unexpected");
            ValidationUtils.validateAssertCondition(fileName != null, "filename should not be null");
            // no resolution being done, expect incoming file names to be resolved but custom logic can be put here
            return resolvedFileNames;
        }

         /*
         * getRecordStartPattern/getRecordEndPattern interface methods:
         * To extract the records from the file, the parser needs to know the record start delimiter - it will search the file sequentially till it finds this delimiter and then from that point on, it will search for the end record delimiter. Once it finds the end record delimiter, it will copy those bytes to the parse document function to create an extracted record
         * For example, for the following log lines in the log file, we define the start pattern as "{\"ts\"" and end pattern as "}\n":
         * Logfile:
         * --------
         * {"ts":1647352053448,"dt":"Mar 15, 2022 6:47:33 AM","hnm":"archimedes-mbp-2.hsd1.wa.comcast.net","unm":"archimedes","lvl":"WARN","thd":"main","cnm":"com.ancient.mathematicians.archimedes.InvalidBuoyantForceException","fnm":"InvalidBuoyantForceException.java","lnm":178,"mnm":"validateArchimedesPrinciple","msg":"The buoyant force is different from the weight. Archimedes' principle has been invalidated."}
         * {"ts":1647352053449,"dt":"Mar 15, 2022 6:47:33 AM","hnm":"archimedes-mbp-2.hsd1.wa.comcast.net","unm":"archimedes","lvl":"WARN","thd":"main","cnm":"com.ancient.mathematicians.archimedes.InvalidBuoyantForceException","fnm":"InvalidBuoyantForceException.java","lnm":178,"mnm":"validateArchimedesPrinciple","msg":"The buoyant force is different from the weight. Archimedes' principle has been invalidated."}
         *
         */
        public RecordParseHint getRecordStartPattern(String s3FileType) {
            ValidationUtils.validateAssertCondition(getS3FileType().equals(fileType.toUpperCase()), "fileType is unexpected");
            return new RecordParseHint(RecordHintType.PATTERN, "{\"ts\"", -1);
        }

        public RecordParseHint getRecordEndPattern(String s3FileType) {
            ValidationUtils.validateAssertCondition(getS3FileType().equals(fileType.toUpperCase()), "fileType is unexpected");
            return new RecordParseHint(RecordHintType.PATTERN, "}\n", -1);
        }

       /**
       * This function is called with each log line's contents in a byteArr
       * The startIndex and endIndex into the byteArr as the start and end of the log line record.
       * The implementer is expected to construct the output record from these bytes.
       * In this example implementation, we parse "lvl":"ERROR" log lines from the logfile:
       */
       public ParseDocumentResult parseDocument(String s3FileType, String s3Filename, long offsetBytes, byte[] byteArr, int startIndex, int endIndex) {
           ValidationUtils.validateAssertCondition(s3FileType != null && s3FileType.equalsIgnoreCase(getS3FileType()), "parseDocument file type is unexpected");
           ValidationUtils.validateAssertCondition(byteArr != null && startIndex >= 0 && byteArr.length > endIndex && endIndex > startIndex, "parseDocument byte array offsets are invalid");

           // uses the Matcher utility from the interface package to do pattern matching
           int errorLevelIndex = Matcher.match(byteArr, startIndex, endIndex, "\"lvl\":\"ERROR\"");
           if (errorLevelIndex == -1) {
               // skip since we are interested only in ERROR records
               Map<String, String> startOffset = new HashMap<>();
               startOffset.put(s3FileType, Long.toString(offsetBytes));
               Map<String, String> endOffset = new HashMap<>();
               endOffset.put(s3FileType, Long.toString(offsetBytes+(endIndex-startIndex)));
               String errorMessage = "skipping message - level is not ERROR";
               String documentId = null;
               String recordType = null;
               Map<String, Object> documentMetadata = null;
               String serialize = null;
               String partitionKey = s3Filename;
               String nextRecordType = null;
               DocumentInterface skipDoc = new SkipDoc(startOffset, endOffset, errorMessage, documentId, recordType, documentMetadata, serialize, partitionKey);
               return new ParseDocumentResult(nextRecordType, skipDoc, ParseDocumentResultStatus.SKIP);
           } else {
               try {
                   Map<String, Object> jsonMap = objectMapper.readValue(byteArr, startIndex, endIndex, HashMap.class);
                   Long timestamp = (Long) jsonMap.get("ts");
                   String docId = ""+timestamp+s3Filename;
                   String recordType = (String) jsonMap.get("lvl");
                   DocumentInterface doc = new JsonLogDocument(docId, recordType, null,  objectMapper.writeValueAsString(jsonMap), docId);
                   String nextRecordType = null;
                   return new ParseDocumentResult(nextRecordType, doc, ParseDocumentResultStatus.SUCCESS);
                } catch (Exception e) {
                   logger.error("error processing json document from file", s3FileType, s3Filename, offsetBytes, e);
                   Map<String, String> startOffset = new HashMap<>();
                   startOffset.put(s3FileType, Long.toString(offsetBytes));
                   Map<String, String> endOffset = new HashMap<>();
                   endOffset.put(s3FileType, Long.toString(offsetBytes+(endIndex-startIndex)));
                   String errorMessage = "error processing json document from file - ex: "+e;
                   String documentId = null;
                   String recordType = null;
                   Map<String, Object> documentMetadata = null;
                   String serialize = null;
                   String partitionKey = s3Filename;
                   DocumentInterface errorDoc = new ErrorDoc(startOffset, endOffset, errorMessage, documentId, recordType, documentMetadata, serialize, partitionKey);
                   return new ParseDocumentResult(null, errorDoc, ParseDocumentResultStatus.ERROR);
               }
            }
        }
    }
```
### Single File State Machine Reader
You'll need to implement the `com.resonance.letsdata.data.readers.interfaces.parsers.SingleFileStateMachineParser` interface (to parse individual records from file and maintain a state machine) & `com.resonance.letsdata.data.readers.interfaces.SingleFileStateMachineReader` interface (to output a composite doc from the parsed records)

The Single File State Machine Reader usecase, as explained earlier, is when data document to be extracted is completely contained in a single file and is created from multiple data record in the file. The records in the file follow a finite state machine. Simple example is a data file where a record's metadata and data are written sequentially as two separate records.
```
    Example: Single File State Machine Reader Files and Record Layout:
    +-----------------------+---------------------------+---------------------------+
    | s3file_1.gz           | s3file_2.gz               | s3file_3.gz               |
    +-----------------------+---------------------------+---------------------------+
    | <metadata_record_1>   | <metadata_record_n>       | <metadata_record_m>       |
    | <data_record_1>       | <data_record_n>           | <data_record_m>           |
    | <metadata_record_2>   | <metadata_record_n+1>     | <metadata_record_m+1>     |
    | <data_record_2>       | <data_record_n+1>         | <data_record_m+1>         |
    | ...                   | ...                       | ...                       |
    +-----------------------+---------------------------+---------------------------+
```
The records in each file follow the following state machine - this state machine is encoded in the SingleFileStateMachineParser implementation as it parses the records from the file.
```
    Example:  S3 File State Machine
    +----------+                   +------+
    | Metadata | -------->-------- | Data | -->----+
    +----------+                   +------+        |
    ^                                              V
    |                                              |
    +--------<----------------------<--------------+

    Example code:
    -------------
    public class DataFileParserImpl implements SingleFileReaderParserInterface {
        /**
        * The filetype of the file - for the examples we've used, we define the filetype (logical name) as "DATAFILE"
        */
        public String getS3FileType() {
            return "DATAFILE"
        }

        /**
        * Given the filetype (DATAFILE) and filename (s3file_1.gz/s3file_2.gz/s3file_3.gz) from the manifest file, return the resolved filename if necessary.
        * In most cases, the resolved filename would be the same as the filename, but in some cases, you might need to prepend paths if the data in the s3 bucket is not in the root directory.
        * For example, dataset_name/data_date/s3file_1.gz
        */
        public String getResolvedS3FileName(String s3FileType, String fileName) {
            ValidationUtils.validateAssertCondition(getS3FileType().equals(fileType.toUpperCase()), "fileType is unexpected");
            ValidationUtils.validateAssertCondition(fileName != null, "filename should not be null");
            // no resolution being done, expect incoming file names to be resolved but custom logic can be put here
            return resolvedFileNames;
        }

         /*
         * getNextRecordStartPattern/getNextRecordEndPattern interface methods:
         * To extract the records from the file, the parser needs to know the expected record type for the state machine and the record type start and end delimiters - it will search the file sequentially till it finds this delimiter and then from that point on, it will search for the end record delimiter. Once it finds the end record delimiter, it will copy those bytes to the parse document function to create an extracted record
         * For example, for the following {metadata, data} records in the data file, we define the start patterns
         *                                        metadata record start pattern: \r\nRecord-Type: metadata
         *                                        metadata record end pattern: \r\nRecord-Type: data
         *
         *                                        data record start pattern: \r\nRecord-Type: data
         *                                        data record end pattern: \r\nRecord-Type: metadata
         *
         * Logfile:
         * --------
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
         */
        @Override
        public RecordParseHint getNextRecordStartPattern(String s3FileType, String nextExpectedRecordType, DocumentInterface lastProcessedDoc) {
           ValidationUtils.validateAssertCondition(s3FileType != null && s3FileType.equalsIgnoreCase(ParserFileType), "METADATALOGFILE getNextRecordStartPattern file type is unexpected");
           switch(nextExpectedRecordType)
           {
               case "HEADER": {
                   // the header record starts with a new line followed by the Record-Type: header line
                   return new RecordParseHint(RecordHintType.PATTERN, "\r\nRecord-Type: metadata", -1);
               }
               case "DATA": {
                   // the data record starts with a new line followed by the Record-Type: data line
                   return new RecordParseHint(RecordHintType.PATTERN, "\r\nRecord-Type: data", -1);
               }
               default: {
                   throw new RuntimeException("unexpected record type");
               }
           }
       }

        @Override
        public RecordParseHint getNextRecordEndPattern(String s3FileType, String nextExpectedRecordType, DocumentInterface lastProcessedDoc) {
           ValidationUtils.validateAssertCondition(s3FileType != null && s3FileType.equalsIgnoreCase(ParserFileType), "METADATALOGFILE getNextRecordStartPattern file type is unexpected");
           switch(nextExpectedRecordType)
           {
               case "HEADER": {
                    // the data record start is the end of the header record
                   return new RecordParseHint(RecordHintType.PATTERN, "\r\nRecord-Type: data", -1);
               }
               case "DATA": {
                   // the header record start is the end of the data record
                   return new RecordParseHint(RecordHintType.PATTERN, "\r\nRecord-Type: metadata", -1);
               }
               default: {
                   throw new RuntimeException("unexpected record type");
               }
           }
       }

       /**
        * Given the fileType and the last processed record type, find the next expected record type from the state machine
        * This method encodes the file's state machine. From the DATALOGFILE example above, an example implementation could be as follows:
        */
        @Override
        public String getNextExpectedRecordType(String s3FileType, String lastProcessedRecordType) {
            ValidationUtils.validateAssertCondition(s3FileType != null && s3FileType.equalsIgnoreCase(ParserFileType), "DATAFILE getNextRecordStartPattern file type is unexpected");

            if (lastProcessedRecordType == null) {
                return "METADATA";
            } else {
                if (lastProcessedRecordType.equals("METADATA")) {
                    return "DATA";
                } else if (lastProcessedRecordType.equals("DATA")) {
                    return "METADATA";
                } else {
                    throw new RuntimeException("Unexpected lastProcessedRecordType "+lastProcessedRecordType);
                }
            }
        }

       /**
       * This function is called with each record's contents in a byteArr
       * The startIndex and endIndex into the byteArr as the start and end of the log line record.
       * The implementer is expected to construct the output record from these bytes.
       * Here is an example implementation which parses error records from the DATALOGFILE example:
       */
       public ParseDocumentResult parseDocument(String s3FileType, String s3Filename, long offsetBytes, String lastProcessedRecordType, DocumentInterface lastProcessedDoc, byte[] byteArr, int startIndex, int endIndex) {
            ValidationUtils.validateAssertCondition(s3FileType != null && s3FileType.equalsIgnoreCase(getS3FileType()), "parseDocument file type is unexpected");
            ValidationUtils.validateAssertCondition(byteArr != null && startIndex >= 0 && byteArr.length > endIndex && endIndex > startIndex, "parseDocument byte array offsets are invalid");
            String nextRecordType = getNextExpectedRecordType(s3FileType, lastProcessedRecordType);
            ValidationUtils.validateAssertCondition(nextRecordType  != null && (nextRecordType.equalsIgnoreCase("DATA") || nextRecordType.equalsIgnoreCase("METADATA")), "nextRecordType should not be null or invalid");

                AbstractDataRecord record = null;
                switch (nextRecordType.toUpperCase()) {
                    case "METADATA": {
                        try {
                            record = DataRecordFactory.constructMetadataRecord(byteArr, startIndex, endIndex);
                        } catch (Exception ex) {
                            logger.error("Exception in parseDocument for METADATA record - recordType: "+nextRecordType+", ex: "+exception);

                            String nextRecordTypeForExtractedRecord = getNextExpectedRecordType(s3FileType, nextRecordType);
                            Map<String, String> startOffset = new HashMap<>();
                            startOffset.put(s3FileType, Long.toString(offsetBytes));
                            Map<String, String> endOffset = new HashMap<>();
                            endOffset.put(s3FileType, Long.toString(offsetBytes+(endIndex-startIndex)));
                            String errorMessage = "Exception in parseDocument for METADATA record - recordType: "+nextRecordType+", ex: "+exception;
                            String documentId = null;
                            String recordType = null;
                            Map<String, Object> documentMetadata = null;
                            String serialize = null;
                            String partitionKey = s3Filename;
                            DocumentInterface errorDoc = new ErrorDoc(startOffset, endOffset, errorMessage, documentId, recordType, documentMetadata, serialize, partitionKey);
                            return new ParseDocumentResult(nextRecordTypeForExtractedRecord, errorDoc, ParseDocumentResultStatus.ERROR);
                        }
                        break;
                    }
                   case "DATA": {
                        try {
                            record = DataRecordFactory.constructDataRecord(byteArr, startIndex, endIndex);
                        } catch (Exception ex) {
                            logger.error("Exception in parseDocument for DATA record - recordType: "+nextRecordType+", ex: "+exception);

                            String nextRecordTypeForExtractedRecord = getNextExpectedRecordType(s3FileType, nextRecordType);
                            Map<String, String> startOffset = new HashMap<>();
                            startOffset.put(s3FileType, Long.toString(offsetBytes));
                            Map<String, String> endOffset = new HashMap<>();
                            endOffset.put(s3FileType, Long.toString(offsetBytes+(endIndex-startIndex)));
                            String errorMessage = "Exception in parseDocument for DATA record - recordType: "+nextRecordType+", ex: "+exception;
                            String documentId = null;
                            String recordType = null;
                            Map<String, Object> documentMetadata = null;
                            String serialize = null;
                            String partitionKey = s3Filename;
                            DocumentInterface errorDoc = new ErrorDoc(startOffset, endOffset, errorMessage, documentId, recordType, documentMetadata, serialize, partitionKey);
                            return new ParseDocumentResult(nextRecordTypeForExtractedRecord, errorDoc, ParseDocumentResultStatus.ERROR);
                        }
                        break;
                    }
                    default: {
                        logger.error("Unexpected record type in datafile - recordType: "+nextRecordType);
                        throw new RuntimeException("Unexpected record type in datafile - recordType: "+nextRecordType);
                    }
                }

                String nextRecordTypeForExtractedRecord = getNextExpectedRecordType(s3FileType, nextRecordType);
                return new ParseDocumentResult(nextRecordTypeForExtractedRecord, record, ParseDocumentResultStatus.SUCCESS);

        }
    }
```
The SingleFileStateMachineReader implements the logic to combine each {metadata, data} record pair into an output doc.
```
                    Example: Output Doc

                    (Diagram 1)
    +-----------------------+ SingleFileStateMachineParser   +-----------------+
    | s3file_1.gz           |-------------->-----------------| metadata_record | -------->---------+
    +-----------------------+                                +-----------------+                   | SingleFileStateMachineReader     +---------------+
    | <metadata_record_1>   |                                                                      |-------------->--------------->---| composite_doc |
    | <data_record_1>       | SingleFileStateMachineParser   +-------------+                       |                                  +---------------+
    | <metadata_record_2>   |-------------->-----------------| data_record | -------->-------------+
    | <data_record_2>       |                                +-------------+
    | ...                   |
    +-----------------------+

                    (Diagram 2)
                            +--------------------------------+
                            |                                |
     ---> parseDocument-->--|                                |                       +--------------------+                       +-------------------------+
                            |                                |                       |XXXXXXXXXXXXXXXXXXXX|                       |                         |
                            |                                |---> nextRecord --->---|XXXXXXXXXXXXXXXXXXXX|---> parseDocument-->--|                         |
                            | SingleFileStateMachineReader   |<--- metadata rec --<--|XXXXXXXXXXXXXXXXXXXX|<--- metadata rec --<--|                         |
                            |                                |                       | System File Reader |                       | SingleFileStateMachine  |
                            |                                |---> nextRecord --->---|XXXXXXXXXXXXXXXXXXXX|---> parseDocument-->--|       Parser            |
                            |   { metadata + data }          |<----- data rec ----<--|XXXXXXXXXXXXXXXXXXXX|<----- data rec ----<--|                         |
     -<-- output doc---<----|        = output doc            |                       |XXXXXXXXXXXXXXXXXXXX|                       |                         |
                            |                                |                       +--------------------+                       +-------------------------+
                            |                                |                                |   |
                            +--------------------------------+                                |   |
                                                                                              |   |    File read
                                                                                             \|   |/
                                                                                              \   /
      Legend:                                                                                  \ /
      +--+                                                                         +-----------------------+
      |  |   Customer Implements Interface                                         | s3file_1.gz           |
      +--+                                                                         +-----------------------+
                                                                                   | {metadata_record_1}   |
      +--+                                                                         | {data_record_1}       |
      |XX|   System Implementation                                                 | {metadata_record_2}   |
      +--+                                                                         | {data_record_2}       |
                                                                                   | ...                   |
                                                                                   +-----------------------+
                                                            
    Example code:
    -------------

    public class DataFileParserImpl implements SingleFileReaderParserInterface {
        /**
        * The filetype of the file - for the examples we've used, we define the filetype (logical name) as "DATAFILE"
        */
        public String getS3FileType() {
            return "DATAFILE"
        }

        /**
        * Given the filetype (DATAFILE) and filename (s3file_1.gz/s3file_2.gz/s3file_3.gz) from the manifest file, return the resolved filename if necessary.
        * In most cases, the resolved filename would be the same as the filename, but in some cases, you might need to prepend paths if the data in the s3 bucket is not in the root directory.
        * For example, dataset_name/data_date/s3file_1.gz
        */
        public String getResolvedS3FileName(String s3FileType, String fileName) {
            ValidationUtils.validateAssertCondition(getS3FileType().equals(fileType.toUpperCase()), "fileType is unexpected");
            ValidationUtils.validateAssertCondition(fileName != null, "filename should not be null");
            // no resolution being done, expect incoming file names to be resolved but custom logic can be put here
            return resolvedFileNames;
        }

       /**
        * getReaderParserInterfacesForS3FileType;
        * This function is responsible for creating the parser that are needed to parse the file that is being read by the Single File State Machine reader
        * For example, the input filetype is {DATAFILE}, the output is the filetype's corresponding SingleFileStateMachineParser
        * The function will create the parsers for the filetype (and should cache these in implementation as well).
        * The parser implementation that it returns would be the user implementations on how to parse the file types.
        * We'll reuse the parser class we've defined above in this example (DataFileParserImpl)
        * Here is an example implementation:
        */
        private SingleFileStateMachineParser fileParser;       // class member - cached for reuse

        @Override
        public SingleFileStateMachineParser getReaderParserInterfacesForS3FileType(String fileType) {
            ValidationUtils.validateAssertCondition(getS3FileType().equals(fileType.toUpperCase()), "fileType is unexpected");
            if (fileParser == null) {
                fileParser = new DataFileParserImpl();
            }
            return fileParser;
        }

        /**
        * Given the fileType and the last processed record type, find the next expected record type from the state machine
        * Defer to the file parser's getNextExpectedRecordType method or add custom logic
        */
        @Override
        public String getNextExpectedRecordType(String s3FileType, String lastProcessedRecordType) {
            ValidationUtils.validateAssertCondition(getS3FileType().equals(fileType.toUpperCase()), "fileType is unexpected");
            return this.getReaderParserInterfacesForS3FileType(fileType).getNextExpectedRecordType(s3FileType, lastProcessedRecordType);
        }

        /**
        * ParseCompositeDocumentResult parseDocument(String s3FileType, String lastProcessedRecordType, DocumentInterface lastProcessedDoc, SystemFileReader fileReader);
        *      This function is responsible for returning the extracted document - it is given:
        *          * the current state in the datafile which it can use to determine next state
        *          * the last document that was extracted in case the documents have some link with each other in the file
        *          * the filetype reader interface - that it can use to get the next records from the  datafile - this interface is a simple wrapper over the parser interface that we defined earlier
        *      With these inputs, this function will construct the extracted document from the records obtained from the file reader and return the result.
        *      Note that the document being created is a CompositeDocument - since its being created by combining different records from different files.
        */
        @Override
        public ParseCompositeDocumentResult parseDocument(String s3FileType, String lastProcessedRecordType, DocumentInterface lastProcessedDoc, SystemFileReader fileReader) {
            ValidationUtils.validateAssertCondition(getS3FileType().equals(fileType.toUpperCase()), "fileType is unexpected");
            ValidationUtils.validateAssertCondition(fileReader.getFileTypeFileNameMap().get(s3FileType) != null, "fileReader fileTypeFileNameMap is unexpected");

            if (fileReader.getOffset().get(s3FileType) == 0) {
                // optional: skip if there are any file headers that need not be processed
                skipFileHeaders(fileReader);
            }

            DocumentInterface document = fileReader.nextRecord(peek: false);
            if (document == null) {
                // End of file
                ValidationUtils.validateAssertCondition(fileReader.getState() == SingleFileReaderState.COMPLETED, "fileReader state should be completed when nextRecord is null", fileReader);
                Map<String, String> s3FileTypeNextRecordTypeMap = new HashMap<>();
                s3FileTypeNextRecordTypeMap.put(s3FileType, null);

                Map<String, String> s3FileTypeOffsetMap = new HashMap<>();
                s3FileTypeOffsetMap.putAll(fileReader.getOffset());

                Map<String, String> lastProcessedRecordTypeMap = fileReader.getLastRecordTypeMap();

                Map<String, String> s3FileTypeNextRecordTypeMap = null;
                CompositeDocInterface resultDocument = null;
                return new ParseCompositeDocumentResult(s3FileTypeNextRecordTypeMap, resultDocument, lastProcessedRecordTypeMap, s3FileTypeOffsetMap, SingleFileReaderState.COMPLETED, ParseDocumentResultStatus.SUCCESS);
            }

            // get metatdata record
            MetadataRecord metadataDocument = null;
            ErrorDocInterface metadataError = null;
            if (document instanceof MetadataRecord) {
                metadataDocument = (MetadataRecord) document;
            } else if (document instanceof ErrorDocInterface) {
                metadataError = (ErrorDocInterface) document;
            } else {
                throw new RuntimeException("parseDocument metadata nextRecord is of unexpected type");
            }


            // get data record
            document = fileReader.nextRecord(peek: false);
            ValidationUtils.validateAssertCondition(document != null, "data document should not be null");

            DataRecord dataDocument = null;
            ErrorDocInterface dataError = null;
            if (document instanceof DataRecord) {
                dataDocument = (DataRecord) document;
            } else if (document instanceof ErrorDocInterface) {
                dataError = (ErrorDocInterface) document;
            } else {
                throw new RuntimeException("parseDocument data nextRecord is of unexpected type");
            }

            if (metadataError != null || dataError != null) {
                // Records have errors, construct and return error document

                Map<String, String> lastProcessedRecordTypeMap = fileReader.getLastRecordTypeMap();
                Map<String, String> nextRecordTypeMap = fileReader.getNextExpectedRecordType(s3FileType, lastProcessedRecordTypeMap.get(s3FileType));

                Map<String, String> s3FileTypeOffsetMap = new HashMap<>();
                s3FileTypeOffsetMap.putAll(fileReader.getOffset());

                LinkedHashMap<SingleDocInterface, List<ErrorDocInterface>> docMap = new LinkedHashMap<>();
                // add metadata doc and errors
                Listlt;ErrorDocInterface> metadataErrorRecordsList = metadataError == null ? null : Arrays.asList(metadataError);
                docMap.put(metadataDocument == null ? DocFactoryImpl.createNullDoc("metadata") : metadataDocument, metadataErrorRecordsList);

                // add data doc and errors
                Listlt;ErrorDocInterface> dataErrorRecordsList = dataError == null ? null : Arrays.asList(dataError);
                docMap.put(dataDocument == null ? DocFactoryImpl.createNullDoc("data") : dataDocument, dataErrorRecordsList);

                CompositeDocInterface compositeDoc = DocFactoryImpl.createCompositeDoc(docMap);
                return new ParseCompositeDocumentResult(nextRecordTypeMap, compositeDoc, lastProcessedRecordTypeMap, s3FileTypeOffsetMap, SingleFileReaderState.PROCESSING, ParseDocumentResultStatus.ERROR);
            }


            Listlt;ErrorDocInterface> errorRecordsList = null;
            LinkedHashMap<SingleDocInterface, List<ErrorDocInterface>> docMap = new LinkedHashMap<>();
            docMap.put(metadataDocument, errorRecordsList);
            docMap.put(dataDocument, errorRecordsList);
            CompositeDocInterface compositeDoc = DocFactoryImpl.createCompositeDoc(docMap);

            Map<String, String> lastProcessedRecordTypeMap = fileReader.getLastRecordTypeMap();
            Map<String, String> nextRecordTypeMap = fileReader.getNextExpectedRecordType(s3FileType, lastProcessedRecordTypeMap.get(s3FileType));

            Map<String, String> s3FileTypeOffsetMap = new HashMap<>();
            s3FileTypeOffsetMap.putAll(fileReader.getOffset());
            return new ParseCompositeDocumentResult(nextRecordTypeMap, compositeDoc, lastProcessedRecordTypeMap, s3FileTypeOffsetMap, SingleFileReaderState.PROCESSING, ParseDocumentResultStatus.SUCCESS);
        }
    }
```

### Multiple File State Machine Reader
You'll need to implement either the `com.resonance.letsdata.data.readers.interfaces.parsers.SingleFileParser` or `com.resonance.letsdata.data.readers.interfaces.parsers.SingleFileStateMachineParser` interface for each file. 

If the records in the file are a single record type and do not follow a state machine, use the SingleFileParser interface. 

If there are multiple record types in the file that follow a state machine, use the SingleFileStateMachineParser interface. 

You'll also need to implement `com.resonance.letsdata.data.readers.interfaces.MultipleFileStateMachineReader` interface - this will combine the records returned by the individual file parsers and produce a Composite document. It will also maintain the state machine across files - you'll be adding logic to get the next records from each file and combining them into the result doc.

The Multiple File State Machine Reader usecase, as explained earlier, is when data document to be extracted is contained across multiple files and the records in the files follow a finite state machine across these files. Simple example is group of two types of data files - The metadata files: [metadata_file1.gz,metadata_file2.gz,...] and the data files: [data_file1.gz, data_file2.gz, ...]. A reader data task is created for each {metadata_file, data_file} pair - this has a MultipleFileStateMachineReader that reads over these files using each file's SingleFileParser interface.
```
    Example: Multiple File State Machine Reader Files and Record Layout:
    +--------------------+--------------------+    +-----------------+-----------------+
    | metadata_file1.gz  | metadata_file2.gz  |    | data_file1.gz   | data_file2.gz   |
    +--------------------+--------------------+    +-----------------+-----------------+
    | <metadata_rec_1>   | <metadata_rec_n>   |    | <data_rec_1>    | <data_rec_n>    |
    | <metadata_rec_2>   | <metadata_rec_n+1> |    | <data_rec_2>    | <data_rec_n+1>  |
    | <metadata_rec_3>   | <metadata_rec_n+2> |    | <data_rec_3>    | <data_rec_n+2>  |
    | <metadata_rec_4>   | <metadata_rec_n+3> |    | <data_rec_4>    | <data_rec_n+3>  |
    | ...                | ...                |    | ...             | ...             |
    +--------------------+--------------------+    +-----------------+-----------------+
```
The records in these file follow the following state machine - this state machine is encoded in the MultipleFileStateMachineReader implementation as it parses the records from the file.
```
    Example:  S3 File State Machine
    +------------------------------+                   +----------------------+
    | metadata_file:metadata_rec_X | -------->-------- | data_file:data_rec_X | ----->----+
    +------------------------------+                   +----------------------+           |
    ^                                                                                     V
    |                                                                                     |
    +--------<----------------------<--------------<-------------------<------------<-----+
```
Here is a sample MultipleFileStateMachineReader implementation - the SingleFileParser/SingleFileStateMachineParser implementations for each of the files are not provided for brevity - they can be coded using the examples in Single File Reader and Single File State Machine Reader sections.
```
        Example: Output Doc

        (Diagram 1)
        +-----------------------+
        | metadata_file1.gz     |
        +-----------------------+
        | <metadata_record_1>   |
        | <metadata_record_2>   | SingleFileParser               +-----------------+
        | <metadata_record_3>   |-------------->-----------------| metadata_record | -------->---------+
        | <metadata_record_4>   |                                +-----------------+                   |
        | ...                   |                                                                      V
        +-----------------------+                                                                      |
                                                                                                       | SingleFileStateMachineReader     +---------------+
                                                                                                       |-------------->--------------->---| composite_doc |
        +-----------------------+                                                                      |                                  +---------------+
        | data_file_1.gz        |                                                                      ^
        +-----------------------+                                                                      |
        | <data_record_1>       |                                                                      |
        | <data_record_2>       | SingleFileParser               +-------------+                       |
        | <data_record_3>       |-------------->-----------------| data_record | -------->-------------+
        | <data_record_4>       |                                +-------------+
        | ...                   |
        +-----------------------+


        (Diagram 2)
                            +--------------------------------+
                            | MultipleFileStateMachineReader |
     ---> parseDocument-->--|                                |                  +--------------------+                  +-------------------------+
                            |                                |-> nextRecord -->-| System File Reader |-> parseDocument->| SingleFileParser        |
                            |                                |<- metadata rec-<-|XXXXXXXXXXXXXXXXXXXX|<- metadata rec-<-|                         |
                            |                                |                  +--------------------+                  | (MetadataLogFileParser) |
                            |                                |                         | |                              +-------------------------+
                            |                                |                         | | File read
                            |   { metadata + data }          |                        \| |/
                            |        = output doc            |                         \ /
                            |                                |                     +-----------------------+
                            |                                |                     | metadata_file1.gz     |
                            |                                |                     +-----------------------+
                            |                                |                     | {metadata_record_1}   |
     -<-- output doc---<----|                                |                     | {metadata_record_2}   |
                            |                                |                     +-----------------------+
                            |                                |
                            |                                |
                            |                                |                 +--------------------+                  +-------------------------+
                            |                                |-> nextRecord ->-| System File Reader |-> parseDocument->| SingleFileParser        |
                            |                                |<- data rec --<--|XXXXXXXXXXXXXXXXXXXX|<- data rec --<---|                         |
                            +--------------------------------+                 +--------------------+                  |   (DataLogFileParser)   |
                                                                                        | |                            +-------------------------+
      Legend:                                                                           | | File read
      +--+                                                                             \| |/
      |  |   Customer Implements Interface                                              \ /
      +--+                                                                         +-----------------------+
                                                                                   | data_file1.gz         |
      +--+                                                                         +-----------------------+
      |XX|   System Implementation                                                 | {data_record_1}       |
      +--+                                                                         | {data_record_2}       |
                                                                                   | ...                   |
                                                                                   +-----------------------+
                                                                
    Example code:
    -------------
    public class DataRecordReaderImpl {
        /**
        * This method returns a set of all the filetypes that are being read by the Multiple File State Machine reader.
        * For example, if the metadata records are in file metadata_file_X.gz and the data records are in file data_file_X.gz
        * and we've named these filetypes in the dataset creation as METADATALOGFILE and DATALOGFILE respectively, then this function
        * will return a set {METADATALOGFILE, DATALOGFILE}
        */
        private static final Set data_file1 etc
                    resolvedFileNames.put(fileTypeUpperCased, fileTypeFileNameMap.get(fileType));
                }
            }

            return resolvedFileNames;
        }

        /**
        * getParserInterfacesForS3FileTypes
        * This function is responsible for creating the different reader parsers that are needed to parse each file type that is being read by the Multiple File State Machine reader
        *      For example,
        *          The input filetypes are {METADATALOGFILE, DATALOGFILE}
        *          The output is a map of each filetype and its corresponding SingleFileStateMachineParser (Although METADATALOGFILE / DATALOGFILE might have 1 type of record only (no state machine) and might require SingleFileParser instead of SingleFileStateMachineParser, we're keeping the SingleFileStateMachineParser as the return value since single file reader parses can be implemented using a simple state machine with 1 record.)
        *
        *      The function will create / return the parsers for each file (and should cache these in implementation as well). The parser implementations that it returns would be the user implementations on how to parse the file types.
        *
        *      Here is an example implementation:
        */
        private Map<String, SingleFileStateMachineParser> fileParserMap;       // class member - cached for reuse
        @Override
        public Map<String, SingleFileStateMachineParser> getParserInterfacesForS3FileTypes(Set data).
        * In this implementation, we are using the individual file parser's getNextExpectedRecordType method to get the next expected records. (Custom logic can be added here if needed)
        */
        public Map<String, String> getNextExpectedRecordType(Map<String, String> lastRecordTypeMap) {
           Map<String, String> nextExpectedRecordType = null;
           // initial case, nothing has been processed (null map or null types)
           if(lastRecordTypeMap == null) {
               nextExpectedRecordType = new HashMap<>();
               for(String fileType : ExpectedFileTypes){
                  // NOTE: deferring the next record type to the file's parser
                  nextExpectedRecordType.put(fileType, fileParserMap.get(fileType).getNextExpectedRecordType(fileType, null));
               }
               return nextExpectedRecordType;
           } else {
               ValidationUtils.validateAssertCondition(lastRecordTypeMap != null && lastRecordTypeMap.size() == ExpectedFileTypes.size(), "lastRecordTypeMap size should equal expected");
               nextExpectedRecordType = new HashMap<>();
               for(String fileType : lastRecordTypeMap.keySet()){
                   // NOTE: deferring the next record type to the file's parser
                   nextExpectedRecordType.put(fileType, fileParserMap.get(fileType.toUpperCase()).getNextExpectedRecordType(fileType, lastRecordTypeMap.get(fileType)));
               }
               return nextExpectedRecordType;
           }
        }

        /**
        *   parseDocument
        *      This function is responsible for returning the extracted document - it is given:
        *          * the current state in each file which it can use to determine next state
        *          * the last document that was extracted in case the documents have some link with each other in the file
        *          * the filetype reader interfaces - that it can use to get the next records from each file
        *      With these inputs, it will construct the extracted document from the records obtained from each file and return that as the result. Note that the document being created is a CompositeDocument - since its being created by combining different records from different files.
        *
        *      For example, in our example above where there are two file types {METADATALOGFILE, DATALOGFILE} and the records in each are output as follows:
        *
        *          METADATALOGFILE                 DATALOGFILE                  OUTPUT_DOC
        *         -----------------                -------------                ----------
        *         {metadata_rec_1}                 {data_rec_1}                 {composite_doc_1}
        *         {metadata_rec_2}                 {data_rec_2}                 {composite_doc_2}
        *         {metadata_rec_3}                 {data_rec_3}                 {composite_doc_3}
        *         {metadata_rec_4}                 {data_rec_4}                 {composite_doc_4}
        *
        *      Here is what an example implementation would look like:

        @Override
        public ParseCompositeDocumentResult parseDocument(Map<String, String> fileNameFileTypeMap, DocumentInterface lastProcessedDocument, Map<String, SystemFileReader> fileTypeReaderMap) {
            // validate inputs
            ValidationUtils.validateAssertCondition(ExpectedFileTypes.size() == fileNameFileTypeMap.size(), "fileTypeFileNameMap size is unexpected");
            ValidationUtils.validateAssertCondition(ExpectedFileTypes.size() == fileTypeReaderMap.size(), "fileTypeReaderMap size is unexpected");
            try {
                // get the file readers for each file
                SingleFileStateMachineReaderInterface metadataLogFileReader = fileTypeReaderMap.get("METADATALOGFILE");
                ValidationUtils.validateAssertCondition(metadataLogFileReader != null, "metadataLogFileReader is null");
                SingleFileStateMachineReaderInterface dataLogFileReader = fileTypeReaderMap.get("DATALOGFILE");
                ValidationUtils.validateAssertCondition(dataLogFileReader != null, "dataLogFileReader is null");

                DocumentInterface metadataDocument = metadataLogFileReader.nextRecord(peek: false);
                DocumentInterface dataDocument = dataLogFileReader.nextRecord(peek: false);

                if (metadataDocument == null && dataDocument == null) {
                    // End of file
                    ValidationUtils.validateAssertCondition(metadataLogFileReader.getState() == SingleFileReaderState.COMPLETED, "metadataLogFileReader state should be completed when nextRecord is null", fileReader);
                    ValidationUtils.validateAssertCondition(dataLogFileReader.getState() == SingleFileReaderState.COMPLETED, "dataLogFileReader state should be completed when nextRecord is null", fileReader);

                    Map<String, String> s3FileTypeNextRecordTypeMap = new HashMap<>();
                    s3FileTypeNextRecordTypeMap.put("METADATALOGFILE", null);
                    s3FileTypeNextRecordTypeMap.put("DATALOGFILE", null);

                    Map<String, String> s3FileTypeOffsetMap = new HashMap<>();
                    s3FileTypeOffsetMap.putAll(metadataLogFileReader.getOffset());
                    s3FileTypeOffsetMap.putAll(dataLogFileReader.getOffset());

                    Map<String, String> lastProcessedRecordTypeMap = new HashMap<>();
                    lastProcessedRecordTypeMap.putAll(metadataLogFileReader.getLastRecordTypeMap());
                    lastProcessedRecordTypeMap.putAll(dataLogFileReader.getLastRecordTypeMap());

                    CompositeDocInterface resultDocument = null;
                    return new ParseCompositeDocumentResult(s3FileTypeNextRecordTypeMap, resultDocument, lastProcessedRecordTypeMap, s3FileTypeOffsetMap, SingleFileReaderState.COMPLETED, ParseDocumentResultStatus.SUCCESS);
                } else if (metadataDocument == null || dataDocument == null) {
                    throw new RuntimeException("metadataDocument / dataDocument should both be null or not null");
                }

                // process metatdata record
                MetadataRecord metadataDocument = null;
                ErrorDocInterface metadataError = null;
                if (metadataDocument instanceof MetadataRecord) {
                    metadataDocument = (MetadataRecord) metadataDocument;
                } else if (metadataDocument instanceof ErrorDocInterface) {
                    metadataError = (ErrorDocInterface) metadataDocument;
                } else {
                    throw new RuntimeException("parseDocument metadata nextRecord is of unexpected type");
                }


                // process data record
                DataRecord dataDocument = null;
                ErrorDocInterface dataError = null;
                if (dataDocument instanceof DataRecord) {
                    dataDocument = (DataRecord) dataDocument;
                } else if (dataDocument instanceof ErrorDocInterface) {
                    dataError = (ErrorDocInterface) dataDocument;
                } else {
                    throw new RuntimeException("parseDocument data nextRecord is of unexpected type");
                }

                if (metadataError != null || dataError != null) {
                    // Records have errors, construct and return error document
                    Map<String, String> s3FileTypeNextRecordTypeMap = new HashMap<>();
                    s3FileTypeNextRecordTypeMap.putAll(metadataLogFileReader.getNextExpectedRecordType("METADATALOGFILE", lastProcessedRecordTypeMap.get("METADATALOGFILE")));
                    s3FileTypeNextRecordTypeMap.putAll(dataLogFileReader.getNextExpectedRecordType("DATALOGFILE", lastProcessedRecordTypeMap.get("DATALOGFILE")));

                    Map<String, String> s3FileTypeOffsetMap = new HashMap<>();
                    s3FileTypeOffsetMap.putAll(metadataLogFileReader.getOffset());
                    s3FileTypeOffsetMap.putAll(dataLogFileReader.getOffset());

                    Map<String, String> lastProcessedRecordTypeMap = new HashMap<>();
                    lastProcessedRecordTypeMap.putAll(metadataLogFileReader.getLastRecordTypeMap());
                    lastProcessedRecordTypeMap.putAll(dataLogFileReader.getLastRecordTypeMap());


                    LinkedHashMaplt;SingleDocInterface, Listlt;ErrorDocInterface>> docMap = new LinkedHashMap<>();

                    // add metadata doc and errors
                    Listlt;ErrorDocInterface> metadataErrorRecordsList = metadataError == null ? null : Arrays.asList(metadataError);
                    docMap.put(metadataDocument == null ? DocFactoryImpl.createNullDoc("metadata") : metadataDocument, metadataErrorRecordsList);

                    // add data doc and errors
                    Listlt;ErrorDocInterface> dataErrorRecordsList = dataError == null ? null : Arrays.asList(dataError);
                    docMap.put(dataDocument == null ? DocFactoryImpl.createNullDoc("data") : dataDocument, dataErrorRecordsList);

                    CompositeDocInterface compositeDoc = DocFactoryImpl.createCompositeDoc(docMap);
                    return new ParseCompositeDocumentResult(nextRecordTypeMap, compositeDoc, lastProcessedRecordTypeMap, s3FileTypeOffsetMap, SingleFileReaderState.PROCESSING, ParseDocumentResultStatus.ERROR);
                }


                Listlt;ErrorDocInterface> errorRecordsList = null;
                LinkedHashMaplt;SingleDocInterface, Listlt;ErrorDocInterface>> docMap = new LinkedHashMap<>();
                docMap.put(metadataDocument, errorRecordsList);
                docMap.put(dataDocument, errorRecordsList);
                CompositeDocInterface compositeDoc = DocFactoryImpl.createCompositeDoc(docMap);

                Map<String, String> s3FileTypeNextRecordTypeMap = new HashMap<>();
                s3FileTypeNextRecordTypeMap.putAll(metadataLogFileReader.getNextExpectedRecordType("METADATALOGFILE", lastProcessedRecordTypeMap.get("METADATALOGFILE")));
                s3FileTypeNextRecordTypeMap.putAll(dataLogFileReader.getNextExpectedRecordType("DATALOGFILE", lastProcessedRecordTypeMap.get("DATALOGFILE")));

                Map<String, String> s3FileTypeOffsetMap = new HashMap<>();
                s3FileTypeOffsetMap.putAll(metadataLogFileReader.getOffset());
                s3FileTypeOffsetMap.putAll(dataLogFileReader.getOffset());

                Map<String, String> lastProcessedRecordTypeMap = new HashMap<>();
                lastProcessedRecordTypeMap.putAll(metadataLogFileReader.getLastRecordTypeMap());
                lastProcessedRecordTypeMap.putAll(dataLogFileReader.getLastRecordTypeMap());

                return new ParseCompositeDocumentResult(nextRecordTypeMap, compositeDoc, lastProcessedRecordTypeMap, s3FileTypeOffsetMap, SingleFileReaderState.PROCESSING, ParseDocumentResultStatus.SUCCESS);
            }
            catch (Exception ex) {
                throw ex;
            }
        }
    }
```

## How to build and import this package for dev environments
* Assuming maven is installed and in the PATH - run the command `mvn clean compile assembly:single package` - This produces 3 artifacts: a single jar, a single jar with dependencies and a sources jar
* Install the maven file in the local maven repo
```
mvn -e install:install-file -Dfile=target/letsdata-data-interface-1.0-SNAPSHOT-jar-with-dependencies.jar -Dsources=target/letsdata-data-interface-1.0-SNAPSHOT-sources.jar -DgroupId=com.resonance.letsdata -DartifactId=letsdata-data-interface -Dpackaging=jar -Dversion=1.0-SNAPSHOT
```
* Now import this in your project with the following maven dependency
```
<dependency>
    <groupId>com.resonance.letsdata</groupId>
    <artifactId>letsdata-data-interface</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```
