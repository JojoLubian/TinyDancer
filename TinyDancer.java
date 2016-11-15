package irsystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

/**
 * Command line based Information Retrieval System
 * Parses and indexes .htm,.html,.txt files
 * Uses Porter Stemmer w/ Stop Word elimination
 * Uses inverted index
 * Delivers a ranked list of files that fulfill the query
 */
public class TinyDancer {
	
	private String catalog;										//directory to be indexed
	private ArrayList<File> files = new ArrayList<File>();		//list of added files
	private IndexWriter writer;									//creates index of files
	private StandardAnalyzer analyzer = new StandardAnalyzer();	//standard tokenizer, uses Porter Stemmer with stopword elimination
	private int verbose = 0;									//log level for debug purposes
	
	/**
	 * Constructor
	 * 1. Initialize this.catalog.
	 * 2. Open the directory.
	 * 3. Create configuration for indexer based on analyzer.
	 * 4. Initialize the indexer this.writer.
	 * @param directory to be indexed
	 * @throws IOException
	 */
	public TinyDancer (String directory) throws IOException
	{
		this.catalog=directory;
		FSDirectory dir = FSDirectory.open(Paths.get(directory));
		IndexWriterConfig config = new IndexWriterConfig(this.analyzer);
		this.writer=new IndexWriter(dir,config);	
	}
	
	/**
	 * Adds all of the files to be indexed to the files list
	 * 1. Checks if file exists.
	 * 2. If file is a directory, adds recursive all files in the directory
	 * 3. .htm, .html, .txt files are added to the list.
	 * @param file to be checked
	 */
	private void addFiles(File file)
	{
		if (!file.exists())
		{
			System.out.println("ERROR| The file: "+file.getName()+" doesn't exist\n");
		}
		else if (file.isDirectory())
		{
			if (this.verbose>0)	System.out.println("INFO| Entering the directory: "+file.getAbsolutePath());
			for (File f: file.listFiles())
				addFiles(f);
		}
		else
		{
			if (file.getName().endsWith(".htm")
					||file.getName().endsWith(".html")
					||file.getName().endsWith(".txt"))
			{
				if (this.verbose > 0) System.out.println("INFO| The file: "+file.getName()+" is added to the file list.");
				this.files.add(file);
			}
			else
			{
				if (this.verbose>0) System.out.println("INFO| The file: "+file.getName()+" is skipped. The files of such format cannot be indexed");
			}
		}
	}
	
	/**
	 * TODO: Indexes all the files
	 * 1. Adds all the files from the directory to the list. 
	 */
	public void createIndex()
	{
		this.addFiles(new File(this.catalog));
	}
	
	
	/**
	 * Creates formatted output of the files stored in this.files
	 * 1. Number
	 * 2. Full name
	 * 3. Last modified date
	 */
	public void listFiles()
	{
		int i=1;
		System.out.println("Indexed files:");
		SimpleDateFormat sf=new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		for (File f: this.files)
			System.out.format("%4d | %-100s | %s\n",(i++),f.getAbsolutePath(),sf.format(f.lastModified()));
	}
	
	public static void main(String[] args) {
		
		//directory to index
		String dir="/home/urmikl18/Uni/WiSe1617/InformationRetrieval/testfolder";
//		String dir = args[0];
		try {
			TinyDancer td=new TinyDancer(dir);	//new irsystem
			td.createIndex();					//index files
			td.listFiles();						//show all indexed files
		} catch (IOException e) {
			System.out.println("ERROR| Could not index the directory: "+dir);
			e.printStackTrace();
		}
	}

}
