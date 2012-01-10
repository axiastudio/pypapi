/*
 * Copyright (C) 2011 AXIA Studio (http://www.axiastudio.it)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.pypapi.ui.widgets;

import com.trolltech.qt.core.*;
import com.trolltech.qt.gui.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pypapi.GlobalManager;
import org.pypapi.ui.Form;
import org.pypapi.ui.TableModel;
import org.pypapi.ui.Util;

/**
 *
 * @author AXIA Studio (http://www.axiastudio.it)
 *
 * PyPaPiTableView is a custom widget implementing info, open, add and delete
 * of rows in the bag list.
 *
 */
public class PyPaPiTableView extends QTableView{

    private QAction actionAdd, actionDel, actionOpen, actionInfo;
    private QMenu menuPopup;

    public PyPaPiTableView(){
        this(null);
    }

    public PyPaPiTableView(QWidget parent){
        /*
         *  init
         */
        this.setSelectionBehavior(SelectionBehavior.SelectRows);
        this.initializeContextMenu();
    }
    
    private void initializeContextMenu(){
        this.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu);
        this.customContextMenuRequested.connect(this, "contextMenu(QPoint)");
        this.menuPopup = new QMenu(this);

        this.actionInfo = new QAction("Info", this);
        QIcon iconInfo = new QIcon("classpath:org/pypapi/ui/resources/toolbar/information.png");
        this.actionInfo.setIcon(iconInfo);
        this.menuPopup.addAction(actionInfo);

        this.actionOpen = new QAction("Open", this);
        QIcon iconOpen = new QIcon("classpath:org/pypapi/ui/resources/open.png");
        this.actionOpen.setIcon(iconOpen);
        this.menuPopup.addAction(actionOpen);

        this.actionAdd = new QAction("Add", this);
        QIcon iconAdd = new QIcon("classpath:org/pypapi/ui/resources/toolbar/add.png");
        this.actionAdd.setIcon(iconAdd);
        this.menuPopup.addAction(actionAdd);

        this.actionDel = new QAction("Delete", this);
        QIcon iconDel = new QIcon("classpath:org/pypapi/ui/resources/toolbar/delete.png");
        this.actionDel.setIcon(iconDel);
        this.menuPopup.addAction(actionDel);


    }


    private void contextMenu(QPoint point){
        List<QModelIndex> rows = this.selectionModel().selectedRows();
        Boolean selected = !rows.isEmpty();
        this.actionInfo.setEnabled(selected);
        this.actionOpen.setEnabled(selected);
        this.actionDel.setEnabled(selected);
        QAction action = this.menuPopup.exec(this.mapToGlobal(point));
        if (this.actionOpen.equals(action)){
            for (QModelIndex idx: rows){
                TableModel model = (TableModel) this.model();
                Object entity = model.getEntityByRow(idx.row());
                Object relation = GlobalManager.queryRelation(this, "reference");
                if ( relation != null ){
                    try {
                        String name = (String) relation;
                        String getterName = "get" + name.substring(0,1).toUpperCase() +
                        name.substring(1);
                        Method m = entity.getClass().getMethod(getterName);
                        entity = m.invoke(entity);
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(PyPaPiTableView.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalArgumentException ex) {
                        Logger.getLogger(PyPaPiTableView.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InvocationTargetException ex) {
                        Logger.getLogger(PyPaPiTableView.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (NoSuchMethodException ex) {
                        Logger.getLogger(PyPaPiTableView.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (SecurityException ex) {
                        Logger.getLogger(PyPaPiTableView.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                Form form = Util.formFromEntity(entity);
                form.show();
            }
        } else if (this.actionAdd.equals(action)){
            // TODO: select and insert new row
        }
    }
    
    @Override
    protected void enterEvent(QEvent event){
        //QPoint point = new QPoint(-50, -50);
        //this.contextMenu(point);
    }

    @Override
    protected void leaveEvent(QEvent event){
        //this.menuPopup.close();
    }

}
