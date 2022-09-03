package javaparser;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.PrimitiveType;
import me.ramidzkh.die.NodeMetadataProvider;

import java.util.List;

public class JavaParserNodeMetadataProvider implements NodeMetadataProvider<Node> {

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
}
