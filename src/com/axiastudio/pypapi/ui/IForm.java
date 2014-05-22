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

import com.axiastudio.pypapi.db.Store;
import com.trolltech.qt.core.QObject;

import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Tiziano Lattisi <tiziano at axiastudio.it>
 */
public interface IForm {

    void init();

    void init(Store store);

    void select(Long id);

    public void show();
    
    public Context getContext();
    
    public void setContext(Context context);
    
    List findChildren();
    
    public void setColumns(List<Column> columns);
    
    public void setEntities(List<Column> entities);
    
    public void setWidgets(HashMap<String, QObject> widgets);
    
    public List<Column> getColumns();

    public Column getColumn(String columnName);

    public List<Column> getEntities();
    
    public HashMap<String, QObject> getWidgets();
    
    public String windowTitle();

    public IForm getParentForm();

    public void setParentForm(IForm parentForm);

}
