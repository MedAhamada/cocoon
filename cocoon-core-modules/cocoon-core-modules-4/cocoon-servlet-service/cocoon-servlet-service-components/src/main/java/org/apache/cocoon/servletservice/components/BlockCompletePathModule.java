/**
 * 
 */
package org.apache.cocoon.servletservice.components;

import java.util.Iterator;
import java.util.Map;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.cocoon.components.modules.input.InputModule;
import org.apache.cocoon.environment.ObjectModelHelper;

/**
 * This module provides almost exactly the same functionality as {@link BlockPathModule}. The only difference is that
 * this module adds prefix-path on which block/servlet is mounted.<br>
 * Use this module if you need a base path for URLs pointing resources in your block.
 *
 */
public class BlockCompletePathModule implements InputModule {
	
	BlockPathModule blockPathModule;
	
	public BlockPathModule getBlockPathModule() {
		return blockPathModule;
	}

	public void setBlockPathModule(BlockPathModule blockPathModule) {
		this.blockPathModule = blockPathModule;
	}

	/* (non-Javadoc)
	 * @see org.apache.cocoon.components.modules.input.InputModule#getAttribute(java.lang.String, org.apache.avalon.framework.configuration.Configuration, java.util.Map)
	 */
	public Object getAttribute(String name, Configuration modeConf, Map objectModel) throws ConfigurationException {
		return ObjectModelHelper.getRequest(objectModel).getContextPath() + blockPathModule.getAttribute(name, modeConf, objectModel);
	}

	/* (non-Javadoc)
	 * @see org.apache.cocoon.components.modules.input.InputModule#getAttributeNames(org.apache.avalon.framework.configuration.Configuration, java.util.Map)
	 */
	public Iterator getAttributeNames(Configuration modeConf, Map objectModel) throws ConfigurationException {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.apache.cocoon.components.modules.input.InputModule#getAttributeValues(java.lang.String, org.apache.avalon.framework.configuration.Configuration, java.util.Map)
	 */
	public Object[] getAttributeValues(String name, Configuration modeConf, Map objectModel) throws ConfigurationException {
		throw new UnsupportedOperationException();
	}

}
