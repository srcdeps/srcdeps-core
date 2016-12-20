/**
 * Copyright 2015-2016 Maven Source Dependencies
 * Plugin contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.srcdeps.core.config.scalar;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A simple time value-unit combo that we have to model ourselves as long as we are on Java 7.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public final class Duration {

    private static final Duration MAX_VALUE = new Duration(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

    private static final Map<TimeUnit, String> UNIT_TO_STRING_MAP;
    static {
        EnumMap<TimeUnit, String> unitToString = new EnumMap<>(TimeUnit.class);
        unitToString.put(TimeUnit.NANOSECONDS, "ns");
        unitToString.put(TimeUnit.MICROSECONDS, "us");
        unitToString.put(TimeUnit.MILLISECONDS, "ms");
        unitToString.put(TimeUnit.SECONDS, "s");
        unitToString.put(TimeUnit.MINUTES, "m");
        unitToString.put(TimeUnit.HOURS, "h");
        unitToString.put(TimeUnit.DAYS, "d");
        UNIT_TO_STRING_MAP = Collections.unmodifiableMap(unitToString);
    }

    /**
     * @return {@link Long#MAX_VALUE} milliseconds.
     */
    public static Duration maxValue() {
        return MAX_VALUE;
    }

    /**
     * @param rawDuration
     *            a string consisting of a number and unit abbreviation. Valid unit abbreviations are liste in
     *            {@link #UNIT_TO_STRING_MAP}.
     * @return a new {@link Duration} parsed out of the given {@code rawDuration}
     * @throws IllegalArgumentException on any parse problems
     */
    public static Duration of(String rawDuration) {
        if (rawDuration == null) {
            return null;
        }
        if (rawDuration.length() <= 1) {
            throw new IllegalArgumentException(
                    String.format("Cannot parse a %s out of string [%s]: must be longer than 1",
                            Duration.class.getSimpleName(), rawDuration));
        }
        int len = rawDuration.length();
        char lastChar = rawDuration.charAt(len - 1);
        try {
            switch (lastChar) {
            case 's':
                char lastButOneChar = rawDuration.charAt(len - 2);
                switch (lastButOneChar) {
                case 'n':
                    return new Duration(Long.parseLong(rawDuration.substring(0, len - 2)), TimeUnit.NANOSECONDS);
                case 'u':
                    return new Duration(Long.parseLong(rawDuration.substring(0, len - 2)), TimeUnit.MICROSECONDS);
                case 'm':
                    return new Duration(Long.parseLong(rawDuration.substring(0, len - 2)), TimeUnit.MILLISECONDS);
                default:
                    return new Duration(Long.parseLong(rawDuration.substring(0, len - 1)), TimeUnit.SECONDS);
                }
            case 'm':
                return new Duration(Long.parseLong(rawDuration.substring(0, len - 1)), TimeUnit.MINUTES);
            case 'h':
                return new Duration(Long.parseLong(rawDuration.substring(0, len - 1)), TimeUnit.HOURS);
            case 'd':
                return new Duration(Long.parseLong(rawDuration.substring(0, len - 1)), TimeUnit.DAYS);
            default:
                throw new IllegalArgumentException(
                        String.format("Cannot parse a %s out of string [%s]: must end with any of time units [%s]",
                                Duration.class.getSimpleName(), rawDuration, UNIT_TO_STRING_MAP.values()));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format(
                    "Cannot parse a %s out of string [%s]: must consist of a number appended with any of time units [%s]",
                    Duration.class.getSimpleName(), rawDuration, UNIT_TO_STRING_MAP.values()));
        }
    }

    private final TimeUnit unit;
    private final long value;

    public Duration(long value, TimeUnit unit) {
        super();
        this.value = value;
        this.unit = unit;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Duration other = (Duration) obj;
        if (unit != other.unit)
            return false;
        if (value != other.value)
            return false;
        return true;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    public long getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((unit == null) ? 0 : unit.hashCode());
        result = prime * result + (int) (value ^ (value >>> 32));
        return result;
    }

    /**
     * @return {@code unit.toMillis(value)}
     */
    public long toMilliseconds() {
        return unit.toMillis(value);
    }

    @Override
    public String toString() {
        return value + UNIT_TO_STRING_MAP.get(unit);
    }

}
