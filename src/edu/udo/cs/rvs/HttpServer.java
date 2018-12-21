package edu.udo.cs.rvs;

import java.io.*;
import java.lang.*;
import java.lang.annotation.*;
import java.lang.invoke.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.*;
import java.security.acl.*;
import java.security.cert.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;
import java.util.jar.*;
import java.util.regex.*;
import java.util.spi.*;
import java.util.stream.*;
import java.util.zip.*;

/**
 * Nutzen Sie diese Klasse um den HTTP Server zu implementieren. Sie duerfen
 * weitere Klassen erstellen, sowie Klassen aus den in der Aufgabe aufgelisteten
 * Paketen benutzen. Achten Sie darauf, Ihren Code zu dokumentieren und moegliche
 * Ausnahmen (Exceptions) sinnvoll zu behandeln.
 *
 * @author Kamil Czaja, 201147
 * @author Christian Goltz, xxxxxx
 */
public class HttpServer
{
    /**
     * Beispiel Dokumentation fuer dieses Attribut:
     * Dieses Attribut gibt den Basis-Ordner fuer den HTTP-Server an.
     */
    private static final File wwwroot = new File("wwwroot");

    
    /**
     * Der Port, auf dem der HTTP-Server lauschen soll.
     */
    private int port;
    private final String ip = "127.0.0.1";
    private final InetSocketAddress address = new InetSocketAddress(this.ip, this.port);

    private ServerSocket serverSocket;

    /**
     * Beispiel Dokumentation fuer diesen Konstruktor:
     * Der Server wird initialisiert und der gewuenschte Port
     * gespeichert.
     * 
     * @param port
     *            der Port auf dem der HTTP-Server lauschen soll
     */
    public HttpServer(int port)
    {
        this.port = port;
    }
    
    /**

     */
    public void startServer() throws IOException
    {
        ConnectionHandler client;
        Thread thread;

        this.serverSocket = new ServerSocket(this.port, -1);
        this.serverSocket.bind(this.address);

        while (true)
        {
            Socket incoming = this.serverSocket.accept();
            client = new ConnectionHandler(incoming);
            thread = new Thread(client);
            thread.start();
        }
    }

    protected void finalize() throws IOException
    {
        this.serverSocket.close();
    }
}
