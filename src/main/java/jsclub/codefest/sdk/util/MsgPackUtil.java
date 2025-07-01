package jsclub.codefest.sdk.util;

import com.google.gson.Gson;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import java.io.IOException;

public class MsgPackUtil {
    /**
     * Encodes a message into MsgPack format from an object.
     *
     * @param obj The object to encode.
     * @return The encoded MsgPack message.
     * @throws IOException If an I/O error occurs.
     */
    public static byte[] encodeFromObject(Object obj) throws IOException {
        String jsonString = new Gson().toJson(obj);
        return encodeFromJSONString(jsonString);
    }

    /**
     * Encodes a message into MsgPack format from a JSON string.
     *
     * @param jsonString The JSON string to encode.
     * @return The encoded MsgPack message.
     * @throws IOException If an I/O error occurs.
     */
    public static byte[] encodeFromJSONString(String jsonString) throws IOException {
        Value value = ValueFactory.newString(jsonString);

        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        packer.packValue(value);
        packer.close();

        return packer.toByteArray();
    }

    /**
     * Decodes a MsgPack message.
     *
     * @param arg The message to decode.
     * @return The decoded message as a string.
     * @throws IOException If an I/O error occurs.
     */
    public static String decode(Object arg) throws IOException {
        byte[] data = (byte[]) arg;
        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
        Value msgpackValue =  unpacker.unpackValue();
        return msgpackValue.toJson();
    }
}
