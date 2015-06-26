package com.zitego.remoteCommandProcessor.process;

import com.zitego.remoteCommandProcessor.CommandProcessor;
import com.zitego.remoteCommandProcessor.CommandProcessorDocument;
import com.zitego.remoteCommandProcessor.ProcessorArguments;
import com.zitego.remoteCommandProcessor.CommandProcessorResult;
import com.zitego.remoteCommandProcessor.CommandProcessorException;
import com.zitego.markup.xml.XmlTag;
import com.zitego.filemanager.util.WildcardFilter;
import java.io.IOException;
import java.io.File;
import java.util.Date;
import java.util.Vector;
import java.util.Hashtable;

/**
 * This class handles listing files in specified directories. The expected processor arguments object
 * should contain one or more directory entities. Each entity should contain the absolute path to the
 * directory to be listed. The xml should look as follows:<br>
 * <xmp>
 * <processor-arguments>
 *  <directory>/<some_directory_path</directory>
 *  ...
 * </processor-arguments>
 * </xmp>
 *
 * @author John Glorioso
 * @version $Id: FileLister.java,v 1.2 2010/11/09 02:34:36 jglorioso Exp $
 */
public class FileLister extends CommandProcessor
{
    private Vector<String> _directories = new Vector<String>();
    private Hashtable<String, Vector<String>> _filters = new Hashtable<String, Vector<String>>();

    public static void main(String[] a) throws Exception
    {
        FileLister lister = new FileLister();
        ProcessorArguments args = new ProcessorArguments();
        StringBuffer xml = new StringBuffer();
        java.io.BufferedReader in = new java.io.BufferedReader( new java.io.FileReader(a[0]) );
        String line = null;
        while ( (line=in.readLine()) != null )
        {
            xml.append(line).append("\r\n");
        }
        args.parse(xml.toString(), com.zitego.format.FormatType.XML);
        lister.init(args);
        CommandProcessorResult result = lister.execute();
        System.out.println( "Result: " + (result.getType() == result.SUCCESS ? "SUCCESS" : "FAILURE") );
        if (result.getReason() != null) System.out.println( "        Reason: "+result.getReason() );
    }

    /**
     * Creates a new file lister.
     */
    public FileLister()
    {
        super();
    }

    /**
     * The processor arguments should one or more file arguments.
     *
     * @param args The processor arguments.
     */
    public void init(ProcessorArguments args) throws CommandProcessorException
    {
        Vector files = args.getChildrenWithName("directory");
        int size = files.size();
        for (int i=0; i<size; i++)
        {
            XmlTag tag = (XmlTag)files.get(i);
            addDirectory( tag.getValue(), tag.getTagAttribute("filter") );
        }
    }

    /**
     * Adds a directory to be listed.
     *
     * @param dir The directory to add.
     */
    public void addDirectory(String dir)
    {
        addDirectory(dir, null);
    }

    /**
     * Adds a directory to be listed with a wildcard filter pattern that can be null.
     *
     * @param dir The directory to add.
     * @param filter The filter.
     */
    public void addDirectory(String dir, String filter)
    {
        if (dir == null) return;
        if ( !_directories.contains(dir) ) _directories.add(dir);
        if (filter != null)
        {
            if (_filters.get(dir) == null) _filters.put( dir, new Vector<String>() );
            _filters.get(dir).add(filter);
        }
    }

    public CommandProcessorResult execute()
    {
        FileListerResult ret = new FileListerResult();
        int size = _directories.size();
        for (int i=0; i<size; i++)
        {
            String dir = _directories.get(i);
            if (_filters.get(dir) == null || _filters.get(dir).size() == 0)
            {
                File[] files = new File(dir).listFiles();
                if (files == null) files = new File[0];
                for (int j=0; j<files.length; j++)
                {
                    ret.addResult(dir, files[j].getName(), new Date(files[j].lastModified()), files[j].length() );
                }
            }
            else
            {
                Vector<String> filters = _filters.get(dir);
                int size2 = filters.size();
                for (int j=0; j<size2; j++)
                {
                    File[] files = new File(dir).listFiles( new WildcardFilter(filters.get(j), false) );
                    if (files == null) files = new File[0];
                    for (int k=0; k<files.length; k++)
                    {
                        ret.addResult(dir, files[k].getName(), new Date(files[k].lastModified()), files[k].length() );
                    }
                }
            }
        }
        ret.setType(CommandProcessorResult.SUCCESS);
        return ret;
    }

    public CommandProcessorDocument createDocument()
    {
        CommandProcessorDocument doc = super.createDocument();
        ProcessorArguments args = doc.getProcessorArguments();
        int size = _directories.size();
        for (int i=0; i<size; i++)
        {
            String dir = _directories.get(i);
            Vector<String> filters = _filters.get(dir);
            if (filters == null || filters.size() == 0)
            {
                XmlTag tag = new XmlTag("directory", args);
                tag.addBodyContent(dir);
                args.addBodyContent(tag);
            }
            else
            {
                int size2 = filters.size();
                for (int j=0; j<size2; j++)
                {
                    XmlTag tag = new XmlTag("directory", args);
                    tag.setAttribute( "filter", filters.get(j) );
                    tag.addBodyContent(dir);
                    args.addBodyContent(tag);
                }
            }
        }
        return doc;
    }
}