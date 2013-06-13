package org.thobe.testing.subprocess;

import com.sun.jdi.BooleanValue;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.Field;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LongValue;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveType;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.ShortValue;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;

import static java.lang.String.format;

abstract class BasicReference implements Reference
{
    abstract Value get( String name );

    abstract Value get( int offset );

    @Override
    public boolean getBoolean( String name )
    {
        return primitive( BooleanValue.class, Boolean.class, name, get( name ) ).booleanValue();
    }

    @Override
    public boolean getBoolean( int offset )
    {
        return primitive( BooleanValue.class, Boolean.class, offset, get( offset ) ).booleanValue();
    }

    @Override
    public byte getByte( String name )
    {
        return primitive( ByteValue.class, Byte.class, name, get( name ) ).byteValue();
    }

    @Override
    public byte getByte( int offset )
    {
        return primitive( ByteValue.class, Byte.class, offset, get( offset ) ).byteValue();
    }

    @Override
    public short getShort( String name )
    {
        return primitive( ShortValue.class, Short.class, name, get( name ) ).shortValue();
    }

    @Override
    public short getShort( int offset )
    {
        return primitive( ShortValue.class, Short.class, offset, get( offset ) ).shortValue();
    }

    @Override
    public char getChar( String name )
    {
        return primitive( CharValue.class, Character.class, name, get( name ) ).charValue();
    }

    @Override
    public char getChar( int offset )
    {
        return primitive( CharValue.class, Character.class, offset, get( offset ) ).charValue();
    }

    @Override
    public final int getInt( String name )
    {
        return primitive( IntegerValue.class, Integer.class, name, get( name ) ).intValue();
    }

    @Override
    public final int getInt( int offset )
    {
        return primitive( IntegerValue.class, Integer.class, offset, get( offset ) ).intValue();
    }

    @Override
    public long getLong( String name )
    {
        return primitive( LongValue.class, Long.class, name, get( name ) ).longValue();
    }

    @Override
    public long getLong( int offset )
    {
        return primitive( LongValue.class, Long.class, offset, get( offset ) ).longValue();
    }

    @Override
    public float getFloat( String name )
    {
        return primitive( FloatValue.class, Float.class, name, get( name ) ).floatValue();
    }

    @Override
    public float getFloat( int offset )
    {
        return primitive( FloatValue.class, Float.class, offset, get( offset ) ).floatValue();
    }

    @Override
    public double getDouble( String name )
    {
        return primitive( DoubleValue.class, Double.class, name, get( name ) ).doubleValue();
    }

    @Override
    public double getDouble( int offset )
    {
        return primitive( DoubleValue.class, Double.class, offset, get( offset ) ).doubleValue();
    }

    @Override
    public String getString( String name )
    {
        return string( name, get( name ) );
    }

    @Override
    public String getString( int offset )
    {
        return string( offset, get( offset ) );
    }

    @Override
    public Reference getObject( String name )
    {
        return object( name, get( name ) );
    }

    @Override
    public Reference getObject( int offset )
    {
        return object( offset, get( offset ) );
    }

    private <T extends PrimitiveValue> T primitive( Class<T> type, Class<?> boxedType, Object key, Value value )
    {
        if ( type.isInstance( value ) )
        {
            return type.cast( value );
        }
        if ( value instanceof ObjectReference )
        {
            ObjectReference reference = (ObjectReference) value;
            if ( boxedType.getName().equals( reference.type().name() ) )
            {
                for ( Field field : reference.referenceType().fields() )
                {
                    try
                    {
                        if ( field.isStatic() && field.type() instanceof PrimitiveType )
                        {
                            return type.cast( reference.getValue( field ) );
                        }
                    }
                    catch ( ClassNotLoadedException e )
                    {
                        throw new IllegalStateException( e );
                    }
                }
            }
            return null;
        }
        throw invalidType( key, boxedType.getSimpleName().toLowerCase() );
    }

    private String string( Object key, Value value )
    {
        if ( value instanceof StringReference )
        {
            return ((StringReference) value).value();
        }
        if ( value == null )
        {
            return null;
        }
        throw invalidType( key, "String" );
    }

    private Reference object( Object key, Value value )
    {
        if ( value instanceof ObjectReference )
        {
            return new JavaObject( (ObjectReference) value );
        }
        if ( value == null )
        {
            return null;
        }
        throw invalidType( key, "Object" );
    }

    private IllegalArgumentException invalidType( Object key, String expectedType )
    {
        return new IllegalArgumentException(
                format( (key instanceof String ? "'%s' is not a %s" : "[%s] is not a %s"), key, expectedType ) );
    }
}
