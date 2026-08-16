/*****************************************************************************
 * AVTransportService.java
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
 * UPnP AVTransport service exposing a minimal DLNA Media Renderer.
 *
 * Commands are forwarded to {@link DmrBridge} which drives the VLC playback service.
 * Dynamic state is refreshed into state variable fields at the start of each action,
 * so that jUPnP can read current values when building action responses.
 *
 * Implemented in Java because jUPnP's annotations (@UpnpServiceId/@UpnpServiceType have
 * an empty @Target, @UpnpStateVariable targets FIELD only) are not fully usable from Kotlin.
 */
@UpnpService(
        serviceId = @UpnpServiceId("AVTransport"),
        serviceType = @UpnpServiceType(value = "AVTransport", version = 1)
)
public class AVTransportService {

    // --- Static state variables ---

    @UpnpStateVariable(name = "AVTransportURI", datatype = "string", defaultValue = "")
    public String avTransportUri = "";

    @UpnpStateVariable(name = "AVTransportURIMetaData", datatype = "string", defaultValue = "")
    public String avTransportUriMetaData = "";

    @UpnpStateVariable(name = "CurrentTrackURI", datatype = "string", defaultValue = "")
    public String currentTrackUri = "";

    @UpnpStateVariable(
            name = "CurrentPlayMode", datatype = "string", defaultValue = "NORMAL",
            allowedValues = {"NORMAL", "REPEAT_ONE", "REPEAT_ALL", "SHUFFLE", "DIRECT_1", "INTRO", "SEQUENTIAL"}
    )
    public String currentPlayMode = "NORMAL";

    @UpnpStateVariable(name = "TransportPlaySpeed", datatype = "string", defaultValue = "1")
    public String transportPlaySpeed = "1";

    @UpnpStateVariable(name = "CurrentTrack", datatype = "i4", defaultValue = "0")
    public int currentTrack = 0;

    // --- Dynamic state variables, refreshed by the actions below ---

    @UpnpStateVariable(name = "TransportState", datatype = "string", defaultValue = "STOPPED")
    public String transportState = "STOPPED";

    @UpnpStateVariable(name = "TransportStatus", datatype = "string", defaultValue = "OK")
    public String transportStatus = "OK";

    @UpnpStateVariable(name = "CurrentTransportActions", datatype = "string", defaultValue = "Play,Stop,Pause,Seek")
    public String currentTransportActions = "Play,Stop,Pause,Seek";

    @UpnpStateVariable(name = "CurrentTrackDuration", datatype = "string", defaultValue = "00:00:00")
    public String currentTrackDuration = "00:00:00";

    @UpnpStateVariable(name = "RelativeTimePosition", datatype = "string", defaultValue = "00:00:00")
    public String relativeTimePosition = "00:00:00";

    @UpnpStateVariable(name = "Track", datatype = "i4", defaultValue = "0")
    public int track = 0;

    @UpnpStateVariable(name = "TrackMetaData", datatype = "string", defaultValue = "")
    public String trackMetaData = "";

    @UpnpStateVariable(name = "TrackUri", datatype = "string", defaultValue = "")
    public String trackUri = "";

    @UpnpStateVariable(name = "RelCount", datatype = "i4", defaultValue = "0")
    public int relCount = 0;

    @UpnpStateVariable(name = "NumTracks", datatype = "i4", defaultValue = "0")
    public int numTracks = 0;

    @UpnpStateVariable(name = "MediaDuration", datatype = "string", defaultValue = "00:00:00")
    public String mediaDuration = "00:00:00";

    @UpnpStateVariable(name = "NextUri", datatype = "string", defaultValue = "")
    public String nextUri = "";

    @UpnpStateVariable(name = "NextUriMetaData", datatype = "string", defaultValue = "")
    public String nextUriMetaData = "";

    @UpnpStateVariable(name = "PlayMedium", datatype = "string", defaultValue = "NETWORK")
    public String playMedium = "NETWORK";

    @UpnpStateVariable(name = "RecordMedium", datatype = "string", defaultValue = "NOT_IMPLEMENTED")
    public String recordMedium = "NOT_IMPLEMENTED";

    @UpnpStateVariable(name = "WriteStatus", datatype = "string", defaultValue = "NOT_IMPLEMENTED")
    public String writeStatus = "NOT_IMPLEMENTED";

    @UpnpStateVariable(name = "PlayMode", datatype = "string", defaultValue = "NORMAL")
    public String playMode = "NORMAL";

    @UpnpStateVariable(name = "RecQualityMode", datatype = "string", defaultValue = "NOT_IMPLEMENTED")
    public String recQualityMode = "NOT_IMPLEMENTED";

    @UpnpStateVariable(name = "PlayMedia", datatype = "string", defaultValue = "VIDEO,AUDIO")
    public String playMedia = "VIDEO,AUDIO";

    @UpnpStateVariable(name = "RecMedia", datatype = "string", defaultValue = "NOT_IMPLEMENTED")
    public String recMedia = "NOT_IMPLEMENTED";

    @UpnpStateVariable(name = "RecQualityModes", datatype = "string", defaultValue = "")
    public String recQualityModes = "";

    // --- State variables for action input arguments ---

    @UpnpStateVariable(name = "A_ARG_TYPE_InstanceID", datatype = "i4")
    public int argInstanceId = 0;

    @UpnpStateVariable(name = "A_ARG_TYPE_URI", datatype = "string")
    public String argUri = "";

    @UpnpStateVariable(name = "A_ARG_TYPE_URIMetaData", datatype = "string")
    public String argUriMetaData = "";

    @UpnpStateVariable(
            name = "A_ARG_TYPE_SeekMode", datatype = "string",
            allowedValues = {"TRACK_NR", "ABS_TIME", "REL_TIME", "ABS_COUNT", "REL_COUNT", "CHANNEL_FREQ", "TAPE-INDEX", "FRAME"}
    )
    public String argSeekMode = "";

    @UpnpStateVariable(name = "A_ARG_TYPE_SeekTarget", datatype = "string")
    public String argSeekTarget = "";

    // --- Actions ---

    @UpnpAction(name = "SetAVTransportURI")
    public void setAvTransportUri(
            @UpnpInputArgument(name = "InstanceID") int instanceId,
            @UpnpInputArgument(name = "CurrentURI", stateVariable = "A_ARG_TYPE_URI") String currentUri,
            @UpnpInputArgument(name = "CurrentURIMetaData", stateVariable = "A_ARG_TYPE_URIMetaData") String currentUriMetaData) {
        avTransportUri = currentUri;
        avTransportUriMetaData = currentUriMetaData;
        currentTrackUri = currentUri;
        currentTrack = 0;
        // Received content starts playing immediately, as expected by DLNA control points
        DmrBridge.INSTANCE.load(currentUri);
    }

    @UpnpAction(name = "Play")
    public void play(
            @UpnpInputArgument(name = "InstanceID") int instanceId,
            @UpnpInputArgument(name = "Speed", stateVariable = "TransportPlaySpeed") String speed) {
        DmrBridge.INSTANCE.play();
    }

    @UpnpAction(name = "Pause")
    public void pause(@UpnpInputArgument(name = "InstanceID") int instanceId) {
        DmrBridge.INSTANCE.pause();
    }

    @UpnpAction(name = "Stop")
    public void stop(@UpnpInputArgument(name = "InstanceID") int instanceId) {
        DmrBridge.INSTANCE.stop();
    }

    @UpnpAction(name = "Seek")
    public void seek(
            @UpnpInputArgument(name = "InstanceID") int instanceId,
            @UpnpInputArgument(name = "Unit", stateVariable = "A_ARG_TYPE_SeekMode") String unit,
            @UpnpInputArgument(name = "Target", stateVariable = "A_ARG_TYPE_SeekTarget") String target) {
        if ("REL_TIME".equalsIgnoreCase(unit) || "ABS_TIME".equalsIgnoreCase(unit)) {
            DmrBridge.INSTANCE.seek(DmrBridge.INSTANCE.parseUpnpTime(target));
        }
    }

    @UpnpAction(
            name = "GetTransportInfo",
            out = {
                    @UpnpOutputArgument(name = "CurrentTransportState", stateVariable = "TransportState"),
                    @UpnpOutputArgument(name = "CurrentTransportStatus", stateVariable = "TransportStatus"),
                    @UpnpOutputArgument(name = "CurrentSpeed", stateVariable = "TransportPlaySpeed")
            }
    )
    public void getTransportInfo(@UpnpInputArgument(name = "InstanceID") int instanceId) {
        refreshTransportState();
    }

    @UpnpAction(
            name = "GetPositionInfo",
            out = {
                    @UpnpOutputArgument(name = "Track", stateVariable = "Track"),
                    @UpnpOutputArgument(name = "TrackDuration", stateVariable = "CurrentTrackDuration"),
                    @UpnpOutputArgument(name = "TrackMetaData", stateVariable = "TrackMetaData"),
                    @UpnpOutputArgument(name = "TrackURI", stateVariable = "TrackUri"),
                    @UpnpOutputArgument(name = "RelTime", stateVariable = "RelativeTimePosition"),
                    @UpnpOutputArgument(name = "AbsTime", stateVariable = "RelativeTimePosition"),
                    @UpnpOutputArgument(name = "RelCount", stateVariable = "RelCount"),
                    @UpnpOutputArgument(name = "AbsCount", stateVariable = "RelCount")
            }
    )
    public void getPositionInfo(@UpnpInputArgument(name = "InstanceID") int instanceId) {
        refreshPosition();
    }

    @UpnpAction(
            name = "GetMediaInfo",
            out = {
                    @UpnpOutputArgument(name = "NumTracks", stateVariable = "NumTracks"),
                    @UpnpOutputArgument(name = "MediaDuration", stateVariable = "MediaDuration"),
                    @UpnpOutputArgument(name = "CurrentURI", stateVariable = "TrackUri"),
                    @UpnpOutputArgument(name = "CurrentURIMetaData", stateVariable = "TrackMetaData"),
                    @UpnpOutputArgument(name = "NextURI", stateVariable = "NextUri"),
                    @UpnpOutputArgument(name = "NextURIMetaData", stateVariable = "NextUriMetaData"),
                    @UpnpOutputArgument(name = "PlayMedium", stateVariable = "PlayMedium"),
                    @UpnpOutputArgument(name = "RecordMedium", stateVariable = "RecordMedium"),
                    @UpnpOutputArgument(name = "WriteStatus", stateVariable = "WriteStatus")
            }
    )
    public void getMediaInfo(@UpnpInputArgument(name = "InstanceID") int instanceId) {
        refreshPosition();
    }

    @UpnpAction(
            name = "GetTransportSettings",
            out = {
                    @UpnpOutputArgument(name = "PlayMode", stateVariable = "PlayMode"),
                    @UpnpOutputArgument(name = "RecQualityMode", stateVariable = "RecQualityMode")
            }
    )
    public void getTransportSettings(@UpnpInputArgument(name = "InstanceID") int instanceId) {
        // Static values
    }

    @UpnpAction(
            name = "GetDeviceCapabilities",
            out = {
                    @UpnpOutputArgument(name = "PlayMedia", stateVariable = "PlayMedia"),
                    @UpnpOutputArgument(name = "RecMedia", stateVariable = "RecMedia"),
                    @UpnpOutputArgument(name = "RecQualityModes", stateVariable = "RecQualityModes")
            }
    )
    public void getDeviceCapabilities(@UpnpInputArgument(name = "InstanceID") int instanceId) {
        // Static values
    }

    // --- Internal state refresh helpers ---

    private void refreshTransportState() {
        transportState = DmrBridge.INSTANCE.isPlaying() ? "PLAYING" : "STOPPED";
    }

    private void refreshPosition() {
        refreshTransportState();
        currentTrackDuration = DmrBridge.INSTANCE.formatUpnpTime(DmrBridge.INSTANCE.getDurationMillis());
        relativeTimePosition = DmrBridge.INSTANCE.formatUpnpTime(DmrBridge.INSTANCE.getPositionMillis());
        track = currentTrack;
        trackMetaData = avTransportUriMetaData;
        trackUri = currentTrackUri;
        numTracks = currentTrackUri.isEmpty() ? 0 : 1;
        mediaDuration = currentTrackDuration;
    }
}
