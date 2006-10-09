/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.spi2dav;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.HrefProperty;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;
import org.apache.jackrabbit.webdav.version.LabelInfo;
import org.apache.jackrabbit.webdav.version.UpdateInfo;
import org.apache.jackrabbit.webdav.version.MergeInfo;
import org.apache.jackrabbit.webdav.version.VersionControlledResource;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.ReportMethod;
import org.apache.jackrabbit.webdav.client.methods.DavMethodBase;
import org.apache.jackrabbit.webdav.client.methods.OptionsMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.PropPatchMethod;
import org.apache.jackrabbit.webdav.client.methods.OrderPatchMethod;
import org.apache.jackrabbit.webdav.client.methods.CheckinMethod;
import org.apache.jackrabbit.webdav.client.methods.CheckoutMethod;
import org.apache.jackrabbit.webdav.client.methods.LockMethod;
import org.apache.jackrabbit.webdav.client.methods.UnLockMethod;
import org.apache.jackrabbit.webdav.client.methods.LabelMethod;
import org.apache.jackrabbit.webdav.client.methods.MoveMethod;
import org.apache.jackrabbit.webdav.client.methods.CopyMethod;
import org.apache.jackrabbit.webdav.client.methods.UpdateMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.apache.jackrabbit.webdav.client.methods.SearchMethod;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.MergeMethod;
import org.apache.jackrabbit.webdav.client.methods.SubscribeMethod;
import org.apache.jackrabbit.webdav.client.methods.UnSubscribeMethod;
import org.apache.jackrabbit.webdav.client.methods.PollMethod;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.observation.SubscriptionInfo;
import org.apache.jackrabbit.webdav.observation.EventType;
import org.apache.jackrabbit.webdav.observation.Filter;
import org.apache.jackrabbit.webdav.observation.ObservationConstants;
import org.apache.jackrabbit.webdav.observation.EventDiscovery;
import org.apache.jackrabbit.webdav.security.SecurityConstants;
import org.apache.jackrabbit.webdav.security.CurrentUserPrivilegeSetProperty;
import org.apache.jackrabbit.webdav.security.Privilege;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.lock.LockDiscovery;
import org.apache.jackrabbit.webdav.transaction.TransactionConstants;
import org.apache.jackrabbit.webdav.transaction.TransactionInfo;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.ElementIterator;
import org.apache.jackrabbit.webdav.xml.Namespace;
import org.apache.jackrabbit.webdav.header.CodedUrlHeader;
import org.apache.jackrabbit.webdav.header.IfHeader;
import org.apache.jackrabbit.webdav.search.SearchConstants;
import org.apache.jackrabbit.webdav.jcr.version.report.RepositoryDescriptorsReport;
import org.apache.jackrabbit.webdav.jcr.version.report.RegisteredNamespacesReport;
import org.apache.jackrabbit.webdav.jcr.version.report.NodeTypesReport;
import org.apache.jackrabbit.webdav.jcr.version.report.JcrPrivilegeReport;
import org.apache.jackrabbit.webdav.jcr.nodetype.NodeTypeConstants;
import org.apache.jackrabbit.webdav.jcr.nodetype.NodeTypeProperty;
import org.apache.jackrabbit.webdav.jcr.property.ValuesProperty;
import org.apache.jackrabbit.webdav.jcr.property.NamespacesProperty;
import org.apache.jackrabbit.webdav.jcr.observation.SubscriptionImpl;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.QNodeTypeDefinitionIterator;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.EventListener;
import org.apache.jackrabbit.spi.EventIterator;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.value.ValueFormat;
import org.apache.jackrabbit.value.QValue;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import javax.jcr.RepositoryException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ItemExistsException;
import javax.jcr.AccessDeniedException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.MergeException;
import javax.jcr.NamespaceException;
import javax.jcr.Credentials;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.LoginException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.util.Collection;
import java.util.HashMap;
import java.util.Collections;
import java.io.InputStream;
import java.io.IOException;

/**
 * <code>RepositoryServiceImpl</code>...
 */
// TODO: encapsulate URI building, escaping, unescaping...
// TODO: cache info objects
// TODO: TO-BE-FIXED. caches don't get adjusted upon removal/move of items
public class RepositoryServiceImpl implements RepositoryService, DavConstants {

    private static Logger log = LoggerFactory.getLogger(RepositoryServiceImpl.class);


    private static final EventType[] ALL_EVENTS = new EventType[5];
    static {
        ALL_EVENTS[0] = SubscriptionImpl.getEventType(javax.jcr.observation.Event.NODE_ADDED);
        ALL_EVENTS[1] = SubscriptionImpl.getEventType(javax.jcr.observation.Event.NODE_REMOVED);
        ALL_EVENTS[2] = SubscriptionImpl.getEventType(javax.jcr.observation.Event.PROPERTY_ADDED);
        ALL_EVENTS[3] = SubscriptionImpl.getEventType(javax.jcr.observation.Event.PROPERTY_CHANGED);
        ALL_EVENTS[4] = SubscriptionImpl.getEventType(javax.jcr.observation.Event.PROPERTY_REMOVED);
    }
    private static final SubscriptionInfo S_INFO = new SubscriptionInfo(ALL_EVENTS, true, DavConstants.INFINITE_TIMEOUT);

    private static long POLL_INTERVAL = 300000000;  // TODO: make configurable

    private final IdFactory idFactory;
    private final ValueFactory valueFactory;

    private final Document domFactory;
    private final NamespaceResolverImpl nsResolver;
    private final URIResolverImpl uriResolver;

    private final HttpClient client;

    public RepositoryServiceImpl(String uri, IdFactory idFactory, ValueFactory valueFactory) throws RepositoryException {
        if (uri == null || "".equals(uri)) {
            throw new RepositoryException("Invalid repository uri '" + uri + "'.");
        }
        if (idFactory == null || valueFactory == null) {
            throw new RepositoryException("IdFactory and ValueFactory may not be null.");
        }
        this.idFactory = idFactory;
        this.valueFactory = valueFactory;

        try {
            domFactory = DomUtil.BUILDER_FACTORY.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new RepositoryException(e);
        }

        try {
            URI repositoryUri = new URI((uri.endsWith("/")) ? uri : uri+"/", true);
            HostConfiguration hostConfig = new HostConfiguration();
            hostConfig.setHost(repositoryUri);

            client = new HttpClient(new MultiThreadedHttpConnectionManager());
            client.setHostConfiguration(hostConfig);
            // always send authentication not waiting for 401
            client.getParams().setAuthenticationPreemptive(true);

            nsResolver = new NamespaceResolverImpl();
            uriResolver = new URIResolverImpl(repositoryUri, this, nsResolver, domFactory);

        } catch (URIException e) {
            throw new RepositoryException(e);
        }
    }

    private static void checkSessionInfo(SessionInfo sessionInfo) throws RepositoryException {
        if (!(sessionInfo instanceof SessionInfoImpl)) {
            throw new RepositoryException("Unknown SessionInfo implementation.");
        }
    }

    private static boolean isLockMethod(DavMethod method) {
        int code = DavMethods.getMethodCode(method.getName());
        return DavMethods.DAV_LOCK == code || DavMethods.DAV_UNLOCK == code;
    }

    private static void initMethod(DavMethod method, SessionInfo sessionInfo, boolean addIfHeader) {
        if (addIfHeader) {
            String[] locktokens = sessionInfo.getLockTokens();
            // TODO: ev. build tagged if header
            if (locktokens != null && locktokens.length > 0) {
                IfHeader ifH = new IfHeader(locktokens);
                method.setRequestHeader(ifH.getHeaderName(), ifH.getHeaderValue());
            }
        }
    }

    private HttpClient getClient(org.apache.commons.httpclient.Credentials credentials) {
        // NOTE: null credentials only work if 'missing-auth-mapping' param is
        // set on the server
        client.getState().setCredentials(AuthScope.ANY, credentials);
        return client;
    }

    HttpClient getClient(SessionInfo sessionInfo) throws RepositoryException {
        checkSessionInfo(sessionInfo);
        return getClient(((SessionInfoImpl) sessionInfo).getCredentials().getCredentials());
    }

    private String getItemUri(ItemId itemId, SessionInfo sessionInfo) throws RepositoryException {
        return uriResolver.getItemUri(itemId, sessionInfo.getWorkspaceName(), sessionInfo);
    }

    private String getItemUri(NodeId parentId, QName childName, SessionInfo sessionInfo) throws RepositoryException {
        String parentUri = uriResolver.getItemUri(parentId, sessionInfo.getWorkspaceName(), sessionInfo);
        try {
            return parentUri + NameFormat.format(childName, nsResolver);
        } catch (NoPrefixDeclaredException e) {
            throw new RepositoryException(e);
        }
    }

    private NodeId getParentId(DavPropertySet propSet, SessionInfo sessionInfo)
        throws RepositoryException {
        NodeId parentId = null;
        if (propSet.contains(ItemResourceConstants.JCR_PARENT)) {
            HrefProperty parentProp = new HrefProperty(propSet.get(ItemResourceConstants.JCR_PARENT));
            String parentHref = parentProp.getHrefs().get(0).toString();
            if (parentHref != null && parentHref.length() > 0) {
                parentId = uriResolver.getNodeId(parentHref, sessionInfo);
            }
        }
        return parentId;
    }

    //--------------------------------------------------------------------------

    /**
     * Execute a 'Workspace' operation that immediately needs to return events.
     *
     * @param method
     * @param sessionInfo
     * @return
     * @throws RepositoryException
     */
    private EventIterator execute(DavMethod method, SessionInfo sessionInfo) throws RepositoryException {
        // TODO: build specific subscrUri
        // TODO: check if 'all event' subscription is ok
        String subscrUri = uriResolver.getRootItemUri(sessionInfo.getWorkspaceName());
        String subscrId = subscribe(subscrUri, S_INFO, null, sessionInfo);
        try {
            if (isLockMethod(method)) {
                initMethod(method, sessionInfo, false);
            } else {
                initMethod(method, sessionInfo, true);
            }
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            EventIterator events = poll(subscrUri, subscrId, sessionInfo);
            return events;
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
            unsubscribe(subscrUri, subscrId, sessionInfo);
        }
    }


    //--------------------------------------------------< RepositoryService >---
    /**
     * @see RepositoryService#getIdFactory()
     */
    public IdFactory getIdFactory() {
        return idFactory;
    }

    /**
     * @see RepositoryService#getRepositoryDescriptors()
     */
    public Properties getRepositoryDescriptors() throws RepositoryException {
        ReportInfo info = new ReportInfo(RepositoryDescriptorsReport.REPOSITORY_DESCRIPTORS_REPORT, DavConstants.DEPTH_0);
        ReportMethod method = null;
        try {
            method = new ReportMethod(uriResolver.getRepositoryUri(), info);

            getClient((org.apache.commons.httpclient.Credentials) null).executeMethod(method);
            method.checkSuccess();
            Document doc = method.getResponseBodyAsDocument();
            Properties descriptors = new Properties();
            if (doc != null) {
                Element rootElement = doc.getDocumentElement();
                ElementIterator nsElems = DomUtil.getChildren(rootElement, ItemResourceConstants.XML_DESCRIPTOR, ItemResourceConstants.NAMESPACE);
                while (nsElems.hasNext()) {
                    Element elem = nsElems.nextElement();
                    String key = DomUtil.getChildText(elem, ItemResourceConstants.XML_DESCRIPTORKEY, ItemResourceConstants.NAMESPACE);
                    String descriptor = DomUtil.getChildText(elem, ItemResourceConstants.XML_DESCRIPTORVALUE, ItemResourceConstants.NAMESPACE);
                    if (key != null && descriptor != null) {
                        descriptors.setProperty(key, descriptor);
                    } else {
                        log.error("Invalid descriptor key / value pair: " + key + " -> " + descriptor);
                    }
                }
            }
            return descriptors;
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * TODO: handle impersonation
     *
     * @see RepositoryService#obtain(Credentials, String)
     */
    public SessionInfo obtain(Credentials credentials, String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        // check if the workspace with the given name is accessible
        PropFindMethod method = null;
        try {
            DavPropertyNameSet nameSet = new DavPropertyNameSet();
            nameSet.add(DeltaVConstants.WORKSPACE);
            method = new PropFindMethod(uriResolver.getWorkspaceUri(workspaceName), nameSet, DavConstants.DEPTH_0);
            CredentialsWrapper dc = new CredentialsWrapper(credentials);
            getClient(dc.getCredentials()).executeMethod(method);

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length != 1) {
                throw new LoginException("Login failed: Unknown workspace '" + workspaceName+ " '.");
            }

            DavPropertySet props = responses[0].getProperties(DavServletResponse.SC_OK);
            if (props.contains(DeltaVConstants.WORKSPACE)) {
                String wspHref = new HrefProperty(props.get(DeltaVConstants.WORKSPACE)).getHrefs().get(0).toString();
                String wspName = Text.unescape(Text.getName(wspHref, true));
                if (!wspName.equals(workspaceName)) {
                    throw new LoginException("Login failed: Invalid workspace name " + workspaceName);
                }
                return new SessionInfoImpl(dc, workspaceName, new SubscriptionMgrImpl());
            } else {
                throw new LoginException("Login failed: Unknown workspace '" + workspaceName+ " '.");
            }
        } catch (IOException e) {
            throw new RepositoryException(e.getMessage());
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    public void dispose(SessionInfo sessionInfo) throws RepositoryException {
        checkSessionInfo(sessionInfo);
        SubscriptionManager sMgr = ((SessionInfoImpl)sessionInfo).getSubscriptionManager();
        sMgr.dispose();
    }

    /**
     * @see RepositoryService#getWorkspaceNames(SessionInfo)
     */
    public String[] getWorkspaceNames(SessionInfo sessionInfo) throws RepositoryException {
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(DeltaVConstants.WORKSPACE);
        PropFindMethod method = null;
        try {
            method = new PropFindMethod(uriResolver.getRepositoryUri(), nameSet, DEPTH_1);
            getClient(sessionInfo).executeMethod(method);
            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            Set ids = new HashSet();
            for (int i = 0; i < responses.length; i++) {
                DavPropertySet props = responses[i].getProperties(DavServletResponse.SC_OK);
                if (props.contains(DeltaVConstants.WORKSPACE)) {
                    HrefProperty hp = new HrefProperty(props.get(DeltaVConstants.WORKSPACE));
                    String wspHref = hp.getHrefs().get(0).toString();
                    String id = Text.getName(wspHref, true);
                    ids.add(id);
                }
            }
            return (String[]) ids.toArray(new String[ids.size()]);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#isGranted(SessionInfo, ItemId, String[] actions)
     */
    public boolean isGranted(SessionInfo sessionInfo, ItemId itemId, String[] actions) throws RepositoryException {
        ReportMethod method = null;
        try {
            String uri = getItemUri(itemId, sessionInfo);
            ReportInfo reportInfo = new ReportInfo(JcrPrivilegeReport.PRIVILEGES_REPORT);
            reportInfo.setContentElement(DomUtil.hrefToXml(uri, domFactory));

            method = new ReportMethod(uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName()), reportInfo);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length < 1) {
                throw new ItemNotFoundException("Unable to retrieve permissions for item " + itemId);
            }
            DavProperty p = responses[0].getProperties(DavServletResponse.SC_OK).get(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
            if (p == null) {
                return false;
            }
            // build set of privileges from given actions. NOTE: since the actions
            // have no qualifying namespace, the {@link ItemResourceConstants#NAMESPACE}
            // is used.
            Set requiredPrivileges = new HashSet();
            for (int i = 0; i < actions.length; i++) {
               requiredPrivileges.add(Privilege.getPrivilege(actions[i], ItemResourceConstants.NAMESPACE));
            }
            // build set of privileges granted to the current user.
            CurrentUserPrivilegeSetProperty privSet = new CurrentUserPrivilegeSetProperty(p);
            Collection privileges = (Collection) privSet.getValue();

            // check privileges present against required privileges.
            return privileges.containsAll(requiredPrivileges);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#getRootId(SessionInfo)
     */
    public NodeId getRootId(SessionInfo sessionInfo) throws RepositoryException {
        String rootUri = uriResolver.getRootItemUri(sessionInfo.getWorkspaceName());
        return uriResolver.getNodeId(rootUri, sessionInfo);
    }

    /**
     * @see RepositoryService#getNodeDefinition(SessionInfo, NodeId)
     */
    public QNodeDefinition getNodeDefinition(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        return (QNodeDefinition) getItemDefinition(sessionInfo, nodeId);
    }

    /**
     * @see RepositoryService#getPropertyDefinition(SessionInfo, PropertyId)
     */
    public QPropertyDefinition getPropertyDefinition(SessionInfo sessionInfo, PropertyId propertyId) throws RepositoryException {
        return (QPropertyDefinition) getItemDefinition(sessionInfo, propertyId);
    }

    /**
     *
     * @param sessionInfo
     * @param itemId
     * @return
     * @throws RepositoryException
     */
    private QItemDefinition getItemDefinition(SessionInfo sessionInfo, ItemId itemId) throws RepositoryException {
        // set of properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(ItemResourceConstants.JCR_DEFINITION);
        nameSet.add(DavPropertyName.RESOURCETYPE);

        DavMethodBase method = null;
        try {
            String uri = getItemUri(itemId, sessionInfo);
            method = new PropFindMethod(uri, nameSet, DEPTH_0);
            getClient(sessionInfo).executeMethod(method);

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length < 1) {
                throw new ItemNotFoundException("Unable to retrieve the item definition for " + itemId);
            }
            if (responses.length > 1) {
                throw new RepositoryException("Internal error: ambigous item definition found '" + itemId + "'.");
            }
            DavPropertySet propertySet = responses[0].getProperties(DavServletResponse.SC_OK);

            // check if definition matches the type of the id
            DavProperty rType = propertySet.get(DavPropertyName.RESOURCETYPE);
            if (rType.getValue() == null && itemId.denotesNode()) {
                throw new RepositoryException("Internal error: requested node definition and got property definition.");
            }

            // build the definition
            QItemDefinition definition = null;
            if (propertySet.contains(ItemResourceConstants.JCR_DEFINITION)) {
                DavProperty prop = propertySet.get(ItemResourceConstants.JCR_DEFINITION);
                Object value = prop.getValue();
                if (value != null && value instanceof Element) {
                    Element idfElem = (Element) value;
                    if (itemId.denotesNode()) {
                        definition = new QNodeDefinitionImpl(null, idfElem, nsResolver);
                    } else {
                        definition = new QPropertyDefinitionImpl(null, idfElem, nsResolver);
                    }
                }
            }
            if (definition == null) {
                throw new RepositoryException("Unable to retrieve definition for item with id '" + itemId + "'.");
            }
            return definition;
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#exists(SessionInfo, ItemId)
     */
    public boolean exists(SessionInfo sessionInfo, ItemId itemId) throws RepositoryException {
        HeadMethod method = new HeadMethod(getItemUri(itemId, sessionInfo));
        try {
            int statusCode = getClient(sessionInfo).executeMethod(method);
            if (statusCode == DavServletResponse.SC_OK) {
                return true;
            } else if (statusCode == DavServletResponse.SC_NOT_FOUND) {
                return false;
            } else {
                String msg = "Unexpected status code ("+ statusCode +") while testing existence of item with id " + itemId;
                log.error(msg);
                throw new RepositoryException(msg);
            }
        } catch (IOException e) {
            log.error("Unexpected error while testing existence of item.",e);
            throw new RepositoryException(e);
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * @see RepositoryService#getNodeInfo(SessionInfo, NodeId)
     */
    public NodeInfo getNodeInfo(SessionInfo sessionInfo, NodeId nodeId) throws ItemNotFoundException, RepositoryException {
        // set of properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(ItemResourceConstants.JCR_NAME);
        nameSet.add(ItemResourceConstants.JCR_INDEX);
        nameSet.add(ItemResourceConstants.JCR_PARENT);
        nameSet.add(ItemResourceConstants.JCR_PRIMARYNODETYPE);
        nameSet.add(ItemResourceConstants.JCR_MIXINNODETYPES);
        nameSet.add(ItemResourceConstants.JCR_REFERENCES);
        nameSet.add(ItemResourceConstants.JCR_UUID);
        nameSet.add(DavPropertyName.RESOURCETYPE);

        DavMethodBase method = null;
        try {
            String uri = getItemUri(nodeId, sessionInfo);
            method = new PropFindMethod(uri, nameSet, DEPTH_1);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length < 1) {
                throw new ItemNotFoundException("Unable to retrieve the node with id " + nodeId);
            }

            MultiStatusResponse nodeResponse = null;
            List childResponses = new ArrayList();
            for (int i = 0; i < responses.length; i++) {
                String respHref = responses[i].getHref();
                if (uri.equals(respHref)) {
                    nodeResponse = responses[i];
                } else {
                    childResponses.add(responses[i]);
                }
            }
            if (nodeResponse == null) {
                throw new ItemNotFoundException("Unable to retrieve the node with id " + nodeId);
            }

            DavPropertySet propSet = nodeResponse.getProperties(DavServletResponse.SC_OK);
            NodeId parentId = getParentId(propSet, sessionInfo);
            NodeId id = uriResolver.buildNodeId(parentId, nodeResponse, sessionInfo.getWorkspaceName());

            NodeInfoImpl nInfo = new NodeInfoImpl(id, parentId, propSet, nsResolver);

            for (Iterator it = childResponses.iterator(); it.hasNext();) {
                MultiStatusResponse resp = (MultiStatusResponse) it.next();
                DavPropertySet childProps = resp.getProperties(DavServletResponse.SC_OK);
                if (childProps.contains(DavPropertyName.RESOURCETYPE) &&
                    childProps.get(DavPropertyName.RESOURCETYPE).getValue() != null) {
                    // any other resource type than default (empty) is represented by a node item
                    NodeId childId = uriResolver.buildNodeId(id, resp, sessionInfo.getWorkspaceName());
                    nInfo.addChildId(childId);
                } else {
                    PropertyId childId = uriResolver.buildPropertyId(id, resp, sessionInfo.getWorkspaceName());
                    nInfo.addChildId(childId);
                }
            }

            if (propSet.contains(ItemResourceConstants.JCR_REFERENCES)) {
                HrefProperty refProp = new HrefProperty(propSet.get(ItemResourceConstants.JCR_REFERENCES));
                Iterator hrefIter = refProp.getHrefs().iterator();
                while(hrefIter.hasNext()) {
                    String propertyHref = hrefIter.next().toString();
                    PropertyId propertyId = uriResolver.getPropertyId(propertyHref, sessionInfo);
                    nInfo.addReference(propertyId);
                }
            }

            return nInfo;
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#getPropertyInfo(SessionInfo, PropertyId)
     */
    public PropertyInfo getPropertyInfo(SessionInfo sessionInfo, PropertyId propertyId) throws ItemNotFoundException, RepositoryException {
        // set of Dav-properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(ItemResourceConstants.JCR_NAME);
        nameSet.add(ItemResourceConstants.JCR_PARENT);
        nameSet.add(ItemResourceConstants.JCR_TYPE);
        nameSet.add(ItemResourceConstants.JCR_VALUE);
        nameSet.add(ItemResourceConstants.JCR_VALUES);
        nameSet.add(DavPropertyName.RESOURCETYPE);

        PropFindMethod method = null;
        try {
            String uri = getItemUri(propertyId, sessionInfo);
            method = new PropFindMethod(uri, nameSet, DEPTH_1);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length < 1) {
                throw new ItemNotFoundException("Unable to retrieve the property with id " + propertyId);
            }


            DavPropertySet propSet = responses[0].getProperties(DavServletResponse.SC_OK);
            NodeId parentId = getParentId(propSet, sessionInfo);
            PropertyId id = uriResolver.buildPropertyId(parentId, responses[0], sessionInfo.getWorkspaceName());

            PropertyInfo pInfo = new PropertyInfoImpl(id, parentId, propSet, nsResolver, valueFactory);
            return pInfo;
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#createBatch(ItemId, SessionInfo)
     */
    public Batch createBatch(ItemId itemId, SessionInfo sessionInfo) throws RepositoryException {
        return new BatchImpl(itemId, sessionInfo);
    }

    /**
     * @see RepositoryService#submit(Batch)
     */
    public EventIterator submit(Batch batch) throws RepositoryException {
        if (!(batch instanceof BatchImpl)) {
            throw new RepositoryException("Unknown Batch implementation.");
        }
        BatchImpl batchImpl = (BatchImpl) batch;
        if (batchImpl.isEmpty()) {
            batchImpl.dispose();
            return IteratorHelper.EMPTY;
        }
        // send batched information
        try {
            HttpClient client = batchImpl.start();
            EventIterator events;
            boolean success = false;
            try {
                Iterator it = batchImpl.methods();
                while (it.hasNext()) {
                    DavMethod method = (DavMethod) it.next();
                    initMethod(method, batchImpl.sessionInfo, true);
                    // add batchId as separate header
                    CodedUrlHeader ch = new CodedUrlHeader(TransactionConstants.HEADER_TRANSACTIONID, batchImpl.batchId);
                    method.setRequestHeader(ch.getHeaderName(), ch.getHeaderValue());

                    client.executeMethod(method);
                    method.checkSuccess();
                    method.releaseConnection();
                }
                success = true;
            } finally {
                // make sure the lock is removed. if any of the methods
                // failed the unlock is used to abort any pending changes
                // on the server.
                events = batchImpl.end(client, success);
            }
            return events;
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            batchImpl.dispose();
        }
    }

    /**
     * @see RepositoryService#importXml(SessionInfo, NodeId, InputStream, int)
     */
    public EventIterator importXml(SessionInfo sessionInfo, NodeId parentId, InputStream xmlStream, int uuidBehaviour) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        // TODO: improve. currently random name is built instead of retrieving name of new resource from top-level xml element within stream
        QName nodeName = new QName(QName.NS_DEFAULT_URI, UUID.randomUUID().toString());
        String uri = getItemUri(parentId, nodeName, sessionInfo);
        MkColMethod method = new MkColMethod(uri);
        method.setRequestEntity(new InputStreamRequestEntity(xmlStream, "text/xml"));

        return execute(method, sessionInfo);
    }

    /**
     * @see RepositoryService#move(SessionInfo, NodeId, NodeId, QName)
     */
    public EventIterator move(SessionInfo sessionInfo, NodeId srcNodeId, NodeId destParentNodeId, QName destName) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        String uri = getItemUri(srcNodeId, sessionInfo);
        String destUri = getItemUri(destParentNodeId, destName, sessionInfo);
        MoveMethod method = new MoveMethod(uri, destUri, true);
        return execute(method, sessionInfo);
    }

    /**
     * @see RepositoryService#copy(SessionInfo, String, NodeId, NodeId, QName)
     */
    public EventIterator copy(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, QName destName) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        String uri = uriResolver.getItemUri(srcNodeId, srcWorkspaceName, sessionInfo);
        String destUri = getItemUri(destParentNodeId, destName, sessionInfo);
        CopyMethod method = new CopyMethod(uri, destUri, true, false);
        return execute(method, sessionInfo);
    }

    /**
     * @see RepositoryService#update(SessionInfo, NodeId, String)
     */
    public EventIterator update(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        String workspUri = uriResolver.getWorkspaceUri(srcWorkspaceName);

        return update(uri, new String[] {workspUri}, UpdateInfo.UPDATE_BY_WORKSPACE, false, sessionInfo);
    }

    /**
     * @see RepositoryService#clone(SessionInfo, String, NodeId, NodeId, QName, boolean)
     */
    public EventIterator clone(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId, NodeId destParentNodeId, QName destName, boolean removeExisting) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        // TODO: missing implementation
        throw new UnsupportedOperationException("Missing implementation");
    }

    /**
     * @see RepositoryService#getLockInfo(SessionInfo, NodeId)
     */
    public LockInfo getLockInfo(SessionInfo sessionInfo, NodeId nodeId) throws LockException, RepositoryException {
        // set of Dav-properties to be retrieved
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(DavPropertyName.LOCKDISCOVERY);

        PropFindMethod method = null;
        try {
            String uri = getItemUri(nodeId, sessionInfo);
            method = new PropFindMethod(uri, nameSet, DEPTH_0);
            initMethod(method, sessionInfo, true);

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            MultiStatusResponse[] responses = method.getResponseBodyAsMultiStatus().getResponses();
            if (responses.length != 1) {
                throw new ItemNotFoundException("Unable to retrieve the property with id " + nodeId);
            }

            DavPropertySet ps = responses[0].getProperties(DavServletResponse.SC_OK);
            if (ps.contains(DavPropertyName.LOCKDISCOVERY)) {
                DavProperty p = ps.get(DavPropertyName.LOCKDISCOVERY);
                return new LockInfoImpl(LockDiscovery.createFromXml(p.toXml(domFactory)), nodeId);
            } else {
                throw new LockException("No Lock present on node with id " + nodeId);
            }

        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#lock(SessionInfo, NodeId, boolean)
     */
    public EventIterator lock(SessionInfo sessionInfo, NodeId nodeId, boolean deep) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        try {
            String uri = getItemUri(nodeId, sessionInfo);
            LockMethod method = new LockMethod(uri, Scope.EXCLUSIVE, Type.WRITE,
                sessionInfo.getUserID(), DavConstants.INFINITE_TIMEOUT, deep);
            EventIterator events = execute(method, sessionInfo);

            String lockToken = method.getLockToken();
            sessionInfo.addLockToken(lockToken);

            // TODO: ev. need to take care of 'timeout' ?
            // TODO: ev. evaluate lock response, if depth and type is according to request?
            return events;

        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see RepositoryService#refreshLock(SessionInfo, NodeId)
     */
    public EventIterator refreshLock(SessionInfo sessionInfo, NodeId nodeId) throws LockException, RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        // since sessionInfo does not allow to retrieve token by NodeId,
        // pass all available lock tokens to the LOCK method (TODO: correct?)
        LockMethod method = new LockMethod(uri, DavConstants.INFINITE_TIMEOUT, sessionInfo.getLockTokens());
        return execute(method, sessionInfo);
    }

    /**
     * @see RepositoryService#unlock(SessionInfo, NodeId)
     */
    public EventIterator unlock(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        // Note: since sessionInfo does not allow to identify the id of the
        // lock holding node, we need to access the token via lockInfo
        // TODO: review this.
        LockInfo lInfo = getLockInfo(sessionInfo, nodeId);
        String lockToken = lInfo.getLockToken();

        // TODO: ev. additional check if lt is present on the sessionInfo?

        UnLockMethod method = new UnLockMethod(uri, lockToken);
        EventIterator events = execute(method, sessionInfo);

        sessionInfo.removeLockToken(lockToken);
        return events;
    }

    /**
     * @see RepositoryService#checkin(SessionInfo, NodeId)
     */
    public EventIterator checkin(SessionInfo sessionInfo, NodeId nodeId) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        CheckinMethod method = new CheckinMethod(uri);

        return execute(method, sessionInfo);
    }

    /**
     * @see RepositoryService#checkout(SessionInfo, NodeId)
     */
    public EventIterator checkout(SessionInfo sessionInfo, NodeId nodeId) throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        CheckoutMethod method = new CheckoutMethod(uri);

        return execute(method, sessionInfo);
    }

    /**
     * @see RepositoryService#restore(SessionInfo, NodeId, NodeId, boolean)
     */
    public EventIterator restore(SessionInfo sessionInfo, NodeId nodeId, NodeId versionId, boolean removeExisting) throws VersionException, PathNotFoundException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        String uri = getItemUri(nodeId, sessionInfo);
        String vUri = getItemUri(versionId, sessionInfo);

        return update(uri, new String[] {vUri}, UpdateInfo.UPDATE_BY_VERSION, removeExisting, sessionInfo);
    }

    /**
     * @see RepositoryService#restore(SessionInfo, NodeId[], boolean)
     */
    public EventIterator restore(SessionInfo sessionInfo, NodeId[] versionIds, boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException {
        String uri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());
        String[] vUris = new String[versionIds.length];
        for (int i = 0; i < versionIds.length; i++) {
            vUris[i] = getItemUri(versionIds[i], sessionInfo);
        }

        return update(uri, vUris, UpdateInfo.UPDATE_BY_VERSION, removeExisting, sessionInfo);
    }

    private EventIterator update(String uri, String[] updateSource, int updateType, boolean removeExisting, SessionInfo sessionInfo) throws RepositoryException {
        try {
            UpdateInfo uInfo;
            if (removeExisting) {
                Element uElem = UpdateInfo.createUpdateElement(updateSource, updateType, domFactory);
                DomUtil.addChildElement(uElem, ItemResourceConstants.XML_REMOVEEXISTING, ItemResourceConstants.NAMESPACE);
                uInfo = new UpdateInfo(uElem);
            } else {
                uInfo = new UpdateInfo(updateSource, updateType, new DavPropertyNameSet());
            }

            UpdateMethod method = new UpdateMethod(uri, uInfo);
            return execute(method, sessionInfo);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        }
    }
    /**
     * @see RepositoryService#merge(SessionInfo, NodeId, String, boolean)
     */
    public EventIterator merge(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        try {
            String wspHref = uriResolver.getWorkspaceUri(srcWorkspaceName);
            Element mElem = MergeInfo.createMergeElement(new String[] {wspHref}, bestEffort, false, domFactory);
            MergeInfo mInfo = new MergeInfo(mElem);

            MergeMethod method = new MergeMethod(getItemUri(nodeId, sessionInfo), mInfo);
            // TODO: need to evaluate response?
            return execute(method, sessionInfo);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        }
    }

    /**
     * @see RepositoryService#resolveMergeConflict(SessionInfo, NodeId, NodeId[], NodeId[])
     */
    public EventIterator resolveMergeConflict(SessionInfo sessionInfo, NodeId nodeId, NodeId[] mergeFailedIds, NodeId[] predecessorIds) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        try {
            List changeList = new ArrayList();
            String[] mergeFailedHref = new String[mergeFailedIds.length];
            for (int i = 0; i < mergeFailedIds.length; i++) {
                mergeFailedHref[i] = getItemUri(mergeFailedIds[i], sessionInfo);
            }
            changeList.add(new HrefProperty(VersionControlledResource.AUTO_MERGE_SET, mergeFailedHref, false));

            if (predecessorIds != null && predecessorIds.length > 0) {
                String[] pdcHrefs = new String[predecessorIds.length];
                for (int i = 0; i < predecessorIds.length; i++) {
                    pdcHrefs[i] = getItemUri(predecessorIds[i], sessionInfo);
                }
                changeList.add(new HrefProperty(VersionControlledResource.PREDECESSOR_SET, pdcHrefs, false));
            }

            PropPatchMethod method = new PropPatchMethod(getItemUri(nodeId, sessionInfo), changeList);
            // TODO: ev. evaluate response
            return execute(method, sessionInfo);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see RepositoryService#addVersionLabel(SessionInfo,NodeId,NodeId,QName,boolean)
     */
    public EventIterator addVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, QName label, boolean moveLabel) throws VersionException, RepositoryException {
         try {
            String uri = getItemUri(versionId, sessionInfo);
            LabelMethod method = new LabelMethod(uri, NameFormat.format(label, nsResolver), (moveLabel) ? LabelInfo.TYPE_SET : LabelInfo.TYPE_ADD);
            return execute(method, sessionInfo);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (NoPrefixDeclaredException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see RepositoryService#removeVersionLabel(SessionInfo,NodeId,NodeId,QName)
     */
    public EventIterator removeVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId, QName label) throws VersionException, RepositoryException {
        try {
            String uri = getItemUri(versionId, sessionInfo);
            LabelMethod method = new LabelMethod(uri, NameFormat.format(label, nsResolver), LabelInfo.TYPE_REMOVE);
            return execute(method, sessionInfo);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (NoPrefixDeclaredException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see RepositoryService#getSupportedQueryLanguages(SessionInfo)
     */
    public String[] getSupportedQueryLanguages(SessionInfo sessionInfo) throws RepositoryException {
        OptionsMethod method = new OptionsMethod(uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName()));
        try {
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            Header daslHeader = method.getResponseHeader(SearchConstants.HEADER_DASL);
            CodedUrlHeader h = new CodedUrlHeader(daslHeader.getName(), daslHeader.getValue());
            return h.getCodedUrls();
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#executeQuery(SessionInfo, String, String)
     */
    public QueryInfo executeQuery(SessionInfo sessionInfo, String statement, String language) throws RepositoryException {
        SearchMethod method = null;
        try {
            String uri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());
            method = new SearchMethod(uri, statement, language);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            MultiStatus ms = method.getResponseBodyAsMultiStatus();
            return new QueryInfoImpl(ms, sessionInfo, uriResolver,
                nsResolver, valueFactory);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        }  finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#addEventListener(SessionInfo,NodeId,EventListener,int,boolean,String[],QName[])
     */
    public void addEventListener(SessionInfo sessionInfo, NodeId nodeId, EventListener listener, int eventTypes, boolean isDeep, String[] uuids, QName[] nodeTypeIds) throws RepositoryException {
        // build event types
        List eTypes = new ArrayList();
        if ((eventTypes & Event.NODE_ADDED) == Event.NODE_ADDED) {
            eTypes.add(SubscriptionImpl.getEventType(javax.jcr.observation.Event.NODE_ADDED));
        }
        if ((eventTypes & Event.NODE_REMOVED) == Event.NODE_REMOVED) {
            eTypes.add(SubscriptionImpl.getEventType(javax.jcr.observation.Event.NODE_REMOVED));
        }
        if ((eventTypes & Event.PROPERTY_ADDED) == Event.PROPERTY_ADDED) {
            eTypes.add(SubscriptionImpl.getEventType(javax.jcr.observation.Event.PROPERTY_ADDED));
        }
        if ((eventTypes & Event.PROPERTY_REMOVED) == Event.PROPERTY_REMOVED) {
            eTypes.add(SubscriptionImpl.getEventType(javax.jcr.observation.Event.PROPERTY_REMOVED));
        }
        if ((eventTypes & Event.PROPERTY_CHANGED) == Event.PROPERTY_CHANGED) {
            eTypes.add(SubscriptionImpl.getEventType(javax.jcr.observation.Event.PROPERTY_CHANGED));
        }
        EventType[] etArr = (EventType[]) eTypes.toArray(new EventType[eTypes.size()]);

        // build filters from params
        List filters = new ArrayList();
        for (int i = 0; uuids != null && i < uuids.length; i++) {
            filters.add(new Filter(ObservationConstants.XML_UUID, ObservationConstants.NAMESPACE, uuids[i]));
        }
        for (int i = 0; nodeTypeIds != null && i < nodeTypeIds.length; i++) {
            try {
                String ntName = NameFormat.format(nodeTypeIds[i], nsResolver);
                filters.add(new Filter(ObservationConstants.XML_NODETYPE_NAME, ObservationConstants.NAMESPACE, ntName));
            } catch (NoPrefixDeclaredException e) {
                throw new RepositoryException(e);
            }
        }
        Filter[] ftArr = (Filter[]) filters.toArray(new Filter[filters.size()]);

        boolean noLocal = true;
        SubscriptionInfo subscriptionInfo = new SubscriptionInfo(etArr, ftArr, noLocal, isDeep, DavConstants.UNDEFINED_TIMEOUT);
        String uri = getItemUri(nodeId, sessionInfo);

        checkSessionInfo(sessionInfo);
        SubscriptionManager sMgr = ((SessionInfoImpl)sessionInfo).getSubscriptionManager();

        if (sMgr.subscriptionExists(listener)) {
            String subscriptionId = sMgr.getSubscriptionId(listener);
            subscribe(uri, subscriptionInfo, subscriptionId, sessionInfo);
            log.debug("Subscribed on server for listener " + listener);
        } else {
            String subscriptionId = subscribe(uri, subscriptionInfo, null, sessionInfo);
            log.debug("Subscribed on server for listener " + listener);
            sMgr.addSubscription(uri, subscriptionId, listener);
            log.debug("Added subscription for listener " + listener);
        }
    }

    /**
     * @see RepositoryService#removeEventListener(SessionInfo, NodeId, EventListener)
     */
    public void removeEventListener(SessionInfo sessionInfo, NodeId nodeId, EventListener listener) throws RepositoryException {
        checkSessionInfo(sessionInfo);
        SubscriptionManager sMgr = ((SessionInfoImpl)sessionInfo).getSubscriptionManager();
        String subscriptionId = sMgr.getSubscriptionId(listener);

        String uri = getItemUri(nodeId, sessionInfo);
        sMgr.removeSubscription(listener);
        log.debug("Removed subscription for listener " + listener);
        unsubscribe(uri, subscriptionId, sessionInfo);
        log.debug("Unsubscribed on server for listener " + listener);
    }


    private String subscribe(String uri, SubscriptionInfo subscriptionInfo, String subscriptionId, SessionInfo sessionInfo) throws RepositoryException {
        SubscribeMethod method = null;
        try {
            if (subscriptionId != null) {
                method = new SubscribeMethod(uri, subscriptionInfo, subscriptionId);
            } else {
                method = new SubscribeMethod(uri, subscriptionInfo);
            }
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();
            return method.getSubscriptionId();
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        }  finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    private void unsubscribe(String uri, String subscriptionId, SessionInfo sessionInfo) throws RepositoryException {
        UnSubscribeMethod method = null;
        try {
            method = new UnSubscribeMethod(uri, subscriptionId);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        }  finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    private EventIterator poll(String uri, String subscriptionId,  SessionInfo sessionInfo) throws RepositoryException {
        PollMethod method = null;
        try {
            method = new PollMethod(uri, subscriptionId);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            EventDiscovery disc = method.getResponseAsEventDiscovery();
            if (disc.isEmpty()) {
                return IteratorHelper.EMPTY;
            } else {
                Element discEl = disc.toXml(domFactory);
                return new EventIteratorImpl(discEl, uriResolver, sessionInfo);
            }
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        }  finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#getRegisteredNamespaces(SessionInfo)
     */
    public Properties getRegisteredNamespaces(SessionInfo sessionInfo) throws RepositoryException {
        ReportInfo info = new ReportInfo(RegisteredNamespacesReport.REGISTERED_NAMESPACES_REPORT, DEPTH_0);
        ReportMethod method = null;
        try {
            method = new ReportMethod(uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName()), info);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            Document doc = method.getResponseBodyAsDocument();
            Properties namespaces = new Properties();
            if (doc != null) {
                Element rootElement = doc.getDocumentElement();
                ElementIterator nsElems = DomUtil.getChildren(rootElement, ItemResourceConstants.XML_NAMESPACE, ItemResourceConstants.NAMESPACE);
                while (nsElems.hasNext()) {
                    Element elem = nsElems.nextElement();
                    String prefix = DomUtil.getChildText(elem, ItemResourceConstants.XML_PREFIX, ItemResourceConstants.NAMESPACE);
                    String uri = DomUtil.getChildText(elem, ItemResourceConstants.XML_URI, ItemResourceConstants.NAMESPACE);
                    // default namespace
                    if (prefix == null && uri == null) {
                        prefix = uri = "";
                    }
                    // any other uri must not be null
                    if (uri != null) {
                        namespaces.setProperty(prefix, uri);
                        // TODO: not correct since nsRegistry is retrieved from each session
                        nsResolver.add(prefix, uri);
                    } else {
                        log.error("Invalid prefix / uri pair: " + prefix + " -> " + uri);
                    }
                }
            }
            return namespaces;
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#registerNamespace(SessionInfo, String, String)
     */
    public void registerNamespace(SessionInfo sessionInfo, String prefix, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        Properties namespaces = nsResolver.getNamespaces();
        // add new pair that needs to be registered.
        namespaces.setProperty(prefix, uri);

        internalSetNamespaces(sessionInfo, namespaces);
        // adjust internal mappings:
        // TODO: not correct since nsRegistry is retrieved from each session
        nsResolver.add(prefix, uri);
    }

    /**
     * @see RepositoryService#unregisterNamespace(SessionInfo, String)
     */
    public void unregisterNamespace(SessionInfo sessionInfo, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        String prefix = nsResolver.getPrefix(uri);
        Properties namespaces = nsResolver.getNamespaces();
        // remove pair that needs to be unregistered
        namespaces.remove(prefix);

        internalSetNamespaces(sessionInfo, namespaces);
        // adjust internal mappings:
        // TODO: not correct since nsRegistry is retrieved from each session
        nsResolver.remove(prefix, uri);
    }

    /**
     *
     * @param sessionInfo
     * @param namespaces
     * @throws NamespaceException
     * @throws UnsupportedRepositoryOperationException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    private void internalSetNamespaces(SessionInfo sessionInfo, Properties namespaces) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        DavPropertySet setProperties = new DavPropertySet();
        setProperties.add(new NamespacesProperty(namespaces));

        PropPatchMethod method = null;
        try {
            String uri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());

            method = new PropPatchMethod(uri, setProperties, new DavPropertyNameSet());
            initMethod(method, sessionInfo, true);

            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @see RepositoryService#getNodeTypeDefinitions(SessionInfo)
     */
    public QNodeTypeDefinitionIterator getNodeTypeDefinitions(SessionInfo sessionInfo) throws RepositoryException {
        ReportInfo info = new ReportInfo(NodeTypesReport.NODETYPES_REPORT, DEPTH_0);
        info.setContentElement(DomUtil.createElement(domFactory, NodeTypeConstants.XML_REPORT_ALLNODETYPES, NodeTypeConstants.NAMESPACE));

        ReportMethod method = null;
        try {
            String workspaceUri = uriResolver.getWorkspaceUri(sessionInfo.getWorkspaceName());
            method = new ReportMethod(workspaceUri, info);
            getClient(sessionInfo).executeMethod(method);
            method.checkSuccess();

            Document reportDoc = method.getResponseBodyAsDocument();
            ElementIterator it = DomUtil.getChildren(reportDoc.getDocumentElement(), NodeTypeConstants.NODETYPE_ELEMENT, null);
            List ntDefs = new ArrayList();
            while (it.hasNext()) {
                ntDefs.add(new QNodeTypeDefinitionImpl(it.nextElement(), nsResolver));
            }
            return new IteratorHelper(ntDefs);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (DavException e) {
            throw ExceptionConverter.generate(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * @throws UnsupportedRepositoryOperationException
     * @see RepositoryService#registerNodeTypes(SessionInfo, QNodeTypeDefinition[])
     */
    public void registerNodeTypes(SessionInfo sessionInfo, QNodeTypeDefinition[] nodetypeDefs) throws NoSuchNodeTypeException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException("JSR170 does not defined methods to register nodetypes.");
    }

    /**
     * @throws UnsupportedRepositoryOperationException
     * @see RepositoryService#reregisterNodeTypes(SessionInfo, QNodeTypeDefinition[])
     */
    public void reregisterNodeTypes(SessionInfo sessionInfo, QNodeTypeDefinition[] nodetypeDefs) throws NoSuchNodeTypeException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException("JSR170 does not defined methods to reregister nodetypes.");
    }

    /**
     * @throws UnsupportedRepositoryOperationException
     * @see RepositoryService#unregisterNodeTypes(SessionInfo, QName[])
     */
    public void unregisterNodeTypes(SessionInfo sessionInfo, QName[] nodetypeNames) throws NoSuchNodeTypeException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException("JSR170 does not defined methods to unregister nodetypes.");
    }

    /**
     * The XML elements and attributes used in serialization
     */
    private static final Namespace SV_NAMESPACE = Namespace.getNamespace(QName.NS_SV_PREFIX, QName.NS_SV_URI);
    private static final String NODE_ELEMENT = "node";
    private static final String PROPERTY_ELEMENT = "property";
    private static final String VALUE_ELEMENT = "value";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String TYPE_ATTRIBUTE = "type";

    //------------------------------------------------< Inner Class 'Batch' >---
    private class BatchImpl implements Batch {

        private final SessionInfo sessionInfo;
        private final ItemId targetId;
        private final List methods = new ArrayList();

        private String batchId;
        private String subscriptionId;

        private boolean isConsumed = false;

        private BatchImpl(ItemId targetId, SessionInfo sessionInfo) {
            this.targetId = targetId;
            this.sessionInfo = sessionInfo;
        }

        private HttpClient start() throws RepositoryException {
            checkConsumed();
            try {
                String uri = getItemUri(targetId, sessionInfo);
                // start special 'lock'
                LockMethod method = new LockMethod(uri, TransactionConstants.LOCAL, TransactionConstants.TRANSACTION, null, DavConstants.INFINITE_TIMEOUT, true);
                initMethod(method, sessionInfo, false);

                HttpClient client = getClient(sessionInfo);
                client.executeMethod(method);
                method.checkSuccess();

                batchId = method.getLockToken();

                // register subscription
                String subscrUri = (targetId.denotesNode() ? uri : getItemUri(((PropertyId) targetId).getParentId(), sessionInfo));
                subscriptionId = subscribe(subscrUri, S_INFO, null, sessionInfo);
                return client;
            } catch (IOException e) {
                throw new RepositoryException(e);
            } catch (DavException e) {
                throw ExceptionConverter.generate(e);
            }
        }

        private EventIterator end(HttpClient client, boolean commit) throws RepositoryException {
            checkConsumed();

            String uri = getItemUri(targetId, sessionInfo);
            String subscrUri = (targetId.denotesNode() ? uri : getItemUri(((PropertyId) targetId).getParentId(), sessionInfo));

            UnLockMethod method = null;
            try {
                // make sure the lock initially created is removed again on the
                // server, asking the server to persist the modifications
                method = new UnLockMethod(uri, batchId);
                // todo: check if 'initmethod' would work (ev. conflict with TxId header).
                String[] locktokens = sessionInfo.getLockTokens();
                if (locktokens != null && locktokens.length > 0) {
                    IfHeader ifH = new IfHeader(locktokens);
                    method.setRequestHeader(ifH.getHeaderName(), ifH.getHeaderValue());
                }
                // in contrast to standard UNLOCK, the tx-unlock provides a
                // request body.
                method.setRequestBody(new TransactionInfo(commit));
                client.executeMethod(method);
                method.checkSuccess();

                // retrieve events
                EventIterator events = poll(subscrUri, subscriptionId, sessionInfo);
                return events;
            } catch (IOException e) {
                throw new RepositoryException(e);
            } catch (DavException e) {
                throw ExceptionConverter.generate(e);
            } finally {
                if (method != null) {
                    // release UNLOCK method
                    method.releaseConnection();
                }
                // unsubscribe
                unsubscribe(subscrUri, subscriptionId, sessionInfo);
            }
        }

        private void dispose() {
            methods.clear();
            isConsumed = true;
        }

        private void checkConsumed() {
            if (isConsumed) {
                throw new IllegalStateException("Batch has already been consumed.");
            }
        }

        private boolean isEmpty() {
            return methods.isEmpty();
        }

        private Iterator methods() {
            return methods.iterator();
        }

        //----------------------------------------------------------< Batch >---
        /**
         * @see Batch#addNode(NodeId, QName, QName, String)
         */
        public void addNode(NodeId parentId, QName nodeName, QName nodetypeName, String uuid) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, NoSuchNodeTypeException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            try {
                String uri = getItemUri(parentId, nodeName, sessionInfo);
                MkColMethod method = new MkColMethod(uri);

                // build 'sys-view' for the node to create and append it as request body
                if (nodetypeName != null || uuid != null) {
                    Document body = DomUtil.BUILDER_FACTORY.newDocumentBuilder().newDocument();
                    Element nodeElement = DomUtil.addChildElement(body, NODE_ELEMENT, SV_NAMESPACE);
                    DomUtil.setAttribute(nodeElement, NAME_ATTRIBUTE, SV_NAMESPACE, Text.getName(uri, true));

                    if (nodetypeName != null) {
                        Element propElement = DomUtil.addChildElement(nodeElement, PROPERTY_ELEMENT, SV_NAMESPACE);
                        DomUtil.setAttribute(propElement, NAME_ATTRIBUTE, SV_NAMESPACE, NameFormat.format(QName.JCR_PRIMARYTYPE, nsResolver));
                        DomUtil.setAttribute(propElement, TYPE_ATTRIBUTE, SV_NAMESPACE, PropertyType.nameFromValue(PropertyType.NAME));
                        DomUtil.addChildElement(propElement, VALUE_ELEMENT, SV_NAMESPACE, NameFormat.format(nodetypeName, nsResolver));
                    }
                    if (uuid != null) {
                        Element propElement = DomUtil.addChildElement(nodeElement, PROPERTY_ELEMENT, SV_NAMESPACE);
                        DomUtil.setAttribute(propElement, NAME_ATTRIBUTE, SV_NAMESPACE, NameFormat.format(QName.JCR_UUID, nsResolver));
                        DomUtil.setAttribute(propElement, TYPE_ATTRIBUTE, SV_NAMESPACE, PropertyType.nameFromValue(PropertyType.STRING));
                        DomUtil.addChildElement(propElement, VALUE_ELEMENT, SV_NAMESPACE, uuid);
                    }
                    method.setRequestBody(body);
                }

                methods.add(method);
            } catch (IOException e) {
                throw new RepositoryException(e);
            } catch (ParserConfigurationException e) {
                throw new RepositoryException(e);
            } catch (NoPrefixDeclaredException e) {
                throw new RepositoryException(e);
            }
        }

        /**
         * @see Batch#addProperty(NodeId, QName, String, int)
         */
        public void addProperty(NodeId parentId, QName propertyName, String value, int propertyType) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            QValue qV = QValue.create(value, propertyType);
            Value jcrValue = ValueFormat.getJCRValue(qV, nsResolver, valueFactory);
            ValuesProperty vp = new ValuesProperty(jcrValue);
            internalAddProperty(parentId, propertyName, vp);
        }

        /**
         * @see Batch#addProperty(NodeId, QName, String[], int)
         */
        public void addProperty(NodeId parentId, QName propertyName, String[] values, int propertyType) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            Value[] jcrValues = new Value[values.length];
            for (int i = 0; i < values.length; i++) {
                QValue v = QValue.create(values[i], propertyType);
                jcrValues[i] = ValueFormat.getJCRValue(v, nsResolver, valueFactory);
            }
            ValuesProperty vp = new ValuesProperty(jcrValues);
            internalAddProperty(parentId, propertyName, vp);
        }

        /**
         * @see Batch#addProperty(NodeId, QName, InputStream, int)
         */
        public void addProperty(NodeId parentId, QName propertyName, InputStream value, int propertyType) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            QValue qV = null;
            try {
                qV = QValue.create(value, propertyType);
                Value jcrValue = ValueFormat.getJCRValue(qV, nsResolver, valueFactory);
                ValuesProperty vp = new ValuesProperty(jcrValue);
                internalAddProperty(parentId, propertyName, vp);
            } catch (IOException e) {
                throw new ValueFormatException(e);
            }
        }

        /**
         * @see Batch#addProperty(NodeId, QName, InputStream[], int)
         */
        public void addProperty(NodeId parentId, QName propertyName, InputStream[] values, int propertyType) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            try {
                Value[] jcrValues = new Value[values.length];
                for (int i = 0; i < values.length; i++) {
                    QValue qV = QValue.create(values[i], propertyType);
                    jcrValues[i] = ValueFormat.getJCRValue(qV, nsResolver, valueFactory);
                }
                ValuesProperty vp = new ValuesProperty(jcrValues);
                internalAddProperty(parentId, propertyName, vp);
            } catch (IOException e) {
                throw new ValueFormatException(e);
            }
        }

        private void internalAddProperty(NodeId parentId, QName propertyName, ValuesProperty vp) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            try {
                String uri = getItemUri(parentId, propertyName, sessionInfo);
                PutMethod method = new PutMethod(uri);
                method.setRequestBody(vp);

                methods.add(method);

            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }

        /**
         * @see Batch#setValue(PropertyId, String, int)
         */
        public void setValue(PropertyId propertyId, String value, int propertyType) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            DavPropertySet setProperties = new DavPropertySet();
            if (value == null) {
                // setting property value to 'null' is identical to a removal
                remove(propertyId);
            } else {
                // qualified value must be converted to jcr value
                QValue qV = QValue.create(value, propertyType);
                Value jcrValue = ValueFormat.getJCRValue(qV, nsResolver, valueFactory);
                ValuesProperty vp = new ValuesProperty(jcrValue);
                setProperties.add(vp);
            }
            internalSetValue(propertyId, setProperties);
        }

        /**
         * @see Batch#setValue(PropertyId, String[], int)
         */
        public void setValue(PropertyId propertyId, String[] values, int propertyType) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            DavPropertySet setProperties = new DavPropertySet();
            if (values == null) {
                // setting property value to 'null' is identical to a removal
                remove(propertyId);
            } else {
                // qualified values must be converted to jcr values
                Value[] jcrValues = new Value[values.length];
                for (int i = 0; i < values.length; i++) {
                    QValue qV = QValue.create(values[i], propertyType);
                    jcrValues[i] = ValueFormat.getJCRValue(qV, nsResolver, valueFactory);
                }
                setProperties.add(new ValuesProperty(jcrValues));
            }
            internalSetValue(propertyId, setProperties);
        }

        /**
         * @see Batch#setValue(PropertyId, InputStream, int)
         */
        public void setValue(PropertyId propertyId, InputStream value, int propertyType) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            try {
                DavPropertySet setProperties = new DavPropertySet();
                if (value == null) {
                    // setting property value to 'null' is identical to a removal
                    remove(propertyId);
                } else {
                    // qualified value must be converted to jcr value
                    QValue qV = QValue.create(value, propertyType);
                    Value jcrValue = ValueFormat.getJCRValue(qV, nsResolver, valueFactory);
                    ValuesProperty vp = new ValuesProperty(jcrValue);
                    setProperties.add(vp);
                }
                internalSetValue(propertyId, setProperties);
            } catch (IOException e) {
                throw new ValueFormatException(e);
            }
        }

        /**
         * @see Batch#setValue(PropertyId, InputStream[], int)
         */
        public void setValue(PropertyId propertyId, InputStream[] values, int propertyType) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            try {
                DavPropertySet setProperties = new DavPropertySet();
                if (values == null) {
                    // setting property value to 'null' is identical to a removal
                    remove(propertyId);
                } else {
                    // qualified values must be converted to jcr values
                    Value[] jcrValues = new Value[values.length];
                    for (int i = 0; i < values.length; i++) {
                        QValue qV = QValue.create(values[i], propertyType);
                        jcrValues[i] = ValueFormat.getJCRValue(qV, nsResolver, valueFactory);
                    }
                    setProperties.add(new ValuesProperty(jcrValues));
                }
                internalSetValue(propertyId, setProperties);
            }   catch (IOException e) {
                throw new ValueFormatException(e);
            }
        }

        /**
         *
         * @param propertyId
         * @param setProperties
         * @throws ValueFormatException
         * @throws VersionException
         * @throws LockException
         * @throws ConstraintViolationException
         * @throws AccessDeniedException
         * @throws UnsupportedRepositoryOperationException
         * @throws RepositoryException
         */
        private void internalSetValue(PropertyId propertyId, DavPropertySet setProperties) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            try {
                String uri = getItemUri(propertyId, sessionInfo);
                PropPatchMethod method = new PropPatchMethod(uri, setProperties, new DavPropertyNameSet());

                methods.add(method);
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }

        /**
         * @see Batch#remove(ItemId)
         */
        public void remove(ItemId itemId) throws VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            String uri = getItemUri(itemId, sessionInfo);
            DeleteMethod method = new DeleteMethod(uri);

            methods.add(method);
        }

        /**
         * @see Batch#reorderNodes(NodeId, NodeId, NodeId)
         */
        public void reorderNodes(NodeId parentId, NodeId srcNodeId, NodeId beforeNodeId) throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException, ItemNotFoundException, LockException, AccessDeniedException, RepositoryException {
            checkConsumed();
            try {
                String srcUri = getItemUri(srcNodeId, sessionInfo);
                String srcSegment = Text.getName(srcUri, true);
                String targetSegment = Text.getName(getItemUri(beforeNodeId, sessionInfo), true);

                String uri = getItemUri(parentId, sessionInfo);
                OrderPatchMethod method = new OrderPatchMethod(uri, srcSegment, targetSegment, true);

                methods.add(method);
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }

        /**
         * @see Batch#setMixins(NodeId, QName[])
         */
        public void setMixins(NodeId nodeId, QName[] mixinNodeTypeIds) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            try {
                DavPropertySet setProperties;
                DavPropertyNameSet removeProperties;
                if (mixinNodeTypeIds == null || mixinNodeTypeIds.length == 0) {
                    setProperties = new DavPropertySet();
                    removeProperties = new DavPropertyNameSet();
                    removeProperties.add(ItemResourceConstants.JCR_MIXINNODETYPES);
                } else {
                    String[] ntNames = new String[mixinNodeTypeIds.length];
                    for (int i = 0; i < mixinNodeTypeIds.length; i++) {
                        ntNames[i] = NameFormat.format(mixinNodeTypeIds[i], nsResolver);
                    }
                    setProperties = new DavPropertySet();
                    setProperties.add(new NodeTypeProperty(ItemResourceConstants.JCR_MIXINNODETYPES, ntNames, false));
                    removeProperties = new DavPropertyNameSet();
                }

                String uri = getItemUri(nodeId, sessionInfo);
                PropPatchMethod method = new PropPatchMethod(uri, setProperties, removeProperties);

                methods.add(method);
            } catch (IOException e) {
                throw new RepositoryException(e);
            } catch (NoPrefixDeclaredException e) {
                // should not occur.
                throw new RepositoryException(e);
            }
        }

        /**
         * @see Batch#move(NodeId, NodeId, QName)
         */
        public void move(NodeId srcNodeId, NodeId destParentNodeId, QName destName) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
            checkConsumed();
            String uri = getItemUri(srcNodeId, sessionInfo);
            String destUri = getItemUri(destParentNodeId, destName, sessionInfo);
            MoveMethod method = new MoveMethod(uri, destUri, true);

            methods.add(method);
        }
    }

    /**
     * <code>SubscriptionManager</code>...
     */
    private class SubscriptionMgrImpl implements SubscriptionManager {

        private SessionInfo sessionInfo;

        private final Map subscriptions = new HashMap();
        private final Object subscriptionsLock = new Object();
        private Map currentSubscriptions;

        private Thread t;

        public void setSessionInfo(SessionInfo sessionInfo) {
            this.sessionInfo = sessionInfo;
        }

        public boolean subscriptionExists(EventListener listener) {
            return getSubscriptions().containsKey(listener);
        }

        public String getSubscriptionId(EventListener listener) {
            if (getSubscriptions().containsKey(listener)) {
                return ((String[]) getSubscriptions().get(listener))[1];
            } else {
                return null;
            }
        }

        public void addSubscription(String uri, String subscriptionId, EventListener listener) {
            synchronized (subscriptionsLock) {
                boolean doStart = subscriptions.isEmpty();
                subscriptions.put(listener, new String[] {uri,subscriptionId});
                currentSubscriptions = null;
                if (doStart) {
                    startPolling();
                }
            }
        }

        public synchronized void removeSubscription(EventListener listener) {
            synchronized (subscriptionsLock) {
                subscriptions.remove(listener);
                currentSubscriptions = null;
                if (subscriptions.isEmpty()) {
                    stopPolling();
                }
            }
        }

        public void dispose() {
            synchronized (subscriptionsLock) {
                if (!subscriptions.isEmpty()) {
                    subscriptions.clear();
                    currentSubscriptions = null;
                    stopPolling();
                }
            }
        }

        private Map getSubscriptions() {
            synchronized (subscriptionsLock) {
                if (currentSubscriptions == null) {
                    currentSubscriptions = Collections.unmodifiableMap(new HashMap(subscriptions));
                }
                return currentSubscriptions;
            }
        }

        private void startPolling() {
            Runnable r = new Runnable() {
                public void run() {
                    while (t == Thread.currentThread()) {
                        try {
                            // sleep
                            Thread.sleep(POLL_INTERVAL);
                            // poll
                            Iterator lstnIterator = getSubscriptions().keySet().iterator();
                            while (lstnIterator.hasNext()) {
                                EventListener listener = (EventListener) lstnIterator.next();
                                String[] value = (String[]) getSubscriptions().get(listener);
                                String uri = value[0];
                                String subscriptionId = value[1];
                                EventIterator eventIterator = poll(uri, subscriptionId, sessionInfo);
                                listener.onEvent(eventIterator);
                            }
                        } catch (InterruptedException e) {
                            log.debug("Polling thread interrupted: " + e.getMessage());
                            return;
                        } catch (RepositoryException e) {
                            log.warn("Polling failed: ", e.getMessage());
                        }
                    }
                }
            };
            t = new Thread(r);
            t.start();
        }

        private void stopPolling() {
            t.interrupt();
        }
    }
}
