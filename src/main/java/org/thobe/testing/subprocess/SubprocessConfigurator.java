package org.thobe.testing.subprocess;

public interface SubprocessConfigurator
{
    <T extends SubprocessConfiguration<T>> void configureProcess( T starter );
}
