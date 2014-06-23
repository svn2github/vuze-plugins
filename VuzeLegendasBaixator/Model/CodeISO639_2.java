package Model;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Bruno Mendonça with IntelliJ IDEA.
 * User: brunol
 * Date: 20/06/11
 * Time: 15:23
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CodeISO639_2 {
    String value();
}
