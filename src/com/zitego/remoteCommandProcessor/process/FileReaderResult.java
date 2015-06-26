package com.zitego.remoteCommandProcessor.process;

import com.zitego.remoteCommandProcessor.CommandProcessorResult;
import com.zitego.markup.xml.XmlTag;
import com.zitego.markup.xml.CData;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * An extension of the command processor result to contain file contents.
 *
 * @author John Glorioso
 * @version $Id: FileReaderResult.java,v 1.1 2010/11/09 02:35:47 jglorioso Exp $
 */
public class FileReaderResult extends CommandProcessorResult
{
    private String _content;

    /**
     * Creates a new FileReader result.
     */
    public FileReaderResult()
    {
        super(SUCCESS);
    }

    /**
     * Creates a new FileLister result given the result type.
     */
    public FileReaderResult(int type)
    {
        this();
        setType(type);
    }
    /**
     * Sets the file content.
     *
     * @param content The file content.
     */
    public void setContent(String content)
    {
        _content = content;
    }

    public String getContent()
    {
        return _content;
    }

    protected XmlTag getDetails()
    {
        XmlTag ret = new XmlTag("content");
        CData content = new CData( ret, _content.replaceAll("\\]\\]>", "]]&gt;") );
        return ret;
    }

    public void deserialize(XmlTag tag)
    {
        if (tag == null) return;
        _content = tag.getChildValue("content");
    }
}