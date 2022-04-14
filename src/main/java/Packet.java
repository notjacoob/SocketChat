import java.util.HashMap;
import java.util.Map;

public class Packet {

    private final Map<String, String> keys = new HashMap<>();
    private final String topic;
    public Packet(String topic) {
        this.topic=topic;
    }
    public Packet addValue(String key, String value) {
        keys.put(key, value);
        return this;
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(topic).append("/");
        keys.forEach((s1, s2) -> sb.append(s1).append(":").append(s2).append(";"));
        if (keys.size() > 0) {
            return sb.substring(0, sb.length()-1);
        } else return sb.toString();
    }
    public String topic() {
        return topic;
    }
    public String getValue(String key) {
        return keys.get(key);
    }
    public static Packet from(String encoded) {
        String topic;
        String[] sections = encoded.split("/");
        topic=sections[0];
        Packet p = new Packet(topic);
        if (sections.length > 1) {
            for (String s : sections[1].split(";")) {
                String[] split = s.split(":");
                p.addValue(split[0], split[1]);
            }
        }
        return p;
    }

}
