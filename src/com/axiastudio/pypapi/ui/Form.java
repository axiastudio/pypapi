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
import com.axiastudio.pypapi.db.Store;
import com.axiastudio.pypapi.ui.widgets.PyPaPiEntityPicker;
import com.axiastudio.pypapi.ui.widgets.PyPaPiMenuBar;
import com.axiastudio.pypapi.ui.widgets.PyPaPiTableView;
import com.trolltech.qt.core.*;
import com.trolltech.qt.designer.QUiLoader;
import com.trolltech.qt.designer.QUiLoaderException;
import com.trolltech.qt.gui.QHeaderView.ResizeMode;
import com.trolltech.qt.gui.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tiziano Lattisi <tiziano at axiastudio.it>
 */
public class Form extends QMainWindow implements IForm {

    private Class entityClass;
    private String uiFile;
    private String title;
    private Context context;
    private HashMap<String, QObject> widgets;
    private List<Column> columns;
    private List<Column> entities;

    public Form(Form other) {
        this(other.uiFile, other.entityClass);
    }

    public Form(String uiFile, Class entityClass) {
        this(uiFile, entityClass, "");
    }

    public Form(String uiFile, Class entityClass, String title) {
        this.entityClass = entityClass;
        this.uiFile = uiFile;
        this.title = title;
        QFile file = Util.ui2jui(new QFile(uiFile));
        //QFile file = new QFile(uiFile);
        this.loadUi(file);
    }
    
    @Override
    public void init(){
        this.init(null);
    }
        
    @Override
    public void init(Store store){
        this.resolveColumns();
        this.addValidators();
        /* root context */
        this.context = this.createContext(".", store);
        this.addMappers();
        /* context's children */
        this.initModels();
        PyPaPiMenuBar bar = new PyPaPiMenuBar("Navigation", this);
        bar.setMovable(true);
        this.addToolBar(bar);
        this.context.getMapper().currentIndexChanged.connect(this, "indexChanged(int)");
        this.context.getMapper().toFirst();
        bar.refresh();
    }

    private void loadUi(QFile uiFile){
        QWidget window = null;
        try {
            window = (QMainWindow) QUiLoader.load(uiFile);
        } catch (QUiLoaderException ex) {
            Logger.getLogger(Form.class.getName()).log(Level.SEVERE, null, ex);
        }
        for( QByteArray name: window.dynamicPropertyNames()){
            this.setProperty(name.toString(), window.property(name.toString()));
        }
        this.setCentralWidget(window);
    }

    private void initModels(){
        for (Object objColumn:this.entities){
            Column column = (Column) objColumn;
            this.createContext(column.getName());
        }

    }

    private Context createContext(String path){
        return this.createContext(path, null);
    }

    private Context createContext(String path, Store store){
        List contextColumns;
        Context dataContext;
        if(".".equals(path)){
            contextColumns = this.columns;
        } else {
            contextColumns = (List) Register.queryRelation(this.widgets.get(path), "columns");
        }
        if( store == null){
            dataContext = new Context(this, this.entityClass, path, contextColumns);
        } else {
            dataContext = new Context(this, this.entityClass, path, contextColumns, store);
        }
        Register.registerRelation(dataContext, this, path);
        if(! ".".equals(path)){
            QTableView qtv = (QTableView) this.widgets.get(path);
            qtv.setModel(dataContext.getModel());
            this.setResizeModes(qtv);
        }
        return dataContext;
    }

    private void resolveColumns(){

        QObject child;
        List children = this.findChildren();
        Boolean isColumn;
        Boolean isEntity;
        List<Column> criteria;
        List<Column> searchColumns;

        criteria = new ArrayList();
        searchColumns = new ArrayList();
        this.columns = new ArrayList();
        this.entities = new ArrayList();
        this.widgets = new HashMap();
        EntityBehavior behavior = new EntityBehavior(this.entityClass.getName());

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
                column = new Column(columnPropertyName, columnPropertyName, columnPropertyName,
                        lookupPropertyName);
                boolean add = this.columns.add(column);
                Object put = this.widgets.put(columnPropertyName, child);
                // Reg Exp validator
                Object validatorProperty = child.property("validator");
                if( validatorProperty != null){
                    behavior.setReValidator(columnPropertyName, (String) validatorProperty);
                }
            }
            if (isEntity){
                String entityPropertyName = this.capitalize((String) entityProperty);
                column = new Column(entityPropertyName, entityPropertyName, entityPropertyName);
                boolean add = this.entities.add(column);
                Object put = this.widgets.put(entityPropertyName, child);
            }

            // XXX: implements ILookable?
            if (child.getClass().equals(PyPaPiEntityPicker.class)){
                ((PyPaPiEntityPicker) child).setBindColumn(column);
            }
            // search dynamic property
            Object searchProperty = child.property("search");
            if (searchProperty != null){
                if ((Boolean) searchProperty){
                    criteria.add(column);
                }
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
                        String name = this.capitalize(columnNames[c]);
                        String label = name;
                        Integer resizeMode=0;
                        if( headerNames != null){
                            label = headerNames[c];
                        }
                        if( Character.isLowerCase(columnNames[c].charAt(0)) ){
                            /* ex. columnname -> QHeaderView::ResizeToContent */
                            resizeMode = 3;
                        } else if ( Character.isUpperCase(columnNames[c].charAt(0)) &&
                                    Character.isUpperCase(columnNames[c].charAt(1))){
                            /* ex. COLUMNNAME -> QHeaderView::Stretch */
                            resizeMode = 1;
                        } else if ( Character.isUpperCase(columnNames[c].charAt(0)) &&
                                    Character.isLowerCase(columnNames[c].charAt(1))){
                            /* ex. Columnname -> QHeaderView::Interactive */
                            resizeMode = 0;
                        }
                        Column tableColumn = new Column(name, label, name, null, resizeMode);
                        tableColumns.add(tableColumn);
                    }
                    Register.registerRelation(tableColumns, child, "columns");
                }
                Object referenceProperty = child.property("reference");
                if (referenceProperty != null){
                    Register.registerRelation((String) referenceProperty, child, "reference");
                }
            }
            // Delegate
            if (child.getClass().equals(PyPaPiTableView.class)){
                PyPaPiTableView ptv = (PyPaPiTableView) child;
                ptv.setItemDelegate(new Delegate(ptv));
            }
        }
        // search columns
        Object searchColumnsProperty = this.property("searchcolumns");
        if (searchColumnsProperty != null){
            String[] columnNames = ((String) searchColumnsProperty).split(",");
            for(String name: columnNames){
                name = this.capitalize(name);
                Column searchColumn = new Column(name, name, name);
                searchColumns.add(searchColumn);
            }
        }
        behavior.setCriteria(criteria);
        behavior.setSearchColumns(searchColumns);
        Register.registerUtility(behavior, IEntityBehavior.class, this.entityClass.getName());
    }

    private void addMappers() {
        for (int i=0; i<this.columns.size(); i++){
            Column column = (Column) this.columns.get(i);
            QObject widget = (QObject) this.widgets.get(column.getName());
            if( widget.getClass().equals(QTextEdit.class)){
                this.context.getMapper().addMapping((QTextEdit) widget, i, new QByteArray("plainText"));
                ((QTextEdit) widget).setTabChangesFocus(true);
            } else if( widget.getClass().equals(QCheckBox.class) ){
                this.context.getMapper().addMapping((QCheckBox) widget, i, new QByteArray("checked"));
                ((QCheckBox) widget).clicked.connect(this.context.getMapper(), "submit()", Qt.ConnectionType.AutoConnection);
            } else if( widget.getClass().equals(QComboBox.class) ){
                this.context.getMapper().addMapping((QComboBox) widget, i, new QByteArray("currentIndex"));
            } else {
                this.context.getMapper().addMapping((QWidget) widget, i);
            }
        }
    }
    
    private void addValidators() {
        EntityBehavior behavior = (EntityBehavior) Register.queryUtility(IEntityBehavior.class, this.entityClass.getName());
        for( String widgetName: behavior.getReValidatorKeys() ){
            QObject widget = this.widgets.get(widgetName);
            if( widget.getClass() == QLineEdit.class ){
                String re = behavior.getReValidator(widgetName);
                QRegExp regExp = new QRegExp(re);
                QRegExpValidator validator = new QRegExpValidator(widget);
                validator.setRegExp(regExp);
                ((QLineEdit) widget).setValidator(validator);
            }
        }
    }
    
    private void setResizeModes(QTableView qtv){
        TableModel model = (TableModel) qtv.model();
        QHeaderView horizontalHeader = qtv.horizontalHeader();
        for( int i=0; i<model.getColumns().size(); i++ ){
            Column c = model.getColumns().get(i);
            ResizeMode mode = QHeaderView.ResizeMode.Interactive;
            switch(c.getResizeMode()){
                case 0:
                    mode = QHeaderView.ResizeMode.Interactive;
                    break;
                case 1:
                    mode = QHeaderView.ResizeMode.Stretch;
                    break;
                case 2:
                    mode = QHeaderView.ResizeMode.Fixed;
                    break;
                case 3:
                    mode = QHeaderView.ResizeMode.ResizeToContents;
                    break;
            }
            horizontalHeader.setResizeMode(i, mode);
        }
    }
    
    private void indexChanged(int row){
        int idx = this.context.getMapper().currentIndex() + 1;
        int tot = this.context.getModel().rowCount();
        this.setWindowTitle(this.title + " (" + idx + " of " + tot +")");
    }

    public Context getContext() {
        return context;
    }
    
    private String capitalize(String s) {
        s = s.toLowerCase();
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }
    
}
