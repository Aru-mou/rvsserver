package edu.udo.cs.rvs;

import javax.management.RuntimeOperationsException;
import java.io.*;
import java.lang.*;
import java.net.*;
import java.nio.*;
import java.security.*;
import java.util.*;
import java.text.*;

import static edu.udo.cs.rvs.RequestType.*;

public class ConnectionHandler implements Runnable
{
    private Socket client;
    private RequestType requestType;
    private boolean isHTTP10;
    private String path;

    /**
     * Constructor.
     * Sets this class attribute to the connected client
     *
     * @param client
     *              the client socket provided by the server socked through s_socket.accept()
     */
    ConnectionHandler(Socket client)
    {
        this.client = client;
    }

    /**
     * This thread's main method.
     * Handles the incoming client connection, listens to input, responds to input and finally closes
     * the connection socket.
     *
     * @see java.lang.Runnable
     */
    public void run()
    {
        try
        {
            readRequest(client.getInputStream());
            System.out.println();
            System.out.println("HTTP 1.0 200 OK");
            System.out.println("Server: RvS");
            System.out.println("Date: Thu, 10 Jan 2019 20:03:10 GMT");
            System.out.println("Content-Type: text/html; charset=utf8");
            System.out.println("Content-Length: 0");
            System.out.println("Location: http://localhost" + path);

            // TODO: 2019-01-10 HTTP-Version überprüfen und entsprechenden response-code schicken (200 oder 400)
            // TODO: 2019-01-10 Checken, ob Zieldatei/-verzeichnis existiert und entsprechend Fehlercode schicken oder halt nicht 
            // TODO: 2019-01-10 Checken, ob Zugriff auf Zieldatei/-verzeichnis besteht 
            // TODO: 2019-01-10 Den Response formatieren (vll in String abspeichern k.a) 
            // TODO: 2019-01-10 Response über OutputStream zurückschicken 
            // TODO: 2019-01-10 Socket.close() 

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    /**
     * Method to read the incoming HTTP request.
     * Reads the inputStream line by line, splitting each line by the ':' separator
     * and puts the values into a hashtable for better reading later on.
     *
     * @param request
     *               the InputStream object received by the socket
     *
     * @return Hashtable
     *               the hashtable contains the request header split into a better readable format.
     *         null
     *               when the reading fails
     */
    private void readRequest(InputStream request)
    {
        try
        {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(request));
            String line;

            while (!(line = bufferedReader.readLine()).isEmpty())
            {
                System.out.println(line);

                //region GET/POST/HEAD
                if (line.contains("GET"))
                {
                    requestType = GET;
                }
                else if (line.contains("POST"))
                {
                    requestType = POST;
                }
                else if (line.contains("HEAD"))
                {
                    requestType = HEAD;
                }
                else
                {
                    requestType = null;
                }
                //endregion

                if (line.contains("HTTP/1.0"))
                {
                    isHTTP10 = true;
                    path = line.split(" ")[1];
                }

            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }


}
