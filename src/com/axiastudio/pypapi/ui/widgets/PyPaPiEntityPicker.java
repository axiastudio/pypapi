/*
 * Copyright (C) 2011 AXIA Studio (http://www.axiastudio.com)
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
package com.axiastudio.pypapi.ui.widgets;

import com.trolltech.qt.core.*;
import com.trolltech.qt.gui.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.axiastudio.pypapi.Register;
import com.axiastudio.pypapi.db.Controller;
import com.axiastudio.pypapi.db.IController;
import com.axiastudio.pypapi.ui.Util;
import com.axiastudio.pypapi.ui.Column;
import com.axiastudio.pypapi.ui.Context;
import com.axiastudio.pypapi.ui.Form;
import com.axiastudio.pypapi.ui.ItemLookup;
import com.axiastudio.pypapi.ui.PickerDialog;

/**
 *
 * @author tiziano
 */
public class PyPaPiEntityPicker extends QLineEdit{
    
    private Column bindColumn;
    private QAction actionOpen, actionSelect;
    private QMenu menuPopup;

    public PyPaPiEntityPicker(){
        /*
         *  init
         */
        this.initializeContextMenu();
    }

    private void initializeContextMenu(){
        this.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu);
        this.customContextMenuRequested.connect(this, "contextMenu(QPoint)");
        this.menuPopup = new QMenu(this);

        this.actionSelect = new QAction("Select", this);
        QIcon iconSelect = new QIcon("classpath:com/axiastudio/pypapi/ui/resources/link.png");
        this.actionSelect.setIcon(iconSelect);
        this.menuPopup.addAction(actionSelect);

        this.actionOpen = new QAction("Open", this);
        QIcon iconOpen = new QIcon("classpath:com/axiastudio/pypapi/ui/resources/open.png");
        this.actionOpen.setIcon(iconOpen);
        this.menuPopup.addAction(actionOpen);
        
        this.installEventFilter(this);
        
    }


    private void contextMenu(QPoint point){
        QAction action = this.menuPopup.exec(this.mapToGlobal(point));
        Context context = (Context) Register.queryRelation(this.window(), ".");
        ItemLookup item;
        try {
            item = (ItemLookup) context.getModel().getByEntity(context.getCurrentEntity(), bindColumn);
        } catch (Exception ex) {
            Logger.getLogger(PyPaPiEntityPicker.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        if (this.actionOpen.equals(action)){
            Object entity = item.get();
            Form newForm = Util.formFromEntity(entity);
            newForm.show();
        } else if (this.actionSelect.equals(action)){
            Controller controller = (Controller) Register.queryUtility(IController.class, item.getName());
            PickerDialog pd = new PickerDialog(this, controller);
        int res = pd.exec();
        if ( res == 1 ){
            System.out.println(pd.getSelection());
        }

        }
    }
    
    @Override
    public boolean eventFilter(QObject dist, QEvent event){
        // TODO: fast selector from id?
        return false;
    }

    
    public Column getBindColumn() {
        return bindColumn;
    }

    public void setBindColumn(Column column) {
        this.bindColumn = column;
    }

}