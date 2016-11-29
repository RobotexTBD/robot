package ee.ut.physics.digi.tbd.robot.context;

import com.fazecast.jSerialComm.SerialPort;
import ee.ut.physics.digi.tbd.robot.debug.Configurable;
import ee.ut.physics.digi.tbd.robot.mainboard.Mainboard;
import ee.ut.physics.digi.tbd.robot.mainboard.MainboardImpl;
import ee.ut.physics.digi.tbd.robot.mainboard.MainboardMock;
import ee.ut.physics.digi.tbd.robot.referee.RefereeImpl;
import ee.ut.physics.digi.tbd.robot.referee.RefereeMock;
import ee.ut.physics.digi.tbd.robot.referee.RefereeProxy;
import ee.ut.physics.digi.tbd.robot.util.SerialUtil;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Proxy;

public class RobotConfig {

    private final PassThroughInvocationHandler<Mainboard> mainboardInvocationHandler;

    @Getter
    private final Mainboard mainboardProxy;
    private final ClassLoader classLoader;

    @Configurable("Mock mainboard")
    @Getter
    private boolean mockMainboard;

    @Configurable("Mainboard port name")
    @Getter
    private String mainboardPortName;

    @Getter
    private final RefereeProxy refereeProxy;

    @Configurable("Mock referee")
    @Getter
    private boolean mockReferee;

    @Configurable("Referee port name")
    @Getter
    private String refereePortName;

    @Configurable(value = "Max speed", maxFloat = 255.0f)
    @Getter @Setter
    private float speed = 0;

    public RobotConfig() {
        mainboardInvocationHandler = new PassThroughInvocationHandler<>();
        classLoader = this.getClass().getClassLoader();
        mainboardProxy = (Mainboard) Proxy.newProxyInstance(classLoader, new Class[] {Mainboard.class},
                                                            mainboardInvocationHandler);
        mainboardInvocationHandler.setTarget(new MainboardMock());
        refereeProxy = new RefereeProxy();
    }

    public void setMockMainboard(boolean mockMainboard) {
        this.mockMainboard = mockMainboard;
        reloadMainboard();
    }

    public void setMainboardPortName(String mainboardPortName) {
        this.mainboardPortName = mainboardPortName;
        reloadMainboard();
    }

    private void reloadMainboard() {
        if(mockMainboard) {
            mainboardInvocationHandler.setTarget(new MainboardMock());
            return;
        }
        try {
            SerialPort serialPort = SerialUtil.openPort(mainboardPortName);
            mainboardInvocationHandler.setTarget(new MainboardImpl(serialPort));
        } catch(Exception e) {
            mockMainboard = true;
            throw e;
        }
    }

    public void setMockReferee(boolean mockReferee) {
        this.mockReferee = mockReferee;
        reloadReferee();
    }

    public void setRefereePortName(String refereePortName) {
        this.refereePortName = refereePortName;
        reloadReferee();
    }

    private void reloadReferee() {
        if(mockReferee) {
            refereeProxy.setConcreteReferee(new RefereeMock());
            return;
        }
        try {
            SerialPort serialPort = SerialUtil.openPort(refereePortName);
            refereeProxy.setConcreteReferee(new RefereeImpl(serialPort));
        } catch(Exception e) {
            mockReferee = true;
            throw e;
        }
    }


}
