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

import org.derecalliance.derec.api.DeRecIdentity;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static com.thebuildingblocks.keypr.common.Cryptography.keyPairGenerator;

public class TestIds {

    public static String[] helperNames = {"leemon", "rohit", "dipti", "cate", "jo", "niall", "daniel", "noone", "nowhere"};

    public static Map<String, KeyPair> DEFAULT_KEYPAIRS = new HashMap<>();

    static {
        for (String name: helperNames) {
            DEFAULT_KEYPAIRS.put(name, keyPairGenerator.generateKeyPair());
        }
    }

    private static String pem(String name){
        return Base64.getEncoder().encodeToString(DEFAULT_KEYPAIRS.get(name).getPublic().getEncoded());
    }

    public static String pemFrom(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    public static DeRecIdentity[] DEFAULT_IDS = {
            new DeRecIdentity("leemon", "mailto:leemon@swirldslabs.com", "http://localhost:8080/leemon", pem("leemon")),
            new DeRecIdentity("rohit", "mailto:rohit@swirldslabs.com", "http://localhost:8080/rohit", pem("rohit")),
            new DeRecIdentity("dipti", "mailto:dipti@swirldslabs.com", "http://localhost:8080/dipti", pem("dipti")),
            new DeRecIdentity("cate", "mailto:cate@swirldslabs.com", "http://localhost:8080/cate", pem("cate")),
            new DeRecIdentity("jo", "mailto:jo@thebuildingblocks.com", "http://localhost:8080/jo", pem("jo")),
            new DeRecIdentity("niall", "mailto:niall@thebuildingblocks.com", "http://localhost:8080/niall", pem("niall")),
            new DeRecIdentity("daniel", "mailto:daniel@thebuildingblocks.com", "http://localhost:8080/daniel", pem("daniel")),
            new DeRecIdentity("noone", "mailto:noone@thebuildingblocks.com", "http://localhost:8080/noone", pem("noone")),
            new DeRecIdentity("nowhere", "mailto:nowhere@thebuildingblocks.com", "http://192.168.1.40/nowhere", pem("nowhere")),
    };

}
