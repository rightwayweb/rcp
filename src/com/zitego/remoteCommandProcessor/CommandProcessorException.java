package com.zitego.remoteCommandProcessor;

/**
 * An exception to be thrown when a problem occurs executing the CommandProcessor.
 *
 * @author John Glorioso
 * @version $Id: CommandProcessorException.java,v 1.1.1.1 2008/02/20 14:58:34 jglorioso Exp $
 */
public class CommandProcessorException extends Exception
{
    public CommandProcessorException(String msg)
    {
        super(msg);
    }

    public CommandProcessorException(String msg, Throwable root)
    {
        super(msg, root);
    }
}