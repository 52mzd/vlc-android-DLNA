/*****************************************************************************
 * ConnectionManagerService.java
 *****************************************************************************
 * Copyright © 2025 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *  ***************************************************************************
 */

package org.videolan.vlc.dmr;

import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.binding.annotations.UpnpInputArgument;
import org.jupnp.binding.annotations.UpnpOutputArgument;
import org.jupnp.binding.annotations.UpnpService;
import org.jupnp.binding.annotations.UpnpServiceId;
import org.jupnp.binding.annotations.UpnpServiceType;
import org.jupnp.binding.annotations.UpnpStateVariable;

/**
 * UPnP ConnectionManager service: declares the media formats accepted by this
 * DLNA renderer (video + audio over http-get) and reports connections.
 */
@UpnpService(
        serviceId = @UpnpServiceId("ConnectionManager"),
        serviceType = @UpnpServiceType(value = "ConnectionManager", version = 1)
)
public class ConnectionManagerService {

    public static final String SINK_PROTOCOL_INFO = "http-get:*:video/*:*,http-get:*:audio/*:*";
    public static final String SOURCE_PROTOCOL_INFO = "";

    // --- State variables for action input arguments ---

    @UpnpStateVariable(name = "A_ARG_TYPE_ConnectionID", datatype = "i4")
    public int argConnectionId = 0;

    // --- State variables ---

    @UpnpStateVariable(name = "SourceProtocolInfo", datatype = "string", defaultValue = SOURCE_PROTOCOL_INFO)
    public String sourceProtocolInfo = SOURCE_PROTOCOL_INFO;

    @UpnpStateVariable(name = "SinkProtocolInfo", datatype = "string", defaultValue = SINK_PROTOCOL_INFO)
    public String sinkProtocolInfo = SINK_PROTOCOL_INFO;

    @UpnpStateVariable(name = "CurrentConnectionIDs", datatype = "string", defaultValue = "0")
    public String currentConnectionIds = "0";

    @UpnpStateVariable(name = "RcsID", datatype = "i4", defaultValue = "-1")
    public int rcsId = -1;

    @UpnpStateVariable(name = "AVTransportID", datatype = "i4", defaultValue = "-1")
    public int avTransportId = -1;

    @UpnpStateVariable(name = "ProtocolInfo", datatype = "string", defaultValue = "")
    public String protocolInfo = SINK_PROTOCOL_INFO;

    @UpnpStateVariable(name = "PeerConnectionManager", datatype = "string", defaultValue = "")
    public String peerConnectionManager = "";

    @UpnpStateVariable(name = "PeerConnectionID", datatype = "i4", defaultValue = "-1")
    public int peerConnectionId = -1;

    @UpnpStateVariable(name = "Direction", datatype = "string", defaultValue = "Output")
    public String direction = "Output";

    @UpnpStateVariable(name = "Status", datatype = "string", defaultValue = "OK")
    public String status = "OK";

    // --- Actions ---

    @UpnpAction(
            name = "GetProtocolInfo",
            out = {
                    @UpnpOutputArgument(name = "Source", stateVariable = "SourceProtocolInfo"),
                    @UpnpOutputArgument(name = "Sink", stateVariable = "SinkProtocolInfo")
            }
    )
    public void getProtocolInfo() {
        // Static values
    }

    @UpnpAction(
            name = "GetCurrentConnectionIDs",
            out = {@UpnpOutputArgument(name = "ConnectionIDs", stateVariable = "CurrentConnectionIDs")}
    )
    public void getCurrentConnectionIds() {
        // Static values
    }

    @UpnpAction(
            name = "GetCurrentConnectionInfo",
            out = {
                    @UpnpOutputArgument(name = "RcsID", stateVariable = "RcsID"),
                    @UpnpOutputArgument(name = "AVTransportID", stateVariable = "AVTransportID"),
                    @UpnpOutputArgument(name = "ProtocolInfo", stateVariable = "ProtocolInfo"),
                    @UpnpOutputArgument(name = "PeerConnectionManager", stateVariable = "PeerConnectionManager"),
                    @UpnpOutputArgument(name = "PeerConnectionID", stateVariable = "PeerConnectionID"),
                    @UpnpOutputArgument(name = "Direction", stateVariable = "Direction"),
                    @UpnpOutputArgument(name = "Status", stateVariable = "Status")
            }
    )
    public void getCurrentConnectionInfo(@UpnpInputArgument(name = "ConnectionID") int connectionId) {
        // Static values
    }
}
