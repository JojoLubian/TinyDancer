package irsystem;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
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
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

/**
 * Command line based Information Retrieval System Parses and indexes
 * .htm,.html,.txt files Uses Porter Stemmer w/ Stop Word elimination Uses
 * inverted index Delivers a ranked list of files that fulfill the query
 */
public class TinyDancer {

	private String catalog; // directory to be indexed
	private ArrayList<File> files = new ArrayList<File>(); // list of added
															// files
	private StandardAnalyzer analyzer = new StandardAnalyzer(); // standard
																// tokenizer,
																// uses Porter
																// Stemmer with
																// stopword
																// elimination
	private IndexWriter writer; // creates index of files
	private ArrayList<Document> indfiles = new ArrayList<Document>(); // list of
																		// indexed
																		// and
																		// parsed
																		// files
	private Directory dir;
	private int verbose = 0; // log level for debug purposes

	/**
	 * Constructor 1. Initialize this.catalog. 2. Open the directory. 3. Create
	 * configuration for indexer based on analyzer. 4. Initialize the indexer
	 * this.writer.
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
				if (f.getName().endsWith(".txt")) {
					d.add(new TextField("contents", fr));
					d.add(new StringField("path", f.getAbsolutePath(),
							Field.Store.YES));
					d.add(new StringField("modified", DateTools.timeToString(
							f.lastModified(), DateTools.Resolution.SECOND),
							Field.Store.YES));
				} else {
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

	public static void main(String[] args) {

		// directory to index
		String dir = "/home/urmikl18/Uni/WiSe1617/InformationRetrieval/testfolder";
		String query = "river";
		// String dir = args[0];
		try {
			TinyDancer td = new TinyDancer(dir); // new irsystem
			td.createIndex(); // index files
			td.listFiles();
			td.searchFor(query);
			query = "holler";
			td.searchFor(query);
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
