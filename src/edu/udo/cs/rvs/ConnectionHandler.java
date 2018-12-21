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

public class ConnectionHandler implements Runnable
{
    private Socket client;

    ConnectionHandler(Socket client)
    {
        this.client = client;
    }

    public void run()
    {

    }


}
