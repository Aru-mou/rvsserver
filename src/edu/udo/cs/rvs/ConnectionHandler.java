package edu.udo.cs.rvs;

import java.io.*;
import java.lang.*;
import java.net.*;
import java.nio.*;
import java.security.*;
import java.util.*;
import java.text.*;

import static edu.udo.cs.rvs.RequestType.*;
import static edu.udo.cs.rvs.ContentType.*;

public class ConnectionHandler implements Runnable
{
    private Socket client;
    private boolean isHTTP10;
    private String path;
    private String endOfPath;
    private ContentType contentType;
    private RequestType requestType = null;


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

            // TODO: 2019-01-10 HTTP-Version überprüfen und entsprechenden response-code schicken (200 oder 400)
            // TODO: 2019-01-10 Checken, ob Zieldatei/-verzeichnis existiert und entsprechend Fehlercode schicken oder halt nicht 
            // TODO: 2019-01-10 Checken, ob Zugriff auf Zieldatei/-verzeichnis besteht 
            // TODO: 2019-01-10 Den Response formatieren (vll in String abspeichern k.a) 
            // TODO: 2019-01-10 Response über OutputStream zurückschicken 
            // TODO: 2019-01-10 Socket.close()

            String DATE = "Date: " + getDate();
            String CONTENTTYPE = getContentType();
            String CONTENTLENGTH = "Content-Length: 0";
            String RESPONSE;
            String SERVER = "Server: RvS";

            if (path == null)
            {
                path = "/";
            }

            String LOCATION = "Location: http://localhost" + path;

            //HTTP Version
            if  (!isHTTP10)
            {
                RESPONSE = "HTTP/1.0 400 Bad Request";
            }

            //Request Type
            else if (requestType == null || contentType == otherEnd)
            {
                RESPONSE = "HTTP/1.0 501 Not Implemented";
            }

            //No Content
            else if (!isContent(path))
            {
                RESPONSE = "HTTP/1.0 204 No Content";
            }

            //Not found
            else if (!isPath(path))
            {
                RESPONSE = "HTTP/1.0 404 Not Found";
            }

            else
            {
                RESPONSE = "HTTP/1.0 200 OK";
            }

            sendToClient(RESPONSE, SERVER, DATE, CONTENTTYPE, CONTENTLENGTH, LOCATION);


        }
        catch (IOException e)
        {
            //e.printStackTrace();
            throw500();
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
                //endregion

                if (line.contains("HTTP/1."))
                {
                    isHTTP10 = true;
                    path = line.split(" ")[1];
                    String [] splits = path.split("(?<=/)");
                    endOfPath = splits[splits.length -1];
                }
                setContentType(endOfPath);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            /*
            sendToClient(RESPONSE, SERVER, DATE, CONTENTTYPE, CONTENTLENGTH, LOCATION);
            */
        }

    }

    private boolean isPath(String path)
    {
        return false;    //filler
    }

    private boolean isContent(String path)
    {
        if (contentType != noEnd)
        {
            return true;
        }
        return false;
    }

    private String getDate()
    {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        DateFormat df = new SimpleDateFormat("dd. MMMM yyyy HH:mm:ss");
        String Date = df.format(date);
        return Date;
    }

    private String getContentType()
    {
        if (contentType == txt)
        {
            return "Content-Type: text/plain; charset=utf-8";
        }
        else if (contentType == html)
        {
            return "Content-Type: text/html; charset=utf-8";
        }
        else if (contentType == htm)
        {
            return "Content-Type: text/html; charset=utf-8";
        }
        else if (contentType == css)
        {
            return "Content-Type: text/css; charset=utf-8";
        }
        else if (contentType == ico)
        {
            return "Content-Type: image/x-icon";
        }
        else if (contentType == pdf)
        {
            return "Content-Type: application/pdf";
        }
        else
        {
            return "Content-Type: application/octet-stream";
        }
    }

    private void setContentType(String string)
    {
        if (string.matches("(^.*\\.txt$)"))
        {
            contentType = txt;
        }
        else if (string.matches("(^.*\\.html$)"))
        {
            contentType = html;
        }
        else if (string.matches("(^.*\\.htm$)"))
        {
            contentType = htm;
        }
        else if (string.matches("(^.*\\.css$)"))
        {
            contentType = css;
        }
        else if (string.matches("(^.*\\.ico$)"))
        {
            contentType = ico;
        }
        else if (string.matches("(^.*\\.pdf$)"))
        {
            contentType = pdf;
        }
        else if (string.matches("(^.+\\..+$)"))
        {
            contentType = otherEnd;
        }
        else
        {
            contentType = noEnd;
        }
    }

    private void sendToClient(String response, String server, String date, String contentType, String contentLength, String location) throws IOException
    {
        System.out.println();
        System.out.println(response);
        System.out.println(server);
        System.out.println(date);
        System.out.println(contentType);
        System.out.println(contentLength);
        System.out.println(location);
        System.out.println();
        System.out.println();

        PrintWriter pw = new PrintWriter(this.client.getOutputStream(), true);
        pw.println(response);
        pw.println(server);
        pw.println(date);
        pw.println(contentType);
        pw.println(contentLength);
        pw.println(location);
        pw.println();
        //HIER DATEN SCHICKEN
        client.close();
    }

    private void throw500()
    {
        try
        {
            PrintWriter pw = new PrintWriter(this.client.getOutputStream(), true);
            pw.println("HTTP/1.0 500 Internal Server Error");
            pw.println("Server: RvS");
            pw.println(getDate());
            pw.println();

            client.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

    }


}
