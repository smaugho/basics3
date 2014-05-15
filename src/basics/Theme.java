package basics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javatools.administrative.Announce;

/**
 * Represents a theme
 * 
 * If a theme is assigned to a file, it is a file fact source, i.e., it can
 * iterate over the facts in the file. It can also write facts.
 * 
 * @author Fabian M. Suchanek
 * 
 */
public class Theme extends FactSource.FileFactSource implements
		Comparable<Theme> {

	/** Types of Theme */
	public enum ThemeGroup {
		TAXONOMY, SIMPLETAX, CORE, GEONAMES, META, MULTILINGUAL, LINK, OTHER, INTERNAL, WIKIPEDIA
	}

	/** Types of my theme */
	public final ThemeGroup themeGroup;

	/** Name of the theme */
	public final String name;

	/** Description of the theme */
	public final String description;

	/** maps the names to themes */
	protected static Map<String, Theme> name2theme = new TreeMap<>();

	public Theme(String name, String description) {
		this(name, description, name.startsWith("yago") ? ThemeGroup.OTHER
				: ThemeGroup.INTERNAL);
	}

	public Theme(String name, String description, ThemeGroup group) {
		super(null);
		this.name = name;
		this.description = description;
		if (name2theme.containsKey(name))
			throw new RuntimeException("Duplicate Theme: " + name);
		name2theme.put(this.name, this);
		themeGroup = group;
	}

	public static synchronized Theme getOrCreate(String name, String description, ThemeGroup group) {
		Theme result=name2theme.get(name);
		if(result==null) result=new Theme(name, description, group);
		return(result);
	}
	
	public Theme(String name, String language, String description,
			ThemeGroup group) {
		this(name + "_" + language, description, group);
	}

	public Theme(String name, String language, String description) {
		this(name + "_" + language, description);
	}

	/** Returns the language of a theme (or NULL) */
	public String language() {
		if (name.length() < 3 || name.charAt(name.length() - 3) != '_')
			return (null);
		return (name.substring(name.length() - 2));
	}

	/** TRUE for export-ready themes */
	public boolean isFinal() {
		return (name.startsWith("yago"));
	}

	@Override
	public int compareTo(Theme o) {
		return name.compareTo(o.name);
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof Theme) && ((Theme) obj).name.equals(name);
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	/** Returns all available themes */
	public static Collection<Theme> all() {
		return (name2theme.values());
	}

	/** Removes all known themes */
	public static void forgetAllFiles() {
		for(Theme t : all()) t.forgetFile();
	}

	/**
	 * Returns the file name of this theme in the given folder, in either TTL or
	 * TSV. Gives preference to TSV. Returns null if not found.
	 */
	public File findFileInFolder(File folder) {
		File tsv = new File(folder, name + ".tsv");
		File ttl = new File(folder, name + ".ttl");
		if (tsv.exists()) {
			if (ttl.exists())
				Announce.warning("Theme", this, "exists as ttl and tsv in",
						folder, ". Using tsv.");
			return (tsv);
		}
		if (ttl.exists())
			return (ttl);
		return (null);
	}

	/** Fact writer for writing the theme */
	protected FactWriter factWriter;
	/** Caching the theme */
	protected FactCollection cache = null;

	/** Opens the theme for writing */
	public synchronized void openForWritingInFolder(File folder, String header)
			throws Exception {
		if (factWriter != null)
			throw new RuntimeException("Already writing into Theme " + this);
		if (file != null)
			throw new RuntimeException("Theme " + this + " already written to "+file);
		file = new File(folder, name + ".tsv");
		factWriter = FactWriter.from(file, header);
		cache=null;
	}

	/** Closes the theme for writing */
	public void close() throws IOException {
		if (factWriter == null)
			throw new IOException("Theme " + this
					+ " cannot be closed because it was not open");
		factWriter.close();
		factWriter = null;
	}

	/** Assigns the theme to a file (to use data that is already there) */
	public synchronized Theme assignToFolder(File folder) throws IOException {
		File f = findFileInFolder(folder);
		if (f == null)
			throw new FileNotFoundException("Cannot find theme " + this
					+ " in " + folder);
		if (file != null) {
			if (file.equals(f))
				return (this);
			else
				throw new IOException("Theme " + this
						+ " is already assigned to file "+file+", cannot assign it to "+f);
		}
		file = f;
		cache=null;
		return (this);
	}

	/** Writes a fact */
	public void write(Fact f) throws IOException {
		factWriter.write(f);
	}

	/** True if the facts can be read from this source */
	public boolean isAvailableForReading() {
		return file != null && factWriter == null;
	}

	/** Forgets the file */
	public void forgetFile() {
		if (factWriter != null)
			throw new RuntimeException(this
					+ " cannot forget a file while writing to it: " + this.file);
		file = null;
		cache=null;
	}

	/** Returns the file of this theme (or null) */
	public File file() {
		return file;
	}

	@Override
	public Iterator<Fact> iterator() {
		if (file == null)
			throw new RuntimeException("Theme " + this
					+ " has not yet been assigned to a file.\nMaybe the theme was not declared as an input to an extractor?");
		if (factWriter != null)
			throw new RuntimeException("Theme " + this
					+ " is currently being written");
		return (super.iterator());
	}

	/** returns the cache, or creates a cache */
	public FactCollection factCollection() throws IOException {
		if (factWriter != null)
			throw new IOException("Theme " + this
					+ " is currently being written");
		if (file == null)
			throw new IOException("Theme " + this
					+ " has not yet been assigned to a file.\n"
					+ "Maybe it was not declared as input to an extractor?");
		if (cache == null)
			cache = new FactCollection(file, true);
		return (cache);
	}

	/** TRUE if is cached*/
	public boolean isCached() {
		return(cache!=null);
	}
	/** Removes the cache */
	public void killCache() {
		cache = null;
	}

}
