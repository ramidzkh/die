package javaparser;

import me.ramidzkh.die.Differ;

public class SoutVisitor<T> implements Differ.Visitor<T> {

    @Override
    public void delete(T node) {
        System.out.println("delete " + node);
    }

    @Override
    public void insert(T node, T parent, int pos) {
        System.out.println("insert " + node + " in " + parent + " at pos " + pos);
    }

    @Override
    public void move(T node, T parent, int pos) {
        System.out.println("move " + node + " into " + parent + " at pos " + pos);
    }

    @Override
    public void replace(T node, T x) {
        System.out.println("replace " + node + " with " + x);
    }

    @Override
    public void finish() {
        System.out.println("done");
    }
}
