package org.ruoyi.system;

import net.jqwik.api.*;

class JqwikSmokeTest {

    @Property(tries = 5)
    void simpleProperty(@ForAll int x) {
        assert x == x;
    }
}
