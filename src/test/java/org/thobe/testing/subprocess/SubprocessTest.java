package org.thobe.testing.subprocess;

import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SubprocessTest
{
    @Test
    public void shouldForwardStandardOut() throws Exception
    {
        // given
        StringWriter stdOut = new StringWriter();
        Subprocess.Starter starter = Subprocess.starter( PrintToStdOut.class )
                                               .arg( "hello world" )
                                               .stdOut( stdOut, "" );

        // when
        int exitCode = starter.start().awaitTermination( 5, TimeUnit.SECONDS );

        // then
        assertEquals( 0, exitCode );
        assertEquals( "hello world\n", stdOut.toString() );
    }

    @Test
    public void shouldForwardStandardErr() throws Exception
    {
        // given
        StringWriter stdErr = new StringWriter();
        Subprocess.Starter starter = Subprocess.starter( PrintToStdErr.class )
                                               .arg( "hello world" )
                                               .stdErr( stdErr, "" );

        // when
        int exitCode = starter.start().awaitTermination( 5, TimeUnit.SECONDS );

        // then
        assertEquals( 0, exitCode );
        assertEquals( "hello world\n", stdErr.toString() );
    }

    @Test
    public void shouldPrefixEachStdOutLineWithSpecifiedPrefix() throws Exception
    {
        // given
        StringWriter stdOut = new StringWriter();
        Subprocess.Starter starter = Subprocess.starter( PrintToStdOut.class )
                                               .arg( "hello world" )
                                               .arg( "goodbye world" )
                                               .stdOut( stdOut, "[prefix]" );

        // when
        int exitCode = starter.start().awaitTermination( 5, TimeUnit.SECONDS );

        // then
        assertEquals( 0, exitCode );
        assertEquals( "[prefix] hello world\n[prefix] goodbye world\n", stdOut.toString() );
    }

    @Test
    public void shouldPrefixEachStdErrLineWithSpecifiedPrefix() throws Exception
    {
        // given
        StringWriter stdErr = new StringWriter();
        Subprocess.Starter starter = Subprocess.starter( PrintToStdErr.class )
                                               .arg( "hello world" )
                                               .arg( "goodbye world" )
                                               .stdErr( stdErr, "[prefix]" );

        // when
        int exitCode = starter.start().awaitTermination( 5, TimeUnit.SECONDS );

        // then
        assertEquals( 0, exitCode );
        assertEquals( "[prefix] hello world\n[prefix] goodbye world\n", stdErr.toString() );
    }

    @Test
    public void shouldDefaultPrefixToClassNamePlusPid() throws Exception
    {
        // given
        StringWriter stdOut = new StringWriter();
        Subprocess.Starter starter = Subprocess.starter( PrintToStdOut.class )
                                               .arg( "hello world" )
                                               .stdOut( stdOut );

        // when
        int exitCode = starter.start().awaitTermination( 5, TimeUnit.SECONDS );

        // then
        assertEquals( 0, exitCode );
        String output = stdOut.toString();
        assertTrue( output, output.startsWith( '[' + PrintToStdOut.class.getSimpleName() + ':' ) );
        assertTrue( output, output.endsWith( "] hello world\n" ) );
    }

    @Test
    public void shouldKillProcess() throws Exception
    {
        // given
        Subprocess process = Subprocess.starter( Sleep.class ).arg( "10000" ).start();

        // when
        process.kill();

        // then
        assertEquals( 143, process.awaitTermination( 20, TimeUnit.SECONDS ) );
    }
}
