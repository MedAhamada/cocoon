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
package org.apache.cocoon.core.container.spring;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.cocoon.configuration.Settings;
import org.apache.cocoon.configuration.SettingsDefaults;
import org.apache.cocoon.configuration.impl.MutableSettings;
import org.apache.cocoon.configuration.impl.PropertyHelper;
import org.apache.cocoon.core.CoreInitializationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.ServletContextResourceLoader;

/**
 * This is a bean factory post processor which handles all the settings stuff
 * for Cocoon. It reads in all properties files and replaces references to
 * them in the spring configuration files.
 * In addition this bean acts as a factory bean providing the settings object.
 *
 * @since 2.2
 * @version $Id$
 */
public class SettingsBeanFactoryPostProcessor
    extends PropertyPlaceholderConfigurer
    implements ServletContextAware, BeanFactoryPostProcessor, FactoryBean {

    /** Logger (we use the same logging mechanism as Spring!) */
    protected final Log logger = LogFactory.getLog(getClass());

    protected ServletContext servletContext;

    protected Settings settings;

    /**
     * @see org.springframework.web.context.ServletContextAware#setServletContext(javax.servlet.ServletContext)
     */
    public void setServletContext(ServletContext sContext) {
        this.servletContext = sContext;
    }

    public void init()
    throws Exception {
        final MutableSettings s = this.createSettings();
        this.initSettingsFiles(s);
        // update configuration
        final URL u = this.getConfigFile(s.getConfiguration());
        s.setConfiguration(u.toExternalForm());

        // settings can't be changed anymore
        s.makeReadOnly();
        this.settings = s;
    }

    /**
     * Get the URL of the main Cocoon configuration file.
     */
    protected URL getConfigFile(final String configFileName)
    throws Exception {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Using configuration file: " + configFileName);
        }

        URL result;
        try {
            // test if this is a qualified url
            if (configFileName.indexOf(':') == -1) {
                result = this.servletContext.getResource(configFileName);
            } else {
                result = new URL(configFileName);
            }
        } catch (Exception mue) {
            String msg = "Setting for 'configuration' is invalid : " + configFileName;
            this.logger.error(msg, mue);
            throw new CoreInitializationException(msg, mue);
        }

        if (result == null) {
            File resultFile = new File(configFileName);
            if (resultFile.isFile()) {
                try {
                    result = resultFile.getCanonicalFile().toURL();
                } catch (Exception e) {
                    String msg = "Setting for 'configuration' is invalid : " + configFileName;
                    this.logger.error(msg, e);
                    throw new CoreInitializationException(msg, e);
                }
            }
        }

        if (result == null) {
            String msg = "Setting for 'configuration' doesn't name an existing resource : " + configFileName;
            this.logger.error(msg);
            throw new CoreInitializationException(msg);
        }
        return result;
    }

    /**
     * Init work, upload and cache directory
     * @param settings 
     * @param log 
     */
    protected void initSettingsFiles(MutableSettings settings) {
        // first init the work-directory for the logger.
        // this is required if we are running inside a war file!
        final String workDirParam = settings.getWorkDirectory();
        File workDir;
        if (workDirParam != null) {
            // No context path : consider work-directory as absolute
            workDir = new File(workDirParam);
        } else {
            workDir = new File("cocoon-files");
        }
        workDir.mkdirs();
        settings.setWorkDirectory(workDir.getAbsolutePath());

        // Output some debug info
        if (this.logger.isDebugEnabled()) {
            if (workDirParam != null) {
                this.logger.debug("Using work-directory " + workDir);
            } else {
                this.logger.debug("Using default work-directory " + workDir);
            }
        }

        final String uploadDirParam = settings.getUploadDirectory();
        File uploadDir;
        if (uploadDirParam != null) {
            uploadDir = new File(uploadDirParam);
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Using upload-directory " + uploadDir);
            }
        } else {
            uploadDir = new File(workDir, "upload-dir" + File.separator);
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Using default upload-directory " + uploadDir);
            }
        }
        uploadDir.mkdirs();
        settings.setUploadDirectory(uploadDir.getAbsolutePath());

        String cacheDirParam = settings.getCacheDirectory();
        File cacheDir;
        if (cacheDirParam != null) {
            cacheDir = new File(cacheDirParam);
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Using cache-directory " + cacheDir);
            }
        } else {
            cacheDir = new File(workDir, "cache-dir" + File.separator);
            File parent = cacheDir.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("cache-directory was not set - defaulting to " + cacheDir);
            }
        }
        cacheDir.mkdirs();
        settings.setCacheDirectory(cacheDir.getAbsolutePath());
    }

    /**
     * Get the settings for Cocoon.
     * This method reads several property files and merges the result. If there
     * is more than one definition for a property, the last one wins.
     * The property files are read in the following order:
     * 1) context://WEB-INF/cocoon/properties/*.properties
     *    Default values for the core and each block - the order in which the files are read is not guaranteed.
     * 2) context://WEB-INF/cocoon/properties/[RUNNING_MODE]/*.properties
     *    Default values for the running mode - the order in which the files are read is not guaranteed.
     * 4) Working directory from servlet context (if not already set)
     * 5) Optional property file which is stored under ".cocoon/settings.properties" in the user
     *    directory.
     * 6) Additional property file specified by the "org.apache.cocoon.settings" property.
     * 7) System properties
     *
     * This means that system properties (provided on startup of the web application) override all
     * others etc.
     *
     * @return A new Settings object
     */
    public MutableSettings createSettings() {
        // get the running mode
        final String mode = getSystemProperty(Settings.PROPERTY_RUNNING_MODE, SettingsDefaults.DEFAULT_RUNNING_MODE);
        if ( !Arrays.asList(SettingsDefaults.RUNNING_MODES).contains(mode) ) {
            final String msg =
                "Invalid running mode: " + mode + " - Use one of: " + Arrays.asList(SettingsDefaults.RUNNING_MODES);
            servletContext.log(msg);
            throw new IllegalArgumentException(msg);
        }
        servletContext.log("Running in mode: " + mode);

        // create an empty settings objects
        final MutableSettings s = new MutableSettings(mode);
        // create an empty properties object
        final Properties properties = new Properties();

        // now read all properties from the properties directory
        readProperties("/WEB-INF/cocoon/properties", s, properties);
        // read all properties from the mode dependent directory
        readProperties("/WEB-INF/cocoon/properties/" + mode, s, properties);

        // fill from the servlet context
        if ( s.getWorkDirectory() == null ) {
            final File workDir = (File)this.servletContext.getAttribute("javax.servlet.context.tempdir");
            s.setWorkDirectory(workDir.getAbsolutePath());
        }

        // read additional properties file
        // first try in home directory
        final String homeDir = getSystemProperty("user.home");
        if ( homeDir != null ) {
            final String fileName = homeDir + File.separator + ".cocoon" + File.separator + "settings.properties";
            final File testFile = new File(fileName);
            if ( testFile.exists() ) {
                servletContext.log("Reading user settings from '" + fileName + "'");
                try {
                    final FileInputStream fis = new FileInputStream(fileName);
                    properties.load(fis);
                } catch (IOException ignore) {
                    servletContext.log("Unable to read '" + fileName + "' - continuing with initialization.");
                    this.logger.debug("Unable to read '" + fileName + "' - continuing with initialization.", ignore);
                }
            }
        }
        // check for additionally specified custom file        
        String additionalPropertyFile = s.getProperty(Settings.PROPERTY_USER_SETTINGS, 
                                                      getSystemProperty(Settings.PROPERTY_USER_SETTINGS));
        if ( additionalPropertyFile != null ) {
            servletContext.log("Reading user settings from '" + additionalPropertyFile + "'");
            try {
                final FileInputStream fis = new FileInputStream(additionalPropertyFile);
                properties.load(fis);
                fis.close();
            } catch (IOException ignore) {
                servletContext.log("Unable to read '" + additionalPropertyFile + "' - continuing with initialization.");
                this.logger.debug("Unable to read '" + additionalPropertyFile + "' - continuing with initialization.", ignore);
            }
        }
        // now overwrite with system properties
        try {
            properties.putAll(System.getProperties());
        } catch (SecurityException se) {
            // we ignore this
        }
        PropertyHelper.replaceAll(properties, null);
        s.configure(properties);

        return s;
    }

    /**
     * Read all property files from the given directory and apply them to the settings.
     */
    public void readProperties(String          directoryName,
                               Settings        s,
                               Properties      properties) {
        final String pattern = directoryName + "/*.properties";
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(new ServletContextResourceLoader(this.servletContext));
        Resource[] resources = null;
        try {
            resources = resolver.getResources(pattern);
        } catch (IOException ignore) {
            this.servletContext.log("Unable to read properties from directory '" + directoryName + "' - Continuing initialization.");
            this.logger.debug("Unable to read properties from directory '" + directoryName + "' - Continuing initialization.", ignore);
        }
        if ( resources != null ) {
            final List propertyUris = new ArrayList();
            for(int i=0; i<resources.length; i++ ) {
                propertyUris.add(resources[i]);
            }
            // sort
            Collections.sort(propertyUris, getResourceComparator());
            // now process
            final Iterator i = propertyUris.iterator();
            while ( i.hasNext() ) {
                Resource src = null;
                try {
                    src = (Resource)i.next();
                    final InputStream propsIS = src.getInputStream();
                    this.servletContext.log("Reading settings from '" + src.getURL() + "'.");
                    properties.load(propsIS);
                    propsIS.close();
                } catch (IOException ignore) {
                    this.servletContext.log("Unable to read properties from file '" + src.getDescription() + "' - Continuing initialization.");
                    this.logger.debug("Unable to read properties from file '" + src.getDescription() + "' - Continuing initialization.", ignore);
                }
            }
        }
    }

    /**
     * Return a resource comparator
     */
    public static Comparator getResourceComparator() {
        return new ResourceComparator();
    }

    protected static String getSystemProperty(String key) {
        return getSystemProperty(key, null);
    }

    protected static String getSystemProperty(String key, String defaultValue) {
        try {
            return System.getProperty(key, defaultValue);
        } catch (SecurityException se) {
            // we ignore this
            return defaultValue;
        }
    }

    protected final static class ResourceComparator implements Comparator {

        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object o1, Object o2) {
            if ( !(o1 instanceof Resource) || !(o2 instanceof Resource)) {
                return 0;
            }
            return ((Resource)o1).getFilename().compareTo(((Resource)o2).getFilename());
        }
    }

    /**
     * @see org.springframework.beans.factory.config.PropertyPlaceholderConfigurer#processProperties(org.springframework.beans.factory.config.ConfigurableListableBeanFactory, java.util.Properties)
     */
    protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess,
                                     Properties props)
    throws BeansException {
        final BeanDefinitionVisitor visitor = new CocoonSettingsResolvingBeanDefinitionVisitor(this.settings);
        String[] beanNames = beanFactoryToProcess.getBeanDefinitionNames();
        for (int i = 0; i < beanNames.length; i++) {
            BeanDefinition bd = beanFactoryToProcess.getBeanDefinition(beanNames[i]);
            try {
                visitor.visitBeanDefinition(bd);
            } catch (BeanDefinitionStoreException ex) {
                throw new BeanDefinitionStoreException(bd
                        .getResourceDescription(), beanNames[i], ex
                        .getMessage());
            }
        }
    }

    protected class CocoonSettingsResolvingBeanDefinitionVisitor
        extends BeanDefinitionVisitor {

        protected final Properties props;

        public CocoonSettingsResolvingBeanDefinitionVisitor(Settings settings) {
            this.props = new SettingsProperties(settings);
        }

        protected String resolveStringValue(String strVal) {
            return parseStringValue(strVal, this.props, null);
        }
    }

    protected static class SettingsProperties extends Properties {

        protected final Settings settings;

        public SettingsProperties(Settings s) {
            this.settings = s;
        }

        /**
         * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
         */
        public String getProperty(String key, String defaultValue) {
            return this.settings.getProperty(key, defaultValue);
        }

        /**
         * @see java.util.Properties#getProperty(java.lang.String)
         */
        public String getProperty(String key) {
            return this.settings.getProperty(key);
        }
        
    }

    /**
     * @see org.springframework.beans.factory.FactoryBean#getObject()
     */
    public Object getObject() throws Exception {
        return this.settings;
    }

    /**
     * @see org.springframework.beans.factory.FactoryBean#getObjectType()
     */
    public Class getObjectType() {
        return Settings.class;
    }

    /**
     * @see org.springframework.beans.factory.FactoryBean#isSingleton()
     */
    public boolean isSingleton() {
        return true;
    }
}
