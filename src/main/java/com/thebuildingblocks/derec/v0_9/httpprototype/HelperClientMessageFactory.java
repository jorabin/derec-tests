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

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import derec.message.Secretidsversions.GetSecretIdsVersionsRequestMessage;
import org.derecalliance.derec.api.DeRecIdentity;
import derec.message.Derecmessage.DeRecMessage.SharerMessageBody;

import static derec.message.Communicationinfo.*;
import static derec.message.Derecmessage.*;
import static derec.message.Derecmessage.DeRecMessage.*;
import static derec.message.Pair.*;
import static derec.message.Storeshare.*;
import static derec.message.Unpair.*;
import static derec.message.Verify.*;

/**
 * Provides a binding between the world of Protobuf Messages and DeRec Classes for requests from client
 */
public class HelperClientMessageFactory {

    public static class ProtocolInfo {
        private int versionMinor = 9;
        private int versionMajor = 0;
        private byte[] secretId = new byte[0];
        private byte[] sharerPublicKeyDigest;
        private byte[] helperPublicKeyDigest;

        public static Builder newBuilder(){
            return new Builder();
        }

        public static class Builder {
            ProtocolInfo protocolInfo = new ProtocolInfo();
            private Builder(){}

            Builder secretId(byte[] secretId) {
                protocolInfo.secretId = secretId;
                return this;
            }
            Builder sharerPublicKeyDigest(byte[] sharerPublicKeyDigest) {
                protocolInfo.sharerPublicKeyDigest = sharerPublicKeyDigest;
                return this;
            }
            Builder helperPublicKeyDigest(byte[] helperPublicKeyDigest) {
                protocolInfo.helperPublicKeyDigest = helperPublicKeyDigest;
                return this;
            }
            ProtocolInfo build() {
                return protocolInfo;
            }
        }
    }

    public static SharerMessageBody getPairRequestMessageBody (DeRecIdentity deRecId) {
        return SharerMessageBody.newBuilder()
                .setPairRequestMessage(PairRequestMessage.newBuilder()
                        .setCommunicationInfo(CommunicationInfo.newBuilder()
                                .addCommunicationInfoEntries(CommunicationInfoKeyValue.newBuilder()
                                        .setKey("email")
                                        .setStringValue(deRecId.getContact().toString()))
                                .addCommunicationInfoEntries(CommunicationInfoKeyValue.newBuilder()
                                        .setKey("address")
                                        .setStringValue(deRecId.getAddress().toString()))
                                .addCommunicationInfoEntries(CommunicationInfoKeyValue.newBuilder()
                                        .setKey("name")
                                        .setStringValue(deRecId.getName()))
                                .build())
                        .build())
                .build();
    }

    public static SharerMessageBody getShareRequestMessageBody (Version.Share share) {
        ByteString bytes = DeRecShare.newBuilder()
                .setVersion(share.version.versionNumber)
                .setSecretId(ByteString.copyFrom(share.version.secret.getSecretId()))
                .build()
                .toByteString();
        return SharerMessageBody.newBuilder()
                .setStoreShareRequestMessage(StoreShareRequestMessage.newBuilder()
                        .setCommittedDeRecShare(CommittedDeRecShare.newBuilder()
                                .setDeRecShare(bytes)
                                .build())
                        .build())
                .build();
    }

    public static SharerMessageBody getVerifyRequestMessageBody (Version.Share share) {
        return SharerMessageBody.newBuilder()
                .setVerifyShareRequestMessage(VerifyShareRequestMessage.newBuilder()
                        .setNonce(ByteString.copyFrom(share.nonce))
                        .setVersion(share.version.versionNumber)
                        .build())
                .build();
    }

    public static SharerMessageBody getUnPairRequestMessageBody (String reason) {
        return SharerMessageBody.newBuilder()
                .setUnpairRequestMessage(UnpairRequestMessage.newBuilder()
                        .setMemo(reason)
                        .build())
                .build();
    }

    public static SharerMessageBody getSecretsIdsVersionsMessageBody () {
        return SharerMessageBody.newBuilder()
                .setGetSecretIdsVersionsRequestMessage(GetSecretIdsVersionsRequestMessage.newBuilder()
                        .build())
                .build();
    }


    public static DeRecMessage getMessage(ProtocolInfo info, SharerMessageBody body) {
        return newBuilder()
                .setProtocolVersionMinor(info.versionMinor)
                .setProtocolVersionMajor(info.versionMajor)
                .setReceiver(ByteString.copyFrom(info.helperPublicKeyDigest))
                .setSender(ByteString.copyFrom(info.sharerPublicKeyDigest))
                .setSecretId(ByteString.copyFrom(info.secretId))
                .setTimestamp(Timestamp.newBuilder().setSeconds(System.currentTimeMillis()).build())
                .setMessageBodies(MessageBodies.newBuilder()
                        .setSharerMessageBodies(SharerMessageBodies.newBuilder()
                                .addSharerMessageBody(body)
                                .build())
                        .build())
                .build();
    }
}
