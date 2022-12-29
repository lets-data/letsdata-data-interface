package com.resonance.letsdata.data.readers.model;

import com.resonance.letsdata.data.documents.interfaces.CompositeDocInterface;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Map;

public class ParseCompositeDocumentResult {
    private final Map<String, String> s3FileTypeNextRecordTypeMap;
    private final CompositeDocInterface document;
    private final Map<String, String> s3FileTypeLastProcessedRecordType;
    private final Map<String, Long> s3FileTypeOffsets;
    private final SingleFileReaderState fileReaderState;
    private final ParseDocumentResultStatus parseDocumentResultStatus;

    public ParseCompositeDocumentResult(Map<String, String> s3FileTypeNextRecordTypeMap, CompositeDocInterface document, Map<String, String> s3FileTypeLastProcessedRecordType, Map<String, Long> s3FileTypeOffsets, SingleFileReaderState fileReaderState, ParseDocumentResultStatus parseDocumentResultStatus) {
        this.s3FileTypeNextRecordTypeMap = s3FileTypeNextRecordTypeMap;
        this.document = document;
        this.s3FileTypeLastProcessedRecordType = s3FileTypeLastProcessedRecordType;
        this.s3FileTypeOffsets = s3FileTypeOffsets;
        this.fileReaderState = fileReaderState;
        this.parseDocumentResultStatus = parseDocumentResultStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof ParseCompositeDocumentResult)) return false;

        ParseCompositeDocumentResult that = (ParseCompositeDocumentResult) o;

        return new EqualsBuilder()
                .append(s3FileTypeNextRecordTypeMap, that.s3FileTypeNextRecordTypeMap)
                .append(document, that.document)
                .append(s3FileTypeLastProcessedRecordType, that.s3FileTypeLastProcessedRecordType)
                .append(s3FileTypeOffsets, that.s3FileTypeOffsets)
                .append(fileReaderState, that.fileReaderState)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(s3FileTypeNextRecordTypeMap)
                .append(document)
                .append(s3FileTypeLastProcessedRecordType)
                .append(s3FileTypeOffsets)
                .append(fileReaderState)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "ParseDocumentListResult{" +
                "s3FileTypeNextRecordTypeMap=" + s3FileTypeNextRecordTypeMap +
                ", document=" + document +
                ", s3FileTypeLastProcessedRecordType=" + s3FileTypeLastProcessedRecordType +
                ", s3FileTypeOffsets=" + s3FileTypeOffsets +
                ", fileReaderState=" + fileReaderState +
                '}';
    }

    public Map<String, String> getS3FileTypeNextRecordTypeMap() {
        return s3FileTypeNextRecordTypeMap;
    }

    public CompositeDocInterface getDocument() {
        return document;
    }

    public Map<String, String> getS3FileTypeLastProcessedRecordType() {
        return s3FileTypeLastProcessedRecordType;
    }

    public Map<String, Long> getS3FileTypeOffsets() {
        return s3FileTypeOffsets;
    }

    public SingleFileReaderState getFileReaderState() {
        return fileReaderState;
    }

    public ParseDocumentResultStatus getParseDocumentResultStatus() {
        return parseDocumentResultStatus;
    }
}
