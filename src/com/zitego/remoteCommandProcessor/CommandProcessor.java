package com.zitego.remoteCommandProcessor;

import com.zitego.util.TimeoutException;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileWriter;

/**
 * This is an abstract class that defines how a command processor should work.
 *
 * @author John Glorioso
 * @version $Id: CommandProcessor.java,v 1.2 2009/01/31 03:05:03 jglorioso Exp $
 */
public abstract class CommandProcessor
{
    /**
     * Creates a new Command Processor.
     */
    public CommandProcessor() { }

    /**
     * Initializes the command processor with the given ProcessorArguments.
     *
     * @param args The processor arguments.
     */
    public abstract void init(ProcessorArguments args) throws CommandProcessorException;

    /**
     * Executes the command and returns a result.
     *
     * @return CommandProcessorResult
     */
    public abstract CommandProcessorResult execute();

    /**
     * Returns a command processor document to be used to send a remote request. The
     * document already has teh processor-class tag set.
     *
     * @return CommandProcessorDocument
     */
    public CommandProcessorDocument createDocument()
    {
        CommandProcessorDocument doc = new CommandProcessorDocument();
        doc.setCommandProcessorClass( getClass().getName() );
        return doc;
    }

    /**
     * Creates a lock file for editing. If there is no lock, one is created and the method returns immediately,
     * otherwise it waits for the lock to be released. Once the lock is obtained, it checks to make sure that
     * the config file actually exists.
     *
     * @param lockFile The lock file.
     * @param pid The process id or identifier to store in the lock file.
     * @throws IOException if the file does not exist or an error occurs reading from it.
     * @throws TimeoutException if we could not get passed the lock.
     * @throws InterruptedException if an error occurs while waiting for the lock.
     */
    protected void createLockFile(File lockFile, String pid) throws IOException, TimeoutException, InterruptedException
    {
        if (lockFile == null) throw new IllegalArgumentException("lock file not set");
        if (pid == null) throw new IllegalArgumentException("pid not set");
        long start = System.currentTimeMillis();
        boolean created = false;
        while (!created)
        {
            synchronized( lockFile.getAbsolutePath().intern() )
            {
                //See if we can create the lock file
                if ( !lockFile.exists() )
                {
                    if ( lockFile.createNewFile() )
                    {
                        //Write the website id to it
                        FileWriter out = new FileWriter(lockFile);
                        out.write( pid, 0, pid.length() );
                        out.flush();
                        out.close();
                        created = true;
                    }
                }
            }

            if (!created)
            {
                //See if we are passed 30 seconds, if so then error
                if ( (System.currentTimeMillis()-start)/1000L >= 30 )
                {
                    throw new TimeoutException("could not obtain lock after 30 seconds.");
                }
                //Wait 10 seconds and try again
                Thread.sleep(10000);
            }
        }
    }

    /**
     * Returns any error from the error stream of a process.
     *
     * @param p The process.
     * @return String
     * @throws IOException if an error occurs reading the error stream.
     */
    protected String getProcessError(Process p) throws IOException
    {
        BufferedReader err = new BufferedReader( new InputStreamReader(p.getErrorStream()) );
        StringBuffer errMsg = new StringBuffer();
        String line = null;
        while ( (line=err.readLine()) != null )
        {
            errMsg.append(line + "\r\n");
        }
        err = new BufferedReader( new InputStreamReader(p.getInputStream()) );
        while ( (line=err.readLine()) != null )
        {
            errMsg.append(line + "\r\n");
        }
        return errMsg.toString();
    }
}