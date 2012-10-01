package org.thobe.testing.subprocess;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequest;

public class RequestDispatchHandler extends DebugHandler
{
    public static abstract class EventHandler
    {
        protected abstract void handle( VirtualMachine virtualMachine, ThreadReference thread );
    }

    private final Map<EventRequest, EventHandler> handlers = new ConcurrentHashMap<EventRequest, EventHandler>();

    @Override
    protected void onUnhandledThreadConfinedEvent( SuspendPolicy suspension, VirtualMachine virtualMachine,
                                                   ThreadReference thread, EventRequest request )
    {
        EventHandler handler = handlers.get( request );
        if ( handler != null )
        {
            handler.handle( virtualMachine, thread );
        }
    }
}
