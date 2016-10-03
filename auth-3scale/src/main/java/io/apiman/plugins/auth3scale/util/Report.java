package io.apiman.plugins.auth3scale.util;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Report {
	
	interface ReportResponse {
		int getReturnCode();
		String getErrorMessage();
	}

	static SAXParserFactory factory = SAXParserFactory.newInstance();
	
	public static ReportResponse reportResponseCode(String report) {
		try {
			SAXParser saxParser = factory.newSAXParser();
			ReturnCodeListener listener = new ReturnCodeListener();
			saxParser.parse(new InputSource(new StringReader(report)), listener);
			return listener;
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static final class ReturnCodeListener extends DefaultHandler implements ReportResponse {
		private boolean qCode = false;
		private int returnCode = -1;
		
		private boolean qErrorMessage;
		private StringBuilder errorMessage;
		
		@Override
		public void startElement(String uri, String localName,
                String qName, Attributes attributes) throws SAXException {
			if ("code".equalsIgnoreCase(qName)) {
				qCode = true;
			}
			
			if ("error".equalsIgnoreCase(qName)) {
				qErrorMessage = true;
				errorMessage = new StringBuilder();
			}
		}
				
		@Override
	    public void characters(char ch[], int start, int length)
	            throws SAXException {
			if (qCode) {
				
				//Integer.valueOf
				returnCode = ch[0];
			}
			
			if (qErrorMessage) {
				System.out.println("ch = " + ch[1]);
				errorMessage.append(ch);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) 
				throws SAXException {
			qCode = false;
			qErrorMessage = false;
		}
		
		public int getReturnCode() {
			return returnCode;
		}
		
		public String getErrorMessage() {
			return errorMessage.toString();
		}

		@Override
		public String toString() {
			return "ReturnCodeListener [returnCode=" + returnCode + ", errorMessage=" + errorMessage + "]";
		}

	}
	
	public static void main(String... args) {
		System.out.println(reportResponseCode("<error code=\"123\">application with id=\"foo\" was not found</error>"));
	}
}
