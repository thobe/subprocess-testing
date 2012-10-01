package org.thobe.testing.subprocess;

import java.io.IOException;

import com.sun.jdi.Field;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.ClassUnloadRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.MonitorContendedEnterRequest;
import com.sun.jdi.request.MonitorContendedEnteredRequest;
import com.sun.jdi.request.MonitorWaitRequest;
import com.sun.jdi.request.MonitorWaitedRequest;
import com.sun.jdi.request.StepRequest;
import com.sun.jdi.request.ThreadDeathRequest;
import com.sun.jdi.request.ThreadStartRequest;
import com.sun.jdi.request.WatchpointRequest;

public abstract class DebugHandler
{
    public enum SuspendPolicy
    {
        SUSPEND_NONE( EventRequest.SUSPEND_NONE ),
        SUSPEND_EVENT_THREAD( EventRequest.SUSPEND_EVENT_THREAD ),
        SUSPEND_ALL( EventRequest.SUSPEND_ALL );
        final int suspend_policy;

        private SuspendPolicy( int suspend_policy )
        {
            this.suspend_policy = suspend_policy;
        }

        static SuspendPolicy get( int suspend_policy )
        {
            switch ( suspend_policy )
            {
            case EventRequest.SUSPEND_NONE:
                return SUSPEND_NONE;
            case EventRequest.SUSPEND_EVENT_THREAD:
                return SUSPEND_EVENT_THREAD;
            case EventRequest.SUSPEND_ALL:
                return SUSPEND_ALL;
            default:
                return null;
            }
        }
    }

    public final Subprocess start( Subprocess.Starter starter ) throws IOException
    {
        DebugDispatcher.Bootstrapper bootstrapper = new DebugDispatcher.Bootstrapper();
        try
        {
            Subprocess process = starter.vmArg( bootstrapper.listen() ).internalStart();
            bootstrapper.awaitConnection( this, process );
            return starter.postStart( process );
        }
        finally
        {
            bootstrapper.stopListening();
        }
    }

    protected final EventRequestBuilder request( VirtualMachine virtualMachine )
    {
        return request( virtualMachine, SuspendPolicy.SUSPEND_EVENT_THREAD );
    }

    protected final EventRequestBuilder request( VirtualMachine virtualMachine,
                                                 SuspendPolicy suspendPolicy )
    {
        return new EventRequestBuilder( virtualMachine.eventRequestManager(), suspendPolicy );
    }

    protected void onStart( SuspendPolicy suspension, VirtualMachine virtualMachine, ThreadReference thread )
    {
        // default: do nothing
    }

    protected void onDeath( SuspendPolicy suspension, VirtualMachine virtualMachine )
    {
        // default: do nothing
    }

    protected void onDisconnect()
    {
        // default: do nothing
    }

    protected void onBreakpoint( SuspendPolicy suspension, VirtualMachine virtualMachine, ThreadReference thread,
                                 BreakpointRequest request, Location location )
    {
        onUnhandledLocatableEvent( suspension, virtualMachine, thread, request, location );
    }

    protected void onMethodEntry( SuspendPolicy suspension, VirtualMachine virtualMachine, ThreadReference thread,
                                  MethodEntryRequest request, Method method, Location location )
    {
        onUnhandledLocatableEvent( suspension, virtualMachine, thread, request, location );
    }

    protected void onMethodExit( SuspendPolicy suspension, VirtualMachine virtualMachine, ThreadReference thread,
                                 MethodExitRequest request, Method method, Value value, Location location )
    {
        onUnhandledLocatableEvent( suspension, virtualMachine, thread, request, location );
    }

    protected void onClassPrepare( SuspendPolicy suspension, VirtualMachine virtualMachine, ThreadReference thread,
                                   ClassPrepareRequest request, ReferenceType referenceType )
    {
        onUnhandledThreadConfinedEvent( suspension, virtualMachine, thread, request );
    }

    protected void onClassUnload( SuspendPolicy suspension, VirtualMachine virtualMachine, ClassUnloadRequest request,
                                  String className, String classSignature )
    {
        // default: do nothing
    }

    protected void onExceptionThrown( SuspendPolicy suspension, VirtualMachine virtualMachine, ThreadReference thread,
                                      ExceptionRequest request, ObjectReference exception,
                                      Location throwLocation, Location catchLocation )
    {
        onUnhandledLocatableEvent( suspension, virtualMachine, thread, request, throwLocation );
    }

    protected void onStep( SuspendPolicy suspension, VirtualMachine virtualMachine, ThreadReference thread,
                           StepRequest request, Location location )
    {
        onUnhandledLocatableEvent( suspension, virtualMachine, thread, request, location );
    }

    protected void onContendedMonitorBlocked( SuspendPolicy suspension, VirtualMachine virtualMachine,
                                              ThreadReference thread, MonitorContendedEnterRequest request,
                                              ObjectReference monitor, Location location )
    {
        onUnhandledLocatableEvent( suspension, virtualMachine, thread, request, location );
    }

    protected void onContendedMonitorEntered( SuspendPolicy suspension, VirtualMachine virtualMachine,
                                              ThreadReference thread, MonitorContendedEnteredRequest request,
                                              ObjectReference monitor, Location location )
    {
        onUnhandledLocatableEvent( suspension, virtualMachine, thread, request, location );
    }

    protected void onMonitorBeginWait( SuspendPolicy suspension, VirtualMachine virtualMachine, ThreadReference thread,
                                       MonitorWaitRequest request, ObjectReference monitor, long timeout,
                                       Location location )
    {
        onUnhandledLocatableEvent( suspension, virtualMachine, thread, request, location );
    }

    protected void onMonitorEndWait( SuspendPolicy suspension, VirtualMachine virtualMachine, ThreadReference thread,
                                     MonitorWaitedRequest request, ObjectReference monitor, boolean timedout,
                                     Location location )
    {
        onUnhandledLocatableEvent( suspension, virtualMachine, thread, request, location );
    }

    protected void onThreadStart( SuspendPolicy suspension, VirtualMachine virtualMachine, ThreadReference thread,
                                  ThreadStartRequest request )
    {
        onUnhandledThreadConfinedEvent( suspension, virtualMachine, thread, request );
    }

    protected void onThreadDeath( SuspendPolicy suspension, VirtualMachine virtualMachine, ThreadReference thread,
                                  ThreadDeathRequest request )
    {
        onUnhandledThreadConfinedEvent( suspension, virtualMachine, thread, request );
    }

    protected void onWatchpoint( SuspendPolicy suspension, VirtualMachine virtualMachine, ThreadReference thread,
                                 WatchpointRequest request, ObjectReference object, Field field, Value value,
                                 Location location )
    {
        onUnhandledLocatableEvent( suspension, virtualMachine, thread, request, location );
    }

    protected void onAccessWatchpoint( SuspendPolicy suspension, VirtualMachine virtualMachine, ThreadReference thread,
                                       WatchpointRequest request, ObjectReference object, Field field, Value value,
                                       Location location )
    {
        onWatchpoint( suspension, virtualMachine, thread, request, object, field, value, location );
    }

    protected void onModificationWatchpoint( SuspendPolicy suspension, VirtualMachine virtualMachine,
                                             ThreadReference thread, WatchpointRequest request, ObjectReference object,
                                             Field field, Value oldValue, Value newValue, Location location )
    {
        onWatchpoint( suspension, virtualMachine, thread, request, object, field, oldValue, location );
    }

    protected void onUnhandledLocatableEvent( SuspendPolicy suspension, VirtualMachine virtualMachine,
                                              ThreadReference thread, EventRequest request, Location location )
    {
        onUnhandledThreadConfinedEvent( suspension, virtualMachine, thread, request );
    }

    protected void onUnhandledThreadConfinedEvent( SuspendPolicy suspension, VirtualMachine virtualMachine,
                                                   ThreadReference thread, EventRequest request )
    {
        onUnhandledEvent( suspension, virtualMachine, request );
    }

    protected void onUnhandledEvent( SuspendPolicy suspension, VirtualMachine virtualMachine,
                                     EventRequest request )
    {
        throw new UnsupportedOperationException( "not implemented" );
    }
}
