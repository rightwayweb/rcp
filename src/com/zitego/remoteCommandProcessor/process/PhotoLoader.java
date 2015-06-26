package com.zitego.remoteCommandProcessor.process;

import com.zitego.remoteCommandProcessor.CommandProcessor;
import com.zitego.remoteCommandProcessor.ProcessorArguments;
import com.zitego.remoteCommandProcessor.CommandProcessorResult;
import com.zitego.remoteCommandProcessor.CommandProcessorException;
import com.zitego.markup.xml.XmlTag;
import com.zitego.filemanager.explorer.Explorer;
import com.zitego.filemanager.FileListing;
import com.zitego.filemanager.FileSystemObject;
import com.zitego.filemanager.Directory;
import com.zitego.sql.DBHandle;
import com.zitego.sql.DBHandleFactory;
import com.zitego.sql.DBConfig;
import com.zitego.util.ImageUtils;
import java.io.IOException;
import java.io.File;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.util.Vector;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Types;

/**
 * This class handles creating photograph data entities. The processor arguments should contain at least
 * one directory argument to load photos from. The xml should look as follows:<br>
 * <xmp>
 * <processor-arguments>
 *  <db_config>
 *   <site_id>4</site_id>
 *   <host>localhost</host>
 *   <username>someone</username>
 *   <password>somepass</password>
 *  </db_config>
 *  <debug>true</debug> (default is false)
 *  <home_dir>/home/httpd/domains/wave/wave_4</home_dir>
 *  <create_thumbnails>true</create_thumbnails>
 *  <thumbnail_width>150</thumbnail_width> (default is 100)
 *  <include_subdirs>true</include_subdirs> (default is false)
 *  <category_id>16</category_id> (optional)
 *  <directory>images/photos</directory>
 *  <directory>images2/photos2</directory>
 *  ...
 * </processor-arguments>
 * </xmp>
 *
 * Note: FileCopier supports copying directories.
 *
 * @author John Glorioso
 * @version $Id: PhotoLoader.java,v 1.1 2008/04/14 20:21:43 jglorioso Exp $
 */
public class PhotoLoader extends CommandProcessor
{
    private static Hashtable ALLOWED_TYPES = new Hashtable();
    static
    {
        ALLOWED_TYPES.put("jpg", "1");
        ALLOWED_TYPES.put("gif", "1");
        ALLOWED_TYPES.put("png", "1");
    }
    private DBHandle _db;
    private Explorer _homeDir;
    private boolean _createThumbs = false;
    private int _thumbWidth = 100;
    private boolean _loadSubDir = false;
    private long _categoryId = -1L;
    private Vector _directories = new Vector();
    private boolean _debug = false;

    public static void main(String[] a) throws Exception
    {
        PhotoLoader loader = new PhotoLoader();
        ProcessorArguments args = new ProcessorArguments();
        StringBuffer xml = new StringBuffer();
        java.io.BufferedReader in = new java.io.BufferedReader( new java.io.FileReader(a[0]) );
        String line = null;
        while ( (line=in.readLine()) != null )
        {
            xml.append(line).append("\r\n");
        }
        args.parse(xml.toString(), com.zitego.format.FormatType.XML);
        loader.init(args);
        CommandProcessorResult result = loader.execute();
        System.out.println( "Result: " + (result.getType() == result.SUCCESS ? "SUCCESS" : "FAILURE") );
        if (result.getType() != result.SUCCESS ) System.out.println( "        Reason: "+result.getReason()+"\nTrace: "+result.getStackTrace() );
    }

    /**
     * The processor arguments must include the
     * database configuration including host, site, username, and password.
     *
     * In addition, the following arguments are needed:
     * <ul>
     *  <li>home_dir - The home directory for the wave site.
     *  <li>create_thumbnails - A true/false flag on whether to create thumbnails.
     *  <li>thumbnail_width - The thumbnail width. 100 pixels is the default.
     *  <li>include_subdirs - A true/false flag on whether to include sub directories.
     *  <li>category_id - The photo category.
     * </ul>
     *
     * @param args The processor arguments.
     */
    public void init(ProcessorArguments args) throws CommandProcessorException
    {
        XmlTag cfg = args.getFirstOccurrenceOf("db_config");
        if (cfg == null) throw new CommandProcessorException("<db_config> is required");
        String id = cfg.getChildValue("site_id");
        if (id == null) throw new CommandProcessorException("<site_id> is required in db_config element");
        String host = cfg.getChildValue("host");
        if (host == null) throw new CommandProcessorException("<host> is required in db_config element");
        String username = cfg.getChildValue("username");
        if (username == null) throw new CommandProcessorException("<username> is required in db_config element");
        String password = cfg.getChildValue("password");
        if (password == null) throw new CommandProcessorException("<password> is required in db_config element");
        try
        {
            _db = DBHandleFactory.getDBHandle
            (
                new DBConfig
                (
                    "jdbc:mysql://" + host + ":3306/wave_" + id,
                    (java.sql.Driver)Class.forName("com.mysql.jdbc.Driver").newInstance(),
                    username, password, DBConfig.MYSQL
                )
            );
        }
        catch (Exception e)
        {
            throw new CommandProcessorException("Could not create db_config", e);
        }

        String val = args.getChildValue("debug");
        if (val != null) _debug = new Boolean(val).booleanValue();

        val = args.getChildValue("home_dir");
        if (val == null) throw new CommandProcessorException("<home_dir> is required");
        try
        {
            _homeDir = new Explorer(val);
        }
        catch (IOException ioe)
        {
            throw new CommandProcessorException("Could not create explorer with home directory: "+val);
        }

        val = args.getChildValue("create_thumbnails");
        if (val != null) _createThumbs = new Boolean(val).booleanValue();
        val = args.getChildValue("thumbnail_width");
        if (val != null)
        {
            try
            {
                _thumbWidth = Integer.parseInt(val);
            }
            catch (NumberFormatException nfe)
            {
                throw new CommandProcessorException("Invalid value for <thumbnail_width>: "+val);
            }
        }

        val = args.getChildValue("include_subdirs");
        if (val != null) _loadSubDir = new Boolean(val).booleanValue();

        val = args.getChildValue("category_id");
        if (val != null)
        {
            try
            {
                _categoryId = Long.parseLong(val);
            }
            catch (NumberFormatException nfe)
            {
                throw new CommandProcessorException("Invalid value for <category_id>: "+val);
            }
        }

        Vector dirs = args.getChildrenWithName("directory");
        int size = dirs.size();
        for (int i=0; i<size; i++)
        {
            XmlTag dir = (XmlTag)dirs.get(i);
            _directories.add( dir.getValue() );
        }

        if (_directories.size() == 0) throw new CommandProcessorException("At least one <directory> tag is required");

        if (_debug)
        {
            System.out.println("Finished init:");
            System.out.println( "_db = " + _db.getConfig() );
            System.out.println( "_homeDir = " + _homeDir );
            System.out.println( "_createThumbs = " + _createThumbs );
            System.out.println( "_thumbWidth = " + _thumbWidth );
            System.out.println( "_loadSubDir = " + _loadSubDir );
            System.out.println( "_categoryId = " + _categoryId );
            System.out.print( "_directories = " );
            for (int i=0; i<_directories.size(); i++)
            {
                System.out.print( (i > 0 ? ", " : "") + _directories.get(i) );
            }
            System.out.println("");
        }
    }

    public CommandProcessorResult execute()
    {
        CommandProcessorResult ret = null;
        try
        {
            int size = _directories.size();
            for (int i=0; i<size; i++)
            {
                String dir = (String)_directories.get(i);
                _homeDir.selectDirectory(dir, false);
                loadPhotos();
            }
            ret = new CommandProcessorResult(CommandProcessorResult.SUCCESS);
        }
        catch (Exception e)
        {
            ret = new CommandProcessorResult(CommandProcessorResult.FAILURE);
            ret.setReason( e.getMessage() );
            ret.setStackTrace(e);
        }
        return ret;
    }

    private void loadPhotos() throws SQLException, IOException
    {
        if (_debug) System.out.println( "Loading photos in "+_homeDir.getFileListing().getParentDirectory().getRootPath() );
        _db.connect();
        try
        {
            FileListing listing = _homeDir.getFileListing();
            int size = listing.size();
            for (int i=0; i<size; i++)
            {
                FileSystemObject obj = (FileSystemObject)listing.get(i);
                if (obj instanceof Directory)
                {
                    if ( _loadSubDir && !obj.getRootPath().endsWith("/thumbs") )
                    {
                        String cur = _homeDir.getRootPath();
                        _homeDir.selectDirectory(obj.getRootPath(), false);
                        loadPhotos();
                        _homeDir.selectDirectory(cur, false);
                    }
                    else if (_debug) System.out.println( "Skipping directory " + obj.getRootPath() );
                }
                else
                {
                    int index = obj.getRootPath().lastIndexOf(".");
                    String ext = obj.getRootPath().substring(index+1);
                    if (ALLOWED_TYPES.get(ext.toLowerCase()) == null)
                    {
                        if (_debug) System.out.println( "Skipping file " + obj.getRootPath() );
                        continue;
                    }
                    //See if we are supposed to create thumbnails or not.
                    String thumbPath = null;
                    if (_createThumbs)
                    {
                        //See if the thumbs directory exists
                        String imgDir = obj.getAbsolutePath();
                        imgDir = imgDir.substring( 0, imgDir.lastIndexOf("/") );
                        File f = new File(imgDir+"/thumbs");
                        if ( !f.exists() ) f.mkdir();
                        imgDir = obj.getRootPath().substring( 0, obj.getRootPath().lastIndexOf("/") );
                        thumbPath = imgDir + "/thumbs/" + obj.getName();
                        if (_debug) System.out.println( "Creating thumbnail "+thumbPath+" for "+obj.getRootPath() );
                        ImageUtils.scaleImage(new File(obj.getAbsolutePath()), new File(f.getAbsolutePath()+"/"+obj.getName()), _thumbWidth);
                    }
                    loadPhoto( thumbPath, obj.getRootPath() );
                }
            }
        }
        finally
        {
            _db.disconnect();
        }
    }

    private void loadPhoto(String tn, String path) throws SQLException
    {
        StringBuffer sql = new StringBuffer()
            .append("INSERT INTO photo (caption, thumbnail_path, url_path, photo_category_id, order_id, creation_date) ")
            .append("SELECT ?, ?, ?, ?, num, now() ")
            .append("FROM (SELECT COUNT(*)+1 num ")
            .append(      "FROM photo ")
            .append(      "WHERE photo_category_id = ?) num_photos ")
            .append(     "LEFT JOIN photo ON (url_path = ? AND photo_category_id = ?) ")
            .append("WHERE photo_id IS NULL ")
            .append("LIMIT 1");
        PreparedStatement pst = _db.prepareStatement(sql);
        String caption = path;
        int index = caption.lastIndexOf("/");
        if (index != -1) caption = path.substring(index+1);
        pst.setString(1, caption);
        pst.setString(2, tn);
        pst.setString(3, path);
        if (_categoryId > 0) pst.setLong(4, _categoryId);
        else pst.setNull(4, Types.NUMERIC);
        if (_categoryId > 0) pst.setLong(5, _categoryId);
        else pst.setNull(5, Types.NUMERIC);
        pst.setString(6, path);
        if (_categoryId > 0) pst.setLong(7, _categoryId);
        else pst.setNull(7, Types.NUMERIC);
        int count = pst.executeUpdate();
        if (_debug)
        {
            if (count > 0) System.out.println( "Inserted "+count+" row for file: caption="+caption+", thumbnail_path="+tn+", url_path="+path);
            else System.out.println( "Row already existed in category for file: caption="+caption+", thumbnail_path="+tn+", url_path="+path);
        }
    }
}