/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package Interface;

/**
 *
 * @author Bruno
 */
public interface ILogManager {
    void info(String value);
    void warning(String value);
    void error(String value);
    void fatal(String value, Throwable e);
    void debug(String value);
}
