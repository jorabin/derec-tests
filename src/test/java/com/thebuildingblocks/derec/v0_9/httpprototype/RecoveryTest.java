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

package com.thebuildingblocks.derec.v0_9.httpprototype;

import com.thebuildingblocks.keypr.sharer.tools.Recipes;
import com.thebuildingblocks.keypr.sharer.tools.TestHelperServer;
import com.thebuildingblocks.keypr.sharer.tools.Notifier;
import com.thebuildingblocks.keypr.common.TestIds;
import org.derecalliance.derec.api.DeRecHelperStatus;
import org.derecalliance.derec.api.DeRecIdentity;
import org.derecalliance.derec.api.DeRecSecret;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.thebuildingblocks.derec.v0_9.httpprototype.Cryptography.keyPairGenerator;
import static com.thebuildingblocks.keypr.common.TestIds.DEFAULT_IDS;
import static com.thebuildingblocks.keypr.common.TestIds.pemFrom;
import static org.junit.Assert.*;

public class RecoveryTest {
    Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Test
    public void ListSecretIdsVersionsTest() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        TestHelperServer server = new TestHelperServer();
        server.startServer(8080, TestIds.DEFAULT_IDS);
        try {
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            String pem = pemFrom(keyPair.getPublic());
            // build a sharer
            Sharer me = Sharer.newBuilder()
                    .id(new DeRecIdentity("Incremental Inge", "mailto:test@example.org", null, pem))
                    .keyPair(keyPair)
                    .notificationListener(Notifier::logNotification)
                    .build();

            // build a secret and wait for results from all
            Secret secret1 = me.newSecret("A test secret", "some secret or other".getBytes(StandardCharsets.UTF_8));
            //secret1.retryParameters.connectTimeout= Duration.ofSeconds(1);
            List<CompletableFuture<? extends DeRecHelperStatus>> futures = secret1.addHelpersAsync(Arrays.asList(DEFAULT_IDS));
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(6, TimeUnit.SECONDS);
            assertEquals(server.getContexts().size(), secret1.getPairedHelpers());
            secret1.update(secret1.getVersions().lastEntry().getValue().getProtectedValue());

            // update and wait for threshold acknowledgements
            secret1.update("a newfangled version".getBytes(StandardCharsets.UTF_8));

            // start a new secret, share and block
            Secret secret2 = me.newSecret("my second secret",
                    "very hush hush".getBytes(StandardCharsets.UTF_8),
                    Arrays.asList(DEFAULT_IDS));


            // secrets should have same helpers
            Set<DeRecIdentity> helpers1 = secret1.helpers.stream()
                    .filter(h -> h.status.equals(DeRecHelperStatus.PairingStatus.PAIRED))
                    .map(HelperClient::getId)
                    .collect(Collectors.toSet());
            Set<DeRecIdentity> helpers2 = secret2.helpers.stream()
                    .filter(h -> h.status.equals(DeRecHelperStatus.PairingStatus.PAIRED))
                    .map(HelperClient::getId)
                    .collect(Collectors.toSet());
            helpers2.removeAll(helpers1);
            assertEquals(0, helpers2.size());

            // loop over helpers
            for (DeRecIdentity identity: helpers1) {
                //get the secrets and versions they hold
                Future<Map<DeRecSecret.Id, List<Integer>>> future = me.getSecretIdsAsync(identity);
                try {
                    // block for result
                    Map<DeRecSecret.Id, List<Integer>> secretIdsVersions = future.get(3, TimeUnit.SECONDS);
                    // check it has secret1
                    assertTrue(secretIdsVersions.containsKey(secret1.getSecretId()));
                    assertEquals(identity.getName(),2, secretIdsVersions.get(secret1.getSecretId()).size());
                    assertArrayEquals(secretIdsVersions.get(secret1.getSecretId()).toArray(),
                            Recipes.listVersionNumbersForHelper(secret1, identity).toArray());

                    assertTrue(secretIdsVersions.containsKey(secret2.getSecretId()));
                    assertEquals(identity.getName(),1, secretIdsVersions.get(secret2.getSecretId()).size());
                    assertArrayEquals(secretIdsVersions.get(secret2.getSecretId()).toArray(), secret2.versions.keySet().toArray());
                    logger.info("Completed {}", identity.getName());
                } catch (ExecutionException | TimeoutException | InterruptedException e) {
                    fail(e.getMessage());
                }
            }
        } finally {
            server.stopServer();
        }
    }
}