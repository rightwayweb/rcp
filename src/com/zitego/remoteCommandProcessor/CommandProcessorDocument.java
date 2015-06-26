package com.zitego.remoteCommandProcessor;

import com.zitego.markup.xml.XmlTag;
import com.zitego.format.UnsupportedFormatException;
import com.zitego.format.FormatType;
import org.w3c.dom.Element;

/**
 * This class is the full description of a command processor document. It has a username, password,
 * class type, and the processor arguments.
 * <xmp>
 * <command-processor>
 *  <username>some_username</username>
 *  <password>some_password</password>
 *  <type>com.zitego.remoteCommandProcessor.FileCopier</type>
 *  <processor-arguments>
 *   <copy>
 *    <from>/home/directory1/file_or_directory</from>
 *    <to>/home/directory2</to>
 *   </copy>
 *   ...
 *  </processor-arguments>
 * </command-processor>
 * </xmp>
 *
 * @author John Glorioso
 * $version $Id: CommandProcessorDocument.java,v 1.1.1.1 2008/02/20 14:58:34 jglorioso Exp $
 */
public class CommandProcessorDocument extends XmlTag
{
    private ProcessorArguments _args;

    /**
     * Creates a new processor arguments object.
     */
    public CommandProcessorDocument()
    {
        super("command-processor");
        addBodyContent( new XmlTag("username", this) );
        addBodyContent( new XmlTag("password", this) );
        addBodyContent( new XmlTag("type", this) );
        addBodyContent( new ProcessorArguments(this) );
    }

    /**
     * Sets the username.
     *
     * @param username The username.
     */
    public void setUsername(String username)
    {
        if (username == null) username = "";
        XmlTag tag = getFirstOccurrenceOf("username");
        if (tag == null)
        {
            tag = new XmlTag("username");
            addBodyContentAt(0, tag);
        }
        tag.setValue(username);
    }

    /**
     * Returns the username.
     *
     * @return String
     */
    public String getUsername()
    {
        XmlTag tag = getFirstOccurrenceOf("username");
        if (tag != null) return tag.getValue();
        else return null;
    }

    /**
     * Sets the password.
     *
     * @param password The password.
     */
    public void setPassword(String password)
    {
        if (password == null) password = "";
        XmlTag tag = getFirstOccurrenceOf("password");
        if (tag == null)
        {
            tag = new XmlTag("password");
            addBodyContentAt(1, tag);
        }
        tag.setValue(password);
    }

    /**
     * Returns the password.
     *
     * @return String
     */
    public String getPassword()
    {
        XmlTag tag = getFirstOccurrenceOf("password");
        if (tag != null) return tag.getValue();
        else return null;
    }

    /**
     * Sets the command processor classpath.
     *
     * @param cp The command processor class path.
     */
    public void setCommandProcessorClass(String cp)
    {
        XmlTag tag = getFirstOccurrenceOf("type");
        if (tag == null)
        {
            tag = new XmlTag("type");
            addBodyContentAt(2, tag);
        }
        tag.setValue(cp);
    }

    /**
     * Returns the command processor classpath.
     *
     * @return String
     */
    public String getCommandProcessorClass()
    {
        XmlTag tag = getFirstOccurrenceOf("type");
        if (tag != null) return tag.getValue();
        else return null;
    }

    /**
     * Sets the ProcessorArguments.
     *
     * @param args The arguments.
     */
    public void setProcessorArguments(ProcessorArguments args)
    {
        _args = args;
        ProcessorArguments tag = (ProcessorArguments)getFirstOccurrenceOf("processor-arguments");
        if (tag != null) removeBodyContent(tag);
        if (_args != null) addBodyContent(_args);
    }

    /**
     * Returns the ProcessorArguments.
     *
     * @return ProcessorArguments
     */
    public ProcessorArguments getProcessorArguments()
    {
        return (ProcessorArguments)getFirstOccurrenceOf("processor-arguments");
    }

    public void addChild(Element child)
    {
        try
        {
            if ( child.getTagName().equals("username") ) getFirstOccurrenceOf("username").parse(child, FormatType.XML);
            else if ( child.getTagName().equals("password") ) getFirstOccurrenceOf("password").parse(child, FormatType.XML);
            else if ( child.getTagName().equals("type") ) getFirstOccurrenceOf("type").parse(child, FormatType.XML);
            else if ( child.getTagName().equals("processor-arguments") ) getFirstOccurrenceOf("processor-arguments").parse(child, FormatType.XML);
        }
        //XmlTag supports this...
        catch (UnsupportedFormatException ufe)
        {
            throw new RuntimeException("Could not parse: "+child, ufe);
        }
    }
}