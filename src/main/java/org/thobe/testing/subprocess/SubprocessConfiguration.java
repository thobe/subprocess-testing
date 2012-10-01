package org.thobe.testing.subprocess;

import java.io.Writer;

public interface SubprocessConfiguration<T extends SubprocessConfiguration<T>>
{
    T stdOut( Writer stdOut, String prefix );

    T stdOut( Writer stdOut );

    T stdErr( Writer stdErr, String prefix );

    T stdErr( Writer stdErr );

    T vmArg( String arg );
}
