package com.zitego.remoteCommandProcessor;

import com.zitego.format.FormatType;
import com.zitego.format.UnsupportedFormatException;
import com.zitego.markup.xml.XmlTag;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.StringTokenizer;

/**
 * This class defines the various CommandProcessorResults.
 *
 * @author John Glorioso
 * @version $Id: CommandProcessorResult.java,v 1.3 2010/10/06 03:09:45 jglorioso Exp $
 */
public class CommandProcessorResult
{
    private int _type = SUCCESS;
    private String _reason;
    private String _stackTrace;
    public static final int SUCCESS = 1;
    public static final int FAILURE = 0;

    public CommandProcessorResult() { }

    /**
     * Creates a new CommandProcessorResult given the result type.
     *
     * @param type The result type.
     */
    public CommandProcessorResult(int type)
    {
        setType(type);
    }

    /**
     * Sets the result type.
     *
     * @param type The result type.
     */
    public void setType(int type)
    {
        _type = type;
    }

    /**
     * Returns the result type.
     *
     * @return int
     */
    public int getType()
    {
        return _type;
    }

    /**
     * Sets the reason for the result.
     *
     * @param reason The reason.
     */
    public void setReason(String reason)
    {
        _reason = reason;
    }

    /**
     * Returns the reason for the result.
     *
     * @return String
     */
    public String getReason()
    {
        return _reason;
    }

    /**
     * Sets the stack trace for the result given a throwable object.
     *
     * @param t The throwable object with the trace.
     */
    public void setStackTrace(Throwable t)
    {
        StringWriter trace = new StringWriter();
        t.printStackTrace( new PrintWriter(trace, true) );
        setStackTrace( trace.toString() );
    }

    /**
     * Sets the stack trace for the result.
     *
     * @param trace The stack trace.
     */
    public void setStackTrace(String trace)
    {
        _stackTrace = trace;
    }

    /**
     * Returns the stack trace for the result.
     *
     * @return String
     */
    public String getStackTrace()
    {
        return _stackTrace;
    }

    public String toString()
    {
        return _type + (_reason != null ? ":" + _reason : "") + (_stackTrace != null ? "\r\n" + _stackTrace: "");
    }

    public String serialize() throws UnsupportedFormatException
    {
        XmlTag ret = new XmlTag("processor-result");
        ret.setAttribute( "class", getClass().getName() );
        XmlTag child = new XmlTag("type", ret);
        child.addBodyContent( String.valueOf(_type) );
        child = new XmlTag("reason", ret);
        if (_reason != null) child.addBodyContent(_reason);
        child = new XmlTag("stack-trace", ret);
        if (_stackTrace != null) child.addBodyContent(_stackTrace);
        XmlTag details = getDetails();
        if (details != null)
        {
            details.setParent(ret);
            ret.addBodyContent(details);
        }

        return ret.format(FormatType.XML);
    }

    public void deserialize(XmlTag tag) { }

    protected XmlTag getDetails()
    {
        return null;
    }
}