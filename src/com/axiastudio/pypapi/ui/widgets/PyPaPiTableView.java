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
package com.axiastudio.pypapi.ui.widgets;

import com.axiastudio.pypapi.Register;
import com.axiastudio.pypapi.Resolver;
import com.axiastudio.pypapi.db.Controller;
import com.axiastudio.pypapi.db.Database;
import com.axiastudio.pypapi.db.IDatabase;
import com.axiastudio.pypapi.db.Store;
import com.axiastudio.pypapi.ui.*;
import com.trolltech.qt.core.*;
import com.trolltech.qt.gui.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tiziano Lattisi <tiziano at axiastudio.it>
 *
 * PyPaPiTableView is a custom widget implementing info, open, add and delete
 * of rows in the bag list.
 *
 */
public class PyPaPiTableView extends QTableView{
    
    private final Integer DEFAULT_ROW_HEIGHT = 24;
    private final String STYLE="QTableView {"
            + "image: url(classpath:com/axiastudio/pypapi/ui/resources/cog.png);"
            + "image-position: right; border: 1px solid #999999; }";
    private QAction actionAdd, actionDel, actionOpen, actionInfo, actionQuickInsert, actionSaveRows;
    private QMenu menuPopup;
    private QToolBar toolBar;
    private Boolean refreshConnected=false;
    private Boolean readOnly=false;
    private QLineEdit lineEditQuickInsert = new QLineEdit();
    private Object infoEntity=null;
    private List<Object> removed = new ArrayList<Object>();

    public PyPaPiTableView(){
        this(null);
    }

    public PyPaPiTableView(QWidget parent){
        /*
         *  init
         */
        super(parent);
        this.setStyleSheet(this.STYLE);
        this.setSelectionBehavior(SelectionBehavior.SelectRows);
        this.setSortingEnabled(true);
        this.horizontalHeader().setResizeMode(QHeaderView.ResizeMode.Interactive);
        this.verticalHeader().setFixedWidth(2);
        this.initializeMenu();
        this.setItemDelegate(new WikiDelegate(this));
        this.verticalHeader().setDefaultSectionSize(DEFAULT_ROW_HEIGHT);
    }
    
    private void initializeMenu(){
        this.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu);
        this.customContextMenuRequested.connect(this, "contextMenu(QPoint)");
        this.menuPopup = new QMenu(this);
        this.toolBar = new QToolBar(this);
        this.toolBar.setOrientation(Qt.Orientation.Vertical);
        this.toolBar.setIconSize(new QSize(16, 14));
        this.toolBar.move(1, 1);
        this.toolBar.hide();

        this.actionQuickInsert = new QAction(tr("QUICKINSERT"), this);
        QIcon iconQuickInsert = new QIcon("classpath:com/axiastudio/pypapi/ui/resources/toolbar/lightning_add.png");
        this.actionQuickInsert.setIcon(iconQuickInsert);
        this.menuPopup.addAction(actionQuickInsert);
        this.toolBar.addAction(actionQuickInsert);
        this.actionQuickInsert.triggered.connect(this, "actionQuickInsert()");
        
        this.actionInfo = new QAction(tr("INFO"), this);
        QIcon iconInfo = new QIcon("classpath:com/axiastudio/pypapi/ui/resources/toolbar/information.png");
        this.actionInfo.setIcon(iconInfo);
        this.menuPopup.addAction(actionInfo);
        this.toolBar.addAction(actionInfo);
        this.actionInfo.triggered.connect(this, "actionInfo()");

        this.actionOpen = new QAction(tr("OPEN"), this);
        QIcon iconOpen = new QIcon("classpath:com/axiastudio/pypapi/ui/resources/open.png");
        this.actionOpen.setIcon(iconOpen);
        this.menuPopup.addAction(actionOpen);
        this.toolBar.addAction(actionOpen);
        this.actionOpen.triggered.connect(this, "actionOpen()");

        this.actionAdd = new QAction(tr("ADD"), this);
        QIcon iconAdd = new QIcon("classpath:com/axiastudio/pypapi/ui/resources/toolbar/add.png");
        this.actionAdd.setIcon(iconAdd);
        this.menuPopup.addAction(this.actionAdd);
        this.toolBar.addAction(this.actionAdd);
        this.actionAdd.triggered.connect(this, "actionAdd()");

        this.actionDel = new QAction(tr("DELETE"), this);
        QIcon iconDel = new QIcon("classpath:com/axiastudio/pypapi/ui/resources/toolbar/delete.png");
        this.actionDel.setIcon(iconDel);
        this.menuPopup.addAction(actionDel);
        this.toolBar.addAction(actionDel);
        this.actionDel.triggered.connect(this, "actionDel()");

        this.actionSaveRows = new QAction(tr("SAVE_ROWS"), this);
        QIcon iconSaveRows = new QIcon("classpath:com/axiastudio/pypapi/ui/resources/toolbar/disk.png");
        this.actionSaveRows.setIcon(iconSaveRows);
        this.menuPopup.addAction(actionSaveRows);
        this.toolBar.addAction(actionSaveRows);
        this.actionSaveRows.triggered.connect(this, "actionSaveRows()");

    }

    private void modelDataChanged(QModelIndex topLeft, QModelIndex bottomRight){
        ITableModel model = (ITableModel) this.model();
        if( this.model() instanceof ProxyModel ){
            topLeft = ((ProxyModel) this.model()).mapToSource(topLeft);
        }
        infoEntity = model.getEntityByRow(topLeft.row());
        entityUpdated.emit(infoEntity);
    }
    

    private void contextMenu(QPoint point){
        this.refreshButtons();
        QAction action = this.menuPopup.exec(this.mapToGlobal(point));
    }

    /*
    @Override
    protected void enterEvent(QEvent event){
        if( !this.refreshConnected ){
            this.selectionModel().selectionChanged.connect(this, "refreshButtons()");
            this.refreshConnected = true;
        }
        this.verticalHeader().show();
        this.refreshButtons();
        this.toolBar.show();
    }

    @Override
    protected void leaveEvent(QEvent event){
            this.verticalHeader().hide();
            this.toolBar.hide();
    }*/

    
    private void refreshButtons(){
        List<QModelIndex> rows = selectionModel().selectedRows();
        Boolean selected = !rows.isEmpty();
        this.actionInfo.setEnabled(selected);
        this.actionOpen.setEnabled(selected);
        this.actionDel.setEnabled(selected && !getReadOnly());
        this.actionAdd.setEnabled(!getReadOnly());
        Boolean saverows = (Boolean) property("saverows");
        if( saverows == null || !saverows ){
            this.menuPopup.removeAction(this.actionSaveRows);
            this.toolBar.removeAction(this.actionSaveRows);
        } else {
            this.actionSaveRows.setEnabled(!getReadOnly());
        }
        String reference = (String) property("reference");
        this.actionQuickInsert.setEnabled(!getReadOnly() && reference != null);

    }
    
    private void actionOpen(){
        ITableModel model = (ITableModel) this.model();
        List<QModelIndex> rows = this.selectionModel().selectedRows();
        Object reference = Register.queryRelation(this, "reference");
        for (QModelIndex idx: rows){
            if( this.model() instanceof ProxyModel ){ 
                idx = ((ProxyModel) this.model()).mapToSource(idx);
            }
            Object entity = model.getEntityByRow(idx.row());
            if ( reference != null ){
                entity = Resolver.entityFromReference(entity, (String) reference);
                IForm parent = (IForm) Util.findParentForm(this);
                Controller controller = parent.getContext().getController();
                controller.detach(entity);
            }
            IForm form = Util.formFromEntity(entity);
            if( form == null ){
                return;
            }
            QMdiArea workspace = Util.findParentMdiArea(this);
            if( workspace != null ){
                workspace.addSubWindow((QMainWindow) form);
            }
            form.show();
        }
    }

    private void actionInfo(){
        ITableModel model = (ITableModel) this.model();
        List<QModelIndex> rows = this.selectionModel().selectedRows();
        for (QModelIndex idx: rows){
            if( this.model() instanceof ProxyModel ){ 
                idx = ((ProxyModel) this.model()).mapToSource(idx);
            }
            infoEntity = model.getEntityByRow(idx.row());
            IForm form = Util.formFromEntity(infoEntity);
            if( form == null ){
                return;
            }
            if( Dialog.class.isInstance(form) ){
                ((Dialog) form).setModal(true);                
            } else {
                QMdiArea workspace = Util.findParentMdiArea(this);
                if( workspace != null ){
                    workspace.addSubWindow((QMainWindow) form);
                }
            }
            IForm parent = (IForm) Util.findParentForm(this);
            form.setParentForm(parent);
            // when the form's data is changed, get the parent's data dirty
            form.getContext().getModel().dataChanged.connect(this, "refreshInfoEntity()");
            form.getContext().getModel().dataChanged.connect(parent.getContext().getModel().dataChanged);
            form.show();
        }
    }

    private void refreshInfoEntity(){
        QModelIndex idx = this.selectionModel().selectedRows().get(0);
        if( this.model() instanceof ProxyModel ){
            idx = ((ProxyModel) this.model()).mapToSource(idx);
        }
        int row = idx.row();
        ITableModel model = (ITableModel) model();
        model.getContextHandle().updateElement(infoEntity, row);
        entityUpdated.emit(infoEntity);
    }
    
    private void actionDel(){
        ITableModel model = (ITableModel) this.model();
        List<QModelIndex> rows = this.selectionModel().selectedRows();
        List<Integer> selectedRows = new ArrayList();
        for( QModelIndex idx: rows ){
            if( this.model() instanceof ProxyModel ){ 
                idx = ((ProxyModel) this.model()).mapToSource(idx);
            }
            selectedRows.add(idx.row());
        }
        Collections.sort(selectedRows, Collections.reverseOrder());
        for( Integer row: selectedRows ){
            Object toRemove = model.getEntityByRow(row);
            model.removeRows(row, 1, null);
            entityRemoved.emit(toRemove);
            removed.add(toRemove);
        }
    }
    
    private void actionAdd(){
        this.actionAdd(null);
    }
    
    public void actionAdd(List selection){
        ITableModel model = (ITableModel) this.model();
        Class rootClass = model.getContextHandle().getRootClass();
        String entityName = (String) this.property("entity");
        Class collectionClass = Resolver.collectionClassFromReference(rootClass, entityName.substring(1));
        Object reference = Register.queryRelation(this, "reference");
        Boolean openInfo = Boolean.FALSE;
        if ( reference != null ){
            Class referenceClass = Resolver.entityClassFromReference(collectionClass, (String) reference);
            Database db = (Database) Register.queryUtility(IDatabase.class);
            Controller controller = db.createController(referenceClass);

            int res=1;
            if( selection == null ){
                // Selection from PickerDialog
                PickerDialog pd = new PickerDialog(this, controller);
                Map<Column, Object> filters = (Map) Register.queryRelation(this, "filters");
                if( filters != null ){
                    for( Column column: filters.keySet() ){
                        pd.addFilter(column, filters.get(column));
                    }
                }
                res = pd.exec();
                selection = pd.getSelection();
            }

            Map<Column, Object> warnings = (Map) Register.queryRelation(this, "warnings");
            if( warnings != null ){
                for( Column column: warnings.keySet() ){
                    Map<Column, Object> warning = new HashMap<Column, Object>();
                    warning.put(column, warnings.get(column));
                    List warningSelection = Util.filterList(selection, warning, referenceClass);
                    for ( Object obj: warningSelection) {
                        String msg = column.getDescription() + "\n\nper l'oggetto selezionato \n" + obj.toString() +
                                "\n\nSi desidera inserire ugualmente?";
                        if ( !Util.questionBox(this, "Attenzione sull'oggetto selezionato!!", msg) ){
                            selection.remove(obj);
                        }
                    }
                }
            }

            if ( res == 1 ){
                for( int i=0; i<selection.size(); i++ ){
                    Object entity = selection.get(i);
                    Object adapted = null;
                    Class<?> classFrom = entity.getClass();
                    Class<?> classTo = collectionClass;

                    // TODO: to move in a specific utility
                    
                    // from class to class
                    Method adapter = (Method) Register.queryAdapter(classFrom, classTo);
                    String fromTo;
                    if( adapter == null ){
                        // from iface to class
                        Class<?> ifaceFrom = Resolver.interfaceFromEntityClass(entity.getClass());
                        adapter = (Method) Register.queryAdapter(ifaceFrom, classTo);
                        if( adapter == null ){
                            // form class to iface
                            Class<?> ifaceTo = Resolver.interfaceFromEntityClass(collectionClass);
                            adapter = (Method) Register.queryAdapter(classFrom, ifaceTo);
                            if( adapter == null ){
                                // from iface to iface
                                adapter = (Method) Register.queryAdapter(ifaceFrom, ifaceTo);
                            }
                        }
                    }

                    if( adapter != null ){
                        try {
                            adapted = adapter.invoke(null, entity);
                        } catch (IllegalAccessException ex) {
                            Logger.getLogger(PyPaPiTableView.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IllegalArgumentException ex) {
                            Logger.getLogger(PyPaPiTableView.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (InvocationTargetException ex) {
                            Logger.getLogger(PyPaPiTableView.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        List<Method> setters = Resolver.settersFromEntityClass(classTo, classFrom);
                        if(setters.size()==1){
                            try {
                                adapted = classTo.newInstance();
                                setters.get(0).invoke(adapted, entity);
                            } catch (IllegalArgumentException ex) {
                                Logger.getLogger(PyPaPiTableView.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (InvocationTargetException ex) {
                                Logger.getLogger(PyPaPiTableView.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (InstantiationException ex) {
                                Logger.getLogger(PyPaPiTableView.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IllegalAccessException ex) {
                                Logger.getLogger(PyPaPiTableView.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }

                    if( adapted != null ){
                        model.getContextHandle().insertElement(adapted);
                        this.selectRow(this.model().rowCount() - 1);
                        openInfo = Boolean.TRUE;
                        entityInserted.emit(adapted);
                    } else {
                        String title = "Adapter warning";
                        String description = "Unable to find an adapter from "+classFrom+" to "+classTo+".";
                        Util.warningBox(this, title, description);
                    }
                }
            }
        } else {
            try {
                Object notAdapted = collectionClass.newInstance();
                model.getContextHandle().insertElement(notAdapted);
                this.selectRow(this.model().rowCount() - 1);
                openInfo = Boolean.TRUE;
                entityInserted.emit(notAdapted);
            } catch (InstantiationException ex) {
                Logger.getLogger(PyPaPiTableView.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(PyPaPiTableView.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        if( openInfo && this.selectionModel().selectedRows().size()==1 ) {
            /* open info for the selected row */
            this.actionInfo();
        }
    }
    
    private void actionQuickInsert(){
        ITableModel model = (ITableModel) this.model();
        Class rootClass = model.getContextHandle().getRootClass();
        String entityName = (String) this.property("entity");
        String referenceName = (String) Register.queryRelation(this, "reference");
        lineEditQuickInsert.installEventFilter(new QuickInsertFilter(this));
        lineEditQuickInsert.setWindowTitle("Ins. rapido...");
        lineEditQuickInsert.show();
    }

    private void actionSaveRows(){
        Boolean res = Util.questionBox(this, tr("SAVE_ROWS_QUESTION"), tr("SAVE_ROWS_MESSAGE"));
        if( !res ){
            return;
        }
        ITableModel model = (ITableModel) this.model();
        Object rootEntity = model.getContextHandle().getPrimaryContext().getCurrentEntity();
        Database db = (Database) Register.queryUtility(IDatabase.class);
        for( Integer row=0; row<model.rowCount(); row++ ){
            Object entityByRow = model.getEntityByRow(row);
            List<Method> setters = Resolver.settersFromEntityClass(entityByRow.getClass(), rootEntity.getClass());
            if(setters.size()==1){
                try {
                    setters.get(0).invoke(entityByRow, rootEntity);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            Controller controller = db.createController(entityByRow.getClass());
            controller.commit(entityByRow);
        }
        for( Object toDelete: removed){
            Controller controller = db.createController(toDelete.getClass());
            controller.delete(toDelete);
        }
        removed.clear();
    }

    public QLineEdit getLineEditQuickInsert() {
        return lineEditQuickInsert;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public void setModel(QAbstractItemModel model) {
        if( model != null ) {
            if (getReadOnly()) {
                if (model instanceof ProxyModel) {
                    model = ((ProxyModel) model).sourceModel();
                }
                if (model instanceof TableModel) {
                    ((TableModel) model).setEditable(false);
                }
            }
            model.dataChanged.connect(this, "modelDataChanged(QModelIndex, QModelIndex)");
        }
        super.setModel(model);
    }

    /* SIGNALS */

    public Signal1<Object> entityInserted = new Signal1<Object>();
    
    public Signal1<Object> entityRemoved = new Signal1<Object>();

    public Signal1<Object> entityUpdated = new Signal1<Object>();

}


class QuickInsertFilter extends QObject {
    
    private PyPaPiTableView tableView;

    public QuickInsertFilter(PyPaPiTableView tableView){
        super();
        this.tableView = tableView;
    }
    
    @Override
    public boolean eventFilter(QObject qo, QEvent qevent) {
        if( qevent.type() == QEvent.Type.KeyPress ){
            QKeyEvent qke = (QKeyEvent) qevent;
            if( qke.key() == Qt.Key.Key_Tab.value() ){
                String idx = this.tableView.getLineEditQuickInsert().text();
                this.insert(idx);
                this.tableView.getLineEditQuickInsert().clear();
            } else if( qke.key() == Qt.Key.Key_Escape.value() ) {
                this.tableView.getLineEditQuickInsert().close();
            }
        }
        return false;
    }

    private void insert(String idx) {
        ITableModel model = (ITableModel) this.tableView.model();
        Class rootClass = model.getContextHandle().getRootClass();
        String entityName = (String) this.tableView.property("entity");
        String reference = (String) Register.queryRelation(this.tableView, "reference");
        Class collectionClass = Resolver.collectionClassFromReference(rootClass, entityName.substring(1));
        if ( reference != null ){
            Class referenceClass = Resolver.entityClassFromReference(collectionClass, (String) reference);
            Database db = (Database) Register.queryUtility(IDatabase.class);
            Controller controller = db.createController(referenceClass);

            // prendo i filtri dalla dynamic property
            Map<Column, Object> filters = (Map) Register.queryRelation(this.tableView, "filters");
            Map<Column, Object> newFilters = new HashMap<Column, Object>();
            Store whiteList = new Store(new ArrayList());

            // converto "true", "false" e setto il CellEditorType
            if ( filters != null ) {
                for(Column column: filters.keySet() ){
                    Object value = filters.get(column);
                    if( value instanceof String ){
                        // Differential strategy
                        String stringValue = (String) value;
                        if( "true".equals(stringValue) ){
                            column.setEditorType(CellEditorType.BOOLEAN);
                            newFilters.put(column, true);
                        } else if( "false".equals(stringValue) ){
                            column.setEditorType(CellEditorType.BOOLEAN);
                            newFilters.put(column, false);
                        }
                    } else {
                        column.setEditorType(CellEditorType.STRING);
                        newFilters.put(column, value);
                    }
                }
                // mi faccio dare la lista "bianca"
                whiteList = controller.createCriteriaStore(newFilters);
            }

            try{
                Object entity = controller.get(Long.parseLong(idx));
                if( entity != null && (filters == null || whiteList.contains(entity)) ){
                    List selection = new ArrayList();
                    selection.add(entity);
                    this.tableView.actionAdd(selection);
                }
            } catch (NumberFormatException e){
            
            }
        }
    }

}