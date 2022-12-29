package com.resonance.letsdata.data.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

public class StringMatchPattern {
    private static final Logger logger = LoggerFactory.getLogger(StringMatchPattern.class);

    private final String pattern;

    // Boyer Moore matching on byte arrays - see match function code: https://github.com/samskivert/ikvm-openjdk/blob/master/build/linux-amd64/impsrc/com/sun/xml/internal/org/jvnet/mimepull/MIMEParser.java
    private final byte[] patternBytes;
    private final int[] patternBCS;                // Boyer Moore algo: Bad Character Shift table
    private final int[] patternGSS;                // Boyer Moore algo: Good Suffix Shift table

    public StringMatchPattern(String pattern) {
        this.pattern = pattern;
        try {
            this.patternBytes = pattern.getBytes("utf-8");
        } catch (UnsupportedEncodingException usee) {
            logger.error("exception in getting bytes for the pattern "+pattern, usee);
            throw new RuntimeException("exception in getting bytes for the pattern", usee);
        }

        this.patternBCS = new int[128];
        this.patternGSS = new int[patternBytes.length];
        compilePattern();
    }

    /**
     * Boyer-Moore search method. Copied from java.util.regex.Pattern.java
     *
     * Pre calculates arrays needed to generate the bad character
     * shift and the good suffix shift. Only the last seven bits
     * are used to see if chars match; This keeps the tables small
     * and covers the heavily used ASCII range, but occasionally
     * results in an aliased match for the bad character shift.
     */
    private void compilePattern() {
        int i, j;

        // Precalculate part of the bad character shift
        // It is a table for where in the pattern each
        // lower 7-bit value occurs
        for (i = 0; i < patternBytes.length; i++) {
            patternBCS[patternBytes[i]&0x7F] = i + 1;
        }

        // Precalculate the good suffix shift
        // i is the shift amount being considered
        NEXT:   for (i = patternBytes.length; i > 0; i--) {
            // j is the beginning index of suffix being considered
            for (j = patternBytes.length - 1; j >= i; j--) {
                // Testing for good suffix
                if (patternBytes[j] == patternBytes[j-i]) {
                    // src[j..len] is a good suffix
                    patternGSS[j-1] = i;
                } else {
                    // No match. The array has already been
                    // filled up with correct values before.
                    continue NEXT;
                }
            }
            // This fills up the remaining of optoSft
            // any suffix can not have larger shift amount
            // then its sub-suffix. Why???
            while (j > 0) {
                patternGSS[--j] = i;
            }
        }
        // Set the guard value because of unicode compression
        patternGSS[patternBytes.length -1] = 1;
    }

    public String getPattern() {
        return pattern;
    }

    public int strlen() {
        return pattern.length();
    }

    public int bytelen() {
        return patternBytes.length;
    }

    public byte[] getPatternBytes() {
        return patternBytes;
    }

    public int[] getPatternBCS() {
        return patternBCS;
    }

    public int[] getPatternGSS() {
        return patternGSS;
    }
}
