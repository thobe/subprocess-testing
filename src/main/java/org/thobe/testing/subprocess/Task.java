package org.thobe.testing.subprocess;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class Task<STATE, RESULT> implements Serializable
{
    public static <STATE> RunnerStarter<STATE> runner( STATE state )
    {
        try
        {
            return new RunnerStarter<STATE>( Subprocess.starter( TaskRunner.class )
                                                        .copyClasspath()
                                                        .arg( TaskRunner.serialize( state ) ) );
        }
        catch ( IOException e )
        {
            throw new IllegalArgumentException( state + " is not serializable", e );
        }
    }

    protected abstract RESULT run( STATE state ) throws Exception;

    public static final class Runner<STATE>
    {
        private final Subprocess process;
        private final RemoteRunner<STATE> runner;

        public <RESULT> RESULT run( Task<STATE, RESULT> task ) throws Exception
        {
            return runner.run( task );
        }

        private Runner( Subprocess process, RemoteRunner<STATE> runner )
        {
            this.process = process;
            this.runner = runner;
        }
    }

    interface RemoteRunner<STATE> extends Remote
    {
        <RESULT> RESULT run( Task<STATE, RESULT> task ) throws Exception;
    }

    public static final class RunnerStarter<STATE> implements SubprocessConfiguration<RunnerStarter<STATE>>
    {
        final Subprocess.Starter starter;
        private long timeout = 5;
        private TimeUnit timeoutUnit = TimeUnit.SECONDS;

        RunnerStarter( Subprocess.Starter starter )
        {
            this.starter = starter;
        }

        public RunnerStarter<STATE> startupTimeout( long timeout, TimeUnit timeoutUnit )
        {
            this.timeout = timeout;
            this.timeoutUnit = timeoutUnit;
            return this;
        }

        public RunnerStarter<STATE> vmArg( String arg )
        {
            starter.vmArg( arg );
            return this;
        }

        public RunnerStarter<STATE> stdOut( Writer stdOut, String prefix )
        {
            starter.stdOut( stdOut, prefix );
            return this;
        }

        public RunnerStarter<STATE> stdOut( Writer stdOut )
        {
            starter.stdOut( stdOut );
            return this;
        }

        public RunnerStarter<STATE> stdErr( Writer stdErr, String prefix )
        {
            starter.stdErr( stdErr, prefix );
            return this;
        }

        public RunnerStarter<STATE> stdErr( Writer stdErr )
        {
            starter.stdErr( stdErr );
            return this;
        }

        public Runner<STATE> start() throws IOException, TimeoutException, InterruptedException
        {
            TaskRunner.Bootstrapper<STATE> bootstrapper = bootstrapper();
            Subprocess process = starter.start();
            return new Runner<STATE>( process, bootstrapper.await( timeout, timeoutUnit ) );
        }

        public Runner<STATE> start( DebugHandler debugger )
                throws IOException, TimeoutException, InterruptedException
        {
            TaskRunner.Bootstrapper<STATE> bootstrapper = bootstrapper();
            Subprocess process = debugger.start( starter );
            return new Runner<STATE>( process, bootstrapper.await( timeout, timeoutUnit ) );
        }

        private TaskRunner.Bootstrapper<STATE> bootstrapper() throws IOException
        {
            TaskRunner.Bootstrapper<STATE> bootstrapper = new TaskRunner.Bootstrapper<STATE>();
            starter.arg( TaskRunner.serialize( RemoteObject.toStub( bootstrapper ) ) );
            return bootstrapper;
        }
    }
}
