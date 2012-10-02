package org.thobe.testing.subprocess;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

public class Subprocess
{
    public static class Starter extends SubprocessConfiguration<Starter>
    {
        private String java = "java";
        private Writer stdOut = new PrintWriter( System.out ), stdErr = new PrintWriter( System.err );
        private String stdOutPrefix, stdErrPrefix;
        private final Class<?> main;
        private final List<String> args = new ArrayList<String>();
        private final List<String> vmargs = new ArrayList<String>();
        private final Set<String> classpath = new HashSet<String>();

        private Starter( Class<?> name )
        {
            this.main = name;
            classpath.add( codeSourceOf( main ) );
        }

        public Starter copyClasspath()
        {
            for ( String path : System.getProperty( "java.class.path" ).split( ":" ) )
            {
                classpath( path );
            }
            return this;
        }

        public Starter classpath( String path )
        {
            classpath.add( path );
            return this;
        }

        protected Starter( Starter that )
        {
            this.main = that.main;
            this.stdOut = that.stdOut;
            this.stdErr = that.stdErr;
            this.stdOutPrefix = that.stdOutPrefix;
            this.stdErrPrefix = that.stdErrPrefix;
            this.args.addAll( that.args );
            this.vmargs.addAll( that.vmargs );
            this.classpath.addAll( that.classpath );
        }

        @Override
        public Starter stdOut( Writer stdOut, String prefix )
        {
            this.stdOut = NullWriter.filter( stdOut );
            stdOutPrefix = prefix;
            return this;
        }

        @Override
        public Starter stdErr( Writer stdErr, String prefix )
        {
            this.stdErr = NullWriter.filter( stdErr );
            stdErrPrefix = prefix;
            return this;
        }

        public Starter arg( String arg )
        {
            args.add( arg );
            return this;
        }

        @Override
        public Starter vmArg( String arg )
        {
            vmargs.add( arg );
            return this;
        }

        public final Subprocess start() throws IOException
        {
            return postStart( internalStart() );
        }

        final Subprocess internalStart() throws IOException
        {
            return new Subprocess( preStart() );
        }

        @Override
        SubprocessConfiguration config()
        {
            return this;
        }

        Starter preStart()
        {
            return this;
        }

        Subprocess postStart( Subprocess subprocess )
        {
            return subprocess;
        }

        List<String> command()
        {
            ArrayList<String> command = new ArrayList<String>();
            command.add( java );
            command.addAll( vmargs );
            command.add( "-cp" );
            command.add( classpath() );
            command.add( main.getName() );
            command.addAll( args );
            return command;
        }

        private String classpath()
        {
            StringBuilder classpath = new StringBuilder();
            String sep = "";
            for ( String path : this.classpath )
            {
                classpath.append( sep ).append( path );
                sep = ":";
            }
            return classpath.toString();
        }

        Writer stdOut()
        {
            return stdOut;
        }

        Writer stdErr()
        {
            return stdErr;
        }

        String stdOutPrefix()
        {
            return stdOutPrefix;
        }

        String stdErrPrefix()
        {
            return stdErrPrefix;
        }
    }

    public static Starter starter( Class<?> main )
    {
        Method method;
        try
        {
            method = main.getMethod( "main", String[].class );
        }
        catch ( NoSuchMethodException e )
        {
            throw new IllegalArgumentException( format( "%s does not specify a main(String[]) method.",
                                                        main.getName() ) );
        }
        int mod = method.getModifiers();
        if ( !isPublic( mod ) || !isStatic( mod ) )
        {
            throw new IllegalArgumentException( format( "%s.main(String[]) is not public and static.",
                                                        main.getName() ) );
        }
        if ( method.getReturnType() != void.class )
        {
            throw new IllegalArgumentException( format( "%s.main(String[]) does not declare void return type.",
                                                        main.getName() ) );
        }
        return new Starter( main );
    }

    private final Process process;
    private final OutputForwarder outForwarder, errForwarder;

    private Subprocess( Starter starter ) throws IOException
    {
        this.process = new ProcessBuilder( starter.command() ).start();
        this.outForwarder = OutputForwarder.create( process.getInputStream(), starter.stdOut,
                                                    prefix( process, starter.main, starter.stdOutPrefix ) );
        this.errForwarder = OutputForwarder.create( process.getErrorStream(), starter.stdErr,
                                                    prefix( process, starter.main, starter.stdErrPrefix ) );
    }

    public String pid()
    {
        return pidOf( process );
    }

    public void kill()
    {
        process.destroy();
    }

    public int awaitTermination( long time, TimeUnit unit ) throws InterruptedException, TimeoutException
    {
        long start = System.nanoTime();
        int result = new AwaitThread( process ).await( time, unit );
        outForwarder.await( time - unit.convert( System.nanoTime() - start, TimeUnit.NANOSECONDS ), unit );
        errForwarder.await( time - unit.convert( System.nanoTime() - start, TimeUnit.NANOSECONDS ), unit );
        return result;
    }

    private static String codeSourceOf( Class<?> cls )
    {
        return cls.getProtectionDomain().getCodeSource().getLocation().getPath();
    }

    private static String prefix( Process process, Class<?> main, String prefix )
    {
        if ( prefix != null )
        {
            return prefix;
        }
        else
        {
            return format( "[%s:%s]", main.getSimpleName(), pidOf( process ) );
        }
    }

    private static String pidOf( Process process )
    {
        try
        {
            Field pid = process.getClass().getDeclaredField( "pid" );
            pid.setAccessible( true );
            return pid.get( process ).toString();
        }
        catch ( Exception e )
        {
            throw new IllegalStateException( "Could not get pid from: " + process, e );
        }
    }

    private static class AwaitThread extends Thread
    {
        private final Process process;
        private final Exchanger<Integer> exitCode = new Exchanger<Integer>();

        AwaitThread( Process process )
        {
            super( "Awaiting: " + process );
            this.process = process;
        }

        @Override
        public void run()
        {
            try
            {
                exitCode.exchange( process.waitFor() );
            }
            catch ( InterruptedException e )
            {
                // time to exit
            }
        }

        int await( long time, TimeUnit unit ) throws InterruptedException, TimeoutException
        {
            start();
            try
            {
                return exitCode.exchange( null, time, unit );
            }
            finally
            {
                interrupt();
            }
        }
    }
}
