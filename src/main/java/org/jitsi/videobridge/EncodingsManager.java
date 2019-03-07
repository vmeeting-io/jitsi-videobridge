/*
 * Copyright @ 2018 - present 8x8, Inc.
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

package org.jitsi.videobridge;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.jitsi.nlj.rtp.*;

import java.util.*;

/**
 * Process information signaled about encodings (payload types, ssrcs, ssrc associations, etc.)
 * and gather it in a single place where it can be published out to all interested parties.
 *
 * The idea is to alleviate the following problem:
 * When a new endpoint joins, it can use the notification of new local encoding information to trigger
 * updating all other endpoints with that new information, but how does this new endpoint learn of
 * this information from all existing endpoints?  Existing endpoints don't currently have a good trigger
 * to notify new endpoints about their encoding information, so this was devised as a single location to
 * handle dispersing this information to all interested parties.
 */
public class EncodingsManager {
    private Map<String, List<SsrcAssociation>> ssrcAssociations = new HashMap<>();
    private Set<EncodingsUpdateListener> listeners = new ConcurrentHashSet<>();

    public void addSsrcAssociation(String epId, long primarySsrc, long secondarySsrc, SsrcAssociationType type)
    {
        List<SsrcAssociation> epSsrcAssociations = ssrcAssociations.computeIfAbsent(epId, k -> new ArrayList<>());
        epSsrcAssociations.add(new SsrcAssociation(primarySsrc, secondarySsrc, type));

        listeners.forEach(listener -> {
            listener.onNewSsrcAssociation(epId, primarySsrc, secondarySsrc, type);
        });
    }

    /**
     * Subscribe to future updates and be notified of any existing ssrc associations.
     * @param listener
     */
    public void subscribe(EncodingsUpdateListener listener) {
        listeners.add(listener);

        ssrcAssociations.forEach((epId, ssrcAssociations) -> {
            ssrcAssociations.forEach(ssrcAssociation -> {
                listener.onNewSsrcAssociation(epId, ssrcAssociation.primarySsrc, ssrcAssociation.secondarySsrc, ssrcAssociation.type);
            });
        });
    }

    interface EncodingsUpdateListener {
        void onNewSsrcAssociation(String epId, long primarySsrc, long secondarySsrc, SsrcAssociationType type);
    }

    private class SsrcAssociation {
        private long primarySsrc;
        private long secondarySsrc;
        private SsrcAssociationType type;

        SsrcAssociation(long primarySsrc, long secondarySsrc, SsrcAssociationType type) {
            this.primarySsrc = primarySsrc;
            this.secondarySsrc = secondarySsrc;
            this.type = type;
        }
    }
}