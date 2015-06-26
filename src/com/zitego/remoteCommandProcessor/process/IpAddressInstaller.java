package com.zitego.remoteCommandProcessor.process;

import com.zitego.remoteCommandProcessor.ProcessorArguments;
import com.zitego.remoteCommandProcessor.CommandProcessor;
import com.zitego.remoteCommandProcessor.CommandProcessorResult;
import com.zitego.remoteCommandProcessor.CommandProcessorException;
import com.zitego.web.servlet.BaseConfigServlet;
import com.zitego.web.util.StaticWebappProperties;
import com.zitego.markup.xml.XmlTag;
import com.zitego.filemanager.util.WildcardFilter;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;
import java.util.Arrays;

/**
 * <p>This class handles adding and booting a new ip address on a server. The
 * expected processor arguments object should contain one or more ip addresses.</p>
 *
 * An example xml configu is as follows:<br>
 * <xmp>
 * <processor-arguments>
 *  <ip>192.168.0.1</ip>
 *  ...
 * </processor-arguments>
 * </xmp>
 *
 * <p>In addition to the xml configuration the path to the config file, the path to the
 * ifup script is expected to exist within the BaseConfigServlet webapp properties.</p>
 *
 * <p>The ifup script can only be executed by the root user, so a sudo entry must be added
 * for the user that will be executing this class. Additionally, the ifcfg-eth0 files can
 * only be created by a privileged user, so the directory must be writable by the executable
 * user.</p>
 *
 * @author John Glorioso
 * @version $Id: IpAddressInstaller.java,v 1.3 2009/01/31 03:05:09 jglorioso Exp $
 */
public class IpAddressInstaller extends CommandProcessor
{
    private String _ifupScript;
    private Vector _ips = new Vector();

    public static void main(String[] a) throws Exception
    {
        StaticWebappProperties props = BaseConfigServlet.getWebappProperties();
        props.setProperty("ip_installer.ifup_script", "sudo /sbin/ifup");
        IpAddressInstaller installer = new IpAddressInstaller();
        ProcessorArguments args = new ProcessorArguments();
        StringBuffer xml = new StringBuffer();
        for (int i=0; i<a.length; i++)
        {
            XmlTag tag = new XmlTag("ip", args);
            tag.addBodyContent(a[i]);
        }
        installer.init(args);
        CommandProcessorResult result = installer.execute();
        System.out.println( "Result: " + (result.getType() == result.SUCCESS ? "SUCCESS" : "FAILURE") );
        if (result.getReason() != null) System.out.println( "        Reason: "+result.getReason() );
    }

    /**
     * Creates a new ip address installer. It sets the ifup script. If ip_installer.ifup_script do not exist
     * in the webapp properties, then an exception is thrown.
     *
     * @throws IllegalStateException if the required property is missing.
     */
    public IpAddressInstaller() throws IllegalStateException
    {
        super();
        StaticWebappProperties props = BaseConfigServlet.getWebappProperties();
        _ifupScript = (String)props.getProperty("ip_installer.ifup_script");
        if (_ifupScript == null) throw new IllegalStateException("ip_installer.ifup_script not set");
    }

    public void init(ProcessorArguments args) throws CommandProcessorException
    {
        Vector ips = args.getChildrenWithName("ip");
        int size = ips.size();
        for (int i=0; i<size; i++)
        {
            XmlTag tag = (XmlTag)ips.get(i);
            _ips.add( tag.getValue() );
        }
    }

    public CommandProcessorResult execute()
    {
        CommandProcessorResult ret = null;
        try
        {
            int size = _ips.size();
            for (int i=0; i<size; i++)
            {
                String dev = "eth0:" + getNextDeviceId();
                writeConfigFile( dev, (String)_ips.get(i) );
                bootInterface(dev);
            }
            ret = new CommandProcessorResult(CommandProcessorResult.SUCCESS);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            ret = new CommandProcessorResult(CommandProcessorResult.FAILURE);
            ret.setReason( e.toString() );
            ret.setStackTrace(e);
        }
        return ret;
    }

    /**
     * Returns the next device id by listing the files in /etc/sysconfig/network-scripts that match
     * the wildcard pattern eth0*. It then returns the next available device id.
     *
     * @return String
     * @throws IOException if an error occurs.
     */
    protected String getNextDeviceId() throws IOException
    {
        WildcardFilter filter = new WildcardFilter("ifcfg-eth0*", true);
        File dir = new File("/etc/sysconfig/network-scripts");
        String[] files = dir.list(filter);
        Integer exts[] = new Integer[files.length];
        for (int i=0; i<files.length; i++)
        {
            int index = files[i].indexOf(":");
            if (index > -1) exts[i] = new Integer( files[i].substring(index+1) );
            else exts[i] = new Integer(-1);
        }
        Arrays.sort(exts);
        int last = exts[exts.length-1].intValue() + 1;
        return String.valueOf(last);
    }

    /**
     * Creates the new config file given the device id and an ip address.
     *
     * @param dev The device.
     * @param ip The ip.
     * @throws IOException if an error occurs.
     */
    protected void writeConfigFile(String dev, String ip) throws IOException
    {
        Vector contents = new Vector();
        BufferedReader in = new BufferedReader( new FileReader("/etc/sysconfig/network-scripts/ifcfg-eth0") );
        String line = null;
        //Erase contents and append to end
        while ( (line=in.readLine()) != null )
        {
            if ( line.equals("") ) continue;
            //Check for "DEVICE" line
            if (line.indexOf("DEVICE") == 0) contents.add("DEVICE="+dev);
            //Check for IPADDR line
            else if (line.indexOf("IPADDR") == 0) contents.add("IPADDR="+ip);
            else contents.add(line);
        }
        in.close();

        //Write out the file
        PrintWriter out = new PrintWriter( new BufferedWriter(new FileWriter("/etc/sysconfig/network-scripts/ifcfg-"+dev, false)) );
        int size = contents.size();
        for (int i=0; i<size; i++)
        {
            out.println( (String)contents.get(i) );
        }
        out.flush();
        out.close();
    }

    /**
     * Boots the ip interface.
     *
     * @throws IOException if an error occurs.
     */
    protected void bootInterface(String dev) throws IOException, InterruptedException
    {
        Process p = Runtime.getRuntime().exec(_ifupScript+" "+dev+" boot");
        p.waitFor();
        if ( p.exitValue() != 0) throw new IOException( getProcessError(p) );
    }
}