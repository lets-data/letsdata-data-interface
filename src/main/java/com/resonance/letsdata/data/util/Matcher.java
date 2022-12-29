package com.resonance.letsdata.data.util;

import com.resonance.letsdata.data.readers.model.RecordHintType;
import com.resonance.letsdata.data.readers.model.RecordParseHint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Matcher {
    private static final Logger logger = LoggerFactory.getLogger(Matcher.class);

    private static final ConcurrentHashMap<String, StringMatchPattern>  patternStringMap = new ConcurrentHashMap<>();

    public static StringMatchPattern getMatchPatternForString(String pattern)
    {
        return patternStringMap.computeIfAbsent(pattern, new Function<String, StringMatchPattern>() {
            @Override
            public StringMatchPattern apply(String s) {
                return new StringMatchPattern(s);
            }
        });
    }

    public static int match(byte[] buffer, int off, int len, RecordParseHint recordParseHint) {
        if (recordParseHint.getRecordHintType() == RecordHintType.OFFSET) {
            ValidationUtils.validateAssertCondition(recordParseHint.getOffset() > 0, "invalid offset in record parse hint");
            if (recordParseHint.getOffset() < len) {
                return -1;
            }
            return off+recordParseHint.getOffset();
        } else if (recordParseHint.getRecordHintType() == RecordHintType.PATTERN) {
            return match(buffer, off, len, recordParseHint.getPattern());
        } else {
            throw new RuntimeException("Unknown record hint type");
        }
    }

    /**
     * Finds the boundary in the given buffer using Boyer-Moore algo.
     * Copied from java.util.regex.Pattern.java
     *
     * @param buffer boundary to be searched in this mybuf
     * @param off start index in mybuf
     * @param len number of bytes in mybuf
     *
     * @return -1 if there is no match or index where the match starts
     */
    public static int match(byte[] buffer, int off, int len, String patternString) {

        StringMatchPattern patternName = getMatchPatternForString(patternString);

        byte[] pattern = patternName.getPatternBytes();
        int[] bcs = patternName.getPatternBCS();
        int[] gss = patternName.getPatternGSS();

        int last = len - pattern.length;

        // Loop over all possible match positions in text
        NEXT:   while (off <= last) {
            // Loop over pattern from right to left
            for (int j = pattern.length - 1; j >= 0; j--) {
                byte ch = buffer[off+j];
                if (ch != pattern[j]) {
                    // Shift search to the right by the maximum of the
                    // bad character shift and the good suffix shift
                    off += Math.max(j + 1 - bcs[ch&0x7F], gss[j]);
                    continue NEXT;
                }
            }
            // Entire pattern matched starting at off
            return off;
        }
        return -1;
    }

    public static class KeyValueResult {
        private final String key;
        private final String value;
        private final int nextIndex;

        public KeyValueResult(String key, String value, int nextIndex) {
            this.key = key;
            this.value = value;
            this.nextIndex = nextIndex;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public int getNextIndex() {
            return nextIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            KeyValueResult that = (KeyValueResult) o;
            return nextIndex == that.nextIndex &&
                    Objects.equals(key, that.key) &&
                    Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {

            return Objects.hash(key, value, nextIndex);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("KeyValueResult{");
            sb.append("key='").append(key).append('\'');
            sb.append(", value='").append(value).append('\'');
            sb.append(", nextIndex=").append(nextIndex);
            sb.append('}');
            return sb.toString();
        }
    }

    public static KeyValueResult getKeyValueFromLine(byte[] byteArr, int startIndex, int endIndex, String lineEndPattern, String fieldNameSeparatorPattern) throws Exception {
        String key = null;
        String value = null;
        int newLineIndex = Matcher.match(byteArr, startIndex, endIndex, lineEndPattern);
        ValidationUtils.validateAssertCondition(newLineIndex < endIndex, "new line index should be less than endIndex", startIndex, endIndex, newLineIndex);
        if (newLineIndex <= startIndex) {
            newLineIndex = endIndex;
        }

        int separatorIndex = Matcher.match(byteArr, startIndex, newLineIndex, fieldNameSeparatorPattern);
        ValidationUtils.validateAssertCondition(separatorIndex < newLineIndex, "separator index should be less than new line index", startIndex, newLineIndex, separatorIndex);

        StringMatchPattern patternName = getMatchPatternForString(fieldNameSeparatorPattern);

        if (separatorIndex != -1) {
            key = new String(byteArr, startIndex, separatorIndex-startIndex, "utf-8").trim();
            int valueStartIndex = separatorIndex+ patternName.bytelen();
            value = new String(byteArr, valueStartIndex, newLineIndex-valueStartIndex, "utf-8").trim();
        } else {
            if (newLineIndex-startIndex == 2 && byteArr[startIndex] == '\r' && byteArr[startIndex+1] == '\n') {
                return new KeyValueResult(null, null, endIndex+1);
            } else {
                // logger.error("separator not found in the document line - {} " + new String(byteArr, startIndex, newLineIndex - startIndex, "utf-8"));
                // throw new RuntimeException("separator not found in the document line - " + new String(byteArr, startIndex, newLineIndex - startIndex, "utf-8"));
                return new KeyValueResult(null, null, newLineIndex+ patternName.bytelen());
            }
        }

        patternName = getMatchPatternForString(lineEndPattern);
        int nextIndex = newLineIndex+patternName.bytelen();
        return new KeyValueResult(key, value, nextIndex);
    }

    private static final Set<Byte> WHITESPACE_BYTE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList((byte)'\n', (byte)'\t', (byte)'\r', (byte)' ')));
    public static int consumeWhitespace(byte[] byteArr, int startIndex, int endIndex) {
        int i = startIndex;
        for (; i < endIndex; i++) {
            if (!WHITESPACE_BYTE_SET.contains(byteArr[i])) {
                break;
            }
        }
        return i;
    }
}
