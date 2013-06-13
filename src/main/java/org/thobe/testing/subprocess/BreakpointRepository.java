package org.thobe.testing.subprocess;

import java.util.Collection;

interface BreakpointRepository
{
    void enable( String breakpoint );

    void disable( String breakpoint );

    void enabled( Collection<String> breakpoints );

    void disabled( Collection<String> breakpoints );

    void enableAll();

    void propagateTo( BreakpointRepository next );
}
