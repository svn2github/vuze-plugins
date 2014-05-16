/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pointdefence.az.plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;

/**
 *
 * @author wozza
 */
abstract class EqualsHashCodeProxy implements InvocationHandler {


    DiskManagerFileInfo target;
    int hashCode;
    
    public EqualsHashCodeProxy(DiskManagerFileInfo target) {
        this.target = target;
        this.hashCode = generateHashCode();
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    	String method_name = method.getName();
    	
    	if ( method_name.equals( "hashCode" )){
    		return(hashCode);
    	}else if ( method_name.equals( "equals" )){
    		return( hashCode == args[0].hashCode());	// parg - no idea why this is implemented like this, but woreva, need to fix mem leak
    	}else{
    		return method.invoke(target, args);
    	}
    }

    public int hashCode(){
        return hashCode;
    }
    public boolean equals(Object o){
        return hashCode == o.hashCode();
    }

    public abstract int generateHashCode();
}
