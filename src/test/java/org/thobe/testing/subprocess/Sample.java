package org.thobe.testing.subprocess;

public class Sample
{
    public void entryPoint()
    {
        for ( int i = 0; i < 10; i++ )
        {
            loopBody();
        }
        someMethod();
        for ( int i = 10; i < 100; i += 10 )
        {
            loopBody();
        }
    }

    private void someMethod()
    {
        // do nothing
    }

    private void loopBody()
    {
        // do nothing
    }
}
