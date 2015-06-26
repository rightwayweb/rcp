package com.zitego.remoteCommandProcessor.process;

import com.zitego.remoteCommandProcessor.CommandProcessor;
import com.zitego.remoteCommandProcessor.CommandProcessorDocument;
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
import java.io.FileReader;
import java.util.Vector;

/**
 * <p>This class handles starting, stopping, and restarting a wave server.</p>
 * <p>The possible commands are:
 * <ul>
 *  <li>start - Starts the server.</li>
 *  <li>stop - Stops the server.</li>
 *  <li>restart - Restarts the server.</li>
 *  <li>check - Checks to see if the server is running.</li>
 * </ul>
 * </p>
 * An example xml config is as follows:<br>
 * <xmp>
 * <processor-arguments>
 *  <command>restart</command>
 * </processor-arguments>
 * </xmp>
 *
 * <p>In addition to the xml configuration the path to the config file, the path to a lock file,
 * and the path to the start and stop scripts are expected to exist within the BaseConfigServlet
 * webapp properties. The lock file is used to insure that only one process at a time can
 * restart the server.</p>
 *
 * @author John Glorioso
 * @version $Id: WAVEServerControl.java,v 1.1 2009/01/31 03:05:34 jglorioso Exp $
 */
public class WAVEServerControl extends CommandProcessor
{
    public static final int START = 1;
    public static final int STOP = 2;
    public static final int RESTART = 3;
    public static final int CHECK = 4;
    private int _command = -1;
    private File _lockFile;
    private String _stopScript;
    private String _startScript;
    private String _killScript;
    private String _serverRunningCommand;

    public static void main(String[] a) throws Exception
    {
        StaticWebappProperties props = BaseConfigServlet.getWebappProperties();
        props.setProperty("wave_server_control.lock_file", "wave_server_control.lck");
        props.setProperty("wave_server_control.start_script", "/usr/local/tomcat/bin/start_waveserver.sh");
        props.setProperty("wave_server_control.stop_script", "/usr/local/tomcat/bin/stop_waveserver.sh");
        props.setProperty("wave_server_control.kill_script", "/usr/local/tomcat/bin/kill_waveserver.sh");
        props.setProperty("wave_server_control.server_running_command", "/usr/local/tomcat/bin/checkserver.sh");
        WAVEServerControl control = new WAVEServerControl();
        ProcessorArguments args = new ProcessorArguments();
        StringBuffer xml = new StringBuffer();
        BufferedReader in = new BufferedReader( new FileReader(a[0]) );
        String line = null;
        while ( (line=in.readLine()) != null )
        {
            xml.append(line).append("\r\n");
        }
        args.parse(xml.toString(), com.zitego.format.FormatType.XML);
        control.init(args);
        CommandProcessorResult result = control.execute();
        System.out.println( "Result: " + (result.getType() == result.SUCCESS ? "SUCCESS" : "FAILURE") );
        if (result.getReason() != null) System.out.println( "        Reason: "+result.getReason() );
    }

    /**
     * Creates a new wave server control. It sets the lock file, and start script, and
     * stop script. If wave_server_control.lock_file, wave_server_control.start_script,
     * and wave_server_control.stop_script do not exist in the webapp properties, then an
     * exception is thrown.
     *
     * @throws IllegalStateException if any required properties are missing.
     */
    public WAVEServerControl() throws IllegalStateException
    {
        super();
        StaticWebappProperties props = BaseConfigServlet.getWebappProperties();
        String prop = (String)props.getProperty("wave_server_control.lock_file");
        if (prop == null) throw new IllegalStateException("wave_server_control.lock_file not set");
        _lockFile = new File(prop);
        _stopScript = (String)props.getProperty("wave_server_control.stop_script");
        if (_stopScript == null) throw new IllegalStateException("wave_server_control.stop_script not set");
        _startScript = (String)props.getProperty("wave_server_control.start_script");
        if (_startScript == null) throw new IllegalStateException("wave_server_control.start_script not set");
        _serverRunningCommand = (String)props.getProperty("wave_server_control.server_running_command");
        if (_serverRunningCommand == null) throw new IllegalStateException("wave_server_control.server_running_command not set");
        _killScript = (String)props.getProperty("wave_server_control.kill_script");
        if (_killScript == null) throw new IllegalStateException("wave_server_control.kill_script not set");
    }

    public void init(ProcessorArguments args) throws CommandProcessorException
    {
        String cmd = args.getFirstOccurrenceOf("command").getValue();
        if ( "start".equalsIgnoreCase(cmd) ) _command = START;
        else if ( "stop".equalsIgnoreCase(cmd) ) _command = STOP;
        else if ( "restart".equalsIgnoreCase(cmd) ) _command = RESTART;
        else if ( "check".equalsIgnoreCase(cmd) ) _command = CHECK;
        if (_command != START && _command != STOP && _command != RESTART && _command != CHECK)
            throw new CommandProcessorException("command "+cmd+" is invalid");
    }

    public CommandProcessorResult execute()
    {
        CommandProcessorResult ret = null;
        try
        {
            if (_command == CHECK)
            {
                ret = new CommandProcessorResult(CommandProcessorResult.SUCCESS);
                ret.setReason( (checkServer() ? "Server is running" : "Server is not running") );
            }
            else
            {
                createLockFile( _lockFile, String.valueOf(System.currentTimeMillis()) );
                if (_command == STOP || _command == RESTART) stopServer();
                if (_command == START || _command == RESTART) startServer();
                ret = new CommandProcessorResult(CommandProcessorResult.SUCCESS);
            }
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
     * Stops the wave server. Once the stop script is run, it checks for 15 seconds to make sure it stops.
     * If not, it forces a kill command to make sure it is dead.
     *
     * @throws IOException if an error occurs.
     * @throws InterruptedException if an error occurs while waiting for the script to complete.
     */
    protected void stopServer() throws IOException, InterruptedException
    {
        if (_stopScript == null) throw new IOException("wave_server_control.stop_script is not set in webapp properties");
        Process p = Runtime.getRuntime().exec(_stopScript);
        p.waitFor();
        if ( p.exitValue() != 0) throw new IOException( getProcessError(p) );

        //Check to make sure there is not a process running for up to 15 seconds
        long start = System.currentTimeMillis();
        boolean serverStillRunning = true;
        long timeElapsed = 0;
        do
        {
            if ( !checkServer() ) serverStillRunning = false;
            else timeElapsed = System.currentTimeMillis() - start;
            if (serverStillRunning) Thread.sleep(1000);
        }
        while (serverStillRunning && timeElapsed < 15000);
        if (serverStillRunning)
        {
            //Kill it
            p = Runtime.getRuntime().exec(_killScript);
            p.waitFor();
        }
    }

    /**
     * Starts the wave server.
     *
     * @throws IOException if an error occurs.
     * @throws InterruptedException if an error occurs while waiting for the script to complete.
     */
    protected void startServer() throws IOException, InterruptedException
    {
        if (_startScript == null) throw new IOException("wave_server_control.start_script is not set in webapp properties");
        Process p = Runtime.getRuntime().exec(_startScript);
        p.waitFor();
        if ( p.exitValue() != 0) throw new IOException( getProcessError(p) );
    }

    /**
     * Checks to see if the server is running.
     *
     * @return boolean
     */
    public boolean checkServer() throws IOException, InterruptedException
    {
        Process p = Runtime.getRuntime().exec(_serverRunningCommand);
        BufferedReader reader = new BufferedReader( new InputStreamReader(p.getInputStream()) );
        String line;
        StringBuffer output = new StringBuffer();
        while ( (line=reader.readLine()) != null ) output.append(line);
        return (output.length() > 0);
    }

    /**
     * Sets the command.
     *
     * @param cmd The command.
     */
    public void setCommand(int cmd)
    {
        _command = cmd;
    }

    public CommandProcessorDocument createDocument()
    {
        CommandProcessorDocument doc = super.createDocument();
        ProcessorArguments args = doc.getProcessorArguments();
        XmlTag tag = new XmlTag("command", args);
        String cmd = null;
        switch (_command)
        {
            case START:
                cmd = "start";
                break;
            case STOP:
                cmd = "stop";
                break;
            case RESTART:
                cmd = "restart";
                break;
            default:
                cmd = "check";
        }
        tag.addBodyContent(cmd);
        return doc;
    }
}