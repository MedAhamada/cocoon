/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.woody.datatype.typeimpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.cocoon.woody.datatype.Datatype;
import org.apache.cocoon.woody.datatype.DatatypeBuilder;
import org.apache.cocoon.woody.datatype.ValidationError;
import org.apache.cocoon.woody.datatype.ValidationRule;
import org.apache.cocoon.woody.datatype.convertor.Convertor;
import org.outerj.expression.ExpressionContext;

/**
 * Abstract base class for Datatype implementations. Most concreate datatypes
 * will derive from this class.
 * @version $Id: AbstractDatatype.java,v 1.9 2004/03/09 13:53:54 reinhard Exp $
 */
public abstract class AbstractDatatype implements Datatype {
    private List validationRules = new ArrayList();
    private boolean arrayType = false;
    private DatatypeBuilder builder;
    private Convertor convertor;

    public ValidationError validate(Object value, ExpressionContext expressionContext) {
        Iterator validationRulesIt = validationRules.iterator();
        while (validationRulesIt.hasNext()) {
            ValidationRule validationRule = (ValidationRule)validationRulesIt.next();
            ValidationError result = validationRule.validate(value, expressionContext);
            if (result != null)
                return result;
        }
        return null;
    }

    public void addValidationRule(ValidationRule validationRule) {
        validationRules.add(validationRule);
    }

    public boolean isArrayType() {
        return arrayType;
    }

    void setArrayType(boolean arrayType) {
        this.arrayType = arrayType;
    }

    void setConvertor(Convertor convertor) {
        this.convertor = convertor;
    }

    void setBuilder(DatatypeBuilder builder) {
        this.builder = builder;
    }

    public Convertor getPlainConvertor() {
        return builder.getPlainConvertor();
    }

    public DatatypeBuilder getBuilder() {
        return builder;
    }

    public Convertor getConvertor() {
        return convertor;
    }

    public Object convertFromString(String value, Locale locale) {
        return getConvertor().convertFromString(value, locale, null);
    }

    public String convertToString(Object value, Locale locale) {
        return getConvertor().convertToString(value, locale, null);
    }
}
