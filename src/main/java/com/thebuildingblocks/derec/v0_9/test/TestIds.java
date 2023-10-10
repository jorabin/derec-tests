package com.thebuildingblocks.derec.v0_9.test;

import com.thebuildingblocks.derec.v0_9.interfaces.DeRecId;
import derec.message.Derecmessage;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

import static com.thebuildingblocks.derec.v0_9.httpprototype.Cryptography.keyPairGenerator;

public class TestIds {
    public static DeRecId[] DEFAULT_IDS = {
            new DeRecId("leemon", "mailto:leemon@swirldslabs.com", "http://localhost:8080/leemon"),
            new DeRecId("rohit", "mailto:rohit@swirldslabs.com", "http://localhost:8080/rohit"),
            new DeRecId("dipti", "mailto:dipti@swirldslabs.com", "http://localhost:8080/dipti"),
            new DeRecId("cate", "mailto:cate@swirldslabs.com", "http://localhost:8080/cate"),
            new DeRecId("jo", "mailto:jo@thebuildingblocks.com", "http://localhost:8080/jo"),
            new DeRecId("niall", "mailto:niall@thebuildingblocks.com", "http://localhost:8080/niall"),
            new DeRecId("daniel", "mailto:daniel@thebuildingblocks.com", "http://localhost:8080/daniel"),
            new DeRecId("noone", "mailto:noone@thebuildingblocks.com", "http://localhost:8080/noone"),
            new DeRecId("nowhere", "mailto:nowhere@thebuildingblocks.com", "http://192.168.1.40/nowhere"),
    };

    public static Map<String, KeyPair> DEFAULT_KEYPAIRS = new HashMap<>();

    static {
        for (DeRecId id: DEFAULT_IDS) {
            DEFAULT_KEYPAIRS.put(id.getName(), keyPairGenerator.generateKeyPair());
        }
    }
}
