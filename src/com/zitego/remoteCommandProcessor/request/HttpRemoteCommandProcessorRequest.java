package com.zitego.remoteCommandProcessor.request;

import com.zitego.remoteCommandProcessor.CommandProcessorException;
import com.zitego.remoteCommandProcessor.CommandProcessorResult;
import com.zitego.markup.xml.XmlTag;
import com.zitego.markup.tag.TagAttribute;
import com.zitego.format.UnsupportedFormatException;
import com.zitego.format.FormatType;
import com.zitego.http.HttpRequestData;
import com.zitego.http.PostData;
import com.zitego.http.UrlContentReader;
import com.zitego.util.StringValidation;

/**
 * This class handles sending the xml document via an http post request to the remote
 * command server processing servlet.
 *
 * @author John Glorioso
 * @version $Id: HttpRemoteCommandProcessorRequest.java,v 1.2 2010/10/06 03:09:47 jglorioso Exp $
 */
public class HttpRemoteCommandProcessorRequest extends RemoteCommandProcessorRequest
{
    /**
     * Creates a new request with the fully qualified url to the remote processing servlet.
     *
     * @param url The remote processing servlet url.
     */
    public HttpRemoteCommandProcessorRequest(String url)
    {
        super(url, "80");
    }

    public CommandProcessorResult execute() throws CommandProcessorException
    {
        HttpRequestData request = new HttpRequestData( getIp() );
        PostData data = new PostData();
        data.addField( "processor", getXml() );
        request.setPostData(data);
        UrlContentReader reader = new UrlContentReader(request);
        try
        {
            return createResult( reader.getContent(), getIp() );
        }
        catch (Exception e)
        {
            throw new CommandProcessorException("An error occurred processing the command: " + e.toString() );
        }
    }

    private static CommandProcessorResult createResult(String content, String ip) throws CommandProcessorException
    {
        try
        {
            XmlTag tag = new XmlTag();
            tag.parseText(new StringBuffer(content), FormatType.XML);
            if ( !tag.getTagName().equalsIgnoreCase("processor-result") ) throw new UnsupportedFormatException("No processor-result parent tag found");
            String className = tag.getTagAttribute("class");
            if (className == null) throw new UnsupportedFormatException("No processor-result class attribute found");
            CommandProcessorResult ret = (CommandProcessorResult)Class.forName(className).newInstance();
            String type = tag.getChildValue("type");
            if (type == null) throw new UnsupportedFormatException("No processor-result type tag found");
            ret.setType( Integer.parseInt(type) );
            String reason = tag.getChildValue("reason");
            if ( StringValidation.isNotEmpty(reason) ) ret.setReason(reason);
            String stackTrace = tag.getChildValue("stack-trace");
            if ( StringValidation.isNotEmpty(stackTrace) ) ret.setStackTrace(stackTrace);
            ret.deserialize(tag);
            return ret;
        }
        catch (Exception e)
        {
            throw new CommandProcessorException("Could not execute command to: " + ip, e);
        }
    }

    /**
     * Tests to see if the remote command processor accepts connections.
     *
     * @return CommandProcessorResult
     * @throws CommandProcessorException if an error occurs.
     */
    public CommandProcessorResult test() throws CommandProcessorException
    {
        HttpRequestData request = new HttpRequestData( getIp() );
        PostData data = new PostData();
        data.addField("test", "1");
        request.setPostData(data);
        UrlContentReader reader = new UrlContentReader(request);
        try
        {
            return createResult( reader.getContent(), getIp() );
        }
        catch (Exception e)
        {
            throw new CommandProcessorException("An error occurred processing the command: " + e.toString() );
        }
    }
}