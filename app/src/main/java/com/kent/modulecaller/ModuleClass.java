package com.kent.modulecaller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 模块类的注解
 *
 * @author Kent
 * @version 1.0
 * @date 2019/05/02
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleClass {
    /**
     * @return 模块名称
     */
    String module();
}
