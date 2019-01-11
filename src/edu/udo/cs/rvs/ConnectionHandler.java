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

    //region Variables
    private Socket client;
    private boolean isHTTP10;
    private String path;
    private String endOfPath;
    private ContentType contentType;
    private RequestType requestType = null;
    private String CONTENTLENGTH = "Content-Length: 0";
    private byte[] fileContent = null;
    private String host = "";
    private boolean directoryExists;

    private final File HTTP_ROOT = new File("wwwroot");
    //endregion


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

            String DATE = "Date: " + getDate();
            String RESPONSE;
            String SERVER = "Server: RvS";
            String LOCATION = "Location: http://";
            int responseCode;

            if (path == null)
            {
                path = "/";
            }

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
            else if (!isContent(path) && directoryExists)
            {
                RESPONSE = "HTTP/1.0 204 No Content";
                responseCode = 204;
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
            LOCATION += (host + path);
            sendToClient(RESPONSE, SERVER, DATE, getContentType(), CONTENTLENGTH, LOCATION, responseCode);


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
                    this.path = line.split(" ")[1];
                    String [] splits = path.split("(?<=/)");
                    endOfPath = splits[splits.length -1];
                }
                setContentType(endOfPath);

                //Save Host
                if (line.contains("Host: "))
                {
                    this.host = line.substring(6);
                }
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
            File file = new File(HTTP_ROOT, path);
            fileContent = Files.readAllBytes(file.toPath());
            CONTENTLENGTH = "Content-Length: " + fileContent.length;

            setContentType(file.getName());

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
        File dir = new File(HTTP_ROOT, path);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null)
        {
            System.out.println();
            for (File child : directoryListing) {
                if (child.getName().startsWith("index.")) {
                    if (child.isFile()) {
                        this.path += child.getName();
                        directoryExists = true;
                        return true;
                    }
                }
            }
            System.out.println();
        }
        else
        {
            directoryExists = false;
            return false;
        }
        directoryExists = true;
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
        PrintWriter pw = new PrintWriter(this.client.getOutputStream(), true);

        //Data sending
        BufferedOutputStream dataWrite = new BufferedOutputStream(this.client.getOutputStream());
        if (requestType != HEAD)
        {
            switch (responseCode)
            {
                case 400:
                    pw.println(response);
                    pw.println(server);
                    pw.println(date);
                    pw.println("Content-Type: text/plain; charset=utf-8");
                    pw.println();
                    pw.println("Ein Fehler ist aufgetreten!\r\n400 Bad Request");

                    System.out.println();
                    System.out.println(response);
                    System.out.println(server);
                    System.out.println(date);
                    System.out.println("Content-Type: text/plain; charset=utf-8");
                    System.out.println();
                    System.out.println();
                    break;
                case 403:
                    pw.println(response);
                    pw.println(server);
                    pw.println(date);
                    pw.println("Content-Type: text/plain; charset=utf-8");
                    pw.println();
                    pw.println("Keine Zugriffrechte!\r\n403 Forbidden");

                    System.out.println();
                    System.out.println(response);
                    System.out.println(server);
                    System.out.println(date);
                    System.out.println("Content-Type: text/plain; charset=utf-8");
                    System.out.println();
                    System.out.println();
                    break;
                case 404:
                    pw.println(response);
                    pw.println(server);
                    pw.println(date);
                    pw.println("Content-Type: text/plain; charset=utf-8");
                    pw.println();
                    pw.println("Datei nicht gefunden!\r\n404 Not Found");

                    System.out.println();
                    System.out.println(response);
                    System.out.println(server);
                    System.out.println(date);
                    System.out.println("Content-Type: text/plain; charset=utf-8");
                    System.out.println();
                    System.out.println();
                    break;
                case 501:
                    pw.println(response);
                    pw.println(server);
                    pw.println(date);
                    pw.println("Content-Type: text/plain; charset=utf-8");
                    pw.println();
                    pw.println("Anfrage nicht unterstützt!\r\n501 Not Implemented");

                    System.out.println();
                    System.out.println(response);
                    System.out.println(server);
                    System.out.println(date);
                    System.out.println("Content-Type: text/plain; charset=utf-8");
                    System.out.println();
                    System.out.println();
                    break;
                case 204:
                    pw.println(response);
                    pw.println(server);
                    pw.println(date);
                    pw.println("Content-Type: text/plain; charset=utf-8");
                    pw.println();
                    pw.println("Kein Index verfügbar!\r\n204 No Content");

                    System.out.println();
                    System.out.println(response);
                    System.out.println(server);
                    System.out.println(date);
                    System.out.println("Content-Type: text/plain; charset=utf-8");
                    System.out.println();
                    System.out.println();
                    break;
                case 200:
                    pw.println(response);
                    pw.println(server);
                    pw.println(date);
                    pw.println(contentType);
                    pw.println(contentLength);
                    pw.println(location);
                    pw.println();

                    System.out.println();
                    System.out.println(response);
                    System.out.println(server);
                    System.out.println(date);
                    System.out.println(contentType);
                    System.out.println(contentLength);
                    System.out.println(location);
                    System.out.println();
                    System.out.println();

                    dataWrite.write(fileContent, 0, fileContent.length);
                    dataWrite.flush();
                    break;
                case 304:
                    pw.println(response);
                    pw.println(server);
                    pw.println(date);
                    pw.println(contentType);
                    pw.println(contentLength);
                    pw.println(location);
                    pw.println();

                    System.out.println();
                    System.out.println(response);
                    System.out.println(server);
                    System.out.println(date);
                    System.out.println(contentType);
                    System.out.println(contentLength);
                    System.out.println(location);
                    System.out.println();
                    System.out.println();

                    dataWrite.write(fileContent, 0, fileContent.length);
                    dataWrite.flush();
                    break;
                default:
                    throw500();
                    break;
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
