/* 
 * Copyright 2002-2004 The Apache Software Foundation
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
package org.apache.cocoon.core.container;

import java.lang.reflect.Method;

import org.apache.avalon.excalibur.logger.LoggerManager;
import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.cocoon.components.ServiceInfo;

/**
 * Factory for Avalon based components.
 *
 * @version CVS $Id$
 */
public class ComponentFactory {
    
    protected final ServiceInfo serviceInfo;
    
    /** The Context for the component
     */
    protected final Context context;

    /** The service manager for this component
     */
    protected final ServiceManager serviceManager;
    
    /** The parameters for this component
     */
    protected Parameters parameters;
    
    protected final Logger logger;
    
    protected final LoggerManager loggerManager;

    protected final RoleManager roleManager;
    
    /**
     * Construct a new component factory for the specified component.
     *
     * @param componentClass the class to instantiate (must have a default constructor).
     * @param configuration the <code>Configuration</code> object to pass to new instances.
     * @param seerviceManager the service manager to pass to <code>Serviceable</code>s.
     * @param context the <code>Context</code> to pass to <code>Contexutalizable</code>s.
     *
     */
    public ComponentFactory( final ServiceManager serviceManager,
                             final Context context,
                             final Logger logger,
                             final LoggerManager loggerManager,
                             final RoleManager roleManager,
                             final ServiceInfo info) {
        this.serviceManager = serviceManager;
        this.context = context;
        this.loggerManager = loggerManager;
        this.roleManager = roleManager;
        this.serviceInfo = info;
        
        Logger actualLogger = logger;
        // If the handler is created "manually" (e.g. XSP engine), loggerManager can be null
        if(loggerManager != null && this.serviceInfo.getConfiguration() != null) {
            final String category = this.serviceInfo.getConfiguration().getAttribute("logger", null);
            if(category != null) {
                actualLogger = loggerManager.getLoggerForCategory(category);
            }
        }
        this.logger = actualLogger;
    }

    /**
     * Create a new instance
     */
    public Object newInstance()
    throws Exception {
        final Object component = this.serviceInfo.getServiceClass().newInstance();

        if( this.logger.isDebugEnabled() ) {
            this.logger.debug( "ComponentFactory creating new instance of " +
                    this.serviceInfo.getServiceClass().getName() + "." );
        }

        ContainerUtil.enableLogging(component, this.logger);
        ContainerUtil.contextualize( component, this.context );
        ContainerUtil.service( component, this.serviceManager );
        ContainerUtil.configure( component, this.serviceInfo.getConfiguration() );

        if( component instanceof Parameterizable ) {
            if ( this.parameters == null ) {
                this.parameters = Parameters.fromConfiguration( this.serviceInfo.getConfiguration() );
            }
            ContainerUtil.parameterize( component, this.parameters );
        }

        ContainerUtil.initialize( component );

        final Method method = this.serviceInfo.getInitMethod();
        if ( method != null ) {
            method.invoke(component, null);
        }

        ContainerUtil.start( component );

        return component;
    }

    public Class getCreatedClass() {
        return this.serviceInfo.getServiceClass();
    }

    /**
     * Destroy an instance
     */
    public void decommission( final Object component )
    throws Exception {
        if( this.logger.isDebugEnabled() ) {
            this.logger.debug( "ComponentFactory decommissioning instance of " +
                    this.serviceInfo.getServiceClass().getName() + "." );
        }

        ContainerUtil.stop( component );
        ContainerUtil.dispose( component );

        final Method method = this.serviceInfo.getDestroyMethod();
        if ( method != null ) {
            method.invoke(component, null);
        }
    }

    /**
     * Handle service specific methods for getting it out of the pool
     */
    public void exitingPool( final Object component )
    throws Exception {
        final Method method = this.serviceInfo.getPoolOutMethod();
        if ( method != null ) {
            method.invoke(component, null);
        }         
    }

    /**
     * Handle service specific methods for putting it into the pool
     */
    public void enteringPool( final Object component )
    throws Exception {
        // Handle Recyclable objects
        if( component instanceof Recyclable ) {
            ( (Recyclable)component ).recycle();
        }
        final Method method = this.serviceInfo.getPoolInMethod();
        if ( method != null ) {
            method.invoke(component, null);
        }         
    }
}
