/**
* OWASP Benchmark Project
*
* This file is part of the Open Web Application Security Project (OWASP)
* Benchmark Project For details, please see
* <a href="https://www.owasp.org/index.php/Benchmark">https://www.owasp.org/index.php/Benchmark</a>.
*
* The OWASP Benchmark is free software: you can redistribute it and/or modify it under the terms
* of the GNU General Public License as published by the Free Software Foundation, version 2.
*
* The OWASP Benchmark is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
* even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details
*
* @author Heinrich Rust
* @created 2016-02-16
*/

package org.owasp.benchmark.score.parsers;

import java.io.File;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.owasp.benchmark.score.BenchmarkScore;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XanitizerReader extends Reader {

	public XanitizerReader() {
	}

	public TestResults parse(final File f) throws Exception {
		final TestResults tr = new TestResults("Xanitizer", false, TestResults.ToolType.SAST);
		tr.setTime(f);

		/*
		 * Create a SAX handler that collects and registers the needed data from
		 * the findings file.
		 */
		final DefaultHandler handler = new DefaultHandler() {

			private final StringBuilder m_CollectedCharacters = new StringBuilder();

			private String m_ProblemTypeId;
			private int m_CWE = -1;
			private String m_Class;
			private String m_Classification;

			@Override
			public void startElement(final String uri, final String localName, final String qName,
					final Attributes attributes) throws SAXException {

				m_CollectedCharacters.setLength(0);

				switch (qName) {
				case "XanitizerFindingsList":

					String version = attributes.getValue("xanitizerVersionShort");
					tr.setToolVersion(version);

					break;

				}
			}

			@Override
			public void endElement(final String uri, final String localName, final String qName)
					throws SAXException {
				switch (qName) {

				case "problemTypeId":
					m_ProblemTypeId = m_CollectedCharacters.toString();
					break;

				case "class":
					m_Class = m_CollectedCharacters.toString();
					break;

				case "classification":
					m_Classification = m_CollectedCharacters.toString();
					break;

				case "cweNumber":
					// remove leading "CWE-" and thousands delimiter
					try {
						m_CWE = Integer.parseInt(m_CollectedCharacters.toString().substring(4).replace(".", "").replace(",", ""));
					} catch (NumberFormatException e) {
						m_CWE = -1;
					}
					break;

				case "finding":
					// Finishing a finding.

					// Defensiveness: This condition should always be true.
					if (m_ProblemTypeId != null && m_Class != null && m_Classification != null && m_CWE > -1) {

						// Skip informational findings.
						if (!m_Classification.equals("Information")) {

							// Skip findings for non-BenchmarkTest classes.
							if (m_Class.startsWith(BenchmarkScore.BENCHMARKTESTNAME)) {

								final String testNumberAsString = m_Class
										.substring(BenchmarkScore.BENCHMARKTESTNAME.length());

								int testCaseNumber;
								try {
									testCaseNumber = Integer.parseInt(testNumberAsString);
								} catch (final NumberFormatException ex) {
									// Inner classes can lead to this.
									testCaseNumber = -1;
								}

								if (testCaseNumber >= 0) {
									final TestCaseResult tcr = new TestCaseResult();

									tcr.setNumber(testCaseNumber);
									tcr.setCategory(m_ProblemTypeId);
									tcr.setCWE(m_CWE);

									tr.put(tcr);
								}
							}
						}
					}

					m_ProblemTypeId = null;
					m_CWE = -1;
					m_Class = null;
					m_Classification = null;
					break;
				}

				m_CollectedCharacters.setLength(0);
			}

			@Override
			public void characters(final char ch[], final int start, final int length)
					throws SAXException {
				m_CollectedCharacters.append(ch, start, length);
			}
		};

		final SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
		final SAXParser saxParser = factory.newSAXParser();

		saxParser.parse(f, handler);

		return tr;
	}

}

