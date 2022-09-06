package javaparser;

import com.github.javaparser.StaticJavaParser;
import me.ramidzkh.die.network.DieClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;

public class JavaParserTest {

    public static void main(String[] args) throws URISyntaxException {
        // welcome to the crablan, where you get top of the line diffing for 100% prosperity!
        var differ = new DieClient(HttpClient.newHttpClient(), new URI("http://10.24.0.1:42069/"));

        var a = StaticJavaParser.parse("""
                class A {

                    public static void main(String[] args) {
                        System.out.println("deez");
                    }
                }""");
        var b = StaticJavaParser.parse("""
                class B {

                    public static void main(String[] args) {
                        System.out.println("nutz");
                    }
                }""");

        differ.diff(new JavaParserNodeMetadataProvider(), a, b, matches -> new SoutVisitor<>()).join();
    }
}
