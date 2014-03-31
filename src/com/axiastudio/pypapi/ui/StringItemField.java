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

import com.trolltech.qt.core.Qt;

import java.lang.reflect.Method;

/**
 *
 * @author Tiziano Lattisi <tiziano at axiastudio.it>
 */
public class StringItemField extends ItemField {

    public StringItemField(Column column, Object value, Method setterMethod,
                           Object entity){
        super(column, value, setterMethod, entity);
    }

    @Override
    public String getDisplay() {
        String s = (String) this.get();
        return s;
    }

    @Override
    public String getEdit(){
        String s = (String) this.get();
        return s;
    }
    
    @Override
    public boolean setEdit(Object objValue) {
        String s = (String) objValue;
        return this.set(s);
    }
    
    @Override
    protected Qt.ItemFlags getFlags() {
        Qt.ItemFlags flags = Qt.ItemFlag.createQFlags();
        flags.set(Qt.ItemFlag.ItemIsSelectable);
        flags.set(Qt.ItemFlag.ItemIsEnabled);
        flags.set(Qt.ItemFlag.ItemIsEditable);
        return flags;
    }

}
