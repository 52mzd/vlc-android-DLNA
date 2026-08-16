package org.videolan.vlc.dmr;

import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.StateVariable;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the jUPnP annotations on the DMR service classes bind without
 * exceptions (any LocalServiceBindingException here means the device could not
 * be registered and would never be discoverable).
 */
public class DmrServiceBindingTest {

    @Test
    public void bindAvTransportService() {
        LocalService<?> service = new AnnotationLocalServiceBinder().read(AVTransportService.class);
        assertNotNull("AVTransport binding failed", service);
        // Expected actions
        String[] expected = {"SetAVTransportURI", "Play", "Pause", "Stop", "Seek",
                "GetTransportInfo", "GetPositionInfo", "GetMediaInfo", "GetTransportSettings", "GetDeviceCapabilities",
                "QueryStateVariable"};
        String[] actual = new String[service.getActions().length];
        for (int i = 0; i < service.getActions().length; i++) {
            actual[i] = service.getActions()[i].getName();
        }
        Arrays.sort(expected);
        Arrays.sort(actual);
        assertEquals("AVTransport actions", Arrays.toString(expected), Arrays.toString(actual));
    }

    @Test
    public void bindRenderingControlService() {
        LocalService<?> service = new AnnotationLocalServiceBinder().read(RenderingControlService.class);
        assertNotNull("RenderingControl binding failed", service);
        String[] expected = {"GetVolume", "SetVolume", "GetMute", "SetMute", "QueryStateVariable"};
        String[] actual = new String[service.getActions().length];
        for (int i = 0; i < service.getActions().length; i++) {
            actual[i] = service.getActions()[i].getName();
        }
        Arrays.sort(expected);
        Arrays.sort(actual);
        assertEquals("RenderingControl actions", Arrays.toString(expected), Arrays.toString(actual));
    }

    @Test
    public void bindConnectionManagerService() {
        LocalService<?> service = new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);
        assertNotNull("ConnectionManager binding failed", service);
        String[] expected = {"GetProtocolInfo", "GetCurrentConnectionIDs", "GetCurrentConnectionInfo", "QueryStateVariable"};
        String[] actual = new String[service.getActions().length];
        for (int i = 0; i < service.getActions().length; i++) {
            actual[i] = service.getActions()[i].getName();
        }
        Arrays.sort(expected);
        Arrays.sort(actual);
        assertEquals("ConnectionManager actions", Arrays.toString(expected), Arrays.toString(actual));
    }

    @Test
    public void allStateVariablesHaveDatatypes() {
        for (LocalService<?> service : new LocalService[]{
                new AnnotationLocalServiceBinder().read(AVTransportService.class),
                new AnnotationLocalServiceBinder().read(RenderingControlService.class),
                new AnnotationLocalServiceBinder().read(ConnectionManagerService.class)}) {
            for (StateVariable sv : service.getStateVariables()) {
                assertNotNull("State variable " + sv.getName() + " has no datatype",
                        sv.getTypeDetails().getDatatype());
            }
        }
    }
}
