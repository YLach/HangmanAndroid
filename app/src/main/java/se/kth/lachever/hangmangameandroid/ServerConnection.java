package se.kth.lachever.hangmangameandroid;


import android.app.Activity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerConnection implements Runnable, Serializable
{
    private final String host;
    private final int port;
    protected final transient Activity gui;
    private final LinkedBlockingQueue<String> msgs =
            new LinkedBlockingQueue<>();
    private BufferedInputStream in;
    private BufferedOutputStream out;

    /**
     * Creates a new instance. Does not connect to the server.
     *
     * @param gui
     * @param host The server host name.
     * @param port The server port number.
     */
    public ServerConnection(final ServerConnectionActivity gui, String host, int port)
    {
        this.host = host;
        this.port = port;
        this.gui = gui;
    }

    /**
     * The run method of the communication thread. First connects to
     * the server using the host name and port number specified in the
     * constructor. Second waits to receive a string from the se.kth.id2212.ex5.wordreverter.gui and sends
     * that to the reverse server. This is done once, then the thread dies.
     */
    @Override
    public void run()
    {
        callServer();
    }

    /**
     * Connects to the server using the host name and port number
     * specified in the constructor.
     */
    void connect()
    {
        try
        {
            Socket clientSocket = new Socket(host, port);
            in = new BufferedInputStream(clientSocket.getInputStream());
            out = new BufferedOutputStream(clientSocket.getOutputStream());
        } catch (UnknownHostException e)
        {
            System.err.println("Don't know about host: " + host + ".");
            System.exit(1);
        } catch (IOException e)
        {
            System.err.println("Couldn't get I/O for the connection to: "
                    + host + "." + e.getCause());
            System.exit(1);
        }
    }


    public void messageToSend(String mess) {
        this.msgs.add(mess);
    }


    /**
     * Waits to receive a string from the se.kth.id2212.ex5.wordreverter.gui and sends that to the
     * reverse server.
     */
    void callServer()
    {
        String result;
        try
        {
            byte[] toServer = msgs.take().getBytes();
            out.write(toServer, 0, toServer.length);
            out.flush();
            byte[] fromServer = new byte[toServer.length];
            int n = in.read(fromServer, 0, fromServer.length);
            if (n != fromServer.length)
            {
                result = "Failed to reverse, some data was lost.";
            } else
            {
                result = new String(fromServer);
            }
        } catch (InterruptedException | IOException e)
        {
            result = "Failed to reverse, " + e.getMessage();
        }
    }
}
