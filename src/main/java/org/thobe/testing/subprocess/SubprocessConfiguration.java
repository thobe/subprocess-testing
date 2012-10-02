package org.thobe.testing.subprocess;

import java.io.Writer;

public abstract class SubprocessConfiguration<T extends SubprocessConfiguration<T>>
{
    public T stdOut( Writer stdOut, String prefix )
    {
        config().stdOut( stdOut, prefix );
        return cast();
    }

    public final T stdOut( Writer stdOut )
    {
        return stdOut( stdOut, null );
    }

    public T stdErr( Writer stdErr, String prefix )
    {
        config().stdErr( stdErr, prefix );
        return cast();
    }

    public final T stdErr( Writer stdErr )
    {
        return stdErr( stdErr, null );
    }

    public T vmArg( String arg )
    {
        config().vmArg( arg );
        return cast();
    }

    public T config( SubprocessConfigurator config )
    {
        config.configureProcess( this );
        return cast();
    }

    abstract SubprocessConfiguration config();

    SubprocessConfiguration()
    {
        // limited subclasses
    }

    @SuppressWarnings("unchecked")
    private T cast()
    {
        return (T) this;
    }
}
