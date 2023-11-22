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

package com.thebuildingblocks.keypr.sharer.tools;

import com.google.protobuf.ByteString;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.thebuildingblocks.keypr.helper.HelperServerResponseProcessing;
import com.thebuildingblocks.keypr.helper.HelperServerShare;
import org.derecalliance.derec.api.DeRecIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

import static com.thebuildingblocks.keypr.common.TestIds.DEFAULT_IDS;

public class TestHelperServer {
    static Logger logger = LoggerFactory.getLogger(TestHelperServer.class);
    HttpServer server;

    public HttpServer getServer() {
        return server;
    }

    public List<HttpContext> getContexts() {
        return contexts;
    }

    private final List<HttpContext> contexts = new ArrayList<>();

    public TestHelperServer(){

    }
    public static void main(String[] args) throws IOException {
        TestHelperServer ths = new TestHelperServer();
        ths.startServer(8080, DEFAULT_IDS);
        System.out.println("Hit enter to exit");
        Scanner sc = new Scanner(System.in);
        sc.nextLine();
        ths.stopServer();
    }

    public void stopServer() {
        server.stop(0);
        System.out.println("Server stopped");
    }

    public void startServer(int port, DeRecIdentity[] ids) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 10);
        for (DeRecIdentity id : ids) {
            if (id.getName().startsWith("no")) {
                continue;
            }
            HttpContext context = server.createContext(id.getAddress().getPath(), new HelperHandler(id));
            logger.info("Started helper {}", context.getPath());
            contexts.add(context);
        }
        logger.info("Server started");
        server.start();
    }

    public static class HelperHandler implements HttpHandler {

        private final HelperServerResponseProcessing helperServerResponseProcessing;

        HelperServerShare.Storage storage = new HelperServerShare.Storage() {
            final Map<ByteString, List<HelperServerShare>> shares = new HashMap<>();

            @Override
            public void putShare(ByteString id, HelperServerShare share) {
                List<HelperServerShare> list = shares.computeIfAbsent(id, k -> new ArrayList<>());
                list.add(share);
            }

            @Override
            public List<HelperServerShare> getShares(ByteString id) {
                return shares.get(id);
            }
        };

        public HelperHandler(DeRecIdentity id) {
            this.helperServerResponseProcessing = new HelperServerResponseProcessing(id, storage);
        }

        @Override
        public void handle(HttpExchange exchange) {
            try {
                this.helperServerResponseProcessing.process(exchange.getRequestBody(), exchange.getResponseBody(),
                        () -> exchange.sendResponseHeaders(200, 0));

            } catch (Throwable t) {
                logger.error("Exception processing message: {}", t.getMessage());
                try {
                    exchange.sendResponseHeaders(400, -1);
                } catch (IOException e) {
                    throw new RuntimeException("Error sending error response", e);
                }
            }
        }

    }
}
