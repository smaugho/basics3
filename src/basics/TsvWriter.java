package basics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;

import javatools.util.FileUtils;

/**
 * YAGO2s - TsvWriter
 * 
 * Writes facts to TSV files
 * 
 * @author Fabian M. Suchanek
 *
 */
public class TsvWriter implements FactWriter {

	protected Writer out;
	
	@Override
	public void close() throws IOException {
	out.close();	
	}

	@Override
	public void write(Fact f) throws IOException {
		out.write(f.toTsvLine());
	}

	public TsvWriter(File f) throws FileNotFoundException {
		out=FileUtils.getBufferedUTF8Writer(f);
	}
}