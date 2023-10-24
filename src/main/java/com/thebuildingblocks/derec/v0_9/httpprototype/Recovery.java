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
import derec.message.Derecmessage.DeRecMessage;
import derec.message.ResultOuterClass;
import derec.message.Secretidsversions.GetSecretIdsVersionsResponseMessage;
import org.derecalliance.derec.api.DeRecIdentity;
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
import static org.derecalliance.derec.api.DeRecStatusNotification.StandardNotificationType.LIST_SECRET_AVAILABLE;
import static org.derecalliance.derec.api.DeRecStatusNotification.StandardNotificationType.LIST_SECRET_FAILED;

public class Recovery {
    public static Future<Map<byte[], List<Integer>>> getSecretIds(
            DeRecIdentity sharerInfo,
            DeRecIdentity helperInfo,
            Consumer<DeRecStatusNotification> listener) {

        HttpClient httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(Util.RetryParameters.DEFAULT.getConnectTimeout())
                        .build();

        ProtocolInfo protocolInfo = ProtocolInfo.newBuilder()
                .sharerPublicKeyDigest(sharerInfo.getPublicKeyDigest())
                .helperPublicKeyDigest(helperInfo.getPublicKeyDigest())
                .build();

        DeRecMessage message = HelperClientMessageFactory.getMessage(protocolInfo,
                HelperClientMessageFactory.getSecretsIdsVersionsMessageBody());

        HttpRequest httpRequest =
                HttpRequest.newBuilder()
                        .uri(helperInfo.getAddress())
                        .POST(HttpRequest.BodyPublishers.ofByteArray(message.toByteArray()))
                        .build();

        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(r -> {
                    Map<byte[], List<Integer>> result = new HashMap<>();
                    if (r.statusCode() != 200) {
                        listener.accept(Notification.newBuilder()
                                        .severity(WARNING)
                                        .message(String.format("Helper %s HTTP %d", helperInfo.getName(), r.statusCode()))
                                        .build(LIST_SECRET_FAILED));
                        return result;
                    }

                    HelperClientMessageDeserializer deserializer = HelperClientMessageDeserializer.newInstance(r.body(),
                            DeRecMessage.HelperMessageBody.BodyCase.GETSECRETIDSVERSIONSRESPONSEMESSAGE);

                    GetSecretIdsVersionsResponseMessage response =
                            deserializer.getBody().getGetSecretIdsVersionsResponseMessage();
                    if (!response.getResult().getStatus().equals(ResultOuterClass.StatusEnum.OK)) {
                        listener.accept(Notification.newBuilder()
                                .severity(WARNING)
                                .message(response.getResult().getMemo())
                                .build(LIST_SECRET_FAILED));
                        return result;
                    }
                    for (GetSecretIdsVersionsResponseMessage.VersionList versions : response.getSecretListList()) {
                        result.put(versions.getSecretId().toByteArray(), new ArrayList<>(versions.getVersionsList()));
                    }
                    listener.accept(Notification.newBuilder()
                            .severity(NORMAL)
                            .message(response.getResult().getMemo())
                            .build(LIST_SECRET_AVAILABLE));
                    return result;
                });
    }
}
