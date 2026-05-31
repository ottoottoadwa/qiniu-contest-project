import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class TestMapper {
    public static void main(String[] args) {
        // Test conversion
        OffsetDateTime now = OffsetDateTime.now();
        System.out.println("OffsetDateTime: " + now);

        LocalDateTime local = toLocalDateTime(now);
        System.out.println("LocalDateTime: " + local);

        if (local == null) {
            System.out.println("ERROR: Conversion returned null!");
        } else {
            System.out.println("SUCCESS: Conversion worked!");
        }
    }

    private static LocalDateTime toLocalDateTime(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime() : null;
    }
}
