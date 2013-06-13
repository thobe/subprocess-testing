package org.thobe.testing.subprocess;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;

class Frame extends BasicReference
{
    private final StackFrame frame;

    Frame( StackFrame frame )
    {
        this.frame = frame;
    }

    @Override
    Value get( String name )
    {
        try
        {
            LocalVariable local = frame.visibleVariableByName( name );
            if ( local == null )
            {
                throw new IllegalArgumentException( String.format( "'%s' is not visible in this scope.", name ) );
            }
            return frame.getValue( local );
        }
        catch ( AbsentInformationException e )
        {
            throw new IllegalStateException( e );
        }
    }

    @Override
    public boolean isArray()
    {
        return false;
    }

    @Override
    public boolean isObject()
    {
        return false;
    }

    @Override
    Value get( int offset )
    {
        throw new UnsupportedOperationException( "StackFrame does not support offset based access." );
    }

    @Override
    public int arrayLength()
    {
        throw new UnsupportedOperationException( "StackFrame does not support offset based access." );
    }
}
