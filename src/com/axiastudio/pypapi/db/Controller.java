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
package com.axiastudio.pypapi.db;

import com.axiastudio.pypapi.Register;
import com.axiastudio.pypapi.Resolver;
import com.axiastudio.pypapi.annotations.CallbackType;
import com.axiastudio.pypapi.ui.CellEditorType;
import com.axiastudio.pypapi.ui.Column;

import javax.persistence.EntityManager;
import javax.persistence.RollbackException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Tiziano Lattisi <tiziano at axiastudio.it>
 */
public class Controller implements IController {
    
    private Class entityClass;
    private EntityManager entityManager;

    
    public Controller(EntityManager em, Class klass){
        entityClass = klass;
        entityManager = em;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }
    
    @Override
    public Store createCriteriaStore(Map criteria){
        return this.createCriteriaStore(criteria, 0);
    }

    @Override
    public Store createCriteriaStore(Map criteria, Integer limit){
        if( this.entityClass == null ){
            return null;
        }
        EntityManager em = this.getEntityManager();
        Store store;
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Object> cq = cb.createQuery();
        Class<?> returnType = this.entityClass;
        Root from = cq.from(returnType);
        List<Predicate> predicates = new ArrayList();
        Map<String, Path> paths = new HashMap();
        for( Object k: criteria.keySet() ){
            Column column = (Column) k;
            Predicate predicate=null;
            Path path = null;
            if( !column.getName().contains(".") ){
                path = from.get(column.getName().toLowerCase());
            } else {
                for( String token: column.getName().split("\\.") ){
                    if( path == null ){
                        if( paths.containsKey(token)){
                            path = paths.get(token);
                        } else {
                            path = from.get(token);
                            paths.put(token, path);
                        }
                    } else {
                        path = path.get(token);
                    }
                }
            }
            if( column.getEditorType().equals(CellEditorType.STRING) ){
                String value = (String) criteria.get(column);
                value = value.replace("*", "%");
                if( !value.endsWith("%") ){
                    value += "%";
                }
                predicate = cb.like(cb.upper(path), value.toUpperCase());
            } else if( column.getEditorType().equals(CellEditorType.INTEGER) ){
                Integer value = (Integer) criteria.get(column);
                predicate = cb.equal(path, value);
            } else if( column.getEditorType().equals(CellEditorType.LONG) ){
                Long value = (Long) criteria.get(column);
                predicate = cb.equal(path, value);
            } else if( column.getEditorType().equals(CellEditorType.BOOLEAN) ){
                Boolean value = (Boolean) criteria.get(column);
                predicate = cb.equal(path, value);
            } else if( column.getEditorType().equals(CellEditorType.DATE) ){
                List values = (List) criteria.get(column);
                GregorianCalendar gcStart = (GregorianCalendar) values.get(0);
                GregorianCalendar gcEnd = new GregorianCalendar();
                gcEnd.setTime(gcStart.getTime());
                Integer d = (Integer) values.get(1);
                gcEnd.add(Calendar.DAY_OF_MONTH, d);
                if ( d>=0 ) {
                    predicate = cb.and(cb.greaterThanOrEqualTo(path, gcStart.getTime()),
                        cb.lessThan(path, gcEnd.getTime()));
                } else {
                    predicate = cb.and(cb.greaterThanOrEqualTo(path, gcEnd.getTime()),
                            cb.lessThan(path, gcStart.getTime()));
                }
            } else if( column.getEditorType().equals(CellEditorType.CHOICE) || column.getEditorType().equals(CellEditorType.LOOKUP)){
                Object value = criteria.get(column);
                predicate = cb.equal(path, value);
            }
            if( predicate != null ){
                predicates.add(predicate);
            }
        }
        Method method = (Method) Register.queryUtility(ICriteriaFactory.class, this.getClassName());
        if( method != null ){
            try {
                Predicate predicate = (Predicate) method.invoke(null, cb, from);
                if( predicate != null ){
                    predicates.add(predicate);
                }
            } catch (IllegalAccessException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        cq.select(from);
        if( predicates.size()>0 ){
            cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        }
        TypedQuery<Object> tq = em.createQuery(cq);
        if( limit != 0 ){
            tq.setMaxResults(limit);
        }
        List<Object> result = tq.getResultList();
        store = new Store(result);
        return store;
    }

    @Override
    public Store createFullStore(){
        Map<Column, Object> criteriaMap = new HashMap();
        return this.createCriteriaStore(criteriaMap);
    }
    
    @Override
    public Store createNewStore(){
        List<Object> listStore = new ArrayList();
        try {
            Object instance = this.entityClass.newInstance();
            listStore.add(instance);
        } catch (InstantiationException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new Store(listStore);
    }

    /*
     * The parentize method hooks the items of the collections to the parent
     * entity.
     */
    private void parentize(Object entity){
        for(Field f: entity.getClass().getDeclaredFields()){
            for( Annotation a: f.getAnnotations()){
                if( a.annotationType().equals(javax.persistence.OneToMany.class) ){
                    String name = f.getName();
                    Method getter = Resolver.getterFromFieldName(entity.getClass(), name);
                    Collection collection=null;
                    try {
                        collection = (Collection) getter.invoke(entity); // orphan collection
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalArgumentException ex) {
                        Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InvocationTargetException ex) {
                        Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if( collection != null && collection.size() > 0){
                        Method parentSetter = null;
                        for( Method m: a.annotationType().getMethods()){
                           if( "mappedBy".equals(m.getName()) ){
                                String fk=null;
                                try {
                                    fk = (String) m.invoke(a);
                                } catch (IllegalAccessException ex) {
                                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (IllegalArgumentException ex) {
                                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (InvocationTargetException ex) {
                                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                parentSetter = Resolver.setterFromFieldName(collection.iterator().next().getClass(), fk, entity.getClass()); // parent setter
                            }
                        }
                        for (Iterator it = collection.iterator(); it.hasNext();) {
                            Object orphan = it.next();
                            if( orphan != null ){
                                try {
                                    parentSetter.invoke(orphan, entity);
                                } catch (IllegalAccessException ex) {
                                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (IllegalArgumentException ex) {
                                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (InvocationTargetException ex) {
                                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (NullPointerException ex) {
                                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public Validation commit(Object entity) throws RollbackException {
        // XXX: if no CascadeType.ALL?
        //this.parentize(entity);
        
        // BEFORECOMMIT
        Method beforeCommit = Register.queryCallback(entity.getClass(), CallbackType.BEFORECOMMIT);
        Validation beforeValidation = new Validation(true);
        if( beforeCommit != null ){
            try {
                beforeValidation = (Validation) beforeCommit.invoke(null, entity);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if( beforeValidation.getResponse() == true ){
            this.parentize(entity);
            EntityManager em = this.getEntityManager();
            Object merged=null;
            try {
                if (this.getId(entity) == null) {
                    em.getTransaction().begin();
                    em.persist(entity);
                    em.getTransaction().commit();
                    merged = entity;
                } else {
                    em.getTransaction().begin();
                    merged = em.merge(entity);
                    em.getTransaction().commit();
                }
            } catch (RollbackException ex){
                Validation val = new Validation();
                val.setResponse(false);
                val.setMessage(ex.getLocalizedMessage());
                return val;
            }
            em.refresh(merged);

            beforeValidation.setEntity(merged);
            
            // AFTER COMMIT

            Method afterCommit = Register.queryCallback(entity.getClass(), CallbackType.AFTERCOMMIT);
            if( afterCommit != null ){
                try {
                    Validation afterValidation = (Validation) afterCommit.invoke(null, merged);
                    afterValidation.setEntity(merged);
                    return afterValidation;
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
        }
        return beforeValidation;
    }
    
    @Override
    public void delete(Object entity) throws RollbackException{
        EntityManager em = this.getEntityManager();
        em.getTransaction().begin();
        Object merged = em.merge(entity);
        em.remove(merged);
        em.getTransaction().commit();
    }
            
    @Override
    public Object refresh(Object entity){
        EntityManager em = this.getEntityManager();
        if( !em.contains(entity) ){
            Long id = this.getId(entity);
            if( id != null ){
                entity = this.get(id);
            } else {
                return entity;
            }
        }
        em.refresh(entity);
        return entity;
    }
    
    public String getEntityName() {
        if( this.entityClass == null ){
            return null;
        }
        String[] split = this.getClassName().split("\\.");
        return split[split.length-1];
    }
    

    public String getClassName() {
        if( this.entityClass == null ){
            return null;
        }
        return this.entityClass.getName();
    }

    public Class getEntityClass() {
        return entityClass;
    }

    @Override
    public Object get(Long id) {
        EntityManager em = this.getEntityManager();
        Object entity = em.find(this.getEntityClass(), id);
        return entity;
    }

    public Long getId(Object entity){
        Long id=null;
        Method getId;
        try {
            getId = entity.getClass().getMethod("getId");
            id = (Long) getId.invoke(entity);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
        return id;
    }

    @Override
    protected void finalize() throws Throwable {
        entityManager.clear();
        entityManager.close();
        entityManager = null;
        super.finalize();
    }
}
    