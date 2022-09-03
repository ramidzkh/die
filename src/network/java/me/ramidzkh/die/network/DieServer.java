package me.ramidzkh.die.network;

import com.google.common.collect.ArrayListMultimap;
import io.javalin.Javalin;
import me.ramidzkh.die.Differ;
import me.ramidzkh.die.NodeMetadataProvider;

import java.io.*;
import java.util.*;

public class DieServer {

    private record TL(String type, String label) {

    }

    private record Data(Set<Integer> world, ArrayListMultimap<Integer, Integer> children,
            Map<Integer, TL> typesAndLabels) {

    }

    public static void start(Differ differ, Javalin app) {
        app.post("/", ctx -> {
            var input = new DataInputStream(ctx.bodyAsInputStream());
            var data = new Data(new HashSet<>(), ArrayListMultimap.create(), new HashMap<>());

            var original = input.readInt();
            var modified = input.readInt();

            readTree(input, original, data);
            readTree(input, modified, data);

            differ.diff(new NodeMetadataProvider<>() {
                @Override
                public List<Integer> getChildren(Integer node) {
                    return data.children().get(node);
                }

                @Override
                public String getType(Integer node) {
                    return data.typesAndLabels().get(node).type();
                }

                @Override
                public String getLabel(Integer node) {
                    return data.typesAndLabels().get(node).label();
                }
            }, original, modified, matches -> {
                var response = new ByteArrayOutputStream();
                var output = new DataOutputStream(response);
                int toReplace1, toReplace2;
                var replacement1 = 0;

                try {
                    toReplace1 = response.size();
                    output.writeInt(0);

                    for (var node : data.world()) {
                        var to = matches.getMatchedNode(node);

                        if (to != null) {
                            output.writeInt(node);
                            output.writeInt(to);
                            replacement1++;
                        }
                    }

                    toReplace2 = response.size();
                    output.writeInt(0);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                var finalReplacement = replacement1;

                class Visitor implements Differ.Visitor<Integer> {

                    int count;

                    @Override
                    public void delete(Integer node) {
                        try {
                            output.writeByte(0);
                            output.writeInt(node);
                            count++;
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    }

                    @Override
                    public void insert(Integer node, Integer parent, int pos) {
                        try {
                            output.writeByte(1);
                            output.writeInt(node);
                            output.writeInt(parent);
                            output.writeInt(pos);
                            count++;
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    }

                    @Override
                    public void move(Integer node, Integer parent, int pos) {
                        try {
                            output.writeByte(2);
                            output.writeInt(node);
                            output.writeInt(parent);
                            output.writeInt(pos);
                            count++;
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    }

                    @Override
                    public void replace(Integer node, Integer x) {
                        try {
                            output.writeByte(3);
                            output.writeInt(node);
                            output.writeInt(x);
                            count++;
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    }

                    @Override
                    public void finish() {
                        var bytes = response.toByteArray();
                        bytes[toReplace1] = (byte) (finalReplacement >>> 24);
                        bytes[toReplace1 + 1] = (byte) (finalReplacement >>> 16);
                        bytes[toReplace1 + 2] = (byte) (finalReplacement >>> 8);
                        bytes[toReplace1 + 3] = (byte) (finalReplacement);
                        bytes[toReplace2] = (byte) (count >>> 24);
                        bytes[toReplace2 + 1] = (byte) (count >>> 16);
                        bytes[toReplace2 + 2] = (byte) (count >>> 8);
                        bytes[toReplace2 + 3] = (byte) (count);

                        ctx.result(bytes);
                    }
                }

                return new Visitor();
            });
        });
    }

    private static void readTree(DataInput input, int into, Data data) throws IOException {
        data.world().add(into);
        data.typesAndLabels().put(into, new TL(input.readUTF(), input.readUTF()));

        var children = input.readInt();

        for (var i = 0; i < children; i++) {
            var child = input.readInt();
            data.children().put(into, child);
            readTree(input, child, data);
        }
    }

    public static void main(String[] args) {
        var app = Javalin.create();
        app.jettyServer().setServerPort(42069);

        if (args.length >= 1 && !args[0].equals("*")) {
            app.jettyServer().setServerHost(args[0]);
        }

        if (args.length >= 2) {
            app.jettyServer().setServerPort(Integer.parseInt(args[1]));
        }

        app.start();
        start(ServiceLoader.load(Differ.class).findFirst().get(), app);
    }
}
