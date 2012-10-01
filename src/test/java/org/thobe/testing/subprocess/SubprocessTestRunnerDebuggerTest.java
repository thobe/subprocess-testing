package org.thobe.testing.subprocess;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

@RunWith(SubprocessTestRunner.class)
@Ignore("Doesn't work yet")
public class SubprocessTestRunnerDebuggerTest
{
    @Test
    @Debugger.Using(BreakPoints.class)
    public void shouldTriggerBreakpoint()
    {
        new Target().theMethod();
    }

    static class BreakPoints extends Debugger
    {
        boolean invoked = false;

        @Handler(on = Point.ENTRY, type = Target.class, method = "theMethod")
        void handle_theMethod()
        {
            invoked = true;
        }

        @Override
        protected void finish()
        {
            assertTrue( "The breakpoint was never invoked", invoked );
        }
    }

    private static class Target
    {
        void theMethod()
        {
        }
    }
}
