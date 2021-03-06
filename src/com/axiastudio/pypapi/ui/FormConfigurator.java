/*
 * Copyright (C) 2012 AXIA Studio (http://www.axiastudio.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axiastudio.pypapi.ui;

import com.axiastudio.pypapi.Register;
import com.axiastudio.pypapi.Resolver;
import com.axiastudio.pypapi.db.*;
import com.axiastudio.pypapi.ui.widgets.PyPaPiComboBox;
import com.axiastudio.pypapi.ui.widgets.PyPaPiTableView;
import com.trolltech.qt.core.QByteArray;
import com.trolltech.qt.core.QObject;
import com.trolltech.qt.core.QRegExp;
import com.trolltech.qt.core.Qt;
import com.trolltech.qt.core.Qt.SortOrder;
import com.trolltech.qt.gui.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tiziano Lattisi <tiziano at axiastudio.it>
 */
public class FormConfigurator {

    private IForm form;
    private Class entityClass;

    public FormConfigurator(IForm form, Class entityClass) {
        this.form = form;
        this.entityClass = entityClass;
    }
    
    public void configure(){
        this.configure(null);
    }

    public void configure(Store store){
        configure(store, false);
    }

    public void configure(Store store, Boolean newEm){
        this.resolveColumns();
        this.addValidators();
        Context context = this.createContext(".", store, newEm);
        this.form.setContext(context);
        this.addMappers();
        this.initModels();
    }
    
    private void initModels(){
        for (Object objColumn:this.form.getEntities()){
            Column column = (Column) objColumn;
            this.createContext(column.getName());
        }

    }
    private void resolveColumns(){

        QObject child;
        List children = this.form.findChildren();
        Boolean isColumn;
        Boolean isEntity;
        
        EntityBehavior behavior = (EntityBehavior) Register.queryUtility(IEntityBehavior.class, this.entityClass.getName());

        List<Column> columns = new ArrayList();
        List<Column> entities = new ArrayList();
        HashMap<String, QObject> widgets = new HashMap();

        for (int i=0; i<children.size(); i++){
            Object entityProperty=null;
            Column column=null;
            isColumn = false;
            isEntity = false;
            child = (QObject) children.get(i);
            Object columnProperty = child.property("column");
            if (columnProperty != null){
                isColumn = true;
            } else {
                entityProperty = child.property("entity");
                if (entityProperty != null){
                    isEntity = true;
                }
            }
            if (isColumn){
                String lookupPropertyName=null;
                String columnPropertyName = this.capitalize((String) columnProperty);
                Object lookupProperty = child.property("lookup");
                if ( lookupProperty != null){
                    lookupPropertyName = this.capitalize((String) lookupProperty);
                }
                column = behavior.getColumnByName(columnPropertyName);
                boolean add = columns.add(column);
                Object put = widgets.put(columnPropertyName, child);
            }
            if (isEntity){
                String entityPropertyName = this.capitalize((String) entityProperty);
                column = behavior.getColumnByName(entityPropertyName);
                boolean add = entities.add(column);
                Object put = widgets.put(entityPropertyName, child);
            }

            if (child.getClass().equals(PyPaPiComboBox.class)){
                Method storeFactory = (Method) Register.queryUtility(IStoreFactory.class, column.getName());
                Boolean skipInitStoreProperty = (Boolean) child.property("skipinitialstore");
                Store lookupStore= new Store(new ArrayList());
                if ( skipInitStoreProperty==null || !skipInitStoreProperty ) {
                    if( storeFactory != null ){
                        try {
                            lookupStore = (Store) storeFactory.invoke(this.form);
                        } catch (IllegalAccessException ex) {
                            Logger.getLogger(FormConfigurator.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IllegalArgumentException ex) {
                            Logger.getLogger(FormConfigurator.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (InvocationTargetException ex) {
                            Logger.getLogger(FormConfigurator.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        Class entityClassFromReference = Resolver.entityClassFromReference(this.entityClass, column.getName());
                        Database db = (Database) Register.queryUtility(IDatabase.class);
                        Controller controller = db.createController(entityClassFromReference);
                        if( controller == null ){
                            Logger.getLogger(FormConfigurator.class.getName()).log(Level.SEVERE, "Unable to get controller for {0}", entityClassFromReference.getName());
                        }
                        lookupStore = controller.createFullStore();
                    }
                }
                column.setLookupStore(lookupStore);
                // is not null?
                Boolean notnull = false;
                Object nullableProperty = child.property("notnull");
                if ( nullableProperty != null){
                    notnull = (Boolean) nullableProperty;
                }
                // is sorted by toString?
                Boolean sortByToString = false;
                Object sortedbytostringProperty = child.property("sortbytostring");
                if ( sortedbytostringProperty != null){
                    sortByToString = (Boolean) sortedbytostringProperty;
                }
                ((PyPaPiComboBox) child).setLookupStore(lookupStore, notnull, sortByToString);
            }
            // columns and reference for list value widget
            if (child.getClass().equals(PyPaPiTableView.class)){
                Object headersProperty = child.property("headers");
                String[] headerNames=null;
                if (headersProperty != null){
                    headerNames = ((String) headersProperty).split(",");
                }
                Object columnsProperty = child.property("columns");
                if (columnsProperty != null){
                    String[] columnNames = ((String) columnsProperty).split(",");
                    List<Column> tableColumns = new ArrayList();
                    for(int c=0; c<columnNames.length; c++){
                        QHeaderView.ResizeMode resizeMode = Util.extractResizeMode(columnNames[c]);
                        String name = Util.cleanColumnName(columnNames[c]);
                        name = this.capitalize(name);
                        String label = name;
                        if( headerNames != null){
                            label = headerNames[c];
                        }
                        Column tableColumn = new Column(name, label, name, null, resizeMode.value());
                        tableColumns.add(tableColumn);
                    }
                    Register.registerRelation(tableColumns, child, "columns");
                }
                Object referenceProperty = child.property("reference");
                if (referenceProperty != null){
                    Register.registerRelation((String) referenceProperty, child, "reference");
                }
                Object filtersProperty = child.property("filters");
                if( filtersProperty != null ){
                    String[] filterPredicates = ((String) filtersProperty).split(",");
                    Map<Column, Object> filters = new HashMap();
                    for( String predicate: filterPredicates ){
                        String[] fieldAndValue = predicate.split("=");
                        if( fieldAndValue.length == 2 ){
                            String name = fieldAndValue[0];
                            String label = name;
                            String valueString = fieldAndValue[1];
                            Column filterColumn = new Column(name, label, name, null);
                            filters.put(filterColumn, valueString);
                        }
                    }
                    Register.registerRelation(filters, child, "filters");
                }
                Object warningsProperty = child.property("warnings");
                if( warningsProperty != null ){
                    String[] operators = {">=", "<=", "=", ">", "<", "is null", "is not null"};
                    String[] warningsPredicates = ((String) warningsProperty).split(",");
                    Map<Column, Object> warnings = new HashMap();
                    for( String predicate: warningsPredicates ){
                        String[] warning = predicate.split("@");
                        String description = warning[0];
                        if ( warning.length>1 ) {
                            description = warning[1];
                        }
                        for ( String op: operators ) {
                            if ( warning[0].contains(op) ) {
                                String[] fieldAndValue = warning[0].split(op);
                                String name = fieldAndValue[0].trim();
                                String valueString = "";
                                if ( fieldAndValue.length>1) {
                                    valueString=fieldAndValue[1];
                                }
                                Column warningColumn = new Column(name, op, description, null);
                                warnings.put(warningColumn, valueString);
                            }
                        }
                    }
                    Register.registerRelation(warnings, child, "warnings");
                }
                // sorting
                Object sortOrderProperty = child.property("sortorder");
                SortOrder sortOrder=Qt.SortOrder.AscendingOrder;
                if( sortOrderProperty != null ){
                    String sortOrderString = (String) sortOrderProperty;
                    if( sortOrderString.startsWith("-") || sortOrderString.startsWith(">") ){
                        sortOrder = Qt.SortOrder.DescendingOrder;
                    } else if( sortOrderString.startsWith("+") || sortOrderString.startsWith("<") ) {
                        sortOrder = Qt.SortOrder.AscendingOrder;
                    }
                }
                Object sortColumnProperty = child.property("sortcolumn");
                if( sortColumnProperty != null ){
                    Integer sortColumn = (Integer) sortColumnProperty;
                    ((QTableView) child).sortByColumn(sortColumn, sortOrder);
                }                
            }
            // Delegate
            if (child.getClass().equals(PyPaPiTableView.class)){
                PyPaPiTableView ptv = (PyPaPiTableView) child;
            }
            // Set min and max in QDoubleSpinBox as numeric(15, 2)
            if( child.getClass().equals(QDoubleSpinBox.class)){
                QDoubleSpinBox doubleSpinBox = (QDoubleSpinBox) child;
                doubleSpinBox.setMinimum(-9999999999999.99d);
                doubleSpinBox.setMaximum(9999999999999.99d);
            }
        }
        this.form.setColumns(columns);
        this.form.setEntities(entities);
        this.form.setWidgets(widgets);
    }    
    
    private Context createContext(String path){
        return this.createContext(path, null, false);
    }

    private Context createContext(String path, Store store, Boolean newEm){
        List contextColumns;
        Context dataContext;
        if(".".equals(path)){
            contextColumns = this.form.getColumns();
        } else {
            contextColumns = (List) Register.queryRelation(this.form.getWidgets().get(path), "columns");
        }
        if( store == null){
            dataContext = new Context(this.form, this.entityClass, path, contextColumns);
        } else {
            dataContext = new Context(this.form, this.entityClass, path, contextColumns, store, newEm);
        }
        // read-only and no-delete
        EntityBehavior behavior = (EntityBehavior) Register.queryUtility(IEntityBehavior.class, this.entityClass.getName());
        dataContext.setNoDelete(behavior.getNoDelete());
        dataContext.setNoInsert(behavior.getNoInsert());
        dataContext.setReadOnly(behavior.getReadOnly());
        Register.registerRelation(dataContext, this.form, path);
        if(! ".".equals(path)){
            QTableView qtv = (QTableView) this.form.getWidgets().get(path);
            ProxyModel proxy = new ProxyModel();
            proxy.setSourceModel(dataContext.getModel());
            qtv.setModel(proxy);
            this.setResizeModes(qtv);
        }
        return dataContext;
    }
    
    private void setResizeModes(QTableView qtv){
        ITableModel model = (ITableModel) qtv.model();
        QHeaderView horizontalHeader = qtv.horizontalHeader();
        for( int i=0; i<model.getColumns().size(); i++ ){
            Column c = model.getColumns().get(i);
            QHeaderView.ResizeMode mode = QHeaderView.ResizeMode.resolve(c.getResizeModeValue());
            horizontalHeader.setResizeMode(i, mode);
        }
    }
    
    private void addMappers() {
        for (int i=0; i<this.form.getColumns().size(); i++){
            Column column = (Column) this.form.getColumns().get(i);
            QObject widget = (QObject) this.form.getWidgets().get(column.getName());
            if( widget.getClass().equals(QTextEdit.class)){
                this.form.getContext().getMapper().addMapping((QTextEdit) widget, i, new QByteArray("plainText"));
                ((QTextEdit) widget).setTabChangesFocus(true);
            } else if( widget.getClass().equals(QCheckBox.class) ){
                this.form.getContext().getMapper().addMapping((QCheckBox) widget, i, new QByteArray("checked"));
                ((QCheckBox) widget).clicked.connect(this.form.getContext().getMapper(), "submit()", Qt.ConnectionType.AutoConnection);
            } else if( widget.getClass().equals(QRadioButton.class) ){
                this.form.getContext().getMapper().addMapping((QRadioButton) widget, i, new QByteArray("checked"));
                ((QRadioButton) widget).clicked.connect(this.form.getContext().getMapper(), "submit()", Qt.ConnectionType.AutoConnection);
            } else if( widget.getClass().equals(QComboBox.class) ){
                this.form.getContext().getMapper().addMapping((QComboBox) widget, i, new QByteArray("currentIndex"));
            } else if( widget.getClass().equals(PyPaPiComboBox.class) ){
                this.form.getContext().getMapper().addMapping((PyPaPiComboBox) widget, i, new QByteArray("currentIndex"));
            } else {
                this.form.getContext().getMapper().addMapping((QWidget) widget, i);
            }
        }
    }
    
    private void addValidators() {
        EntityBehavior behavior = (EntityBehavior) Register.queryUtility(IEntityBehavior.class, this.entityClass.getName());
        for( String widgetName: behavior.getReValidatorKeys() ){
            QObject widget = this.form.getWidgets().get(widgetName);
            if( widget.getClass() == QLineEdit.class ){
                String re = behavior.getReValidator(widgetName);
                QRegExp regExp = new QRegExp(re);
                QRegExpValidator validator = new QRegExpValidator(widget);
                validator.setRegExp(regExp);
                ((QLineEdit) widget).setValidator(validator);
            }
        }
    }
    
    private String capitalize(String s) {
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }
}
