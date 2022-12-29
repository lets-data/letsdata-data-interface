package com.resonance.letsdata.data.readers.model;

import com.resonance.letsdata.data.documents.interfaces.DocumentInterface;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ParseDocumentResult {
    private final String nextRecordType;
    private final DocumentInterface document;
    private final ParseDocumentResultStatus status;

    public ParseDocumentResult(String nextRecordType, DocumentInterface document, ParseDocumentResultStatus status) {
        this.nextRecordType = nextRecordType;
        this.document = document;
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof ParseDocumentResult)) return false;

        ParseDocumentResult that = (ParseDocumentResult) o;

        return new EqualsBuilder()
                .append(nextRecordType, that.nextRecordType)
                .append(document, that.document)
                .append(status, that.status)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(nextRecordType)
                .append(document)
                .append(status)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "ParseDocumentResult{" +
                "nextRecordType='" + nextRecordType + '\'' +
                ", document=" + document +
                ", status=" + status +
                '}';
    }

    public String getNextRecordType() {
        return nextRecordType;
    }

    public DocumentInterface getDocument() {
        return document;
    }

    public ParseDocumentResultStatus getStatus() {
        return status;
    }
}
