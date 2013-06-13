package org.thobe.testing.subprocess;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class Debugger
{
    private DebuggedThread thread;
    private BreakpointRepository breakpoints = new DummyBreakpointRepository();

    protected void finish() throws Exception
    {
        // default: do nothing
    }

    public enum Point
    {
        ENTRY, EXIT
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Using
    {
        Class<? extends Debugger> value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Handler
    {
        Point on() default Point.ENTRY;

        Class<?> type() default Placeholder.class;

        String typeName() default "";

        String method();

        String signature() default "";

        Class<?>[] parameters() default {Placeholder.class};

        DebugHandler.SuspendPolicy suspend() default DebugHandler.SuspendPolicy.SUSPEND_EVENT_THREAD;
    }

    protected final void printStackTrace()
    {
        if ( thread == null )
        {
            throw new IllegalStateException( "Operation only supported while handling breakpoint." );
        }
        thread.printStackTrace( System.err );
    }

    protected final void enable( String breakpoint )
    {
        breakpoints().enable( breakpoint );
    }

    protected final void disable( String breakpoint )
    {
        breakpoints().disable( breakpoint );
    }

    protected final Reference frame( int depth )
    {
        return thread.frame( depth );
    }

    private synchronized BreakpointRepository breakpoints()
    {
        if ( breakpoints == null )
        {
            throw new IllegalStateException( "Debugger has been terminated." );
        }
        return breakpoints;
    }

    final synchronized void initHandler( BreakpointRepository breakpoints )
    {
        this.breakpoints.propagateTo( breakpoints );
        this.breakpoints = breakpoints;
    }

    final synchronized void destroyHandler() throws Exception
    {
        try
        {
            finish();
        }
        finally
        {
            this.breakpoints = null;
        }
    }

    final synchronized void invokeHandler( DebuggedThread thread, Runnable handler )
    {
        try
        {
            this.thread = thread;
            handler.run();
        }
        finally
        {
            this.thread = null;
        }
    }

    static String className( Handler handler )
    {
        Class<?> type = handler.type();
        return type != Placeholder.class ? type.getName() : handler.typeName();
    }

    static Class<?>[] parameters( Handler handler )
    {
        Class<?>[] parameters = handler.parameters();
        if ( parameters.length == 1 && parameters[0] == Placeholder.class )
        {
            return null;
        }
        return parameters;
    }

    private static class Placeholder
    {
    }
}
