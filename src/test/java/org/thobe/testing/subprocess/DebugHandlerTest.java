package org.thobe.testing.subprocess;

import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DebugHandlerTest
{
    @Rule
    public final TestProcesses subprocess = new TestProcesses();

    @Test
    public void shouldSuspendAllThreadsOnVmStartEvent() throws Exception
    {
        // given
        final Exchanger<DebugHandler.SuspendPolicy> policy = new Exchanger<DebugHandler.SuspendPolicy>();
        DebugHandler handler = new DebugHandler()
        {
            @Override
            protected void onStart( SuspendPolicy suspension, VirtualMachine virtualMachine,
                                    ThreadReference thread )
            {
                try
                {
                    policy.exchange( suspension );
                }
                catch ( InterruptedException e )
                {
                    virtualMachine.exit( -1 );
                }
            }
        };

        // when
        Subprocess process = handler.start( subprocess.starter( PrintToStdOut.class ).stdOut( null )
                                                      .arg( "hello world" ) );

        // then
        assertEquals( DebugHandler.SuspendPolicy.SUSPEND_ALL, policy.exchange( null ) );
        process.awaitTermination( 1, TimeUnit.SECONDS );
    }

    @Test
    public void shouldStartWithWaitingStarter() throws Exception
    {
        // given
        DebugHandler handler = new DebugHandler()
        {
        };

        Subprocess.Starter starter = subprocess
                .starterWithCustomOptions( PrintToStdOut.class, TestProcesses.Option.AWAIT_STDOUT_OUTPUT )
                .stdOut( null ).arg( "START" );

        // when
        Subprocess process = handler.start( starter );

        // then
        process.awaitTermination( 1, TimeUnit.SECONDS );
    }
}
