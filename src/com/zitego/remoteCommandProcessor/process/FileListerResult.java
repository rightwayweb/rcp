package com.zitego.remoteCommandProcessor.process;

import com.zitego.remoteCommandProcessor.CommandProcessorResult;
import com.zitego.markup.xml.XmlTag;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * An extension of the command processor result to contain directory listing results.
 *
 * @author John Glorioso
 * @version $Id: FileListerResult.java,v 1.2 2010/11/09 02:34:36 jglorioso Exp $
 */
public class FileListerResult extends CommandProcessorResult
{
    private Hashtable<String, Vector<File>> _fileListings;

    /**
     * Creates a new FileLister result.
     */
    public FileListerResult()
    {
        super(SUCCESS);
        _fileListings = new Hashtable<String, Vector<File>>();
    }

    /**
     * Creates a new FileLister result given the result type.
     */
    public FileListerResult(int type)
    {
        this();
        setType(type);
    }
    /**
     * Adds a file to the listing of the given directory.
     *
     * @param dir The directory.
     * @param file The file.
     */
    public void addResult(String dir, String file, Date lastModified, long size)
    {
        if (dir == null || file == null) return;

        if (_fileListings.get(dir) == null) _fileListings.put( dir, new Vector<File>() );
        _fileListings.get(dir).add( new File(file, lastModified, size) );
    }

    public Vector<File> getFiles(String dir)
    {
        if (dir == null) return null;
        else return _fileListings.get(dir);
    }

    protected XmlTag getDetails()
    {
        XmlTag ret = new XmlTag("listing");
        for (Enumeration<String> e=_fileListings.keys(); e.hasMoreElements();)
        {
            String path = e.nextElement();
            XmlTag dir = new XmlTag("directory", ret);
            dir.setAttribute("path", path);
            Vector<File> files = _fileListings.get(path);
            int size = files.size();
            for (int i=0; i<size; i++)
            {
                XmlTag file = new XmlTag("file", dir);
                file.setAttribute( "lastModified", String.valueOf(files.get(i).lastModified.getTime()) );
                file.setAttribute( "size", String.valueOf(files.get(i).size) );
                file.addBodyContent(files.get(i).path);
            }
        }
        return ret;
    }

    public void deserialize(XmlTag tag)
    {
        if (tag == null) return;

        XmlTag listing = tag.getFirstOccurrenceOf("listing");
        if (listing != null)
        {
            Vector<XmlTag> directories = (Vector<XmlTag>)listing.getChildrenWithName("directory");
            int size = directories.size();
            for (int i=0; i<size; i++)
            {
                Vector<XmlTag> files = (Vector<XmlTag>)directories.get(i).getChildrenWithName("file");
                int size2 = files.size();
                for (int j=0; j<size2; j++)
                {
                    addResult
                    (
                        directories.get(i).getTagAttribute("path"),
                        files.get(j).getValue(),
                        new Date( Long.parseLong(files.get(j).getTagAttribute("lastModified")) ),
                        Long.parseLong( files.get(j).getTagAttribute("size") )
                    );
                }
            }
        }
    }

    public class File implements Comparable
    {
        public String path;
        public long size;
        public Date lastModified;

        public File(String path, Date lastModified, long size)
        {
            this.path = path;
            this.lastModified = lastModified;
            this.size = size;
        }

        public int compareTo(Object obj)
        {
            if (obj instanceof String) return this.path.compareTo( (String)obj );
            else if (obj instanceof File) return this.path.compareTo( ((File)obj).path );
            else return 1;
        }
    }
}