package com.zitego.remoteCommandProcessor;

import com.zitego.markup.xml.XmlTag;
import com.zitego.util.StringValidation;

/**
 * This class represents customized arguments for a command processor. It is an extension
 * of XmlTag that should exist within a CommandProcessorDocument. It is simply a list
 * of name/value arguments.
 *
 * @author John Glorioso
 * $version $Id: ProcessorArguments.java,v 1.1.1.1 2008/02/20 14:58:34 jglorioso Exp $
 */
public class ProcessorArguments extends XmlTag
{
    /**
     * Creates a new processor arguments object.
     */
    public ProcessorArguments()
    {
        super("processor-arguments");
    }

    /**
     * Creates a new processor arguments object with a parent.
     *
     * @param XmlTag The parent.
     */
    public ProcessorArguments(XmlTag parent)
    {
        super("processor-arguments", parent);
    }

    /**
     * Adds a processor argument to the list.
     *
     * @param name The name of the argument.
     * @param value The value of the argument.
     * @throws IllegalArgumentException if either argument is null or blank.
     */
    public void addArgument(String name, String value) throws IllegalArgumentException
    {
        if ( StringValidation.isEmpty(name) || StringValidation.isEmpty(value) )
        {
            throw new IllegalArgumentException("name and value must be defined");
        }
        XmlTag arg = new XmlTag(name);
        arg.setValue(value);
        addBodyContent(arg);
    }
}