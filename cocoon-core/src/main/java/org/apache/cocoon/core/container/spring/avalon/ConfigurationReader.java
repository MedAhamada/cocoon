/*
 * Copyright 2006 The Apache Software Foundation
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.core.container.spring.avalon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.cocoon.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.xml.sax.InputSource;

/**
 * This component reads in Avalon style configuration files and returns all
 * contained components and their configurations.
 *
 * @since 2.2
 * @version $Id$
 */
public class ConfigurationReader {

    /** Logger (we use the same logging mechanism as Spring!) */
    protected final Log logger = LogFactory.getLog(getClass());

    /** Resolver for reading configuration files. */
    protected final PathMatchingResourcePatternResolver resolver;

    /** The configuration info. */
    protected final ConfigurationInfo configInfo;

    /** All component configurations. */
    protected final List componentConfigs = new ArrayList();

    /** Is this the root context? */
    protected final boolean isRootContext;

    /**
     * This method reads in an Avalon style configuration.
     * @param source         The location of the configuration.
     * @param resourceLoader The resource loader to load included configs.
     * @return               A configuration containing all defined objects.
     * @throws Exception
     */
    public static ConfigurationInfo readConfiguration(String         source,
                                                      ResourceLoader resourceLoader)
    throws Exception {
        final ConfigurationReader converter = new ConfigurationReader(null, resourceLoader);
        converter.convert(source);
        return converter.configInfo;
    }

    /**
     * This method reads in an Avalon style sitemap.
     * @param source         The location of the sitemap.
     * @param resourceLoader The resource loader to load included configs.
     * @return               A configuration containing all defined objects.
     * @throws Exception
     */
    public static ConfigurationInfo readSitemap(ConfigurationInfo parentInfo,
                                                String            source,
                                                ResourceLoader    resourceLoader)
    throws Exception {
        final ConfigurationReader converter = new ConfigurationReader(parentInfo, resourceLoader);
        converter.convertSitemap(source);
        return converter.configInfo;
    }

    public static ConfigurationInfo readConfiguration(Configuration     config,
                                                      ConfigurationInfo parentInfo,
                                                      ResourceLoader    resourceLoader)
    throws Exception {
        return readConfiguration(config, null, parentInfo, resourceLoader);
    }

    public static ConfigurationInfo readConfiguration(Configuration     rolesConfig,
                                                      Configuration     componentConfig,
                                                      ConfigurationInfo parentInfo,
                                                      ResourceLoader    resourceLoader)
    throws Exception {
        final ConfigurationReader converter = new ConfigurationReader(parentInfo, resourceLoader);
        converter.convert(rolesConfig, componentConfig, null);
        return converter.configInfo;        
    }

    private ConfigurationReader(ConfigurationInfo parentInfo,
                                ResourceLoader    resourceLoader)
    throws Exception {
        this.isRootContext = parentInfo == null;
        if ( resourceLoader != null ) {
            this.resolver = new PathMatchingResourcePatternResolver(resourceLoader);
        } else {
            this.resolver = new PathMatchingResourcePatternResolver();
        }

        // now add selectors from parent
        if ( parentInfo != null ) {
            this.configInfo = new ConfigurationInfo(parentInfo);
            final Iterator i = parentInfo.getComponents().values().iterator();
            while ( i.hasNext() ) {
                final ComponentInfo current = (ComponentInfo)i.next();
                if ( current.isSelector() ) {
                    this.configInfo.addRole(current.getRole(), current.copy());
                }
            }
            // TODO - we should add the processor to each container
            //        This would avoid the hacky getting of the current container in the tree processor
            /*
            ComponentInfo processorInfo = (ComponentInfo) parentInfo.getComponents().get(Processor.ROLE);
            if ( processorInfo != null ) {
                this.configInfo.getComponents().put(Processor.ROLE, processorInfo.copy());
            }
            */
        } else {
            this.configInfo = new ConfigurationInfo();
        }
    }

    protected String convertUrl(String url) {
        if ( url == null ) {
            return null;
        }
        if ( url.startsWith("context:") ) {
            return url.substring(10);
        }
        if ( url.startsWith("resource:") ) {
            return "classpath:" + url.substring(10);
        }
        if ( url.indexOf(':') == -1 && !url.startsWith("/")) {
            return '/' + url;
        }
        return url;
    }

    protected InputSource getInputSource(Resource rsrc)
    throws Exception {
        final InputSource is = new InputSource(rsrc.getInputStream());
        is.setSystemId(rsrc.getURL().toExternalForm());
        return is;
    }

    protected String getUrl(String url, String base) {
        if ( url == null ) {
            return url;
        }
        if ( base != null && url.indexOf(":/") < 2) {
            // TODO - we have to check if url is relative or absolute
            System.out.println("URL: " + url + " (" + base + ")");            
        }
        return this.convertUrl(url);
    }

    protected void convert(String relativePath)
    throws Exception {
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("Reading Avalon configuration from " + relativePath);
        }
        Resource root = this.resolver.getResource(this.convertUrl(relativePath));
        final DefaultConfigurationBuilder b = new DefaultConfigurationBuilder(true);
        
        final Configuration config = b.build(this.getInputSource(root));
        // validate cocoon.xconf
        if (!"cocoon".equals(config.getName())) {
            throw new ConfigurationException("Invalid configuration file\n"
                    + config.toString());
        }
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Configuration version: " + config.getAttribute("version"));
        }
        if (!Constants.CONF_VERSION.equals(config.getAttribute("version"))) {
            throw new ConfigurationException(
                    "Invalid configuration schema version. Must be '"
                            + Constants.CONF_VERSION + "'.");
        }            
        this.convert(config, null, root.getURL().toExternalForm());
    }

    protected void convertSitemap(String sitemapLocation)
    throws Exception {
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("Reading sitemap from " + sitemapLocation);
        }
        Resource root = this.resolver.getResource(this.convertUrl(sitemapLocation));
        final DefaultConfigurationBuilder b = new DefaultConfigurationBuilder(true);
        
        final Configuration config = b.build(this.getInputSource(root));
        // validate cocoon.xconf
        if (!"sitemap".equals(config.getName())) {
            throw new ConfigurationException("Invalid sitemap\n"
                    + config.toString());
        }
        final Configuration completeConfig = SitemapHelper.createSitemapConfiguration(config);
        if ( completeConfig != null ) {
            this.convert(config, null, root.getURL().toExternalForm());
        }
    }

    protected void convert(Configuration config, Configuration additionalConfig, String rootUri)
    throws Exception {
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("Converting Avalon configuration from configuration object: " + config);
        }
        // It's possible to define a logger on a per sitemap/service manager base.
        // This is the default logger for all components defined with this sitemap/manager.
        this.configInfo.setRootLogger(config.getAttribute("logger", null));

        // and load configuration with a empty list of loaded configurations
        final Set loadedConfigs = new HashSet();
        // what is it?
        if ( "role-list".equals(config.getName()) || "roles".equals(config.getName())) {
            this.configureRoles(config);
        } else {
            this.parseConfiguration(config, null, loadedConfigs);
        }

        // test for optional user-roles attribute
        if ( rootUri != null ) {
            final String userRoles = config.getAttribute("user-roles", null);
            if (userRoles != null) {
                if ( this.logger.isDebugEnabled() ) {
                    this.logger.debug("Reading additional user roles: " + userRoles);
                }
                final Resource userRolesSource = this.resolver.getResource(this.getUrl(userRoles, rootUri));
                final DefaultConfigurationBuilder b = new DefaultConfigurationBuilder(true);
                final Configuration userRolesConfig = b.build(this.getInputSource(userRolesSource));
                this.parseConfiguration(userRolesConfig, userRolesSource.getURL().toExternalForm(), loadedConfigs);
            }
        }
        if ( additionalConfig != null ) {
            if ( "role-list".equals(additionalConfig.getName()) || "roles".equals(additionalConfig.getName())) {
                this.configureRoles(additionalConfig);
            } else {
                this.parseConfiguration(additionalConfig, null, loadedConfigs);
            }
        }

        // now process all component configurations
        this.processComponents();

        // add roles as components
        final Iterator i = this.configInfo.getRoles().iterator();
        while ( i.hasNext() ) {
            final ComponentInfo current = (ComponentInfo)i.next();
            current.setLazyInit(true);
            this.configInfo.addComponent(current);
        }
        this.configInfo.clearRoles();
    }

    protected void parseConfiguration(final Configuration configuration,
                                      String              contextURI,
                                      Set                 loadedURIs) 
    throws ConfigurationException {
        final Configuration[] configurations = configuration.getChildren();

        for( int i = 0; i < configurations.length; i++ ) {
            final Configuration componentConfig = configurations[i];

            final String componentName = componentConfig.getName();

            if ("include".equals(componentName)) {
                this.handleInclude(contextURI, loadedURIs, componentConfig);
            } else if ( "include-beans".equals(componentName) ) {
                this.handleBeanInclude(contextURI, componentConfig);
                // we ignore include-properties if this is a child context
            } else if ( this.isRootContext || !"include-properties".equals(componentName) ) {
                // Component declaration, add it to list
                this.componentConfigs.add(componentConfig);
            }
        }
    }

    protected void processComponents()
    throws ConfigurationException {
        final Iterator i = this.componentConfigs.iterator();
        while ( i.hasNext() ) {
            final Configuration componentConfig = (Configuration)i.next(); 
            final String componentName = componentConfig.getName();

            // Find the role
            String role = componentConfig.getAttribute("role", null);
            String alias = null;
            if (role == null) {
                // Get the role from the role manager if not explicitely specified
                role = (String)this.configInfo.getShorthands().get( componentName );
                alias = componentName;
                if (role == null) {
                    // Unknown role
                    throw new ConfigurationException("Unknown component type '" + componentName +
                        "' at " + componentConfig.getLocation());
                }
            }
    
            // Find the className
            String className = componentConfig.getAttribute("class", null);
            // If it has a "name" attribute, add it to the role (similar to the
            // declaration within a service selector)
            // Note: this has to be done *after* finding the className above as we change the role
            String name = componentConfig.getAttribute("name", null);
            ComponentInfo info;
            if (className == null) {
                // Get the default class name for this role
                info = this.configInfo.getRole( role );
                if (info == null) {
                    if ( this.configInfo.getComponents().get( role) != null ) {
                        throw new ConfigurationException("Duplicate component definition for role " + role + " at " + componentConfig.getLocation());
                    }
                    throw new ConfigurationException("Cannot find a class for role " + role + " at " + componentConfig.getLocation());
                }
                className = info.getComponentClassName();
                if ( name != null ) {
                    info = info.copy();                    
                } else if ( !className.endsWith("Selector") ) {
                    this.configInfo.removeRole(role);
                }
            } else {                    
                info = new ComponentInfo();
                if ( !className.endsWith("Selector") ) {
                    this.configInfo.removeRole(role);
                }
            }
            // check for name attribute
            // Note: this has to be done *after* finding the className above as we change the role
            if (name != null) {
                role = role + "/" + name;
                if ( alias != null ) {
                    alias = alias + '-' + name;
                }
            }
            info.fill(componentConfig);
            info.setComponentClassName(className);
            info.setRole(role);
            if ( alias != null ) {
                info.setAlias(alias);
            }
            info.setConfiguration(componentConfig);

            this.configInfo.addComponent(info);
            // now if this is a selector, then we have to register the single components
            if ( info.getConfiguration() != null && className.endsWith("Selector") ) {
                String classAttribute = null;
                if ( className.equals("org.apache.cocoon.core.container.DefaultServiceSelector") ) {
                    classAttribute = "class";
                } else if (className.equals("org.apache.cocoon.components.treeprocessor.sitemap.ComponentsSelector") ) {
                    classAttribute = "src";
                } 
                if ( classAttribute == null ) {
                    this.logger.warn("Found unknown selector type (continuing anyway: " + className);
                } else {
                    String componentRole = role;
                    if ( componentRole.endsWith("Selector") ) {
                        componentRole = componentRole.substring(0, componentRole.length() - 8);
                    }
                    componentRole += '/';
                    Configuration[] children = info.getConfiguration().getChildren();
                    final Map hintConfigs = (Map)this.configInfo.getKeyClassNames().get(role);                       
                    for (int j=0; j<children.length; j++) {
                        final Configuration current = children[j];
                        final ComponentInfo childInfo = new ComponentInfo();
                        childInfo.fill(current);
                        childInfo.setConfiguration(current);
                        final ComponentInfo hintInfo = (hintConfigs == null ? null : (ComponentInfo)hintConfigs.get(current.getName()));
                        if ( current.getAttribute(classAttribute, null ) != null 
                             || hintInfo == null ) {
                            childInfo.setComponentClassName(current.getAttribute(classAttribute));
                        } else {
                            childInfo.setComponentClassName(hintInfo.getComponentClassName());
                        }
                        childInfo.setRole(componentRole + current.getAttribute("name"));
                        this.configInfo.addComponent(childInfo);
                    }
                }
            }
        }        
    }

    protected void handleInclude(final String        contextURI,
                                 final Set           loadedURIs,
                                 final Configuration includeStatement)
    throws ConfigurationException {
        final String includeURI = includeStatement.getAttribute("src", null);
        String directoryURI = null;
        if ( includeURI == null ) {
            // check for directories
            directoryURI = includeStatement.getAttribute("dir", null);                    
        }
        if ( includeURI == null && directoryURI == null ) {
            throw new ConfigurationException("Include statement must either have a 'src' or 'dir' attribute, at " +
                    includeStatement.getLocation());
        }

        if ( includeURI != null ) {
            Resource src = null;
            try {
                src = this.resolver.getResource(this.getUrl(includeURI, contextURI));

                this.loadURI(src, loadedURIs, includeStatement);
            } catch (Exception e) {
                throw new ConfigurationException("Cannot load '" + includeURI + "' at " + includeStatement.getLocation(), e);
            }
            
        } else {
            // test if directory exists
            Resource dirResource = this.resolver.getResource(this.convertUrl(directoryURI));
            if ( dirResource.exists() ) {
                final String pattern = includeStatement.getAttribute("pattern", null);
                try {
                    Resource[] resources = this.resolver.getResources(this.getUrl(directoryURI + '/' + pattern, contextURI));
                    if ( resources != null ) {
                        for(int i=0; i < resources.length; i++) {
                           this.loadURI(resources[i], loadedURIs, includeStatement);
                        }
                    }
                } catch (Exception e) {
                    throw new ConfigurationException("Cannot load from directory '" + directoryURI + "' at " + includeStatement.getLocation(), e);
                }
            } else {
                if ( !includeStatement.getAttributeAsBoolean("optional", false) ) {
                    throw new ConfigurationException("Directory '" + directoryURI + "' does not exist (" + includeStatement.getLocation() + ").");
                }
            }
        }
    }

    protected void loadURI(final Resource      src,
                           final Set           loadedURIs,
                           final Configuration includeStatement) 
    throws ConfigurationException, IOException {
        // If already loaded: do nothing
        final String uri = src.getURL().toExternalForm();

        if (!loadedURIs.contains(uri)) {
            if ( this.logger.isDebugEnabled() ) {
                this.logger.debug("Loading configuration: " + uri);
            }
            // load it and store it in the read set
            Configuration includeConfig = null;
            try {
                DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder(true);
                includeConfig = builder.build(src.getInputStream(), uri);
            } catch (Exception e) {
                throw new ConfigurationException("Cannot load '" + uri + "' at " + includeStatement.getLocation(), e);
            }
            loadedURIs.add(uri);

            // what is it?
            final String includeKind = includeConfig.getName();
            if (includeKind.equals("components")) {
                // more components
                this.parseConfiguration(includeConfig, uri, loadedURIs);
            } else if (includeKind.equals("role-list")) {
                // more roles
                this.configureRoles(includeConfig);
            } else {
                throw new ConfigurationException("Unknow document '" + includeKind + "' included at " +
                        includeStatement.getLocation());
            }
        }
    }

    protected void handleBeanInclude(final String contextURI,
                                     final Configuration includeStatement)
    throws ConfigurationException {
        final String includeURI = includeStatement.getAttribute("src", null);
        String directoryURI = null;
        if (includeURI == null) {
            // check for directories
            directoryURI = includeStatement.getAttribute("dir", null);
        }
        if (includeURI == null && directoryURI == null) {
            throw new ConfigurationException(
                    "Include statement must either have a 'src' or 'dir' attribute, at "
                            + includeStatement.getLocation());
        }

        if (includeURI != null) {
            Resource src = null;
            try {
                src = this.resolver.getResource(this.getUrl(includeURI, contextURI));

                this.configInfo.addImport(src.getURL().toExternalForm());
            } catch (Exception e) {
                throw new ConfigurationException("Cannot load '" + includeURI + "' at "
                        + includeStatement.getLocation(), e);
            }

        } else {
            // test if directory exists
            Resource dirResource = this.resolver.getResource(this.convertUrl(directoryURI));
            if ( dirResource.exists() ) {
                final String pattern = includeStatement.getAttribute("pattern", null);
                try {
                    Resource[] resources = this.resolver.getResources(this.getUrl(directoryURI + '/' + pattern, contextURI));
                    if ( resources != null ) {
                        for(int i=0; i < resources.length; i++) {
                           this.configInfo.addImport(resources[i].getURL().toExternalForm());
                        }
                    }
                } catch (IOException ioe) {
                    throw new ConfigurationException("Unable to read configurations from "
                            + directoryURI);
                }
            } else {
                if ( !includeStatement.getAttributeAsBoolean("optional", false) ) {
                    throw new ConfigurationException("Directory '" + directoryURI + "' does not exist (" + includeStatement.getLocation() + ").");
                }
            }
        }
    }

    /**
     * Reads a configuration object and creates the role, shorthand,
     * and class name mapping.
     *
     * @param configuration  The configuration object.
     * @throws ConfigurationException if the configuration is malformed
     */
    protected final void configureRoles( final Configuration configuration )
    throws ConfigurationException {
        final Configuration[] roles = configuration.getChildren();
        for( int i = 0; i < roles.length; i++ ) {
            final Configuration role = roles[i];

            if ( "alias".equals(role.getName()) ) {
                final String roleName = role.getAttribute("role");
                final String shorthand = role.getAttribute("shorthand");
                this.configInfo.getShorthands().put(shorthand, roleName);
                continue;
            }
            if (!"role".equals(role.getName())) {
                throw new ConfigurationException("Unexpected '" + role.getName() + "' element at " + role.getLocation());
            }

            final String roleName = role.getAttribute("name");
            final String shorthand = role.getAttribute("shorthand", null);
            final String defaultClassName = role.getAttribute("default-class", null);

            if (shorthand != null) {
                // Store the shorthand and check that its consistent with any previous one
                Object previous = this.configInfo.getShorthands().put( shorthand, roleName );
                if (previous != null && !previous.equals(roleName)) {
                    throw new ConfigurationException("Shorthand '" + shorthand + "' already used for role " +
                            previous + ": inconsistent declaration at " + role.getLocation());
                }
            }

            if ( defaultClassName != null ) {
                ComponentInfo info = this.configInfo.getRole(roleName);
                if (info == null) {
                    // Create a new info and store it
                    info = new ComponentInfo();
                    info.setComponentClassName(defaultClassName);
                    info.fill(role);
                    info.setRole(roleName);
                    info.setConfiguration(role);
                    info.setAlias(shorthand);
                    this.configInfo.addRole(roleName, info);
                } else {
                    // Check that it's consistent with the existing info
                    if (!defaultClassName.equals(info.getComponentClassName())) {
                        throw new ConfigurationException("Invalid redeclaration: default class already set to " + info.getComponentClassName() +
                                " for role " + roleName + " at " + role.getLocation());
                    }
                    //FIXME: should check also other ServiceInfo members
                }
            }

            final Configuration[] keys = role.getChildren( "hint" );
            if( keys.length > 0 ) {
                Map keyMap = (Map)this.configInfo.getKeyClassNames().get(roleName);
                if (keyMap == null) {
                    keyMap = new HashMap();
                    this.configInfo.getKeyClassNames().put(roleName, keyMap);
                }

                for( int j = 0; j < keys.length; j++ ) {
                    Configuration key = keys[j];
                    
                    final String shortHand = key.getAttribute( "shorthand" ).trim();
                    final String className = key.getAttribute( "class" ).trim();

                    ComponentInfo info = (ComponentInfo)keyMap.get(shortHand);
                    if (info == null) {       
                        info = new ComponentInfo();
                        info.setComponentClassName(className);
                        info.fill(key);
                        info.setConfiguration(key);
                        info.setAlias(shortHand);
                        keyMap.put( shortHand, info );
                    } else {
                        // Check that it's consistent with the existing info
                        if (!className.equals(info.getComponentClassName())) {
                            throw new ConfigurationException("Invalid redeclaration: class already set to " + info.getComponentClassName() +
                                    " for hint " + shortHand + " at " + key.getLocation());
                        }
                        //FIXME: should check also other ServiceInfo members
                    }
                }
            }
        }
    }
}
