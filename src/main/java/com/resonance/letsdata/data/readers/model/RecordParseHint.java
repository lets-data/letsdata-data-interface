package com.resonance.letsdata.data.readers.model;

import com.resonance.letsdata.data.util.ValidationUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class RecordParseHint {
    private final RecordHintType recordHintType;
    private final String pattern;
    private final int offset;

    public RecordParseHint(RecordHintType recordHintType, String pattern, int offset) {
        this.recordHintType = recordHintType;
        this.pattern = pattern;
        this.offset = offset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof RecordParseHint)) return false;

        RecordParseHint that = (RecordParseHint) o;

        return new EqualsBuilder()
                .append(offset, that.offset)
                .append(recordHintType, that.recordHintType)
                .append(pattern, that.pattern)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(recordHintType)
                .append(pattern)
                .append(offset)
                .toHashCode();
    }

    public RecordHintType getRecordHintType() {
        return recordHintType;
    }

    public String getPattern() {
        ValidationUtils.validateAssertCondition(recordHintType == RecordHintType.PATTERN, "getStringMatchPattern - invalid accessor called for recordHintType");
        return pattern;
    }

    public int getOffset() {
        ValidationUtils.validateAssertCondition(recordHintType == RecordHintType.OFFSET, "getOffset - invalid accessor called for recordHintType");
        return offset;
    }
}
