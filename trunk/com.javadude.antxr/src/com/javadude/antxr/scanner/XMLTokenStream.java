/*******************************************************************************
 *  Copyright 2008 Scott Stanchfield.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * Contributors:
 *   Based on the ANTLR parser generator by Terence Parr, http://antlr.org
 *   Ric Klaren <klaren@cs.utwente.nl>
 *******************************************************************************/
package com.javadude.antxr.scanner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;

import com.javadude.antxr.CommonToken;
import com.javadude.antxr.Token;
import com.javadude.antxr.TokenStream;
import com.javadude.antxr.TokenStreamException;

import org.xml.sax.Attributes;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * An XML token stream. You can pass any SAX parser, with whatever configuration
 * you want for use as the scanner.
 */
public class XMLTokenStream implements TokenStream {
    private boolean[] startTag;
    private Map<String, Map<String, Integer>> namespaces = new HashMap<String, Map<String,Integer>>();
    private Map<String, Integer> tokens = new HashMap<String, Integer>();
    private BlockingQueue<Object> blockingQueue;
    private int pcdataNum;
    private StringBuffer currentCharacters = new StringBuffer();
    private int currentCharactersLine = -1;
    private int currentCharactersColumn = -1;
    private int endTagValue;
    private int otherTagValue = -1;

    /**
     * Create the xml token stream. This version does not gate the number of
     * tokens read by the SAX parser. <i>Note that this can cause the entire
     * XML to be read into memory!</i> If you have a small XML document to
     * parse, this is more efficient, but large XML documents can cause memory
     * problems. If you want to use a large XML file, call the other constructor
     * and pass it a maximumQueueSize and resumeQueueSize.
     * @param tokenNames An array of token names for your parser. You can get
     *                   this by passing YourParser._tokenNames, where YourParser
     *                   is an XML parser generated by ANTXR
     * @param namespaceMap  A map of namespace/prefix mappings. You can get this
     *                      by passing YourParser.getNamespaceMap(), where
     *                      YourParser is an XML parser generated by ANTXR
     * @param in The XML InputSource containing the XML to parse
     * @param parser The SAX Parser that you want to use to scan (and possibly
     *               validate) your XML
     * @param entityResolver An XML Entity resolver for the SAX parse (if needed), or null
     * @param dtdHandler and XML DTD Handler for theSAX parse (if needed), or null
     */
    public XMLTokenStream(String[] tokenNames, Map<String, String> namespaceMap, InputSource in, SAXParser parser, EntityResolver entityResolver, DTDHandler dtdHandler) {
        this(tokenNames, namespaceMap, in, parser, entityResolver, dtdHandler, -1, -1);
    }

    /**
     * Create the xml token stream. This version does not gate the number of
     * tokens read by the SAX parser. <i>Note that this can cause the entire
     * XML to be read into memory!</i> If you have a small XML document to
     * parse, this is more efficient, but large XML documents can cause memory
     * problems. If you want to use a large XML file, call the other constructor
     * and pass it a maximumQueueSize and resumeQueueSize.
     * @param tokenNames An array of token names for your parser. You can get
     *                   this by passing YourParser._tokenNames, where YourParser
     *                   is an XML parser generated by ANTXR
     * @param namespaceMap  A map of namespace/prefix mappings. You can get this
     *                      by passing YourParser.getNamespaceMap(), where
     *                      YourParser is an XML parser generated by ANTXR
     * @param in The XML InputSource containing the XML to parse
     * @param parser The SAX Parser that you want to use to scan (and possibly
     *               validate) your XML
     * @param entityResolver An XML Entity resolver for the SAX parse (if needed), or null
     * @param dtdHandler and XML DTD Handler for theSAX parse (if needed), or null
     * @param maximumQueueSize the maximum number of tokens you want to place
     *                         in the blocking queue ready for the ANTXR parser
     *                         to fetch. This will put the SAX parse on hold
     *                         until resumeQueue size is reached.
     * @param resumeQueueSize The number of buffered tokens at which you will
     *                        resume the SAX parse
     */
    public XMLTokenStream(String[] tokenNames, Map<String, String> namespaceMap, InputSource in, SAXParser parser, EntityResolver entityResolver, DTDHandler dtdHandler, int maximumQueueSize, int resumeQueueSize) {
        readTokens(tokenNames, namespaceMap);
        // TODO avoid NPE on following
        Integer tokenNum = tokens.get("PCDATA");
        if (tokenNum == null) {
            pcdataNum = -99;
        } else {
            pcdataNum = tokenNum.intValue();
        }
        blockingQueue = new BlockingQueue<Object>(maximumQueueSize,resumeQueueSize);
        parse(parser, in, entityResolver, dtdHandler);
    }

    /**
     * Set up the tokens to use when scanning
     * @param tokenNames The names of the tokens in the grammar
     * @param namespaceMap A mapping that includes prefixes
     */
    private void readTokens(String[] tokenNames, Map<String, String> namespaceMap) {
        startTag = new boolean[tokenNames.length];
        Pattern pattern = Pattern.compile("\"<((.*):)?(.*)>\"");
        for (int i = 0; i < tokenNames.length; i++) {
            String tokenName = tokenNames[i];
            Matcher matcher = pattern.matcher(tokenName);
            Integer integerValue = new Integer(i);
            if (matcher.matches()) {
                String namespace = matcher.group(2);
                String tag = matcher.group(3);
                if (namespace == null) {
                    namespace = namespaceMap.get("$DEFAULT");
                }
                addTag(namespace, tag, integerValue);
            }
            else {
                tokens.put(tokenName,integerValue);
                if ("XML_END_TAG".equals(tokenName)) {
                    endTagValue = integerValue.intValue();
                }
                if ("OTHER_TAG".equals(tokenName)) {
                    otherTagValue = integerValue.intValue();
                }
            }
        }
    }

    // TODO if only one namespace, optimize further (no hashmap lookup)
    /**
     * Get the numerical token number for an XML tag
     * @param namespace The tag's namespace
     * @param tag The tag name
     * @return The tag's token id
     */
    private Integer getTokenValue(String namespace, String tag) {
        return getTags(namespace).get(tag);
    }

    /**
     * Add an XML tag to our mapping
     * @param namespace The namespace/prefix map from the grammar
     * @param tag The xml tag to store
     * @param integerValue The integer value of the tag
     */
    private void addTag(String namespace, String tag, Integer integerValue) {
        if (namespace == null) {
            namespace = "";
        }

        getTags(namespace).put(tag, integerValue);
        startTag[integerValue.intValue()] = true;
    }

    /**
     * State whether the given token is an XML start tag
     * @param token the token to check
     * @return true if it's a start tag, false otherwise
     */
    public boolean isStartTag(Token token) {
        return startTag[token.getType()];
    }

    /**
     * Get all the tags defined in the given namespace
     * @param namespace The namespace to check
     * @return A map of tags to token ids
     */
    private Map<String, Integer> getTags(String namespace) {
        if (namespace == null) {
            namespace = "";
        }
        Map<String, Integer> tags = namespaces.get(namespace);
        if (tags == null) {
            tags = new HashMap<String, Integer>();
            namespaces.put(namespace, tags);
        }
        return tags;
    }

    /**
     * Start parsing the XML
     * @param parser The SAX parser to use
     * @param in The XML to parse
     * @param entityResolver The user-defined entity resolver (or null)
     * @param dtdHandler The user-defined DTD handler (or null)
     */
    private void parse(final SAXParser parser, final InputSource in, EntityResolver entityResolver, DTDHandler dtdHandler) {

        final ANTXRXMLHandler handler = new ANTXRXMLHandler(entityResolver, dtdHandler);

        Thread saxParseThread = new Thread("saxParserCreatingXMLTokens") {
            @Override
            public void run() {
                try {
                    parser.parse(in,handler);
                }
                catch (Throwable t) {
                    blockingQueue.enqueue(t); // stuff any exceptions in the queue
                }
            }
        };

        saxParseThread.setDaemon(true);
        saxParseThread.start();
    }

    /** {@inheritDoc} */
    public Token nextToken() throws TokenStreamException {
        try {
            Object o = blockingQueue.dequeue();
            if (o instanceof Throwable) {
                throw (Throwable)o;
            }
            return (Token)o;
        }
        catch (Throwable e) {
            throw new TokenStreamException("Error during XML parse", e);
        }
    }

    /**
     * The SAX handler that glues the SAX parser to our blocking queue.
     * This class grabs notifications of tags from the SAX parser, creates
     * ANTXR tokens from them, and stuffs the tokens in the blocking queue.
     * The nextToken method returns tokens off the queue when asked.
     *
     * If the caller passes in a DTD and/or entity resolver, we delegate to
     * them when appropriate during the SAX parse.
     */
    class ANTXRXMLHandler extends DefaultHandler {
        private Locator locator;
        private EntityResolver entityResolver;
        private DTDHandler dtdHandler;

        /**
         * Create the handler
         * @param entityResolver A user-defined entity resolver to delegate to
         * @param dtdHandler A user-defined dtd handler to delegate to
         */
        public ANTXRXMLHandler(EntityResolver entityResolver, DTDHandler dtdHandler) {
            this.entityResolver = entityResolver;
            this.dtdHandler = dtdHandler;
        }

        /** {@inheritDoc} */
        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        /** {@inheritDoc} */
        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            // collect all adjacent character chunks into a single PCDATA
            //   to return to the parser
            // if PCDATA isn't used in the parser, don't collect characters
            if (pcdataNum == -99) {
                return;
            }
            if (currentCharactersLine == -1) {
                currentCharactersLine = locator.getLineNumber();
                currentCharactersColumn = locator.getColumnNumber();
            }
            currentCharacters.append(ch, start, length);
        }

        /**
         * Finish our PCDATA and send it to the parser.
         */
        protected void finishCharacters() {
            // if PCDATA isn't used in the parser, don't collect characters
            if (pcdataNum == -99) {
                return;
            }
            int line = currentCharactersLine;
            int column = currentCharactersColumn;
            currentCharactersLine = -1;
            currentCharactersColumn = -1;
            String characters = currentCharacters.toString();
            currentCharacters.delete(0, currentCharacters.length());
            if ("".equals(characters.trim())) {
                return;
            }

            Token token = new CommonToken(pcdataNum,characters);
            token.setLine(line);
            token.setColumn(column);
            blockingQueue.enqueue(token);
        }

        /** {@inheritDoc} */
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            finishCharacters(); // if we were working on a PCDATA, send it!

            // queue an XML_END_TAG token
            Token token = new CommonToken(endTagValue,"");
            token.setLine(locator.getLineNumber());
            token.setColumn(locator.getColumnNumber());
            blockingQueue.enqueue(token);
        }

        /** {@inheritDoc} */
        @Override
        public void endDocument() throws SAXException {
            finishCharacters(); // if we were working on a PCDATA, send it!
            // queue an EOF_TOKEN
            CommonToken eofToken = new CommonToken(Token.EOF_TYPE,"");
            eofToken.setLine(locator.getLineNumber());
            eofToken.setColumn(locator.getColumnNumber());
            blockingQueue.enqueue(eofToken);
        }

        /** {@inheritDoc} */
        @Override
        public void error(SAXParseException e) throws SAXException {
            finishCharacters(); // if we were working on a PCDATA, send it!
            throw e;
        }

        /** {@inheritDoc} */
        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            finishCharacters(); // if we were working on a PCDATA, send it!
            throw e;
        }

        /** {@inheritDoc} */
        @Override
        public void warning(SAXParseException e) throws SAXException {
            finishCharacters(); // if we were working on a PCDATA, send it!
            throw e;
        }

        /** {@inheritDoc} */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            finishCharacters(); // if we were working on a PCDATA, send it!
            // queue a start tag token for the tag
            if ("".equals(localName)) {
                localName = qName;
            }
            blockingQueue.enqueue(createXMLToken(uri, localName, attributes));
        }

        /**
         * Create an XML token.
         * @param uri The namespace of the tag
         * @param localName The local name of the tag
         * @param attributes The tag attributes
         * @return An XMLToken
         * @throws SAXException If we have trouble accessing the SAX attributes
         */
        private XMLToken createXMLToken(String uri, String localName, Attributes attributes) throws SAXException {
            Integer id = getTokenValue(uri, localName);
            String name = "";
            if (uri != null && !"".equals(uri.trim())) {
                name += uri + ":";
            }
            name += localName;
            int tokenValue;
            if (id != null) {
                tokenValue = id.intValue();
            }
            else if (otherTagValue != -1) {
                tokenValue = otherTagValue;
            }
            else {
                throw new SAXException("Tag '" + name + "' not defined in parser grammar");
            }

            List<Attribute> attributeList;

            if (attributes == null || attributes.getLength() == 0) {
                attributeList = Collections.emptyList();
            } else {
                attributeList = new ArrayList<Attribute>(attributes.getLength());
                for (int i = 0; i < attributes.getLength(); i++) {
                    String localAttributeName = attributes.getLocalName(i);
                    if ("".equals(localAttributeName)) {
                        localAttributeName = attributes.getQName(i);
                    }
                    String namespace = attributes.getURI(i);
                    String value = attributes.getValue(i);
                    String type = attributes.getType(i);
                    Attribute attribute = new Attribute(namespace,localAttributeName,value,type);
                    attributeList.add(attribute);
                }
            }

            XMLToken token = new XMLToken(tokenValue, name, attributeList);
            token.setLine(locator.getLineNumber());
            token.setColumn(locator.getColumnNumber());
            return token;
        }

        /** {@inheritDoc} */
        @Override
        public void notationDecl(String name, String publicId, String systemId) throws SAXException {
            // If we have an explicit DTD handler, delegate to it
            if (dtdHandler != null) {
                dtdHandler.notationDecl(name, publicId, systemId);
            } else {
                super.notationDecl(name, publicId, systemId);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) throws SAXException {
            // If we have an explicit DTD handler, delegate to it
            if (dtdHandler != null) {
                dtdHandler.unparsedEntityDecl(name, publicId, systemId, notationName);
            } else {
                super.unparsedEntityDecl(name, publicId, systemId, notationName);
            }
        }

        /** {@inheritDoc} */
        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
            // If we have an explicit entity resolver, delegate to it
            if (entityResolver != null) {
                try {
                    return entityResolver.resolveEntity(publicId, systemId);
                }
                catch (IOException e) {
                    throw new SAXException(e);
                }
            }
            try {
                return super.resolveEntity(publicId, systemId);
            } catch (Exception e) {
                throw new SAXException(e);
            }
        }
    }
}