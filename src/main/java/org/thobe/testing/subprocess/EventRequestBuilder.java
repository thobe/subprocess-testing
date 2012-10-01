package org.thobe.testing.subprocess;

import com.sun.jdi.Field;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.request.AccessWatchpointRequest;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.ClassUnloadRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.ModificationWatchpointRequest;
import com.sun.jdi.request.MonitorContendedEnterRequest;
import com.sun.jdi.request.MonitorContendedEnteredRequest;
import com.sun.jdi.request.MonitorWaitRequest;
import com.sun.jdi.request.MonitorWaitedRequest;
import com.sun.jdi.request.StepRequest;
import com.sun.jdi.request.ThreadDeathRequest;
import com.sun.jdi.request.ThreadStartRequest;

public class EventRequestBuilder
{
    private final EventRequestManager eventRequestManager;
    private final DebugHandler.SuspendPolicy suspendPolicy;

    EventRequestBuilder( EventRequestManager eventRequestManager, DebugHandler.SuspendPolicy suspendPolicy )
    {
        this.eventRequestManager = eventRequestManager;
        this.suspendPolicy = suspendPolicy;
    }

    private <R extends EventRequest> R request( R request )
    {
        request.setSuspendPolicy( suspendPolicy.suspend_policy );
        return request;
    }

    ClassPrepareRequest classPrepare()
    {
        return request( eventRequestManager.createClassPrepareRequest() );
    }

    ClassUnloadRequest classUnload()
    {
        return request( eventRequestManager.createClassUnloadRequest() );
    }

    ThreadStartRequest threadStart()
    {
        return request( eventRequestManager.createThreadStartRequest() );
    }

    ThreadDeathRequest threadDeath()
    {
        return request( eventRequestManager.createThreadDeathRequest() );
    }

    ExceptionRequest exceptionThrown( ReferenceType exceptionClass, boolean onCaught, boolean onUncaught )
    {
        return request( eventRequestManager.createExceptionRequest( exceptionClass, onCaught, onUncaught ) );
    }

    MethodEntryRequest methodEntry()
    {
        return request( eventRequestManager.createMethodEntryRequest() );
    }

    MethodExitRequest methodExit()
    {
        return request( eventRequestManager.createMethodExitRequest() );
    }

    MonitorContendedEnterRequest contendedMonitorBlocked()
    {
        return request( eventRequestManager.createMonitorContendedEnterRequest() );
    }

    MonitorContendedEnteredRequest contendedMonitorEntered()
    {
        return request( eventRequestManager.createMonitorContendedEnteredRequest() );
    }

    MonitorWaitRequest beginWait()
    {
        return request( eventRequestManager.createMonitorWaitRequest() );
    }

    MonitorWaitedRequest endWait()
    {
        return request( eventRequestManager.createMonitorWaitedRequest() );
    }

    StepRequest step( ThreadReference thread, int size, int depth )
    {
        return request( eventRequestManager.createStepRequest( thread, size, depth ) );
    }

    BreakpointRequest breakpoint( Location location )
    {
        return request( eventRequestManager.createBreakpointRequest( location ) );
    }

    AccessWatchpointRequest accessWatchpoint( Field field )
    {
        return request( eventRequestManager.createAccessWatchpointRequest( field ) );
    }

    ModificationWatchpointRequest modificationWatchpoint( Field field )
    {
        return request( eventRequestManager.createModificationWatchpointRequest( field ) );
    }
}
