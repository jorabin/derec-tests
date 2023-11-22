/*
 * Copyright (c) 2023 The Building Blocks Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thebuildingblocks.keypr.common;


import org.derecalliance.derec.api.DeRecSecret;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * placeholder for stuff needed in a few places
 */

public class Util {

    public static final List<ProtocolVersion> availableVersions = List.of(new ProtocolVersion(0, 9));

    public static UUID asUuid(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }

    public static UUID asUuid(DeRecSecret.Id id) {
        return asUuid(id.getBytes());
    }

    public static byte[] asBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static DeRecSecret.Id asSecretId(UUID uuid) {
        return new DeRecSecret.Id(asBytes(uuid));
    }

    public static <V extends Collection<W>, W> Map<UUID, V> asUuidMap(Map<DeRecSecret.Id, V> secretIdMap) {
        return secretIdMap.entrySet().stream().collect(Collectors.toMap(e -> asUuid(e.getKey()),
                Map.Entry::getValue,
                (a, b) -> {
                    a.addAll(b);
                    return a;
                }));
    }

    /**
     * Placeholder for some way of describing and agreeing retry parameters
     */
    public static class RetryParameters {
        public static RetryParameters DEFAULT = new RetryParameters();
        public long pairingWaitSecs = 5; // time to wait for pairings to complete or fail
        int maxRetries = 0; // don't retry on failure
        public Duration timeout = Duration.ofSeconds(5); // timeout if no response received
        public Duration connectTimeout = Duration.ofSeconds(5);
        public Duration reverification = Duration.ofSeconds(20); // re-verify every
        public Duration updateDelay = Duration.ofSeconds(30); // max update frequency

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public Duration getResponseTimeout() {
            return timeout;
        }
    }

    /**
     * Placeholder for some way of counting retries
     */
    public static class RetryStatus {

    }

    public static class ProtocolVersion {
        int majorVersion;
        int minorVersion;

        public ProtocolVersion(int majorVersion, int minorVersion) {
            this.majorVersion = majorVersion;
            this.minorVersion = minorVersion;
        }
    }
}
