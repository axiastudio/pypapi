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
package com.axiastudio.pypapi;

import com.axiastudio.pypapi.annotations.Callback;
import com.axiastudio.pypapi.annotations.CallbackType;
import com.axiastudio.pypapi.db.IFactory;
import com.axiastudio.pypapi.plugins.IPlugin;
import com.axiastudio.pypapi.ui.IForm;
import com.axiastudio.pypapi.ui.IUIFile;
import com.axiastudio.pypapi.ui.UIInspector;
import com.axiastudio.pypapi.ui.Window;

import javax.persistence.EntityManagerFactory;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * The Register class provides method for register and query utilities,
 * adapters and relations.
 * 
 * @author Tiziano Lattisi <tiziano at axiastudio.it>
 * 
 */
public class Register {
    
    private static HashMap utilities = new HashMap();
    private static HashMap adapters = new HashMap();
    private static HashMap relations = new HashMap();
    private static HashMap<Class, HashMap> callbacks = new HashMap();
    private static HashMap privates = new HashMap();
    private static HashMap plugins = new HashMap();

    /**
     * Registers the unnamed utility for the given interface.
     * 
     * @param utility The utility object to register
     * @param iface The interface implemented by the utility
     * 
     */
    public static void registerUtility(Object utility, Class iface){
        Register.registerUtility(utility, iface, ".");

    }

    /**
     * Registers the named utility for the given interface.
     * 
     * @param utility The utility object to register
     * @param iface The interface implemented by the utility
     * @param name The string name
     * 
     */
    public static void registerUtility(Object utility, Class iface, String name){
        HashMap hm;
        Object hmObject = Register.utilities.get(iface);
        if( hmObject != null ){
            hm = (HashMap) hmObject;
        } else {
            hm = new HashMap();
        }
        hm.put(name, utility);
        Register.utilities.put(iface, hm);
    }

    /**
     * Query the unnamed utility with the given interface.
     * 
     * @param iface The interface implemented by the utility
     * @return  The utility
     * 
     */
    public static Object queryUtility(Class iface){
        // TODO: returns the interface type?
        return Register.queryUtility(iface, ".");
    }

    /**
     * Query the named utility with the given interface.
     * 
     * @param iface The interface implemented by the utility
     * @param name The string name
     * @param noPrefix The string name has no prefix (aka. "name" instead of "pre.name")
     * @return  The utility
     * 
     */
    public static Object queryUtility(Class iface, String name, Boolean noPrefix){
        HashMap hm;
        Object utility = null;
        Object hmObject = Register.utilities.get(iface);
        if( hmObject != null ){
            hm = (HashMap) hmObject;
            if (noPrefix == false){
                utility = hm.get(name);
            } else {
                for (Object o: hm.keySet()){
                    String key = (String) o;
                    if( key.endsWith(name) ){
                        utility = hm.get(key);
                    }
                }
            }
        }
        return utility;
    }

    /**
     * Query the named utility with the given interface (default with prefix)
     * 
     * @param iface The interface implemented by the utility
     * @param name The string name
     * @return  The utility
     * 
     */
    public static Object queryUtility(Class iface, String name){
        return Register.queryUtility(iface, name, Boolean.FALSE);
    }

    /**
     * Registers adapters from a list of methods. This is useful in conjunction
     * with Resolver.adaptersFromEntityClass method.
     * 
     * @param methods The list of methods
     */
    public static void registerAdapters(List<Method> methods){
        for(Method adapter: methods){
            Class<?> toClass = adapter.getReturnType();
            Class<?>[] parameterTypes = adapter.getParameterTypes();
            Class<?> fromClass = null;
            if( parameterTypes.length == 1){
                fromClass = parameterTypes[0];
            }
            if( fromClass != null ){
                Register.registerAdapter(adapter, fromClass, toClass);
            }
        }
    }
    
    /**
     * Registers an adapter for a list of interfaces.
     * 
     * @param adatper The adapter itself
     * @param adapts The list of interfaces that the adapter adapts
     * @param provides The interface that the adapter implements
     */
    public static void registerAdapter(Object adatper, List<Class> adapts, Class provides){
        HashMap hm;
        for (Class c: adapts ){
            Object hmObject = Register.adapters.get(c);
            if( hmObject != null ){
                hm = (HashMap) hmObject;
            } else {
                hm = new HashMap();
            }
            hm.put(provides, adatper);
            Register.adapters.put(c, hm);
        }
    }

    /**
     * Registers an adapter for a single interface.
     * 
     * @param adapter The adapter object
     * @param adapts The class or interface that the adapter adapts
     * @param provides The class or interface that the adapter implements
     */
    public static void registerAdapter(Object adapter, Class adapts, Class provides){
        List adaptsList = new ArrayList();
        adaptsList.add(adapts);
        Register.registerAdapter(adapter, adaptsList, provides);
    }

    /**
     * Query an adapter.
     * 
     * @param adapts The class or interface of the adapted object
     * @param provides The class or interface that the adapter should implement
     * @return  The adapter
     */
    public static Object queryAdapter(Class adapts, Class provides){
        HashMap hm;
        Object adapter = null;
        Object hmObject = Register.adapters.get(adapts);
        if( hmObject != null ){
            hm = (HashMap) hmObject;
            adapter = hm.get(provides);
        }
        return adapter;
    }

    /**
     * Registers callbacks from a list of methods.
     * 
     * @param methods The list of methods
     */
    public static void registerCallbacks(List<Method> methods){
        for(Method callback: methods){
            Class<?>[] parameterTypes = callback.getParameterTypes();
            Class<?> forClass = null;
            if( parameterTypes.length == 1){
                forClass = parameterTypes[0];
            }
            CallbackType type = callback.getAnnotation(Callback.class).type();
            if( forClass != null ){
                Register.registerCallback(callback, forClass, type);
            }
        }
    }

    /**
     * Registers a callback for a class.
     * 
     * @param callback The callback itself
     * @param klass The class
     * @param type The callback type (before create, after create, etc)
     */
    public static void registerCallback(Object callback, Class klass, CallbackType type){
        HashMap classCallbacks = Register.callbacks.get(klass);
        if( classCallbacks == null ){
            HashMap<CallbackType, Method> hm = new HashMap();
            Register.callbacks.put(klass, hm);
            classCallbacks = hm;
        }
        classCallbacks.put(type, callback);
    }

    /**
     * Query the callback for a class.
     * 
     * @param klass The class
     */
    public static Method queryCallback(Class klass, CallbackType type){
        HashMap classCallbacks = Register.callbacks.get(klass);
        Method callback=null;
        if( classCallbacks != null ){
            callback = (Method) classCallbacks.get(type);
        }
        return callback;
    }

    /**
     * Registers privates from a list of methods.
     * 
     * @param methods The list of methods
     */
    public static void registerPrivates(List<Method> methods){
        for(Method privateMethod: methods){
            Class<?>[] parameterTypes = privateMethod.getParameterTypes();
            Class<?> forClass = null;
            if( parameterTypes.length == 1){
                forClass = parameterTypes[0];
            }
            if( forClass != null ){
                Register.registerPrivate(privateMethod, forClass);
            }
        }
    }

    /**
     * Registers a private for a class.
     * 
     * @param privateM The validator itself
     * @param klass The class
     */
    public static void registerPrivate(Object privateM, Class klass){
        Register.privates.put(klass, privateM);
    }

    /**
     * Query the validator for a class.
     * 
     * @param klass The class
     */
    public static Object queryPrivate(Class klass){
        return Register.privates.get(klass);
    }
    
    /**
     * Registers a named relation.
     * 
     * @param related The related object
     * @param object The object to which connect the related object
     * @param name The string name
     * 
     */
    public static void registerRelation(Object related, Object object, String name){
        HashMap hm;
        Object hmObject = Register.relations.get(object);
        if( hmObject != null ){
            hm = (HashMap) hmObject;
        } else {
            hm = new HashMap();
        }
        hm.put(name, related);
        Register.relations.put(object, hm);
    }
    /**
     * Remove the registered relations for an object.
     *
     * @param object The object that we want to remove the relations
     *
     */
    public static void removeRelations(Object object){
            Register.relations.remove(object);
    }

    /**
     * Query a related object.
     * 
     * @param object 
     * @param name The string name
     * @return The object relation
     * 
     */
    public static Object queryRelation(Object object, String name){
        HashMap hm;
        Object related = null;
        Object hmObject = Register.relations.get(object);
        if( hmObject != null ){
            hm = (HashMap) hmObject;
            related = hm.get(name);
        }
        return related;
    }

    /**
     * Registers a plugin for the form.
     * 
     * @param plugin The plugin.
     * @param factory The class of the form.
     */
    public static void registerPlugin(IPlugin plugin, Class factory){
        List<IPlugin>  factoryPlugins;
        Object hmObject = Register.plugins.get(factory);
        if( hmObject != null ){
            factoryPlugins = (List<IPlugin>) hmObject;
        } else {
            factoryPlugins = new ArrayList();
        }
        factoryPlugins.add(plugin);
        Register.plugins.put(factory, factoryPlugins);
    }
    
    /**
     * Query the list of plugins on the given form.
     * 
     * @param factory The class of the form
     * @return  The list of plugins
     * 
     */
    public static Object queryPlugins(Class factory){
        List<IPlugin> get = (List<IPlugin>) Register.plugins.get(factory);
        if( get == null ){
            return new ArrayList();
        }
        return get;
    }

        /**
     * Query the plugin on the given form, for the given pluginName.
     * 
     * @param factory The class of the form
     * @param pluginName The name of the plugin
     * @return  The list of plugins
     * 
     */
    public static Object queryPlugin(Class factory, String pluginName){
        List<IPlugin> iPlugins = (List<IPlugin>) Register.plugins.get(factory);
        for( IPlugin plugin: iPlugins ){
            if( plugin.getName().equals(pluginName) ){
                return plugin;
            }
        }
        return null;
    }
    
    public static void registerForm(EntityManagerFactory emf, String ui, Class factory){
        Register.registerForm(emf, ui, factory, Window.class);
    }        

    /**
     * Registers controller, factory and form as utilities.
     * 
     * @param emf The entity manager factory
     * @param ui The classpath of the ui form
     * @param factory The class factory
     * @param formClass The class of the form
     * 
     */
    public static void registerForm(EntityManagerFactory emf, String ui, Class factory, Class formClass){
        if( ui != null ){
            UIInspector inspector = new UIInspector(ui, factory.getName());
        } else {
            UIInspector inspector = new UIInspector(factory, factory.getName());
        }
        Register.registerUtility(formClass, IForm.class, factory.getName());
        Register.registerUtility(factory, IFactory.class, factory.getName());
        Register.registerUtility(ui, IUIFile.class, factory.getName());
    }
}
