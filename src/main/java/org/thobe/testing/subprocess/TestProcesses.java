package org.thobe.testing.subprocess;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static java.util.concurrent.TimeUnit.SECONDS;

public class TestProcesses implements TestRule
{
    private final EnumSet<Option> options;

    public TestProcesses( Option... options )
    {
        this.options = options( options );
    }

    public Subprocess.Starter starter( Class<?> main )
    {
        return new ProcessStarter( Subprocess.starter( main ), options );
    }

    public Subprocess.Starter starterWithCustomOptions( Class<?> main, Option... options )
    {
        return new ProcessStarter( Subprocess.starter( main ), options( options ) );
    }

    public <STATE> Task.RunnerStarter<STATE> taskRunner( STATE state )
    {
        return new Task.RunnerStarter<STATE>( new ProcessStarter( Task.runner( state ).starter, options ) );
    }

    public <STATE> Task.RunnerStarter<STATE> taskRunnerWithCustomOptions( STATE state, Option... options )
    {
        return new Task.RunnerStarter<STATE>( new ProcessStarter( Task.runner( state ).starter, options( options ) ) );
    }

    private static EnumSet<Option> options( Option[] options )
    {
        EnumSet<Option> override = EnumSet.noneOf( Option.class );
        Collections.addAll( override, options );
        return override;
    }

    public enum Option
    {
        AWAIT_STDOUT_OUTPUT
        {
            @Override
            Object pre( Subprocess.Starter starter )
            {
                WaitingWriter writer = new WaitingWriter( starter.stdOut() );
                starter.stdOut( writer, starter.stdOutPrefix() );
                return writer;
            }

            @Override
            void post( Subprocess.Starter starter, Object state )
            {
                await( (WaitingWriter) state );
            }
        },
        AWAIT_STDERR_OUTPUT
        {
            @Override
            Object pre( Subprocess.Starter starter )
            {
                WaitingWriter writer = new WaitingWriter( starter.stdErr() );
                starter.stdErr( writer, starter.stdErrPrefix() );
                return writer;
            }

            @Override
            void post( Subprocess.Starter starter, Object state )
            {
                await( (WaitingWriter) state );
            }
        };

        abstract Object pre( Subprocess.Starter starter );

        abstract void post( Subprocess.Starter starter, Object state );

    }

    private static void await( WaitingWriter state )
    {
        try
        {
            ((WaitingWriter) state).awaitWrite( 5, SECONDS );
        }
        catch ( TimeoutException e )
        {
            throw new AssertionError( e );
        }
    }

    private class ProcessStarter extends Subprocess.Starter
    {
        private final Set<Option> options;
        Map<Option, Object> state = new EnumMap<Option, Object>( Option.class );

        ProcessStarter( Subprocess.Starter starter, Set<Option> options )
        {
            super( starter );
            this.options = options;
        }

        @Override
        protected Subprocess.Starter preStart()
        {
            for ( Option option : options )
            {
                state.put( option, option.pre( this ) );
            }
            return super.preStart();
        }

        @Override
        protected Subprocess postStart( Subprocess subprocess )
        {
            processes.add( subprocess );
            for ( Option option : options )
            {
                option.post( this, state.get( option ) );
            }
            return subprocess;
        }
    }

    private final List<Subprocess> processes = new ArrayList<Subprocess>();

    @Override
    public Statement apply( final Statement base, Description description )
    {
        return new Statement()
        {
            @Override
            public void evaluate() throws Throwable
            {
                try
                {
                    base.evaluate();
                }
                finally
                {
                    terminate();
                }
            }
        };
    }

    void terminate()
    {
        for ( Subprocess process : processes )
        {
            process.kill();
            try
            {
                process.awaitTermination( 1, SECONDS );
            }
            catch ( Exception e )
            {
                // ignore
            }
        }
    }

    private static class WaitingWriter extends Writer
    {
        private final Writer writer;
        private volatile boolean written;
        private final Queue<Thread> waiters = new ArrayBlockingQueue<Thread>( 4 );

        public WaitingWriter( Writer writer )
        {
            this.writer = writer;
        }

        @Override
        public void write( char[] cbuf, int off, int len ) throws IOException
        {
            wakeUp();
            writer.write( cbuf, off, len );
        }

        @Override
        public void flush() throws IOException
        {
            wakeUp();
            writer.flush();
        }

        @Override
        public void close() throws IOException
        {
            wakeUp();
            writer.close();
        }

        void awaitWrite( long timeout, TimeUnit unit ) throws TimeoutException
        {
            timeout = unit.toNanos( timeout );
            Thread current = Thread.currentThread();
            for ( long end = timeout + System.nanoTime(); !written; )
            {
                waiters.add( current );
                LockSupport.parkNanos( this, timeout );
                waiters.remove( current );
                if ( System.nanoTime() > end )
                {
                    throw new TimeoutException();
                }
            }
        }

        private void wakeUp()
        {
            written = true;
            for ( Thread thread; null != (thread = waiters.poll()); )
            {
                LockSupport.unpark( thread );
            }
        }
    }
}
