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

import derec.message.Derecmessage;
import org.derecalliance.derec.api.DeRecIdentity;
import org.derecalliance.derec.api.DeRecIdentity;
import org.derecalliance.derec.api.DeRecSecret;
import org.derecalliance.derec.api.DeRecSharer;
import org.derecalliance.derec.api.DeRecStatusNotification;

import java.net.http.HttpClient;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class Sharer implements DeRecSharer{
    public static Util.RetryParameters defaultRetryParameters = new Util.RetryParameters();
    // TODO: these should be grouped as "threshold management parameters" or some such
    public static int defaultThresholdRecovery = 3;
    public static int defaultHelpersRequiredForDeletion = 4; // number of helpers that need to confirm a new share

    private final Map<DeRecSecret.Id, Secret> secrets; // a map of secret id to each secret that the sharer wishes to share
    public DeRecIdentity id; // sharer's id
    public KeyPair keyPair; // public/private key pair
    // before deleting an old share
    public X509Certificate certificate; // certificate to use
    private Consumer<DeRecStatusNotification> listener = n -> {}; // do nothing

    /**
     * Hidden constructor
     */
    private Sharer() {
        secrets = new HashMap<>();
    }

    public Secret newSecret(String description, byte[] bytesToProtect) {
        return newSecret(description, bytesToProtect, Collections.emptyList());
    }
    /**
     * Create a new secret with a random UUID as identifier
     *
     * @param bytesToProtect the content of the secret
     * @return a new secret
     */
    @Override
    public Secret newSecret(String description, byte[] bytesToProtect, List<DeRecIdentity> helperIds) {
        UUID secretId = UUID.randomUUID();
        return newSecret(Util.asSecretId(secretId), description, bytesToProtect, helperIds);
    }

    public Secret newSecret(DeRecSecret.Id secretId, String description, byte[] bytesToProtect) {
        return newSecret(secretId, description, bytesToProtect, Collections.emptyList());
    }

    /**
     * Create a new secret with default values for its parameters. Block until paring and update are complete.
     *
     * @param secretId       the id of the secret
     * @param bytesToProtect the content of the secret
     * @return a newly shared secret
     */
    @Override
    public Secret newSecret(DeRecSecret.Id secretId, String description, byte[] bytesToProtect, List<DeRecIdentity> helperIds) {
        if (secrets.containsKey(secretId)) {
            throw new IllegalStateException("Secret with that Id already exists");
        }
        Secret secret = Secret.newBuilder()
                .sharer(this)
                .secretId(secretId)
                .storageRequired(bytesToProtect.length)// assume that secret does not grow in size
                .notificationListener(listener)
                .thresholdSecretRecovery(defaultThresholdRecovery)
                .thresholdForDeletion(defaultHelpersRequiredForDeletion)
                .build();
        secrets.put(secretId, secret);
        // add helpers and block
        secret.addHelpers(helperIds);
        // share secret and block
        secret.update(bytesToProtect, description);
        return secret;
    }

    @Override
    public Secret getSecret(DeRecSecret.Id secretId) {
        return secrets.get(secretId);
    }

    @Override
    public List<Secret> getSecrets() {
        return secrets.values().stream().toList();
    }

    @Override
    public Future<Map<DeRecSecret.Id, List<Integer>>> getSecretIdsAsync(DeRecIdentity deRecHelperInfo) {
        return Recovery.getSecretIds(this.id, deRecHelperInfo, listener);
    }

    @Override
    public DeRecSecret recoverSecret(DeRecSecret.Id id, int version, List<? extends DeRecIdentity> list) {
        return null;
    }

    @Override
    public void setListener(Consumer<DeRecStatusNotification> listener) {
        this.listener = listener;
    }

    public void close() {
        for (Secret secret: secrets.values()) {
            secret.close();
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder for a sharer
     */
    public static class Builder {
        private final Sharer sharer = new Sharer();

        public Builder id(DeRecIdentity id) {
            sharer.id = id;
            return this;
        }

        public Builder keyPair(KeyPair keyPair) {
            sharer.keyPair = keyPair;
            return this;
        }

        public Builder x509Certificate(X509Certificate x509Certificate) {
            sharer.certificate = x509Certificate;
            return this;
        }

        public Builder notificationListener(Consumer<DeRecStatusNotification> listener) {
            sharer.listener = listener;
            return this;
        }

        public Sharer build() {
            return sharer;
        }
    }
}
