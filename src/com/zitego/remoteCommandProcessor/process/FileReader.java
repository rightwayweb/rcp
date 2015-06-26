package com.zitego.remoteCommandProcessor.process;

import com.zitego.remoteCommandProcessor.CommandProcessor;
import com.zitego.remoteCommandProcessor.CommandProcessorDocument;
import com.zitego.remoteCommandProcessor.ProcessorArguments;
import com.zitego.remoteCommandProcessor.CommandProcessorResult;
import com.zitego.remoteCommandProcessor.CommandProcessorException;
import com.zitego.markup.xml.XmlTag;
import com.zitego.util.FileUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.util.Vector;

/**
 * This class handles reading text content from a file. The expected processor arguments object should contain
 * one file entities. Each entity should contain the absolute path to the file to be read.
 * The xml should look as follows:<br>
 * <xmp>
 * <processor-arguments>
 *  <file>/<some_directory_path/<filename></file>
 *  ...
 * </processor-arguments>
 * </xmp>
 *
 * @author John Glorioso
 * @version $Id: FileReader.java,v 1.1 2010/11/09 02:35:47 jglorioso Exp $
 */
public class FileReader extends CommandProcessor
{
    private String _file;

    public static void main(String[] a) throws Exception
    {
        FileReader reader = new FileReader();
        ProcessorArguments args = new ProcessorArguments();
        args.addArgument("file", a[0]);
        reader.init(args);
        FileReaderResult result = (FileReaderResult)reader.execute();
        System.out.println( "Result: " + (result.getType() == result.SUCCESS ? "SUCCESS" : "FAILURE") );
        if (result.getReason() != null) System.out.println( "        Reason: "+result.getReason() );
        System.out.println( "Contents:\n" + result.getContent() );
    }

    /**
     * Creates a new file reader.
     */
    public FileReader()
    {
        super();
    }

    /**
     * The file.
     *
     * @param file The file.
     */
    public void setFile(String file)
    {
        _file = file;
    }

    /**
     * The processor arguments should a file argument.
     *
     * @param args The processor arguments.
     */
    public void init(ProcessorArguments args) throws CommandProcessorException
    {
        _file = args.getChildValue("file");
    }

    public CommandProcessorResult execute()
    {
        FileReaderResult ret = null;
        try
        {
            String content = FileUtils.getFileContents(_file);
            ret = new FileReaderResult(CommandProcessorResult.SUCCESS);
            ret.setContent(content);
        }
        catch (IOException ioe)
        {
            ret = new FileReaderResult(CommandProcessorResult.FAILURE);
            ret.setReason( ioe.getMessage() );
            ret.setStackTrace(ioe);
        }
        return ret;
    }

    public CommandProcessorDocument createDocument()
    {
        CommandProcessorDocument doc = super.createDocument();
        ProcessorArguments args = doc.getProcessorArguments();
        XmlTag tag = new XmlTag("file", args);
        tag.addBodyContent(_file);
        args.addBodyContent(tag);
        return doc;
    }
}