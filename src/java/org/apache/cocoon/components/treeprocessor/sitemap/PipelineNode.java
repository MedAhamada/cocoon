/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Apache Cocoon" and  "Apache Software Foundation" must  not  be
    used to  endorse or promote  products derived from  this software without
    prior written permission. For written permission, please contact
    apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation and was  originally created by
 Stefano Mazzocchi  <stefano@apache.org>. For more  information on the Apache
 Software Foundation, please see <http://www.apache.org/>.

*/
package org.apache.cocoon.components.treeprocessor.sitemap;

import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.cocoon.ConnectionResetException;
import org.apache.cocoon.Constants;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.components.notification.Notifying;
import org.apache.cocoon.components.notification.NotifyingBuilder;
import org.apache.cocoon.components.treeprocessor.AbstractParentProcessingNode;
import org.apache.cocoon.components.treeprocessor.InvokeContext;
import org.apache.cocoon.components.treeprocessor.ParameterizableProcessingNode;
import org.apache.cocoon.components.treeprocessor.ProcessingNode;
import org.apache.cocoon.environment.Environment;
import org.apache.cocoon.environment.ObjectModelHelper;

import java.util.Map;

/**
 * Handles &lt;map:pipeline&gt;
 *
 * @author <a href="mailto:sylvain@apache.org">Sylvain Wallez</a>
 * @author <a href="mailto:gianugo@apache.org">Gianugo Rabellino</a>
 * @author <a href="mailto:cziegeler@apache.org">Carsten Ziegeler</a>
 * @version CVS $Id: PipelineNode.java,v 1.3 2003/04/07 08:21:30 sylvain Exp $
 */
public class PipelineNode
    extends AbstractParentProcessingNode
    implements Composable, ParameterizableProcessingNode {

    // TODO : handle a 'fail-hard' environment attribute
    // can be useful to stop off-line generation when there's an error

    private ProcessingNode[] children;

    private ProcessingNode error404;

    private ProcessingNode error500;
    
    private ComponentManager manager;

    private boolean internalOnly = false;

    /** Is it the last <pipeline> in the enclosing <pipelines> ? */
    private boolean isLast = false;

    /** The component name of the processing pipeline */
    protected String processingPipeline;

    /** Optional Sitemap parameters */
    protected Map parameters;

    /**
     * A constructor to receive the optional expires parameter
     * and optional parameters for the processing pipeline
     */
    public PipelineNode(Configuration config) {
        this.processingPipeline = config.getAttribute("type", null);
    }

    /**
     * The component manager is used to create error pipelines
     */
    public void compose(ComponentManager manager) {
        this.manager = manager;
    }

    public void setChildren(ProcessingNode[] nodes) {
        this.children = nodes;
    }

    public void setParameters(Map parameterMap) {
        this.parameters = parameterMap;
    }

    public void setLast(boolean isLast) {
        this.isLast = isLast;
    }

    public void set404Handler(ProcessingNode node) {
        this.error404 = node;
    }

    public void set500Handler(ProcessingNode node) {
        this.error500 = node;
    }

    public void setInternalOnly(boolean internalOnly) {
        this.internalOnly = internalOnly;
    }

    public final boolean invoke(Environment env, InvokeContext context)
    throws Exception {

        // Always fail on external resquests if internal only.
        if (this.internalOnly && !context.isInternalRequest()) {
            return false;
        }

        context.inform(this.processingPipeline, this.parameters, env.getObjectModel());

        try {
            if (invokeNodes(children, env, context)) {
                return true;
            } else if (this.isLast) {
                String msg = "No pipeline matched request: " + env.getURIPrefix() + env.getURI();
                throw new ResourceNotFoundException(msg);
            } else {
                return false;
            }
        } catch (ConnectionResetException cre) {
            // Will be reported by CocoonServlet, rethrowing
            throw cre;
            
        } catch(Exception ex) {
            
            if (context.isInternalRequest()) {
                // Propagate exception on internal requests
                throw ex;
                
            } else if (error404 != null && ex instanceof ResourceNotFoundException) {
			    // Invoke 404-specific handler
			    return invokeErrorHandler(error404, ex, env);
			    
			} else if (error500 != null) {
                // Invoke global handler
                return invokeErrorHandler(error500, ex, env);
                
			} else {
			    // No handler : propagate
			    throw ex;
			}
        }
    }

    private boolean invokeErrorHandler(ProcessingNode node, Exception ex, Environment env)
    throws Exception {
        InvokeContext errorContext = null;

        try {
            // Try to reset the response to avoid mixing already produced output
            // and error page.
            env.tryResetResponse();

            // Build a new context
            errorContext = new InvokeContext();
            errorContext.enableLogging(getLogger());
            errorContext.compose(this.manager);

            // Create a Notifying
            NotifyingBuilder notifyingBuilder= (NotifyingBuilder)this.manager.lookup(NotifyingBuilder.ROLE);
            Notifying currentNotifying = null;
            try {
                currentNotifying = notifyingBuilder.build(this, ex);
            } finally {
                this.manager.release(notifyingBuilder);
            }

			Map objectModel = env.getObjectModel();
            // Add it to the object model
            objectModel.put(Constants.NOTIFYING_OBJECT, currentNotifying);
            
            // Also add the exception
            objectModel.put(ObjectModelHelper.THROWABLE_OBJECT, ex);

            // <notifier> is added in HandleErrorsNode
            return node.invoke(env, errorContext);
        } catch (Exception subEx) {
            getLogger().error("An exception occured in while handling errors at " + node.getLocation(), subEx);
            // Rethrow it : it will either be handled by the parent sitemap or by the environment (e.g. Cocoon servlet)
            throw subEx;
        } finally {
            if (errorContext != null) {
                errorContext.dispose();
            }
        }
    }

}

