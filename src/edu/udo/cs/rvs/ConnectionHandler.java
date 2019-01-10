package edu.udo.cs.rvs;

import java.io.*;
import java.lang.*;
import java.net.*;
import java.nio.*;
import java.security.*;
import java.util.*;
import java.text.*;

public class ConnectionHandler implements Runnable
{
    private Socket client;

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
            InputStream inputStream = client.getInputStream();
            OutputStream outputStream = client.getOutputStream();
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
    private Hashtable<String, String> readRequest(InputStream request)
    {
        try
        {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(request));
            String line = null;
            String[] requestLine;
            Hashtable<String, String> hashtable = new Hashtable<>();

            while ((line = bufferedReader.readLine()) != null)
            {
                requestLine = line.split("(?<=:)");
                if (requestLine != null)
                {
                    hashtable.put(requestLine[0], requestLine[1]);
                }
            }

            return hashtable;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return null;
    }


}
