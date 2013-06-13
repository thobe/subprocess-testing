package org.thobe.testing.subprocess;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.model.InitializationError;

public class SubprocessTestRunner extends Runner
{
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SubprocessConfiguration
    {
        Class<? extends SubprocessConfigurator> value();
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SubprocessRunWith
    {
        Class<? extends Runner> value();
    }

    private final TestProcesses subprocess = new TestProcesses();
    private final Handler handler;
    private final RemoteRunner runner;

    public SubprocessTestRunner( Class<?> testClass ) throws InitializationError
    {
        SubprocessConfiguration config = testClass.getAnnotation( SubprocessConfiguration.class );
        Task.RunnerStarter<RunnerFactory> starter = subprocess.taskRunner( new RunnerFactory( testClass ) );
        try
        {
            if ( config != null )
            {
                config.value().newInstance().configureProcess( starter );
            }
            List<Description> debuggedMethods = collectAnnotated( new ArrayList<Description>(),
                                                                  createRunner( testClass ).getDescription(),
                                                                  Debugger.Using.class );

            Task.Runner<RunnerFactory> runner;
            if ( debuggedMethods.isEmpty() )
            {
                this.handler = null;
                runner = starter.start();
            }
            else
            {
                runner = starter.start( this.handler = new Handler() );
            }
            this.runner = runner.run( RunnerFactory.CREATE_RUNNER );
        }
        catch ( Exception e )
        {
            subprocess.terminate();
            throw new InitializationError( e );
        }
    }

    private static List<Description> collectAnnotated( List<Description> result, Description description,
                                                       Class<? extends Annotation> annotation )
    {
        if ( description.getAnnotation( annotation ) != null )
        {
            result.add( description );
        }
        for ( Description child : description.getChildren() )
        {
            collectAnnotated( result, child, annotation );
        }
        return result;
    }

    private interface RemoteRunner extends Remote
    {
        Description getDescription() throws RemoteException;

        void run( RemoteRunListener listener ) throws RemoteException;
    }

    public Description getDescription()
    {
        try
        {
            return runner.getDescription();
        }
        catch ( RemoteException e )
        {
            subprocess.terminate();
            throw new IllegalStateException( "Subprocess communication failed.", e );
        }
    }

    public void run( RunNotifier notifier )
    {
        try
        {
            runner.run( new LocalNotifier( notifier, handler ) );
        }
        catch ( RemoteException e )
        {
            notifier.fireTestFailure( new Failure( Description.TEST_MECHANISM, e ) );
        }
        finally
        {
            subprocess.terminate();
        }
    }

    private interface RemoteRunListener extends Remote
    {
        void testRunStarted( Description description ) throws RemoteException;

        void testRunFinished( Result result ) throws RemoteException;

        void testStarted( Description description ) throws Exception;

        void testFinished( Description description ) throws RemoteException;

        void testFailure( Failure failure ) throws RemoteException;

        void testAssumptionFailure( Failure failure ) throws RemoteException;

        void testIgnored( Description description ) throws RemoteException;
    }

    private static class LocalNotifier extends UnicastRemoteObject implements RemoteRunListener
    {
        private final RunNotifier notifier;
        private final Handler handler;

        LocalNotifier( RunNotifier notifier, Handler handler ) throws RemoteException
        {
            this.notifier = notifier;
            this.handler = handler;
        }

        @Override
        public void testRunStarted( Description description )
        {
            notifier.fireTestRunStarted( description );
        }

        @Override
        public void testRunFinished( Result result )
        {
            notifier.fireTestRunFinished( result );
        }

        @Override
        public void testStarted( Description description ) throws Exception
        {
            if ( handler != null )
            {
                handler.setup( description, notifier );
            }
            notifier.fireTestStarted( description );
        }

        @Override
        public void testFinished( Description description )
        {
            if ( handler != null )
            {
                handler.destroy( description, notifier );
            }
            notifier.fireTestFinished( description );
        }

        @Override
        public void testFailure( Failure failure )
        {
            notifier.fireTestFailure( failure );
        }

        @Override
        public void testAssumptionFailure( Failure failure )
        {
            notifier.fireTestAssumptionFailed( failure );
        }

        @Override
        public void testIgnored( Description description )
        {
            notifier.fireTestIgnored( description );
        }
    }

    private static final class RunnerFactory implements Serializable
    {
        static final Task<RunnerFactory, RemoteRunner> CREATE_RUNNER = new Task<RunnerFactory, RemoteRunner>()
        {
            @Override
            protected SubprocessTestRunner.RemoteRunner run( RunnerFactory runnerFactory ) throws RemoteException
            {
                return runnerFactory.createRunner();
            }
        };

        private final String test;

        RunnerFactory( Class<?> testClass )
        {
            this.test = testClass.getName();
        }

        RemoteRunner createRunner() throws RemoteException
        {
            Class<?> testClass;
            try
            {
                testClass = Class.forName( test );
            }
            catch ( ClassNotFoundException e )
            {
                throw new IllegalArgumentException( "Subprorocess does not have access to the test class.", e );
            }
            return new LocalRunner( SubprocessTestRunner.createRunner( testClass ) );
        }
    }

    private static Runner createRunner( Class<?> testClass )
    {
        SubprocessRunWith runWith = testClass.getAnnotation( SubprocessRunWith.class );
        Class<? extends Runner> runnerClass;
        if ( runWith != null )
        {
            runnerClass = runWith.value();
        }
        else
        {
            runnerClass = JUnit4.class;
        }
        Constructor<? extends Runner> constructor;
        try
        {
            constructor = runnerClass.getConstructor( Class.class );
        }
        catch ( NoSuchMethodException e )
        {
            throw new IllegalArgumentException( "Test Runner class " + runnerClass.getName() +
                                                " does not have a public constructor with a single class argument.",
                                                e );
        }
        Runner runner;
        try
        {
            runner = constructor.newInstance( testClass );
        }
        catch ( InvocationTargetException e )
        {
            Throwable exc = e.getTargetException();
            if ( exc instanceof Error )
            {
                throw (Error) exc;
            }
            if ( exc instanceof RuntimeException )
            {
                throw (RuntimeException) exc;
            }
            throw new IllegalArgumentException( "Could not instantiate test Runner " + runnerClass.getName(), exc );
        }
        catch ( Exception e )
        {
            throw new IllegalArgumentException( "Could not instantiate test Runner " + runnerClass.getName(), e );
        }
        return runner;
    }

    private static class LocalRunner extends UnicastRemoteObject implements RemoteRunner
    {
        private final Runner runner;

        LocalRunner( Runner runner ) throws RemoteException
        {
            this.runner = runner;
        }

        @Override
        public Description getDescription()
        {
            return runner.getDescription();
        }

        @Override
        public void run( RemoteRunListener listener )
        {
            RunNotifier notifier = new RunNotifier();
            notifier.addFirstListener( new LocalRunListener( listener ) );
            runner.run( notifier );
        }
    }

    private static class LocalRunListener extends RunListener
    {
        private final RemoteRunListener listener;

        LocalRunListener( RemoteRunListener listener )
        {
            this.listener = listener;
        }

        @Override
        public void testRunStarted( Description description ) throws Exception
        {
            listener.testRunStarted( description );
        }

        @Override
        public void testRunFinished( Result result ) throws Exception
        {
            listener.testRunFinished( result );
        }

        @Override
        public void testStarted( Description description ) throws Exception
        {
            listener.testStarted( description );
        }

        @Override
        public void testFinished( Description description ) throws Exception
        {
            listener.testFinished( description );
        }

        @Override
        public void testFailure( Failure failure ) throws Exception
        {
            listener.testFailure( failure );
        }

        @Override
        public void testAssumptionFailure( Failure failure )
        {
            try
            {
                listener.testAssumptionFailure( failure );
            }
            catch ( RemoteException e )
            {
                e.printStackTrace();
            }
        }

        @Override
        public void testIgnored( Description description ) throws Exception
        {
            listener.testIgnored( description );
        }
    }

    private static class Handler extends DebugHandler
    {
        private final AtomicReference<DebuggerManager> debugger = new AtomicReference<DebuggerManager>();
        private VirtualMachine vm;

        synchronized void setup( Description description, RunNotifier notifier )
        {
            Debugger.Using debugUsing = description.getAnnotation( Debugger.Using.class );
            if ( debugUsing != null )
            {
                try
                {
                    DebuggerManager debugger = new DebuggerManager( debugUsing.value().newInstance(), this, vm );
                    this.debugger.set( debugger );
                }
                catch ( Throwable e )
                {
                    notifier.fireTestFailure( new Failure( description, e ) );
                }
            }
        }

        void destroy( Description description, RunNotifier notifier )
        {
            DebuggerManager debugger = this.debugger.getAndSet( null );
            if ( debugger != null )
            {
                try
                {
                    debugger.destroy( vm );
                }
                catch ( Throwable e )
                {
                    notifier.fireTestFailure( new Failure( description, e ) );
                }
            }
        }

        @Override
        protected synchronized void onStart( SuspendPolicy suspension, VirtualMachine virtualMachine,
                                             ThreadReference thread )
        {
            this.vm = virtualMachine;
        }

        @Override
        protected void onDisconnect()
        {
            this.vm = null;
        }

        @Override
        protected void onMethodEntry( SuspendPolicy suspension, VirtualMachine virtualMachine,
                                      ThreadReference thread, MethodEntryRequest request, Method method,
                                      Location location )
        {
            debugger.get().invokeHandle( request, method, thread );
        }

        @Override
        protected void onMethodExit( SuspendPolicy suspension, VirtualMachine virtualMachine,
                                     ThreadReference thread, MethodExitRequest request, Method method,
                                     Value value, Location location )
        {
            debugger.get().invokeHandle( request, method, thread );
        }
    }
}
