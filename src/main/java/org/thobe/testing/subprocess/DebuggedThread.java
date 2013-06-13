package org.thobe.testing.subprocess;

import java.io.PrintStream;

public interface DebuggedThread
{
    void printStackTrace( PrintStream out );

    Reference frame( int depth );
}
