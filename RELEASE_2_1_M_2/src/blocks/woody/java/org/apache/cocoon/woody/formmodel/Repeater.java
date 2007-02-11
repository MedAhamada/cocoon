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
package org.apache.cocoon.woody.formmodel;

import org.apache.cocoon.woody.Constants;
import org.apache.cocoon.woody.FormContext;
import org.apache.cocoon.xml.AttributesImpl;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.util.*;

/**
 * A repeater is a widget that repeats a number of other widgets.
 *
 * <p>Technically, the Repeater widget is a ContainerWidget whose children are
 * {@link RepeaterRow}s, and the RepeaterRows in turn are ContainerWidgets
 * containing the actual repeated widgets. However, in practice, you won't need
 * to use the RepeaterRow widget directly.
 *
 * <p>Using the methods {@link #getSize} and {@link #getWidget(int, java.lang.String)}
 * you can access all of the repeated widget instances.
 */
public class Repeater extends AbstractWidget implements ContainerWidget {
    private RepeaterDefinition definition;
    private List rows = new ArrayList();

    public Repeater(RepeaterDefinition repeaterDefinition) {
        this.definition = repeaterDefinition;
    }

    public String getId() {
        return definition.getId();
    }

    public int getSize() {
        return rows.size();
    }

    public void addRow() {
        rows.add(new RepeaterRow());
    }

    public RepeaterRow getRow(int index) {
        return (RepeaterRow)rows.get(index);
    }

    /**
     * @throws IndexOutOfBoundsException if the the index is outside the range of existing rows.
     */
    public void removeRow(int index) {
        rows.remove(index);
    }

    /**
     * Gets a widget on a certain row.
     * @param rowIndex startin from 0
     * @param id a widget id
     * @return null if there's no such widget
     */
    public Widget getWidget(int rowIndex, String id) {
        RepeaterRow row = (RepeaterRow)rows.get(rowIndex);
        return row.getWidget(id);
    }

    public Widget getWidget(String id) {
        // TODO catch numberformatexception and throw something nice in that case
        int row = Integer.parseInt(id);
        return (RepeaterRow)rows.get(row);
    }

    public void readFromRequest(FormContext formContext) {
        // read number of rows from request, and make an according number of rows
        String sizeParameter = formContext.getRequest().getParameter(getFullyQualifiedId() + ".size");
        if (sizeParameter != null) {
            int size = 0;
            try {
                size = Integer.parseInt(sizeParameter);
            } catch (NumberFormatException exc) {
                // do nothing
            }
            for (int i = 0; i < size; i++) {
                // TODO: a person with bad intents could pass a very large size parameter, maybe we should
                // check for a built-in limit or something.
                addRow();
            }
        } else {
            rows.clear();
        }

        // let the rows read their data from the request
        Iterator rowIt = rows.iterator();
        while (rowIt.hasNext()) {
            RepeaterRow row = (RepeaterRow)rowIt.next();
            row.readFromRequest(formContext);
        }
    }

    public boolean validate(FormContext formContext) {
        boolean valid = true;
        Iterator rowIt = rows.iterator();
        while (rowIt.hasNext()) {
            RepeaterRow row = (RepeaterRow)rowIt.next();
            valid = valid & row.validate(formContext);
        }
        return valid;
    }

    private static final String REPEATER_EL = "repeater";
    private static final String HEADINGS_EL = "headings";
    private static final String HEADING_EL = "heading";
    private static final String LABEL_EL = "label";
    private static final String REPEATER_SIZE_EL = "repeater-size";

    public void generateSaxFragment(ContentHandler contentHandler, Locale locale) throws SAXException {
        AttributesImpl repeaterAttrs = new AttributesImpl();
        repeaterAttrs.addCDATAAttribute("id", getFullyQualifiedId());
        repeaterAttrs.addCDATAAttribute("size", String.valueOf(getSize()));
        contentHandler.startElement(Constants.WI_NS, REPEATER_EL, Constants.WI_PREFIX_COLON + REPEATER_EL, repeaterAttrs);

        // the repeater's label
        contentHandler.startElement(Constants.WI_NS, LABEL_EL, Constants.WI_PREFIX_COLON + LABEL_EL, Constants.EMPTY_ATTRS);
        definition.generateLabel(contentHandler);
        contentHandler.endElement(Constants.WI_NS, LABEL_EL, Constants.WI_PREFIX_COLON + LABEL_EL);

        // heading element -- currently contains the labels of each widget in the repeater
        contentHandler.startElement(Constants.WI_NS, HEADINGS_EL, Constants.WI_PREFIX_COLON + HEADINGS_EL, Constants.EMPTY_ATTRS);
        Iterator widgetDefinitionIt = definition.getWidgetDefinitions().iterator();
        while (widgetDefinitionIt.hasNext()) {
            WidgetDefinition widgetDefinition = (WidgetDefinition)widgetDefinitionIt.next();
            contentHandler.startElement(Constants.WI_NS, HEADING_EL, Constants.WI_PREFIX_COLON + HEADING_EL, Constants.EMPTY_ATTRS);
            widgetDefinition.generateLabel(contentHandler);
            contentHandler.endElement(Constants.WI_NS, HEADING_EL, Constants.WI_PREFIX_COLON + HEADING_EL);
        }
        contentHandler.endElement(Constants.WI_NS, HEADINGS_EL, Constants.WI_PREFIX_COLON + HEADINGS_EL);

        // the actual rows in the repeater
        Iterator rowIt = rows.iterator();
        while (rowIt.hasNext()) {
            RepeaterRow row = (RepeaterRow)rowIt.next();
            row.generateSaxFragment(contentHandler, locale);
        }
        contentHandler.endElement(Constants.WI_NS, REPEATER_EL, Constants.WI_PREFIX_COLON + REPEATER_EL);
    }

    public void generateLabel(ContentHandler contentHandler) throws SAXException {
        definition.generateLabel(contentHandler);
    }

    /**
     * Generates the label of a certain widget in this repeater.
     */
    public void generateWidgetLabel(String widgetId, ContentHandler contentHandler) throws SAXException {
        WidgetDefinition widgetDefinition = definition.getWidgetDefinition(widgetId);
        if (widgetDefinition == null)
            throw new SAXException("Repeater \"" + getFullyQualifiedId() + "\" contains no widget with id \"" + widgetId + "\".");
        widgetDefinition.generateLabel(contentHandler);
    }

    /**
     * Generates a repeater-size element with a size attribute indicating the size of this repeater.
     */
    public void generateSize(ContentHandler contentHandler) throws SAXException {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addCDATAAttribute("id", getFullyQualifiedId());
        attrs.addCDATAAttribute("size", String.valueOf(getSize()));
        contentHandler.startElement(Constants.WI_NS, REPEATER_SIZE_EL, Constants.WI_PREFIX_COLON + REPEATER_SIZE_EL, attrs);
        contentHandler.endElement(Constants.WI_NS, REPEATER_SIZE_EL, Constants.WI_PREFIX_COLON + REPEATER_SIZE_EL);
    }

    public class RepeaterRow implements ContainerWidget {
        private List widgets;
        private Map widgetsById;

        public RepeaterRow() {
            this.widgets = new ArrayList();
            this.widgetsById = new HashMap();

            // make instances of all the widgets
            Iterator widgetDefinitionIt = definition.getWidgetDefinitions().iterator();
            while (widgetDefinitionIt.hasNext()) {
                WidgetDefinition widgetDefinition = (WidgetDefinition)widgetDefinitionIt.next();
                Widget widget = widgetDefinition.createInstance();
                widget.setParent(this);
                widgets.add(widget);
                widgetsById.put(widget.getId(), widget);
            }
        }

        public String getId() {
            // id of a RepeaterRow is the position of the row in the list of rows.
            return String.valueOf(rows.indexOf(this));
        }

        public ContainerWidget getParent() {
            return Repeater.this;
        }

        public String getNamespace() {
            return getParent().getNamespace() + "." + getId();
        }

        public String getFullyQualifiedId() {
            return getParent().getNamespace() + "." + getId();
        }

        public void setParent(ContainerWidget widget) {
            throw new RuntimeException("Parent of RepeaterRow is fixed, and cannot be set.");
        }

        public Widget getWidget(String id) {
            return (Widget)widgetsById.get(id);
        }

        public void readFromRequest(FormContext formContext) {
            Iterator widgetIt = widgets.iterator();
            while (widgetIt.hasNext()) {
                Widget widget = (Widget)widgetIt.next();
                widget.readFromRequest(formContext);
            }
        }

        public boolean validate(FormContext formContext) {
            boolean valid = true;
            Iterator widgetIt = widgets.iterator();
            while (widgetIt.hasNext()) {
                Widget widget = (Widget)widgetIt.next();
                valid = valid & widget.validate(formContext);
            }
            return valid;
        }

        public Object getValue() {
            return null;
        }

        public boolean isRequired() {
            return false;
        }

        public void generateLabel(ContentHandler contentHandler) throws SAXException {
            // this widget has no label
        }

        private static final String ROW_EL = "repeater-row";

        public void generateSaxFragment(ContentHandler contentHandler, Locale locale) throws SAXException {
            AttributesImpl rowAttrs = new AttributesImpl();
            rowAttrs.addCDATAAttribute("id", getFullyQualifiedId());
            contentHandler.startElement(Constants.WI_NS, ROW_EL, Constants.WI_PREFIX_COLON + ROW_EL, rowAttrs);
            Iterator widgetIt = widgets.iterator();
            while (widgetIt.hasNext()) {
                Widget widget = (Widget)widgetIt.next();
                widget.generateSaxFragment(contentHandler, locale);
            }
            contentHandler.endElement(Constants.WI_NS, ROW_EL, Constants.WI_PREFIX_COLON + ROW_EL);
        }
    }
}
