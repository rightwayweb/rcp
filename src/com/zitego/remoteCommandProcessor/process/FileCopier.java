package com.zitego.remoteCommandProcessor.process;

import com.zitego.remoteCommandProcessor.CommandProcessor;
import com.zitego.remoteCommandProcessor.ProcessorArguments;
import com.zitego.remoteCommandProcessor.CommandProcessorResult;
import com.zitego.remoteCommandProcessor.CommandProcessorException;
import com.zitego.filemanager.FileSystemObjectFactory;
import com.zitego.filemanager.FileSystemObject;
import com.zitego.filemanager.File;
import com.zitego.filemanager.Directory;
import com.zitego.markup.xml.XmlTag;
import java.io.IOException;
import java.util.Vector;

/**
 * This class handles copying files from one place to another. The expected processor arguments object should contain
 * one or more copy entities. Each entity should contain a from and to entity. The xml should look as follows:<br>
 * <xmp>
 * <processor-arguments>
 *  <copy>
 *   <from>/home/directory1/file_or_directory</from>
 *   <to>/home/directory2</to>
 *  </copy>
 *  ...
 * </processor-arguments>
 * </xmp>
 *
 * Note: FileCopier supports copying directories.
 *
 * @author John Glorioso
 * @version $Id: FileCopier.java,v 1.2 2008/04/14 20:21:15 jglorioso Exp $
 */
public class FileCopier extends CommandProcessor
{
    private Vector _froms = new Vector();
    private Vector _tos = new Vector();

    /**
     * Creates a new directory creator.
     */
    public FileCopier()
    {
        super();
    }

    /**
     * The processor arguments should contain one or more copy arguments. The from argument must be a file
     * and the to argument must be a directory.
     *
     * @param args The processor arguments.
     */
    public void init(ProcessorArguments args) throws CommandProcessorException
    {
        Vector copies = args.getChildrenWithName("copy");
        int size = copies.size();
        for (int i=0; i<size; i++)
        {
            String from = null;
            String to = null;
            try
            {
                XmlTag tag = (XmlTag)copies.get(i);
                from = tag.getChildValue("from");
                if (from == null) throw new CommandProcessorException("copy element <from> is required");
                FileSystemObject obj = FileSystemObjectFactory.createObject(from);
                _froms.add(obj);
                to = tag.getChildValue("to");
                if (to == null) throw new CommandProcessorException("copy element <to> is required");
                obj = FileSystemObjectFactory.createObject(to);
                if (obj instanceof File) throw new CommandProcessorException("copy element <to> must be a directory");
                _tos.add(obj);
            }
            catch (Exception e)
            {
                throw new CommandProcessorException("Could not create copy element: from="+from+", to="+to, e);
            }
        }
    }

    public CommandProcessorResult execute()
    {
        CommandProcessorResult ret = null;
        try
        {
            int size = _froms.size();
            for (int i=0; i<size; i++)
            {
                FileSystemObject from = (FileSystemObject)_froms.get(i);
                Directory to = (Directory)_tos.get(i);
                from.copyTo(to);
            }
            ret = new CommandProcessorResult(CommandProcessorResult.SUCCESS);
        }
        catch (IOException ioe)
        {
            ret = new CommandProcessorResult(CommandProcessorResult.FAILURE);
            ret.setReason( ioe.getMessage() );
            ret.setStackTrace(ioe);
        }
        return ret;
    }
}