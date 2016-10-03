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

import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncResult;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.components.http.IHttpClientResponse;

public class ReportResponseHandler implements IAsyncResultHandler<IHttpClientResponse> {
	private final static IAsyncResult<Void> RESULT_OK = AsyncResultImpl.create((Void) null);
	private final static SAXParserFactory factory = SAXParserFactory.newInstance();
	
	private final IAsyncResultHandler<Void> errorHandler;

	public ReportResponseHandler(IAsyncResultHandler<Void> errorHandler) {
		this.errorHandler = errorHandler;
	}

	@Override
	public void handle(IAsyncResult<IHttpClientResponse> result) {
		if (result.isSuccess()) {
			IHttpClientResponse postResponse = result.getResult();
			if (postResponse.getResponseCode() == 200 || postResponse.getResponseCode() == 202) {
				errorHandler.handle(RESULT_OK);
			} else {
				try {
					ReportResponse reportResponse = parseReport(postResponse.getBody());
					RuntimeException re = new RuntimeException(String.format("Backend report failed. Code: %s, Message: %s",
							reportResponse.getErrorCode(), reportResponse.getErrorMessage()));
					errorHandler.handle(AsyncResultImpl.create(re));
				} catch (Exception e) {
					RuntimeException re = new RuntimeException("Unable to parse report response", e);
					errorHandler.handle(AsyncResultImpl.create(re));
				}				
			}
		}
	}
	
	private static ReportResponse parseReport(String report) {
		try {
			SAXParser saxParser = factory.newSAXParser();
			ReturnCodeListener listener = new ReturnCodeListener();
			saxParser.parse(new InputSource(new StringReader(report)), listener);
			return listener;
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private interface ReportResponse {
		String getErrorCode();
		String getErrorMessage();
	}
	
	private static final class ReturnCodeListener extends DefaultHandler implements ReportResponse {
		private boolean qErrorMessage;
		private String returnCode;
		private String errorMessage;
		
		@Override
		public void startElement(String uri, String localName,
                String qName, Attributes attributes) throws SAXException {
			if ("error".equalsIgnoreCase(qName)) {
				qErrorMessage = true;
				returnCode = attributes.getValue("code");
			}
		}
				
		@Override
	    public void characters(char ch[], int start, int length)
	            throws SAXException {
			if (qErrorMessage) {
				errorMessage = new String(ch, start, length);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) 
				throws SAXException {
			qErrorMessage = false;
		}
		
		public String getErrorCode() {
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
		System.out.println(parseReport("<error code=\"123\">application with id=\"foo\" was not found</error>"));
	}
}
