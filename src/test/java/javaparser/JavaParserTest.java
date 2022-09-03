package javaparser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.PrimitiveType;
import me.ramidzkh.die.Differ;
import me.ramidzkh.die.NodeMetadataProvider;
import me.ramidzkh.die.network.DieClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.List;

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

        differ.diff(new NodeMetadataProvider<Node>() {
            @Override
            public List<Node> getChildren(Node node) {
                return node.getChildNodes();
            }

            @Override
            public String getType(Node node) {
                return node.getClass().getSimpleName();
            }

            @Override
            public String getLabel(Node node) {
                var label = "";

                if (node instanceof Name name) {
                    label = name.getIdentifier();
                } else if (node instanceof SimpleName name) {
                    label = name.getIdentifier();
                } else if (node instanceof StringLiteralExpr literal) {
                    label = literal.asString();
                } else if (node instanceof BooleanLiteralExpr expr) {
                    label = Boolean.toString(expr.getValue());
                } else if (node instanceof LiteralStringValueExpr literal) {
                    label = literal.getValue();
                } else if (node instanceof PrimitiveType primitive) {
                    label = primitive.asString();
                } else if (node instanceof Modifier modifier) {
                    label = modifier.getKeyword().asString();
                }

                return label;
            }
        }, a, b, matches -> new Differ.Visitor<>() {
            @Override
            public void delete(Node node) {
                System.out.println("delete " + node);
            }

            @Override
            public void insert(Node node, Node parent, int pos) {
                System.out.println("insert " + node + " in " + parent + " at pos " + pos);
            }

            @Override
            public void move(Node node, Node parent, int pos) {
                System.out.println("move " + node + " into " + parent + " at pos " + pos);
            }

            @Override
            public void replace(Node node, Node x) {
                System.out.println("replace " + node + " with " + x);
            }

            @Override
            public void finish() {
                System.out.println("done");
            }
        });
    }
}
