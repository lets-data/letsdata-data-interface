package com.resonance.letsdata.data.readers.interfaces;

import com.resonance.letsdata.data.documents.interfaces.DocumentInterface;
import com.resonance.letsdata.data.readers.interfaces.parsers.SingleFileStateMachineParser;
import com.resonance.letsdata.data.readers.model.ParseCompositeDocumentResult;

/**
 * The SingleFileStateMachineReader implements the logic to combine the individual records parsed by the SingleFileStateMachine parser and output them to a composite doc.
 * For example, if we have a DATAFILE which contains 2 types of records {metadata record, data record} and the output doc is constructed by the combining these two docs,
 * then the SingleFileStateMachineReader combines each {metadata, data} record pair into an output doc.
 *
 * Here are a couple of system architecture diagrams to explain this usecase:
 *      Diagram 1: Component Diagram with data flow
 *                             +--------------------------------+
 *                             |                                |
 *      ---> parseDocument-->--|                                |                       +--------------------+                       +-------------------------+
 *                             |                                |                       |XXXXXXXXXXXXXXXXXXXX|                       |                         |
 *                             |                                |---> nextRecord --->---|XXXXXXXXXXXXXXXXXXXX|---> parseDocument-->--|                         |
 *                             | SingleFileStateMachineReader   |<--- metadata rec --<--|XXXXXXXXXXXXXXXXXXXX|<--- metadata rec --<--|                         |
 *                             |                                |                       | System File Reader |                       | SingleFileStateMachine  |
 *                             |                                |---> nextRecord --->---|XXXXXXXXXXXXXXXXXXXX|---> parseDocument-->--|       Parser            |
 *                             |   { metadata + data }          |<----- data rec ----<--|XXXXXXXXXXXXXXXXXXXX|<----- data rec ----<--|                         |
 *      -<-- output doc---<----|        = output doc            |                       |XXXXXXXXXXXXXXXXXXXX|                       |                         |
 *                             |                                |                       +--------------------+                       +-------------------------+
 *                             |                                |                                |   |
 *                             +--------------------------------+                                |   |
 *                                                                                               |   |    File read
 *                                                                                              \|   |/
 *                                                                                               \   /
 *       Legend:                                                                                  \ /
 *       +--+                                                                         +-----------------------+
 *       |  |   Customer Implements Interface                                         | s3file_1.gz           |
 *       +--+                                                                         +-----------------------+
 *                                                                                    | {metadata_record_1}   |
 *       +--+                                                                         | {data_record_1}       |
 *       |XX|   System Implementation                                                 | {metadata_record_2}   |
 *       +--+                                                                         | {data_record_2}       |
 *                                                                                    | ...                   |
 *                                                                                    +-----------------------+
 *
 *  Diagram 2: Data flow
 *
 *     +-----------------------+ SingleFileStateMachineParser   +-----------------+
 *     | s3file_1.gz           |-------------->-----------------| metadata_record | -------->---------+
 *     +-----------------------+                                +-----------------+                   | SingleFileStateMachineReader     +---------------+
 *     | &lt;metadata_record_1&gt;   |                                                                      |-------------->--------------->---| composite_doc |
 *     | &lt;data_record_1&gt;       | SingleFileStateMachineParser   +-------------+                       |                                  +---------------+
 *     | &lt;metadata_record_2&gt;   |-------------->-----------------| data_record | -------->-------------+
 *     | &lt;data_record_2&gt;       |                                +-------------+
 *     | ...                   |
 *     +-----------------------+
 */
public interface SingleFileStateMachineReader {
    /**
     * This method returns the filetype for the file being read by the Single File State Machine reader.
     * For example, if the metadata records are in file metadata_file_X.gz and we've named this filetype in
     * the dataset creation as DATALOGFILE, then this function will return a DATALOGFILE as the file type
     * @return - the fileType of the file
     */
    String getS3FileType();

    /**
     * Given the filetype (LOGFILE/METADATALOGFILE/DATALOGFILE) and filename (s3file_1.gz/s3file_2.gz/s3file_3.gz) from the manifest file, return the resolved filename if necessary.
     * In most cases, the resolved filename would be the same as the filename, but in some cases, you might need to prepend paths if the data in the s3 bucket is not in the root directory.
     * For example, s3file_1.gz could be resolved to dataset_name/data_date/s3file_1.gz
     * @param s3FileType - the filetype - example DATALOGFILE
     * @param fileName - the filename - example s3file_1.gz
     * @return - the resolved fileName - example dataset_name/data_date/s3file_1.gz
     */
    String getResolvedS3FileName(String s3FileType, String fileName);

    /**
     * This function is responsible for creating the parser that is needed to parse the file that is being read by the Single File State Machine reader
     * For example, the input filetype is {DATAFILE}, the output is the filetype's corresponding SingleFileStateMachineParser
     * The function will create the parsers for the filetype (and should cache these in implementation as well).
     * The parser implementation that it returns would be the user implementations on how to parse the file types.
     * We'll reuse the parser class we've defined above in this example (DataFileParserImpl)
     *
     * Here is an example implementation:
     *        @Override
     *        public SingleFileStateMachineParser getReaderParserInterfacesForS3FileType(String fileType) {
     *            ValidationUtils.validateAssertCondition(getS3FileType().equals(fileType.toUpperCase()), "fileType is unexpected");
     *            if (fileParser == null) {
     *               fileParser = new DataFileParserImpl();
     *            }
     *            return fileParser;
     *        }
     * @param s3FileType - the filetype - example DATALOGFILE
     * @return - the constructed parser implementation for the filetype
     */
    SingleFileStateMachineParser getReaderParserInterfacesForS3FileType(String s3FileType);

    /**
     * Given the fileType and the last processed record type, find the next expected record type from the state machine
     * Defer to the file parser's getNextExpectedRecordType method or add custom logic. Here is an example implementation:
     *
     *         @Override
     *         public String getNextExpectedRecordType(String s3FileType, String lastProcessedRecordType) {
     *             ValidationUtils.validateAssertCondition(getS3FileType().equals(fileType.toUpperCase()), "fileType is unexpected");
     *             return this.getReaderParserInterfacesForS3FileType(fileType).getNextExpectedRecordType(s3FileType, lastProcessedRecordType);
     *         }
     * @param s3FileType - the filetype - example DATALOGFILE
     * @param lastProcessedRecordType - the lastProcessedRecordType - example METADATA
     * @return - the next expected record type - example DATA
     */
    String getNextExpectedRecordType(String s3FileType, String lastProcessedRecordType);

    /**
     * ParseCompositeDocumentResult parseDocument(String s3FileType, String lastProcessedRecordType, DocumentInterface lastProcessedDoc, SystemFileReader fileReader);
     *      This function is responsible for returning the extracted document - it is given:
     *          * the current state in the datafile which it can use to determine next state
     *          * the last document that was extracted in case the documents have some link with each other in the file
     *          * the filetype reader interface - that it can use to get the next records from the  datafile - this interface is a simple wrapper over the parser interface that we defined earlier
     *      With these inputs, this function will construct the extracted document from the records obtained from the file reader and return the result.
     *      Note that the document being created is a CompositeDocument - since its being created by combining different records from different files.
     *      Here is an example implementation:
     *
     *               @Override
     *               public ParseCompositeDocumentResult parseDocument(String s3FileType, String lastProcessedRecordType, DocumentInterface lastProcessedDoc, SystemFileReader fileReader) {
     *                   ValidationUtils.validateAssertCondition(getS3FileType().equals(fileType.toUpperCase()), "fileType is unexpected");
     *                   ValidationUtils.validateAssertCondition(fileReader.getFileTypeFileNameMap().get(s3FileType) != null, "fileReader fileTypeFileNameMap is unexpected");
     *
     *                   if (fileReader.getOffset().get(s3FileType) == 0) {
     *                       // optional: skip if there are any file headers that need not be processed
     *                       skipFileHeaders(fileReader);
     *                   }
     *
     *                   DocumentInterface document = fileReader.nextRecord(peek: false);
     *                   if (document == null) {
     *                       // End of file
     *                       ValidationUtils.validateAssertCondition(fileReader.getState() == SingleFileReaderState.COMPLETED, "fileReader state should be completed when nextRecord is null", fileReader);
     *                       Map&lt;String, String&gt; s3FileTypeNextRecordTypeMap = new HashMap&lt;&gt;();
     *                       s3FileTypeNextRecordTypeMap.put(s3FileType, null);
     *
     *                       Map&lt;String, Long&gt; s3FileTypeOffsetMap = new HashMap&lt;&gt;();
     *                       s3FileTypeOffsetMap.putAll(fileReader.getOffset());
     *
     *                       Map&lt;String, String&gt; lastProcessedRecordTypeMap = fileReader.getLastRecordTypeMap();
     *
     *                       Map&lt;String, String&gt; s3FileTypeNextRecordTypeMap = null;
     *                       CompositeDocInterface resultDocument = null;
     *                       return new ParseCompositeDocumentResult(s3FileTypeNextRecordTypeMap, resultDocument, lastProcessedRecordTypeMap, s3FileTypeOffsetMap, SingleFileReaderState.COMPLETED, ParseDocumentResultStatus.SUCCESS);
     *                   }
     *
     *                   // get metatdata record
     *                   MetadataRecord metadataDocument = null;
     *                   ErrorDocInterface metadataError = null;
     *                   if (document instanceof MetadataRecord) {
     *                       metadataDocument = (MetadataRecord) document;
     *                   } else if (document instanceof ErrorDocInterface) {
     *                       metadataError = (ErrorDocInterface) document;
     *                   } else {
     *                       throw new RuntimeException("parseDocument metadata nextRecord is of unexpected type");
     *                   }
     *
     *
     *                   // get data record
     *                   document = fileReader.nextRecord(peek: false);
     *                   ValidationUtils.validateAssertCondition(document != null, "data document should not be null");
     *
     *                   DataRecord dataDocument = null;
     *                   ErrorDocInterface dataError = null;
     *                   if (document instanceof DataRecord) {
     *                       dataDocument = (DataRecord) document;
     *                   } else if (document instanceof ErrorDocInterface) {
     *                       dataError = (ErrorDocInterface) document;
     *                   } else {
     *                       throw new RuntimeException("parseDocument data nextRecord is of unexpected type");
     *                   }
     *
     *                   if (metadataError != null || dataError != null) {
     *                       // Records have errors, construct and return error document
     *
     *                       Map&lt;String, String&gt; lastProcessedRecordTypeMap = fileReader.getLastRecordTypeMap();
     *                       Map&lt;String, String&gt; nextRecordTypeMap = fileReader.getNextExpectedRecordType(s3FileType, lastProcessedRecordTypeMap.get(s3FileType));
     *
     *                       Map&lt;String, Long&gt; s3FileTypeOffsetMap = new HashMap&lt;&gt;();
     *                       s3FileTypeOffsetMap.putAll(fileReader.getOffset());
     *
     *                       LinkedHashMap&lt;SingleDocInterface, List&lt;ErrorDocInterface&gt;&gt; docMap = new LinkedHashMap&lt;&gt;();
     *                       // add metadata doc and errors
     *                       List&lt;ErrorDocInterface&gt; metadataErrorRecordsList = metadataError == null ? null : Arrays.asList(metadataError);
     *                       docMap.put(metadataDocument == null ? DocFactoryImpl.createNullDoc("metadata") : metadataDocument, metadataErrorRecordsList);
     *
     *                       // add data doc and errors
     *                       List&lt;ErrorDocInterface&gt; dataErrorRecordsList = dataError == null ? null : Arrays.asList(dataError);
     *                       docMap.put(dataDocument == null ? DocFactoryImpl.createNullDoc("data") : dataDocument, dataErrorRecordsList);
     *
     *                       CompositeDocInterface compositeDoc = DocFactoryImpl.createCompositeDoc(docMap);
     *                       return new ParseCompositeDocumentResult(nextRecordTypeMap, compositeDoc, lastProcessedRecordTypeMap, s3FileTypeOffsetMap, SingleFileReaderState.PROCESSING, ParseDocumentResultStatus.ERROR);
     *                   }
     *
     *
     *                   List&lt;ErrorDocInterface&gt; errorRecordsList = null;
     *                   LinkedHashMap&lt;SingleDocInterface, List&lt;ErrorDocInterface&gt;&gt; docMap = new LinkedHashMap&lt;&gt;();
     *                   docMap.put(metadataDocument, errorRecordsList);
     *                   docMap.put(dataDocument, errorRecordsList);
     *                   CompositeDocInterface compositeDoc = DocFactoryImpl.createCompositeDoc(docMap);
     *
     *                   Map&lt;String, String&gt; lastProcessedRecordTypeMap = fileReader.getLastRecordTypeMap();
     *                   Map&lt;String, String&gt; nextRecordTypeMap = fileReader.getNextExpectedRecordType(s3FileType, lastProcessedRecordTypeMap.get(s3FileType));
     *
     *                   Map&lt;String, Long&gt; s3FileTypeOffsetMap = new HashMap&lt;&gt;();
     *                   s3FileTypeOffsetMap.putAll(fileReader.getOffset());
     *                   return new ParseCompositeDocumentResult(nextRecordTypeMap, compositeDoc, lastProcessedRecordTypeMap, s3FileTypeOffsetMap, SingleFileReaderState.PROCESSING, ParseDocumentResultStatus.SUCCESS);
     *               }
     *           }
     *
     * @param s3FileType - the filetype
     * @param lastProcessedRecordType - the lastProcessedRecordType
     * @param lastProcessedDoc - the lastProcessedDoc
     * @param fileReader - the reference to SystemFileReader implementation for the fileType that can be used to retrieve the next records from the parser
     * @return - ParseCompositeDocumentResult which has the extracted record and the status (error, success or skip)
     */
    ParseCompositeDocumentResult parseDocument(String s3FileType, String lastProcessedRecordType, DocumentInterface lastProcessedDoc, SystemFileReader fileReader);
}
