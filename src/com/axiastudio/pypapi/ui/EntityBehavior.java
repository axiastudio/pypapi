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

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Tiziano Lattisi <tiziano at axiastudio.it>
 * 
 * The EntityBehavior class holds the dynamic properties values, retrieved from the
 * form's design. Thru these properties the framework can determine the
 * behaviors of the object, such the selection's path, the lookup's column, and
 * the search criteria.
 * 
 */
public class EntityBehavior implements IEntityBehavior {
    
    private String className;
    private List<Column> criteria;
    private List<Column> privates;
    private List<Column> exports;
    private List<Column> searchColumns;
    private HashMap<String, String> validators = new HashMap<>();
    
    public EntityBehavior(String className){
        this.className = className;
    }

    /**
     * @return the className
     */
    @Override
    public String getClassName() {
        return className;
    }

    /**
     * @return the criteria
     */
    @Override
    public List<Column> getCriteria() {
        return criteria;
    }

    /**
     * @param criteria the criteria to set
     */
    @Override
    public void setCriteria(List<Column> criteria) {
        this.criteria = criteria;
    }

    public List<Column> getPrivates() {
        return privates;
    }

    public void setPrivates(List<Column> privates) {
        this.privates = privates;
    }

    /**
     * @return the searchColumns
     */
    @Override
    public List<Column> getSearchColumns() {
        return searchColumns;
    }

    /**
     * @param searchColumns the searchColumns to set
     */
    @Override
    public void setSearchColumns(List<Column> searchColumns) {
        this.searchColumns = searchColumns;
    }

    public List<Column> getExports() {
        return exports;
    }

    public void setExports(List<Column> exports) {
        this.exports = exports;
    }

    @Override
    public void setReValidator(String widgetName, String re) {
        this.validators.put(widgetName, re);
    }

    @Override
    public String getReValidator(String widgetName) {
        return this.validators.get(widgetName);
    }

    @Override
    public Set<String> getReValidatorKeys() {
        return this.validators.keySet();
    }
    
}
