/*****************************************************************************
 * RenderingControlService.java
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
 * UPnP RenderingControl service: volume and mute synchronization with the VLC playback engine.
 */
@UpnpService(
        serviceId = @UpnpServiceId("RenderingControl"),
        serviceType = @UpnpServiceType(value = "RenderingControl", version = 1)
)
public class RenderingControlService {

    // --- State variables for action input arguments ---

    @UpnpStateVariable(name = "A_ARG_TYPE_InstanceID", datatype = "i4")
    public int argInstanceId = 0;

    @UpnpStateVariable(
            name = "A_ARG_TYPE_Channel", datatype = "string",
            allowedValues = {"Master", "LF", "RF", "CF", "LFE", "LS", "RS", "LFC", "RFC", "SD", "SL", "SR", "TC", "SBL", "SBC", "SBR", "Setup"}
    )
    public String argChannel = "";

    @UpnpStateVariable(name = "A_ARG_TYPE_DesiredVolume", datatype = "i2")
    public int argDesiredVolume = 0;

    @UpnpStateVariable(name = "A_ARG_TYPE_DesiredMute", datatype = "boolean")
    public boolean argDesiredMute = false;

    // --- Dynamic state variables ---

    @UpnpStateVariable(
            name = "Volume", datatype = "i2", defaultValue = "100",
            allowedValueMinimum = 0, allowedValueMaximum = 100, allowedValueStep = 1
    )
    public int volume = 100;

    @UpnpStateVariable(name = "Mute", datatype = "boolean", defaultValue = "0")
    public boolean mute = false;

    // --- Actions ---

    @UpnpAction(
            name = "GetVolume",
            out = {@UpnpOutputArgument(name = "CurrentVolume", stateVariable = "Volume")}
    )
    public void getVolumeAction(
            @UpnpInputArgument(name = "InstanceID") int instanceId,
            @UpnpInputArgument(name = "Channel") String channel) {
        volume = DmrBridge.INSTANCE.getVolume();
        mute = volume == 0;
    }

    @UpnpAction(name = "SetVolume")
    public void setVolumeAction(
            @UpnpInputArgument(name = "InstanceID") int instanceId,
            @UpnpInputArgument(name = "Channel") String channel,
            @UpnpInputArgument(name = "DesiredVolume") int desiredVolume) {
        volume = desiredVolume;
        mute = volume == 0;
        DmrBridge.INSTANCE.setVolume(desiredVolume);
    }

    @UpnpAction(
            name = "GetMute",
            out = {@UpnpOutputArgument(name = "CurrentMute", stateVariable = "Mute")}
    )
    public void getMuteAction(
            @UpnpInputArgument(name = "InstanceID") int instanceId,
            @UpnpInputArgument(name = "Channel") String channel) {
        volume = DmrBridge.INSTANCE.getVolume();
        mute = volume == 0;
    }

    @UpnpAction(name = "SetMute")
    public void setMuteAction(
            @UpnpInputArgument(name = "InstanceID") int instanceId,
            @UpnpInputArgument(name = "Channel") String channel,
            @UpnpInputArgument(name = "DesiredMute") boolean desiredMute) {
        mute = desiredMute;
        DmrBridge.INSTANCE.setMute(desiredMute);
    }
}
