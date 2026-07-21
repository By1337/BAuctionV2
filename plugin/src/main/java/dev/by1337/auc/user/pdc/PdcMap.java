package dev.by1337.auc.user.pdc;

import dev.by1337.sync.common.packet.ByteBufCodecs;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.booleans.BooleanLists;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleLists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongLists;

import java.util.*;
import java.util.function.Function;

public class PdcMap {

    private final Map<String, StoredValue> map = new HashMap<>();

    public int getInt(String key){
        var v = map.get(key);
        if (v instanceof StoredInt(int value)) return value;
        return 0;
    }
    public void setInt(String key, int v){
        map.put(key, new StoredInt(v));
    }

    public long getLong(String key) {
        var v = map.get(key);
        if (v instanceof StoredLong(long value)) return value;
        return 0L;
    }

    public void setLong(String key, long v) {
        map.put(key, new StoredLong(v));
    }

    public double getDouble(String key) {
        var v = map.get(key);
        if (v instanceof StoredDouble(double value)) return value;
        return 0.0;
    }

    public void setDouble(String key, double v) {
        map.put(key, new StoredDouble(v));
    }

    public String getString(String key) {
        var v = map.get(key);
        if (v instanceof StoredString(String value)) return value;
        return "";
    }

    public void setString(String key, String v) {
        map.put(key, new StoredString(v));
    }

    public boolean getBoolean(String key) {
        var v = map.get(key);
        if (v instanceof StoredBoolean(boolean value)) return value;
        return false;
    }

    public void setBoolean(String key, boolean v) {
        map.put(key, new StoredBoolean(v));
    }

    public IntList getIntList(String key) {
        var v = map.get(key);
        if (v instanceof StoredList(List<StoredValue> list1)) {
            IntList result = new IntArrayList(list1.size());
            for (StoredValue item : list1) {
                if (item instanceof StoredInt(int value)) result.add(value);
            }
            return result;
        }
        return IntLists.EMPTY_LIST;
    }

    public LongList getLongList(String key) {
        var v = map.get(key);
        if (v instanceof StoredList(List<StoredValue> list1)) {
            LongList result = new LongArrayList(list1.size());
            for (StoredValue item : list1) {
                if (item instanceof StoredLong(long value)) result.add(value);
            }
            return result;
        }
        return LongLists.EMPTY_LIST;
    }

    public DoubleList getDoubleList(String key) {
        var v = map.get(key);
        if (v instanceof StoredList(List<StoredValue> list1)) {
            DoubleList result = new DoubleArrayList(list1.size());
            for (StoredValue item : list1) {
                if (item instanceof StoredDouble(double value)) result.add(value);
            }
            return result;
        }
        return DoubleLists.EMPTY_LIST;
    }

    public BooleanList getBooleanList(String key) {
        var v = map.get(key);
        if (v instanceof StoredList(List<StoredValue> list1)) {
            BooleanList result = new BooleanArrayList(list1.size());
            for (StoredValue item : list1) {
                if (item instanceof StoredBoolean(boolean value)) result.add(value);
            }
            return result;
        }
        return BooleanLists.EMPTY_LIST;
    }

    public List<String> getStringList(String key) {
        var v = map.get(key);
        if (v instanceof StoredList(List<StoredValue> list1)) {
            List<String> result = new ArrayList<>(list1.size());
            for (StoredValue item : list1) {
                if (item instanceof StoredString(String value)) result.add(value);
            }
            return result;
        }
        return Collections.emptyList();
    }


    public void setIntList(String key, IntList values) {
        List<StoredValue> list = new ArrayList<>(values.size());
        for (int v : values) list.add(new StoredInt(v));
        map.put(key, new StoredList(list));
    }

    public void setLongList(String key, LongList values) {
        List<StoredValue> list = new ArrayList<>(values.size());
        for (long v : values) list.add(new StoredLong(v));
        map.put(key, new StoredList(list));
    }

    public void setDoubleList(String key, DoubleList values) {
        List<StoredValue> list = new ArrayList<>(values.size());
        for (double v : values) list.add(new StoredDouble(v));
        map.put(key, new StoredList(list));
    }

    public void setBooleanList(String key, BooleanList values) {
        List<StoredValue> list = new ArrayList<>(values.size());
        for (boolean v : values) list.add(new StoredBoolean(v));
        map.put(key, new StoredList(list));
    }

    public void setStringList(String key, List<String> values) {
        List<StoredValue> list = new ArrayList<>(values.size());
        for (String v : values) list.add(new StoredString(v));
        map.put(key, new StoredList(list));
    }

    public static PdcMap read(ByteBuf buf){
        PdcMap map = new PdcMap();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            var key = ByteBufCodecs.readUtf8(buf);
            StoredValue v = StoredType.VALUES[buf.readByte()].reader.apply(buf);
            map.map.put(key, v);
        }
        return map;
    }

    public void write(ByteBuf buf){
        buf.writeInt(map.size());
        for (var e : map.entrySet()) {
            ByteBufCodecs.writeUtf8(buf, e.getKey());
            StoredValue v = e.getValue();
            buf.writeByte(v.type().ordinal());
            v.write(buf);
        }
    }

    public enum StoredType {
        INT(StoredInt::read),
        LONG(StoredLong::read),
        DOUBLE(StoredDouble::read),
        BOOLEAN(StoredBoolean::read),
        STRING(StoredString::read),
        LIST(StoredList::read),
        ;
        private static final StoredType[] VALUES = values();
        private final Function<ByteBuf, StoredValue> reader;

        StoredType(Function<ByteBuf, StoredValue> reader) {
            this.reader = reader;
        }
    }

    public interface StoredValue {
        StoredType type();

        void write(ByteBuf buf);
    }

    public record StoredInt(int value) implements StoredValue {
        public static StoredInt read(ByteBuf buf) {
            return new StoredInt(buf.readInt());
        }

        @Override
        public StoredType type() {
            return StoredType.INT;
        }

        @Override
        public void write(ByteBuf buf) {
            buf.writeInt(value);
        }
    }

    public record StoredLong(long value) implements StoredValue {
        public static StoredLong read(ByteBuf buf) {
            return new StoredLong(buf.readLong());
        }

        @Override
        public StoredType type() {
            return StoredType.LONG;
        }

        @Override
        public void write(ByteBuf buf) {
            buf.writeLong(value);
        }
    }

    public record StoredDouble(double value) implements StoredValue {
        public static StoredDouble read(ByteBuf buf) {
            return new StoredDouble(buf.readDouble());
        }

        @Override
        public StoredType type() {
            return StoredType.DOUBLE;
        }

        @Override
        public void write(ByteBuf buf) {
            buf.writeDouble(value);
        }
    }

    public record StoredBoolean(boolean value) implements StoredValue {
        public static StoredBoolean read(ByteBuf buf) {
            return new StoredBoolean(buf.readBoolean());
        }

        @Override
        public StoredType type() {
            return StoredType.BOOLEAN;
        }

        @Override
        public void write(ByteBuf buf) {
            buf.writeBoolean(value);
        }
    }

    public record StoredString(String value) implements StoredValue {
        public static StoredString read(ByteBuf buf) {
            return new StoredString(ByteBufCodecs.readUtf8(buf));
        }

        @Override
        public StoredType type() {
            return StoredType.STRING;
        }

        @Override
        public void write(ByteBuf buf) {
            ByteBufCodecs.writeUtf8(buf, value);
        }
    }

    public record StoredList(List<StoredValue> list) implements StoredValue {
        public static StoredList read(ByteBuf buf) {
            int size = buf.readInt();
            List<StoredValue> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(StoredType.VALUES[buf.readByte()].reader.apply(buf));
            }
            return new StoredList(list);
        }

        @Override
        public StoredType type() {
            return StoredType.LIST;
        }

        @Override
        public void write(ByteBuf buf) {
            buf.writeInt(list.size());
            for (StoredValue value : list) {
                buf.writeByte(value.type().ordinal());
                value.write(buf);
            }
        }
    }
}
