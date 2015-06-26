package com.zitego.remoteCommandProcessor.process;

import com.zitego.remoteCommandProcessor.CommandProcessor;
import com.zitego.remoteCommandProcessor.ProcessorArguments;
import com.zitego.remoteCommandProcessor.CommandProcessorResult;
import com.zitego.remoteCommandProcessor.CommandProcessorException;
import com.zitego.markup.xml.XmlTag;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.util.Vector;

/**
 * This class handles writing text content to a file. The expected processor arguments object should contain
 * one or more file entities. Each entity should contain the absolute path to the file to be written.
 * Additionally, a content entity is required. It is advised that the content reside in a CDATA block.
 * The xml should look as follows:<br>
 * <xmp>
 * <processor-arguments>
 *  <content>
 *  <![CDATA[
 *  [some_content]
 *  ]]>
 *  </content>
 *  <file>/<some_directory_path/<filename></file>
 *  ...
 * </processor-arguments>
 * </xmp>
 *
 * @author John Glorioso
 * @version $Id: FileWriter.java,v 1.2 2008/04/14 20:21:15 jglorioso Exp $
 */
public class FileWriter extends CommandProcessor
{
    private String _content;
    private Vector _files = new Vector();

    public static void main(String[] a) throws Exception
    {
        FileWriter writer = new FileWriter();
        ProcessorArguments args = new ProcessorArguments();
        StringBuffer xml = new StringBuffer();
        java.io.BufferedReader in = new java.io.BufferedReader( new java.io.FileReader(a[0]) );
        String line = null;
        while ( (line=in.readLine()) != null )
        {
            xml.append(line).append("\r\n");
        }
        args.parse(xml.toString(), com.zitego.format.FormatType.XML);
        writer.init(args);
        CommandProcessorResult result = writer.execute();
        System.out.println( "Result: " + (result.getType() == result.SUCCESS ? "SUCCESS" : "FAILURE") );
        if (result.getReason() != null) System.out.println( "        Reason: "+result.getReason() );
    }

    /**
     * Creates a new file writer.
     */
    public FileWriter()
    {
        super();
    }

    /**
     * The processor arguments should a content argument as well as one or more file arguments.
     *
     * @param args The processor arguments.
     */
    public void init(ProcessorArguments args) throws CommandProcessorException
    {
        _content = args.getChildValue("content");
        Vector files = args.getChildrenWithName("file");
        int size = files.size();
        for (int i=0; i<size; i++)
        {
            XmlTag tag = (XmlTag)files.get(i);
            _files.add( tag.getValue() );
        }
    }

    public CommandProcessorResult execute()
    {
        CommandProcessorResult ret = null;
        try
        {
            int size = _files.size();
            for (int i=0; i<size; i++)
            {
                PrintWriter out = new PrintWriter( new BufferedWriter(new java.io.FileWriter((String)_files.get(i))) );
                out.write( _content, 0, _content.length() );
                out.flush();
                out.close();
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