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

package com.thebuildingblocks.keypr.sharer;

import com.thebuildingblocks.keypr.common.Util;
import org.derecalliance.derec.api.DeRecIdentity;
import org.derecalliance.derec.api.DeRecHelperStatus;
import org.derecalliance.derec.api.DeRecStatusNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import static com.thebuildingblocks.keypr.sharer.HelperClientMessageFactory.*;
import static com.thebuildingblocks.keypr.sharer.HelperClientResponseProcessing.*;
import static com.thebuildingblocks.keypr.sharer.Version.ResultType.SHARE;
import static com.thebuildingblocks.keypr.sharer.Version.ResultType.VERIFY;
import static org.derecalliance.derec.api.DeRecHelperStatus.PairingStatus.*;
import static org.derecalliance.derec.api.DeRecStatusNotification.StandardNotificationType.*;

/**
 * Sharer's view of a helper for a single secret, there will be multiple entries for the
 * same helper - one for each secret shared to that helper
 */
public class HelperClient implements DeRecHelperStatus, Closeable {
    public final Secret secret; // the secret this helper is a helper for
    private final Util.RetryParameters retryParameters;
    private final HttpClient httpClient;
    private final DeRecIdentity helperId; // unique Id for helper
    URI tsAndCs;    // link to legal conditions regarding what the helper is to do about
    // authentication for recovery and substitution of sharer
    PublicKey publicKey; // public key for the helper (for this secret)
    X509Certificate certificate; // The helper's certificate
    String protocolVersion; // accepted protocol version
    PairingStatus status = PairingStatus.NONE; // pairing not yet attempted

    // a list of the shares sent to this helper - this is basically
    // a filtered view of secret.versions for this helper
    NavigableMap<Integer, Version.Share> shares = Collections.synchronizedNavigableMap(new TreeMap<>());
    CompletableFuture<HelperClient> pairingFuture; // awaits completion of pairing or unpairing
    BiConsumer<DeRecStatusNotification.StandardNotificationType, String> notifier;
    Logger logger = LoggerFactory.getLogger(this.getClass());
    ProtocolInfo protocolInfo;

    HelperClient(Secret secret, DeRecIdentity helperId, HttpClient httpClient, Util.RetryParameters retryParameters) {
        this.secret = secret;
        this.helperId = helperId;
        this.httpClient = httpClient;
        this.retryParameters = retryParameters;
        this.notifier = (t, s) -> this.secret.notifyStatus(Notification.newBuilder()
                        .secret(secret)
                        .helper(this)
                        .message(s)
                        .build(t));
        this.protocolInfo = ProtocolInfo.newBuilder()
                .secretId(secret.getSecretId())
                .helperPublicKeyDigest(helperId.getPublicKeyDigest())
                .sharerPublicKeyDigest(secret.sharer.id.getPublicKeyDigest())
                .build();
    }

    // convenience function to build requests consistently
    HttpRequest.Builder buildRequest() {
        return HttpRequest.newBuilder()
                .uri(helperId.getAddress())
                .timeout(retryParameters.getResponseTimeout());
    }



    /**
     * Initiate pairing with this helper
     */
    public void pair() {
        synchronized (this) {
            if (!status.equals(PairingStatus.NONE) && !status.equals(FAILED)) {
                throw new IllegalStateException(String.format("Cannot pair a helper with status %s", status));
            }
            status = PairingStatus.INVITED;
        }

        HttpRequest request = buildRequest()
                .POST(BodyPublishers.ofByteArray(getMessage(this.protocolInfo,
                        getPairRequestMessageBody(helperId)).toByteArray()))
                .build();

        pairingFuture = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(HttpClientHelper.httpStatusChecker)
                .thenApply(r -> pairProcessResponse(r, this))
                .exceptionally(t -> {
                    this.status = FAILED;
                    notifier.accept(HELPER_NOT_PAIRED, HttpClientHelper.getMessageForException(t));
                    return this;
                });
    }

    public void send(Version.Share share) {
        synchronized (this) {
            if (!status.equals(PAIRED)) {
                throw new IllegalStateException("Helper must be paired to share");
            }
            shares.put(share.version.versionNumber, share);
        }

        HttpRequest request = buildRequest()
                .POST(BodyPublishers.ofByteArray(getMessage(this.protocolInfo,
                        getShareRequestMessageBody(share)).toByteArray()))
                .build();

        if (Objects.isNull(share.helper)) {
            throw new IllegalStateException("Share helper must not be null");
        }
        if (Objects.nonNull(share.future)) {
            share.future.cancel(true);
        }

        share.future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(HttpClientHelper.httpStatusChecker)
                .thenApply(inputStream -> HelperClientResponseProcessing.storeShareResponseHandler(inputStream, share))
                .exceptionally(throwable -> {
                    share.processResult(SHARE, false, HttpClientHelper.getMessageForException(throwable));
                    return share;
                });
    }

    public void verify(Version.Share share) {
        synchronized (this) {
            if (!status.equals(PAIRED)) {
                throw new IllegalStateException("Helper must be paired to share");
            }
            if (!share.isShared) {
                throw new IllegalStateException("Share must have been shared to verify");
            }
            if (!shares.containsKey(share.version.versionNumber)) {
                throw new IllegalStateException("Share must have been shared to verify");
            }
        }

        HttpRequest request = buildRequest()
                .POST(BodyPublishers.ofByteArray(getMessage(this.protocolInfo, getVerifyRequestMessageBody(share)).toByteArray()))
                .build();

        share.future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(HttpClientHelper.httpStatusChecker)
                .thenApply(inputStream -> HelperClientResponseProcessing.verifyResponseHandler(inputStream, share))
                .exceptionally(throwable -> {
                    share.processResult(VERIFY, false, HttpClientHelper.getMessageForException(throwable));
                    return share;
                });
    }

    /**
     * Remove pairing with this helper
     */
    public void unPair(String reason) {
        synchronized (this) {
            if (!status.equals(PAIRED)) {
                // todo need to cancel an in progress pairing
                throw new IllegalStateException("Cannot unpair an unpaired helper");
            }
            status = PairingStatus.PENDING_REMOVAL;
        }

        HttpRequest request = buildRequest()
                .POST(BodyPublishers.ofByteArray(getMessage(this.protocolInfo, getUnPairRequestMessageBody(reason)).toByteArray()))
                .build();


        pairingFuture = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(HttpClientHelper.httpStatusChecker)
                .thenApply(r -> unPairProcessResponse(r, this))
                .exceptionally(t -> {
                    this.status = FAILED;
                    notifier.accept(HELPER_UNPAIRED, HttpClientHelper.getMessageForException(t));
                    return this;
                });
    }

    public void close() {
        if (status.equals(PAIRED)) {
            unPair("Helper Client is Closing");
        }
        try {
            pairingFuture.get(secret.retryParameters.pairingWaitSecs, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Error unpairing from {}", helperId.getName(), e);
        }
    }

    @Override
    public DeRecIdentity getId() {
        return helperId;
    }

    @Override
    public PairingStatus getStatus() {
        return status;
    }
}
