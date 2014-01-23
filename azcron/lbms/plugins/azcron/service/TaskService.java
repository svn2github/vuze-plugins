package lbms.plugins.azcron.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * @author Damokles
 *
 */
public class TaskService {

	protected List<Task> tasks = new ArrayList<Task>();

	public TaskService () {

	}

	public TaskService createFromElement (Element e) {
		TaskService ts = new TaskService();
		ts.readFromElement(e);
		return ts;
	}

	public void readFromElement (Element e) {
		List<Element> elems = e.getChildren("Task");
		tasks.clear();
		for (Element ele:elems) {
			tasks.add(new Task(ele));
		}
	}

	public Element toElement() {
		Element e = new Element ("TaskService");
		for (int i=0;i<tasks.size();i++)
			e.addContent(tasks.get(i).toElement());
		return e;
	}

	public void addTask (Task t) {
		tasks.add(t);
	}

	public void removeTask (Task t) {
		tasks.remove(t);
	}

	public Task[] getTasks () {
		return tasks.toArray(Task.EMPTY_ARRAY);
	}

	public void saveToFile (File f) throws IOException{
		OutputStream os = null;
		try {
			os = new FileOutputStream (f);
			if (f.toString().endsWith(".gz")) {
				os = new GZIPOutputStream(os);
			}
			saveToStream(os);
		} finally {
			if (os != null) os.close();
		}
	}

	public void saveToStream (OutputStream os) throws IOException {
		Document doc = new Document ();
		doc.addContent(toElement());
		new XMLOutputter(Format.getPrettyFormat()).output(doc, System.out);
		new XMLOutputter(Format.getPrettyFormat()).output(doc, os);
	}

	public void loadFromStream (InputStream is) throws IOException {
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(is);
			readFromDoc(doc);
		} catch (JDOMException e) {
			e.printStackTrace();
		}
	}

	public void loadFromFile (File f) throws IOException {
		InputStream is = null;
		try {
			is = new FileInputStream (f);
			if (f.toString().endsWith(".gz")) {
				is = new GZIPInputStream(is);
			}
			loadFromStream(is);
		} finally {
			if (is != null) is.close();
		}
	}

	public void readFromDoc (Document doc) {
		readFromElement(doc.getRootElement());
	}
}
