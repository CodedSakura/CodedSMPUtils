package eu.codedsakura.common;

import eu.codedsakura.common.annotations.*;
import eu.codedsakura.common.exceptions.ConfigParserException;
import eu.codedsakura.common.expression.BoolExpression;
import eu.codedsakura.common.expression.IntExpression;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.util.*;

/**
 * Parses a config file
 * @param <T> annotated with @ConfigFile
 */
public class ConfigParser<T> {
    private final File file;
    private final Class<T> type;
    private final String filename;

    /**
     * Class Constructor
     * @param type Class annotated with @ConfigFile
     * @param dir path of the config file directory
     * @throws ConfigParserException if the target class is not annotated with @ConfigFile
     */
    public ConfigParser(Class<T> type, Path dir) throws ConfigParserException {
        this.type = type;

        if (!type.isAnnotationPresent(ConfigFile.class)) {
            throw new ConfigParserException("Target class is not @ConfigFile!");
        } else {
            filename = type.getAnnotation(ConfigFile.class).value();
        }

        this.file = dir.resolve(filename).toFile();
    }

    /**
     * Creates a config file in specified directory using a default file located in java /resources
     * @throws ConfigParserException if file not found in /resources, if buffer mismatch while reading or if file already exists
     * @throws IOException if file cannot be opened
     */
    private void createFromResources() throws ConfigParserException, IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename);
        if (inputStream == null) {
            throw new ConfigParserException("File " + filename + " not found in the resources folder!");
        }

        byte[] buffer = new byte[inputStream.available()];
        if (inputStream.read(buffer) != buffer.length) {
            throw new ConfigParserException("Buffer length mismatch while reading resources/" + filename + "!");
        }

        if (file.createNewFile()) {
            OutputStream outputStream = new FileOutputStream(file);
            outputStream.write(buffer);
        } else {
            throw new ConfigParserException("Cannot init config: File already exists!");
        }
    }

    /**
     * Gets the XML root object from this.file
     * @return the root element
     */
    private Element getXMLRoot() throws ParserConfigurationException, IOException, SAXException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getDocumentElement();
    }

    /**
     * Casts string to target class
     * @param value input value
     * @param target target class type
     * @return parsed value
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <E> E parseValue(String value, Class<E> target) {
        if (target == boolean.class || target == Boolean.class) {
            return (E) (Boolean) (Boolean.parseBoolean(value) || value.equalsIgnoreCase("yes") ||
                    value.equalsIgnoreCase("enabled") || value.equalsIgnoreCase("1") ||
                    value.equalsIgnoreCase("y"));
        } else if (target == int.class || target == Integer.class) {
            return (E) Integer.valueOf(value);
        } else if (target == float.class || target == Float.class) {
            return (E) Float.valueOf(value);
        } else if (target == double.class || target == Double.class) {
            return (E) Double.valueOf(value);
        } else if (target == long.class || target == Long.class) {
            return (E) Long.valueOf(value);
        } else if (target == BoolExpression.class) {
            return (E) new BoolExpression(value);
        } else if (target == IntExpression.class) {
            return (E) new IntExpression(value);
        } else if (target == String.class) {
            return (E) value;
        } else if (target.getSuperclass() == Enum.class) {
            return (E) Enum.valueOf((Class) target, value);
        }
        return null;
    }

    /**
     * Parses a list of elements to target classes
     * @param nodes input elements
     * @param target target class type
     * @param <E> target class
     * @return parsed list
     */
    private <E> List<E> parseList(Element[] nodes, Class<E> target) throws InstantiationException, IllegalAccessException, ConfigParserException {
        ArrayList<E> list = new ArrayList<>();
        for (Element node : nodes) {
            list.add(mapToClass(node, target));
        }
        return list;
    }

    /**
     * Gets child elements by name
     * @param elem base element
     * @param name target name
     * @return array of elements which match then name
     */
    private static Element[] getChildren(Element elem, String name) {
        NodeList nl = elem.getChildNodes();
        ArrayList<Element> elements = new ArrayList<>();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeName().equals(name)) elements.add((Element) n);
        }
        return elements.toArray(new Element[] {});
    }

    /**
     * Gets all class fields including superclass fields
     * @param fields list of fields
     * @param type class type
     * @return expanded list of fields
     */
    private static List<Field> getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }

    /**
     * Maps element to a class
     * @param elem root element
     * @param target target class
     * @param <R> resulting class
     * @return target class, populated with data
     */
    private <R> R mapToClass(Element elem, Class<R> target) throws InstantiationException, IllegalAccessException, ConfigParserException {
        R instance = target.newInstance();

        HashMap<Integer, HashMap<String, Boolean>> requiredGroups = new HashMap<>();
        for (Field field : getAllFields(new LinkedList<>(), target)) {
            boolean required = field.isAnnotationPresent(Required.class);

            if (field.isAnnotationPresent(Property.class)) {
                String key = field.getAnnotation(Property.class).value();
                if (key.equals("")) {
                    key = field.getName();
                }

                if (required && !elem.hasAttribute(key)) {
                    throw new ConfigParserException("Field '" + key + "' required, but not found on '" + elem.getTagName() + "'!");
                }

                if (elem.hasAttribute(key)) {
                    try {
                        field.set(instance, this.parseValue(elem.getAttribute(key), field.getType()));
                    } catch (Exception e) {
                        throw new ConfigParserException("Invalid value at '" + key + "' in element '" + elem.getTagName() + "'");
                    }
                }
            } else if (field.isAnnotationPresent(ChildNode.class)) {
                mapChildNode(instance, elem, requiredGroups, field);
            } else if (field.isAnnotationPresent(Child.class)) {
                if (required && !elem.hasChildNodes()) {
                    throw new ConfigParserException("Child required, but not found on '" + elem.getTagName() + "'!");
                }

                try {
                    field.set(instance, this.parseValue(elem.getTextContent(), field.getType()));
                } catch (Exception e) {
                    throw new ConfigParserException("Invalid child value in element '" + elem.getTagName() + "'");
                }
            }
        }

        for (Map.Entry<Integer, HashMap<String, Boolean>> entry : requiredGroups.entrySet()) {
            boolean atLeastOne = false;
            StringJoiner sj = new StringJoiner(", ");
            for (Map.Entry<String, Boolean> e : entry.getValue().entrySet()) {
                atLeastOne |= e.getValue();
                sj.add(e.getKey());
            }
            if (!atLeastOne) {
                throw new ConfigParserException("At lease one of " + sj + " required, but none found on '" + elem.getTagName() + "'!");
            }
        }

        return instance;
    }

    /**
     * Maps a child node to a field
     * @param instance target instance
     * @param elem base element
     * @param requiredGroups groups containing required maps
     * @param field field of target instance
     */
    private <R> void mapChildNode(R instance, Element elem, HashMap<Integer, HashMap<String, Boolean>> requiredGroups, Field field)
            throws ConfigParserException, IllegalAccessException, InstantiationException {
        boolean required = field.isAnnotationPresent(Required.class);
        String key = field.getAnnotation(ChildNode.class).value();
        boolean isList = field.getAnnotation(ChildNode.class).list();

        Element[] nodes = getChildren(elem, key);

        if (required) {
            int reqGroup = field.getAnnotation(Required.class).atLeastOneOfGroup();

            if (reqGroup < 0 && nodes.length == 0) {
                throw new ConfigParserException("child node '" + key + "' required, but not found in '" + elem.getTagName() + "'!");
            } else if (reqGroup > -1) {
                if (!requiredGroups.containsKey(reqGroup)) {
                    requiredGroups.put(reqGroup, new HashMap<>());
                }
                requiredGroups.get(reqGroup).put(key, nodes.length > 0);
            }
        }

        if (!isList && nodes.length > 1) {
            throw new ConfigParserException("child node '" + key + "' should be singular, but found multiple in '" + elem.getTagName() + "'!");
        }

        if (isList) {
            ParameterizedType type = (ParameterizedType) field.getGenericType();
            Class<?> clazz = (Class<?>) type.getActualTypeArguments()[0];
            field.set(instance, parseList(nodes, clazz));
        } else if (nodes.length > 0) {
            field.set(instance, mapToClass(nodes[0], field.getType()));
        }
    }

    /**
     * Reads the config file mapping it to the generic T
     * @return T with all values loaded
     * @throws Exception if anything goes wrong
     */
    public T read() throws Exception {
        if (!file.exists()) createFromResources();
        Element root = this.getXMLRoot();
        return mapToClass(root, type);
    }
}
