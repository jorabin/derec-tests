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
import org.junit.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.thebuildingblocks.derec.v0_9.test.TestIds.DEFAULT_IDS;

public class RecipesTest {
    static TestHelperServer server;
    Sharer sharer;
    Secret secret1, secret2;

    @BeforeClass
    public static void setUpServer() throws IOException {
        server = new TestHelperServer();
        server.startServer(8080, TestIds.DEFAULT_IDS);
    }

    @AfterClass
    public static void tearDownServer() {
        server.stopServer();
    }

    @Before
    public void setUp() {
        sharer = Sharer.newBuilder().build();
        secret1 = sharer.newSecret("my first secret",
                "quite hush hush".getBytes(StandardCharsets.UTF_8),
                Arrays.asList(DEFAULT_IDS));
        secret1.update("an updated version".getBytes(StandardCharsets.UTF_8));
        secret2 = sharer.newSecret("my second secret",
                "very hush hush".getBytes(StandardCharsets.UTF_8),
                Arrays.asList(DEFAULT_IDS));
    }

    @After
    public void tearDown() {
        secret1.close();
        secret2.close();
    }


    @Test
    public void listVersionNumbersForHelper() {
    }

    @Test
    public void listHelpersAndSecretsForSharer() {
    }

    @Test
    public void listHelpersAndSecretsForSharer2() {
    }
}