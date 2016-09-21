package ee.ut.physics.digi.tbd.robot.colorspace;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum HSV {

	hue(0),
	saturation(1),
	value(2);

	private int index;

}
