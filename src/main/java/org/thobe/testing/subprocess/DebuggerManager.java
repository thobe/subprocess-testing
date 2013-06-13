package org.thobe.testing.subprocess;

import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import com.sun.jdi.Method;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

class DebuggerManager implements BreakpointRepository
{
    private final Map<EventRequest, BreakpointHandler> handlers = new IdentityHashMap<EventRequest, BreakpointHandler>();
    private final Map<String, BreakpointHandler> breakpoints = new HashMap<String, BreakpointHandler>();
    private final Debugger debugger;

    DebuggerManager( Debugger debugger, DebugHandler dh, VirtualMachine vm )
    {
        this.debugger = debugger;
        for ( Class<?> type = debugger.getClass(); type != Debugger.class; type = type.getSuperclass() )
        {
            for ( java.lang.reflect.Method method : type.getDeclaredMethods() )
            {
                Debugger.Handler annotation = method.getAnnotation( Debugger.Handler.class );
                if ( annotation != null )
                {
                    if ( method.getParameterTypes().length != 0 )
                    {
                        throw new IllegalArgumentException( "@Debugger.Handler methods must not take any arguments." );
                    }
                    BreakpointHandler handler = new BreakpointHandler(
                            debugger, dh.request( vm, annotation.suspend() ), method );
                    breakpoints.put( method.getName(), handler );
                    handlers.put( handler.request, handler );
                }
            }
            debugger.initHandler( this );
        }
    }

    void invokeHandle( EventRequest request, Method method, ThreadReference thread )
    {
        handlers.get( request ).invoke( method, thread );
    }

    void destroy( VirtualMachine vm ) throws Exception
    {
        EventRequestManager requestManager = vm.eventRequestManager();
        for ( EventRequest request : handlers.keySet() )
        {
            requestManager.deleteEventRequest( request );
        }
        debugger.destroyHandler();
    }

    @Override
    public void enable( String breakpoint )
    {
        breakpoint( breakpoint ).request.enable();
    }

    @Override
    public void disable( String breakpoint )
    {
        breakpoint( breakpoint ).request.disable();
    }

    private BreakpointHandler breakpoint( String name )
    {
        BreakpointHandler handler = breakpoints.get( name );
        if ( handler == null )
        {
            throw new IllegalArgumentException( "No such breakpoint: " + name );
        }
        return handler;
    }

    @Override
    public void enabled( Collection<String> breakpoints )
    {
        for ( String breakpoint : breakpoints )
        {
            enable( breakpoint );
        }
    }

    @Override
    public void disabled( Collection<String> breakpoints )
    {
        for ( String breakpoint : breakpoints )
        {
            disable( breakpoint );
        }
    }

    @Override
    public void enableAll()
    {
        for ( BreakpointHandler handler : breakpoints.values() )
        {
            handler.request.enable();
        }
    }

    @Override
    public void propagateTo( BreakpointRepository next )
    {
        throw new UnsupportedOperationException( "Cannot propagate from 'live' state." );
    }
}
