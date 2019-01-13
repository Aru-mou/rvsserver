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

    //(caps-lock variables (I know you don't write variables this way but i wanted them obviously different from the others) are our end variables for the actual sendToClient method)

    //region Variables
    private Socket client;                                                      //given socket for our connection
    private boolean isHTTP10;                                                   //true if its a HTTP1.? request -> otherwise false -> 400 Bad Request (not supported HTTP version)
    private String path;                                                        //our given request path (may be updated if we get a folder path and we can find a index.???)
    private String endOfPath;                                                   //subString which only contains the end of the path used to determine the content type
    private ContentType contentType;                                            //content type of the requested file (may be a unsupported type -> ContentType = otherEnd or no type (folder path) ContentType = noEnd
    private RequestType requestType = null;                                     //GET HEAD or POST (default null)
    private String CONTENTLENGTH = "Content-Length: 0";                         //contents length (default 0)
    private byte[] fileContent = null;                                          //file in bytes used to get the length of the file
    private String host = "";                                                   //host name
    private boolean directoryExists;                                            //true if the requested directory really exists -> otherwise false
    private String headerDate;                                                  //date provided by the request header

    DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");            //for date-formatting

    private final File HTTP_ROOT = new File("wwwroot");               //The client has only access to wwwroot and it folders so we start in that folder
    //endregion


    /**
     * Constructor.
     * Sets this class attribute to the connected client.
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
     * the connection socket (actual closing happens directly after sending the response in separately methods).
     *
     * @see java.lang.Runnable
     */
    public void run()
    {
        try
        {
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

            //saves some general information given from the start
            //(caps-lock variables (I know you don't write variables this way but i wanted them obviously different from the others) are our end variables for the actual sendToClient method)
            readRequest(client.getInputStream());
            String DATE;                                                        //the date
            String RESPONSE;                                                    //response-code as String in the right format ready to send
            String SERVER = "Server: RvS";                                      //server name
            String LOCATION = "Location: http://";                              //location which will be updated later with the path
            int responseCode;                                                   //response-code as int for a switch-case in the senToClient method

            if (path == null)
            {
                path = "/";
            }

            //filters all response-codes that could happen other than 200 OK with their own method to determine

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

            //Not Found
            else if (getFile(path) == null)
            {
                RESPONSE = "HTTP/1.0 404 Not Found";
                responseCode = 404;
            }

            //Not Modified
            else if (isFileModifiedSince(headerDate, getFile(path)))
            {
                RESPONSE = "HTTP/1.0 304 Not Modified";
                responseCode = 304;
            }

            else
            {
                RESPONSE = "HTTP/1.0 200 OK";
                responseCode = 200;
            }

            //Date formatting
            DATE = "Date: " + dateFormat.format(getDate());

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
     * Reads the inputStream line by line, searching for certain keywords
     * and saves general information to handle later operations.
     *
     * @param request
     *               the InputStream object received by the socket
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
                    //save correct path and splits end of path in a separate variable
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

                //Check for "If-Modified-Since" Header
                if (line.contains("If-Modified-Since: "))
                {
                    this.headerDate = line.substring(19);
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw500();
        }

    }

    /**
     * This method uses the given path to check if (as written in our task) response-code 204 No Content
     * is needed.
     *
     * @param path
     *              our saved path as String which may refers to a folder instead of a file object
     * @return
     *              returns false if we path referred to a folder and no index.??? was found -> ends in 204 No Content in the run() method
     *              else true -> avoids 204 No Content response
     */

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

    /**
     * Help method for "isContent". Search in the given path for a index.??? file.
     * If one is found it also updates the path to out found index.???.
     *
     * @param path
     *              given path which leads to a folder instead of a file object
     * @return
     *              returns false if no index.??? is found, so "isContent" can response with 204 No Content
     *              else true
     */

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

    /**
     * After "isContent" made sure our path refers to a file object (or other response-codes than 200 OK where send)
     * this method gets the actual file to send to the client.
     *
     * @param path
     *              our saved path as String which refers to a file object for sure
     * @return
     *              returns the file our String refers to
     *              else null if there is no file -> will end in 404 Not Found in the run() method
     */

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

    /**
     * Method to get the date which will be send to the client, in a vivid way.
     *
     * @return
     *              returns the current date
     */

    private Date getDate()
    {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();

        //Time conversion to GMT (HTTP dates are always expressed in GMT instead of local time)
        TimeZone timezone = TimeZone.getDefault();
        Date gmtDate = new Date( date.getTime() - timezone.getRawOffset());


        //Check for daylight saving time
        if (timezone.inDaylightTime(gmtDate))
        {
            Date dst = new Date(gmtDate.getTime() - timezone.getDSTSavings());

            //Check if we didn't mess up conversion
            if (timezone.inDaylightTime(dst))
            {
                gmtDate = dst;
            }
        }

        //We're returning date instead of gmtDate because we convert the date now through DateFormat.
        //Therefore, most of this method is kinda useless now
        return date;
    }

    /**
     * A method to determine, if the date provided in the "If-Modified-Since"-Header is further behind
     * than the local date
     *
     * @param headerDate
     *                  String parsed from the request header
     * @param file
     *                  The file to check
     * @return
     *        true
     *                  if the file was modified since the given date
     *        false
     *                  else
     */

    private boolean isFileModifiedSince(String headerDate, File file)
    {
        try
        {
            //In case we have no "If-Modified-Since"-Header and thus the string is null, we return false to proceed to HTTP 200 check
            if (headerDate == null)
            {
                return false;
            }

            Date hDate = dateFormat.parse(headerDate);
            Date fileDate = new Date(file.lastModified());

            //System.out.println("hDate: " + dateFormat.format(hDate));
            //System.out.println("fileDate: " + dateFormat.format(fileDate));

            if (fileDate.after(hDate))
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        catch (ParseException e)
        {
            e.printStackTrace();
            throw500();
        }

        //Should never occur
        throw new RuntimeException("Problem with try/catch - Should never occur");
    }

    /**
     * A method to get the content type in a right format, since its saved more simple.
     *
     * @return
     *              returns the content type information in a String ready to send to the client
     */

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

    /**
     * Method to set the content type of the file our path refers to.
     * "otherEnd" if we don't support the given content type or
     * "noEnd" if our path refers to a folder
     *
     * @param string
     *              The subString endOfPath since we only check the end ot out given path
     *              (only the file name or the last folders name)
     */

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

    /**
     * The method that actually sends the response according to the response-code.
     *
     * @param response
     *                  the determined response-code
     * @param server
     *                  our server name
     * @param date
     *                  the current date
     *
     * The following parameters are only send when needed according to the response-code
     *
     * @param contentType
     *                  which content type our file to send has
     * @param contentLength
     *                  the length of our file to send
     * @param location
     *                  the path to the file to send
     * @param responseCode
     *                  response-code as int for the switch cases
     * @throws IOException
     *                  if anything fails due to our server -> 500 Internal Server Error
     */

    private void sendToClient(String response, String server, String date, String contentType, String contentLength, String location, int responseCode) throws IOException
    {
        PrintWriter pw = new PrintWriter(this.client.getOutputStream(), true);

        //Data sending
        BufferedOutputStream dataWrite = new BufferedOutputStream(this.client.getOutputStream());
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

                //we only need to send the file if the request wasn't "HEAD"
                if (requestType != HEAD)
                {
                    dataWrite.write(fileContent, 0, fileContent.length);
                    dataWrite.flush();
                }
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
                break;
            default:
                throw500();
                break;
        }

        client.close();
    }

    /**
     * Method to send a 500 Internal Server Error if any Exception occurs throughout the connection handling
     */

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
