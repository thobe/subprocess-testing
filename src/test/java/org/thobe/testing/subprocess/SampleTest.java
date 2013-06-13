package org.thobe.testing.subprocess;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import static java.util.Arrays.asList;

import static org.junit.Assert.assertEquals;

@RunWith(SubprocessTestRunner.class)
public class SampleTest
{
    @Test
    @Debugger.Using(BreakPoints.class)
    public void shouldHandleRaceCondition()
    {
        new Sample().entryPoint();
    }

    static class BreakPoints extends Debugger
    {
        private final List<Integer> loopVariables = new ArrayList<Integer>();

        {
            enable( "handle_someMethod" );
        }

        @Handler(on = Point.ENTRY, type = Sample.class, method = "someMethod")
        void handle_someMethod()
        {
            enable( "handle_loopBody" );
        }

        @Handler(on = Point.ENTRY, type = Sample.class, method = "loopBody")
        void handle_loopBody()
        {
            loopVariables.add( frame( 1 ).getInt( "i" ) );
        }

        @Override
        protected void finish() throws Exception
        {
            assertEquals( asList( 10, 20, 30, 40, 50, 60, 70, 80, 90 ), loopVariables );
        }
    }
}