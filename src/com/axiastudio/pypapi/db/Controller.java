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

import com.axiastudio.pypapi.Resolver;
import com.axiastudio.pypapi.ui.Column;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;


/**
 *
 * @author Tiziano Lattisi <tiziano at axiastudio.it>
 */
public class Controller implements IController {
    
    private Class entityClass;
    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager=null;

    public Controller(EntityManagerFactory emf, Class entityClass){
        this.entityClass = entityClass;
        this.entityManagerFactory = emf;
    }

    public EntityManager getEntityManager() {
        return this.entityManagerFactory.createEntityManager();
    }
    
    public Store createCriteriaStore(HashMap criteria){
        EntityManager em = this.getEntityManager();
        Store store;
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Object> cq = cb.createQuery();
        Class<?> returnType = this.entityClass;
        Root from = cq.from(returnType);
        CriteriaQuery<Object> select = cq.select(from);
        for( Object k: criteria.keySet() ){
            Column column = (Column) k;
            String value = (String) criteria.get(column);
            // TODO: % and * should be are whildchars
            Predicate predicate = cb.like(from.get(column.getName().toLowerCase()), value);
            cq = cq.where(predicate);
        }
        TypedQuery<Object> tq = em.createQuery(cq);
        List<Object> result = tq.getResultList();
        store = new Store(result);
        return store;
    }
    
    @Override
    public Store createFullStore(){
        EntityManager em = this.getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(this.entityClass));
            Query q = em.createQuery(cq);
            List entities = q.getResultList();
            Store store = new Store(entities);
            return store;
        } finally {
            em.close();
        }
    }

    @Override
    public void edit(Object entity){
        EntityManager em = this.getEntityManager();
        em.getTransaction().begin();
        Object res = em.merge(entity);
        em.getTransaction().commit();
    }

    /*
     * The create method hooks the items of the collections to the parent entity
     * and merge.
     */
    public void create(Object entity){
        EntityManager em = this.getEntityManager();
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
                            try {
                                parentSetter.invoke(orphan, entity);
                            } catch (IllegalAccessException ex) {
                                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IllegalArgumentException ex) {
                                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (InvocationTargetException ex) {
                                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }
            }
        }
        em.getTransaction().begin();
        em.merge(entity);
        em.getTransaction().commit();
        em.close();
    }

        
    /*
     * The deprecated createWithReparent method works in a different way than
     * the create method.
     * This method unhooks the collections and persiste the entity, so that the
     * entity can have a primary key. Then set the parent to the items in the
     * collections, and hooks to the entity. Finally execute a merge of the
     * entity. 
     * With the implementations of JPA2 we dont need this machinery of unhooks
     * and hooks, the create method should works fine. 
     */
    @Deprecated
    public void createWithReparent(Object entity){
        EntityManager em = this.getEntityManager();
        List<Collection> collections = new ArrayList(); // collections of orphan items
        List<Method> collectionSetters = new ArrayList(); // the collections setters
        List<Method> parentSetters = new ArrayList(); // the parent setters
        for(Field f: entity.getClass().getDeclaredFields()){
            for( Annotation a: f.getAnnotations()){
                if( a.annotationType().equals(javax.persistence.OneToMany.class) ){
                    String name = f.getName();
                    Method getter = Resolver.getterFromFieldName(entity.getClass(), name);
                    Method setter = Resolver.setterFromFieldName(entity.getClass(), name, Collection.class); // collection setter
                    Collection<?> collection=null; 
                    try {
                        collection = (Collection) getter.invoke(entity); // orphan collection
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalArgumentException ex) {
                        Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InvocationTargetException ex) {
                        Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if( collection != null && collection.size() > 0 ){
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
                        try {
                            setter.invoke(entity, new ArrayList()); // a void collection
                        } catch (IllegalAccessException ex) {
                            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IllegalArgumentException ex) {
                            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (InvocationTargetException ex) {
                            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        collections.add(collection);
                        collectionSetters.add(setter);
                        parentSetters.add(parentSetter);
                    }
                }
            }
        }
        em.getTransaction().begin();
        em.persist(entity);
        for(int i=0; i<collections.size(); i++){
            Collection collection = collections.get(i);
            Method parentSetter = parentSetters.get(i);
            Method collectionSetter = collectionSetters.get(i);
            for (Iterator it = collection.iterator(); it.hasNext();) {
                Object orphan = it.next();
                try {
                    parentSetter.invoke(orphan, entity);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            try {
                collectionSetter.invoke(entity, collection);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        em.merge(entity);
        em.getTransaction().commit();
        em.close();        
    }
    
    @Override
    public Object refresh(Object entity){
        // TODO: controller refresh method
        return entity;
    }
    
    public String getEntityName() {
        String[] split = this.getClassName().split("\\.");
        return split[split.length-1];
    }
    

    public String getClassName() {
        return this.entityClass.getName();
    }
    
}
