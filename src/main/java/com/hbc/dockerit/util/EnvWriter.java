package com.hbc.dockerit.util;

import java.lang.reflect.Field;
import java.util.Map;

// I know, I know.  (╯°□°）╯︵ ┻━┻
// (anyone is welcome to replace with with something less hideous)
public class EnvWriter {

    public static void setEnvVar(String k, String v) {

        Map<String, String> sysEnv = System.getenv();
        try {
            Class c = sysEnv.getClass();
            Field f = c.getDeclaredField("m");
            f.setAccessible(true);
            Map<String, String> modifiedSysEnv = (Map<String, String>) f.get(sysEnv);
            modifiedSysEnv.put(k, v);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Couldn't modify env", e);
        }
    }
}
