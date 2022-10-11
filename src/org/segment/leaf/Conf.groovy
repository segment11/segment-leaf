package org.segment.leaf

import groovy.transform.CompileStatic

@CompileStatic
@Singleton
class Conf {
    Map<String, String> params = [:]

    Conf loadArgs(String[] args) {
        def confFile = new File(this.class.getResource('/conf.properties').path)
        if (confFile.exists()) {
            confFile.readLines().findAll { it.trim() && !it.startsWith('#') }.each {
                def arr = it.split('=')
                if (arr.length == 2) {
                    params[arr[0]] = arr[1]
                }
            }
        }

        if (!args) {
            return this
        }

        for (arg in args) {
            def arr = arg.split('=')
            if (arr.size() == 2) {
                params[arr[0]] = arr[1]
            }
        }
        this
    }

    String get(String key) {
        params[key]
    }

    String getString(String key, String defaultValue) {
        get(key) ?: defaultValue
    }

    int getInt(String key, int defaultValue) {
        def s = get(key)
        s ? s as int : defaultValue
    }

    boolean isOn(String key) {
        '1' == get(key)
    }

    Conf put(String key, Object value) {
        params[key] = value.toString()
        this
    }

    Conf on(String key) {
        put(key, 1)
    }

    Conf off(String key) {
        put(key, 0)
    }

    @Override
    String toString() {
        params.toString()
    }
}
