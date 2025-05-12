package cn.chengzhiya.itemsaddertocraftengine.entity;

import lombok.Data;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

@Data
@SuppressWarnings({"unused", "unchecked"})
public final class YamlConfiguration {
    private Map<Object, Object> data;

    public YamlConfiguration() {
        this.data = new HashMap<>();
    }

    public static YamlConfiguration loadConfiguration(File file) {
        try {
            return loadConfiguration(Files.newInputStream(file.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static YamlConfiguration loadConfiguration(InputStream in) {
        try (in) {
            YamlConfiguration config = new YamlConfiguration();
            Yaml yaml = new Yaml();
            config.setData(yaml.load(in));
            if (config.getData() == null) {
                config.setData(new HashMap<>());
            }
            return config;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void set(String path, Object value) {
        String[] keys = path.split("\\.");
        Map<Object, Object> current = this.data;

        if (value instanceof YamlConfiguration) {
            value = ((YamlConfiguration) value).getData();
        }
        if (value instanceof Collection) {
            value = processCollection((Collection<?>) value);
        }

        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];
            current.putIfAbsent(key, new HashMap<>());
            current = (Map<Object, Object>) current.get(key);
        }
        current.put(keys[keys.length - 1], value);
    }

    private List<Object> processCollection(Collection<?> collection) {
        List<Object> processed = new ArrayList<>();
        for (Object item : collection) {
            if (item instanceof YamlConfiguration) {
                processed.add(((YamlConfiguration) item).getData());
            } else if (item instanceof Collection) {
                processed.add(processCollection((Collection<?>) item));
            } else if (item instanceof Map) {
                processed.add(processMap((Map<?, ?>) item));
            } else {
                processed.add(item);
            }
        }
        return processed;
    }

    private Map<String, Object> processMap(Map<?, ?> map) {
        Map<String, Object> processed = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof YamlConfiguration) {
                processed.put(key.toString(), ((YamlConfiguration) value).getData());
            } else if (value instanceof Collection) {
                processed.put(key.toString(), processCollection((Collection<?>) value));
            } else if (value instanceof Map) {
                processed.put(key.toString(), processMap((Map<?, ?>) value));
            } else {
                processed.put(key.toString(), value);
            }
        }
        return processed;
    }

    public Object get(String path, boolean split) {
        try {
            if (split) {
                String[] keys = path.split("\\.");
                Map<Object, Object> current = this.data;
                for (String key : keys) {
                    Object value = current.get(key);
                    if (!(value instanceof Map)) {
                        return value;
                    }
                    current = (Map<Object, Object>) value;
                }
            }
            return this.data.get(path);
        } catch (Exception ignored) {
        }
        return null;
    }

    public Object get(String path) {
        return get(path, true);
    }

    public void save(File file) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            yaml.dump(this.data, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public YamlConfiguration getConfigurationSection(String path) {
        try {
            String[] keys = path.split("\\.");
            Map<Object, Object> current = this.data;
            for (String key : keys) {
                current = (Map<Object, Object>) current.get(key);
            }
            if (current == null) {
                return null;
            }

            YamlConfiguration section = new YamlConfiguration();
            section.setData(current);
            return section;
        } catch (Exception ignored) {
        }
        return null;
    }

    public Set<String> getKeys() {
        if (this.data == null) {
            return new HashSet<>();
        }
        HashSet<String> keys = new HashSet<>();
        for (Object object : this.data.keySet()) {
            keys.add(object.toString());
        }
        return keys;
    }

    public String getString(String path) {
        return (String) this.get(path);
    }

    public Integer getInt(String path) {
        return (Integer) this.get(path);
    }

    public boolean getBoolean(String path) {
        Object value = this.get(path);
        return value != null ? (Boolean) value : false;
    }

    public Double getDouble(String path) {
        if (this.get(path) instanceof Integer) {
            return Double.valueOf((Integer) this.get(path));
        }
        return (Double) this.get(path);
    }

    public Long getLong(String path) {
        return Long.parseLong((String) Objects.requireNonNull(this.get(path)));
    }

    public List<?> getList(String path) {
        Object value = this.get(path);
        return value != null ? (List<?>) value : new ArrayList<>();
    }

    public List<String> getStringList(String path) {
        Object value = this.get(path);
        return value != null ? (List<String>) value : new ArrayList<>();
    }

    public List<Integer> getIntList(String path) {
        Object value = this.get(path);
        return value != null ? (List<Integer>) value : new ArrayList<>();
    }

    public List<Boolean> getBooleanList(String path) {
        Object value = this.get(path);
        return value != null ? (List<Boolean>) value : new ArrayList<>();
    }

    public List<Double> getDoubleList(String path) {
        Object value = this.get(path);
        return value != null ? (List<Double>) value : new ArrayList<>();
    }

    public List<Long> getLongList(String path) {
        Object value = this.get(path);
        return value != null ? (List<Long>) value : new ArrayList<>();
    }

    public boolean isString(String path) {
        return this.get(path) instanceof String;
    }

    public boolean isInt(String path) {
        return this.get(path) instanceof Integer;
    }

    public boolean isBoolean(String path) {
        return this.get(path) instanceof Boolean;
    }

    public boolean isDouble(String path) {
        return this.get(path) instanceof Double;
    }

    public boolean isLong(String path) {
        return this.get(path) instanceof Long;
    }

    public boolean isList(String path) {
        return this.get(path) instanceof List;
    }
}
