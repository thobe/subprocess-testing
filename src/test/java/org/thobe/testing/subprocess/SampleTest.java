package org.thobe.testing.subprocess;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@Ignore
@RunWith(SubprocessTestRunner.class)
public class SampleTest
{
    @Test
    @Debugger.Using(BreakPoints.class)
    public void shouldHandleRaceCondition()
    {
        new Sample().theMethod();
    }

    static class BreakPoints extends Debugger
    {
        @Handler(on = Point.ENTRY, type = Sample.class, method = "theMethod")
        void handle_theMethod()
        {
            printStackTrace();
        }
    }
}