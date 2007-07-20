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
package org.apache.cocoon.objectmodel;

import java.util.Collection;

import junit.framework.TestCase;

public class ObjectModelImplTestCase extends TestCase {
    
    public void testMap() {
        ObjectModel objectModel = new ObjectModelImpl();
        
        objectModel.put("foo1", "bar1");
        assertEquals("bar2", objectModel.put("foo2", "bar2")); 
        assertEquals(true, objectModel.containsKey("foo1"));
        assertEquals("bar2", objectModel.get("foo2"));
        
        objectModel.remove("foo1");
        assertEquals(null, objectModel.get("foo1"));
    }
    
    public void testMultiValue() {
        ObjectModel objectModel = new ObjectModelImpl();
        
        objectModel.put("foo", "bar1");
        objectModel.put("foo", "bar2");
        assertEquals(2, ((Collection)objectModel.get("foo")).size());
        
        objectModel.remove("foo", "bar2");
        assertEquals("bar1", objectModel.get("foo"));
        
        objectModel.remove("foo", "bar1");
        assertEquals(null, objectModel.get("foo"));
    }
    
    public void testValues() {
        ObjectModel objectModel = new ObjectModelImpl();
        
        objectModel.put("foo", "bar1");
        Collection values = objectModel.values();
        assertEquals(true, values.contains("bar1"));
        
        objectModel.put("foo", "bar2");
        values = objectModel.values();
        assertEquals(true, values.contains("bar1"));
        assertEquals(true, values.contains("bar2"));
        
        objectModel.remove("foo", "bar1");
        values = objectModel.values();
        assertEquals(false, values.contains("bar1"));
    }
    
    public void testLocalContext() {
        ObjectModel objectModel = new ObjectModelImpl();
        
        objectModel.put("foo", "bar1");
        objectModel.markLocalContext();
        objectModel.put("foo", "bar2");
        objectModel.put("abc", "xyz");
        
        assertEquals(true, objectModel.values().contains("bar2"));
        assertEquals(true, objectModel.values().contains("bar1"));
        assertEquals(true, objectModel.containsKey("abc"));
        
        objectModel.cleanupLocalContext();
        
        assertEquals(false, objectModel.values().contains("bar2"));
        assertEquals(true, objectModel.values().contains("bar1"));
        assertEquals(false, objectModel.containsKey("abc"));
    }
    
}