package dev.devdoctor.engine.observe;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class JavaStackTraceParserTest {
    @Test void parsesCausesAndRanksApplicationFrames() {
        String trace = "Exception in thread \"main\" org.springframework.beans.factory.BeanCreationException: bean\n"
                + "\tat org.springframework.beans.Factory.create(Factory.java:10)\n"
                + "\tat com.acme.FonuClient.configure(FonuClient.java:47)\n"
                + "Caused by: java.lang.IllegalArgumentException: invalid header\n"
                + "\tat io.netty.handler.DefaultHeaders.set(DefaultHeaders.java:1019)\n"
                + "\tat com.acme.FonuClient.configure(FonuClient.java:47)";
        var parsed = new JavaStackTraceParser().parse(trace);
        assertThat(parsed.thread()).isEqualTo("main");
        assertThat(parsed.rootCause().exceptionClass()).isEqualTo("java.lang.IllegalArgumentException");
        assertThat(parsed.applicationFrames()).extracting(StackFrame::className).containsOnly("com.acme.FonuClient", "com.acme.FonuClient");
    }
}
