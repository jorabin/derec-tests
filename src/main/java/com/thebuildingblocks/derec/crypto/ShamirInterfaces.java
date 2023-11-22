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

package com.thebuildingblocks.derec.crypto;

import org.derecalliance.derec.api.DeRecSecret;

import java.security.SecureRandom;
import java.util.List;

public interface ShamirInterfaces {

    interface SplitterFactory {
        /**
         * create a new Shamir splitter
         * @param random a random number generator
         * @param count the number of shares to produce
         * @param threshold the recombination threshold
         * @return a splitter
         */
        Splitter newSplitter(SecureRandom random, int count, int threshold);
    }

    interface Splitter {
        /**
         * Split a secret according to the parameters established
         * @param id a secret id
         * @param version a version
         * @param secret some bytes
         * @return a list of shares suitable for redistribution
         */
        List<byte[]> split(DeRecSecret.Id id, int version, byte[] secret);

        byte[] combine(DeRecSecret.Id id, int version, List<byte[]> shares);
    }
}
