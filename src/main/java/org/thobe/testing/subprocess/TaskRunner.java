package org.thobe.testing.subprocess;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

class TaskRunner<STATE> extends UnicastRemoteObject implements Task.RemoteRunner<STATE>
{
    @Override
    public <RESULT> RESULT run( Task<STATE, RESULT> task ) throws Exception
    {
        return task.run( state );
    }

    interface RemoteBootstrapper<STATE> extends Remote
    {
        void start( Task.RemoteRunner<STATE> runner ) throws RemoteException, InterruptedException;
    }

    static class Bootstrapper<STATE> extends UnicastRemoteObject implements RemoteBootstrapper<STATE>
    {
        private final Exchanger<Task.RemoteRunner<STATE>> exchanger = new Exchanger<Task.RemoteRunner<STATE>>();

        Bootstrapper() throws RemoteException
        {
        }

        @Override
        public void start( Task.RemoteRunner<STATE> runner ) throws InterruptedException
        {
            exchanger.exchange( runner );
        }

        Task.RemoteRunner<STATE> await( long timeout, TimeUnit unit ) throws TimeoutException, InterruptedException
        {
            return exchanger.exchange( null, timeout, unit );
        }
    }

    private final STATE state;

    private TaskRunner( STATE state ) throws RemoteException
    {
        this.state = state;
    }

    @SuppressWarnings("unchecked")
    public static void main( String[] args ) throws Exception
    {
        deserialize( RemoteBootstrapper.class, args[1] )
                .start( new TaskRunner( deserialize( Object.class, args[0] ) ) );
    }

    static String serialize( Object object ) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new ObjectOutputStream( out ).writeObject( object );
        return new BASE64Encoder().encode( out.toByteArray() );
    }

    private static <T> T deserialize( Class<T> type, String buffer ) throws IOException, ClassNotFoundException
    {
        byte[] bytes = new BASE64Decoder().decodeBuffer( buffer );
        return type.cast( new ObjectInputStream( new ByteArrayInputStream( bytes ) ).readObject() );
    }
}
