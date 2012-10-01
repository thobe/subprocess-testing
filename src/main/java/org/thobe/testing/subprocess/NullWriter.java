package org.thobe.testing.subprocess;

import java.io.IOException;
import java.io.Writer;

class NullWriter extends Writer
{
    private static final Writer WRITER = new NullWriter();

    private NullWriter()
    {
        // singleton
    }

    @Override
    public void write( char[] cbuf, int off, int len ) throws IOException
    {
        // do nothing
    }

    @Override
    public void flush() throws IOException
    {
        // do nothing
    }

    @Override
    public void close() throws IOException
    {
        // do nothing
    }

    static Writer filter( Writer writer )
    {
        return writer == null ? WRITER : writer;
    }
}
