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

package com.thebuildingblocks.derec.crypto;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.File;
import java.io.IOException;

/**
 * Work in progress to assess viability of cross compilation from Rust and use via GraalVM
 * Requires installation of GraalVM toolchain.
 */
public class Rust {
    public static void main(String[] args) throws IOException {
        File file=new File("derec_crypto-44e324e36f8dbc75.bc");
        Context context = Context.newBuilder().allowAllAccess(true).build();
        Source source = Source.newBuilder("llvm", file).build();
        context.eval(source);
        Value ruspart= context.getBindings("llvm").getMember("cube_root");
        Double cubeRoot = ruspart.execute(10).asDouble();
        System.out.println(cubeRoot);
    }
}
