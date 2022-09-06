package me.ramidzkh.die.network;

import com.google.common.collect.ArrayListMultimap;
import io.javalin.Javalin;
import me.ramidzkh.die.Differ;
import me.ramidzkh.die.NodeMetadataProvider;
import org.eclipse.jetty.server.AbstractConnector;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DieServer {

    private record TL(int type, int label) {

    }

    private record Data(Set<Integer> world, ArrayListMultimap<Integer, Integer> children,
            Map<Integer, TL> typesAndLabels,
            IndexMap<String> stringPool) implements NodeMetadataProvider<Integer> {

        @Override
        public List<Integer> getChildren(Integer node) {
            return children.get(node);
        }

        @Override
        public String getType(Integer node) {
            return stringPool.get(typesAndLabels.get(node).type());
        }

        @Override
        public String getLabel(Integer node) {
            return stringPool.get(typesAndLabels.get(node).label());
        }
    }

    private static CompletableFuture<byte[]> process(Differ differ, byte[] stream)
            throws IOException {
        var input = new DataInputStream(new ByteArrayInputStream(stream));
        var data = new Data(new HashSet<>(), ArrayListMultimap.create(), new HashMap<>(), new IndexMap<>());

        var original = input.readInt();
        var modified = input.readInt();

        readTree(input, original, data);
        readTree(input, modified, data);

        var stringPoolSize = input.readInt();

        for (var i = 0; i < stringPoolSize; i++) {
            data.stringPool().insert(input.readUTF());
        }

        var response = new ByteArrayOutputStream();
        var output = new DataOutputStream(response);
        var replacements = new int[2];

        return differ.diff(data, original, modified, matches -> {
            try {
                output.writeInt(0);
                output.writeInt(0);

                for (var node : data.world()) {
                    var to = matches.getMatchedNode(node);

                    if (to != null) {
                        output.writeInt(node);
                        output.writeInt(to);
                        replacements[0]++;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            class Visitor implements Differ.Visitor<Integer> {

                @Override
                public void delete(Integer node) {
                    try {
                        output.writeByte(0);
                        output.writeInt(node);
                        replacements[1]++;
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
                        replacements[1]++;
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
                        replacements[1]++;
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
                        replacements[1]++;
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                }
            }

            return new Visitor();
        }).thenApply(it -> {
            var bytes = response.toByteArray();
            var matchCount = replacements[0];
            var actionCount = replacements[1];

            bytes[0] = (byte) (matchCount >>> 24);
            bytes[1] = (byte) (matchCount >>> 16);
            bytes[2] = (byte) (matchCount >>> 8);
            bytes[3] = (byte) matchCount;
            bytes[4] = (byte) (actionCount >>> 24);
            bytes[5] = (byte) (actionCount >>> 16);
            bytes[6] = (byte) (actionCount >>> 8);
            bytes[7] = (byte) actionCount;

            return bytes;
        });
    }

    private static void readTree(DataInput input, int into, Data data) throws IOException {
        data.world().add(into);
        data.typesAndLabels().put(into, new TL(input.readInt(), input.readInt()));

        var children = input.readInt();

        for (var i = 0; i < children; i++) {
            var child = input.readInt();
            data.children().put(into, child);
            readTree(input, child, data);
        }
    }

    public static void start(Differ differ, Javalin app) {
        for (var connector : app.jettyServer().server().getConnectors()) {
            if (connector instanceof AbstractConnector abstractConnector) {
                abstractConnector.setIdleTimeout(10000000000000L);
            }
        }

        app.post("/", ctx -> {
            String source = ctx.header("source");
            try {
                ctx.future(process(differ, ctx.bodyAsBytes())
                        .thenApply($ -> {
                            System.out.println("done: " + source);
                            return $;
                        })
                        .thenApply(ByteArrayInputStream::new));
            } catch (Throwable e) {
                new Throwable("error: " + source, e).printStackTrace();
                ctx.status(500);
            }
        });
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
