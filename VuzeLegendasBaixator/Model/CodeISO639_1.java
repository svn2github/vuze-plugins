package Model;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Bruno Mendonça with IntelliJ IDEA.
 * User: brunol
 * Date: 20/06/11
 * Time: 14:15
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CodeISO639_1 {
    String value();
}
