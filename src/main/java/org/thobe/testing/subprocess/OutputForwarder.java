package org.thobe.testing.subprocess;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class OutputForwarder
{
    private static ForwardingThread THREAD;

    public static OutputForwarder create( InputStream source, Writer target, String prefix )
    {
        OutputForwarder forwarder = new OutputForwarder( source, target, prefix );
        add( forwarder );
        return forwarder;
    }

    private final InputStream source;
    private final PrintWriter target;
    private final StringBuilder line;
    private final int resetPoint;
    private final CountDownLatch completionLatch = new CountDownLatch( 1 );
    private volatile boolean terminated;

    private OutputForwarder( InputStream source, Writer target, String prefix )
    {
        this.source = source;
        if ( target instanceof PrintWriter )
        {
            this.target = (PrintWriter) target;
        }
        else
        {
            this.target = new PrintWriter( target );
        }
        StringBuilder line = new StringBuilder( prefix );
        if ( prefix.isEmpty() )
        {
            this.resetPoint = 0;
        }
        else
        {
            line.append( ' ' );
            this.resetPoint = prefix.length() + 1;
        }
        this.line = line;
    }

    @Override
    public String toString()
    {
        return "OutputForwarder{" + line.substring( 0, Math.max( resetPoint - 1, 0 ) ) + '}';
    }

    void await( long time, TimeUnit unit ) throws InterruptedException, TimeoutException
    {
        terminated = true;
        if ( !completionLatch.await( time, unit ) )
        {
            throw new TimeoutException( String.format( "%s did not complete within %s %s", this, time, unit ) );
        }
    }

    boolean run()
    {
        try
        {
            int available = source.available();
            if ( available != 0 )
            {
                byte[] data = new byte[available];
                ByteBuffer chars = ByteBuffer.wrap( data, 0, source.read( data ) );
                while ( chars.hasRemaining() )
                {
                    char c = (char) chars.get();
                    line.append( c );
                    if ( c == '\n' )
                    {
                        print();
                    }
                }
            }
            else if ( terminated )
            {
                int c = source.read();
                if ( c == -1 )
                {
                    completionLatch.countDown();
                    return false;
                }
                else if ( c == '\n' )
                {
                    print();
                }
                else
                {
                    line.append( c );
                }
            }
        }
        catch ( IOException e )
        {
            if ( line.length() > resetPoint )
            {
                line.append( '\n' );
                print();
            }
            completionLatch.countDown();
            return false;
        }
        return true;
    }

    private void print()
    {
        target.write( line.toString() );
        target.flush();
        line.setLength( resetPoint );
    }

    private static class ForwardingThread extends Thread
    {
        private final Set<OutputForwarder> forwarders = new CopyOnWriteArraySet<OutputForwarder>();

        ForwardingThread()
        {
            super( Subprocess.class.getSimpleName() + " " + OutputForwarder.class.getSimpleName() );
            setDaemon( true );
        }

        @Override
        public void run()
        {
            for (; ; )
            {
                for ( OutputForwarder forwarder : forwarders )
                {
                    if ( !forwarder.run() )
                    {
                        forwarders.remove( forwarder );
                    }
                }
                if ( forwarders.isEmpty() && terminateForwardingThread( this ) )
                {
                    return;
                }
            }
        }
    }

    private static void add( OutputForwarder forwarder )
    {
        ForwardingThread thread = THREAD;
        boolean start = false;
        if ( thread == null )
        {
            start = true;
            THREAD = thread = new ForwardingThread();
        }
        thread.forwarders.add( forwarder );
        if ( start )
        {
            thread.start();
        }
    }

    private static synchronized boolean terminateForwardingThread( ForwardingThread thread )
    {
        if ( THREAD != thread )
        {
            return true;
        }
        if ( thread.forwarders.isEmpty() )
        {
            THREAD = null;
            return true;
        }
        return false;
    }
}
