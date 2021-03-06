/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.systest.jaxrs.security.jose.jwejws;

import java.net.URL;
import java.security.Security;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.rs.security.jose.jaxrs.JwsDetachedSignatureProvider;
import org.apache.cxf.rs.security.jose.jaxrs.multipart.JwsMultipartClientRequestFilter;
import org.apache.cxf.rs.security.jose.jaxrs.multipart.JwsMultipartClientResponseFilter;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.systest.jaxrs.security.jose.BookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSJwsMultipartTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerJwsMultipart.PORT;
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerJwsMultipart.class, true));
        registerBouncyCastleIfNeeded();
    }

    private static void registerBouncyCastleIfNeeded() throws Exception {
        // Still need it for Oracle Java 7 and Java 8
        Security.addProvider(new BouncyCastleProvider());
    }
    @AfterClass
    public static void unregisterBouncyCastleIfNeeded() throws Exception {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    }
        
    @Test
    public void testJwsJwkBookHMacMultipart() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjwkhmacSinglePart";
        BookStore bs = createJwsBookStoreHMac(address, true, false);
        Book book = bs.echoBookMultipart(new Book("book", 123L));
        assertEquals("book", book.getName());
        assertEquals(123L, book.getId());
    }
    
    @Test
    public void testJwsJwkBookHMacMultipartJwsJson() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjwkhmacSinglePartJwsJson";
        BookStore bs = createJwsBookStoreHMac(address, true, true);
        Book book = bs.echoBookMultipart(new Book("book", 123L));
        assertEquals("book", book.getName());
        assertEquals(123L, book.getId());
    }
    
    @Test
    public void testJwsJwkBookRSAMultipart() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjwkrsaSinglePart";
        BookStore bs = createJwsBookStoreRSA(address, true);
        Book book = bs.echoBookMultipart(new Book("book", 123L));
        assertEquals("book", book.getName());
        assertEquals(123L, book.getId());
    }
    
    @Test
    public void testJwsJwkBooksHMacMultipart() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjwkhmacManyParts";
        BookStore bs = createJwsBookStoreHMac(address, false, false);
        List<Book> books = new LinkedList<Book>();
        books.add(new Book("book", 123L));
        books.add(new Book("book2", 124L));
        List<Book> returnBooks = bs.echoBooksMultipart(books);
        assertEquals("book", returnBooks.get(0).getName());
        assertEquals(123L, returnBooks.get(0).getId());
        assertEquals("book2", returnBooks.get(1).getName());
        assertEquals(124L, returnBooks.get(1).getId());
    }
    @Test(expected = ProcessingException.class)
    public void testJwsJwkBooksHMacMultipartClientRestriction() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjwkhmacManyParts";
        BookStore bs = createJwsBookStoreHMac(address, true, false);
        List<Book> books = new LinkedList<Book>();
        books.add(new Book("book", 123L));
        books.add(new Book("book2", 124L));
        bs.echoBooksMultipart(books);
    }
    @Test(expected = BadRequestException.class)
    public void testJwsJwkBooksHMacMultipartServerRestriction() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjwkhmacSinglePart";
        BookStore bs = createJwsBookStoreHMac(address, false, false);
        List<Book> books = new LinkedList<Book>();
        books.add(new Book("book", 123L));
        books.add(new Book("book2", 124L));
        bs.echoBooksMultipart(books);
    }
    
    @Test(expected = BadRequestException.class)
    public void testJwsJwkBooksHMacMultipartUnsigned() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjwkhmacSinglePart";
        BookStore bs = JAXRSClientFactory.create(address, BookStore.class, 
                            JAXRSJwsMultipartTest.class.getResource("client.xml").toString());
        bs.echoBookMultipart(new Book("book", 123L));
    }
    @Test
    public void testJwsJwkBookHMacMultipartModified() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjwkhmacSinglePartModified";
        BookStore bs = createJwsBookStoreHMac(address, true, false);
        try {
            bs.echoBookMultipart(new Book("book", 123L));
            fail("Exception is expected");
        } catch (WebApplicationException ex) {
            // expected
        }
    }
    private BookStore createJwsBookStoreHMac(String address, 
                                             boolean supportSinglePart,
                                             boolean useJwsJsonSignatureFormat) throws Exception {
        JAXRSClientFactoryBean bean = createJAXRSClientFactoryBean(address, supportSinglePart, 
                                                                   useJwsJsonSignatureFormat);
        bean.getProperties(true).put("rs.security.signature.properties",
            "org/apache/cxf/systest/jaxrs/security/secret.jwk.properties");
        return bean.create(BookStore.class);
    }
    private BookStore createJwsBookStoreRSA(String address, boolean supportSinglePart) throws Exception {
        JAXRSClientFactoryBean bean = createJAXRSClientFactoryBean(address, supportSinglePart, false);
        bean.getProperties(true).put("rs.security.signature.properties",
            "org/apache/cxf/systest/jaxrs/security/alice.jwk.properties");
        return bean.create(BookStore.class);
    }
    private JAXRSClientFactoryBean createJAXRSClientFactoryBean(String address, 
                                                                boolean supportSinglePart,
                                                                boolean useJwsJsonSignatureFormat) throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSJwsMultipartTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        bean.setServiceClass(BookStore.class);
        bean.setAddress(address);
        List<Object> providers = new LinkedList<Object>();
        JwsMultipartClientRequestFilter outFilter = new JwsMultipartClientRequestFilter();
        outFilter.setSupportSinglePartOnly(supportSinglePart);
        outFilter.setUseJwsJsonSignatureFormat(useJwsJsonSignatureFormat);
        providers.add(outFilter);
        JwsMultipartClientResponseFilter inFilter = new JwsMultipartClientResponseFilter();
        inFilter.setSupportSinglePartOnly(supportSinglePart);
        providers.add(inFilter);
        providers.add(new JwsDetachedSignatureProvider());
        bean.setProviders(providers);
        return bean;
    }
}
