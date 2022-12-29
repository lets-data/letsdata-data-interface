package com.resonance.letsdata.data.readers.interfaces.parsers;

import com.resonance.letsdata.data.readers.model.ParseDocumentResult;
import com.resonance.letsdata.data.readers.model.RecordParseHint;

/**
 * The parser interface for Single File Reader usecase. This is used when all the files are of a single type and the records in the file do not follow a state machine.
 * This interface is where you tell us how to parse the individual records from the file. Since this is single file reader, there is no state machine maintained.
 * Simple example is a log file where each line is a data record and the extracted document is transformed from each data record (line in the file).
 *
 *             Example: Single File Reader Files and Record Layout:
 *
 *             +----------------------+------------------=---+---------------------+
 *             | logfile_1.gz         | logfile_2.gz         | logfile_3.gz        |
 *             +---=------------------+----------------------+---------------------+
 *             | &lt;logline_1&gt;          | &lt;logline_1&gt;          | &lt;logline_1&gt;         |
 *             | &lt;logline_2&gt;          | &lt;logline_2&gt;          | &lt;logline_2&gt;         |
 *             | &lt;logline_3&gt;          | &lt;logline_3&gt;          | &lt;logline_3&gt;         |
 *             | &lt;logline_4&gt;          | &lt;logline_4&gt;          | &lt;logline_4&gt;         |
 *             | ...                  | ...                  | ...                 |
 *             +----------------------+----------------------+---------------------+
 */
public interface SingleFileParser {
    /**
     * The filetype of the file - for the example we've used, we define the filetype (logical name) as "LOGFILE"
     * Here is an example implementation:
     *
     *     public String getS3FileType() {
     *         return "LOGFILE"
     *     }
     *
     * @return - The file type
     */
    String getS3FileType();

    /**
     * Given the filetype (LOGLINE) and filename (logfile_1.gz/logfile_2.gz/logfile_3.gz) from the manifest file, return the resolved filename if necessary.
     * In most cases, the resolved filename would be the same as the filename, but in some cases, you might need to prepend paths if the data in the s3 bucket is not in the root directory.
     * For example, dataset_name/log_date/logfile_1.gz (See the docs for MultipleFileStateMachineReader.getResolvedS3Filename method for additional details around the file name resolution)
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
     * @param s3FileType - the file type - example LOGFILE
     * @param fileName - the file name - example logfile_1.gz
     * @return - the resolved file name - example dataset_name/data_date/logfile_1.gz
     */
    String getResolvedS3FileName(String s3FileType, String fileName);

    /**
     * The record start pattern - to extract the records from the file, the parser needs to know the record start delimiter - it will search the file sequentially till it finds this delimiter and then from that point on, it will search for the end record delimiter.
     * Once it finds the end record delimiter, it will copy those bytes to the parse document function to create an extracted record
     *
     *      For example, for the following log lines in the log file, we define the start pattern as "{\"ts\"" and end pattern as "}\n":
     *
     *      Logfile:
     *      --------
     *       {"ts":1647352053448,"dt":"Mar 15, 2022 6:47:33 AM","hnm":"archimedes-mbp-2.hsd1.wa.comcast.net","unm":"archimedes","lvl":"WARN","thd":"main","cnm":"com.ancient.mathematicians.archimedes.InvalidBuoyantForceException","fnm":"InvalidBuoyantForceException.java","lnm":178,"mnm":"validateArchimedesPrinciple","msg":"The buoyant force is different from the weight. Archimedes' principle has been invalidated."}
     *       {"ts":1647352053449,"dt":"Mar 15, 2022 6:47:33 AM","hnm":"archimedes-mbp-2.hsd1.wa.comcast.net","unm":"archimedes","lvl":"WARN","thd":"main","cnm":"com.ancient.mathematicians.archimedes.InvalidBuoyantForceException","fnm":"InvalidBuoyantForceException.java","lnm":178,"mnm":"validateArchimedesPrinciple","msg":"The buoyant force is different from the weight. Archimedes' principle has been invalidated."}
     *
     *       Example implementation:
     *       -----------------------
     *       public RecordParseHint getRecordStartPattern(String s3FileType) {
     *           ValidationUtils.validateAssertCondition(getS3FileType().equals(fileType.toUpperCase()), "fileType is unexpected");
     *           return new RecordParseHint(RecordHintType.PATTERN, "{\"ts\"", -1);
     *       }
     *
     * @param s3FileType - the filetype
     * @return - the record start pattern as a RecordParseHint object
     */
    RecordParseHint getRecordStartPattern(String s3FileType);

    /**
     * The record end pattern - to extract the records from the file, the parser needs to know the record start delimiter - it will search the file sequentially till it finds this delimiter and then from that point on, it will search for the end record delimiter.
     * Once it finds the end record delimiter, it will copy those bytes to the parse document function to create an extracted record
     *
     *      For example, for the following log lines in the log file, we define the start pattern as "{\"ts\"" and end pattern as "}\n":
     *
     *      Logfile:
     *      --------
     *       {"ts":1647352053448,"dt":"Mar 15, 2022 6:47:33 AM","hnm":"archimedes-mbp-2.hsd1.wa.comcast.net","unm":"archimedes","lvl":"WARN","thd":"main","cnm":"com.ancient.mathematicians.archimedes.InvalidBuoyantForceException","fnm":"InvalidBuoyantForceException.java","lnm":178,"mnm":"validateArchimedesPrinciple","msg":"The buoyant force is different from the weight. Archimedes' principle has been invalidated."}
     *       {"ts":1647352053449,"dt":"Mar 15, 2022 6:47:33 AM","hnm":"archimedes-mbp-2.hsd1.wa.comcast.net","unm":"archimedes","lvl":"WARN","thd":"main","cnm":"com.ancient.mathematicians.archimedes.InvalidBuoyantForceException","fnm":"InvalidBuoyantForceException.java","lnm":178,"mnm":"validateArchimedesPrinciple","msg":"The buoyant force is different from the weight. Archimedes' principle has been invalidated."}
     *
     *       Example implementation:
     *       -----------------------
     *       public RecordParseHint getRecordEndPattern(String s3FileType) {
     *           ValidationUtils.validateAssertCondition(getS3FileType().equals(fileType.toUpperCase()), "fileType is unexpected");
     *           return new RecordParseHint(RecordHintType.PATTERN, "}\n", -1);
     *       }
     * @param s3FileType - the filetype
     * @return - the record end pattern as a RecordParseHint object
     */
    RecordParseHint getRecordEndPattern(String s3FileType);

    /**
     *  This function is called with the document contents in a byteArr and the startIndex and endIndex into the byteArr as the start and end of the record.
     *  The implementer is expected to construct the output record from these bytes.
     *
     *  Here is an example implementation which parses error records from the file:
     *  @Override
     *  public ParseDocumentResult parseDocument(String s3FileType, String s3Filename, long offsetBytes, byte[] byteArr, int startIndex, int endIndex) {
     *      ValidationUtils.validateAssertCondition(s3FileType != null && s3FileType.equalsIgnoreCase(ParserFileType), "ResonanceJsonLogProcessor.parseDocument file type is unexpected");
     *      ValidationUtils.validateAssertCondition(byteArr != null && startIndex >= 0 && byteArr.length > endIndex && endIndex > startIndex, "ResonanceJsonLogProcessor.parseDocument byte array offsets are invalid");
     *
     *      // uses the Matcher utility from the interface package to do pattern matching
     *      int errorLevelIndex = Matcher.match(byteArr, startIndex, endIndex, "\"lvl\":\"ERROR\"");
     *      if (errorLevelIndex == -1) {
     *          // skip since we are interested only in ERROR records
     *          Map&lt;String, Long&gt; startOffset = new HashMap&lt;&gt;();
     *          startOffset.put(s3FileType, offsetBytes);
     *          Map&lt;String, Long&gt; endOffset = new HashMap&lt;&gt;();
     *          endOffset.put(s3FileType, offsetBytes);
     *          String errorMessage = "skipping message - level is not ERROR";
     *          String documentId = null;
     *          String recordType = null;
     *          Map&lt;String, Object&gt; documentMetadata = null;
     *          String serialize = null;
     *          String partitionKey = s3Filename;
     *          DocumentInterface skipDoc = new SkipDoc(startOffset, endOffset, errorMessage, documentId, recordType, documentMetadata, serialize, partitionKey);
     *          return new ParseDocumentResult(null, skipDoc, ParseDocumentResultStatus.SKIP);
     *      } else {
     *          try {
     *              Map&lt;String, Object&gt; jsonMap = objectMapper.readValue(byteArr, startIndex, endIndex, HashMap.class);
     *              Long timestamp = (Long) jsonMap.get("ts");
     *              String docId = ""+timestamp+s3Filename;
     *              String recordType = (String) jsonMap.get("lvl");
     *              DocumentInterface doc = new JsonLogDocument(docId, recordType, null,  objectMapper.writeValueAsString(jsonMap), docId);
     *              return new ParseDocumentResult(null, doc, ParseDocumentResultStatus.SUCCESS);
     *          } catch (Exception e) {
     *              logger.error("error processing json document from file", s3FileType, s3Filename, offsetBytes, e);
     *              Map&lt;String, Long&gt; startOffset = new HashMap&lt;&gt;();
     *              startOffset.put(s3FileType, offsetBytes);
     *              Map&lt;String, Long&gt; endOffset = new HashMap&lt;&gt;();
     *              endOffset.put(s3FileType, offsetBytes);
     *              String errorMessage = "error processing json document from file - ex: "+e;
     *              String documentId = null;
     *              String recordType = null;
     *              Map&lt;String, Object&gt; documentMetadata = null;
     *              String serialize = null;
     *              String partitionKey = s3Filename;
     *              DocumentInterface errorDoc = new ErrorDoc(startOffset, endOffset, errorMessage, documentId, recordType, documentMetadata, serialize, partitionKey);
     *              return new ParseDocumentResult(null, errorDoc, ParseDocumentResultStatus.ERROR);
     *          }
     *      }
     *  }
     *
     * @param s3FileType - the filetype
     * @param s3Filename - the filename
     * @param offsetBytes - the offset bytes into the file
     * @param byteArr - the byteArr that has the contents of the record
     * @param startIndex - the start index of the record in the byteArr
     * @param endIndex - the end index of the record in the byteArr
     * @return - ParseDocumentResult which has the extracted record and the status (error, success or skip)
     */
    ParseDocumentResult parseDocument(String s3FileType, String s3Filename, long offsetBytes, byte[] byteArr, int startIndex, int endIndex);
}
