package com.resonance.letsdata.data.documents.implementation;

import com.resonance.letsdata.data.documents.interfaces.ErrorDocInterface;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Map;

/**
 * The default implementation for the ErrorDocInterface, which can be used by implementers to return record errors during processing
 */
public class ErrorDoc implements ErrorDocInterface {

    /**
     * The documentId of the error doc record
     */
    private final String documentId;

    /**
     * The record type of the error doc
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
     * The error message
     */
    private final String errorMessage;

    /**
     * Any metadata to attach with the error records
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
     * The default constructor that constructs the error doc
     * @param startOffset - The start offset into the file for the error record in bytes. This is a map of fileType to file offset in bytes
     * @param endOffset - The end offset into the file for the error record in bytes. This is a map of fileType to file offset in bytes
     * @param errorMessage - The error message
     * @param documentId - The documentId of the error doc record
     * @param recordType - The record type of the error doc
     * @param documentMetadata - Any metadata to attach with the error records
     * @param serialize - The serialized string representation of the error record
     * @param partitionKey - Identifier to identify the partition key of the record
     */
    public ErrorDoc(Map<String, Long> startOffset, Map<String, Long> endOffset, String errorMessage, String documentId, String recordType, Map<String, Object> documentMetadata, String serialize, String partitionKey) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.errorMessage = errorMessage;
        this.documentId = documentId;
        this.recordType = recordType;
        this.documentMetadata = documentMetadata;
        this.serialize = serialize;
        this.partitionKey = partitionKey;
    }

    /**
     * The start offset into the file for the error record in bytes
     * @return Map of fileType to file offset in bytes
     */
    @Override
    public Map<String, Long> getErrorStartOffsetMap() {
        return startOffset;
    }

    /**
     * The end offset into the file for the error record in bytes
     * @return Map of fileType to file offset in bytes
     */
    @Override
    public Map<String, Long> getErrorEndOffsetMap() {
        return endOffset;
    }

    /**
     * The error message
     * @return the error message
     */
    @Override
    public String getErrorMessage() {
        return errorMessage;
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

        if (!(o instanceof ErrorDoc)) return false;

        ErrorDoc errorDoc = (ErrorDoc) o;

        return new EqualsBuilder().append(getDocumentId(), errorDoc.getDocumentId()).append(getRecordType(), errorDoc.getRecordType()).append(startOffset, errorDoc.startOffset).append(endOffset, errorDoc.endOffset).append(getErrorMessage(), errorDoc.getErrorMessage()).append(getDocumentMetadata(), errorDoc.getDocumentMetadata()).append(serialize, errorDoc.serialize).append(getPartitionKey(), errorDoc.getPartitionKey()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getDocumentId()).append(getRecordType()).append(startOffset).append(endOffset).append(getErrorMessage()).append(getDocumentMetadata()).append(serialize).append(getPartitionKey()).toHashCode();
    }

    @Override
    public String toString() {
        return "ErrorDoc{" +
                "documentId='" + documentId + '\'' +
                ", recordType='" + recordType + '\'' +
                ", startOffset=" + startOffset +
                ", endOffset=" + endOffset +
                ", errorMessage='" + errorMessage + '\'' +
                ", documentMetadata=" + documentMetadata +
                ", serialize='" + serialize + '\'' +
                ", partitionKey='" + partitionKey + '\'' +
                '}';
    }

    @Override
    public boolean isSingleDoc() {
        return true;
    }
}
