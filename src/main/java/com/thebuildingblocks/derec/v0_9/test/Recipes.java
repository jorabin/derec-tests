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

package com.thebuildingblocks.derec.v0_9.test;

import com.thebuildingblocks.derec.v0_9.httpprototype.Secret;
import com.thebuildingblocks.derec.v0_9.httpprototype.Sharer;
import com.thebuildingblocks.derec.v0_9.httpprototype.Version;
import com.thebuildingblocks.derec.v0_9.httpprototype.HelperClient;
import org.derecalliance.derec.api.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * utilities for doing useful things
 */
public class Recipes {

    /**
     * Get a printable helpers and their status for a Secret
     * @return a printable list
     */
    public static String listHelpersForSecretAsString(DeRecSecret secret) {
        return secret.getHelpers().stream()
                .map(h -> h.getId().getName() + ": " + h.getStatus().name())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Get a list of versions of a secret
     */
    public static String listVersionsForSecretAsString(DeRecSecret secret) {
        return secret.getVersions().entrySet().stream().
                map(e -> e.getKey() + ": " + e.getValue().isProtected()).
                collect(Collectors.joining("\n"));
    }

    /**
     * Get a list of versions of a secret that a helper has a share of
     */
    public static List<Version> listVersionsForHelper(Secret secret, DeRecIdentity identity) {
        List<Version> result = new ArrayList<>();
        for (DeRecVersion version: secret.getVersions().values()) {
            for (Version.Share share: ((Version) version).getShares()){
                if (share.getHelper().getId().equals(identity)) {
                    result.add((Version) version);
                }
            }
        }
        return result;
    }


    public static List<Integer> listVersionNumbersForHelper(Secret secret, DeRecIdentity identity) {
        return listVersionsForHelper(secret, identity).stream().map(Version::getVersionNumber).toList();
    }

    /**
     * Get a list of all helper ids and the secrets they protect
     */
    public static Map<DeRecIdentity, List<Secret>> listHelpersAndSecretsForSharer(Sharer sharer) {
        return sharer.getSecrets().stream()
                .flatMap(s -> s.getHelpers().stream())
                .collect(Collectors.groupingBy(HelperClient::getId,
                        Collectors.mapping(c -> c.secret, Collectors.toList())));
    }

    /**
     * Get a list of all helper ids and the secrets they protect (ole fashioned way)
     */
    public static Map<DeRecIdentity, List<DeRecSecret>> listHelpersAndSecretsForSharer2(DeRecSharer sharer){
    // create a map of DeRecId and List<Secret>
        final Map<DeRecIdentity, List<DeRecSecret>> secretMap = new HashMap<>();
        // populate the map
        sharer.getSecrets()
                .forEach(s -> s.getHelpers()
                        .forEach(h -> secretMap.computeIfAbsent(h.getId(), __ -> new ArrayList<>()).add(s))
                );
        return secretMap;
    }
}
