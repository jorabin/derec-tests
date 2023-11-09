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

import com.thebuildingblocks.derec.v0_9.httpprototype.HelperClientMessageFactory.ProtocolInfo;
import org.derecalliance.derec.protobuf.Derecmessage.DeRecMessage;
import org.derecalliance.derec.protobuf.ResultOuterClass;
import org.derecalliance.derec.protobuf.Secretidsversions.GetSecretIdsVersionsResponseMessage;
import org.derecalliance.derec.api.DeRecIdentity;
import org.derecalliance.derec.api.DeRecSecret;
import org.derecalliance.derec.api.DeRecStatusNotification;


import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static org.derecalliance.derec.api.DeRecStatusNotification.NotificationSeverity.NORMAL;
import static org.derecalliance.derec.api.DeRecStatusNotification.NotificationSeverity.WARNING;
import static org.derecalliance.derec.api.DeRecStatusNotification.StandardNotificationType.*;

/**
 * Functionality to get a list of secrets and versions from a helper.
 */
public class Recovery {
    /**
     * Get a list of secrets belonging to the named sharer from the named helper.
     * @param sharerInfo sharer
     * @param helperInfo helper
     * @param listener a listener for success/fail events
     * @return a Future containing a Map of secret ids to corresponding versions
     */
    public static Future<Map<DeRecSecret.Id, List<Integer>>> getSecretIds(
            DeRecIdentity sharerInfo,
            DeRecIdentity helperInfo,
            Consumer<DeRecStatusNotification> listener) {

        // protocol info describing the sharer and helper
        ProtocolInfo protocolInfo = ProtocolInfo.newBuilder()
                .sharerPublicKeyDigest(sharerInfo.getPublicKeyDigest())
                .helperPublicKeyDigest(helperInfo.getPublicKeyDigest())
                .build();

        // build the request message
        DeRecMessage message = HelperClientMessageFactory.getMessage(protocolInfo,
                HelperClientMessageFactory.getSecretsIdsVersionsMessageBody());

        // build the request
        HttpRequest httpRequest =
                HttpRequest.newBuilder()
                        .uri(helperInfo.getAddress())
                        .POST(HttpRequest.BodyPublishers.ofByteArray(message.toByteArray()))
                        .build();

        // the result of the operation
        Map<DeRecSecret.Id, List<Integer>> result = new HashMap<>();

        // build an HttpClient (TODO ideally we would share the client between multiple requests)
        HttpClient httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(Util.RetryParameters.DEFAULT.getConnectTimeout())
                        .build();

        // send the request
        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofInputStream())
                // check the status, throw exception if not 200,n exception is caught below
                .thenApply(HttpHelper.httpStatusChecker)
                // process the input stream which is the response message
                .thenApply(r -> {
                    // deserialize the message
                    HelperClientMessageDeserializer deserializer = HelperClientMessageDeserializer.newInstance(r,
                            DeRecMessage.HelperMessageBody.BodyCase.GETSECRETIDSVERSIONSRESPONSEMESSAGE);
                    // get the response (todo handle if not a response)
                    GetSecretIdsVersionsResponseMessage response =
                            deserializer.getBody().getGetSecretIdsVersionsResponseMessage();
                    // report bad status, return empty list (todo should this be a failure on the future)
                    if (!response.getResult().getStatus().equals(ResultOuterClass.StatusEnum.OK)) {
                        listener.accept(Notification.newBuilder()
                                .severity(WARNING)
                                .message(response.getResult().getMemo())
                                .build(LIST_SECRET_FAILED));
                        return result;
                    }
                    // loop over the secrets and put in map
                    for (GetSecretIdsVersionsResponseMessage.VersionList versions : response.getSecretListList()) {
                        result.put(new DeRecSecret.Id(versions.getSecretId().toByteArray()), new ArrayList<>(versions.getVersionsList()));
                    }
                    // report success
                    listener.accept(Notification.newBuilder()
                            .secret(Secret.EMPTY_SECRET) // todo refigure this
                            .severity(NORMAL)
                            .message(response.getResult().getMemo())
                            .build(LIST_SECRET_AVAILABLE));
                    return result;
                })
                .exceptionally(t -> {
                    // report failure
                    listener.accept(Notification.newBuilder()
                            .severity(WARNING)
                            .message(HttpHelper.getMessageForException(t))
                            .build(LIST_SECRET_FAILED));
                    return result;
                });
    }
}
