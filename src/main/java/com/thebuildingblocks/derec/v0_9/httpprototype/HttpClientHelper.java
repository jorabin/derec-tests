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

import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.function.Function;

/**
 * Helpers to assist in use of {@link java.net.http.HttpClient}
 */
public class HttpClientHelper {
    /**
     * function to check response is 200 and return the body as an input stream  or throw exception if not
     */
    public static Function<HttpResponse<InputStream>, InputStream> httpStatusChecker = response -> {
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP Status " + response.statusCode());
        }
        return response.body();
    };

    /**
     * Get message from exception or cause of exception if no message in exception
     */
    public static String getMessageForException(Throwable throwable) {
        String message = throwable.getCause().getMessage();
        return Objects.nonNull(message) ? message : throwable.getCause().getClass().getName();
    }
}
