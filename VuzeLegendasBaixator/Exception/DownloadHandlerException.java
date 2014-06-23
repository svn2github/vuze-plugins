package Exception;

/**
 * Created by IntelliJ IDEA.
 * User: Brunol
 * Date: 17/05/2010
 * Time: 11:48:47
 * To change this template use File | Settings | File Templates.
 */
public class DownloadHandlerException extends Exception {
// --------------------------- CONSTRUCTORS ---------------------------

    public DownloadHandlerException() {
        super();
    }
    
    public DownloadHandlerException(String message) {
	    super(message);
    }

    public DownloadHandlerException(String message, Throwable cause) {
        super(message, cause);
    }
}
