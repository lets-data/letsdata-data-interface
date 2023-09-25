package com.resonance.letsdata.data.documents.interfaces;

import java.util.Map;

/**
 * The "SkipDocInterface" extends the "SingleDocInterface" is the base interface for any skip documents that are returned by the user handlers.
 * A skip document is returned when the processor determines that the record from the file is not of interest to the current processor and should be skipped from being written to the write destination.
 * A default implementation for the interface is provided at "com.resonance.letsdata.data.documents.implementation.SkipDoc" which is used by default.
 * Customers can return skip records from handlers using this default implementation or write their own Skip docs and return these during processing.
 */
public interface SkipDocInterface extends SingleDocInterface {
    /**
     * The skip record start offset (in bytes) of the error record in the files by file types
     * For 'Single File' and 'Single File State Machine' readers, there would be a single file type in the return map.
     * For example,
     *  {
     *      "CLICKSTREAMLOGS": 58965L
     *  }
     *  For 'Multiple File State Machine' readers, the return map should have offsets (in bytes) into each of the files.
     *  For example,
     *  {
     *      "METADATALOG": 58965L,
     *      "DATALOG": 5484726L,
     *  }
     * @return Map of &lt;FileType, RecordStartOffsetInBytes&gt;
     */
    Map<String, String> getErrorStartOffsetMap();

    /**
     * The skip record end offset (in bytes) of the error record in the files by file types
     * For 'Single File' and 'Single File State Machine' readers, there would be a single file type in the return map.
     * For example,
     *  {
     *      "CLICKSTREAMLOGS": "58965"
     *  }
     *  For 'Multiple File State Machine' readers, the return map should have offsets (in bytes) into each of the files.
     *  For example,
     *  {
     *      "METADATALOG": "58965",
     *      "DATALOG": "5484726",
     *  }
     * @return Map of &lt;FileType, RecordStartOffsetInBytes&gt;
     */
    Map<String, String> getErrorEndOffsetMap();

    /**
     * The skip message string that will be captured in the skip record
     * @return The skip message string
     */
    String getSkipMessage();
}