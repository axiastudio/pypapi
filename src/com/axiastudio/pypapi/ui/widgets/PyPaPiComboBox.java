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

import com.axiastudio.pypapi.db.Store;
import com.trolltech.qt.gui.QComboBox;
import com.trolltech.qt.gui.QWidget;

/**
 *
 * @author Tiziano Lattisi <tiziano at axiastudio.it>
 */
public class PyPaPiComboBox extends QComboBox {
    
    private Store lookupStore;

    public PyPaPiComboBox(QWidget qw) {
        super(qw);
        this.setEditable(true);
    }

    public PyPaPiComboBox() {
        this(null);
    }

    public void setLookupStore(Store lookupStore) {
        this.lookupStore = lookupStore;
        for(int i=0; i<lookupStore.size(); i++){
            Object object = lookupStore.get(i);
            String key = object.toString();
            this.addItem(key, object);
        }
    }
    
}
