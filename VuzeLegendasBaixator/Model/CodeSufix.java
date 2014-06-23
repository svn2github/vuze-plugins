package Model;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Bruno Mendonça with IntelliJ IDEA.
 * User: brunol
 * Date: 07/12/11
 * Time: 14:29
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CodeSufix {
    String[] value();
}
