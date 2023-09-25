package com.resonance.letsdata.data.documents.interfaces;

import java.util.Map;

/**
 * The "ErrorDocInterface" extends the "SingleDocInterface" is the base interface for any error documents that are returned by the user handlers.
 * A default implementation for the interface is provided at "com.resonance.letsdata.data.documents.implementation.ErrorDoc" which is used by default.
 * Customers can return errors from handlers using this default implementation or write their own Error docs and return these during processing.
 */
public interface ErrorDocInterface extends SingleDocInterface {
    /**
     * The erroneous record start offset (in bytes) of the error record in the files by file types
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
    Map<String, String> getErrorStartOffsetMap();

    /**
     * The erroneous record end offset (in bytes) of the error record in the files by file types
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
     * @return Map of &lt;FileType, RecordEndOffsetInBytes&gt;
     */
    Map<String, String> getErrorEndOffsetMap();

    /**
     * The error message string that will be captured in the error record
     * @return The error message string
     */
    String getErrorMessage();
}
