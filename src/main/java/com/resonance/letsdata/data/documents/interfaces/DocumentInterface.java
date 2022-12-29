package com.resonance.letsdata.data.documents.interfaces;

import java.util.Map;

/**
 * The "DocumentInterface" is the base interface for any document that can be returned by the user handlers. All other document interfaces and documents either extend or implement this interface.
 */
public interface DocumentInterface {
    /**
     * Gets the documentId for the document
     * @return documentId
     */
    String getDocumentId();

    /**
     * Gets the record type of the document
     * @return the record type
     */
    String getRecordType();

    /**
     * Gets any optional metadata for the document as a map
     * @return map of optional document metadata
     */
    Map<String, Object> getDocumentMetadata();

    /**
     * Interface method that serializes the document to string that can be written to the destination
     * @return serialized document as string
     */
    String serialize();

    /**
     * The partition key of the document - useful to determine the partition for the document that would be written to
     * @return the partition key for the document
     */
    String getPartitionKey();
}
