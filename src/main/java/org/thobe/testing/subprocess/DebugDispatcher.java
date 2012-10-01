package org.thobe.testing.subprocess;

import java.io.IOException;
import java.util.Map;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.ListeningConnector;
import com.sun.jdi.event.AccessWatchpointEvent;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.ClassUnloadEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.ModificationWatchpointEvent;
import com.sun.jdi.event.MonitorContendedEnterEvent;
import com.sun.jdi.event.MonitorContendedEnteredEvent;
import com.sun.jdi.event.MonitorWaitEvent;
import com.sun.jdi.event.MonitorWaitedEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.event.WatchpointEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.ClassUnloadRequest;
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

import static com.sun.jdi.Bootstrap.virtualMachineManager;

class DebugDispatcher implements Runnable
{
    private final VirtualMachine vm;
    private final DebugHandler handler;
    private final Subprocess process;

    private DebugDispatcher( VirtualMachine vm, DebugHandler handler, Subprocess process )
    {
        this.vm = vm;
        this.handler = handler;
        this.process = process;
    }

    @Override
    public void run()
    {
        for ( EventQueue queue = vm.eventQueue(); ; )
        {
            EventSet events;
            try
            {
                events = queue.remove();
            }
            catch ( InterruptedException e )
            {
                Thread.interrupted(); // reset
                vm.exit( -1 ); // The debugger was interrupted, kill the VM
                continue; // then await the disconnect event
            }
            DebugHandler.SuspendPolicy suspension = DebugHandler.SuspendPolicy.get( events.suspendPolicy() );
            for ( Event event : events )
            {
                if ( event instanceof LocatableEvent )
                {
                    if ( event instanceof WatchpointEvent )
                    {
                        if ( event instanceof AccessWatchpointEvent )
                        {
                            AccessWatchpointEvent accessEvent = (AccessWatchpointEvent) event;
                            handler.onAccessWatchpoint( suspension, event.virtualMachine(), accessEvent.thread(),
                                                        (WatchpointRequest) event.request(), accessEvent.object(),
                                                        accessEvent.field(), accessEvent.valueCurrent(),
                                                        accessEvent.location() );
                        }
                        else if ( event instanceof ModificationWatchpointEvent )
                        {
                            ModificationWatchpointEvent modificationEvent = (ModificationWatchpointEvent) event;
                            handler.onModificationWatchpoint( suspension, event.virtualMachine(),
                                                              modificationEvent.thread(),
                                                              (WatchpointRequest) event.request(),
                                                              modificationEvent.object(),
                                                              modificationEvent.field(),
                                                              modificationEvent.valueCurrent(),
                                                              modificationEvent.valueToBe(),
                                                              modificationEvent.location() );
                        }
                        else
                        {
                            WatchpointEvent watchpointEvent = (WatchpointEvent) event;
                            handler.onWatchpoint( suspension, event.virtualMachine(), watchpointEvent.thread(),
                                                  (WatchpointRequest) event.request(), watchpointEvent.object(),
                                                  watchpointEvent.field(), watchpointEvent.valueCurrent(),
                                                  watchpointEvent.location() );
                        }
                    }
                    else if ( event instanceof BreakpointEvent )
                    {
                        BreakpointEvent breakpointEvent = (BreakpointEvent) event;
                        handler.onBreakpoint( suspension, breakpointEvent.virtualMachine(), breakpointEvent.thread(),
                                              (BreakpointRequest) breakpointEvent.request(),
                                              breakpointEvent.location() );
                    }
                    else if ( event instanceof MethodEntryEvent )
                    {
                        MethodEntryEvent entryEvent = (MethodEntryEvent) event;
                        handler.onMethodEntry( suspension, entryEvent.virtualMachine(), entryEvent.thread(),
                                               (MethodEntryRequest) entryEvent.request(), entryEvent.method(),
                                               entryEvent.location() );
                    }
                    else if ( event instanceof MethodExitEvent )
                    {
                        MethodExitEvent exitEvent = (MethodExitEvent) event;
                        handler.onMethodExit( suspension, exitEvent.virtualMachine(), exitEvent.thread(),
                                              (MethodExitRequest) exitEvent.request(), exitEvent.method(),
                                              exitEvent.returnValue(), exitEvent.location() );
                    }
                    else if ( event instanceof StepEvent )
                    {
                        StepEvent stepEvent = (StepEvent) event;
                        handler.onStep( suspension, stepEvent.virtualMachine(), stepEvent.thread(),
                                        (StepRequest) stepEvent.request(), stepEvent.location() );
                    }
                    else if ( event instanceof MonitorContendedEnterEvent )
                    {
                        MonitorContendedEnterEvent contendedEnterEvent = (MonitorContendedEnterEvent) event;
                        handler.onContendedMonitorBlocked( suspension, event.virtualMachine(),
                                                           contendedEnterEvent.thread(),
                                                           (MonitorContendedEnterRequest) event.request(),
                                                           contendedEnterEvent.monitor(),
                                                           contendedEnterEvent.location() );
                    }
                    else if ( event instanceof MonitorContendedEnteredEvent )
                    {
                        MonitorContendedEnteredEvent contendedEnteredEvent = (MonitorContendedEnteredEvent) event;
                        handler.onContendedMonitorEntered( suspension, event.virtualMachine(),
                                                           contendedEnteredEvent.thread(),
                                                           (MonitorContendedEnteredRequest) event.request(),
                                                           contendedEnteredEvent.monitor(),
                                                           contendedEnteredEvent.location() );
                    }
                    else if ( event instanceof MonitorWaitEvent )
                    {
                        MonitorWaitEvent waitEvent = (MonitorWaitEvent) event;
                        handler.onMonitorBeginWait( suspension, event.virtualMachine(), waitEvent.thread(),
                                                    (MonitorWaitRequest) event.request(), waitEvent.monitor(),
                                                    waitEvent.timeout(), waitEvent.location() );
                    }
                    else if ( event instanceof MonitorWaitedEvent )
                    {
                        MonitorWaitedEvent waitedEvent = (MonitorWaitedEvent) event;
                        handler.onMonitorEndWait( suspension, event.virtualMachine(), waitedEvent.thread(),
                                                  (MonitorWaitedRequest) event.request(), waitedEvent.monitor(),
                                                  waitedEvent.timedout(), waitedEvent.location() );
                    }
                    else if ( event instanceof ExceptionEvent )
                    {
                        ExceptionEvent exceptionEvent = (ExceptionEvent) event;
                        handler.onExceptionThrown( suspension, exceptionEvent.virtualMachine(), exceptionEvent.thread(),
                                                   (ExceptionRequest) exceptionEvent.request(),
                                                   exceptionEvent.exception(),
                                                   exceptionEvent.location(), exceptionEvent.catchLocation() );
                    }
                    else
                    {
                        LocatableEvent locatableEvent = (LocatableEvent) event;
                        handler.onUnhandledLocatableEvent( suspension, event.virtualMachine(), locatableEvent.thread(),
                                                           event.request(), locatableEvent.location() );
                    }
                }
                else if ( event instanceof ClassPrepareEvent )
                {
                    ClassPrepareEvent prepareEvent = (ClassPrepareEvent) event;
                    handler.onClassPrepare( suspension, prepareEvent.virtualMachine(), prepareEvent.thread(),
                                            (ClassPrepareRequest) prepareEvent.request(),
                                            prepareEvent.referenceType() );
                }
                else if ( event instanceof ClassUnloadEvent )
                {
                    ClassUnloadEvent unloadEvent = (ClassUnloadEvent) event;
                    handler.onClassUnload( suspension, unloadEvent.virtualMachine(),
                                           (ClassUnloadRequest) unloadEvent.request(),
                                           unloadEvent.className(), unloadEvent.classSignature() );
                }
                else if ( event instanceof ThreadStartEvent )
                {
                    ThreadStartEvent threadStartEvent = (ThreadStartEvent) event;
                    handler.onThreadStart( suspension, event.virtualMachine(), threadStartEvent.thread(),
                                           (ThreadStartRequest) event.request() );
                }
                else if ( event instanceof ThreadDeathEvent )
                {
                    ThreadDeathEvent threadDeathEvent = (ThreadDeathEvent) event;
                    handler.onThreadDeath( suspension, event.virtualMachine(), threadDeathEvent.thread(),
                                           (ThreadDeathRequest) event.request() );
                }
                else if ( event instanceof VMStartEvent )
                {
                    VMStartEvent startEvent = (VMStartEvent) event;
                    handler.onStart( suspension, startEvent.virtualMachine(), startEvent.thread() );
                }
                else if ( event instanceof VMDeathEvent )
                {
                    VMDeathEvent deathEvent = (VMDeathEvent) event;
                    handler.onDeath( suspension, deathEvent.virtualMachine() );
                }
                else if ( event instanceof VMDisconnectEvent )
                {
                    handler.onDisconnect();
                    return;
                }
                else
                {
                    handler.onUnhandledEvent( suspension, event.virtualMachine(), event.request() );
                }
            }
            events.resume();
        }
    }

    static class Bootstrapper
    {
        private final Map<String, ? extends Connector.Argument> args = connector.defaultArguments();

        String listen() throws IOException
        {
            try
            {
                return String.format( "-agentlib:jdwp=transport=%s,address=%s",
                                      connector.transport().name(),
                                      connector.startListening( args ) );
            }
            catch ( NullPointerException e )
            {
                throw new UnsupportedOperationException( "Debugger not supported" );
            }
            catch ( IllegalConnectorArgumentsException e )
            {
                throw new IOException( "Debugger bootstrapping failed: " + e.getMessage(), e );
            }
        }

        void awaitConnection( DebugHandler handler, Subprocess process ) throws IOException
        {
            VirtualMachine vm;
            try
            {
                vm = connector.accept( args );
            }
            catch ( IllegalConnectorArgumentsException e )
            {
                throw new IOException( "Debugger bootstrapping failed: " + e.getMessage(), e );
            }
            new Thread( new DebugDispatcher( vm, handler, process ),
                        String.format( "Debugger: [%s]", process.pid() ) ).start();
        }

        void stopListening() throws IOException
        {
            try
            {
                connector.stopListening( args );
            }
            catch ( IllegalConnectorArgumentsException e )
            {
                throw new IOException( "Debugger bootstrapping failed: " + e.getMessage(), e );
            }
        }
    }

    private static final ListeningConnector connector;

    static
    {
        ListeningConnector first = null;
        for ( ListeningConnector conn : virtualMachineManager().listeningConnectors() )
        {
            first = conn;
            break;
        }
        connector = first;
    }
}
