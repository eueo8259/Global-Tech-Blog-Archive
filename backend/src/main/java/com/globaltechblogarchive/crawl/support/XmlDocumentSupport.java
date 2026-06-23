package com.globaltechblogarchive.crawl.support;

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public final class XmlDocumentSupport {

    private XmlDocumentSupport() {
    }

    public static Element parseRoot(String xml, String failureMessage) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(false);
            return factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)))
                    .getDocumentElement();
        } catch (Exception exception) {
            throw new IllegalArgumentException(failureMessage, exception);
        }
    }

    public static String text(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return "";
        }
        Node node = nodes.item(0);
        String value = node.getTextContent();
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
