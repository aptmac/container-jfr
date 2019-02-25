package es.andrewazor.containertest.commands.internal;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.management.remote.JMXServiceURL;

import org.openjdk.jmc.rjmx.IConnectionListener;
import org.openjdk.jmc.rjmx.internal.DefaultConnectionHandle;
import org.openjdk.jmc.rjmx.internal.JMXConnectionDescriptor;
import org.openjdk.jmc.rjmx.internal.RJMXConnection;
import org.openjdk.jmc.rjmx.internal.ServerDescriptor;
import org.openjdk.jmc.ui.common.security.InMemoryCredentials;

import es.andrewazor.containertest.ConnectionListener;
import es.andrewazor.containertest.JMCConnection;
import es.andrewazor.containertest.commands.Command;

@Singleton 
class ConnectCommand implements Command {

    private final Set<ConnectionListener> connectionListeners;

    @Inject ConnectCommand(Set<ConnectionListener> connectionListeners) {
        this.connectionListeners = connectionListeners;
    }

    @Override
    public String getName() {
        return "connect";
    }

    @Override
    public boolean validate(String[] args) {
        // TODO better validation of host
        return args.length == 1;
    }

    @Override
    public void execute(String[] args) throws Exception {
        JMCConnection connection = new JMCConnection(
                new DefaultConnectionHandle(attemptConnect(args[0]), "RJMX Connection", new IConnectionListener[0]));
        connectionListeners.forEach(listener -> listener.connectionChanged(connection));
    }

    private static RJMXConnection attemptConnect(String host) throws Exception {
        return attemptConnect(host, 0);
    }

    private static RJMXConnection attemptConnect(String host, int maxRetry) throws Exception {
        JMXConnectionDescriptor cd = new JMXConnectionDescriptor(
                new JMXServiceURL(String.format("service:jmx:rmi:///jndi/rmi://%s/jmxrmi", host)),
                new InMemoryCredentials(null, null));
        ServerDescriptor sd = new ServerDescriptor(null, "Container", null);

        int attempts = 0;
        while (true) {
            try {
                RJMXConnection conn = new RJMXConnection(cd, sd, ConnectCommand::failConnection);
                if (!conn.connect()) {
                    failConnection();
                }
                return conn;
            } catch (Exception e) {
                attempts++;
                System.out.println(String.format("Connection attempt #%s failed", attempts));
                if (attempts >= maxRetry) {
                    System.out.println("Aborting...");
                    throw e;
                } else {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private static void failConnection() {
        throw new RuntimeException("Connection Failed");
    }

}