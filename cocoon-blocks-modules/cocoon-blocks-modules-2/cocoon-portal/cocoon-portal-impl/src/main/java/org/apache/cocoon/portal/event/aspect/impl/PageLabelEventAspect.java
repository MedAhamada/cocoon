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
package org.apache.cocoon.portal.event.aspect.impl;

import java.util.Iterator;

import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.portal.impl.PageLabelManager;
import org.apache.cocoon.portal.event.Event;
import org.apache.cocoon.portal.event.EventManager;
import org.apache.cocoon.portal.event.aspect.EventAspect;
import org.apache.cocoon.portal.event.aspect.EventAspectContext;
import org.apache.cocoon.util.AbstractLogEnabled;

/**
 * Converts links generated by the PageLabelLinkService into events and publishes them.
 * Used in conjunction with the PageLabelLinkService, links generated from the layout
 * portal.xml will be based upon the names of the named items.
 *
 * @version $Id$
 */
public class PageLabelEventAspect
    extends AbstractLogEnabled
	implements EventAspect, ThreadSafe, Serviceable, Disposable {

    protected ServiceManager manager;

    protected PageLabelManager labelManager;

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager aManager) throws ServiceException {
        this.manager = aManager;
        this.labelManager = (PageLabelManager)this.manager.lookup(PageLabelManager.ROLE);
    }

    /**
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    public void dispose() {
        if (this.manager != null) {
            if (this.labelManager != null) {
                this.manager.release(this.labelManager);
                this.labelManager = null;
            }
            this.manager = null;
        }
    }

	/**
	 * @see org.apache.cocoon.portal.event.aspect.EventAspect#process(org.apache.cocoon.portal.event.aspect.EventAspectContext)
	 */
	public void process(EventAspectContext context) {
        if (this.labelManager != null) {
            final EventManager publisher = context.getPortalService().getEventManager();
            final Request request = ObjectModelHelper.getRequest(context.getPortalService().getProcessInfoProvider().getObjectModel());
            final String parameterName = this.labelManager.getRequestParameterName();

            String label = request.getParameter(parameterName);
            // The pageLabel must be single valued
            if (label != null) {
                String previous = this.labelManager.getPreviousLabel();
                if (previous != null && previous.equals(label)) {
                    // Already on this page. Don't publish the pageLabel events
                } else {
                    Iterator iter = this.labelManager.getPageLabelEvents(label).iterator();
                    // Publish all the events for this page label.
                    while (iter.hasNext()) {
                        Event event = (Event) iter.next();
                        publisher.send(event);
                    }
            //        return;
                }
            }
        }

        context.invokeNext();
	}
}
