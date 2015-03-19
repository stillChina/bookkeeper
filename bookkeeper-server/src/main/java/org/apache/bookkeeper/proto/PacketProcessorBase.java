/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bookkeeper.proto;

import org.apache.bookkeeper.bookie.Bookie;
import org.apache.bookkeeper.proto.BookieProtocol.Request;
import org.jboss.netty.channel.Channel;
import org.apache.bookkeeper.stats.ServerStatsProvider;
import org.apache.bookkeeper.util.MathUtils;
import org.apache.bookkeeper.util.SafeRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class PacketProcessorBase extends SafeRunnable {
    private final static Logger logger = LoggerFactory.getLogger(PacketProcessorBase.class);
    final Request request;
    final Channel channel;
    final Bookie bookie;
    protected long enqueueNanos;

    PacketProcessorBase(Request request, Channel channel, Bookie bookie) {
        this.request = request;
        this.channel = channel;
        this.bookie = bookie;
        this.enqueueNanos = MathUtils.nowInNano();
    }

    protected void sendResponse(int rc, Enum statOp, Object response) {
        channel.write(response);
        if (BookieProtocol.EOK == rc) {
            ServerStatsProvider.getStatsLoggerInstance().getOpStatsLogger(statOp)
                    .registerSuccessfulEvent(MathUtils.elapsedMicroSec(enqueueNanos));
        } else {
            ServerStatsProvider.getStatsLoggerInstance().getOpStatsLogger(statOp)
                    .registerFailedEvent(MathUtils.elapsedMicroSec(enqueueNanos));
        }
    }

    public boolean isVersionCompatible(Request request) {
        byte version = request.getProtocolVersion();
        if (version < BookieProtocol.LOWEST_COMPAT_PROTOCOL_VERSION
                || version > BookieProtocol.CURRENT_PROTOCOL_VERSION) {
            logger.error("Invalid protocol version. Expected something between " +
                    BookieProtocol.LOWEST_COMPAT_PROTOCOL_VERSION + " and " +
                    BookieProtocol.CURRENT_PROTOCOL_VERSION + ". Got " + version + ".");
            return false;
        }
        return true;
    }
}
