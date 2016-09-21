package ee.ut.physics.digi.tbd.robot.colorspace;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum HSL {

	hue(0),
	saturation(1),
	lightness(2);

	private int index;

}
