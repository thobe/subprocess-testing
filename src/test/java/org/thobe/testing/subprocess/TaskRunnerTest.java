package org.thobe.testing.subprocess;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TaskRunnerTest
{
    @Rule
    public final TestProcesses subprocess = new TestProcesses();

    @Test
    public void shouldExecuteTaskInSubprocess() throws Exception
    {
        // given
        String key = getClass().getName(), value = "hello world";
        Task.Runner<String> runner = subprocess.taskRunner( key ).vmArg( "-D" + key + "=" + value ).start();

        // when
        String result = runner.run( GET_PROPERTY );

        // then
        assertEquals( value, result );
    }

    private static Task<String, String> GET_PROPERTY = new Task<String, String>()
    {
        @Override
        protected String run( String key )
        {
            return System.getProperty( key );
        }
    };
}
