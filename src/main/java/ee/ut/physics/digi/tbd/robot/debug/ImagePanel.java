package ee.ut.physics.digi.tbd.robot.debug;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ImagePanel {

    ORIGINAL("original"),
    MUTATED1("mutated1"),
    MUTATED2("mutated2"),
    MUTATED3("mutated3"),
    MUTATED4("mutated4"),
    MUTATED5("mutated5");

    private final String id;

}
