/*
 * Copyright (C) 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package com.google.android.voicemail.example.spec;

import static com.google.android.voicemail.example.spec.Omtp.ProtocolVersion.V1_1;

import java.util.HashMap;
import java.util.Map;

/**
 * A utility class that holds configuration params for different VVM providers/servers.
 */
public class ProviderConfigs {
    // Known providers.
    public static final String MY_CARRIER = "my-carrier";

    private static final Map<String, ProviderConfig> providerConfigs =
            new HashMap<String, ProviderConfig>();

    static {
        /*-----------------------------------------------------------------------------*
         *  Name,    version,    Client type,     dest#,  port#,    date format        *
         * ----------------------------------------------------------------------------*/
        put(MY_CARRIER, V1_1, "google.VVM.10", "1000", 2000, "dd/MM/yyyy HH:mm zz");
    }

    private static void put(String providerName, Omtp.ProtocolVersion protocolVersion,
            String clientType, String smsDestinationNumber, int smsApplicationPort,
            String dateFormat) {
        providerConfigs.put(providerName,
                new ProviderConfig(protocolVersion, clientType, smsDestinationNumber,
                        (short) smsApplicationPort, dateFormat));
    }

    /**
     * Returns the provider config for the requested providerName. Returns null if provider config
     * is not available for the requested providerName.
     */
    public static ProviderConfig getProviderConfig(String providerName) {
        return providerConfigs.get(providerName);
    }
}
