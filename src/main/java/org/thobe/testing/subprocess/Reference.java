package org.thobe.testing.subprocess;

public interface Reference
{
    boolean getBoolean( String name );

    boolean getBoolean( int offset );

    byte getByte( String name );

    byte getByte( int offset );

    short getShort( String name );

    short getShort( int offset );

    char getChar( String name );

    char getChar( int offset );

    int getInt( String name );

    int getInt( int offset );

    long getLong( String name );

    long getLong( int offset );

    float getFloat( String name );

    float getFloat( int offset );

    double getDouble( String name );

    double getDouble( int offset );

    String getString( String name );

    String getString( int offset );

    Reference getObject( String name );

    Reference getObject( int offset );

    boolean isObject();

    boolean isArray();

    int arrayLength();
}
