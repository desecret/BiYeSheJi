package org.example.server.locator;

public interface ElementLocatorStrategy {
    String getName();

    String getLocator(String elementName);

    String getLocator(String elementName, String context);

    String getLocatorType();
}
