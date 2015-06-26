package com.zitego.remoteCommandProcessor.process;

import com.zitego.remoteCommandProcessor.CommandProcessor;
import com.zitego.remoteCommandProcessor.CommandProcessorDocument;
import com.zitego.remoteCommandProcessor.ProcessorArguments;
import com.zitego.remoteCommandProcessor.CommandProcessorResult;
import com.zitego.remoteCommandProcessor.CommandProcessorException;
import com.zitego.markup.xml.XmlTag;
import java.io.File;
import java.lang.SecurityException;
import java.util.Vector;

/**
 * This class handles creating directories. The expected processor arguments object should contain
 * one or more directories to create. The xml should look as follows:<br>
 * <xmp>
 * <processor-arguments>
 *  <directory>/home/directory1</directory>
 *  <directory>/home/directory2</directory>
 *  ...
 * </processor-arguments>
 * </xmp>
 *
 * Note: Parent directories will be created if they do not already exist. Ex: directory argument is
 *       /home/directory1/directory2. If /home/directory1 does not yet exist, it will first be created.
 *
 * @author John Glorioso
 * @version $Id: DirectoryCreator.java,v 1.2 2008/04/14 20:21:15 jglorioso Exp $
 */
public class DirectoryCreator extends CommandProcessor
{
    private Vector _directories = new Vector();

    /**
     * Creates a new directory creator.
     */
    public DirectoryCreator()
    {
        super();
    }

    public void init(ProcessorArguments args) throws CommandProcessorException
    {
        Vector directories = args.getChildrenWithName("directory");
        int size = directories.size();
        for (int i=0; i<size; i++)
        {
            XmlTag tag = (XmlTag)directories.get(i);
            addDirectory( tag.getValue() );
        }
    }

    public CommandProcessorResult execute()
    {
        CommandProcessorResult ret = null;
        try
        {
            int size = _directories.size();
            for (int i=0; i<size; i++)
            {
                File dir = new File( (String)_directories.get(i) );
                boolean success = dir.mkdirs();
                ret = new CommandProcessorResult( (success ? CommandProcessorResult.SUCCESS : CommandProcessorResult.FAILURE) );
            }
        }
        catch (SecurityException se)
        {
            ret = new CommandProcessorResult(CommandProcessorResult.FAILURE);
            ret.setReason( se.getMessage() );
            ret.setStackTrace(se);
        }
        return ret;
    }

    /**
     * Adds a new directory to be created.
     *
     * @param dir The directory to be added.
     * @throws IllegalArgumentException if the directory is null or an empty string.
     */
    public void addDirectory(String dir) throws IllegalArgumentException
    {
        if ( dir == null || "".equals(dir) ) throw new IllegalArgumentException("Directory cannot be null or an empty string");
        _directories.add(dir);
    }

    public CommandProcessorDocument createDocument()
    {
        CommandProcessorDocument doc = super.createDocument();
        ProcessorArguments args = doc.getProcessorArguments();
        int size = _directories.size();
        for (int i=0; i<size; i++)
        {
            XmlTag tag = new XmlTag("directory", args);
            tag.addBodyContent( (String)_directories.get(i) );
            args.addBodyContent(tag);
        }
        return doc;
    }
}