package com.zitego.remoteCommandProcessor.process;

import com.zitego.remoteCommandProcessor.CommandProcessor;
import com.zitego.remoteCommandProcessor.CommandProcessorDocument;
import com.zitego.remoteCommandProcessor.ProcessorArguments;
import com.zitego.remoteCommandProcessor.CommandProcessorResult;
import com.zitego.remoteCommandProcessor.CommandProcessorException;
import com.zitego.markup.xml.XmlTag;
import java.io.File;
import java.util.Vector;

/**
 * This class handles removing files. The expected processor arguments object should contain
 * one or more file entities. Each entity should contain the absolute path to the file to be removed.
 * The xml should look as follows:<br>
 * <xmp>
 * <processor-arguments>
 *  <file>/<some_directory_path/<filename></file>
 *  ...
 * </processor-arguments>
 * </xmp>
 *
 * @author John Glorioso
 * @version $Id: FileRemover.java,v 1.1.1.1 2008/02/20 14:58:34 jglorioso Exp $
 */
public class FileRemover extends CommandProcessor
{
    private Vector _files = new Vector();

    public static void main(String[] a) throws Exception
    {
        FileRemover remover = new FileRemover();
        ProcessorArguments args = new ProcessorArguments();
        StringBuffer xml = new StringBuffer();
        java.io.BufferedReader in = new java.io.BufferedReader( new java.io.FileReader(a[0]) );
        String line = null;
        while ( (line=in.readLine()) != null )
        {
            xml.append(line).append("\r\n");
        }
        args.parse(xml.toString(), com.zitego.format.FormatType.XML);
        remover.init(args);
        CommandProcessorResult result = remover.execute();
        System.out.println( "Result: " + (result.getType() == result.SUCCESS ? "SUCCESS" : "FAILURE") );
        if (result.getReason() != null) System.out.println( "        Reason: "+result.getReason() );
    }

    /**
     * Creates a new file remover.
     */
    public FileRemover()
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
        Vector files = args.getChildrenWithName("file");
        int size = files.size();
        for (int i=0; i<size; i++)
        {
            XmlTag tag = (XmlTag)files.get(i);
            addFile( tag.getValue() );
        }
    }

    /**
     * Adds a file to be removed.
     *
     * @param file The path of the file to be removed.
     * @throws IllegalArgumentException if the file is null or an empty string.
     */
    public void addFile(String file) throws IllegalArgumentException
    {
        if ( file == null || "".equals(file) ) throw new IllegalArgumentException("File path cannot be null or an empty string");
        _files.add(file);
    }

    public CommandProcessorResult execute()
    {
        int size = _files.size();
        for (int i=0; i<size; i++)
        {
            File f = new File( (String)_files.get(i) );
            if ( f.exists() ) f.delete();
        }
        return new CommandProcessorResult(CommandProcessorResult.SUCCESS);
    }

    public CommandProcessorDocument createDocument()
    {
        CommandProcessorDocument doc = super.createDocument();
        ProcessorArguments args = doc.getProcessorArguments();
        int size = _files.size();
        for (int i=0; i<size; i++)
        {
            XmlTag tag = new XmlTag("file", args);
            tag.addBodyContent( (String)_files.get(i) );
            args.addBodyContent(tag);
        }
        return doc;
    }
}