package com.resonance.letsdata.data.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class StringFunctions {
    private static final Logger logger = LoggerFactory.getLogger(StringFunctions.class);
    public static void validateStringIsNotBlank(String input, String keyName) {
        if (StringUtils.isBlank(input)) {
            logger.error("validateStringIsNotBlank failure - "+keyName+", value: "+input);
            throw new RuntimeException("input string is blank - keyName: "+keyName);
        }
    }

    public static void validateStringsAreEqual(String lhs, String rhs, String keyName) {
        if (!StringUtils.equals(lhs, rhs)) {
            logger.error("validateStringsAreEqual failure - "+keyName+", lhs: "+lhs+", rhs: "+rhs);
            throw new RuntimeException("validateStringsAreEqual is false - keyName: "+keyName);
        }
    }

    public static void validateStringsAreASCIIEqual(String lhs, String rhs, String keyName) {
        if (!StringUtils.equals(lhs, rhs)) {
            if (!StringUtils.equals(stringToASCII(lhs), stringToASCII(rhs))) {
                logger.error("validateStringsAreEqual failure - " + keyName + ", lhs: " + lhs + ", rhs: " + rhs);
                throw new RuntimeException("validateStringsAreEqual is false - keyName: " + keyName);
            }
        }
    }

    private static String stringToASCII(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++)
        {
            char curr = input.charAt(i);
            // https://lwp.interglacial.com/appf_01.htm
            if (curr <= 0x7F) {
                sb.append(curr);
            }
        }
        return sb.toString();
    }

    public static void validateStringsAreEqualIgnoreCase(String lhs, String rhs, String keyName) {
        if (!StringUtils.equalsIgnoreCase(lhs, rhs)) {
            logger.error("validateStringsAreEqualIgnoreCase failure - "+keyName+", lhs: "+lhs+", rhs: "+rhs);
            throw new RuntimeException("validateStringsAreEqual is false - keyName: "+keyName);
        }
    }

    public static boolean equalsIgnoreCase(String lhs, String rhs) {
        return StringUtils.equalsIgnoreCase(lhs, rhs);
    }

    private static final Set<Character> WHITESPACE_CHARACTER_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList('\n', '\t', '\r', ' ')));
    public static boolean isRecordEmpty(StringBuilder sb) {
        for (int i = 0; i < sb.length(); i++) {
            if (!WHITESPACE_CHARACTER_SET.contains(sb.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static int consumeWhitespace(StringBuilder sb, int startIndex) {
        int i = startIndex;
        for (; i < sb.length(); i++) {
            if (!WHITESPACE_CHARACTER_SET.contains(sb.charAt(i))) {
                break;
            }
        }
        return i;
    }
}
