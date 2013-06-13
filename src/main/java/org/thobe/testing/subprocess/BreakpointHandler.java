package org.thobe.testing.subprocess;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;

class BreakpointHandler
{
    private final Debugger debugger;
    private final Method method;
    final EventRequest request;

    public BreakpointHandler( Debugger debugger, EventRequestBuilder request, Method method )
    {
        method.setAccessible( true );
        this.debugger = debugger;
        this.method = method;
        Debugger.Handler handler = method.getAnnotation( Debugger.Handler.class );
        switch ( handler.on() )
        {
        case ENTRY:
            MethodEntryRequest methodEntry = request.methodEntry();
            methodEntry.addClassFilter( Debugger.className( handler ) );
            this.request = methodEntry;
            break;
        case EXIT:
            MethodExitRequest methodExit = request.methodExit();
            methodExit.addClassFilter( Debugger.className( handler ) );
            this.request = methodExit;
            break;
        default:
            throw new IllegalArgumentException( "Invalid point: " + handler.on() );
        }
    }

    void invoke( com.sun.jdi.Method method, ThreadReference thread )
    {
        Debugger.Handler handler = this.method.getAnnotation( Debugger.Handler.class );
        if ( handler.method().equals( method.name() ) )
        {
            String signature = handler.signature();
            if ( signature.isEmpty() )
            {
                Class[] parameters = Debugger.parameters( handler );
                if ( parameters != null )
                {
                    List<String> types = method.argumentTypeNames();
                    if ( parameters.length != types.size() )
                    {
                        return;
                    }
                    for ( int i = 0; i < parameters.length; i++ )
                    {
                        if ( !parameters[i].getName().equals( types.get( i ) ) )
                        {
                            return;
                        }
                    }
                }
            }
            else if ( !signature.equals( method.signature() ) )
            {
                return;
            }
            debugger.invokeHandler( new DebuggedThreadReference( thread ), handler( this.method, debugger ) );
        }
    }

    private static Runnable handler( final Method method, final Debugger debugger )
    {
        return new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    method.invoke( debugger );
                }
                catch ( IllegalAccessException e )
                {
                    throw new IllegalStateException( "Method should be accessible." );
                }
                catch ( InvocationTargetException e )
                {
                    Throwable exception = e.getTargetException();
                    if ( exception instanceof RuntimeException )
                    {
                        throw (RuntimeException) exception;
                    }
                    if ( exception instanceof Error )
                    {
                        throw (Error) exception;
                    }
                    throw new IllegalArgumentException( "Unexpected exception: " + exception, exception );
                }
            }
        };
    }
}
