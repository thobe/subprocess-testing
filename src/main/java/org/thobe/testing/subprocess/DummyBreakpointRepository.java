package org.thobe.testing.subprocess;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class DummyBreakpointRepository implements BreakpointRepository
{
    private Set<String> enabled, disabled;

    private Set<String> enable()
    {
        if ( enabled == null )
        {
            enabled = new HashSet<String>();
        }
        return enabled;
    }

    private Set<String> disable()
    {
        if ( disabled == null )
        {
            disabled = new HashSet<String>();
        }
        return disabled;
    }

    @Override
    public void enable( String breakpoint )
    {
        enable().add( breakpoint );
    }

    @Override
    public void disable( String breakpoint )
    {
        disable().add( breakpoint );
    }

    @Override
    public void enabled( Collection<String> breakpoints )
    {
        enable().addAll( breakpoints );
    }

    @Override
    public void disabled( Collection<String> breakpoints )
    {
        disable().addAll( breakpoints );
    }

    @Override
    public void propagateTo( BreakpointRepository next )
    {
        if ( enabled == null )
        {
            next.enableAll();
        }
        if ( disabled != null )
        {
            next.disabled( disabled );
        }
        if ( enabled != null )
        {
            next.enabled( enabled );
        }
    }

    @Override
    public void enableAll()
    {
        enabled = disabled = null;
    }
}
