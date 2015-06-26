package com.zitego.remoteCommandProcessor.request;

import com.zitego.remoteCommandProcessor.CommandProcessorException;
import com.zitego.remoteCommandProcessor.CommandProcessorResult;

/**
 * This class handles sending a remote command processor request to the remote server.
 * It has only two methods. Set xml and an abstract execute method which handles the
 * communication to the remote command server.
 *
 * @author John Glorioso
 * @version $Id: RemoteCommandProcessorRequest.java,v 1.1.1.1 2008/02/20 14:58:34 jglorioso Exp $
 */
public abstract class RemoteCommandProcessorRequest
{
    private String _xml;
    private String _ip;
    private String _port;

    /**
     * Creates a new RemoteCommandProcessorRequest with the ip and port
     * to connect to.
     *
     * @param ip The ip to connect to.
     * @param port The port to connect to.
     */
    protected RemoteCommandProcessorRequest(String ip, String port)
    {
        setIp(ip);
        setPort(port);
    }

    /**
     * Sends the request to the server, reads back the response, and returns
     * a CommandProcessorResult.
     *
     * @return CommandProcessorResult
     * @throws CommandProcessorException if an error occurs.
     */
    public abstract CommandProcessorResult execute() throws CommandProcessorException;

    /**
     * Sets the ip.
     *
     * @param ip The ip.
     */
    public void setIp(String ip)
    {
        _ip = ip;
    }

    /**
     * Returns the ip.
     *
     * @return String
     */
    public String getIp()
    {
        return _ip;
    }

    /**
     * Sets the port.
     *
     * @param port The port.
     */
    public void setPort(String port)
    {
        _port = port;
    }

    /**
     * Returns the port.
     *
     * @return String
     */
    public String getPort()
    {
        return _port;
    }

    /**
     * Sets the xml request.
     *
     * @param xml The xml data.
     */
    public void setXml(String xml)
    {
        _xml = xml;
    }

    /**
     * Returns the xml.
     *
     * @return String
     */
    public String getXml()
    {
        return _xml;
    }
}