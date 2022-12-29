package com.resonance.letsdata.data.documents.implementation;

import com.resonance.letsdata.data.documents.interfaces.SkipDocInterface;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Map;

/**
 * The default implementation of the "SkipDocInterface".
 * A skip document is returned when the processor determines that the record from the file is not of interest to the current processor and should be skipped from being written to the write destination.
 * Customers can return skip records from handlers using this default implementation or write their own Skip docs by implementing the SkipDocInterface.
 */
public class SkipDoc implements SkipDocInterface {

    /**
     * The documentId of the skip doc record
     */
    private final String documentId;

    /**
     * The record type of the skip doc
     */
    private final String recordType;

    /**
     * The start offset into the file for the error record in bytes
     * Map of fileType to file offset in bytes
     */
    private final Map<String, Long> startOffset;

    /**
     * The end offset into the file for the error record in bytes
     * Map of fileType to file offset in bytes
     */
    private final Map<String, Long> endOffset;

    /**
     * The skip message
     */
    private final String skipMessage;

    /**
     * Any metadata to attach with the skip record
     */
    private final Map<String, Object> documentMetadata;
    /**
     * The serialized string representation of the error record
     */
    private final String serialize;

    /**
     * Identifier to identify the partition key of the record
     */
    private final String partitionKey;

    /**
     * The default constructor that constructs the skip doc
     * @param startOffset - The start offset into the file for the skip record in bytes. This is a map of fileType to file offset in bytes
     * @param endOffset - The end offset into the file for the error skip in bytes. This is a map of fileType to file offset in bytes
     * @param skipMessage - The skip message
     * @param documentId - The documentId of the skip doc record
     * @param recordType - The record type of the skip doc
     * @param documentMetadata - Any metadata to attach with the skip records
     * @param serialize - The serialized string representation of the skip record
     * @param partitionKey - Identifier to identify the partition key of the record
     */
    public SkipDoc(Map<String, Long> startOffset, Map<String, Long> endOffset, String skipMessage, String documentId, String recordType, Map<String, Object> documentMetadata, String serialize, String partitionKey) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.skipMessage = skipMessage;
        this.documentId = documentId;
        this.recordType = recordType;
        this.documentMetadata = documentMetadata;
        this.serialize = serialize;
        this.partitionKey = partitionKey;
    }

    /**
     * The start offset into the file for the error record in bytes
     * @return - Map of fileType to file offset in bytes
     */
    @Override
    public Map<String, Long> getErrorStartOffsetMap() {
        return startOffset;
    }

    /**
     * The end offset into the file for the error record in bytes
     * @return - Map of fileType to file offset in bytes
     */
    @Override
    public Map<String, Long> getErrorEndOffsetMap() {
        return endOffset;
    }

    /**
     * The skip message
     * @return - The skip message
     */
    @Override
    public String getSkipMessage() {
        return skipMessage;
    }


    /**
     * The documentId of the error doc record
     * @return the documentId
     */
    @Override
    public String getDocumentId() {
        return documentId;
    }

    /**
     * The record type of the error doc
     * @return the return type
     */
    @Override
    public String getRecordType() {
        return recordType;
    }

    /**
     * Any metadata to attach with the error records
     * @return Map of String to Object
     */
    @Override
    public Map<String, Object> getDocumentMetadata() {
        return documentMetadata;
    }

    /**
     * The serialized string representation of the error record
     * @return serialized string
     */
    @Override
    public String serialize() {
        return serialize;
    }

    /**
     * Identifier to identify the partition key of the record
     * @return the partition key as string
     */
    @Override
    public String getPartitionKey() {
        return partitionKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof SkipDoc)) return false;

        SkipDoc skipDoc = (SkipDoc) o;

        return new EqualsBuilder().append(getDocumentId(), skipDoc.getDocumentId()).append(getRecordType(), skipDoc.getRecordType()).append(startOffset, skipDoc.startOffset).append(endOffset, skipDoc.endOffset).append(getSkipMessage(), skipDoc.getSkipMessage()).append(getDocumentMetadata(), skipDoc.getDocumentMetadata()).append(serialize, skipDoc.serialize).append(getPartitionKey(), skipDoc.getPartitionKey()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getDocumentId()).append(getRecordType()).append(startOffset).append(endOffset).append(getSkipMessage()).append(getDocumentMetadata()).append(serialize).append(getPartitionKey()).toHashCode();
    }

    @Override
    public String toString() {
        return "SkipDoc{" +
                "documentId='" + documentId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", startOffset=" + startOffset +
                ", endOffset=" + endOffset +
                ", skipMessage='" + skipMessage + '\'' +
                ", documentMetadata=" + documentMetadata +
                ", serialize='" + serialize + '\'' +
                ", partitionKey='" + partitionKey + '\'' +
                '}';
    }
}
