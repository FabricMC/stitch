/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.stitch.util;

/**
 * Annotations used to declare a sided environment.
 */
public final class EnvironmentAnnotations {
    public static final String ENV_SERVER = "SERVER";
    public static final String ENV_CLIENT = "CLIENT";

    private static String stitchAnnotation(final String name) {
        return "Lnet/fabricmc/stitch/annotation/" + name + ";";
    }

    public static final String SIDE_DESCRIPTOR = stitchAnnotation("EnvType");
    public static final String ITF_DESCRIPTOR = stitchAnnotation("EnvironmentInterface");
    public static final String ITF_LIST_DESCRIPTOR = stitchAnnotation("EnvironmentInterfaces");
    public static final String SIDED_DESCRIPTOR = stitchAnnotation("Environment");

    private EnvironmentAnnotations() {
    }
}
