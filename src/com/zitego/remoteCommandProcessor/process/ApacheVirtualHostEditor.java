package com.zitego.remoteCommandProcessor.process;

import com.zitego.remoteCommandProcessor.CommandProcessor;
import com.zitego.remoteCommandProcessor.ProcessorArguments;
import com.zitego.remoteCommandProcessor.CommandProcessorResult;
import com.zitego.remoteCommandProcessor.CommandProcessorException;
import com.zitego.web.servlet.BaseConfigServlet;
import com.zitego.web.util.StaticWebappProperties;
import com.zitego.markup.xml.XmlTag;
import com.zitego.util.TimeoutException;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.FileReader;
import java.util.Vector;

/**
 * <p>This class handles editing a virtual host entry in the apache config file. The
 * expected processor arguments object should contain a unique website_id that is used
 * to surround the config block in the apache configuration file and one or more virtual
 * host configuration blocks. The virtual host configuration block must contain an ip,
 * port, server_admin, server_name, and document_root directives. Optionally, any other
 * apache virtual server directive is allowed including nested blocks.</p>
 *
 * An example xml configu is as follows:<br>
 * <xmp>
 * <processor-arguments>
 *  <website_id>100</website_id>
 *  <virtual_host>
 *   <is_name_virtual_host>false</is_name_virtual_host>
 *   <ip>192.168.0.1</ip>
 *   <port>80</port>
 *   <server_admin>webmaster@example.com</server_admin>
 *   <server_name>example.com</server_name>
 *   <server_alias>www.example.com alt.example.com</server_alias>
 *   <document_root>/home/httpd/domains/example.com</document_root>
 *   <ErrorLog>logs/example.com_error.log</ErrorLog>
 *   <CustomLog>logs/example.com_access.log common</CustomLog>
 *   <KeepAlive>On</KeepAlive>
 *  </virtual_host>
 *  ...
 * </processor-arguments>
 * </xmp>
 *
 * <p>In addition to the xml configuration the path to the config file, the path to a lock file,
 * and the path to the apache restart script are expected to exist within the BaseConfigServlet
 * webapp properties. The lock file is used to insure that only one process at a time can
 * change the configuration file.</p>
 *
 * @author John Glorioso
 * @version $Id: ApacheVirtualHostEditor.java,v 1.6 2013/09/01 12:22:52 jglorioso Exp $
 */
public class ApacheVirtualHostEditor extends CommandProcessor
{
    private File _configFile;
    private File _lockFile;
    private String _restartScript;
    private String _websiteId;
    private Vector _virtualHosts = new Vector();

    public static void main(String[] a) throws Exception
    {
        StaticWebappProperties props = BaseConfigServlet.getWebappProperties();
        props.setProperty("vhost_editor.config_file", "hosted_domains.conf");
        props.setProperty("vhost_editor.lock_file", "hosted_domains.lck");
        props.setProperty("vhost_editor.apache_restart_script", "/etc/init.d/apache restart");
        ApacheVirtualHostEditor editor = new ApacheVirtualHostEditor();
        ProcessorArguments args = new ProcessorArguments();
        StringBuffer xml = new StringBuffer();
        BufferedReader in = new BufferedReader( new FileReader(a[0]) );
        String line = null;
        while ( (line=in.readLine()) != null )
        {
            xml.append(line).append("\r\n");
        }
        args.parse(xml.toString(), com.zitego.format.FormatType.XML);
        editor.init(args);
        CommandProcessorResult result = editor.execute();
        System.out.println( "Result: " + (result.getType() == result.SUCCESS ? "SUCCESS" : "FAILURE") );
        if (result.getReason() != null) System.out.println( "        Reason: "+result.getReason() );
    }

    /**
     * Creates a new apache config editor. It sets the config file, lock file, and apache
     * restart script. If vhost_editor.config_file, vhost_editor.lock_file, and
     * vhost_editor.apache_restart_script do not exist in the webapp properties, then an
     * exception is thrown.
     *
     * @throws IllegalStateException if any required properties are missing.
     */
    public ApacheVirtualHostEditor() throws IllegalStateException
    {
        super();
        StaticWebappProperties props = BaseConfigServlet.getWebappProperties();
        String prop = (String)props.getProperty("vhost_editor.config_file");
        if (prop == null) throw new IllegalStateException("vhost_editor.config_file not set");
        _configFile = new File(prop);
        prop = (String)props.getProperty("vhost_editor.lock_file");
        if (prop == null) throw new IllegalStateException("chost_editor.lock_file not set");
        _lockFile = new File(prop);
        _restartScript = (String)props.getProperty("vhost_editor.apache_restart_script");
        if (_restartScript == null) throw new IllegalStateException("chost_editor.apache_restart_script not set");
    }

    public void init(ProcessorArguments args) throws CommandProcessorException
    {
        _websiteId = args.getFirstOccurrenceOf("website_id").getValue();
        if (_websiteId == null) throw new CommandProcessorException("website_id not set");

        Vector hosts = args.getChildrenWithName("virtual_host");
        int size = hosts.size();
        for (int i=0; i<size; i++)
        {
            XmlTag tag = (XmlTag)hosts.get(i);
            VirtualHost host = new VirtualHost();
            host.isNameVirtualHost = new Boolean( tag.getChildValue("is_name_virtual_host") ).booleanValue();
            host.ip = tag.getChildValue("ip");
            if (host.ip == null) throw new CommandProcessorException("ip not defined in virtual host block");
            host.port = tag.getChildValue("port");
            if (host.port == null) host.port = "80";
            host.serverAdmin = tag.getChildValue("server_admin");
            if (host.serverAdmin == null) throw new CommandProcessorException("server_admin not defined for virtual host: "+host.ip);
            host.serverName = tag.getChildValue("server_name");
            if (host.serverName == null) throw new CommandProcessorException("server_name not defined for virtual host: "+host.ip);
            host.serverName = tag.getChildValue("server_name");
            host.serverAlias = tag.getChildValue("server_alias");
            if ( "".equals(host.serverAlias) ) host.serverAlias = null;
            host.docRoot = tag.getChildValue("document_root");
            if (host.docRoot == null) throw new CommandProcessorException("document_root not defined for virtual host: "+host.serverName);
            int size2 = tag.getBodySize();
            String tagsToSkip = "is_name_virtual_host,ip,port,server_admin,ServerAdmin,server_name,server_alias,ServerName,document_root,DocumentRoot";
            for (int j=0; j<size2; j++)
            {
                XmlTag tag2 = (XmlTag)tag.getBodyContent(j);
                if (tagsToSkip.indexOf( tag2.getTagName() ) > -1) continue;
                host.configs.add(tag2);
            }
            _virtualHosts.add(host);
        }
    }

    public CommandProcessorResult execute()
    {
        CommandProcessorResult ret = null;
        try
        {
            createLockFile(_lockFile, _websiteId);
            if ( !_configFile.exists() ) throw new IOException("apache config file: "+_configFile+" does not exist");
            writeConfigFile();
            restartApache();
            ret = new CommandProcessorResult(CommandProcessorResult.SUCCESS);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            ret = new CommandProcessorResult(CommandProcessorResult.FAILURE);
            ret.setReason( e.toString() );
            ret.setStackTrace(e);
        }
        finally
        {
            if (_lockFile != null) _lockFile.delete();
        }
        return ret;
    }

    /**
     * Reads through the config file and looks for a section to edit for the website. If it finds one, it
     * erases it. It then appends the new virtual host configuration to the end.
     *
     * @throws IOException if an error occurs.
     */
    protected void writeConfigFile() throws IOException
    {
        Vector contents = new Vector();
        BufferedReader in = new BufferedReader( new FileReader(_configFile) );
        String line = null;
        boolean foundBegin = false;
        boolean foundEnd = false;
        //Erase contents and append to end
        while ( (line=in.readLine()) != null )
        {
            if ( line.equals("") ) continue;
            //Check for begin
            if (line.indexOf("# BEGIN website:"+_websiteId+" config") == 0) foundBegin = true;
            if (foundBegin && !foundEnd)
            {
                //Check for end
                if (line.indexOf("# END website:"+_websiteId+" config") == 0) foundEnd = true;
            }
            else
            {
                contents.add(line);
            }
        }
        in.close();
        if (foundBegin && !foundEnd) throw new RuntimeException("Config file error. BEGIN tag found for website: "+_websiteId+", but no END tag found");
        //Write out the file and append the new stuff on the end
        PrintWriter out = new PrintWriter( new BufferedWriter(new FileWriter(_configFile, false)) );
        int size = contents.size();
        for (int i=0; i<size; i++)
        {
            line = (String)contents.get(i);
            if ( line.startsWith("# BEGIN") ) out.println("");
            out.println( contents.get(i) );
        }
        out.println("");
        out.println("# BEGIN website:"+_websiteId+" config");
        size = _virtualHosts.size();
        for (int i=0; i<size; i++)
        {
            if (i > 0) out.println("");
            VirtualHost host = (VirtualHost)_virtualHosts.get(i);
            if (!host.isNameVirtualHost) out.println("LISTEN "+host.ip+":"+host.port);
            out.println("<VirtualHost "+host.ip+":"+host.port+">");
            out.println("   ServerAdmin "+host.serverAdmin);
            out.println("   ServerName "+host.serverName);
            if (host.serverAlias != null) out.println("   ServerAlias "+host.serverAlias);
            out.println("   DocumentRoot "+host.docRoot);
            //Loop through the rest of the configs and print out the config
            int size2 = host.configs.size();
            for (int j=0; j<size2; j++)
            {
                printConfig(out, (XmlTag)host.configs.get(j), "   ");
            }
            out.println("</VirtualHost>");
        }
        out.println("# END website:"+_websiteId+" config");
        out.flush();
        out.close();
    }

    private void printConfig(PrintWriter out, XmlTag config, String indent) throws IOException
    {
        int size = config.getBodySize();
        if ( size == 1 && !(config.getBodyContent(0) instanceof XmlTag) )
        {
            if ( "COMMENT".equals(config.getTagName()) ) out.println( indent + config.getValue() );
            else out.println( indent + config.getTagName() + " " + config.getValue() );
        }
        else
        {
            out.println(indent + "<" + config.getTagName() + ">");
            for (int i=0; i<size; i++)
            {
                printConfig(out, (XmlTag)config.getBodyContent(i), indent+"   ");
            }
            out.println(indent + "</" + config.getTagName() + ">");
        }
    }

    /**
     * Restarts the apache webserver.
     *
     * @throws IOException if an error occurs.
     * @throws InterruptedException if an error occurs while waiting for the script to complete.
     */
    protected void restartApache() throws IOException, InterruptedException
    {
        if (_restartScript == null) throw new IOException("website.apache.restart is not set in webapp properties");
        Process p = Runtime.getRuntime().exec(_restartScript);
        p.waitFor();
        if ( p.exitValue() != 0) throw new IOException( getProcessError(p) );
    }

    /**
     * This static method takes an old url and new url and given current apache settings, a line id
     * that identifies the line with the urls to change, and the proxy port to forward to will
     * re-create the apache settings with the new url. If the urls are both null or are equal,
     * null is returned.
     *
     * @param oldUrl The old url to change.
     * @param newUrl The new url to change.
     * @param apacheSettings The apache settings with the urls to change. Can be null.
     * @param lineId The identifier to look for to find the line that needs to be changed.
     * @param proxyUrl The proxy url to forward to.
     * @return String
     */
    public static String changeCustomUrl(String oldUrl, String newUrl, String apacheSettings, String lineId, String proxyUrl)
    {
        //See if we need to do anything
        if (newUrl == null) return null;
        else if ( oldUrl != null && oldUrl.equals(newUrl) ) return null;

        if (apacheSettings == null) apacheSettings = "";
        BufferedReader in = new BufferedReader( new StringReader(apacheSettings) );
        String line = null;
        Vector contents = new Vector();
        try
        {
            //Erase contents and append to end
            while ( (line=in.readLine()) != null )
            {
                if ( line.equals("") ) continue;
                //Check for begin
                if (lineId == null || line.indexOf(lineId) == -1) contents.add(line);
            }
            in.close();
        }
        catch (IOException ioe)
        {
            //Should never happen on a string reader, but if so print stack trace and return null
            ioe.printStackTrace();
            return null;
        }
        //Re-create the settings and append the new stuff on the end
        StringBuffer newSettings = new StringBuffer();
        int size = contents.size();
        for (int i=0; i<size; i++)
        {
            newSettings.append( contents.get(i) ).append("\n");
        }

        newUrl = newUrl.replaceAll("\\.", "\\\\.").replaceAll("\\-", "\\\\-");
        newSettings.append("RewriteRule ^").append(newUrl).append("\\??(.*)    ").append(proxyUrl).append(" [P,QSA,NC,L] ").append(lineId).append("\n");

        return newSettings.toString();
    }

    private class VirtualHost
    {
        private boolean isNameVirtualHost = false;
        private String ip;
        private String port;
        private String serverAdmin;
        private String serverName;
        private String serverAlias;
        private String docRoot;
        private Vector configs = new Vector();

        private VirtualHost() { }
    }
}
