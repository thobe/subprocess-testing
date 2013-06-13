package org.thobe.testing.subprocess;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;

class JavaObject extends BasicReference
{
    private final ObjectReference value;

    public JavaObject( ObjectReference value )
    {
        this.value = value;
    }

    @Override
    Value get( String name )
    {
        Field field = value.referenceType().fieldByName( name );
        if ( field == null )
        {
            for ( Field candidate : value.referenceType().fields() )
            {
                if ( name.equals( candidate.name() ) )
                {
                    field = candidate;
                    break;
                }
            }
            if ( field == null )
            {
                throw new IllegalArgumentException( String.format( "'%s' does not have a '%s' field.",
                                                                   value.referenceType().name(), name ) );
            }
        }
        return value.getValue( field );
    }

    @Override
    public boolean isObject()
    {
        return !isArray();
    }

    @Override
    public boolean isArray()
    {
        return value instanceof ArrayReference;
    }

    @Override
    Value get( int offset )
    {
        return array().getValue( offset );
    }

    @Override
    public int arrayLength()
    {
        return array().length();
    }

    private ArrayReference array()
    {
        if ( !(value instanceof ArrayReference) )
        {
            throw new IllegalArgumentException( "not an array" );
        }
        else
        {
            return (ArrayReference) value;
        }
    }
}
