package edu.udo.cs.rvs;

import java.io.*;
import java.lang.*;
import java.net.*;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
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
    private String CONTENTLENGTH = "Content-Length: 0";
    private byte[] fileContent = null;


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
            String RESPONSE;
            String SERVER = "Server: RvS";
            int responseCode;

            if (path == null)
            {
                path = "/";
            }

            String LOCATION = "Location: http://localhost" + path;

            //HTTP Version
            if  (!isHTTP10)
            {
                RESPONSE = "HTTP/1.0 400 Bad Request";
                responseCode = 400;
            }

            //Request Type
            else if (requestType == null || contentType == otherEnd)
            {
                RESPONSE = "HTTP/1.0 501 Not Implemented";
                responseCode = 501;
            }

            //No Content
            else if (!isContent(path))
            {
                RESPONSE = "HTTP/1.0 204 No Content";
                responseCode = 400;
            }

            //Not found
            else if (getFile(path) == null)
            {
                RESPONSE = "HTTP/1.0 404 Not Found";
                responseCode = 404;
            }

            else
            {
                RESPONSE = "HTTP/1.0 200 OK";
                responseCode = 200;
            }

            sendToClient(RESPONSE, SERVER, DATE, CONTENTTYPE, CONTENTLENGTH, LOCATION, responseCode);


        }
        catch (IOException e)
        {
            e.printStackTrace();
            //throw500();
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

    private File getFile(String path)
    {
        try
        {
            File file = new File(path);
            fileContent = Files.readAllBytes(file.toPath());
            CONTENTLENGTH = "Content-Length: " + fileContent.length;

            return file;
        }
        catch (IOException e)
        {
            return null;
        }
    }

    private boolean isContent(String path)
    {
        if (contentType == noEnd)
        {
            return isIndex(path);
        }
        else
        {
            return true;
        }
    }

    private boolean isIndex(String path)
    {
        File dir = new File(path);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null)
        {
            System.out.println();
            for (File child : directoryListing) {
                if (child.getName().contains("(^index\\..+$)")) {
                    if (child.isFile()) {
                        path = path + "/" + child.getName();
                        return true;
                    }
                }

                System.out.println("Datei: " + child.getName());
            }
            System.out.println();
        }
        else
        {
            throw500();
            throw new RuntimeException("Problem with directory listing");
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

    private void sendToClient(String response, String server, String date, String contentType, String contentLength, String location, int responseCode) throws IOException
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

        //Data sending
        BufferedOutputStream dataWrite = new BufferedOutputStream(this.client.getOutputStream());
        if (requestType != HEAD)
        {
            switch (responseCode)
            {
                case 400:
                    pw.println("Ein Fehler ist aufgetreten!\r\n400 Bad Request");
                    break;
                case 403:
                    pw.println("Keine Zugriffrechte!\r\n403 Forbidden");
                    break;
                case 404:
                    pw.println("Datei nicht gefunden!\r\n404 Not Found");
                    break;
                case 501:
                    pw.println("Anfrage nicht unterstützt!\r\n501 Not Implemented");
                    break;
                case 204:
                    pw.println("Kein Index verfügbar!\r\n204 No Content");
                    break;
                case 200:
                    dataWrite.write(fileContent, 0, fileContent.length);
                    dataWrite.flush();
                    break;
                case 304:
                    dataWrite.write(fileContent, 0, fileContent.length);
                    dataWrite.flush();
                    break;
                default:
                    throw500();

            }
        }

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

            System.out.println();
            System.out.println("HTTP/1.0 500 Internal Server Error");
            System.out.println("Server: RvS");
            System.out.println("Date: " + getDate());
            System.out.println();

            pw.println("Interner Fehler ist aufgetreten!\r\n500 Internal Server Error");

            client.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

    }


}
