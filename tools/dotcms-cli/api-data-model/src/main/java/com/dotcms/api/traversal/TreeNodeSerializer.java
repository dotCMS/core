package com.dotcms.api.traversal;

import com.dotcms.model.file.File;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

/**
 * Serializer for TreeNode instances. This class is used to customize the serialization of TreeNode
 * objects to JSON format, allowing for the representation of the tree structure and associated file
 * information.
 */
public class TreeNodeSerializer extends StdSerializer<TreeNode> {

    /**
     * Constructs a new TreeNodeSerializer instance.
     *
     * @param t The class of the TreeNode objects to serialize.
     */
    public TreeNodeSerializer(Class<TreeNode> t) {
        super(t);
    }

    /**
     * Serializes the given TreeNode object to JSON format.
     *
     * @param node     The TreeNode object to serialize.
     * @param gen      The JSON generator to use for serialization.
     * @param provider The serializer provider to use for serialization.
     * @throws IOException If an I/O error occurs during serialization.
     */
    @Override
    public void serialize(TreeNode node, JsonGenerator gen, SerializerProvider provider)
            throws IOException {

        gen.writeStartObject();
        gen.writeStringField("name", node.folder().name());
        gen.writeNumberField("level", node.folder().level());

        if (!node.files().isEmpty()) {
            gen.writeArrayFieldStart("files");
            for (File file : node.files()) {
                //gen.writeObject(file);
                gen.writeStartObject();
                gen.writeStringField("fileName", file.fileName());
                gen.writeStringField("status", file.status());
                gen.writeStringField("language", file.language());
                gen.writeEndObject();
            }
            gen.writeEndArray();
        }

        if (!node.children().isEmpty()) {
            gen.writeArrayFieldStart("subFolders");
            for (TreeNode child : node.children()) {
                gen.writeObject(child);
            }
            gen.writeEndArray();
        }

        gen.writeEndObject();
    }
}

