package com.resonance.letsdata.data.readers.interfaces;

import com.resonance.letsdata.data.documents.interfaces.DocumentInterface;
import com.resonance.letsdata.data.readers.interfaces.parsers.SingleFileStateMachineParser;
import com.resonance.letsdata.data.readers.model.ParseCompositeDocumentResult;

import java.util.Map;
import java.util.Set;

/**
 * The reader interface for "Multiple File State Machine" reader.
 * This is where you tell us how to make sense of the individual records that are parsed from multiple files.
 * This class would maintain the overall state machine across the files.
 * It will create the extracted document from different file records that are being read from the files.
 *
 *  Here are a couple of system architecture diagrams to explain this usecase:
 *       Diagram 1: Component Diagram with data flow
 *                             +--------------------------------+
 *                             | MultipleFileStateMachineReader |
 *      ---> parseDocument-->--|                                |                  +--------------------+                  +-------------------------+
 *                             |                                |-> nextRecord -->-| System File Reader |-> parseDocument->| SingleFileParser        |
 *                             |                                |<- metadata rec-<-|XXXXXXXXXXXXXXXXXXXX|<- metadata rec-<-|                         |
 *                             |                                |                  +--------------------+                  | (MetadataLogFileParser) |
 *                             |                                |                         | |                              +-------------------------+
 *                             |                                |                         | | File read
 *                             |   { metadata + data }          |                        \| |/
 *                             |        = output doc            |                         \ /
 *                             |                                |                     +-----------------------+
 *                             |                                |                     | metadata_file1.gz     |
 *                             |                                |                     +-----------------------+
 *                             |                                |                     | {metadata_record_1}   |
 *      -<-- output doc---<----|                                |                     | {metadata_record_2}   |
 *                             |                                |                     +-----------------------+
 *                             |                                |
 *                             |                                |
 *                             |                                |                 +--------------------+                  +-------------------------+
 *                             |                                |-> nextRecord ->-| System File Reader |-> parseDocument->| SingleFileParser        |
 *                             |                                |<- data rec --<--|XXXXXXXXXXXXXXXXXXXX|<- data rec --<---|                         |
 *                             +--------------------------------+                 +--------------------+                  |   (DataLogFileParser)   |
 *                                                                                         | |                            +-------------------------+
 *       Legend:                                                                           | | File read
 *       +--+                                                                             \| |/
 *       |  |   Customer Implements Interface                                              \ /
 *       +--+                                                                         +-----------------------+
 *                                                                                    | data_file1.gz         |
 *       +--+                                                                         +-----------------------+
 *       |XX|   System Implementation                                                 | {data_record_1}       |
 *       +--+                                                                         | {data_record_2}       |
 *                                                                                    | ...                   |
 *                                                                                    +-----------------------+
 *
 *      Diagram 2: Data flow
 *                     +-----------------------+
 *                     | metadata_file1.gz     |
 *                     +-----------------------+
 *                     | &lt;metadata_record_1&gt;   |
 *                     | &lt;metadata_record_2&gt;   | SingleFileParser               +-----------------+
 *                     | &lt;metadata_record_3&gt;   |-------------->-----------------| metadata_record | -------->---------+
 *                     | &lt;metadata_record_4&gt;   |                                +-----------------+                   |
 *                     | ...                   |                                                                      V
 *                     +-----------------------+                                                                      |
 *                                                                                                                    | SingleFileStateMachineReader     +---------------+
 *                                                                                                                    |-------------->--------------->---| composite_doc |
 *                     +-----------------------+                                                                      |                                  +---------------+
 *                     | data_file_1.gz        |                                                                      ^
 *                     +-----------------------+                                                                      |
 *                     | &lt;data_record_1&gt;       |                                                                      |
 *                     | &lt;data_record_2&gt;       | SingleFileParser               +-------------+                       |
 *                     | &lt;data_record_3&gt;       |-------------->-----------------| data_record | -------->-------------+
 *                     | &lt;data_record_4&gt;       |                                +-------------+
 *                     | ...                   |
 *                     +-----------------------+
 */
public interface MultipleFileStateMachineReader {
    /**
     *  This method returns a set of all the filetypes that are being read by the Multiple File State Machine reader.
     *  For example, if the metadata records are in file metadata_file_X.gz and the data records are in file data_file_X.gz
     *  and we've named these filetypes in the dataset creation as METADATALOGFILE and DATALOGFILE respectively, then this function
     *  will return a set {METADATALOGFILE, DATALOGFILE}
     *  @return a set of file types
     */
    Set<String> getS3FileTypes();

    /**
     *      Input: The log line from the manifest file as a {filetype, filelocation} map
     *      Output: Updated {filetype, filelocation} map - this could be the same as the input or resolved to fix the relative paths if needed.
     *
     *      Given the filetypes (METADATALOGFILE, DATALOGFILE) and filenames (metadata_file1.gz/data_file_1.gz) from the manifest file, return the resolved filename if necessary.
     *      In most cases, the resolved filename would be the same as the filename, but in some cases, you might need to prepend paths if the data in the s3 bucket is not in the root directory.
     *
     *      Here are the details - This method is an alternative to specifying the complete paths in the manifest file.
     *      For example, lets suppose the data is stored in s3 bucket as follows:
     *          S3 Bucket
     *          ---------
     *              June2022/metadata/metadata_file1.gz
     *              June2022/metadata/metadata_file2.gz
     *              June2022/metadata/metadata_file3.gz
     *              June2022/data/data_file1.gz
     *              June2022/data/data_file2.gz
     *              June2022/data/data_file3.gz
     *
     *      We can create the manifest file in one of the following two ways:
     *          Fully Specified manifest file:
     *          -----------------------------
     *              METADATALOGFILE:June2022/metadata/metadata_file1.gz|DATALOGFILE:June2022/data/data_file1.gz
     *              METADATALOGFILE:June2022/metadata/metadata_file2.gz|DATALOGFILE:June2022/data/data_file2.gz
     *              METADATALOGFILE:June2022/metadata/metadata_file3.gz|DATALOGFILE:June2022/data/data_file3.gz
     *
     *              In this case, the getResolvedS3FileNames will validate the file names are correct and the mapping is correct
     *                  * METADATALOGFILE is specified, DATALOGFILE is specified
     *                  * the mapping of metadata file and data file is correct - metadata_file1.gz corresponds to data_file1.gz i.e. file1 ->file1, file2 ->file2 etc.
     *              The returned map would be an echo of the manifest file log line
     *                  {
     *                      METADATALOGFILE:June2022/metadata/metadata_file1.gz
     *                      DATALOGFILE:June2022/data/data_file1.gz
     *                  }
     *
     *          Lazily resolved manifest file:
     *          -------------------------------
     *              METADATALOGFILE:June2022/metadata/metadata_file1.gz|DATALOGFILE:data_file1.gz
     *              METADATALOGFILE:June2022/metadata/metadata_file2.gz|DATALOGFILE:data_file2.gz
     *              METADATALOGFILE:June2022/metadata/metadata_file3.gz|DATALOGFILE:data_file3.gz
     *
     *              In this case, we've fully specified the METADATALOGFILE's location in the S3 Bucket but the DATALOGFILE's name has only been specified.
     *              The getResolvedS3FileNames will resolve the data_fileX.gz's location in the s3 bucket. It knows that the root folder is June2022 and knows
     *              that data files are in a peer folder of the metedata folder and that folder is named data. It will resolve the data file's location and return
     *              the resolved file locations in the map as follows:
     *                  {
     *                      METADATALOGFILE:June2022/metadata/metadata_file1.gz
     *                      DATALOGFILE:June2022/data/data_file1.gz
     *                  }
     *
     *         Here is an example implementation for the "Fully Specified manifest file":
     *              private static final Set&lt;String&gt; EXPECTED_FILE_TYPES = new HashSet&lt;&gt;(Arrays.asList(RecordLogFileTypes.METADATALOGFILE.name(), RecordLogFileTypes.DATALOGFILE.name()));
     *              private final Map&lt;String, String&gt; resolvedFileNames;    // class member - cached for reuse
     *
     *              @Override
     *              public Map&lt;String, String&gt; getResolvedS3FileNames(Map&lt;String, String&gt; fileTypeFileNameMap) {
     *                  ValidationUtils.validateAssertCondition(fileTypeFileNameMap != null && fileTypeFileNameMap.size() == EXPECTED_FILE_TYPES.size(), "fileTypeFileNameMap size should equal expected");
     *                  for (String fileType : fileTypeFileNameMap.keySet())
     *                  {
     *                      String fileTypeUpperCased = fileType.toUpperCase();
     *                      ValidationUtils.validateAssertCondition(EXPECTED_FILE_TYPES.contains(fileTypeUpperCased), "fileType is unexpected");
     *                      if (!resolvedFileNames.containsKey(fileTypeUpperCased)) {
     *                          // no resolution being done, expect incoming file names to be resolved but custom logic can be put here
     *                          // file name mapping can be validated if needed metadata_file1 -> data_file1 etc
     *                          resolvedFileNames.put(fileTypeUpperCased, fileTypeFileNameMap.get(fileType));
     *                      }
     *                  }
     *
     *                  return resolvedFileNames;
     *              }
     * @param s3FileTypeFileNameMap The log line from the manifest file as a {filetype, filelocation} map
     * @return Updated {filetype, filelocation} map - this could be the same as the input or resolved to fix the relative paths if needed.
     */
    Map<String, String> getResolvedS3FileNames(Map<String, String> s3FileTypeFileNameMap);

    /**
     *      This function is responsible for creating the different reader parsers that are needed to parse each file type that is being read by the Multiple File State Machine reader
     *      For example,
     *          The input filetypes are {METADATALOGFILE, DATALOGFILE}
     *          The output is a map of each filetype and its corresponding SingleFileStateMachineParser (Although METADATALOGFILE / DATALOGFILE might have 1 type of record only (no state machine) and might require SingleFileParserInterface instead of SingleFileStateMachineParser, we're keeping the SingleFileStateMachineParser as the return value since single file reader parses can be implemented using a simple state machine with 1 record.)
     *
     *      The function will create / return the parses for each file (and should cache these in implementation as well). The parser implementations that it returns would be the user implementations on how to parse the file types.
     *
     *      Here is an example implementation:
     *
     *      private final Map&lt;String, SingleFileStateMachineParser&gt; fileParserMap;       // class member - cached for reuse
     *
     *      @Override
     *      public Map&lt;String, SingleFileStateMachineParser&gt; getParserInterfacesForS3FileTypes(Set&lt;String&gt; fileTypeSet) {
     *         ValidationUtils.validateAssertCondition(fileTypeSet != null && fileTypeSet.size() == EXPECTED_FILE_TYPES.size(), "fileTypeFileNameMap size should equal expected");
     *         for (String fileType : fileTypeSet)
     *         {
     *             String fileTypeUpperCased = fileType.toUpperCase();
     *             ValidationUtils.validateAssertCondition(EXPECTED_FILE_TYPES.contains(fileTypeUpperCased), "fileType is unexpected");
     *             if (!fileParserMap.containsKey(fileTypeUpperCased)) {
     *                 switch (fileTypeUpperCased)
     *                 {
     *                     case "METADATALOGFILE": {
     *                         fileParserMap.put(fileTypeUpperCased, new MetadataLogFileParser());
     *                         break;
     *                     }
     *                     case "DATALOGFILE": {
     *                         fileParserMap.put(fileTypeUpperCased, new DataLogFileParser());
     *                         break;
     *                     }
     *                     default:{
     *                         throw new RuntimeException("unexpected filetype: "+fileTypeUpperCased);
     *                     }
     *                 }
     *             }
     *         }
     *
     *         return fileParserMap;
     *     }
     * @param s3FileTypes input is a set of filetypes
     * @return The output is a map of each filetype and its corresponding SingleFileStateMachineParser (Although METADATALOGFILE / DATALOGFILE might have 1 type of record only (no state machine) and might require SingleFileParserInterface instead of SingleFileStateMachineParser, we're keeping the SingleFileStateMachineParser as the return value since single file reader parses can be implemented using a simple state machine with 1 record.)
     */
    Map<String /*s3FileType*/, SingleFileStateMachineParser> getParserInterfacesForS3FileTypes(Set<String> s3FileTypes);

    /**
     *      This function is responsible for encoding the state machine across the files and returning the next expected state given the current state.
     *
     *      Input: The input is a map of each file type and the last processed record type in that file.
     *      Output: The output is a map of each file type and the next expected record type in that file.
     *
     *      For example, in our example above where there are two file types {METADATALOGFILE, DATALOGFILE} and the records in each file have a separate header and payload record as follows:
     *
     *          METADATALOGFILE                          DATALOGFILE
     *         -----------------                        -------------
     *         {metadata_headers_rec_1}                 {data_headers_rec_1}
     *         {metadata_payload_rec_1}                 {data_payload_rec_1}
     *         {metadata_headers_rec_2}                 {data_headers_rec_1}
     *         {metadata_payload_rec_2}                 {data_payload_rec_1}
     *
     *      examples input / outputs in this case:
     *          * nothing has been processed, expect the header records
     *              Input: {                                Output: {
     *                      METADATALOGFILE: null,              METADATALOGFILE: HEADER,
     *                      DATALOGFILE: null                   DATALOGFILE: HEADER,
     *                  }                                   }
     *          * last processed records are header records
     *               Input: {                                Output: {
     *                       METADATALOGFILE: HEADER,              METADATALOGFILE: PAYLOAD,
     *                       DATALOGFILE: HEADER                   DATALOGFILE: PAYLOAD,
     *                   }                                   }
     *          ...
     *          * last processed records are payload records, wrap around to header records
     *              Input: {                                Output: {
     *                       METADATALOGFILE: PAYLOAD,              METADATALOGFILE: HEADER,
     *                       DATALOGFILE: PAYLOAD                   DATALOGFILE: HEADER,
     *                   }                                   }
     *
     *      Some things to note:
     *          * for files with only a single record type, this simplifies to returning the same next record given the same last record
     *          * additional validations can be done to validate the integrity of the state machine and the last record set for each file type if needed
     *          * although this example shows returning the state machine for individual files, the actual next record for each file should be obtained by calling the respective method for the file type parser - this defers the state machine of each file to its own parser (better design)
     *
     *     Here is an example implementation for the setup above:
     *
     *          @Override
     *          public Map&lt;String, String&gt; getNextExpectedRecordType(Map&lt;String, String&gt; lastRecordTypeMap) {
     *              Map&lt;String, String&gt; nextExpectedRecordType = null;
     *              // initial case, nothing has been processed (null map or null types)
     *              if(lastRecordTypeMap == null) {
     *                  nextExpectedRecordType = new HashMap&lt;&gt;();
     *                  for(String fileType : EXPECTED_FILE_TYPES){
     *                     // NOTE: deferring the next record type to the file's parser
     *                     nextExpectedRecordType.put(fileType, fileParserMap.get(fileType).getNextExpectedRecordType(fileType, null));
     *                  }
     *                  return nextExpectedRecordType;
     *              } else {
     *                 ValidationUtils.validateAssertCondition(lastRecordTypeMap != null && lastRecordTypeMap.size() == EXPECTED_FILE_TYPES.size(), "lastRecordTypeMap size should equal expected");
     *                 nextExpectedRecordType = new HashMap&lt;&gt;();
     *                 for(String fileType : lastRecordTypeMap.keySet()){
     *                     // NOTE: deferring the next record type to the file's parser
     *                     nextExpectedRecordType.put(fileType, fileParserMap.get(fileType.toUpperCase()).getNextExpectedRecordType(fileType, lastRecordTypeMap.get(fileType)));
     *                 }
     *                 return nextExpectedRecordType;
     *             }
     *          }
     * @param s3FileTypeToLastProcessedRecordType map of each file type and the last processed record type in that file.
     * @return map of each file type and the next expected record type in that file.
     */
    Map<String /*s3FileType*/, String /*nextExpectedRecordType*/> getNextExpectedRecordType(Map<String, String> s3FileTypeToLastProcessedRecordType);

    /**
     *      This function is responsible for returning the extracted document - it is given:
     *          * the current state in each file which it can use to determine next state
     *          * the last document that was extracted in case the documents have some link with each other in the file
     *          * the filetype reader interfaces - that it can use to get the next records from each file
     *      With these inputs, it will construct the extracted document from the records obtained from each file and return that as the result. Note that the document being created is a CompositeDocument - since its being created by combining different records from different files.
     *
     *      For example, in our example above where there are two file types {METADATALOGFILE, DATALOGFILE} and the records in each file have a separate header and payload record as follows:
     *
     *          METADATALOGFILE                          DATALOGFILE
     *         -----------------                        -------------
     *         {metadata_headers_rec_1}                 {data_headers_rec_1}
     *         {metadata_payload_rec_1}                 {data_payload_rec_1}
     *         {metadata_headers_rec_2}                 {data_headers_rec_1}
     *         {metadata_payload_rec_2}                 {data_payload_rec_1}
     *
     *      Here is what an example implementation would look like:
     *
     *          @Override
     *          public ParseCompositeDocumentResult parseDocument(Map&lt;String, String&gt; fileNameFileTypeMap, DocumentInterface lastProcessedDocument, Map&lt;String, SingleFileStateMachineReaderInterface&gt; fileTypeReaderMap) {
     *              // validate inputs
     *              ValidationUtils.validateAssertCondition(EXPECTED_FILE_TYPES.size() == fileNameFileTypeMap.size(), "fileTypeFileNameMap size is unexpected");
     *              ValidationUtils.validateAssertCondition(EXPECTED_FILE_TYPES.size() == fileTypeReaderMap.size(), "fileTypeReaderMap size is unexpected");
     *              try {
     *                  // get the file readers for each file
     *                  SingleFileStateMachineReaderInterface metadataLogFileReader = fileTypeReaderMap.get(RecordLogFileTypes.METADATALOGFILE.name());
     *                  ValidationUtils.validateAssertCondition(metadataLogFileReader != null, "metadataLogFileReader is null");
     *                  SingleFileStateMachineReaderInterface dataLogFileReader = fileTypeReaderMap.get(RecordLogFileTypes.DATALOGFILE.name());
     *                  ValidationUtils.validateAssertCondition(dataLogFileReader != null, "dataLogFileReader is null");
     *
     *                  // init error list
     *                  List&lt;ErrorDocInterface&gt; errorRecordsList = new ArrayList();
     *
     *                  // Get the header record from metadata file
     *                  SingleDocInterface metadataHeaderRecord = null;
     *                  ErrorDocInterface metadataHeaderRecordError = null;
     *                  DocumentInterface document = metadataLogFileReader.nextRecord(peak: false)
     *                  if (document instanceof ErrorDocInterface) {
     *                      metadataHeaderRecordError = document;
     *                      errorRecordsList.Add(metadataHeaderRecordError);
     *                  } else if (document instanceof SkipDocInterface) {
     *                      throw new RuntimeException("Skip records are not expected");
     *                  } else {
     *                      metadataHeaderRecord = document;
     *                      ValidationUtils.validateAssertCondition("METADATA_HEADER".equals(metadataHeaderRecord.getRecordType()), "metadataHeaderRecord type should be METADATA_HEADER");
     *                  }
     *
     *                  // Get the payload record from metadata file
     *                  SingleDocInterface metadataPayloadRecord = null;
     *                  ErrorDocInterface metadataPayloadRecordError = null;
     *                  document = metadataLogFileReader.nextRecord(peak: false)
     *                  if (document instanceof ErrorDocInterface) {
     *                      metadataPayloadRecordError = document;
     *                      errorRecordsList.Add(metadataPayloadRecordError);
     *                  } else if (document instanceof SkipDocInterface) {
     *                      throw new RuntimeException("Skip records are not expected");
     *                  } else {
     *                      metadataPayloadRecord = document;
     *                      ValidationUtils.validateAssertCondition("METADATA_PAYLOAD".equals(metadataPayloadRecord.getRecordType()), "metadataPayloadRecord type should be METADATA_PAYLOAD");
     *                  }
     *
     *                  // Get the header record from data file
     *                  SingleDocInterface dataHeaderRecord = null;
     *                  ErrorDocInterface dataHeaderRecordError = null;
     *                  document = dataLogFileReader.nextRecord(peak: false)
     *                  if (document instanceof ErrorDocInterface) {
     *                      dataHeaderRecordError = document;
     *                      errorRecordsList.Add(dataHeaderRecordError);
     *                  } else if (document instanceof SkipDocInterface) {
     *                      throw new RuntimeException("Skip records are not expected");
     *                  } else {
     *                      dataHeaderRecord = document;
     *                      ValidationUtils.validateAssertCondition("DATA_PAYLOAD".equals(dataHeaderRecord.getRecordType()), "dataHeaderRecord type should be DATA_HEADER");
     *                  }
     *
     *                  // Get the payload record from data file
     *                  SingleDocInterface dataPayloadRecord = null;
     *                  ErrorDocInterface dataPayloadRecordError = null;
     *                  document = dataLogFileReader.nextRecord(peak: false)
     *                  if (document instanceof ErrorDocInterface) {
     *                      dataPayloadRecordError = document;
     *                      errorRecordsList.Add(dataPayloadRecordError);
     *                  } else if (document instanceof SkipDocInterface) {
     *                      throw new RuntimeException("Skip records are not expected");
     *                  } else {
     *                      dataPayloadRecord = document;
     *                      ValidationUtils.validateAssertCondition("DATA_PAYLOAD".equals(dataPayloadRecord.getRecordType()), "dataPayloadRecord type should be DATA_PAYLOAD");
     *                  }
     *
     *                  // validate the records are for the same documentId - we could do a peak: true when getting the records and validate Ids and then do peak: false in case there is concern that data corruption would lead to different file readers being at different / incorrect locations, but we also have a reference to last record in case we want to look at the last record to determine current position in file - implementers can decide what fits their usecase
     *                  String documentId = null;
     *                  if (metadataHeaderRecord != null) {
     *                      documentId = metadataHeaderRecord.getDocumentId();
     *                      ValidationUtils.validateAssertCondition(documentId != null, "documentId should not be null");
     *                  }
     *
     *                  if (metadataPayloadRecord != null) {
     *                      if (documentId == null) {
     *                          documentId = metadataPayloadRecord.getDocumentId();
     *                      } else {
     *                          ValidationUtils.validateAssertCondition(documentId.equals(metadataPayloadRecord.getDocumentId()), "documentId should not be the same");
     *                      }
     *                  }
     *
     *                  if (dataHeaderRecord != null) {
     *                      if (documentId == null) {
     *                          documentId = dataHeaderRecord.getDocumentId();
     *                      } else {
     *                          ValidationUtils.validateAssertCondition(documentId.equals(dataHeaderRecord.getDocumentId()), "documentId should not be the same");
     *                      }
     *                  }
     *
     *                  if (dataPayloadRecord != null) {
     *                      if (documentId == null) {
     *                          documentId = dataPayloadRecord.getDocumentId();
     *                      } else {
     *                          ValidationUtils.validateAssertCondition(documentId.equals(dataPayloadRecord.getDocumentId()), "documentId should not be the same");
     *                      }
     *                  }
     *
     *                  // construct the extracted document
     *                  CompositeDocInterface extractedDocument = null;
     *                  if (errorRecordsList.size() == 0) {
     *                      SingleDocInterface recordLogDocument = new LogRecordDocument(metadataHeaderRecord, metadataPayloadRecord, dataHeaderRecord, dataPayloadRecord);
     *                      LinkedHashMap&lt;SingleDocInterface, List&lt;ErrorDocInterface&gt;&gt; docMap = new LinkedHashMap&lt;&gt;();
     *                      docMap.put(recordLogDocument, errorRecordsList);
     *                      extractedDocument = new CompositeLogRecord(recordLogDocument.getDocumentId(), docMap);
     *                  } else {
     *                      SingleDocInterface recordLogDocument = CompositeLogRecord.NULL_DOCUMENT;
     *                      LinkedHashMap&lt;SingleDocInterface, List&lt;ErrorDocInterface&gt;&gt; docMap = new LinkedHashMap&lt;&gt;();
     *                      docMap.put(recordLogDocument, errorRecordsList);
     *
     *                      if (documentId == null) {
     *                          // if id is null, generate a random id, framework will add offsets to the error record, so we'll know where to look for the error record
     *                          documentId = "ERROR_ID_"+UUID.randomUUID().toString();
     *                      }
     *                      extractedDocument = new CompositeLogRecord(documentId, docMap);
     *                  }
     *
     *                  // generate the last expected record type map
     *                  Map&lt;String, String&gt; lastExpectedRecordType = new HashMap&lt;&gt;();
     *                  for(String fileType : fileTypeReaderMap.keySet()){
     *                      if (fileTypeReaderMap.get(fileType).getLastRecordTypeMap() == null) {
     *                          lastExpectedRecordType.put(fileType, null);
     *                      } else {
     *                          lastExpectedRecordType.putAll(fileTypeReaderMap.get(fileType).getLastRecordTypeMap());
     *                      }
     *                  }
     *
     *                  // generate the next expected record type map
     *                  Map&lt;String, String&gt; nextExpectedRecordType = this.getNextExpectedRecordType(lastExpectedRecordType);
     *
     *                  // create the offset map
     *                  Map&lt;String, String&gt; s3FileTypeOffsetMap = new HashMap&lt;&gt;();
     *                  s3FileTypeOffsetMap.putAll(metadataLogFileReader.getOffset());
     *                  s3FileTypeOffsetMap.putAll(dataLogFileReader.getOffset());
     *
     *                  return new ParseCompositeDocumentResult(nextRecordType, compositeDoc, lastProcessedRecordTypeMap, s3FileTypeOffsetMap, SingleFileReaderState.PROCESSING, errorRecordsList.size() == 0 ? ParseDocumentResultStatus.SUCCESS : ParseDocumentResultStatus.ERROR);
     *              }
     *              catch (Exception ex) {
     *                  throw ex;
     *              }
     *          }
     * @param s3FileTypeToLastProcessedRecordTypeMap the current state in each file which it can use to determine next state (map of filetype to last processed record)
     * @param lastProcessedDoc the last document that was extracted in case the documents have some link with each other in the file
     * @param s3FileTypeToFileReaderInterfaceMap the filetype reader interfaces - that it can use to get the next records from each file
     * @return The parse result containing the update file reader state and the extracted document / errors
     */
    ParseCompositeDocumentResult parseDocument(Map<String, String> s3FileTypeToLastProcessedRecordTypeMap, DocumentInterface lastProcessedDoc, Map<String, SystemFileReader> s3FileTypeToFileReaderInterfaceMap);
}