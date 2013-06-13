package org.thobe.testing.subprocess;

import java.io.PrintStream;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;

class DebuggedThreadReference implements DebuggedThread
{
    private final ThreadReference thread;

    DebuggedThreadReference( ThreadReference thread )
    {
        this.thread = thread;
    }

    @Override
    public void printStackTrace( PrintStream out )
    {
        try
        {
            out.println( "Thread: \"" + thread.name() + '"' );
            for ( StackFrame frame : thread.frames() )
            {
                Location location = frame.location();
                StackTraceElement trace = new StackTraceElement( location.declaringType().name(),
                                                                 location.method().name(),
                                                                 sourceFileName( location ),
                                                                 location.lineNumber() );
                out.println( "\tat " + trace );
            }
        }
        catch ( IncompatibleThreadStateException e )
        {
            throw new IllegalStateException( e );
        }
    }

    @Override
    public Reference frame( int depth )
    {
        try
        {
            return new Frame(thread.frame( depth ) );
        }
        catch ( IncompatibleThreadStateException e )
        {
            throw new IllegalStateException( e );
        }
    }

    private static String sourceFileName( Location location )
    {
        try
        {
            return location.sourceName();
        }
        catch ( AbsentInformationException e )
        {
            return null;
        }
    }
}
