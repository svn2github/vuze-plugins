package lbms.plugins.azcron.actions;

import java.io.InvalidObjectException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Element;


/**
 * @author Damokles
 *
 */
public class Parameter {

	private Pattern datePattern = Pattern.compile("%(.+?)%");

	private String name;
	private String value;
	private Type type;

	private Parameter (Element e) {
		this.name = e.getAttributeValue("name");
		this.value = e.getAttributeValue("value");
		this.type = Type.valueOf(e.getAttributeValue("type"));
	}

	public Parameter(String name, String value, Type type) {
		super();
		this.name = name;
		this.value = value;
		this.type = type;
	}

	public Parameter (Parameter c) {
		name = c.name;
		value = c.value;
		type = c.type;
	}

	public Element toElement() {
		Element e = new Element ("Parameter");
		e.setAttribute("name", name);
		e.setAttribute("value", value);
		e.setAttribute("type", type.name());
		return e;
	}

	public static Parameter createFromElement (Element e) throws ClassCastException {
		if (e.getName().equalsIgnoreCase("Parameter")) {
			return new Parameter(e);
		} else throw new ClassCastException();
	}

	public enum Type {
		Boolean, Integer, Long, Float, Double, String, ParsedString;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return type.toString()+" "+name+" = "+value;
	}

	public Object getAsObject () throws InvalidObjectException {
		try {
			if (type == Type.Boolean) {
				return Boolean.valueOf(value);
			} else if (type == Type.Integer) {
				return Integer.valueOf(value);
			} else if (type == Type.Long) {
				return Long.valueOf(value);
			} else if (type == Type.Float) {
				return Float.valueOf(value);
			} else if (type == Type.Double) {
				return Double.valueOf(value);
			} else if (type == Type.String) {
				return value;
			} else if (type == Type.ParsedString) {
				return parseString(value);
			}
		} catch (NumberFormatException e) {
			new InvalidObjectException("NumberFormatException: "+e.getMessage());
		}
		throw new InvalidObjectException("Invalid Object Type");
	}

	private String parseString(String input) {
		System.out.println("Parsing String: "+input);
		Matcher m = datePattern.matcher(input);
		Date d = new Date();
		while (m.find()) {
			SimpleDateFormat sdf = new SimpleDateFormat(m.group(1));
			input = m.replaceFirst(sdf.format(d));
			m = datePattern.matcher(input);
		}
		System.out.println("Parsed String: "+input);
		return input;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
