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
package org.apache.cocoon.portal.layout;

/**
*
* @version CVS $Id: Parameters.java 30932 2004-07-29 17:35:38Z vgritsenko $
*/
public class SkinDescription {

    protected String name;
    protected String basePath;
    protected String thumbnailPath;
    
    
    /**
     * @return Returns the basePath.
     */
    public String getBasePath() {
        return basePath;
    }
    /**
     * @param basePath The basePath to set.
     */
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }
    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return Returns the thumbnailPath.
     */
    public String getThumbnailPath() {
        return thumbnailPath;
    }
    /**
     * @param thumbnailPath The thumbnailPath to set.
     */
    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }
}