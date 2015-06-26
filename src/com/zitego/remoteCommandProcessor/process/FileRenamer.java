package com.zitego.remoteCommandProcessor.process;

import com.zitego.remoteCommandProcessor.CommandProcessor;
import com.zitego.remoteCommandProcessor.ProcessorArguments;
import com.zitego.remoteCommandProcessor.CommandProcessorResult;
import com.zitego.remoteCommandProcessor.CommandProcessorException;
import com.zitego.markup.xml.XmlTag;
import java.io.File;
import java.util.Vector;

/**
 * This class handles renaming/moving files from one place to another. The expected processor arguments object should contain
 * one or more rename entities. Each entity should contain a from and to entity. The xml should look as follows:<br>
 * <xmp>
 * <processor-arguments>
 *  <rename>
 *   <from>full path to old file or directory</from>
 *   <to>full path to new file or directory</to>
 *  </rename>
 *  ...
 * </processor-arguments>
 * </xmp>
 *
 * Note: FileRenamer supports renaming directories.
 *
 * @author John Glorioso
 * @version $Id: FileRenamer.java,v 1.1.1.1 2008/02/20 14:58:34 jglorioso Exp $
 */
public class FileRenamer extends CommandProcessor
{
    private Vector _froms = new Vector();
    private Vector _tos = new Vector();

    /**
     * Creates a new directory creator.
     */
    public FileRenamer()
    {
        super();
    }

    /**
     * The processor arguments should contain one or more rename arguments. The from and to arguments must both be files
     * or directories.
     *
     * @param args The processor arguments.
     */
    public void init(ProcessorArguments args) throws CommandProcessorException
    {
        Vector renames = args.getChildrenWithName("rename");
        int size = renames.size();
        for (int i=0; i<size; i++)
        {
            String from = null;
            String to = null;
            try
            {
                XmlTag tag = (XmlTag)renames.get(i);
                from = tag.getChildValue("from");
                if (from == null) throw new CommandProcessorException("rename element <from> is required");
                File obj = new File(from);
                _froms.add(obj);
                to = tag.getChildValue("to");
                if (to == null) throw new CommandProcessorException("rename element <to> is required");
                File obj2 = new File(to);
                if ( obj2.exists() )
                {
                    if ( obj.isFile() && !obj2.isFile() || obj.isDirectory() && !obj2.isDirectory() )
                    {
                        throw new CommandProcessorException("rename element <to> must be the same type as <from>");
                    }
                }
                _tos.add(obj2);
            }
            catch (Exception e)
            {
                throw new CommandProcessorException("Could not create rename element: from="+from+", to="+to, e);
            }
        }
    }

    public CommandProcessorResult execute()
    {
        CommandProcessorResult ret = null;
        int size = _froms.size();
        for (int i=0; i<size; i++)
        {
            File from = (File)_froms.get(i);
            File to = (File)_tos.get(i);
            if ( !from.renameTo(to) )
            {
                ret = new CommandProcessorResult(CommandProcessorResult.FAILURE);
                ret.setReason("An error occurred renaming the file");
            }
        }
        if (ret == null) ret = new CommandProcessorResult(CommandProcessorResult.SUCCESS);
        return ret;
    }
}