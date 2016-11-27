import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

/**
 * Command line based Information Retrieval System Parses and indexes
 * .htm,.html,.txt files Uses Porter Stemmer Stop Word elimination Uses
 * inverted index Delivers a ranked list of files that fulfill the query
 */
public class TinyDancer {

	private String catalog; // directory to be indexed
	private ArrayList<File> files = new ArrayList<File>(); // list of added
															// files
	private EnglishAnalyzer analyzer = new EnglishAnalyzer(); // standard
																// tokenizer,
																// uses 
																// Stemmer with
																// stopword
																// elimination
	private IndexWriter writer; // creates index of files
	private ArrayList<Document> indfiles = new ArrayList<Document>(); // list of
																		// indexed
																		// and
																		// parsed
																		// files
	private Directory dir; // index folder
	private int verbose = 0; // log level for debug purposes

	/**
	 * Constructor 
	 * 1. Initialize this.catalog. 
	 * 2. create new RAM-folder 
	 * 3. Create configuration for indexer based on analyzer. 
	 * 4. Initialize the indexer this.writer.
	 * 
	 * @param directory
	 *            to be indexed
	 * @throws IOException
	 */
	public TinyDancer(String directory) throws IOException {
		this.catalog = directory;
		this.dir=new RAMDirectory();
		IndexWriterConfig config = new IndexWriterConfig(this.analyzer);
		this.writer = new IndexWriter(dir, config);
	}

	/**
	 * Adds all of the files to be indexed to the files list 1. Checks if file
	 * exists. 2. If file is a directory, adds recursive all files in the
	 * directory 3. .htm, .html, .txt files are added to the list.
	 * 
	 * @param file
	 *            to be checked
	 */
	private void addFiles(File file) {
		if (!file.exists()) {
			System.out.println("ERROR| The file: " + file.getName()
					+ " doesn't exist\n");
		} else if (file.isDirectory()) {
			if (this.verbose > 0)
				System.out.println("INFO| Entering the directory: "
						+ file.getAbsolutePath());
			for (File f : file.listFiles())
				addFiles(f);
		} else {
			if (file.getName().endsWith(".htm")
					|| file.getName().endsWith(".html")
					|| file.getName().endsWith(".txt")) {
				if (this.verbose > 0)
					System.out.println("INFO| The file: " + file.getName()
							+ " is added to the file list.");
				this.files.add(file);
			} else {
				if (this.verbose > 0)
					System.out
							.println("INFO| The file: "
									+ file.getName()
									+ " is skipped. The files of such format cannot be indexed");
			}
		}
	}

	/**
	 * Indexes all the files 1. Adds all the files from the directory to the
	 * list. 2. Parses all the files from this.files into this.indfiles
	 * 
	 * @throws IOException
	 */
	public void createIndex() throws IOException {
		this.addFiles(new File(this.catalog));
		for (File f : this.files) {
			FileReader fr = null;
			try {
				if (this.verbose > 0) {
					System.out.println("INFO| Parsing file: "
							+ f.getAbsolutePath());
				}
				Document d = new Document();
				fr = new FileReader(f);
				if (f.getName().endsWith(".txt")) {								//.txt search for content
					d.add(new TextField("contents", fr));
					d.add(new StringField("path", f.getAbsolutePath(),
							Field.Store.YES));
					d.add(new StringField("modified", DateTools.timeToString(
							f.lastModified(), DateTools.Resolution.SECOND),
							Field.Store.YES));
				} else {														//TODO: .html search for body, content, title
					d.add(new TextField("contents", fr));
					d.add(new StringField("path", f.getAbsolutePath(),
							Field.Store.YES));
					d.add(new TextField("modified", DateTools.timeToString(
							f.lastModified(), DateTools.Resolution.SECOND),
							Field.Store.YES));
				}
				this.indfiles.add(d);
				this.writer.addDocument(d);
			} catch (Exception e) {
				if (this.verbose > 0) {
					System.out.println("ERROR| Parsing file: "
							+ f.getAbsolutePath() + " failed");
				}
			} finally {
				fr.close();
			}
		}
		this.writer.close();
	}

	/**
	 * Creates formatted output of the files stored in this.files 1. Number 2.
	 * Full name 3. Last modified date
	 * 
	 * @throws ParseException
	 */
	public void listFiles() throws ParseException {
		int i = 1;
		System.out.println("\nIndexed files:");
		System.out.format("%-4s | %-100s | %-20s\n", "ID", "Full file name",
				"Last modified date");
		System.out
				.println("------------------------------------------------------------------"
						+ "---------------------------------------------------------------");
		for (Document d : this.indfiles) {
			SimpleDateFormat orig = new SimpleDateFormat("yyyyMMddHHmmss");
			SimpleDateFormat sf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			System.out.format("%-4d | %-100s | %-20s\n", (i++), d.get("path"),
					sf.format(orig.parse(d.get("modified"))));
		}
	}

	/**
	 * 1. open index to read
	 * 2. create searcher to look up in index
	 * 3. parse query
	 * 4. store results in hits (sorted by score)
	 * 5. formated output: results
	 * 
	 * @param str
	 * @throws IOException
	 * @throws org.apache.lucene.queryparser.classic.ParseException
	 * @throws ParseException
	 */
	public void searchFor(String str) throws IOException,
			org.apache.lucene.queryparser.classic.ParseException,
			ParseException {
		IndexReader reader = DirectoryReader.open(this.dir);
		IndexSearcher searcher = new IndexSearcher(reader);
		Query query = new QueryParser("contents", analyzer).parse(str);

		TopDocs col = searcher.search(query, 10);
		ScoreDoc[] hits = col.scoreDocs;

		SimpleDateFormat orig = new SimpleDateFormat("yyyyMMddHHmmss");
		SimpleDateFormat sf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

		System.out.println("\nINFO| Found " + hits.length + " hits.");
		System.out.format("%-4s | %-100s | %-20s | %-10s\n", "ID",
				"Full file name", "Last modified date", "Score");
		System.out
				.println("------------------------------------------------------------------"
						+ "---------------------------------------------------------------"
						+ "------------");
		for (int i = 0; i < hits.length; ++i) {
			int docId = hits[i].doc;
			Document d = searcher.doc(docId);
			System.out.format("%-4d | %-100s | %-20s | %-10f\n", (i + 1),
					d.get("path"), sf.format(orig.parse(d.get("modified"))),
					hits[i].score);
		}
		reader.close();
	}

	/**
	 * 1. getting path
	 * 2. creates new TinyDancer (new IR - System)
	 * 3. creates index
	 * 4. output: indexed and parsed files
	 * 5. getting query from console (input)
	 * 6. search for query
	 * 7. return until input = 'q'
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		
		Scanner sc = new Scanner(System.in);
		String query;
		String dir = args[0];
		
		try {
			TinyDancer td = new TinyDancer(dir); // new irsystem
			td.createIndex(); // index files
			td.listFiles();
			
			System.out.println("Tip 'q' to quit \n Searching for: ");
			query = sc.next();
			while(!query.equals("q"))
			{
				td.searchFor(query);
				System.out.println("\n Tip 'q' to quit \n Searching for: ");
				query = sc.next();
			}
			
		} catch (IOException e) {
			System.out.println("ERROR| Could not index the directory: " + dir);
			e.printStackTrace();
		} catch (ParseException e) {
			System.out.println("ERROR| Could not parse the date");
			e.printStackTrace();
		} catch (org.apache.lucene.queryparser.classic.ParseException e) {
			System.out.println("ERROR| Could not parse the query");
			e.printStackTrace();
		}
	}

}
