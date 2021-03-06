/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.brooklyn.util.text;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

public class Identifiers {
    
    private static Random random = new Random();

    public static final String UPPER_CASE_ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String LOWER_CASE_ALPHA = "abcdefghijklmnopqrstuvwxyz";
    public static final String NUMERIC = "1234567890";
    public static final String NON_ALPHA_NUMERIC = "!@$%^&*()-_=+[]{};:\\|/?,.<>~";

    /** @see #JAVA_GOOD_PACKAGE_OR_CLASS_REGEX */
    public static final String JAVA_GOOD_START_CHARS = UPPER_CASE_ALPHA + LOWER_CASE_ALPHA + "_";
    /** @see #JAVA_GOOD_PACKAGE_OR_CLASS_REGEX */
    public static final String JAVA_GOOD_NONSTART_CHARS = JAVA_GOOD_START_CHARS + NUMERIC;
    /** @see #JAVA_GOOD_PACKAGE_OR_CLASS_REGEX */ 
    public static final String JAVA_GOOD_SEGMENT_REGEX = "["+JAVA_GOOD_START_CHARS+"]"+"["+JAVA_GOOD_NONSTART_CHARS+"]*";
    /** regex for a java package or class name using "good" chars, that is no accents or funny unicodes.
     * see http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.8 for the full set supported by the spec;
     * but it's rare to deviate from this subset and it causes problems when charsets aren't respected (tsk tsk but not uncommon!).
     * our use cases so far only require testing for "good" names. */
    public static final String JAVA_GOOD_PACKAGE_OR_CLASS_REGEX = "("+JAVA_GOOD_SEGMENT_REGEX+"\\."+")*"+JAVA_GOOD_SEGMENT_REGEX;
    /** as {@link #JAVA_GOOD_PACKAGE_OR_CLASS_REGEX} but allowing a dollar sign inside a class name (e.g. Foo$1) */
    public static final String JAVA_GOOD_BINARY_REGEX = JAVA_GOOD_PACKAGE_OR_CLASS_REGEX+"(\\$["+JAVA_GOOD_NONSTART_CHARS+"]+)*";

    public static final String JAVA_GENERATED_IDENTIFIER_START_CHARS = UPPER_CASE_ALPHA + LOWER_CASE_ALPHA;

    public static final String JAVA_GENERATED_IDENTIFIERNONSTART_CHARS = JAVA_GENERATED_IDENTIFIER_START_CHARS + NUMERIC;

    public static final String BASE64_VALID_CHARS = JAVA_GENERATED_IDENTIFIERNONSTART_CHARS + "+=";

    public static final String ID_VALID_START_CHARS = UPPER_CASE_ALPHA + LOWER_CASE_ALPHA;
    public static final String ID_VALID_NONSTART_CHARS = ID_VALID_START_CHARS + NUMERIC;

    public static final String PASSWORD_VALID_CHARS = NON_ALPHA_NUMERIC + ID_VALID_NONSTART_CHARS;

    // We only create a secure random when it is first used
    private static Random secureRandom = null;

    /** makes a random id string (letters and numbers) of the given length;
     * starts with letter (upper or lower) so can be used as java-id;
     * tests ensure random distribution, so random ID of length 5 
     * is about 2^29 possibilities 
     * <p>
     * With ID of length 4 it is not unlikely (15% chance) to get
     * duplicates in the first 2000 attempts.
     * With ID of length 8 there is 1% chance to get duplicates
     * in the first 1M attempts and 50% for the first 16M.
     * <p>
     * implementation is efficient, uses char array, and 
     * makes one call to random per 5 chars; makeRandomId(5)
     * takes about 4 times as long as a simple Math.random call,
     * or about 50 times more than a simple x++ instruction;
     * in other words, it's appropriate for contexts where random id's are needed,
     * but use efficiently (ie cache it per object), and 
     * prefer to use a counter where feasible
     * <p>
     * in general this is preferable to base64 as is more portable,
     * can be used throughout javascript (as ID's which don't allow +)
     * or as java identifiers (which don't allow numbers in the first char)
     * <p>
     * <b>NOTE</b> This version is 30-50% faster than the old double-based one,
     * which computed a random every 3 turns -- takes about 600 ns to do id
     * of len 10, compared to 10000 ns for old version [on 1.6ghz machine]
     * <p>
     * <b>TODO</b> The integer value passed to {@link Randonm#nextInt(int)}
     * will overflow if the length of the character sets passed in is more
     * than 128. It is possible to mitigate this by truncating the strings,
     * or calculating the maximum number of characters per invocation of
     * {@code nextInt()} by taking the logarithm of {@link Integer#MAX_INT}
     * using the length of the character set as the base. Currently this
     * method is private to prevent overly long arguments.
     */
    private static String makeRandomId(int l, String validStartChars, String validNonStartChars) {
        if (l <= 0) return "";
        char[] id = new char[l];
        int s = validStartChars.length();
        int n = validNonStartChars.length();
        int d = random.nextInt(s * n * n * n);
        int i = 0;
        id[i] = validStartChars.charAt(d % s);
        d /= s;
        if (++i < l) do {
            id[i] = validNonStartChars.charAt(d % n);
            if (++i >= l) break;
            if (i % 4 == 0) {
                d = random.nextInt(n * n * n *n);
            } else {
                d /= n;
            }
        } while (true);
        return new String(id);
    }
    public static String makeRandomId(int l) {
        return makeRandomId(l, ID_VALID_START_CHARS, ID_VALID_NONSTART_CHARS);
    }
    public static String makeRandomLowercaseId(int l) {
        return makeRandomId(l, LOWER_CASE_ALPHA, LOWER_CASE_ALPHA + NUMERIC);
    }

    /**
     *
     * @param length of password to be returned
     * @return randomly generated password containing at least one of each upper case,
     * lower case, numeric, and non alpha-numeric characters.  Hopefully this is acceptible
     * for most password schemes.
     */
    public static String makeRandomPassword(final int length) {
        return makeRandomPassword(length, UPPER_CASE_ALPHA, LOWER_CASE_ALPHA, NUMERIC, NON_ALPHA_NUMERIC, PASSWORD_VALID_CHARS);
    }

    /**
     * A fairly slow but hopefully secure way to randomly select characters for a password
     * Takes a pool of acceptible characters using the first set in the pool for the first character,
     * second set for the second character, ..., nth set for all remaining character.
     *
     * @param length length of password
     * @param passwordValidCharsPool pool of acceptable character sets
     * @return a randomly generated password
     */
    public static String makeRandomPassword(final int length, String... passwordValidCharsPool) {
        Preconditions.checkState(length >= passwordValidCharsPool.length);
        int l = 0;

        Character[] password = new Character[length];

        for(int i = 0; i < passwordValidCharsPool.length; i++){
            password[l++] = pickRandomCharFrom(passwordValidCharsPool[i]);
        }

        String remainingValidChars = mergeCharacterSets(passwordValidCharsPool);

        while(l < length) {
            password[l++] = pickRandomCharFrom(remainingValidChars);
        }

        List<Character> list = Arrays.asList(password);
        Collections.shuffle(list);
        return Joiner.on("").join(list);
    }

    protected static String mergeCharacterSets(String... s) {
        Set characters = new HashSet<Character>();
        for (String characterSet : s) {
            characters.addAll(Arrays.asList(characterSet.split("")));
        }

        return Joiner.on("").join(characters);
    }

    /** creates a short identifier comfortable in java and OS's, given an input hash code
     * <p>
     * result is always at least of length 1, shorter if the hash is smaller */
    public static String makeIdFromHash(long d) {
        StringBuffer result = new StringBuffer();
        if (d<0) d=-d;
        // correction for Long.MIN_VALUE
        if (d<0) d=-(d+1000);

        result.append(ID_VALID_START_CHARS.charAt((int)(d % (26+26))));
        d /= (26+26);
        while (d!=0) {
            result.append(ID_VALID_NONSTART_CHARS.charAt((int)(d%(26+26+10))));
            d /= (26+26+10);
        }
        return result.toString();
    }

    /**
     * Makes a random id string (letters and numbers) of the given length;
     * starts with letter (upper or lower) so can be used as Java id.
     */
    public static String makeRandomJavaId(int l) {
        return makeRandomId(l, JAVA_GENERATED_IDENTIFIER_START_CHARS, JAVA_GENERATED_IDENTIFIERNONSTART_CHARS);
    }

    public static double randomDouble() {
        return random.nextDouble();
    }

    public static long randomLong() {
        return random.nextLong();
    }
    public static boolean randomBoolean() {
        return random.nextBoolean();
    }
    public static int randomInt() {
        return random.nextInt();
    }
    /** returns in [0,upbound) */
    public static int randomInt(int upbound) {
        return random.nextInt(upbound);
    }
    /** returns the array passed in */
    public static byte[] randomBytes(byte[] buf) {
        random.nextBytes(buf);
        return buf;
    }
    public static byte[] randomBytes(int length) {
        byte[] buf = new byte[length];
        return randomBytes(buf);
    }
    public static String makeRandomBase64Id(int length) {
        StringBuilder s = new StringBuilder();
        while (length>0) {
            appendBase64IdFromValueOfLength(randomLong(), length>10 ? 10 : length, s);
            length -= 10;
        }
        return s.toString();
    }

    public static String getBase64IdFromValue(long value) {
        return getBase64IdFromValue(value, 10);
    }
    public static String getBase64IdFromValue(long value, int length) {
        StringBuilder s = new StringBuilder();
        appendBase64IdFromValueOfLength(value, length, s);
        return s.toString();
    }
    public static void appendBase64IdFromValueOfLength(long value, int length, StringBuffer sb) {
        if (length>11)
            throw new IllegalArgumentException("can't get a Base64 string longer than 11 chars from a long");
        long idx = value;
        for (int i=0; i<length; i++) {
            byte x = (byte)(idx & 63);
            sb.append(BASE64_VALID_CHARS.charAt(x));
            idx = idx >> 6;
        }
    }
    public static void appendBase64IdFromValueOfLength(long value, int length, StringBuilder sb) {
        if (length>11)
            throw new IllegalArgumentException("can't get a Base64 string longer than 11 chars from a long");
        long idx = value;
        for (int i=0; i<length; i++) {
            byte x = (byte)(idx & 63);
            sb.append(BASE64_VALID_CHARS.charAt(x));
            idx = idx >> 6;
        }
    }
    public static boolean isValidToken(String token, String validStartChars, String validSubsequentChars) {
        if (token==null || token.length()==0) return false;
        if (validStartChars.indexOf(token.charAt(0))==-1) return false;
        for (int i=1; i<token.length(); i++)
            if (validSubsequentChars.indexOf(token.charAt(i))==-1) return false;
        return true;
    }

    private static char pickRandomCharFrom(String validChars) {
        return validChars.charAt(getSecureRandom().nextInt(validChars.length()));
    }

    private static Random getSecureRandom() {
        if (secureRandom == null) {
            secureRandom = new SecureRandom();
        }
        return secureRandom;
    }
}
