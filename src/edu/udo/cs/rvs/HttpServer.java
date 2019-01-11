package edu.udo.cs.rvs;

import java.io.*;
import java.lang.*;
import java.net.*;
import java.nio.*;
import java.security.*;
import java.util.*;
import java.text.*;

/**
 * Nutzen Sie diese Klasse um den HTTP Server zu implementieren. Sie duerfen
 * weitere Klassen erstellen, sowie Klassen aus den in der Aufgabe aufgelisteten
 * Paketen benutzen. Achten Sie darauf, Ihren Code zu dokumentieren und moegliche
 * Ausnahmen (Exceptions) sinnvoll zu behandeln.
 *
 * @author Kamil Czaja, 201147
 * @author Christian Goltz, 201244
 */
public class HttpServer
{
    
    /**
     * Der Port, auf dem der HTTP-Server lauschen soll.
     */
    private int port;
    private final String IP = "localhost";
    private final InetSocketAddress ADDRESS;

    private ServerSocket serverSocket;

    /**
     * Constructor.
     * Sets the server's port to the value provided by the user. Creates then an InetSocketAddress object
     * with the provided port and the IP provided in this class' attributes
     * 
     * @param port
     *            port the server will listen on
     */
    HttpServer(int port)
    {
        this.port = port;
        this.ADDRESS = new InetSocketAddress(this.IP, this.port);
    }

    void startServer()
    {
        try
        {
            ConnectionHandler client;
            Thread thread;

            this.serverSocket = new ServerSocket(this.port);

            // loops the listening method in order to stay alive
            while (true)
            {
                Socket incoming = this.serverSocket.accept();
                client = new ConnectionHandler(incoming);
                thread = new Thread(client);
                thread.start();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    protected void finalize() throws IOException
    {
        this.serverSocket.close();
    }
}
