package org.thobe.testing.subprocess;

class PrintToStdErr
{
    public static void main( String... args )
    {
        for ( String arg : args )
        {
            System.err.println( arg );
        }
    }
}
