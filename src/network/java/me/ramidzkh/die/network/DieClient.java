package me.ramidzkh.die.network;

import me.ramidzkh.die.Differ;
import me.ramidzkh.die.NodeMetadataProvider;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

public class DieClient implements Differ {

    private final HttpClient client;
    private final URI uri;

    public DieClient(HttpClient client, URI uri) {
        this.client = client;
        this.uri = uri;
    }

    @Override
    public <N> void diff(NodeMetadataProvider<N> metadata, N original, N modified, DiffVisitorProvider<N> provider) {
        try {
            var indexMap = new IndexMap<N>();
            var request = createRequest(metadata, original, modified, indexMap);
            var response = client.send(HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(request))
                    .build(), HttpResponse.BodyHandlers.ofByteArray()).body();
            applyResponse(provider, indexMap, response);
        } catch (IOException | InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static <N> byte[] createRequest(NodeMetadataProvider<N> metadata, N original, N modified,
            IndexMap<N> indexMap) throws IOException {
        var bytes = new ByteArrayOutputStream();
        var output = new DataOutputStream(bytes);

        output.writeInt(indexMap.insert(original));
        output.writeInt(indexMap.insert(modified));
        put(output, metadata, indexMap, original);
        put(output, metadata, indexMap, modified);

        return bytes.toByteArray();
    }

    private static <N> void put(DataOutputStream output, NodeMetadataProvider<N> metadata, IndexMap<N> indexMap, N node)
            throws IOException {
        output.writeUTF(metadata.getType(node));
        output.writeUTF(metadata.getLabel(node));

        var children = metadata.getChildren(node);
        output.writeInt(children.size());

        for (var child : children) {
            output.writeInt(indexMap.insert(child));
            put(output, metadata, indexMap, child);
        }
    }

    private static <N> void applyResponse(DiffVisitorProvider<N> provider, IndexMap<N> indexMap, byte[] response)
            throws IOException {
        var matches = new HashMap<N, N>();

        var input = new DataInputStream(new ByteArrayInputStream(response));
        var matchCount = input.readInt();

        for (var i = 0; i < matchCount; i++) {
            var a = indexMap.get(input.readInt());
            var b = indexMap.get(input.readInt());
            matches.put(a, b);
            matches.put(b, a);
        }

        var visitor = provider.createVisitor(new NodeMatches<>() {
            @Override
            public boolean areMatched(N a, N b) {
                return matches.get(a) == b;
            }

            @Override
            public @Nullable N getMatchedNode(N node) {
                return matches.get(node);
            }
        });

        var actionCount = input.readInt();

        for (var i = 0; i < actionCount; i++) {
            switch (input.readByte()) {
                case 0 -> visitor.delete(indexMap.get(input.readInt()));
                case 1 -> visitor.insert(indexMap.get(input.readInt()), indexMap.get(input.readInt()), input.readInt());
                case 2 -> visitor.move(indexMap.get(input.readInt()), indexMap.get(input.readInt()), input.readInt());
                case 3 -> visitor.replace(indexMap.get(input.readInt()), indexMap.get(input.readInt()));
            }
        }

        visitor.finish();
    }
}
